/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.login;

import com.eas.client.threetier.PlatypusConnection;
import com.eas.client.threetier.requests.LogoutRequest;
import com.eas.script.AlreadyPublishedException;
import com.eas.script.HasPublished;
import com.eas.script.NoPublisherException;
import com.eas.script.ScriptFunction;
import com.eas.script.ScriptUtils;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import jdk.nashorn.api.scripting.JSObject;

/**
 *
 * @author pk, mg, bl, vv
 */
public class PlatypusPrincipal implements Principal, HasPublished {

    protected JSObject published;

    private final String context;
    private final Set<String> roles;
    private final String name;
    private final PlatypusConnection conn;

    private static PlatypusPrincipal clientSpacePrincipal;

    public static PlatypusPrincipal getInstance() {
        return (PlatypusPrincipal) ScriptUtils.getPrincipal();
    }

    public static void setInstance(PlatypusPrincipal aValue) {
        ScriptUtils.setPrincipal(aValue);
    }

    public static PlatypusPrincipal getClientSpacePrincipal() {
        return clientSpacePrincipal;
    }

    public static void setClientSpacePrincipal(PlatypusPrincipal aValue) {
        clientSpacePrincipal = aValue;
    }

    public PlatypusPrincipal(String aUserName, String aContext, Set<String> aRoles, PlatypusConnection aConn) {
        super();
        name = aUserName;
        context = aContext;
        roles = aRoles;
        conn = aConn;
    }

    private static final String NAME_JS_DOC = "/**\n"
            + "* The username..\n"
            + "*/";

    @ScriptFunction(jsDoc = NAME_JS_DOC)
    @Override
    public String getName() {
        return name;
    }

    protected static final String HAS_ROLE_JS_DOC = ""
            + "/**\n"
            + "* Checks if a user have a specified role.\n"
            + "* @param role a role's name to test.\n"
            + "* @return <code>true</code> if the user has the role.\n"
            + "*/";

    @ScriptFunction(jsDoc = HAS_ROLE_JS_DOC)
    public boolean hasRole(String aRole) {
        return roles != null ? roles.contains(aRole) : true;
    }

    protected static final String LOGOUT_JS_DOC = ""
            + "/**\n"
            + " * Logs out from  user's session on a server.\n"
            + " * @param onSuccess The function to be invoked after the logout (optional).\n"
            + " * @param onFailure The function to be invoked when exception raised while logout process (optional).\n"
            + " */";

    @ScriptFunction(jsDoc = LOGOUT_JS_DOC, params = {"onSuccess", "onFailure"})
    public void logout(JSObject aOnSuccess, JSObject aOnFailure) throws Exception {
        LogoutRequest req = new LogoutRequest();
        if (aOnSuccess != null) {
            if (conn != null) {
                conn.enqueueRequest(req, (LogoutRequest.Response res) -> {
                    clientSpacePrincipal = new AnonymousPlatypusPrincipal();
                    aOnSuccess.call(null, new Object[]{});
                }, (Exception ex) -> {
                    if (aOnFailure != null) {
                        aOnFailure.call(null, new Object[]{ex.getMessage()});
                    }
                });
            } else {
                aOnSuccess.call(null, new Object[]{});
            }
        } else {
            if (conn != null) {
                conn.executeRequest(req);
                clientSpacePrincipal = new AnonymousPlatypusPrincipal();
            }
        }
    }

    public void logout(JSObject aOnSuccess) throws Exception {
        logout(aOnSuccess, null);
    }

    public void logout() throws Exception {
        logout(null, null);
    }

    public Set<String> getRoles() {
        return roles != null ? Collections.unmodifiableSet(roles) : null;
    }

    public String getContext() {
        return context;
    }

    public boolean hasAnyRole(Set<String> aRoles) {
        if (aRoles != null && !aRoles.isEmpty()) {
            return aRoles.stream().anyMatch((role) -> (hasRole(role)));
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "{username: \"" + name + "\"}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatypusPrincipal)) {
            return false;
        }
        PlatypusPrincipal that = (PlatypusPrincipal) o;
        if (this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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
