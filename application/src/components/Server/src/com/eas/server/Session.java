/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server;

import com.eas.script.AlreadyPublishedException;
import com.eas.script.HasPublished;
import com.eas.script.NoPublisherException;
import com.eas.script.ScriptFunction;
import com.eas.server.scripts.ModulesJSFacade;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.internal.runtime.JSType;

/**
 * A client session
 *
 * <p>
 * This object is created to represent a session with successfully authenticated
 * client. It is used to associate various resources such as tasks with a a
 * client. Whenever a session is <code>cleanup()</code>-ed, the resources are
 * deleted.</p> Method rollback of database client is also invoked.
 *
 * @author pk, mg refactoring
 */
public class Session implements HasPublished {

    protected JSObject published;
    //
    private final String sessionId;
    private final long ctime;
    private final AtomicLong atime = new AtomicLong();
    private final Map<String, JSObject> modulesInstances = new HashMap<>();
    private int maxInactiveInterval = 3600000; // 1 hour
    private final JSObject jsModules = new ModulesJSFacade(this);

    /**
     * Creates a new session with given session id.
     *
     * @param aServerCore
     * @param aSessionId unique session id.
     */
    public Session(String aSessionId) {
        super();
        sessionId = aSessionId;
        ctime = System.currentTimeMillis();
        atime.set(ctime);
    }

    /**
     * Deletes all resources belonging to this session.
     */
    public synchronized void cleanup() {
        // server modules
        modulesInstances.clear();
        // data in client's transaction
    }

    /**
     * Returns the creation time of this session (server time).
     *
     * @return session creation time.
     */
    public long getCTime() {
        return ctime;
    }

    /**
     * Returns the last access time of this session (server time).
     *
     * <p>
     * The last access time is the last time accessed() was called. This
     * mechanism is used to track down sessions which have been idle for a long
     * time, i.e. possible zombies.</p>
     *
     * @return last access time.
     */
    public long getATime() {
        return atime.get();
    }

    /**
     * Mark that this session was just accessed by its client, update last
     * access time.
     *
     * <p>
     * The last access time is the last time accessed() was called. This
     * mechanism is used to track down sessions which have been idle for a long
     * time, i.e. possible zombies.</p>
     *
     * <p>
     * Call this method once for each client request inside this session.</p>
     *
     * @return new last access time.
     */
    public long accessed() {
        atime.set(System.currentTimeMillis());
        return atime.get();
    }

/*
    public void setPrincipal(PlatypusPrincipal aPrincipal) {
        String oldUserName = principal != null ? principal.getName() : null;
        String newUserName = aPrincipal != null ? aPrincipal.getName() : null;
        if (oldUserName == null ? newUserName != null : !oldUserName.equals(newUserName)) {
            userContext = null;
            if (newUserName != null && serverCore != null && serverCore.getDatabasesClient() != null) {
                try {
                    Map<String, String> userProps = DatabasesClient.getUserProperties(serverCore.getDatabasesClient(), newUserName, null, null);
                    userContext = userProps.get(ClientConstants.F_USR_CONTEXT);
                } catch (Exception ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, "Could not get user {0} properties (USR_CONTEXT, etc).", newUserName);
                }
            }
        }
        if (principal != null) {
            principal.setContext(null);
        }
        principal = aPrincipal;
        if (principal != null) {
            // let's update pricipal's 
            principal.setContext(userContext);
            // pricipal's roles are processed by container, so there is no need to update them here
        }
    }
*/
    /**
     * Returns server module by name.
     *
     * @param aName
     * @return
     */
    public synchronized JSObject getModule(String aName) {
        return modulesInstances.get(aName);
    }

    public synchronized boolean containsModule(String aName) {
        return modulesInstances.containsKey(aName);
    }

    public void registerModule(JSObject aModule) {
        registerModule(null, aModule);
    }

    public synchronized void registerModule(String aName, JSObject aModule) {
        if (aName == null || aName.isEmpty()) {
            JSObject c = (JSObject) aModule.getMember("constructor");
            aName = JSType.toString(c.getMember("name"));
        }
        modulesInstances.put(aName, aModule);
    }

    public synchronized void unregisterModule(String aModuleName) {
        modulesInstances.remove(aModuleName);
    }

    public synchronized void unregisterModules() {
        modulesInstances.clear();
    }

    public synchronized Set<Map.Entry<String, JSObject>> getModulesEntries() {
        return Collections.unmodifiableSet(modulesInstances.entrySet());
    }

    /**
     * Returns this session's id.
     *
     * @return session id.
     */
    public String getId() {
        return sessionId;
    }

    public void setMaxInactiveInterval(int aInterval) {
        maxInactiveInterval = aInterval;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public boolean isNew() {
        return false;
    }

    @ScriptFunction(jsDoc = ""
            + "/**\n"
            + " * Contains modules collection of this session.\n"
            + " */")
    public JSObject getModules() {
        return jsModules;
    }

    @Override
    public void setPublished(JSObject aValue) {
        if (published != null) {
            throw new AlreadyPublishedException();
        }
        published = aValue;
    }

    @Override
    public JSObject getPublished() {
        if (published == null) {
            if (publisher == null || !publisher.isFunction()) {
                throw new NoPublisherException();
            }
            published = (JSObject) publisher.call(null, new Object[]{this});
        }
        return published;
    }

    private static JSObject publisher;

    public static void setPublisher(JSObject aPublisher) {
        publisher = aPublisher;
    }
}
