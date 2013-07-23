/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.scripts;

import com.eas.client.AppCache;
import com.eas.client.Client;
import com.eas.client.ClientConstants;
import com.eas.client.login.PlatypusPrincipal;
import com.eas.client.login.PrincipalHost;
import com.eas.client.metadata.ApplicationElement;
import com.eas.client.model.application.ApplicationModel;
import com.eas.client.settings.SettingsConstants;
import com.eas.debugger.jmx.server.Breakpoints;
import com.eas.script.JsDoc;
import com.eas.script.JsDoc.Tag;
import com.eas.script.ScriptFunction;
import com.eas.script.ScriptUtils;
import com.eas.util.BinaryUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.*;

/**
 *
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
        } catch (IOException ex) {
            Logger.getLogger(ScriptRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ScriptRunner(String aAppElementId, Client aClient, Scriptable aScope, PrincipalHost aPrincipalHost, CompiledScriptDocumentsHost aCompiledScriptDocumentsHost, Object[] args) throws Exception {
        this(aClient, aScope, aPrincipalHost, aCompiledScriptDocumentsHost, args);
        setApplicationElementId(aAppElementId, args);
    }

    public ScriptRunner(Client aClient, Scriptable aScope, PrincipalHost aPrincipalHost, CompiledScriptDocumentsHost aCompiledScriptDocumentsHost, Object[] args) throws Exception {
        super(aScope, null);
        client = aClient;
        principalHost = aPrincipalHost;
        compiledScriptDocumentsHost = aCompiledScriptDocumentsHost;
        definePropertiesAndMethods();
        setPrototype(ScriptRunnerPrototype.getInstance());
    }

    public boolean hasModuleAnnotation(String aName) {
        return Tag.containsTagWithName(moduleAnnotations, aName);
    }

    protected void doExecute() throws Exception {
        ScriptUtils.enterContext();
        try {
            model.setRuntime(true);
            // Executing of the datamodel here is very useful.
            // At this moment all of the scripts, including standard and platypus libraries
            // are executed, and so, it works properly!
        } finally {
            Context.exit();
        }
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
        functionAllowedRoles = scriptDoc.getFunctionAllowedRoles();
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

    protected void prepareScript(ScriptDocument scriptDoc, Object[] args) throws Exception {
        Context context = ScriptUtils.enterContext();
        try {
            model.setScriptScope(this);
            if (scriptDoc.getScript() != null) {
                if (System.getProperty(DEBUG_PROPERTY) != null) {
                    Breakpoints.getInstance().checkPendingBreakpoints();
                }
                if (scriptDoc.getScript() instanceof Function) {
                    ((Function) scriptDoc.getScript()).call(context, this, this, args);
                } else {
                    scriptDoc.getScript().exec(context, this);
                }
            }
            model.resolveHandlers();
        } finally {
            Context.exit();
        }
    }

    public void execute() throws Exception {
        if (!executed) {
            executed = true;
            doExecute();
        }
    }

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
    private static final String GET_APPICATION_ELEMENT_ID_JSDOC = ""
            + "/**\n"
            + "* Gets application element Id.\n"
            + "* @return Module's application element Id\n"
            + "*/";

    /**
     * Gets application element Id
     *
     * @return Module's application element Id
     */
    @ScriptFunction(jsDoc = GET_APPICATION_ELEMENT_ID_JSDOC)
    public String getApplicationElementId() {
        return appElementId;
    }

    public long getTxtContentLength() {
        return txtContentLength;
    }

    public long getTxtCrc32() {
        return txtCrc32;
    }
    private static final String GET_PRINCIPAL_JSDOC = ""
            + "/**\n"
            + "* Script security API.\n"
            + "* @return PlatypusPrincipal instance, wich may be used to check roles in\n"
            + "* application code with calls of the hasRole method.\n"
            + "*/";

    /**
     * Script security API.
     *
     * @return PlatypusPrincipal instance, wich may be used to check roles in
     * application code with calls of the hasRole method.
     * @see PlatypusPrincipal
     */
    @ScriptFunction(jsDoc = GET_PRINCIPAL_JSDOC)
    public PlatypusPrincipal getPrincipal() {
        if (principalHost != null) {
            return principalHost.getPrincipal();
        }
        return null;
    }

    @Override
    public String getClassName() {
        return ScriptRunner.class.getName();
    }

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
            // no op
        }
        Object obj = super.get(name, start);
        if (obj instanceof Function) {
            obj = new SecureFunction(name, (Function) obj);
        }
        return obj;
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

    public static class PlatypusScriptedResource {

        private static final Pattern pattern = Pattern.compile("https?://.*");
        protected static AppCache cache;

        public static void init(AppCache aCache) {
            assert cache == null : "Platypus application resources may be initialized only once.";
            cache = aCache;
        }

        protected static String translateResourcePath(String aPath) throws Exception {
            /*
             File test = new File(aPath);
             if (test.exists()) {
             // it seems, that id is a real file path
             return test.getPath();
             } else {
             */
            if (aPath.startsWith("/")) {
                throw new IllegalStateException("Platypus resource path can't begin with /. Platypus resource paths must point somewhere in application, but not in filesystem.");
            }
            if (aPath.startsWith("..") || aPath.startsWith(".")) {
                /*
                 EvaluatorException ex = Context.reportRuntimeError("_");
                 ScriptStackElement[] stack = ex.getScriptStack();
                 traverse stack to reach non platypusStandardLib script and use it as base path
                 */
                throw new IllegalStateException("Platypus resource paths must be application-absolute. \"" + aPath + "\" is not application-absolute");
            }
            URI uri = new URI(null, null, aPath, null);
            return uri.normalize().getPath();
            //}
        }

        public static byte[] load(String aResourceId) throws Exception {
            if (aResourceId != null && !aResourceId.isEmpty()) {
                Matcher htppMatcher = pattern.matcher(aResourceId);
                if (htppMatcher.matches()) {
                    URL url = new URL(aResourceId);
                    try {
                        try (InputStream is = url.openStream()) {
                            return BinaryUtils.readStream(is, -1);
                        }
                    } catch (IOException ex) {
                        url = encodeUrl(url);
                        try (InputStream is = url.openStream()) {
                            return BinaryUtils.readStream(is, -1);
                        }
                    }
                } else {
                    if (cache == null) {
                        throw new IllegalStateException("Platypus application resources have to be initialized first.");
                    }

                    String resourceId = translateResourcePath(aResourceId);
                    /*
                     File test = new File(resourceId);
                     if (test.exists()) {
                     return FileUtils.readBytes(test);
                     } else {
                     */
                    ApplicationElement appElement = cache.get(resourceId);
                    if (appElement != null) {
                        if (appElement.getType() == ClientConstants.ET_RESOURCE) {
                            // let's check actuality
                            if (!cache.isActual(appElement.getId(), appElement.getTxtContentLength(), appElement.getTxtCrc32())) {
                                cache.remove(appElement.getId());
                                appElement = cache.get(resourceId);
                            }
                        } else {
                            throw new NotResourceException(resourceId);
                        }
                    }
                    if (appElement != null && appElement.getType() == ClientConstants.ET_RESOURCE) {
                        return appElement.getBinaryContent();
                    } else {
                        return null;
                    }
                    //}
                }
            } else {
                return null;
            }
        }

        public static String loadText(String aResourceId) throws Exception {
            return loadText(aResourceId, SettingsConstants.COMMON_ENCODING);
        }

        public static String loadText(String aResourceId, String aEncodingName) throws Exception {
            byte[] data = load(aResourceId);
            return data != null ? new String(data, aEncodingName) : null;
        }

        private static URL encodeUrl(URL url) throws URISyntaxException, MalformedURLException {
            String file = "";
            if (url.getPath() != null && !url.getPath().isEmpty()) {
                file += (new URI(null, null, url.getPath(), null)).toASCIIString();
            }
            if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                file += "?" + url.getQuery();
            }
            if (url.getRef() != null && !url.getRef().isEmpty()) {
                file += "#" + url.getRef();
            }
            url = new URL(url.getProtocol(), IDN.toASCII(url.getHost()), url.getPort(), file);
            return url;
        }
    }
    protected static Set<String> executedScriptResources = new HashSet<>();

    public static void executeResource(String aResourceId) throws Exception {
        String resourceId = PlatypusScriptedResource.translateResourcePath(aResourceId);
        if (!executedScriptResources.contains(resourceId)) {
            Context cx = Context.getCurrentContext();
            boolean wasContext = cx != null;
            if (!wasContext) {
                cx = ScriptUtils.enterContext();
            }
            try {
                try {
                    String source = PlatypusScriptedResource.loadText(resourceId);
                    if (source != null) {
                        Script lib = cx.compileString(source, resourceId, 0, null);
                        lib.exec(cx, checkStandardObjects(cx));
                    } else {
                        throw new IllegalArgumentException("Script resource not found: " + resourceId + ". Hint: Regular platypus modules can't be used as resources.");
                    }
                } catch (NotResourceException ex) {
                    // Silently return.
                    // There are cases, when require is called with regular platypus module id.
                    // In such case, we have to ignore require call is SE client and server and servlet,
                    // and perform standard actions in browser html5 client.
                    return;
                }
            } finally {
                if (!wasContext) {
                    Context.exit();
                }
            }
            executedScriptResources.add(resourceId);
        }
    }

    public static Script importScriptLibrary(String libResourceName, String aLibName, Context currentContext, Scriptable aScope) throws IOException {
        try (InputStream is = ScriptRunner.class.getResourceAsStream(libResourceName); InputStreamReader isr = new InputStreamReader(is)) {
            Script compiled = currentContext.compileReader(isr, aLibName, 0, null);
            compiled.exec(currentContext, aScope);
            return compiled;
        }
    }

    public static Scriptable checkStandardObjects(Context currentContext) {
        synchronized (standardObjectsScopeLock) {
            if (standardObjectsScope == null) {
                standardObjectsScope = ScriptUtils.getScope();
            }
        }
        return standardObjectsScope;
    }

    public static Scriptable initializePlatypusStandardLibScope() throws IOException {
        synchronized (standardObjectsScopeLock) {
            if (platypusStandardLibScope == null) {
                Context context = ScriptUtils.enterContext();
                try {
                    checkStandardObjects(context);
                    // Fix from rsp! Let's initialize library functions
                    // in top-level scope.
                    //platypusStandardLibScope = (ScriptableObject) context.newObject(standardObjectsScope);
                    //platypusStandardLibScope.setPrototype(standardObjectsScope);
                    platypusStandardLibScope = standardObjectsScope;
                    importScriptLibrary("/com/eas/client/scripts/standartLib.js", "platypusStandardLib", context, platypusStandardLibScope);
                } finally {
                    Context.exit();
                }
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

    protected void setApplicationElementId(String aAppElementId, Object[] args) throws Exception {
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

    private void defineJsClass(final String aClassName, ApplicationElement aAppElement) {
        compiledScriptDocumentsHost.defineJsClass(aClassName, aAppElement);
    }

    /**
     * Wrapper class for function with security check.
     */
    public class SecureFunction implements Function, Runnable {

        String name;
        Function func;

        public SecureFunction(String aName, Function aFun) {
            super();
            name = aName;
            func = aFun;
        }

        @Override
        public void run() {
            Context cx = ScriptUtils.enterContext();
            try {
                call(cx, ScriptUtils.getScope(), null, new Object[]{});
            } finally {
                Context.exit();
            }
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            checkPrincipalPermission();
            return func.call(cx, scope, thisObj, args);
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            checkPrincipalPermission();
            return func.construct(cx, scope, args);
        }

        @Override
        public String getClassName() {
            return func.getClassName();
        }

        private void checkPrincipalPermission() throws AccessControlException {
            try {
                PlatypusPrincipal principal = getPrincipal();
                if (functionAllowedRoles != null && functionAllowedRoles.get(name) != null && !functionAllowedRoles.get(name).isEmpty()) {
                    if (principal != null && principal.hasAnyRole(functionAllowedRoles.get(name))) {
                        return;
                    }
                    throw new AccessControlException(String.format("Access denied to %s function in %s module for %s PlatypusPrincipal.",//NOI18N
                            name,
                            ScriptRunner.this.appElementId,
                            principal != null ? principal.getName() : null));
                } else {
                    if (moduleAllowedRoles != null && !moduleAllowedRoles.isEmpty()) {
                        if (principal != null && principal.hasAnyRole(moduleAllowedRoles)) {
                            return;
                        }
                        throw new AccessControlException(String.format("Access denied to %s function in %s module for %s PlatypusPrincipal at module level.",//NOI18N
                                name,
                                ScriptRunner.this.appElementId,
                                principal != null ? principal.getName() : null));
                    }
                }
            } catch (Exception ex) {
                if (ex instanceof AccessControlException) {
                    throw (AccessControlException) ex;
                } else {
                    throw new AccessControlException(ex.getMessage());
                }
            }
        }

        @Override
        public Object get(String name, Scriptable start) {
            return func.get(name, start);
        }

        @Override
        public Object get(int index, Scriptable start) {
            return func.get(index, start);
        }

        @Override
        public boolean has(String name, Scriptable start) {
            return func.has(name, start);
        }

        @Override
        public boolean has(int index, Scriptable start) {
            return func.has(index, start);
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            func.put(name, start, value);
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            func.put(index, start, value);
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
    /*
     public class ModuleConstructor extends BaseFunction {

     public ModuleConstructor(Scriptable scope, Scriptable prototype) {
     super(scope, prototype);
     }

     @Override
     public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
     return super.construct(cx, scope, args);
     }
     }
     */
}
