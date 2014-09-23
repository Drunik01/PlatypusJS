/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.server.handlers;

import com.eas.client.AppElementFiles;
import com.eas.client.ModuleStructure;
import com.eas.client.cache.PlatypusFiles;
import com.eas.client.scripts.ScriptDocument;
import com.eas.client.settings.SettingsConstants;
import com.eas.client.threetier.requests.ModuleStructureRequest;
import com.eas.server.PlatypusServerCore;
import com.eas.server.Session;
import com.eas.util.FileUtils;
import java.io.File;
import java.security.AccessControlException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mg
 */
public class ModuleStructureRequestHandler extends SessionRequestHandler<ModuleStructureRequest, ModuleStructureRequest.Response> {

    public static final String ACCESS_DENIED_MSG = "Access denied to application element '%s' [ %s ] for user %s";

    public ModuleStructureRequestHandler(PlatypusServerCore aServerCore, ModuleStructureRequest aRequest) {
        super(aServerCore, aRequest);
    }

    @Override
    public void handle2(Session aSession, Consumer<ModuleStructureRequest.Response> onSuccess, Consumer<Exception> onFailure) {
        try {
            // Security check
            checkModuleRoles(aSession, serverCore.getIndexer().nameToFiles(getRequest().getModuleOrResourceName()));
            // Actual work
            serverCore.getModules().getModule(getRequest().getModuleOrResourceName(), (ModuleStructure aStructure) -> {
                ModuleStructureRequest.Response resp = new ModuleStructureRequest.Response();
                if (aStructure.getParts().getFiles().size() > 1) {// If there is a single file, the request is about a plain resource
                    String localPath = serverCore.getModules().getLocalPath();
                    for (File f : aStructure.getParts().getFiles()) {
                        String resourceName = f.getPath().substring(localPath.length());
                        resourceName = resourceName.replace("\\", "/");
                        resp.getStructure().add(resourceName);
                    }
                    resp.getClientDependencies().addAll(aStructure.getClientDependencies());
                    resp.getServerDependencies().addAll(aStructure.getServerDependencies());
                    resp.getQueryDependencies().addAll(aStructure.getQueryDependencies());
                }
                onSuccess.accept(resp);
            }, onFailure);
        } catch (Exception ex) {
            Logger.getLogger(ModuleStructureRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
            onFailure.accept(ex);
        }
    }

    private void checkModuleRoles(Session aSession, AppElementFiles aAppElementFiles) throws Exception {
        if (aAppElementFiles.hasExtension(PlatypusFiles.JAVASCRIPT_EXTENSION)) {
            File jsFile = aAppElementFiles.findFileByExtension(PlatypusFiles.JAVASCRIPT_EXTENSION);
            String jsContent = FileUtils.readString(jsFile, SettingsConstants.COMMON_ENCODING);
            ScriptDocument jsDoc = ScriptDocument.parse(jsContent);
            Set<String> rolesAllowed = jsDoc.getModuleAllowedRoles();
            if (rolesAllowed != null && !aSession.getPrincipal().hasAnyRole(rolesAllowed)) {
                throw new AccessControlException(String.format(ACCESS_DENIED_MSG, jsFile.getName(), getRequest().getModuleOrResourceName(), aSession.getPrincipal().getName()));
            }
        }
    }
}
