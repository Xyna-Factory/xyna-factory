/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonStringVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.CloseJson;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.replace.ReplaceProcessor;
import com.gip.xyna.xact.filter.replace.ReplaceProcessor.ReplaceResult;
import com.gip.xyna.xact.filter.json.PersistJson;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.View.ViewWrapperJson;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.session.exceptions.DocumentLockedException;
import com.gip.xyna.xact.filter.session.exceptions.DocumentNotOpenException;
import com.gip.xyna.xact.filter.session.exceptions.InvalidRevisionException;
import com.gip.xyna.xact.filter.session.exceptions.LockUnlockException;
import com.gip.xyna.xact.filter.session.exceptions.MergeConflictException;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectIdPrefix;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.references.Reference;
import com.gip.xyna.xact.filter.session.gb.references.ReferenceType;
import com.gip.xyna.xact.filter.session.messagebus.PollEventFetcher;
import com.gip.xyna.xact.filter.session.messagebus.events.XmomTypeEvent;
import com.gip.xyna.xact.filter.session.messagebus.events.XmomTypeEvent.XmomType;
import com.gip.xyna.xact.filter.session.modify.operations.CopyOperation;
import com.gip.xyna.xact.filter.session.modify.operations.copy.StepCopier;
import com.gip.xyna.xact.filter.session.repair.XMOMRepair;
import com.gip.xyna.xact.filter.session.save.Persistence;
import com.gip.xyna.xact.filter.session.workflowissues.WorkflowIssuesRequestProcessor;
import com.gip.xyna.xact.filter.session.workflowwarnings.DefaultWorkflowWarningsHandler;
import com.gip.xyna.xact.filter.session.workflowwarnings.WorkflowWarningsHandler;
import com.gip.xyna.xact.filter.util.ReadonlyUtil;
import com.gip.xyna.xact.filter.xmom.session.json.GboJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.DataflowJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.LabelInputOutputJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.WorkflowStepVisitor;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.OperationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface.AvariableNotResolvableException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_FileExistsException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FactoryManagedRevisionXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Utils;

import xmcp.processmodeller.datatypes.ClipboardEntry;
import xmcp.processmodeller.datatypes.Connection;
import xmcp.processmodeller.datatypes.Issue;
import xmcp.processmodeller.datatypes.ModellingItem;
import xmcp.processmodeller.datatypes.OrderInputSource;
import xmcp.processmodeller.datatypes.RuntimeContext;
import xmcp.processmodeller.datatypes.XMOMItemInvalidatedHint;
import xmcp.processmodeller.datatypes.datatypemodeller.ServiceReferenceCandidates;
import xmcp.processmodeller.datatypes.datatypemodeller.XMOMItemReference;
import xmcp.processmodeller.datatypes.response.CloseResponse;
import xmcp.processmodeller.datatypes.response.DeleteResponse;
import xmcp.processmodeller.datatypes.response.FactoryItem;
import xmcp.processmodeller.datatypes.response.GetClipboardResponse;
import xmcp.processmodeller.datatypes.response.GetDataflowResponse;
import xmcp.processmodeller.datatypes.response.GetIssuesResponse;
import xmcp.processmodeller.datatypes.response.GetObjectXMLResponse;
import xmcp.processmodeller.datatypes.response.GetOrderInputSourcesResponse;
import xmcp.processmodeller.datatypes.response.GetRelationsResponse;
import xmcp.processmodeller.datatypes.response.GetWarningsResponse;
import xmcp.processmodeller.datatypes.response.GetXMLResponse;
import xmcp.processmodeller.datatypes.response.RefactorResponse;
import xmcp.processmodeller.datatypes.response.UnlockResponse;
import xmcp.xact.modeller.Hint;
import xmcp.yggdrasil.Event;
import xmcp.yggdrasil.GetEventsResponse;
import xmcp.yggdrasil.UnsubscribeProjectPollEventsResponse;
import xmcp.yggdrasil.Message;
import xmcp.yggdrasil.SubscribeProjectPollEventsResponse;

/**
 * Speichert alle Daten einer Session:
 * View: Anzeige-Informationen zu Workflows
 * Modification: aktuell modifizierte Workflows
 */
public class SessionBasedData {

  public static final XynaPropertyInt UNDO_LIMIT = new XynaPropertyInt("xyna.processmodeller.undo.limit", 50).
      setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen der Undo-Historie.").
      setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of entries in the undo history.");
  public static final XynaPropertyInt REDO_LIMIT = new XynaPropertyInt("xyna.processmodeller.redo.limit", 50).
      setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen der Redo-Historie.").
      setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of entries in the redo history.");

  public static final XynaPropertyInt CLIENT_POLLING_TIMEOUT_FACTOR = new XynaPropertyInt("xyna.messagebus.modeller.clientpollingtimeoutfactor", 10).
      setDefaultDocumentation(DocumentationLanguage.DE, "Faktor, der mit xyna.messagebus.request.timeout.millis multipliziert die Zeit ergibt, nach der spätestens ein neuer Polling-Request eines GUI-Tabs folgen muss, um nicht als verwaist gelöscht zu werden.").
      setDefaultDocumentation(DocumentationLanguage.EN, "Factor that gives, multiplied by xyna.messagebus.request.timeout.millis, the maximum amount of time allowed between two polling requests for a GUI tab. Exceeding the limit leads to the tab being treated as orphaned.");

  public static final long MESSAGE_BUS_UPDATE_SLEEP_TIME = 100;

  private static final long ORPHANED_POLL_REQUEST_CLEAN_INTERVAL = 1000L; // time in ms between checks for pollings for orphaned GUI tabs

  private static final long LOCK_UNLOCK_WAIT_TIME = 100; // time in ms to wait repeatetly if a lock/unlock doesn't react immediately
  private static final long LOCK_UNLOCK_WAIT_TIME_LIMIT = 20000; // max time in ms to wait for lock/unlock

  private static final Logger logger = CentralFactoryLogging.getLogger(SessionBasedData.class);
  
  private static final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
  private static final LockManagement lockMgmt = XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement();
  
  private Map<FQName, GenerationBaseObject> gbos;
  private Map<FQName, WorkflowWarningsHandler> warningHandlers;
  private Map<FQName, Modification> modifications; // Die Map wird nur noch in der Methode upload(XMOMGuiRequest) verwendet.
  private XMOMGui xmomGui;
  private final XmomGuiSession session;
  private ConcurrentHashMap<XmomType, XmomTypeEvent> locks;
  private HashMap<FQName, LinkedList<XMOMHistoryItem>> xmomUndoHistory;
  private HashMap<FQName, LinkedList<XMOMHistoryItem>> xmomRedoHistory;
  private HashMap<FQName, Long> autosaveCount;
  private Clipboard clipboard;

  private final Map<String, List<Event>> pollRequestUUIDToEventsMap = new ConcurrentHashMap<>();
  private final Map<String, Boolean> pollRequestUUIDToIsProject = new ConcurrentHashMap<>();
  private final Map<String, Long> pollRequestUUIDToLastPoll = new ConcurrentHashMap<>();
  private final AtomicLong pollRequestId = new AtomicLong();
  private final List<Long> pendingPollRequestIds = Collections.synchronizedList(new ArrayList<>());
  private volatile boolean orphanCleanerShouldRun = true;
  private volatile boolean shutdownInProgress = false;
  private volatile boolean pollRequestTerminationPending = false;
  private PollEventFetcher pollEventFetcher;


  public SessionBasedData(XmomGuiSession session, XMOMGui xmomGui) {
    this.gbos = new HashMap<>();
    this.modifications = new HashMap<>();
    this.xmomGui = xmomGui;
    this.session = session;
    this.locks = new ConcurrentHashMap<>();
    this.xmomUndoHistory = new HashMap<>();
    this.xmomRedoHistory = new HashMap<>();
    this.autosaveCount = new HashMap<>();
    this.clipboard = new Clipboard();
    this.warningHandlers = new HashMap<>();
  }

  public void init() {
    pollEventFetcher = new PollEventFetcher(session.getId(), pollRequestUUIDToEventsMap, pollRequestUUIDToIsProject, locks, this); // TODO: Geht das auch im Konstruktor?

    Thread orphanedPollRequestCleaner = new Thread("Orphaned poll-request cleaner") {
      @Override
      public void run() {
        while (orphanCleanerShouldRun) {
          try {
            long maxWaitTime = XynaProperty.MESSAGE_BUS_FETCH_TIMEOUT.getMillis() * CLIENT_POLLING_TIMEOUT_FACTOR.get();
            List<String> orphanedUUIDs = new ArrayList<>();

            synchronized (pollRequestUUIDToLastPoll) {
              for (Entry<String, Long> lastPoll : pollRequestUUIDToLastPoll.entrySet()) {
                if (System.currentTimeMillis() > (lastPoll.getValue() + maxWaitTime)) {
                  orphanedUUIDs.add(lastPoll.getKey());
                  pollRequestUUIDToEventsMap.remove(lastPoll.getKey());
                  pollRequestUUIDToIsProject.remove(lastPoll.getKey());
                }
              }

              for (String orphanedUUID : orphanedUUIDs) {
                pollRequestUUIDToLastPoll.remove(orphanedUUID);
              }
            }

            try {
              Thread.sleep(ORPHANED_POLL_REQUEST_CLEAN_INTERVAL);
            } catch (Exception e) {
              logger.error("Multiuser: Cleanup thread for orphaned polling requests for session " + session.getId() + " died", e);
              break;
            }
          } catch (OutOfMemoryError t) {
            Department.handleThrowable(t);
          }
        }
      }
    };

    orphanedPollRequestCleaner.setDaemon(true);
    orphanedPollRequestCleaner.start();
  }
  
  public void clean() {
    shutdownInProgress = true;
    pollEventFetcher.stop();
    locks.clear();
    orphanCleanerShouldRun = false;
  }
  
  
  public XmomGuiSession getSession() {
    return session;
  }
  
  public boolean hasObject(FQName fqName) {
    return gbos.containsKey(fqName);
  }


  public void add(GenerationBaseObject gbo) {
    gbos.put(gbo.getFQName(), gbo);
    gbo.setWarningsHandler(getOrCreateWFWarningsHandler(gbo.getFQName()));
    pollEventFetcher.documentOpened(gbo);
  }
  
  public void removeFromGboMap(FQName fqName) {
    gbos.remove(fqName);
  }
  
  public XMOMGuiReply processRequest(XMOMGuiRequest request) {
    XMOMGuiReply reply = createXMOMGuiReply(request);
    if(reply.getXynaObject() != null) {
      ReadonlyUtil.setReadonlyRecursive(reply);
    }
    return reply;
  }
  
  private XMOMGuiReply createXMOMGuiReply(XMOMGuiRequest request) {
    try {
      switch( request.getOperation() ) {
      case Create:
        return createNew(request);
      case DataflowSaved:
      case DataflowDeployed:
        return dataflow(request);
      case Relations:
        return getRelations(request);
      case Upload:
        return upload(request);
      case Save:
        return save(request);
      case Deploy:
        return deploy(request);
      case Refactor:
        return refactor(request);
      case Replace:
        return replace(request);
      case DeleteDocument:
        return deleteDocument(request);
      case Close:
        return close(request);
      case Unlock:
        return unlock(request, true);
      case ViewSaved:
      case ViewDeployed:
        return view(request);
      case Session:
        return session(request);
      case OrderInputSources:
        return orderInputSources(request);
      case Undo:
        return undo(request);
      case Redo:
        return redo(request);
      case ReferenceCandidates:
        return referenceCandidates(request);
      case ViewXml:
        return viewXml(request);
      case Issues:
        return showIssues(request);
      case ShowClipboard:
        return showClipboard();
      case ClearClipboard:
        return clearClipboard();
      case GetPollEvents:
        return getPollEvents(request.getObjectId(), false);
      case GetProjectPollEvents:
        return getPollEvents(request.getObjectId(), true);
      case SubscribeProjectPollEvents:
        return subscribeProjectPollEvents(request);
      case UnsubscribeProjectPollEvents:
        return unsubscribeProjectPollEvents(request);
      case CopyXml:
        return copyXml(request);
      case Warnings:
         return getWarnings(request);
      case ModelledExpressions:
        return getModelledExpressions(request);
      default:
        if( request.getOperation().isModification() ) {
          return objectModification(request);
        } else {
          return XMOMGuiReply.fail(Status.failed, "Unhandled operation "+request.getOperation());
        }
      }
    } catch( InvalidJSONException | UnexpectedJSONContentException | InvalidRevisionException | UnknownObjectIdException e ) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
      return XMOMGuiReply.fail(Status.badRequest, e);
    } catch( MissingObjectException e ) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
      return XMOMGuiReply.fail(Status.notfound, e);
    } catch( XPRC_FileExistsException e ) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
      return XMOMGuiReply.fail(Status.conflict, e);
    } catch( Exception e ) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
      return XMOMGuiReply.fail(Status.failed, e);
    }
  }


  private XMOMGuiReply getModelledExpressions(XMOMGuiRequest request) {
    FQName fqn = request.getFQName();
    GenerationBaseObject gbo = gbos.get(fqn);
    XMOMGuiReply reply = new XMOMGuiReply();
    ModelledExpressionConverter converter = new ModelledExpressionConverter();
    GetModelledExpressionsResponse response = converter.convert(gbo, request.getObjectId());
    reply.setXynaObject(response);
    return reply;
  }

  private XMOMGuiReply getWarnings(XMOMGuiRequest request) {
    FQName fqn = request.getFQName();
    WorkflowWarningsHandler handler = getOrCreateWFWarningsHandler(fqn);
    GenerationBaseObject gbo = gbos.get(fqn);
    XMOMGuiReply reply = new XMOMGuiReply();
    GetWarningsResponse response = handler.createWarningsResponse(gbo);
    reply.setXynaObject(response);

    return reply;
  }
  
  public WorkflowWarningsHandler getWFWarningsHandler(FQName fqn) {
    return getOrCreateWFWarningsHandler(fqn);
  }

  private WorkflowWarningsHandler getOrCreateWFWarningsHandler(FQName fqn) {
    if (warningHandlers.get(fqn) == null) {
      warningHandlers.put(fqn, new DefaultWorkflowWarningsHandler());
    }

    return warningHandlers.get(fqn);
  }


  //does not work with objects from ClipBoard yet -> fromGBO
  private XMOMGuiReply copyXml(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    GetObjectXMLResponse response = new GetObjectXMLResponse();

    GenerationBaseObject fromGBO = gbos.get(request.getFQName());

    GBSubObject object;
    WF wf;
    try {
      object = fromGBO.getObject(request.getObjectId());
      wf = WF.createNewWorkflow("tmp.copyWF", new GenerationBaseCache(), -15l);
    } catch (UnknownObjectIdException | MissingObjectException | XynaException e) {
      throw new RuntimeException(e);
    }

    FQName fqName = new FQName(-1l, null, "tmp", "clipboardWF");
    GenerationBaseObject gbo = new GenerationBaseObject(fqName, wf, object.getRoot().getXmomLoader(), true);
    gbo.createDataflow();
    wf.getWfAsStep().getChildStep().getChildSteps().clear(); //remove stepAssign

    if(object.getType() == ObjectType.step) {
      StepCopier.copyStepIntoGenerationBaseObject(gbo, object);
    } else if (object.getType() == ObjectType.variable) {
      AVariable orgVar = object.getVariable().getVariable().getIdentifiedVariable();
      AVariable varCpy = CopyOperation.createCopyOfVariable(orgVar, wf);
      wf.getWfAsStep().addInput(varCpy);
    } else {
      throw new RuntimeException("Can't copy '" + object.getType() + "' to system clipboard.");
    }
    com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType persistType =
        new com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType("tmp", "cpy", "cpy");
    
    String xml = Persistence.createWorkflowXML(wf, persistType);
    response.setXml(xml);
    reply.setXynaObject(response);
    return reply;
  }

  private XMOMGuiReply showIssues(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    GetIssuesResponse issues = new GetIssuesResponse();
    FQName fqName = request.getFQName();
    GenerationBaseObject gbo = gbos.get(fqName);
    
    if(gbo == null) {
      return XMOMGuiReply.fail(Status.badRequest, new RuntimeException("Workflow not open"));
    }
    
    try {
      List<Issue> entries = WorkflowIssuesRequestProcessor.getIssuesOfWorkflow(gbo, gbo.getXmomLoader() );
      issues.setIssues(entries);
      reply.setStatus(Status.success);
    } catch (Exception e) {
      reply.setStatus(Status.success); //TODO:
      List<Hint> hints = new ArrayList<Hint>();
      Hint hint = new Hint.Builder().description("Exception during issue calculation.").instance();
      hints.add(hint);
      issues.setHints(hints);
    }
    
    reply.setXynaObject(issues);
    
    return reply;
  }

  private XMOMGuiReply showClipboard() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);

    List<ClipboardEntry> clipboardEntries = new ArrayList<>();
    for (Pair<Integer, Clipboard.ClipboardEntry> clipboardEntry : clipboard.getEntries()) {
      Clipboard.ClipboardEntry cEntry = clipboardEntry.getSecond();
      GBSubObject entry = cEntry.getObject();
      Long revision = cEntry.getRevision();
      String originalFqn = cEntry.getFqn();
      RuntimeContext originalRtc = revision != null ? com.gip.xyna.xact.filter.util.Utils.getModellerRtc(revision) : null;

      ModellingItem item;
      switch (entry.getType()) {
        case step:
          WorkflowStepVisitor workflowStepVisitor = new WorkflowStepVisitor(new View(entry.getRoot()), entry.getStep());
          item = (ModellingItem)workflowStepVisitor.createWorkflowSteps().get(0);
          break;
        case variable:
          item = (ModellingItem)(new VariableJson(entry).getXoRepresentation());
          break;
        default: continue;
      }
      ObjectId clipboardId = ObjectId.createClipboardId(clipboardEntry.getFirst());
      item.setId(clipboardId.getObjectId());
      clipboardEntries.add(new ClipboardEntry(originalRtc, originalFqn, item));
    }

    GetClipboardResponse response = new GetClipboardResponse();
    response.setEntries(clipboardEntries);
    reply.setXynaObject(response);

    return reply;
  }

  private XMOMGuiReply clearClipboard() {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);

    GetClipboardResponse response = new GetClipboardResponse();
    response.setEntries(new ArrayList<>());
    reply.setXynaObject(response);

    clipboard.clear();

    return reply;
  }

  private XMOMGuiReply getPollEvents(String pollUuid, boolean projectEvents) throws InterruptedException {
    XMOMGuiReply reply;
    GetEventsResponse response;
    Long curPollRequestId;

    synchronized (pollRequestUUIDToLastPoll) {
      if (shutdownInProgress) {
        throw new InterruptedException();
      }

      pollRequestUUIDToLastPoll.put(pollUuid, System.currentTimeMillis());

      reply = new XMOMGuiReply();
      reply.setStatus(Status.success);
      response = new GetEventsResponse();
      reply.setXynaObject(response);

      if (!pollRequestUUIDToEventsMap.containsKey(pollUuid)) {
        pollRequestUUIDToEventsMap.put(pollUuid, Collections.synchronizedList(new ArrayList<>()));
        pollRequestUUIDToIsProject.put(pollUuid, projectEvents);
        refreshSubscriptions();
        response.setUpdates(new ArrayList<>());

        return reply;
      }

      curPollRequestId = pollRequestId.getAndIncrement();
      pendingPollRequestIds.add(curPollRequestId);
    }

    // wait for multiuser updates or response timeout
    long timeout = System.currentTimeMillis() + XynaProperty.MESSAGE_BUS_FETCH_TIMEOUT.getMillis();
    while (System.currentTimeMillis() < timeout) {
      if (shutdownInProgress) {
        throw new InterruptedException();
      }

      if (pollRequestTerminationPending) {
        // GUI has been reloaded -> terminate currently running poll request since GUI might not receive it, anymore. Pending events will be sent with next polling event
        pendingPollRequestIds.remove(curPollRequestId);
        response.setUpdates(new ArrayList<>());

        return reply;
      }

      if (!pollRequestUUIDToEventsMap.get(pollUuid).isEmpty()) {
        break;
      }

      Thread.sleep(MESSAGE_BUS_UPDATE_SLEEP_TIME);
    }

    synchronized (pollRequestUUIDToEventsMap) {
      response.setUpdates(new ArrayList<>(pollRequestUUIDToEventsMap.get(pollUuid)));
      pollRequestUUIDToEventsMap.get(pollUuid).clear();
      pendingPollRequestIds.remove(curPollRequestId);
    }

    return reply;
  }

  private XMOMGuiReply subscribeProjectPollEvents(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(new SubscribeProjectPollEventsResponse());

    Message message = (Message)com.gip.xyna.xact.filter.util.Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(request.getJson());
    pollEventFetcher.addProjectPolling(request.getObjectId(), message.getCorrelation(), message.getProduct(), message.getContext());
    terminatePollRequests();

    return reply;
  }

  private XMOMGuiReply unsubscribeProjectPollEvents(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(new UnsubscribeProjectPollEventsResponse());

    // the current browser tab of the user does not need updates for this subscription, anymore
    String pollUuid = request.getObjectId();
    synchronized (pollRequestUUIDToEventsMap) {
      pollRequestUUIDToEventsMap.remove(pollUuid);
      pollRequestUUIDToIsProject.remove(pollUuid);
    }

    Message message = (Message)com.gip.xyna.xact.filter.util.Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(request.getJson());
    pollEventFetcher.cancelProjectPolling(pollUuid, message.getCorrelation());
    terminatePollRequests();

    return reply;
  }

  private void refreshSubscriptions() {
    terminatePollRequests();

    for (Entry<FQName, GenerationBaseObject> gboEntry : gbos.entrySet()) {
      pollEventFetcher.documentClosed(gboEntry.getValue());
      pollEventFetcher.documentOpened(gboEntry.getValue());
    }
  }

  private void terminatePollRequests() {
    pollRequestTerminationPending = true;
    long timeout = System.currentTimeMillis() + 100*MESSAGE_BUS_UPDATE_SLEEP_TIME;
    while (System.currentTimeMillis() < timeout) {
      if (pendingPollRequestIds.isEmpty()) {
        break;
      }

      try {
        Thread.sleep(MESSAGE_BUS_UPDATE_SLEEP_TIME);
      } catch (Exception e) {
        logger.warn("Multiuser: Poll request termination has been interrupted", e);
      }
    }

    if (!pendingPollRequestIds.isEmpty()) {
      logger.warn("Multiuser: Failed to terminate running poll requests");
    }

    pollRequestTerminationPending = false;
  }

  private XMOMGuiReply viewXml(XMOMGuiRequest request) throws XynaException {
    if (!isAllowed(UserManagement.GuiRight.ZETA_PROCESS_MODELLER_SHOWXML.getKey())) {
      return XMOMGuiReply.fail(Status.forbidden, new XFMG_ACCESS_VIOLATION(UserManagement.GuiRight.ZETA_PROCESS_MODELLER_SHOWXML.getKey(), session.getRole().getName()));
    }

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    GetXMLResponse response = new GetXMLResponse();
    response.setCurrent(getCurrentXML(request));
    
    response.setDeploy(getDeployedXml(request.getFQName().getRevision(), request.getFQName().getFqName()));
    response.setSaved(getSavedXml(request.getFQName().getRevision(), request.getFQName().getFqName()));
    
    reply.setXynaObject(response);    
    return reply;
  }
  
  private String getDeployedXml(Long revision, String fqn) {
    return readXmlFile(GenerationBase.getFileLocationOfXmlNameForDeployment(fqn, revision) + ".xml");
  }
  
  private String getSavedXml(Long revision, String fqn) {
    return readXmlFile(GenerationBase.getFileLocationOfXmlNameForSaving(fqn, revision) + ".xml");
  }
  
  private String readXmlFile(String filename) {
    File file = new File(filename);
    if(file.exists() && file.canRead()) {
      try {
        return new String(Files.readAllBytes(Paths.get(filename)));
      } catch (IOException e) {
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }
    }
    return null;
  }
  
  private XMOMGuiReply orderInputSources(XMOMGuiRequest request) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    long rtcRevision = com.gip.xyna.xact.filter.util.Utils.getRtcRevision(request.getRuntimeContext());
    List<OrderInputSourceStorable> orderInputSources = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement().getOrderInputSourcesForRevision(rtcRevision);

    List<OrdertypeParameter> ordertypes = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement().listOrdertypes(rtcRevision);
    final List<OrdertypeParameter> matchingWorkflowOrderTypes = ordertypes.stream().filter(ot -> {
      if(ot.getExecutionDestinationValue() != null && ExecutionType.XYNA_FRACTAL_WORKFLOW == ot.getExecutionDestinationValue().getDestinationTypeEnum()) {
        return ot.getExecutionDestinationValue().getFullQualifiedName().equals(request.getFQName().getFqName());
      }
      return false;
    }).collect(Collectors.toList());
    
    List<OrderInputSourceStorable> matchingOrderInputSources = orderInputSources.stream().filter(ois -> {
      for (OrdertypeParameter ot : matchingWorkflowOrderTypes) {
        if(ot.getOrdertypeName().equals(ois.getOrderType())) {
          return true;
        }
      }
      return false;
    }).collect(Collectors.toList());
    GetOrderInputSourcesResponse response = new GetOrderInputSourcesResponse();
    response.setOrderInputSources(matchingOrderInputSources.stream().map(oiss -> {
      OrderInputSource ois = new OrderInputSource();
      ois.setId(String.valueOf(oiss.getId()));
      ois.setName(oiss.getName());
      return ois;
    }).collect(Collectors.toList()));
    reply.setXynaObject(response);
    return reply;
  }

  private XMOMGuiReply upload(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, XynaException {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    FQName fqName = request.getFQName();
    
    ViewWrapperJson upload = ViewWrapperJson.parse(request.getJson(), "data", new JsonStringVisitor() ); // TODO: remove wrapping
    
    //FIXME check: passt fqName zu upload.getIdentifier()?
    Modification mod = modifications.get(fqName);
    if( mod == null ) {
      GenerationBaseObject gbo = null;
      try {
        gbo = xmomGui.createNewObject(upload.getIdentifier() );
        add(gbo);
      } catch( Exception e ) {
        logger.warn( "Could not create "+ fqName.getFqName(), e ); //FIXME
        return XMOMGuiReply.fail(Status.failed, "Could not create "+ fqName.getFqName(), e );
      }
      mod = getOrCreateModification(fqName);
    }
    GenerationBaseObject gbo = gbos.get(request.getFQName());
    int revision = gbo.getRevision(); //FIXME woher Revision  upload.getMeta().getRevision()
    mod.upload( revision, upload.getContentAsString() );
    
    View view = gbos.get(request.getFQName()).getView();
    reply.setXynaObject(view.viewAll(request) );
    return reply;
  }
  
  private XMOMGuiReply save(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, XynaException, LockUnlockException {
    JsonParser jp = new JsonParser();
    PersistJson saveRequest = jp.parse(request.getJson(), PersistJson.getJsonVisitor());
    FQName oldFqn = request.getFQName();
    Modification mod = getOrCreateModification(oldFqn);

    boolean saveAs = (saveRequest.getLabel() != null && saveRequest.getPath() != null) // path and label != null
        && (!saveRequest.getLabel().isEmpty() && !saveRequest.getPath().isEmpty()) // path and label not empty
        && (
            (!saveRequest.getLabel().equals(mod.getObject().getGenerationBase().getLabel())) || // label != GBO label
            (!saveRequest.getPath().equals(mod.getObject().getGenerationBase().getOriginalPath())) // path != GBO path
        );

    if (!saveAs && isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }

    GenerationBaseObject gbo = mod.getObject();
    if (saveAs) {
      String javaName = Utils.labelToJavaName(saveRequest.getLabel(), true);
      if(javaName == null || javaName.isEmpty() || javaName.matches("[\\s]+")) {
        return XMOMGuiReply.fail(Status.policyNotFulfilled, "Unable to create Java class name from label " + saveRequest.getLabel());
      }
    }

    // for data types that are storables, add unique identifier of not already present
    if (gbo.getType() == XMOMType.DATATYPE) {
      try {
        addUniqueIdentifierIfNeeded(request);
      } catch (Exception e) {
        // in case of errors continue, saving storable without unique identifier
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }
    }

    String fqn = mod.save(saveRequest, request.getRevision());

    resetAutosaveCount(oldFqn);
    FQName newFqn;
    if (saveAs) {
      if (!isLockedNotByMe(oldFqn)) {
        publishXmomModification(oldFqn, -1L, null);
        unlock(oldFqn, false);
      }

      // reload document and update data structures
      newFqn = new FQName(oldFqn.getRevision(), fqn);
      replace(oldFqn, newFqn);
      mod = getOrCreateModification(newFqn);
      
      if(gbo.getType() == XMOMType.DATATYPE) {
        copyLibs(gbo, mod.getObject());
      }
      
    } else {
      newFqn = oldFqn;
      
      LinkedList<XMOMHistoryItem> undoHistoryItems = xmomUndoHistory.get(request.getFQName());
      if(undoHistoryItems != null) {
        undoHistoryItems.forEach(item -> setSavedAndModified(item));
      }
      
      LinkedList<XMOMHistoryItem> redoHistoryItems = xmomRedoHistory.get(request.getFQName());
      if(redoHistoryItems != null) {
        redoHistoryItems.forEach(item -> setSavedAndModified(item));
      }
    }

    publishXmomModification(newFqn, -1L, null);
    unlock(newFqn, false);

    getOrCreateWFWarningsHandler(oldFqn).deleteAllWarnings();
    gbos.get(newFqn).markAsSaved();
    mod.getObject().setViewType(request.getType()); // save requested type to distinguish whether a data type or a service group is to be shown

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(mod.getXoRepresentation());

    return reply;
  }
  

  private void setSavedAndModified(XMOMHistoryItem item) {
    if (item == null) {
      return;
    }
    item.setModified(true);
    item.setSaveState(true);
  }


  private void copyLibs(GenerationBaseObject source, GenerationBaseObject destination) throws XPRC_JarFileForServiceImplNotFoundException {
    Set<String> libNames = source.getDOM().getAdditionalLibraries();
    for (String libName : libNames) {
      String sourceFilePathAndName = DOM.getJarFileForServiceLocation(source.getFQName().getFqName(), source.getFQName().getRevision(), libName, true, new FactoryManagedRevisionXMLSource()).getPath();
      File sourceFile = new File(sourceFilePathAndName);
      if(!sourceFile.canRead()) {
        continue;
      }
      String destinationFolderPath = GenerationBase.getFileLocationOfServiceLibsForSaving(destination.getFQName().getFqName(), destination.getFQName().getRevision());
      File destinationFolder = new File(destinationFolderPath);
      if(!destinationFolder.exists()) {
        destinationFolder.mkdirs();
      }
      File destinationFile = new File(destinationFolderPath + "/" + sourceFile.getName());
      
      try(FileOutputStream fos = new FileOutputStream(destinationFile)){
        FileUtils.copyFile(sourceFile, destinationFile, true);
      } catch (IOException | Ex_FileAccessException e) {
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }
    }
  }

  private XMOMGuiReply deploy(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, XynaException {
    if (isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }

    JsonParser jp = new JsonParser();
    PersistJson deployRequest = jp.parse(request.getJson(), PersistJson.getJsonVisitor());

    Modification mod = getOrCreateModification(request.getFQName());
    mod.getObject().setViewType(request.getType()); // save requested type to distinguish whether a data type or a service group is to be shown
    mod.deploy(deployRequest, request.getRevision());

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(mod.getXoRepresentation());
    
    return reply;
  }

  private boolean addUniqueIdentifierIfNeeded(XMOMGuiRequest request) throws Exception {
    GenerationBaseObject gbo = gbos.get(request.getFQName());
    DOM dom = gbo.getDOM();
    if (dom == null || !dom.isInheritedFromStorable()) {
      // not a storable type
      return false;
    }

    List<AVariable> memberVars = dom.getAllMemberVarsIncludingInherited();
    for (AVariable memberVar : memberVars) {
      if (memberVar.getPersistenceTypes() != null && memberVar.getPersistenceTypes().contains(PersistenceTypeInformation.UNIQUE_IDENTIFIER)) {
        // storable already has a unique identifier
        return false;
      }
    }

    // unique identifier has to be added

    // add new variable
    createXmomUndoHistoryItem(request);
    String objectId = ObjectIdPrefix.memberVarArea.toString(); // request.getObjectId();
    Modification mod = getOrCreateModification(request.getFQName());
    mod.modify(objectId, com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation.Insert, "{\"index\":-1, \"content\":{\"type\":\"memberVar\", \"label\":\"Unique Identifier\"}, \"revision\": " + request.getRevision() + "}");

    // make new variable the unique identifier
    objectId = ObjectIdPrefix.memberVar.toString() + memberVars.size();
    mod.modify(objectId, com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation.Change, "{\"primitiveType\":\"long\",\"revision\": " + request.getRevision() + "}");
    mod.modify(objectId, com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation.Change, "{\"storableRole\":\"uniqueIdentifier\",\"revision\": " + request.getRevision() + "}");

    gbo.incrementRevision();

    return true;
  }

  private XMOMGuiReply replace(XMOMGuiRequest request) throws Exception {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    XMOMGuiReply reply = new XMOMGuiReply();
    try {
      JsonParser jp = new JsonParser();
      PersistJson refactorRequest = jp.parse(request.getJson(), PersistJson.getJsonVisitor());
      Long rev = rm.getRevision(request.getRuntimeContext());
      
      String newFqn = refactorRequest.getPath() + "." + Utils.labelToJavaName(refactorRequest.getLabel(), true);
      String rmvFqn = request.getFQName().getFqName();

      ReplaceProcessor processor = new ReplaceProcessor();
      List<ReplaceResult> result = processor.replace(rmvFqn, newFqn, rev, request.getRuntimeContext());
      RefactorResponse response = new RefactorResponse();
      int total = result.size();
      long successCount = result.stream().filter(x -> x.isSuccess()).count();
      long failCount = total-successCount;
      Hint hint = new Hint(String.format("Replaced %d occurences. Successes %d, Fails: %d", total, successCount, failCount));
      Hint hintSuccess = new Hint("Successes:\n" + String.join("\n", result.stream().filter(x -> x.isSuccess()).map(x -> x.getObjectFqn()).collect(Collectors.toList())));
      Hint hintFail = new Hint("Fails:\n" + String.join("\n", result.stream().filter(x -> !x.isSuccess()).map(x -> x.getObjectFqn()).collect(Collectors.toList())));
      response.unversionedSetHints(List.of(hint, hintSuccess, hintFail));
      reply.setXynaObject(response);
      reply.setStatus(Status.success);
    } catch (Exception e) {
      reply.setXynaObject(new RefactorResponse());
      reply.setStatus(Status.failed);
    }
    return reply;
  }
  


  private XMOMGuiReply refactor(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, XynaException, UnknownObjectIdException, MissingObjectException, DocumentLockedException {
    if(isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }
    JsonParser jp = new JsonParser();
    PersistJson refactorRequest = jp.parse(request.getJson(), PersistJson.getJsonVisitor());
    Modification mod = prepareModClosedDoc(request.getFQName());
    mod.refactor(refactorRequest, request.getRevision(), request.getObjectId());
    
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setXynaObject(new RefactorResponse());
    reply.setStatus(Status.success);
    
    return reply;
  }

  private XMOMGuiReply deleteDocument(XMOMGuiRequest request) throws DocumentLockedException, InvalidJSONException, UnexpectedJSONContentException, XynaException, UnknownObjectIdException, MissingObjectException, InvalidRevisionException {
    if(isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }
    
    XMOMGuiReply reply = new XMOMGuiReply();
    DeleteResponse deleteResponse = new DeleteResponse();
    reply.setXynaObject(deleteResponse);
    
    JsonParser jp = new JsonParser();
    PersistJson deleteRequest = jp.parse(request.getJson(), PersistJson.getJsonVisitor());
    
    //Hints
    GenerationBaseObject gbo = load(request.getFQName());
    ObjectType objectType = ObjectType.of(gbo.getType());
    if(ObjectType.datatype == objectType && gbo.getViewType() == Type.serviceGroup) {
      objectType = ObjectType.servicegroup;
    }
    GBSubObject sub = gbo.getObject(new ObjectId(objectType, null).getObjectId());
    
    List<Hint> hints = getDeleteHints(sub);
    if(deleteRequest.isForce()) {
      if(forceDeleteSavedMDM(request.getFQName().getFqName(), request.getRevision())) {
        deleteResponse.setHints(hints);
        reply.setStatus(Status.success);
      } else {
        reply.setStatus(Status.failed);
      }
    } else {
      Modification mod = prepareModClosedDoc(request.getFQName());
      mod.getObject().setViewType(request.getType()); // save requested type to distinguish whether a data type or a service group is to be shown
      mod.delete(deleteRequest, request.getRevision());
      deleteResponse.setHints(hints);
      reply.setStatus(Status.success);
    }
    return reply;
  }
  
  
  private List<Hint> getDeleteHints(GBSubObject sub) {
    List<Hint> result = new ArrayList<Hint>();
    List<Reference> references = null;
    try {
      references = sub.getReferences();
    } catch (XynaException e) {
      references = Collections.emptyList();
    }

    XMOMItemInvalidatedHint hint;
    List<FQName> addedFqNames = new ArrayList<FQName>();
    for (Reference ref : references) {

      //parent XMOMs are not invalidated
      //possessed XMOM are not invalidated
      if (ref.getReferenceType() == ReferenceType.extend || ref.getReferenceType() == ReferenceType.possesses) {
        continue;
      }
      
      //do not add the same fqName multiple times
      if(addedFqNames.stream().anyMatch(x -> x.equals(ref.getFqName()))) {
        continue;
      }
      
      addedFqNames.add(ref.getFqName());

      hint = new XMOMItemInvalidatedHint();
      hint.setDescription("XMOM Item was invalidated. (" + ref.getReferenceType() + ")");
      hint.setFqn(ref.getFqName().getFqName());
      result.add(hint);
    }

    return result;
  }
  
  
  private boolean forceDeleteSavedMDM(String fqn, Long revision) {
    boolean deleted = false;
    try {
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteWorkflow(fqn, false, true, false, true, revision);
      deleted = true;
    } catch (Exception ex) {
      com.gip.xyna.xact.filter.util.Utils.logError(ex);
    }
    XynaMultiChannelPortalSecurityLayer xmcpsl = (XynaMultiChannelPortalSecurityLayer)XynaFactory.getInstance().getXynaMultiChannelPortal();
    try {
      xmcpsl.deleteDatatype(fqn, false, true, revision);
      deleted = true;
    } catch (Exception ex) {
      com.gip.xyna.xact.filter.util.Utils.logError(ex);
    }
    try {
      xmcpsl.deleteException(fqn, false, true, revision);
      deleted = true;
    } catch (Exception ex) {
      com.gip.xyna.xact.filter.util.Utils.logError(ex);
    }
    return deleted;
  }

  private Modification prepareModClosedDoc(FQName fqName) throws DocumentLockedException, InvalidJSONException, UnexpectedJSONContentException, XynaException {
    String lockingUser = lockMgmt.getLockingUser(new Path(fqName.getFqName(), fqName.getRevision()));
    // the document must not be locked
    if (isLockedByMe(fqName)) {
      throw new DocumentLockedException(fqName);
    } else if (lockingUser != null) {
      throw new DocumentLockedException(fqName, lockingUser);
    }

    GenerationBaseObject gbo = load(fqName);
    Modification mod = new Modification(this, gbo);
    xmomGui.prepareModification(gbo);
    modifications.put(fqName, mod);

    return mod;
  }

  private XMOMGuiReply close(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, DocumentNotOpenException, LockUnlockException {
    JsonParser jp = new JsonParser();
    CloseJson closeRequest = jp.parse(request.getJson(), CloseJson.getJsonVisitor());
    FQName fqn = request.getFQName();
    GenerationBaseObject gbo = gbos.get(fqn);
    if (gbo == null) {
      throw new DocumentNotOpenException(fqn);
    }

    if (closeRequest.getRevision() != gbo.getRevision()) {
      // TODO: for now, send warning instead of exception to GUI (revision isn't essential until multiuser-feature is implemented)
//      throw new InvalidRevisionException(closeRequest.getRevision(), gbo.getRevision()); 
    }
    
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setXynaObject(new CloseResponse());

    if (!closeRequest.isForce() && gbo.hasBeenModified()) {
      reply.setStatus(Status.conflict);
      // TODO: more Info? 
    } else {
      try {
        if (isLockedByMe(fqn)) {
          publishXmomModification(fqn, -1L, null);
          unlock(fqn, false);
        }
      } catch (XynaException e) {
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }

      close(fqn);
      reply.setStatus(Status.success);
    }

    return reply;
  }

  private void close(FQName fqn) throws DocumentNotOpenException {
    GenerationBaseObject gbo = gbos.get(fqn);
    if (gbo == null) {
      throw new DocumentNotOpenException(fqn);
    }

    gbo.setViewType(null);
    xmomGui.prepareModification(gbo); // remove gbo from cache
    gbos.remove(fqn);
    modifications.remove(fqn);
    xmomRedoHistory.remove(fqn);
    xmomUndoHistory.remove(fqn);
    pollEventFetcher.documentClosed(gbo);
    clearLockCache(fqn);
  }

  private boolean clearLockCache(FQName fqName) {
    Enumeration<XmomType> lockedTypes = locks.keys();
    while (lockedTypes.hasMoreElements()) {
      XmomType type = lockedTypes.nextElement();
      try {
        if (type.getName().equals(fqName.getFqName()) && Objects.equals(type.getRtc(), fqName.getDefiningRuntimeContext())) {
          locks.remove(type);
          return true;
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Could not clear lock", e);
        return false;
      }
    }

    return false;
  }

  private XMOMGuiReply view(XMOMGuiRequest request) throws Exception {
    boolean readonly = false;
    if (request.getFQName().getRuntimeContext() instanceof Application) {
      readonly = true;
    }

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    View view = gbos.get(request.getFQName()).getView();
    view.setReadonly(readonly);
    if (view.getGenerationBaseObject().getType() == XMOMType.DATATYPE && request.getObjectId() != null) {
      view.getGenerationBaseObject().focusOperation(request.getObjectId());
    }

    if (view.getGenerationBaseObject().getType() == XMOMType.WORKFLOW) {
      view.getGenerationBaseObject().createDataflow(getOrCreateWFWarningsHandler(view.getGenerationBaseObject().getFQName()));

      FQName fqName = request.getFQName();
      ObjectId objectId = new ObjectId(ObjectType.workflow, null);
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, view.getGenerationBaseObject().getWorkflow());
      getWFWarningsHandler(fqName).handleChange(objectId, notification);
    }

    reply.setXynaObject(view.viewAll(request));

    return reply;
  }
  
  
  private boolean isLocked(FQName fqName) {
    return getLockEvent(fqName) != null;
  }
  
  public boolean isLockedNotByMe(FQName fqName) {
    XmomTypeEvent event = getLockEvent(fqName);
    if(event != null) {
      return event.getMessageOutputParameter().getCreator() != null && !event.getMessageOutputParameter().getCreator().equals(session.getUser());
    }
    return false;
  }
  
  public boolean isLockedByMe(FQName fqName) {
    XmomTypeEvent event = getLockEvent(fqName);
    if(event != null) {
      return event.getMessageOutputParameter().getCreator() != null && event.getMessageOutputParameter().getCreator().equals(session.getUser());
    }
    return false;
  }
  
  private XmomTypeEvent getLockEvent(FQName fqName) {
    if(fqName == null || fqName.getFqName() == null || fqName.getDefiningRevision() == null) {
      return null;
    }

    Enumeration<XmomType> lockedTypes = locks.keys();
    while(lockedTypes.hasMoreElements()) {
      XmomType type = lockedTypes.nextElement();
      try {
        if (type.getName().equals(fqName.getFqName()) && Objects.equals(type.getRtc(), fqName.getDefiningRuntimeContext())){
          return locks.get(type);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Could not determine if lock exists", e);
        return null;
      }
    }

    return null;
  }

  private String createLockMessage(FQName fqName) {
    XmomTypeEvent event = getLockEvent(fqName);
    if (event != null && event.getMessageOutputParameter() != null && !StringUtils.isEmpty(event.getMessageOutputParameter().getCreator())) {
      return fqName.getFqName() + " is locked by " + event.getMessageOutputParameter().getCreator() + ".";
    }

    return fqName.getFqName() + " is locked.";
  }
  
  private boolean lock(FQName fqName) throws XynaException, LockUnlockException {
    if (isLockedByMe(fqName)) {
      return false;
    }

    Long revision = fqName.getRevision();
    String fqn = fqName.getFqName();
    GenerationBaseObject gbo = gbos.get(fqName);
    String typeName = gbo.getType().getNiceName();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    boolean wasSuccessful = false;
    if (rm.getRuntimeContext(revision).getType() == RuntimeContextType.Workspace) {
      wasSuccessful = multiChannelPortal.lockXMOM(session.getId(), session.getUser(), new Path(fqn, fqName.getDefiningRevision()), typeName);
      if (!gbo.getSaveState()) {
        return wasSuccessful;
      }

      if (wasSuccessful) {
        waitForLockUnlock(fqName, true);
      }
    }

    return wasSuccessful;
  }

  private XMOMGuiReply unlock(XMOMGuiRequest request, boolean force) throws XynaException, LockUnlockException {
    return unlock(request.getFQName(), force);
  }

  private XMOMGuiReply unlock(FQName fqName, boolean force) throws XynaException, LockUnlockException {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setXynaObject(new UnlockResponse());

    boolean isLockedByMe = isLockedByMe(fqName);
    if (!isLockedByMe && !force) {
      reply.setStatus(Status.forbidden);
      return reply;
    }

    if (!isLockedByMe && !isAllowed(UserManagement.GuiRight.ZETA_PROCESS_MODELLER_STEAL_LOCK.getKey())) {
      return XMOMGuiReply.fail(Status.forbidden, new XFMG_ACCESS_VIOLATION(UserManagement.GuiRight.ZETA_PROCESS_MODELLER_STEAL_LOCK.getKey(), session.getRole().getName()));
    }

    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (rm.getRuntimeContext(fqName.getRevision()).getType() == RuntimeContextType.Workspace) {
      String user;
      if (force) {
        user = lockMgmt.getLockingUser(new LockManagement.Path(fqName.getFqName(), fqName.getDefiningRevision()));
      } else {
        user = session.getUser();
      }

      resetAutosaveCount(fqName);

      String typeName = gbos.get(fqName).getType().getNiceName();
      multiChannelPortal.unlockXMOM(session.getId(), user, new Path(fqName.getFqName(), fqName.getDefiningRevision()), typeName);
      waitForLockUnlock(fqName, false);
      reply.setStatus(Status.success);
    } else {
      reply.setStatus(Status.forbidden);
    }

    return reply;
  }

  private void waitForLockUnlock(FQName fqName, boolean waitForLock) throws LockUnlockException {
    long sleepTime = LOCK_UNLOCK_WAIT_TIME;
    while ( (isLocked(fqName) != waitForLock) && (sleepTime < LOCK_UNLOCK_WAIT_TIME_LIMIT) ) {
      try {
        Thread.sleep(LOCK_UNLOCK_WAIT_TIME);
        sleepTime += LOCK_UNLOCK_WAIT_TIME;
      } catch (Exception e) {
        if (waitForLock) {
          com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Error while waiting for lock", e);
        } else {
          com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Error while waiting for unlock", e);
        }
      }
    }

    if (sleepTime >= LOCK_UNLOCK_WAIT_TIME_LIMIT) {
      if (waitForLock) {
        com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Timeout while waiting for lock.", null);
      } else {
        com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Timeout while waiting for unlock.", null);
      }
    }

    if (isLocked(fqName) != waitForLock) {
      throw new LockUnlockException(fqName);
    }
  }

  private XMOMGuiReply referenceCandidates(XMOMGuiRequest request) throws UnknownObjectIdException, MissingObjectException, XynaException {
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    GenerationBaseObject gbo = load(request.getFQName());
    GBSubObject object = gbo.getObject(request.getObjectId());
    Operation op = object.getOperation();

    // create interface to match with
    TypeInterface myType = TypeInterface.of(gbo.getGenerationBase());
    OperationInterface oi;
    try {
      oi = OperationInterface.of(myType, op);
    } catch (AvariableNotResolvableException e) {
      throw new RuntimeException(e);
    }
    nullName(oi);

    List<XMOMDatabaseSearchResultEntry> allCandidates = determineAllReferenceCandidates(request.getRevision());
    List<XMOMItemReference> candidates = new ArrayList<XMOMItemReference>();
    
    if(logger.isDebugEnabled()) {
      logger.debug("checking " + allCandidates.size() + " reference Candidates");
    }
    
    DeploymentItemRegistry domReg = dism.getRegistry(request.getRevision());
    DeploymentItemState domDis = domReg.get(request.getFQName().getFqName());
    
    //Datatype is not saved/deployed
    if(domDis == null) {
      XMOMGuiReply reply = new XMOMGuiReply();
      reply.setXynaObject(new ServiceReferenceCandidates(candidates));
      reply.setStatus(Status.success);
      return reply;
    }
    
    for (XMOMDatabaseSearchResultEntry aWf : allCandidates) {
      long aWfRevision = revMgmt.getRevision(aWf.getRuntimeContext());
      DeploymentItemRegistry registry = dism.getRegistry(aWfRevision);
      DeploymentItemState dis = registry.get(aWf.getFqName());
      if (dis != null) {
        candidates.addAll(determineCandidatesFromWf(dis, domDis, revMgmt, aWf, oi));
      }
    }
    
    if(logger.isDebugEnabled()) {
      logger.debug("found " + candidates.size() + " reference Candidates");
    }

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setXynaObject(new ServiceReferenceCandidates(candidates));
    reply.setStatus(Status.success);

    return reply;
  }
  
  
  private List<XMOMItemReference> determineCandidatesFromWf(DeploymentItemState dis, DeploymentItemState domDis, RevisionManagement revMgmt,
                                                            XMOMDatabaseSearchResultEntry aWf, OperationInterface oi)
      throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<XMOMItemReference> candidates = new ArrayList<XMOMItemReference>();
    
    InterfaceResolutionContext.updateCtx(DeploymentLocation.SAVED, dis);
    
    try {
      PublishedInterfaces interfaces = dis.getPublishedInterfaces(DeploymentLocation.SAVED);
      Set<DeploymentItemInterface> difs = interfaces.getAll();
      for (DeploymentItemInterface dif : difs) {
        boolean compatibleSignature = false;
        
        try {
          InterfaceResolutionContext.revertCtx();
          InterfaceResolutionContext.updateCtx(DeploymentLocation.SAVED, domDis);
          compatibleSignature = compatibleSignature(oi, dif);
        } finally {
          InterfaceResolutionContext.revertCtx();
          InterfaceResolutionContext.updateCtx(DeploymentLocation.SAVED, dis);
        }
        
        if (compatibleSignature) {
          logger.debug("found: " + dis.getName());
          RuntimeContext rtc = com.gip.xyna.xact.filter.util.Utils.getModellerRtc(revMgmt.getRevision(aWf.getRuntimeContext()));
          candidates.add(new XMOMItemReference(aWf.getFqName(), rtc));
        }
      }
    } finally {
      InterfaceResolutionContext.revertCtx();
    }

    return candidates;
  }
  
  
  private List<XMOMDatabaseSearchResultEntry> determineAllReferenceCandidates(Long revision) throws XNWH_SelectParserException, XNWH_InvalidSelectStatementException, PersistenceLayerException {
    List<XMOMDatabaseSearchResultEntry> result = new ArrayList<XMOMDatabaseSearchResultEntry>();
    XynaFactoryControl control = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    XMOMDatabase xmomDB = control.getXMOMDatabase();
    RuntimeContextDependencyManagement rcdp = control.getRuntimeContextDependencyManagement();
    SearchRequestBean srb;

    Set<Long> revisions = new HashSet<Long>();
    rcdp.getDependenciesRecursivly(revision, revisions);
    revisions.add(revision);
    
    XMOMDatabaseSearchResult partialResult;
    for (Long rev : revisions) {
      srb = new SearchRequestBean();
      srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
      srb.setMaxRows(-1);
      srb.setSelection(XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL.getColumnName() + "," + XMOMDatabaseEntryColumn.NAME.getColumnName()
          + "," + XMOMDatabaseEntryColumn.PATH.getColumnName() + "," + XMOMDatabaseEntryColumn.REVISION.getColumnName());

      List<XMOMDatabaseSelect> selects = new ArrayList<XMOMDatabaseSelect>();
      XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      select.addDesiredResultTypes(XMOMDatabaseType.WORKFLOW);
      selects.add(select);
      
      
      partialResult = xmomDB.searchXMOMDatabase(selects, -1, rev);
      result.addAll(partialResult.getResult());
    }

    return result;
  }
  

  private boolean compatibleSignature(OperationInterface dt_oi, DeploymentItemInterface wf_dif) {
    
    if(!(wf_dif instanceof OperationInterface) || ((OperationInterface)wf_dif).getType() != OperationType.WORKFLOW) {
      return false;
    }
    
    OperationInterface wf_interface = (OperationInterface)wf_dif;
    
    List<TypeInterface> dt_in = dt_oi.getInput();
    List<TypeInterface> dt_out = dt_oi.getOutput();
    List<TypeInterface> dt_throw = dt_oi.getExceptions();
    
    List<TypeInterface> wf_in = wf_interface.getInput();
    List<TypeInterface> wf_out = wf_interface.getOutput();
    List<TypeInterface> wf_throw = wf_interface.getExceptions();
    
    if(dt_in.size() != wf_in.size() || dt_out.size() != wf_out.size()) {
      return false;
    }
    
    if(!compatibleTypeInterfaceLists(wf_in, dt_in)) {
      return false;
    }
    
    if(!compatibleTypeInterfaceLists(dt_out, wf_out)) {
      return false;
    }
    
    if(!containsCompatibileTypes(dt_throw, wf_throw)) {
      return false;
    }
    
    
    return true;
  }
  

  //for every entry in moreSpecific, is there an entry in moreGeneral that matches?
  private boolean containsCompatibileTypes(List<TypeInterface> moreGeneral, List<TypeInterface> moreSpecific) {
    for (TypeInterface specific : moreSpecific) {
      if (!containsCompatibleType(moreGeneral, specific)) {
        return false;
      }
    }
    return true;
  }


  private boolean containsCompatibleType(List<TypeInterface> moreGeneral, TypeInterface specific) {
    for (TypeInterface general : moreGeneral) {
      if (general.isAssignableFrom(specific)) {
        return true;
      }
    }
    return false;
  }


  //assumes both lists have the same length
  private boolean compatibleTypeInterfaceLists(List<TypeInterface> moreGeneral, List<TypeInterface> moreSpecific) {
    
    for(int i=0; i<moreGeneral.size(); i++) {
      TypeInterface general = moreGeneral.get(i);
      TypeInterface specific = moreSpecific.get(i);
      
      if(!general.isAssignableFrom(specific)) {
        return false;
      }
    }
    
    return true;
  }

  public static void nullName(OperationInterface oi) {
    try {
      Field nameField = OperationInterface.class.getDeclaredField("name");
      nameField.setAccessible(true);
      nameField.set(oi, null);
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new RuntimeException("",e);
    }
  }

  private XMOMGuiReply dataflow(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, UnknownObjectIdException, MissingObjectException, XynaException {
    if (request.getJson() == null) {
      return getDataflow(request);
    } else {
      return modifyDataflow(request);
    }
  }


  private XMOMGuiReply getDataflow(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    View view = gbos.get(request.getFQName()).getView();
    view.getGenerationBaseObject().refreshDataflow(); // TODO reicht createDataflow()?
    boolean debug = request.getBooleanParamter("debug", false);
    GenerationBaseObject gbo = gbos.get(request.getFQName());
    
    GetDataflowResponse response = new GetDataflowResponse();
    response.setRevision(gbo.getRevision());
    Dataflow df = view.getGenerationBaseObject().getDataflow();
    df.setDebug(debug);    
    for (HasXoRepresentation xo : df.asEntryList()) {
      response.addToConnections((Connection) xo.getXoRepresentation());
    }
    if(response.getConnections() == null) {
      response.setConnections(Collections.emptyList());
    }
    reply.setXynaObject(response);
    return reply;
  }


  private XMOMGuiReply modifyDataflow(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, InvalidRevisionException, UnknownObjectIdException, MissingObjectException, XynaException {
    if(isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }
    JsonParser jp = new JsonParser();
    DataflowJson dataflowRequest = jp.parse(request.getJson(), DataflowJson.getJsonVisitor());
    Modification mod = getOrCreateModification(request.getFQName());
    mod.modifyDataflow(dataflowRequest, request.getRevision());
    mod.getObject().markAsModified();
    return getDataflow(request);
  }


  private XMOMGuiReply getRelations(XMOMGuiRequest request) throws UnknownObjectIdException, MissingObjectException, XynaException  {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    
    //return empty result for base.AnyType
    if(request.getFQName().getFqName().equals(GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME)) {
      GetRelationsResponse response = new GetRelationsResponse();
      reply.setXynaObject(response);
      return reply;
    }
    
    
    GenerationBaseObject gbo = gbos.get(request.getFQName());
    GBSubObject sub = gbo.getObject(new ObjectId(ObjectType.of(gbo.getType()), null).getObjectId());
    List<Reference> references = sub.getReferences();
    
    GetRelationsResponse response = new GetRelationsResponse();
    reply.setXynaObject(response);
    
    Map<Long, RuntimeContext> rtcCache = new HashMap<Long, RuntimeContext>();
    Map<ReferenceType, List<FactoryItem>> lists = new HashMap<>();
    for (Reference reference : references) {
      FactoryItem factoryItem = new FactoryItem();
      factoryItem.setFqn(reference.getFqName().getFqName());
      Long revision = reference.getFqName().getRevision();
      RuntimeContext rtc = rtcCache.get(revision);
      if(rtc == null) {
        rtc = com.gip.xyna.xact.filter.util.Utils.getModellerRtc(reference.getFqName().getRevision());
        rtcCache.put(revision, rtc);
      }
      
      factoryItem.setRtc(rtc);

      // determine type/label
      factoryItem.setLabel(reference.getLabel());
      factoryItem.setType(reference.getObjectType().toString());
      
      //TODO: PMOD-2789 - show static and instance services
      //TODO: turn base.Text.Text.trim to base.Text
      if(reference.getObjectType().equals(Type.codedService)) {
        continue;
      }
      lists.putIfAbsent(reference.getReferenceType(), new ArrayList<FactoryItem>());
      lists.get(reference.getReferenceType()).add(factoryItem);
    }

    response.setCalls(lists.get(ReferenceType.calls));
    response.setExtends0(lists.get(ReferenceType.extend));
    response.setOutputOf0(lists.get(ReferenceType.needs));
    response.setInputOf0(lists.get(ReferenceType.produces));
    response.setCalledBy(lists.get(ReferenceType.calledBy));
    response.setInputOf0(lists.get(ReferenceType.neededBy));
    response.setThrownBy0(lists.get(ReferenceType.thrownBy));
    response.setOutputOf0(lists.get(ReferenceType.producedBy));
    response.setUsedIn0(lists.get(ReferenceType.usedInImplOf));
    response.setHasMemberOf(lists.get(ReferenceType.possesses));
    response.setExceptions(lists.get(ReferenceType.exceptions));
    response.setExtendedBy0(lists.get(ReferenceType.extendedBy));
    response.setIsMemberOf(lists.get(ReferenceType.possessedBy));
    response.setInstanceServiceReferenceOf(lists.get(ReferenceType.instanceServiceReferenceOf));

    return reply;
  }


  private XMOMGuiReply createNew(XMOMGuiRequest request) throws InvalidJSONException, UnexpectedJSONContentException, XPRC_InvalidPackageNameException, XFMG_NoSuchRevision {
    
    if(isLockedNotByMe(request.getFQName())) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(request.getFQName()));
    }
    
    JsonParser jp = new JsonParser();
    LabelInputOutputJson label = jp.parse(request.getJson(), LabelInputOutputJson.getJsonVisitor());
    
    ObjectIdentifierJson object = new ObjectIdentifierJson();
    object.setRuntimeContext( new RuntimeContextJson( request.getRuntimeContext() ));
    object.setLabel(label.getLabel());
    object.setType( request.getType() );
    object.setFQName( new FQNameJson( "new_"+System.currentTimeMillis(), Utils.labelToJavaName(label.getLabel(), true ) ) );
    
    GenerationBaseObject gbo = createNewObject(object);
    if (request.getType().equals(Type.workflow)) {
      label.parseInputOutput();
      WF generationBase = (WF) gbo.getGenerationBase();
      for (VariableJson json: label.getInputJson()) {
        generationBase.getInputVars().add(json.toAVariable(generationBase, gbo.getFQName().getRevision()));
      }
      for (VariableJson json: label.getOutputJson()) {
        generationBase.getOutputVars().add(json.toAVariable(generationBase, gbo.getFQName().getRevision()));
      }
      generationBase.getWfAsStep().refreshVars(generationBase.getInputVars(), generationBase.getOutputVars());
    }
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(gbo.getView().viewAll(request));
    return reply;
  }
  
  public GenerationBaseObject createNewObject(ObjectIdentifierJson objectIdentifierJson) throws XPRC_InvalidPackageNameException, XFMG_NoSuchRevision {
    GenerationBaseObject gbo = xmomGui.createNewObject(objectIdentifierJson);
    add(gbo);
    getOrCreateModification(gbo.getFQName());
    
    if (objectIdentifierJson.getType() == Type.workflow) {
      gbo.createDataflow(getOrCreateWFWarningsHandler(gbo.getFQName()));
    }
    
    return gbo;
  }
  
  private String getCurrentXML(XMOMGuiRequest request) throws XynaException {
    GenerationBaseObject gbo = gbos.get(request.getFQName());
    PersistJson persistRequest = new PersistJson(request.getRevision().intValue(), true); 
    Persistence persistence = new Persistence(gbo, request.getRevision(), persistRequest, session);
    return persistence.createXML();
  }

  private void createXmomUndoHistoryItem(XMOMGuiRequest request) {
    try {
      if (!xmomUndoHistory.containsKey(request.getFQName())) {
        xmomUndoHistory.put(request.getFQName(), new LinkedList<>());
      }

      if (xmomUndoHistory.get(request.getFQName()).size() > UNDO_LIMIT.get()) {
        while (xmomUndoHistory.get(request.getFQName()).size() > UNDO_LIMIT.get()){
          xmomUndoHistory.get(request.getFQName()).removeFirst();
        }
      }

      GenerationBaseObject currentGbo = gbos.get(request.getFQName());
      XMOMHistoryItem history = new XMOMHistoryItem(request.getFQName(), getCurrentXML(request), currentGbo.getSaveState(), currentGbo.hasBeenModified());
      xmomUndoHistory.get(request.getFQName()).add(history);
    } catch (XynaException e) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
    }
  }

  private void createXmomRedoHistoryItem(XMOMGuiRequest request) {
    try {
      if (!xmomRedoHistory.containsKey(request.getFQName())) {
        xmomRedoHistory.put(request.getFQName(), new LinkedList<>());
      }

      if (xmomRedoHistory.get(request.getFQName()).size() > REDO_LIMIT.get()) {
        while (xmomRedoHistory.get(request.getFQName()).size() > REDO_LIMIT.get()){
          xmomRedoHistory.get(request.getFQName()).removeFirst();
        }
      }

      GenerationBaseObject currentGbo = gbos.get(request.getFQName());
      XMOMHistoryItem historyItem = new XMOMHistoryItem(request.getFQName(), getCurrentXML(request), currentGbo.getSaveState(), currentGbo.hasBeenModified());
      xmomRedoHistory.get(request.getFQName()).add(historyItem);
    } catch (XynaException e) {
      com.gip.xyna.xact.filter.util.Utils.logError(e);
    }
  }

  private XMOMGuiReply redo(XMOMGuiRequest request) throws XynaException, LockUnlockException {
    if(!xmomRedoHistory.containsKey(request.getFQName()) || xmomRedoHistory.get(request.getFQName()).isEmpty()) {
      return XMOMGuiReply.fail(Status.notfound, "No History for " + request.getFQName() + " available");
    }

    createXmomUndoHistoryItem(request);
    XMOMHistoryItem redoHistoryItem = xmomRedoHistory.get(request.getFQName()).removeLast();

    return restoreHistoryItem(request, redoHistoryItem);
  }

  private XMOMGuiReply undo(XMOMGuiRequest request) throws XynaException, LockUnlockException {
    if(!xmomUndoHistory.containsKey(request.getFQName()) || xmomUndoHistory.get(request.getFQName()).isEmpty()) {
      return XMOMGuiReply.fail(Status.notfound, "No History for " + request.getFQName() + " available");
    }

    createXmomRedoHistoryItem(request);
    XMOMHistoryItem undoHistoryItem = xmomUndoHistory.get(request.getFQName()).removeLast();

    return restoreHistoryItem(request, undoHistoryItem);
  }

  private XMOMGuiReply restoreHistoryItem(XMOMGuiRequest request, XMOMHistoryItem historyItem) throws XynaException, LockUnlockException {
    GenerationBaseObject gbo = loadGboFromHistory(request.getFQName(), historyItem);
    refreshGbo(gbo);

    lock(request.getFQName());
    if (gbo.hasBeenModified()) {
      publishXmomModification(request.getFQName(), getNextAutosaveCount(gbo.getFQName()), getCurrentXML(request));
    } else {
      publishXmomModification(request.getFQName(), -1L, null);
      unlock(request.getFQName(), false);
    }

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(gbo.getView().viewAll(request));

    return reply;
  }

  private GenerationBaseObject loadGboFromHistory(FQName fqName, XMOMHistoryItem historyItem) throws XynaException {
    GenerationBaseObject gbo = loadGboFromXml(fqName, historyItem.getXml());

    gbo.setSaveState(historyItem.getSaveState());
    if (historyItem.getModified()) {
      gbo.markAsModified();
    }

    return gbo;
  }

  private GenerationBaseObject loadGboFromXml(FQName fqName, String xml) throws XynaException {
    GenerationBaseObject gbo = xmomGui.load(fqName, xml);
    applyOldViewTyp(fqName, gbo, true);

    return gbo;
  }

  public void reloadGbo(FQName fqName) throws XynaException {
    GenerationBaseObject gbo = xmomGui.load(fqName);
    reloadGbo(fqName, gbo);
  }

  public void reloadGbo(FQName fqName, String xml) throws XynaException {
    GenerationBaseObject gbo = loadGboFromXml(fqName, xml);
    reloadGbo(fqName, gbo);
  }

  public void reloadGbo(FQName fqName, GenerationBaseObject gbo) {
    try {
      applyOldViewTyp(fqName, gbo, true);
      refreshGbo(gbo);

      // reset undo/redo history
      xmomUndoHistory.put(fqName, new LinkedList<>());
      xmomRedoHistory.put(fqName, new LinkedList<>());
    } catch (Exception e) {
      com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Could not refresh document " + fqName, e);
      try {
        close(fqName);
      } catch (DocumentNotOpenException e1) {
        com.gip.xyna.xact.filter.util.Utils.logError("Multiuser: Could not close document that could not be refreshed " + fqName, e1);
      }
    }
  }

  private void applyOldViewTyp(FQName fqName, GenerationBaseObject gbo, boolean forceRepair) {
    GenerationBaseObject oldGbo = gbos.get(fqName);
    if (gbo == oldGbo) {
      return;
    }

    if (forceRepair) {
      // repair if necessary
      XMOMRepair repair = new XMOMRepair();
      repair.repair(gbo);
    }

    if (oldGbo != null) {
      gbo.setViewType(oldGbo.getViewType());
    }
  }

  private void refreshGbo(GenerationBaseObject gbo) {
    add(gbo);
    Modification mod = getOrCreateModification(gbo.getFQName());
    modifications.remove(gbo.getFQName());
    mod.setObject(gbo);
    modifications.put(gbo.getFQName(), mod);
    gbo.incrementRevision();
    if(gbo.getType() == XMOMType.WORKFLOW) {
      gbo.createDataflow(getOrCreateWFWarningsHandler(gbo.getFQName())); // calculate dataflow before modifiy operation to backup connection information in dataflow
      gbo.getDataflow().applyDataflowToGB();
    }
  }


  private XMOMGuiReply objectModification(XMOMGuiRequest request) throws Exception {
    String objectId = request.getObjectId();
    if ((ObjectId.parse(objectId).getType() == ObjectType.warning)) {
      return deleteWarning(request);
    }

    FQName fqName = request.getFQName();
    if(isLockedNotByMe(fqName)) {
      return XMOMGuiReply.fail(Status.forbidden, createLockMessage(fqName));
    }

    lock(fqName);

    // Undo-History 
    createXmomUndoHistoryItem(request);
    
    // Every change makes any existing redo impossible
    xmomRedoHistory.remove(fqName);
    
    Modification mod = getOrCreateModification(fqName);

    // apply modification
    GenerationBaseObject gbo = gbos.get(fqName);
    
    try {
      switch (gbo.getType()) {
        case DATATYPE:
        case EXCEPTION:
          mod.modify(objectId, request.getOperation(), request.getJson() );
          gbo.incrementRevision();
          break;
        case WORKFLOW:
          gbo.createDataflow(getOrCreateWFWarningsHandler(gbo.getFQName())); // calculate dataflow before modifiy operation to backup connection information in dataflow
          mod.modify(objectId, request.getOperation(), request.getJson() );
          gbo.getDataflow().applyDataflowToGB(); // apply backuped connection information since indices of variables might have changed
          break;
        default:
          break;
      }
    } catch (MergeConflictException e) {
      throw e;
    } catch (Exception e) {
      // rollback
      XMOMHistoryItem historyItem = xmomUndoHistory.get(request.getFQName()).removeLast();
      gbo = loadGboFromHistory(fqName, historyItem);
      refreshGbo(gbo);
      publishXmomModification(fqName, getNextAutosaveCount(fqName), getCurrentXML(request));
      unlock(fqName, false);

      throw e;
    }

    if (request.getOperation() != com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation.CopyToClipboard) {
      publishXmomModification(fqName, getNextAutosaveCount(fqName), getCurrentXML(request));
    }

    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    reply.setXynaObject(mod.getXoRepresentation());

    return reply;
  }

  private XMOMGuiReply deleteWarning(XMOMGuiRequest request) throws Exception {
    ObjectId warningId = ObjectId.parse(request.getObjectId());
    warningHandlers.get(request.getFQName()).deleteWarning(warningId.getObjectId());

    return getWarnings(request);
  }

  private void publishXmomModification(FQName fqName, Long autosaveCount, String xml) throws XynaException {  
    GenerationBaseObject gbo = gbos.get(fqName);
    Path path = new Path(fqName.getFqName(), fqName.getDefiningRevision());
    multiChannelPortal.publishXMOM(
              session.getId(), 
              session.getUser(), 
              path, 
              gbo.getType().getNiceName(), 
              xml, 
              autosaveCount);
  }

  private void resetAutosaveCount(FQName fqName) {
    autosaveCount.remove(fqName);
  }
  
  private Long getNextAutosaveCount(FQName fqName) {
    if(!autosaveCount.containsKey(fqName)) {
      autosaveCount.put(fqName, 0L);
    } else {
      autosaveCount.put(fqName, autosaveCount.get(fqName) + 1);
    }
    return autosaveCount.get(fqName);
  }
  
  private Modification getOrCreateModification(FQName fqName) {
    Modification mod = modifications.get(fqName);
    if( mod == null ) {
      GenerationBaseObject gbo = gbos.get(fqName);
      mod = new Modification(this, gbo, clipboard);
      xmomGui.prepareModification(gbo);
      modifications.put(fqName, mod);
    }
    return mod;
  }

  private XMOMGuiReply session(XMOMGuiRequest request) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.setStatus(Status.success);
    
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();{
      jb.addObjectListAttribute( "objects", GboJson.list(gbos.values()) ); // TODO: Konstante
    } jb.endObject();
    reply.setJson(jb.toString());
    
    return reply;
    
  }

  private void replace(FQName oldFqn, FQName newFqn) throws XynaException {
    GenerationBaseObject oldGbo = gbos.get(oldFqn);
    xmomGui.prepareModification(oldGbo); // remove old gbo from cache
    gbos.remove(oldFqn);
    pollEventFetcher.documentClosed(oldGbo);
    clearLockCache(oldFqn);

    // in case a gbo with the new fqn is already existing, remove it from the cache
    GenerationBaseObject newGbo = gbos.get(newFqn);
    if (newGbo != null) {
      xmomGui.prepareModification(newGbo);
    }
    
    // reload with new fqn
    newGbo = xmomGui.load(newFqn);
    newGbo.setRevision(oldGbo.getRevision());
    newGbo.setSgLibsToUpload(oldGbo.getSgLibsToUpload());
    newGbo.setSgLibsToDelete(oldGbo.getSgLibsToDelete());
    newGbo.setViewType(oldGbo.getViewType());
    add(newGbo);
    
    Modification mod = modifications.get(oldFqn);
    if (mod != null) {
      modifications.remove(oldFqn);
      mod.setObject(newGbo);
      modifications.put(newFqn, mod);
    }
    
    LinkedList<XMOMHistoryItem> redoHistory = xmomRedoHistory.remove(oldFqn);
    if(redoHistory != null) {
      xmomHistoryFqnChanged(redoHistory, newGbo);
      xmomRedoHistory.put(newFqn, redoHistory);
    }
    
    LinkedList<XMOMHistoryItem> undoHistory = xmomUndoHistory.remove(oldFqn);
    if(undoHistory != null) {
      xmomHistoryFqnChanged(undoHistory, newGbo);
      xmomUndoHistory.put(newFqn, undoHistory);
    }
  }
  
  private void xmomHistoryFqnChanged(LinkedList<XMOMHistoryItem> history, final GenerationBaseObject newGbo) {
    if(history == null) {
      return;
    }
    history.forEach(h -> {
      try {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(h.getXml())));
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList)xpath.evaluate("/Service", doc, XPathConstants.NODESET);
        if(nodes != null && nodes.getLength() == 1) {
          Node label = nodes.item(0).getAttributes().getNamedItem("Label");
          Node typeName = nodes.item(0).getAttributes().getNamedItem("TypeName");
          Node typePath = nodes.item(0).getAttributes().getNamedItem("TypePath");
          label.setNodeValue(newGbo.getGenerationBase().getLabel());
          typeName.setNodeValue(newGbo.getGenerationBase().getOriginalSimpleName());
          typePath.setNodeValue(newGbo.getGenerationBase().getOriginalPath());
          
          StringWriter writer = new StringWriter();
          
          Transformer xformer = TransformerFactory.newInstance().newTransformer();
          xformer.transform(new DOMSource(doc), new StreamResult(writer));
          
          h.setXml(writer.getBuffer().toString());
          setSavedAndModified(h); 
        }
      } catch (Exception e) {
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }
    });
  }

  public GenerationBaseObject load(FQName fqName) throws XynaException {
    GenerationBaseObject gbo = gbos.get(fqName);
    if( gbo != null ) {
      return gbo;
    }

    gbo = xmomGui.load(fqName);
    pollEventFetcher.documentOpened(gbo);

    return gbo;
  }


  protected Map<FQName, GenerationBaseObject> getGbos() {
    return gbos;
  }
  
  private boolean isAllowed(String right) throws XynaException {
    return XynaFactory.getInstance().getFactoryManagementPortal().hasRight(right, session.getRole());
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((session == null) ? 0 : session.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SessionBasedData other = (SessionBasedData) obj;
    if (session == null) {
      if (other.session != null)
        return false;
    } else if (!session.equals(other.session))
      return false;
    return true;
  }


  public Clipboard getClipboard() {
    return clipboard;
  }

}
