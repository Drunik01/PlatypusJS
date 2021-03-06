/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server;

import com.eas.client.login.PlatypusPrincipal;
import com.eas.client.threetier.Request;
import com.eas.client.threetier.Response;
import com.eas.script.ScriptUtils;
import java.util.function.Consumer;

/**
 *
 * @param <T>
 * @author pk, mg refactoring
 * @param <R>
 */
public abstract class SessionRequestHandler<T extends Request, R extends Response> extends RequestHandler<T, R> {

    public SessionRequestHandler(PlatypusServerCore aServerCore, T aRequest) {
        super(aServerCore, aRequest);
    }

    public SessionManager getSessionManager() {
        assert serverCore != null;
        return serverCore.getSessionManager();
    }

    public void handle(Session aSession, PlatypusPrincipal aPrincipal, Consumer<R> onSuccess, Consumer<Exception> onFailure) {
        if (aSession == null) {
            if (onFailure != null) {
                onFailure.accept(new UnauthorizedRequestException("Unauthorized. Login first."));
            }
        } else {
            aSession.accessed();
            assert PlatypusPrincipal.getInstance() == null : "Principal must be null before session request handler is invoked.";
            assert ScriptUtils.getSession() == null : "Session must be null before session request handler is invoked.";
            PlatypusPrincipal.setInstance(aPrincipal);
            ScriptUtils.setSession(aSession);
            try {
                handle2(aSession, onSuccess, onFailure);
            } finally {
                PlatypusPrincipal.setInstance(null);
                ScriptUtils.setSession(null);
            }
        }
    }

    protected abstract void handle2(Session aSession, Consumer<R> onSuccess, Consumer<Exception> onFailure);
}
