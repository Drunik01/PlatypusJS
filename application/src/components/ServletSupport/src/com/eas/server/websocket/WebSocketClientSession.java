/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server.websocket;

import com.eas.client.login.PlatypusPrincipal;
import com.eas.script.AlreadyPublishedException;
import com.eas.script.HasPublished;
import com.eas.script.NoPublisherException;
import com.eas.script.ScriptFunction;
import com.eas.script.ScriptObj;
import com.eas.script.ScriptUtils;
import java.net.URI;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import jdk.nashorn.api.scripting.JSObject;

/**
 *
 * @author mg
 */
@ScriptObj(name = "WebSocket")
public class WebSocketClientSession implements HasPublished {

    protected JSObject published;
    protected Session webSocketSession;
    protected final Object lock;
    protected final Object request;
    protected final Object reqponse;
    protected final Object platypusSession;
    protected final PlatypusPrincipal principal;
    //
    protected JsClientEndPoint endPoint = new JsClientEndPoint(this);

    @ScriptFunction(params = {"uri"})
    public WebSocketClientSession(String aUri) throws Exception {
        super();
        lock = ScriptUtils.getLock();
        if (lock == null) {
            throw new IllegalStateException("Web socket contructor must be called in valid context (Platypus Async IO attributes must present).");
        }
        request = ScriptUtils.getRequest();
        reqponse = ScriptUtils.getResponse();
        platypusSession = ScriptUtils.getSession();
        principal = PlatypusPrincipal.getInstance();

        webSocketSession = ContainerProvider.getWebSocketContainer().connectToServer(endPoint, URI.create(aUri));
    }

    public void inContext(Runnable anActor) {
        if (ScriptUtils.getLock() != null) {
            throw new IllegalStateException("Web socket callback must be called in clear context (from new thread or new pooled task).");
        }
        ScriptUtils.setLock(lock);
        ScriptUtils.setRequest(request);
        ScriptUtils.setResponse(reqponse);
        ScriptUtils.setSession(platypusSession);
        PlatypusPrincipal.setInstance(principal);
        try {
            synchronized (lock) {
                anActor.run();
            }
        } finally {
            ScriptUtils.setLock(null);
            ScriptUtils.setRequest(null);
            ScriptUtils.setResponse(null);
            ScriptUtils.setSession(null);
            PlatypusPrincipal.setInstance(null);
        }
    }

    @ScriptFunction
    public void close(Double opCode, String aReason) throws Exception {
        if (opCode == null) {
            opCode = Double.valueOf(CloseReason.CloseCodes.NO_STATUS_CODE.getCode());
        }
        if (aReason == null) {
            aReason = "";
        }
        if (webSocketSession.isOpen()) {
            webSocketSession.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(opCode.intValue()), aReason));
        }
    }

    @ScriptFunction(params = "data")
    public void send(String aData) {
        webSocketSession.getAsyncRemote().sendText(aData);
    }

    @ScriptFunction
    public String getId() {
        return webSocketSession.getId();
    }

    @ScriptFunction
    public String getProtocolVersion() {
        return webSocketSession.getProtocolVersion();
    }

    @ScriptFunction
    public String getQuery() {
        return webSocketSession.getQueryString();
    }

    @ScriptFunction
    public String getUri() {
        return webSocketSession.getRequestURI().toString();
    }

    @ScriptFunction
    public JSObject getOnopen() {
        return endPoint.getOnopen();
    }

    @ScriptFunction
    public void setOnopen(JSObject onopen) {
        endPoint.setOnopen(onopen);
    }

    @ScriptFunction
    public JSObject getOnclose() {
        return endPoint.getOnclose();
    }

    @ScriptFunction
    public void setOnclose(JSObject onclose) {
        endPoint.setOnclose(onclose);
    }

    @ScriptFunction
    public JSObject getOnerror() {
        return endPoint.getOnerror();
    }

    @ScriptFunction
    public void setOnerror(JSObject onerror) {
        endPoint.setOnerror(onerror);
    }

    @ScriptFunction
    public JSObject getOnmessage() {
        return endPoint.getOnmessage();
    }

    @ScriptFunction
    public void setOnmessage(JSObject onmessage) {
        endPoint.setOnmessage(onmessage);
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
