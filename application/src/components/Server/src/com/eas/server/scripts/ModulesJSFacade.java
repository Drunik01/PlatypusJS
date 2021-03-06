/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server.scripts;

import com.eas.server.Session;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.Undefined;

/**
 *
 * @author mg
 */
public class ModulesJSFacade extends AbstractJSObject {

    protected static final String RESIDENT_MODULES_MODIFICATION_MSG = "Resident modules modification is not allowed.";
    protected Session session;

    public ModulesJSFacade(Session aSession) {
        super();
        session = aSession;
    }

    @Override
    public synchronized Object getMember(String name) {
        return session.getModule(name);
    }

    @Override
    public synchronized void setMember(String name, Object value) {
        value = value instanceof ScriptObject ? jdk.nashorn.api.scripting.ScriptUtils.wrap((ScriptObject)value) : value;
        if (value instanceof JSObject) {
            if (session.getId() == null) {
                throw new IllegalStateException(RESIDENT_MODULES_MODIFICATION_MSG);
            } else {
                session.registerModule(name, (JSObject) value);
            }
        } else if (value == null || value instanceof Undefined) {
            session.unregisterModule(name);
        }
    }

    @Override
    public synchronized void removeMember(String name) {
        if (session.getId() == null) {
            throw new IllegalStateException(RESIDENT_MODULES_MODIFICATION_MSG);
        }
        session.unregisterModule(name);
    }

    @Override
    public synchronized boolean hasMember(String name) {
        return session.containsModule(name);
    }

    @Override
    public synchronized Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        session.getModulesEntries().stream().forEach((Map.Entry<String, JSObject> aEntry) -> {
            keys.add(aEntry.getKey());
        });
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public synchronized Collection<Object> values() {
        Set<Object> values = new HashSet<>();
        session.getModulesEntries().stream().forEach((Map.Entry<String, JSObject> aEntry) -> {
            values.add(aEntry.getValue());
        });
        return Collections.unmodifiableSet(values);
    }

}
