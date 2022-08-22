/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xact.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.CallStatistics.StatisticsEntry;
import com.gip.xyna.xact.filter.FilterAction.FilterActionInstance;
import com.gip.xyna.xact.filter.actions.*;
import com.gip.xyna.xact.filter.actions.auth.ChangePasswordAction;
import com.gip.xyna.xact.filter.actions.auth.ExternalUserLoginAction;
import com.gip.xyna.xact.filter.actions.auth.ExternalUserLoginInformationAction;
import com.gip.xyna.xact.filter.actions.auth.InfoAction;
import com.gip.xyna.xact.filter.actions.auth.LoginAction;
import com.gip.xyna.xact.filter.actions.auth.LogoutAction;
import com.gip.xyna.xact.filter.actions.auth.SharedLoginAction;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.actions.generateinput.GenerateinputAction;
import com.gip.xyna.xact.filter.actions.monitor.AuditsOrderIdDownloadAction;
import com.gip.xyna.xact.filter.actions.monitor.ImportedAuditsAction;
import com.gip.xyna.xact.filter.actions.monitor.OpenAuditAction;
import com.gip.xyna.xact.filter.actions.orderinputdetails.OrderinputdetailsAction;
import com.gip.xyna.xact.filter.actions.startorder.StartorderAction;
import com.gip.xyna.xact.filter.actions.starttestcase.StarttestcaseAction;
import com.gip.xyna.xact.filter.actions.xacm.CreateUserAction;
import com.gip.xyna.xact.filter.actions.xacm.UpdateUserAction;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.DomOrExceptionStructure;
import com.gip.xyna.xact.filter.util.xo.DomOrExceptionSubtypes;
import com.gip.xyna.xact.filter.util.xo.ServiceSignature;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.CustomOrderEntryInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xprc.xsched.CapacityStorable;

public class H5XdevFilter extends ConnectionFilter<HTTPTriggerConnection> {

  private static final long serialVersionUID = 1L;

  private static final String NAME = "Xyna H5 XDEV";
  public static final String ORDERENTRYNAME = "H5XdevFilter";

  private static Logger logger = CentralFactoryLogging.getLogger(H5XdevFilter.class);

  public static final XynaPropertyString STATIC_FILES = new XynaPropertyString("xmcp.guihttp.static_file_location", "../guihttp")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Path to directory with static files (.css, .ico, etc.)")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Pfad zum Verzeichnis, in dem statische Dateien liegen (.css, .ico, etc.)");

  //used by GUIHTTP as well!
  public static final XynaPropertyString ACCESS_CONTROL_ALLOW_ORIGIN = new XynaPropertyString("xmcp.guihttp.access_control_allow_origin", "")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Cross-origin resource sharing (CORS): Comma separated list of origins or *. A origin is of format http[s]://<fqhostname>[:port]")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Cross-origin resource sharing (CORS): Kommaseparierte Liste von Origins oder *. Ein Origin hat Format http[s]://<fqhostname>[:port]");

  public static final XynaPropertyBuilds<Long> DEFAULT_WORKSPACE =
      new XynaPropertyBuilds<Long>("xmcp.guihttp.default_workspace", new WorkspaceRevisionBuilder(), Long.valueOf(-1L))
          .setDefaultDocumentation(DocumentationLanguage.EN, "Default workspace")
          .setDefaultDocumentation(DocumentationLanguage.DE, "Default Workspace");

  private static final String cache_size_property_name = "xmcp.guihttp.xmom_cache_size";
  
  public static final XynaPropertyInt GENERATION_BASE_CACHE_SIZE = new XynaPropertyInt(cache_size_property_name, 100)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Number of XMOM Objects held in cache. Shared across all users")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Anzahl an XMOM Objekten, die maximal im Cache gehalten werden. Dieser Cache wird zwischen allen Benutzern geteilt.");

  public static final XynaPropertyBoolean USE_CACHE = new XynaPropertyBoolean("xmcp.guihttp.use_cache", true)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Use a cache to store recently used objects. Cache size is determined by " + cache_size_property_name)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Verwende einen Cache um auf zuletzt verwendete Objekte schneller zugreifen zu können. Größe des Caches is bestimmt durch " + cache_size_property_name);

  public static final XynaPropertyBoolean AVARCONSTANTS = new XynaPropertyBoolean("xmcp.guihttp.new_constants", true).
      setDefaultDocumentation(DocumentationLanguage.EN, "Prevent instantiation problems by using a different approach to convert json to constants.");
  
  public static final XynaPropertyBoolean CompressResponse = new XynaPropertyBoolean("xmcp.guihttp.compress_response", true)
      .setDefaultDocumentation(DocumentationLanguage.EN, "compress response of requests using gzip, if supported by caller")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Komprimiere Antworten mit gzip, wenn es vom Aufrufer unterstützt wird");
  
  private static class WorkspaceRevisionBuilder implements XynaPropertyBuilds.Builder<Long> {

    private RevisionManagement rm;


    private RevisionManagement getRevisionManagement() {
      if (rm == null) {
        rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      }
      return rm;
    }


    @Override
    public Long fromString(String string)
        throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
      try {
        return getRevisionManagement().getRevision(new Workspace(string));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new ParsingException("Invalid workspace name", e);
      }
    }


    @Override
    public String toString(Long revision) {
      try {
        return getRevisionManagement().getRuntimeContext(revision).getName();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }

  }


  protected static List<FilterAction> allFilterActions;

  private FilterActionInstance filterActionInstance;

  private static XMOMGui xmomGui;

  private static final String BASE_PATH = "/xmom";

  private static CallStatistics callStatistics;

  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   * @throws XynaException 
   */
  @SuppressWarnings("rawtypes")
  public void onDeployment(EventListener triggerInstance) {
    
    Long revision = null;
    try {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      revision = clb.getRevision();
    } catch(Exception e) {
      return;
    }
    
    CustomOrderEntryInformation entry = new CustomOrderEntryInformation();
    entry.setName(ORDERENTRYNAME);
    entry.setDefiningRevision(revision);
    entry.setDescription("allows H5XdevFilter to start orders");
    entry.setDefaultBehavior(RevisionOrderControl.CustomOrderEntryInformation.DefaultBehavior.alwaysOpen);
    RevisionOrderControl.registerCustomOrderEntryType(revision, entry);
    
    
    try {
      xmomGui = new XMOMGui();
    } catch (XynaException e) {
      throw new RuntimeException("Instantiating XMOMGui failed", e);
    }

    String applicationVersion = getApplicationVersion();

    callStatistics = new CallStatistics(50);//TODO

    StaticFileAction sfa = new StaticFileAction(STATIC_FILES);
    sfa.addFavIcon("favicon.ico");
    sfa.addCSS("/gipstyle.css", "gipstyle.css");

    allFilterActions = new ArrayList<>();
//    allFilterActions.add(new SessionAction(xmomGui)); // Deaktiviert PMOD-589
    allFilterActions.add(new PathsAction());

    allFilterActions.add(new ObjectsAction(xmomGui.getXmomLoader()));
    allFilterActions.add(new ObjectsPathAction(xmomGui.getXmomLoader()));
    allFilterActions.add(new ObjectsPathNameAction(xmomGui));

    allFilterActions.add(new CodedservicesAction(xmomGui));
    allFilterActions.add(new CodedservicesPathAction(xmomGui));
    allFilterActions.add(new CodedservicesPathNameAction(xmomGui));

    allFilterActions.add(new DatatypesAction(xmomGui));
    allFilterActions.add(new DatatypesPathAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdAction(xmomGui));
    //allFilterActions.add( new DatatypesPathNameVariablesAction(xmomGui) );//TODO
    //allFilterActions.add( new DatatypesPathNameBasetypesAction(xmomGui) );//TODO
    //allFilterActions.add( new DatatypesPathNameSubtypesAction(xmomGui) );//TODO
    allFilterActions.add(new DatatypesPathNameSaveAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameDeployAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameDeleteAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameRefactorAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdRefactorAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameUploadAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameCloseAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameUnlockAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameUndoAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameRedoAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameXmlAction(xmomGui));
    //allFilterActions.add( new DatatypesPathNameDeployAction(xmomGui) );//TODO
    allFilterActions.add(new DatatypesPathNameObjectsIdInsertAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdChangeAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdCopyAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdCopyToClipboard(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdMoveAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdDeleteAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdTemplateCallAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameObjectsIdReferenceCandidatesAction(xmomGui));
    allFilterActions.add(new DatatypesPathNameRelationsAction(xmomGui) );
    
    allFilterActions.add(new ExceptionsAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameDeployAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameDeleteAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameRefactorAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameSaveAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameCloseAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameUnlockAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameUndoAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameRedoAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameXmlAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameObjectsIdCopyToClipboard(xmomGui));
    allFilterActions.add(new ExceptionsPathNameObjectsIdInsertAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameObjectsIdMoveAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameObjectsIdChangeAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameObjectsIdDeleteAction(xmomGui));
    allFilterActions.add(new ExceptionsPathNameRelationsAction(xmomGui) );
    
    allFilterActions.add( new ServiceGroupsAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameDeployAction(xmomGui));
    allFilterActions.add( new ServiceGroupsPathNameDeleteAction(xmomGui));
    allFilterActions.add( new ServiceGroupsPathNameRefactorAction(xmomGui));
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdRefactorAction(xmomGui));
    allFilterActions.add( new ServiceGroupsPathNameCloseAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameUnlockAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdChangeAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdInsertAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdCopyAction(xmomGui));
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdMoveAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdDeleteAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameObjectsIdTemplateCallAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameXmlAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameRedoAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameUndoAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameSaveAction(xmomGui) );
    allFilterActions.add( new ServiceGroupsPathNameRelationsAction(xmomGui) );

    allFilterActions.add(new WorkflowsAction(xmomGui));
    allFilterActions.add(new WorkflowsPathAction(xmomGui));
    allFilterActions.add(new WorkflowsPathNameAction(xmomGui));
    allFilterActions.add(new WorkflowsPathNameDataflowAction(xmomGui));
    allFilterActions.add(new WorkflowsPathNameOrderInputSources(xmomGui));
    allFilterActions.add(new WorkflowsPathNameInsertAction(xmomGui));
    allFilterActions.add(new WorkflowsPathNameIssuesAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameSaveAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameDeployAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameCloseAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameUnlockAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameRefactorAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameDeleteAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameUndoAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameRedoAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameXmlAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameRelationsAction(xmomGui) );
    allFilterActions.add( new WorkflowsPathNameWarningsAction(xmomGui) );

    allFilterActions.add( new DeployedWorkflowsPathNameAction(xmomGui) );
    allFilterActions.add( new DeployedWorkflowsPathNameDataflowAction(xmomGui) );

    //allFilterActions.add( new WorkflowsPathNameConstantsAction(xmomGui) ); //TODO
    //allFilterActions.add( new WorkflowsPathNameConstantsIdAction(xmomGui) ); //TODO

    //allFilterActions.add( new WorkflowsPathNameObjectsAction(xmomGui) ); //TODO
    allFilterActions.add(new WorkflowsPathNameObjectsId(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdChange(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdComplete(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdDecouple(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdSort(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdDelete(xmomGui));
    //allFilterActions.add( new WorkflowsPathNameObjectsIdDetailsAction(xmomGui) ); //TODO
    allFilterActions.add(new WorkflowsPathNameObjectsIdMove(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdCopy(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdCopyToClipboard(xmomGui));
    allFilterActions.add(new WorkflowsPathNameClipboardIndexPaste(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdType(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdConvert(xmomGui));
    allFilterActions.add(new WorkfowsPathNameObjectsIdXml(xmomGui));
    
    allFilterActions.add(new WorkflowsPathNameObjectsIdConstant(xmomGui));
    allFilterActions.add(new WorkflowsPathNameObjectsIdConstantDelete(xmomGui));
    
    allFilterActions.add(new ClipboardAction(xmomGui));
    allFilterActions.add(new ClipboardClearAction(xmomGui));

    allFilterActions.add(new EventsUUIDAction(xmomGui));

    allFilterActions.add(new RemoteDestinationsAction());
    
    allFilterActions.add(sfa);
    allFilterActions.add(new StatisticsAction(applicationVersion, NAME, callStatistics));
    allFilterActions.add(new OptionsAction(ACCESS_CONTROL_ALLOW_ORIGIN));
    allFilterActions.add(new IndexAction(allFilterActions, applicationVersion, NAME, BASE_PATH));

    allFilterActions.add(new LoginAction());
    allFilterActions.add(new InfoAction());
    allFilterActions.add(new LogoutAction());
    allFilterActions.add(new ExternalUserLoginInformationAction());
    allFilterActions.add(new ExternalUserLoginAction());
    allFilterActions.add(new SharedLoginAction());
    allFilterActions.add(new ChangePasswordAction());

    allFilterActions.add(new StartorderAction());
    allFilterActions.add(new StarttestcaseAction());
    allFilterActions.add(new OrderinputdetailsAction());
    allFilterActions.add(new GenerateinputAction());

    allFilterActions.add( new DomOrExceptionStructure() );
    allFilterActions.add( new DomOrExceptionSubtypes() );
    allFilterActions.add( new ServiceSignature() );
    
    allFilterActions.add( new CreateUserAction() );
    allFilterActions.add( new UpdateUserAction() );
    
    allFilterActions.add( new OpenAuditAction() );
    allFilterActions.add( new ImportedAuditsAction() );
    allFilterActions.add( new AuditsOrderIdDownloadAction() );

    STATIC_FILES.registerDependency(UserType.Filter, NAME);
    ACCESS_CONTROL_ALLOW_ORIGIN.registerDependency(UserType.Filter, NAME);
    DEFAULT_WORKSPACE.registerDependency(UserType.Filter, NAME);
    GENERATION_BASE_CACHE_SIZE.registerDependency(UserType.Filter, NAME);
    USE_CACHE.registerDependency(UserType.Filter, NAME);
    AVARCONSTANTS.registerDependency(UserType.Filter, NAME);
    
    super.onDeployment(triggerInstance);
  }


  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @SuppressWarnings("rawtypes")
  public void onUndeployment(EventListener triggerInstance) {
    
    if(xmomGui != null) {
      try {
        xmomGui.quitSessionsForAllKnownLogins();
      } catch (Exception ex) {
        Utils.logError(ex);
      }
    }
    
    super.onUndeployment(triggerInstance);
    
    STATIC_FILES.unregister();
    ACCESS_CONTROL_ALLOW_ORIGIN.unregister();
    DEFAULT_WORKSPACE.unregister();
    GENERATION_BASE_CACHE_SIZE.unregister();
    USE_CACHE.unregister();
    AVARCONSTANTS.unregister();
  }


  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc) throws XynaException {
    URLPath url;
    try {
      tc.read(true); // Reads only the headers because the FileUploadAction need an unread Stream.

      // FIXME URLPath returns different PathLength if a trailing '/' is present...path-lengths a regularly checked by commands
      url = URLPath.parseURLPath(tc);

      tc.readPayload();
    } catch (InterruptedException e) {
      Utils.logError("Could not read request", e);
      DefaultFilterActionInstance dfai = new DefaultFilterActionInstance();
      dfai.sendError(tc, "Could not read request"); //FIXME
      return FilterResponse.responsibleWithoutXynaorder();
    }

    logger.info(NAME + " called : \"" + url + "\"");
    StatisticsEntry statisticsEntry = callStatistics.newRequest(url.getPath(), tc.getMethodEnum(), tc);

    
    try {
      for (FilterAction fa : allFilterActions) {
        if (matchAction(fa, url, tc.getMethodEnum())) {
          filterActionInstance = fa.act(url, tc);
          if (filterActionInstance == null) {
            continue;
          }
          filterActionInstance.fillStatistics(statisticsEntry);
          FilterResponse fr = filterActionInstance.filterResponse();
          switch (fr.getResponsibility()) {
            case RESPONSIBLE_WITHOUT_XYNAORDER :
              filterActionInstance.onResponsibleWithoutXynaOrder(tc);
              break;
            case RESPONSIBLE :
              break;
            case NOT_RESPONSIBLE :
              break;
            case RESPONSIBLE_BUT_TOO_NEW :
              break;
            default :
              break;
          }
          return fr;
        }
      }
    } catch (Throwable t) {
      JsonFilterActionInstance jfai = new JsonFilterActionInstance();
      AuthUtils.replyError(tc, jfai, Status.failed, t);
      return FilterResponse.responsibleWithoutXynaorder();
    }
    return FilterResponse.notResponsible();
  }

  
  private boolean matchAction(FilterAction fa, URLPath url, Method method) {
    try {
      return fa.match(url, method);
    } catch (Exception e) {
      logger.debug("Exception during match of filterAction. " + fa, e);
    }
    return false;
  }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc) {
    if(filterActionInstance != null) {
      filterActionInstance.onResponse(response, tc);
    }
  }


  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, HTTPTriggerConnection tc) {
    if(filterActionInstance != null) {
      filterActionInstance.onError(e, tc);
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "This Filter handles HTTP requests for H5 Modeller";
  }


  private String getApplicationVersion() {
    StringBuilder sb = new StringBuilder();
    try {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      Long rev = clb.getRevision();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      try {
        RuntimeContext rc = rm.getRuntimeContext(rev);
        sb.append(rc.toString());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        sb.append("unknown");
      }
      sb.append(" (rev=").append(rev).append(")");
    } catch (ClassCastException e) {
      sb.append("unknown (mocked?)");
    }

    if (!XynaFactory.isInstanceMocked()) {
      CapacityStorable tmpInstance = new CapacityStorable();
      int ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
      if (ownBinding != 0) {
        sb.append(" on node ").append(ownBinding);
      }
    }
    
    return sb.toString();
  }
}
