/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages active sessions.
 *
 * <p>
 * This class is responsible for tracking down active session, their creation
 * whenever needed, and removing.</p>
 *
 * @author pk, mg refactoring
 */
public class SessionManager {

    protected final Map<String, Session> sessions = new HashMap<>();

    /**
     * Creates a new session manager.
     */
    public SessionManager() {
        super();
        sessions.put(null, new Session(null));
    }

    /**
     * Creates a new session object for the specified user.
     *
     * <p>
     * The session instance returned is already registered inside this
     * manager.</p>
     *
     * <p>
     * It is assumed that by the time this method is called, the user already
     * authenticated successfully.</p>
     *
     * @param sessionId session id; use IDGenerator to generate.
     * @return a new Session instance.
     */
    public synchronized Session createSession(String sessionId) {
        assert sessionId != null;
        assert !sessions.containsKey(sessionId);
        Session result = new Session(sessionId);
        sessions.put(sessionId, result);
        return result;
    }

    public synchronized Session getOrCreateSession(String sessionId) {
        assert sessionId != null;
        if (!sessions.containsKey(sessionId)) {
            return createSession(sessionId);
        } else {
            return sessions.get(sessionId);
        }
    }

    public Session getSystemSession() {
        return sessions.get(null);
    }

    /**
     * Returns the session with given id.
     *
     * @param sessionId the session id
     * @return session instance, or null if no such session.
     */
    public synchronized Session get(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Removes specified session from manager.
     *
     * <p>
     * This method calls the <code>cleanup()</code> method of the session, so
     * nothing is needed else to close the session.</p>
     *
     * @param sessionId the session to remove.
     * @return instance removed, or null if no such session found.
     */
    public synchronized Session remove(String sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            removed.cleanup();
        }
        return removed;
    }

    /**
     * Returns a set of active sessions.
     *
     * <p>
     * However this method is synchronized, caller should always use the set
     * returned in a section synchronized with this class instance to protect it
     * from modifications made by other threads.</p>
     *
     * @return set of active sessions.
    public synchronized Set<Entry<String, Session>> entrySet() {
        return sessions.entrySet();
    }
     */
}
