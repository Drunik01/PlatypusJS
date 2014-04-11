/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.scripts;

import com.eas.client.Client;
import com.eas.client.ClientConstants;
import com.eas.client.login.PlatypusPrincipal;
import com.eas.client.login.PrincipalHost;
import com.eas.client.metadata.ApplicationElement;
import com.eas.client.model.application.ApplicationModel;
import com.eas.debugger.jmx.server.Breakpoints;
import com.eas.script.JsDoc;
import com.eas.script.JsDoc.Tag;
import com.eas.script.ScriptFunction;
import com.eas.script.ScriptUtils;
import com.eas.script.ScriptUtils.ScriptAction;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.*;

/**
 * Main core js module implementation.
 * Integrated with Rhino via <code>ScriptRunnerPrototype</code>.
 * @see ScriptRunnerPrototype
 * @author pk, mg refactoring
 */
public class ScriptRunner extends ScriptableObject {

    public static final String DEBUG_PROPERTY = "com.sun.management.jmxremote";
    public static final Object SEAL_KEY = new Object();
    protected static ScriptableObject standardObjectsScope;
    protected static ScriptableObject platypusStandardLibScope;
    private static final Object standardObjectsScopeLock = new Object();
    public static final String APP_ELEMENT_NOT_FOUND_MSG = "Application element with id %s not found.";
    // configuration
    protected Client client;
    protected String appElementId;
    protected long txtContentLength;
    protected long txtCrc32;
    // runtime
    //protected ApplicationElement appElement;
    // model from the document.
    // We need to copy it because document is cached.
    protected ApplicationModel<?, ?, ?, ?> model;
    protected Map<String, Set<String>> functionAllowedRoles;
    protected Set<String> moduleAllowedRoles;
    protected List<JsDoc.Tag> moduleAnnotations;
    /**
     * Current principal provider
     */
    private PrincipalHost principalHost;
    private CompiledScriptDocumentsHost compiledScriptDocumentsHost;
    protected boolean executed;

    static {
        try {
            initializePlatypusStandardLibScope();
        } catch (Exception ex) {
            Logger.getLogger(ScriptRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ScriptRunner(String aAppElementId, Client aClient, Scriptable aScope, PrincipalHost aPrincipalHost, CompiledScriptDocumentsHost aCompiledScriptDocumentsHost, Object[] args) throws Exception {
        this(aClient, aScope, aPrincipalHost, aCompiledScriptDocumentsHost);
        loadApplicationElement(aAppElementId, args);
    }

    public ScriptRunner(Client aClient, Scriptable aScope, PrincipalHost aPrincipalHost, CompiledScriptDocumentsHost aCompiledScriptDocumentsHost) throws Exception {
        super(aScope, ScriptRunnerPrototype.getInstance());
        client = aClient;
        principalHost = aPrincipalHost;
        compiledScriptDocumentsHost = aCompiledScriptDocumentsHost;
        definePropertiesAndMethods();
    }

    public boolean hasModuleAnnotation(String aName) {
        return Tag.containsTagWithName(moduleAnnotations, aName);
    }

    /**
     * Queries model's data for the first time.
     * @throws Exception 
     */
    protected void doExecute() throws Exception {
        ScriptUtils.inContext(new ScriptAction() {
            @Override
            public Object run(Context cx) throws Exception {
                model.setRuntime(true);
                // Executing of the datamodel here is very useful.
                // At this moment all of the scripts, including standard and platypus libraries
                // are executed, and so, it works properly!
                return null;
            }
        });
    }

    protected void definePropertiesAndMethods() {
        defineProperty("applicationElementId", ScriptRunner.class, READONLY);
        defineProperty("principal", ScriptRunner.class, READONLY);
    }

    protected void prepare(ScriptDocument scriptDoc, Object[] args) throws Exception {
        prepareRoles(scriptDoc);
        prepareModel(scriptDoc);
        prepareScript(scriptDoc, args);
    }

    protected void prepareRoles(ScriptDocument scriptDoc) throws Exception {
        txtContentLength = scriptDoc.getTxtContentLength();
        txtCrc32 = scriptDoc.getTxtCrc32();
        functionAllowedRoles = scriptDoc.getPropertyAllowedRoles();
        moduleAllowedRoles = scriptDoc.getModuleAllowedRoles();
        moduleAnnotations = scriptDoc.getModuleAnnotations();
    }

    protected void prepareModel(ScriptDocument scriptDoc) throws Exception {
        if (scriptDoc.getModel() != null) {
            model = (ApplicationModel<?, ?, ?, ?>) scriptDoc.getModel().copy();
            // Executing of the datamodel here is very dangerous.
            // There might be a onRequeried/onFiltered handler,
            // but scriptDoc is not executed yet, so it will not work!
        }
    }

    /**
     * Injects platypus features into 'this' object, calls js module's constructor with arguments and finally resolves model'd entities' events handlers.
     * @param scriptDoc
     * @param args
     * @throws Exception 
     */
    protected void prepareScript(final ScriptDocument scriptDoc, final Object[] args) throws Exception {
        ScriptUtils.inContext(new ScriptAction() {
            @Override
            public Object run(Context cx) throws Exception {
                model.setScriptThis(ScriptRunner.this);
                if (scriptDoc.getFunction() != null) {
                    Object[] jsArgs = new Object[args != null ? args.length : 0];
                    for (int i = 0; i < jsArgs.length; i++) {
                        jsArgs[i] = ScriptUtils.javaToJS(args[i], ScriptUtils.getScope());
                    }
                    scriptDoc.getFunction().call(cx, getParentScope(), ScriptRunner.this, jsArgs);
                }
                return null;
            }
        });
    }

    /**
     * Ensures that model's data is queried and fetched from sources. (Note: in asynchronous clients data in not fetched here).
     * @throws Exception 
     */
    public void execute() throws Exception {
        if (!executed) {
            executed = true;
            doExecute();
        }
    }

    /**
     * Shrinks internal structures, allowing to reuse this instance of <code>ScriptRunner</code> with probably new content (hot reloading feature).
     * @see #refresh() 
     * @throws Exception 
     */
    protected void shrink() throws Exception {
        model = null;
        functionAllowedRoles = null;
        moduleAllowedRoles = null;
        moduleAnnotations = null;
    }

    /**
     * Refreshs content of the script runner. Reloads it from application
     * storage and re-executes new instance of model.
     *
     * @see #shrink() 
     * @throws Exception
     */
    public synchronized void refresh() throws Exception {
        shrink();
        assert compiledScriptDocumentsHost != null;
        ScriptDocument scriptDoc = compiledScriptDocumentsHost.getDocuments().compileScriptDocument(appElementId);
        if (scriptDoc == null) {
            throw new NullPointerException(String.format(APP_ELEMENT_NOT_FOUND_MSG, appElementId));
        }
        prepare(scriptDoc, new Object[]{});
        executed = false;
        execute();
    }

    public Client getClient() {
        return client;
    }

    public ApplicationModel<?, ?, ?, ?> getModel() {
        return model;
    }

    /**
     * Gets application element Id
     *
     * @return Module's application element Id
     */
    public String getApplicationElementId() {
        return appElementId;
    }

    public long getTxtContentLength() {
        return txtContentLength;
    }

    public long getTxtCrc32() {
        return txtCrc32;
    }
    
    private static final String PRINCIPAL_JSDOC = ""
            + "/**\n"
            + "* <code>PlatypusPrincipal</code> instance, wich may be used to check roles in\n"
            + "* application code with calls of the <code>hasRole</code> method.\n"
            + "*/";

    /**
     * Script security API.
     *
     * @return PlatypusPrincipal instance, wich may be used to check roles in
     * application code with calls of the hasRole method.
     * @see PlatypusPrincipal
     */
    @ScriptFunction(jsDoc = PRINCIPAL_JSDOC)
    public Object getPrincipal() {
        if (principalHost != null) {
            return Context.javaToJS(principalHost.getPrincipal(), this);
        }
        return null;
    }

    protected PlatypusPrincipal _getPrincipal() {
        if (principalHost != null) {
            return principalHost.getPrincipal();
        }
        return null;
    }

    @Override
    public String getClassName() {
        return ScriptRunner.class.getName();
    }

    /**
     * Overriden to provide dynamic js classes definition.
     * @param name Script property name. In case of global ScriptRunner object it may serve as platypus js module name.
     * @param start
     * @return 
     */
    @Override
    public Object get(String name, Scriptable start) {
        try {
            if (name != null && !name.isEmpty() && !super.has(name, start) && !ScriptUtils.getScope().has(name, ScriptUtils.getScope())) {
                ApplicationElement moduleCandidate = client.getAppCache().get(name);
                if (moduleCandidate != null && (moduleCandidate.getType() == ClientConstants.ET_COMPONENT || moduleCandidate.getType() == ClientConstants.ET_FORM || moduleCandidate.getType() == ClientConstants.ET_REPORT)) {
                    defineJsClass(name, moduleCandidate);
                }
            }
        } catch (Exception ex) {
            // no op. Because absence of a particuler class will lead to script error anyway.
        }
        Object obj = super.get(name, start);
        if (obj instanceof Function) {
            obj = new SecureFunction(name, (Function) obj);
        }
        return obj;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    protected static class NotResourceException extends Exception {

        protected String resourceId;

        public NotResourceException() {
            super();
        }

        public NotResourceException(String aResourceId) {
            super(aResourceId + " is not a platypus resource. Hint: may be it is regular platypus module.");
            resourceId = aResourceId;
        }

        public String getResourceId() {
            return resourceId;
        }
    }
    
    /**
     * Accounting of already executed scripts. Allows to avoid reexecution.
     */
    protected static Set<String> executedScriptResources = new HashSet<>();

    /**
     * Executes a plain js resource (file).
     * @param aResourceId
     * @throws Exception 
     */
    public static void executeResource(final String aResourceId) throws Exception {
        final String resourceId = PlatypusScriptedResource.translateResourcePath(aResourceId);
        if (!executedScriptResources.contains(resourceId)) {
            try {
                ScriptUtils.inContext(new ScriptAction() {
                    @Override
                    public Object run(Context cx) throws Exception {
                        String source = PlatypusScriptedResource.loadText(resourceId);
                        if (source != null) {
                            /**
                             * TODO: check update of rhino. May be it would be
                             * able to use bytecode generation. if
                             * (currentContext.getDebugger() == null) {
                             * currentContext.setOptimizationLevel(5); }else
                             * currentContext.setOptimizationLevel(-1);
                             */
                            cx.setOptimizationLevel(-1);
                            Script lib = cx.compileString(source, resourceId, 0, null);
                            if (System.getProperty(DEBUG_PROPERTY) != null) {
                                Breakpoints.getInstance().checkPendingBreakpoints();
                            }
                            lib.exec(cx, checkStandardObjects(cx));
                        } else {
                            throw new IllegalArgumentException("Script resource not found: " + resourceId + ". Hint: Regular platypus modules can't be used as resources.");
                        }
                        return null;
                    }
                });
            } catch (NotResourceException ex) {
                // Silently return.
                // There are cases, when require is called with regular platypus module id.
                // In such case, we have to ignore require call in SE client and server and servlet,
                // and perform standard actions for html5 browser client.
                return;
            }
            executedScriptResources.add(resourceId);
        }
    }

    /**
     * Imports a system library js resource (file) into global js space.
     * @param libResourceName Js resource (file) name (aka standardLib.js). See use cases for convenience.
     * @param aLibName Js virtual library name. See use cases for convenience.
     * @param currentContext
     * @param aScope
     * @return
     * @throws IOException 
     */
    public static Script importScriptLibrary(String libResourceName, String aLibName, Context currentContext, Scriptable aScope) throws IOException {
        try (InputStream is = ScriptRunner.class.getResourceAsStream(libResourceName); InputStreamReader isr = new InputStreamReader(is)) {
            Script compiled = currentContext.compileReader(isr, aLibName, 0, null);
            compiled.exec(currentContext, aScope);
            return compiled;
        }
    }

    /**
     * 
     * Ensures that platypus standard script scope is constructed.
     * @param currentContext
     * @return
     * @throws Exception 
     * @see #get(java.lang.String, org.mozilla.javascript.Scriptable) 
     */
    public static ScriptableObject checkStandardObjects(Context currentContext) throws Exception {
        synchronized (standardObjectsScopeLock) {
            if (standardObjectsScope == null) {
                standardObjectsScope = new ScriptRunner(PlatypusScriptedResource.getClient(), ScriptUtils.getScope(), PlatypusScriptedResource.getPrincipalHost(), PlatypusScriptedResource.getScriptDocumentsHost());
            }
        }
        return standardObjectsScope;
    }

    public static Scriptable initializePlatypusStandardLibScope() throws Exception {
        synchronized (standardObjectsScopeLock) {
            if (platypusStandardLibScope == null) {
                ScriptUtils.inContext(new ScriptAction() {
                    @Override
                    public Object run(Context cx) throws Exception {
                        checkStandardObjects(cx);
                        // Fix from rsp! Let's initialize library functions
                        // in top-level scope.
                        //platypusStandardLibScope = (ScriptableObject) context.newObject(standardObjectsScope);
                        //platypusStandardLibScope.setPrototype(standardObjectsScope);
                        platypusStandardLibScope = standardObjectsScope;
                        importScriptLibrary("/com/eas/client/scripts/standartLib.js", "platypusStandardLib", cx, platypusStandardLibScope);
                        return null;
                    }
                });
            }
        }
        return platypusStandardLibScope;
    }

    public PrincipalHost getPrincipalHost() {
        return principalHost;
    }

    public CompiledScriptDocumentsHost getCompiledScriptDocumentsHost() {
        return compiledScriptDocumentsHost;
    }

    /**
     * Loads a script document from application database or filesystem and prepares it for execution and calls module's constructor.
     * @param aAppElementId Global module name (at application level) or path to executable js file.
     * @param args Js module's constructor arguments.
     * @throws Exception 
     */
    public void loadApplicationElement(String aAppElementId, Object[] args) throws Exception {
        if (appElementId == null ? aAppElementId != null : !appElementId.equals(aAppElementId)) {
            shrink();
            appElementId = aAppElementId;
            assert compiledScriptDocumentsHost != null;
            ScriptDocument scriptDoc = compiledScriptDocumentsHost.getDocuments().compileScriptDocument(appElementId);
            if (scriptDoc == null) {
                throw new NullPointerException(String.format(APP_ELEMENT_NOT_FOUND_MSG, appElementId));
            }
            prepare(scriptDoc, args);
        }
    }

    /**
     * Checks module roles.
     *
     * @throws Exception
     */
    public void checkPrincipalPermission() throws Exception {
        if (moduleAllowedRoles != null && !moduleAllowedRoles.isEmpty()) {
            PlatypusPrincipal principal = _getPrincipal();
            if (principal == null || !principal.hasAnyRole(moduleAllowedRoles)) {
                throw new AccessControlException(String.format("Access denied to %s module for '%s'.",//NOI18N
                        ScriptRunner.this.appElementId,
                        principal != null ? principal.getName() : null));
            }
        }
    }

    private void defineJsClass(final String aClassName, ApplicationElement aAppElement) {
        compiledScriptDocumentsHost.defineJsClass(aClassName, aAppElement);
    }

    /**
     * Wrapper class for function with security check.
     */
    public class SecureFunction extends WrapperFunction implements Runnable {

        protected String name;

        public SecureFunction(String aName, Function aFun) {
            super(aFun);
            name = aName;
        }

        @Override
        public void run() {
            try {
                ScriptUtils.inContext(new ScriptAction() {
                    @Override
                    public Object run(Context cx) throws Exception {
                        call(cx, ScriptUtils.getScope(), null, new Object[]{});
                        return null;
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(ScriptRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            checkPrincipalPermission();
            return super.call(cx, scope, thisObj, args);
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            checkPrincipalPermission();
            return super.construct(cx, scope, args);
        }

        private void checkPrincipalPermission() throws AccessControlException {
            try {
                PlatypusPrincipal principal = _getPrincipal();
                if (functionAllowedRoles != null && functionAllowedRoles.get(name) != null && !functionAllowedRoles.get(name).isEmpty()) {
                    if (principal != null && principal.hasAnyRole(functionAllowedRoles.get(name))) {
                        return;
                    }
                    throw new AccessControlException(String.format("Access denied to %s function in %s module for '%s'.",//NOI18N
                            name,
                            ScriptRunner.this.appElementId,
                            principal != null ? principal.getName() : null));
                } else {
                    ScriptRunner.this.checkPrincipalPermission();
                }
            } catch (Exception ex) {
                if (ex instanceof AccessControlException) {
                    throw (AccessControlException) ex;
                } else {
                    throw new AccessControlException(ex.getMessage());
                }
            }
        }
    }

    public static abstract class PlatypusModuleConstructorWrapper extends WrapperFunction {

        protected String moduleName;

        public PlatypusModuleConstructorWrapper(String aModuleName, Function aDelegate) {
            super(aDelegate);
            moduleName = aModuleName;
        }

        protected abstract Scriptable createObject(Context cx, Scriptable scope, Object[] args);

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            try {
                Scriptable runner = createObject(cx, scope, args);
                Object oprot = get("prototype", this);
                if (oprot instanceof Scriptable) {
                    runner.setPrototype((Scriptable) oprot);
                }
                if (runner instanceof ScriptRunner) {
                    ((ScriptRunner) runner).execute();
                }
                return runner;
            } catch (Exception ex) {
                Logger.getLogger(PlatypusModuleConstructorWrapper.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            throw new IllegalStateException(moduleName + "() should be used as a constructor only.");
        }
    }

    public static class WrapperFunction implements Function {

        protected Function func;

        public WrapperFunction(Function aFun) {
            super();
            func = aFun;
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            return func.construct(cx, scope, args);
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            return func.call(cx, scope, thisObj, args);
        }

        @Override
        public String getClassName() {
            return func.getClassName();
        }

        @Override
        public Object get(String name, Scriptable start) {
            return func.get(name, start == this ? func : start);
        }

        @Override
        public Object get(int index, Scriptable start) {
            return func.get(index, start == this ? func : start);
        }

        @Override
        public boolean has(String name, Scriptable start) {
            return func.has(name, start == this ? func : start);
        }

        @Override
        public boolean has(int index, Scriptable start) {
            return func.has(index, start == this ? func : start);
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            func.put(name, start == this ? func : start, value);
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            func.put(index, start == this ? func : start, value);
        }

        @Override
        public void delete(String name) {
            func.delete(name);
        }

        @Override
        public void delete(int index) {
            func.delete(index);
        }

        @Override
        public Scriptable getPrototype() {
            return func.getPrototype();
        }

        @Override
        public void setPrototype(Scriptable prototype) {
            func.setPrototype(prototype);
        }

        @Override
        public Scriptable getParentScope() {
            return func.getParentScope();
        }

        @Override
        public void setParentScope(Scriptable parent) {
            func.setParentScope(parent);
        }

        @Override
        public Object[] getIds() {
            return func.getIds();
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
            return func.getDefaultValue(hint);
        }

        @Override
        public boolean hasInstance(Scriptable instance) {
            return func.hasInstance(instance);
        }

        public Function unwrap() {
            return func;
        }
    }
}
