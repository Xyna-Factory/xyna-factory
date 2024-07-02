/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xact.filter.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.session.FQName.XmomVersion;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.filter.session.repair.XMOMRepair;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;

import xmcp.processmodeller.datatypes.RepairEntry;
import xmcp.processmodeller.datatypes.RepairsRequiredError;
import xmcp.processmodeller.datatypes.response.GetXMOMItemResponse;

/**
 * XMOMGui behandelt die meisten Anfragen der Gui für den neuen H5-Modeler.
 * Dabei werden alle Sessions verwaltet, um unterschiedliche Einstellungen 
 * und Bearbeitungsstände für jeden Benutzer zu ermöglichen.
 */
public class XMOMGui {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XMOMGui.class);
  private static final SessionManagement sessionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
  
  private static Map<String, SessionBasedData> sessionBasedData = new ConcurrentHashMap<>();
  private XMOMLoader xmomLoader; //gemeinsamer Cache, Änderungen werden differentiell eingepflegt
  
  public XMOMGui() throws XynaException {
    super();
    xmomLoader = new XMOMLoader();
  }  
 
  public XMOMGuiReply processRequest(XmomGuiSession session, XMOMGuiRequest request) throws XynaException {
    if( request.getJson() != null && request.getJson().length() == 0 ) {
      return XMOMGuiReply.fail(Status.badRequest, "Empty request");
    }

    List<RepairEntry> repairResult = new ArrayList<RepairEntry>();

    SessionBasedData responsibleSession = getOrCreateSessionBasedData(session);
    if( (request.hasFQName()) && (request.getOperation() != Operation.Upload) &&
                                 (request.getOperation() != Operation.Close) &&
                                 (request.getOperation() != Operation.Refactor) &&
                                 (request.getOperation() != Operation.OrderInputSources) &&
                                 (request.getOperation() != Operation.ViewXml) &&
                                 (request.getOperation() != Operation.Session) &&
                                 (request.getOperation() != Operation.DeleteDocument) ) {
      if ( request.getFQName().getXmomVersion() == XmomVersion.DEPLOYED ||
           !responsibleSession.hasObject(request.getFQName()) ) {
        FQName fqName = request.getFQName();
        GenerationBaseObject gbo = null;
        try {
          boolean force = request.getBooleanParamter("repair", false);

          gbo = load(fqName, force, repairResult);
          
          if(!force && repairResult != null && repairResult.size() > 0) {
            RepairsRequiredError error = new RepairsRequiredError();
            error.setErrorCode(Status.conflict.toString());
            error.setRepairs(repairResult);
            error.setMessage("Repairs Required.");
            XMOMGuiReply reply = new XMOMGuiReply();
            reply.setStatus(Status.conflict);
            reply.setXynaObject(error);
            return reply;
          }
          
          if(gbo.getViewType() != null && gbo.getViewType().isMultiView() && gbo.getViewType() != request.getType()) {
            return XMOMGuiReply.fail(Status.forbidden, request.getType() + " is already open.");
          }
          gbo.setViewType(request.getType());
        } catch( Ex_FileAccessException e ) {
          return XMOMGuiReply.fail(Status.notfound, e );
        }

        responsibleSession.add(gbo);
      }
    }

    XMOMGuiReply reply = responsibleSession.processRequest(request);
    
    if(reply.getXynaObject() instanceof GetXMOMItemResponse) {
      GetXMOMItemResponse o = ((GetXMOMItemResponse)reply.getXynaObject());
      o.setRepairResult(repairResult);
    }
    
    return reply;
  }
  
  public GenerationBaseObject createNewObject(ObjectIdentifierJson object) throws XPRC_InvalidPackageNameException, XFMG_NoSuchRevision {
    return xmomLoader.createNewObject(object);
  }
  

  //load and getRepairEntries/repair
  public GenerationBaseObject load(FQName fqName, boolean force, List<RepairEntry> repairResult) throws XynaException {
    GenerationBaseObject gbo = xmomLoader.load(fqName, false);
    XMOMRepair repair = new XMOMRepair();

    if (force) {
      repairResult.addAll(repair.repair(gbo));
    } else {
      repairResult.addAll(repair.getRepairEntries(gbo));
    }
    return gbo;
  }
  
  //load without repair
  public GenerationBaseObject load(FQName fqName)  throws XynaException{
    return xmomLoader.load(fqName, false);
  }
  
  public GenerationBaseObject load(FQName fqName, String xml) throws XynaException {
    return xmomLoader.load(fqName, xml);
  }

  public void killSession(XmomGuiSession session) {
    killSession(session.getId());
  }
  
  public synchronized void killSession(String session) {
    SessionBasedData sbd = sessionBasedData.get(session);
    if(sbd != null)
      sbd.clean();
    sessionManagement.removeSessionTerminationHandler(session, this::killSession);
    sessionBasedData.remove(session);
  }

  public void prepareModification(GenerationBaseObject gbo) {
    xmomLoader.prepareModification(gbo);
  }
  
  public synchronized void quitSessionsForAllKnownLogins() {
    List<String> sessions = new ArrayList<>(sessionBasedData.size());
    sessionBasedData.keySet().forEach(sessions::add);
    for (String session : sessions) {
      try {
        sessionManagement.quitSession(session);
      } catch (PersistenceLayerException e) {
        Utils.logError(e);
      }
    }
  }
  
  public synchronized SessionBasedData getOrCreateSessionBasedData(XmomGuiSession session) {
    
    SessionBasedData sbd = sessionBasedData.get(session.getId());
    if(sbd == null) {
      sbd = createNewSessionBasedData(session);
    }
    return sbd;
  }
  
  public synchronized SessionBasedData getSessionBasedData(String sessionId) {
    return sessionBasedData.get(sessionId);
  }
  
  private synchronized SessionBasedData createNewSessionBasedData(XmomGuiSession session) {
    
    SessionBasedData sbd = new SessionBasedData(session, this);
    sbd.init();
    sessionManagement.addSessionTerminationHandler(session.getId(), this::killSession);
    sessionBasedData.put(session.getId(), sbd);
    return sbd;
  }
  
  
  public static synchronized Map<FQName, GenerationBaseObject> getAllOpenGbos(){
    Map<FQName, GenerationBaseObject> result = new HashMap<>();
    sessionBasedData.forEach((sessionId, data) ->
      result.putAll(data.getGbos())
    );
    return Collections.unmodifiableMap(result);
  }

  
  public XMOMLoader getXmomLoader() {
    return xmomLoader;
  }
}
