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
package com.gip.xyna.xmcp;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_ACTION_REQUIRES_SESSION_HANDLING;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordExpiredException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.CopyCLOResult;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.ClearWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionBasedUserContextValue;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xmcp.exceptions.XMCP_RMIExceptionWrapper;
import com.gip.xyna.xmcp.xfcli.generated.Clearworkingset;
import com.gip.xyna.xmcp.xfcli.impl.ClearworkingsetImpl;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_CancelFailedException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.manualinteraction.ExtendedOutsideFactorySerializableManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.EnhancedAudit;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteOutputStream;
import com.healthmarketscience.rmiio.RemoteOutputStreamClient;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;

//FIXME einheitliche behandlung von transformxynaexception/signatur. manche methoden werfen 
//xynaexception+remoteexception und machen transform, andere nicht, andere werfen nur remote.
//FIXME keine logik in rmichannel: rmi channel sollte nur weiterleiten!

//diese klasse sollte nur per reflection mit dem rmiclassloader instanziiert werden
//und bei Änderungen an mdmklassen erneut mit einer neuen rmiclassloader instanz instanziiert werden.
public class RMIChannelImpl extends Section implements XynaRMIChannelBase, InitializableRemoteInterface {

  public final static String DEFAULT_NAME = "Xyna RMI Channel";
  protected static final Logger logger = CentralFactoryLogging.getLogger(RMIChannelImpl.class);
  
  private final RemoteApplicationManagementWrapper ramw;
  
  public RMIChannelImpl() throws XynaException {
    super();
    ramw = new RemoteApplicationManagementWrapper();
  }
  

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  public void init() throws XynaException {
    //ACHTUNG, wird bei jedem redeployment aufgerufen per reflection im konstruktor vom RMIManagement aus beim rebind.
  }


  @Override
  protected void shutdown() {
    try {
      super.shutdown(); //ACHTUNG: rmi hat keine function groups => gefahrlos alles als runtimeexception weiterwerfen
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static XynaPlainSessionCredentials ensureSessionCredentials(XynaCredentials credentials, String requestedAction) throws RemoteException {
    if (credentials instanceof XynaPlainSessionCredentials) {
      return (XynaPlainSessionCredentials) credentials;
    } else {
      throw new RemoteException("Invalid authentification for requested operation!", new XFMG_ACTION_REQUIRES_SESSION_HANDLING(requestedAction));
    }
  }

  private Long getRevision(RuntimeContext runtimeContext) throws RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision;
    try {
      revision = revisionManagement.getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      transformXynaException(e);
      return null;
    }
    
    return revision;
  }


  protected Channel getMultiChannelPortal() {
    return XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal();
  }


  protected XynaMultiChannelPortalSecurityLayer getSecuredPortal() {
    return XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer();
  }


  protected static RemoteException transformXynaException(XynaException e) throws RemoteException {
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("returning error over rmi", e);
      }
      RemoteException remote =
          new RemoteException(InetAddress.getLocalHost().getHostAddress() + " >>> " + e.getCode() + ": "
              + e.getMessage(), excludeInternalClasses(e));
      throw remote;
    } catch (UnknownHostException e1) {
      RemoteException remote = new RemoteException("Unknown Host >>> " + e.getCode() + ": " + e.getMessage(), excludeInternalClasses(e));
      throw remote;
    }
  }


  private static final Field causeFieldOfThrowable;

  static {
    Field causeField = null;
    for (Field f : Throwable.class.getDeclaredFields()) {
      if (f.getName().equals("cause")) {
        f.setAccessible(true);
        causeField = f;
        break;
      }
    }
    causeFieldOfThrowable = causeField;
  }


  private static Throwable excludeInternalClasses(Throwable root) {
    Throwable parent = null;
    Throwable exception = root;
    while (exception != null) {
      if (exception.getClass().getClassLoader() instanceof ClassLoaderBase) {
        Throwable substitution;
        if (exception instanceof XynaException) {
          substitution = new XMCP_RMIExceptionWrapper(exception.getMessage());
        } else {
          substitution = new RuntimeException(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
        }
        if (exception.getCause() != null) {
          substitution.initCause(exception.getCause());
        }
        substitution.setStackTrace(exception.getStackTrace());
        if (parent == null) {
          root = substitution;
        } else {
          try {
            causeFieldOfThrowable.set(parent, substitution);
          } catch (IllegalArgumentException e1) {
            throw new RuntimeException(e1);
          } catch (IllegalAccessException e1) {
            throw new RuntimeException(e1);
          }
        }
        exception = substitution;
      }
      parent = exception;
      exception = exception.getCause();
    }
    return root;
  }


  public static Role authenticate(XynaCredentials credentials) throws RemoteException {
    if (credentials instanceof XynaUserCredentials) {
      return authenticate(((XynaUserCredentials) credentials).getUserName(), ((XynaUserCredentials) credentials).getPassword());
    } else if (credentials instanceof XynaPlainSessionCredentials) {
      return authenticateSession(((XynaPlainSessionCredentials) credentials).getSessionId(), ((XynaPlainSessionCredentials) credentials).getToken(), true);
    } else {
      throw new RemoteException("Could not authenticate credentials of type: '" + credentials.getClass().getSimpleName() + "'"); 
    }
  }
  
  protected Role authenticate(XynaPlainSessionCredentials credentials, boolean countsAsInteraction) throws RemoteException {
    return authenticateSession(credentials.getSessionId(), credentials.getToken(), countsAsInteraction);
  }

  protected XynaMultiChannelPortal.Identity getIdentity(XynaCredentials credentials) throws RemoteException {
    if (credentials instanceof XynaUserCredentials) {
      return new XynaMultiChannelPortal.Identity(((XynaUserCredentials) credentials).getUserName(), null);
    } else if (credentials instanceof XynaPlainSessionCredentials) {
      return new XynaMultiChannelPortal.Identity(null, ((XynaPlainSessionCredentials) credentials).getSessionId());
    } else {
      throw new RemoteException("Could not get identity for credentials of type: '" + credentials.getClass().getSimpleName() + "'"); 
    }
  }
  

  protected static Role authenticate(String publicIdentity, String privateIdentity) throws RemoteException {
    return authenticate(publicIdentity, privateIdentity, false);
  }


  public static Role authenticate(String publicIdentity, String privateIdentity, boolean passwordExpirationAllowed) throws RemoteException {
    try {
      return UserAuthentificationMethod.startAuthenticationProcess(publicIdentity, privateIdentity);
    } catch (XFMG_PasswordExpiredException e) {
      if (passwordExpirationAllowed) {
        //das Passwort darf (z.B. bei changePassword) bereits abgelaufen sein
        //daher nun die Rolle ohne Authentifizierung ermitteln
        return getRoleWithoutAuthentication(publicIdentity);
      } else {
        logSessionAuthenticationProblem(publicIdentity, e, true);
        throw new RuntimeException(); // this is unreachable but a necessity for the compiler
      }
    } catch (XFMG_UserAuthenticationFailedException e) {
      logSessionAuthenticationProblem(publicIdentity, e, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    } catch (XFMG_UserIsLockedException e) {
      logSessionAuthenticationProblem(publicIdentity, e, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logSessionAuthenticationProblem(publicIdentity, t, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    }
  }

  private static Role getRoleWithoutAuthentication(String userName) throws RemoteException {
    try {
      User user = XynaFactory.getPortalInstance().getFactoryManagementPortal().getUser(userName);
      return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().resolveRole(user.getRole());
    } catch (PersistenceLayerException e) {
      logSessionAuthenticationProblem(userName, e, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logSessionAuthenticationProblem(userName, t, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    }
  }


  protected static Role authenticateSession(String publicIdentity, String privateIdentity, boolean countsAsInteraction) throws RemoteException {
    try {
      Role role = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().authenticateSession(publicIdentity, privateIdentity, countsAsInteraction);
      if (role == null) {
        logSessionAuthenticationProblem(publicIdentity, null, true);
      }
      return role;
    } catch (XynaException e) {
      logSessionAuthenticationProblem(publicIdentity, e, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logSessionAuthenticationProblem(publicIdentity, t, true);
      throw new RuntimeException(); // this is unreachable but a necessity for the compiler
    }
  }


  private static void logSessionAuthenticationProblem(String publicIdentity, Throwable cause, boolean throwRemoteException) throws RemoteException {
    if (logger.isDebugEnabled() && !unexpectedSessionAuthentificationException(cause)) {
      logger.debug("Could not authenticate '" + publicIdentity + "'");
    }
    if (throwRemoteException) {
      if (XynaProperty.USE_OLD_EXCEPTIONHANDLING_AUTHENTICATION.get() && cause instanceof XynaException) {
        if (cause instanceof XFMG_DuplicateSessionException) {
          throw new RemoteException("Could not authenticate user '" + publicIdentity + "'. Duplicate Session!");
        }
        transformXynaException((XynaException) cause);
      }
      if (cause instanceof XFMG_DuplicateSessionException) {
        //wird als einzige login exception anders behandelt und z.b. in gui anders dargestellt
        throw new RemoteException("Could not authenticate user '" + publicIdentity + "'. Duplicate Session!");
      }
      if (cause instanceof XFMG_UserIsLockedException && XynaProperty.AUTHENTICATION_LOCKED_EXCEPTION_DIFFERENT.get()) {
        throw new RemoteException("Could not authenticate user '" + publicIdentity + "'. User is locked!");
      }
      if (cause instanceof XFMG_PasswordExpiredException) {
        throw new RemoteException("Could not authenticate user '" + publicIdentity + "'. Password expired!");
      }
      if (logger.isWarnEnabled() && unexpectedSessionAuthentificationException(cause)) {
        logger.warn("Authentification probem.", cause);
      }
      throw new RemoteException("Could not authenticate '" + publicIdentity + "'");
    }
    if (logger.isWarnEnabled() && unexpectedSessionAuthentificationException(cause)) {
      logger.warn("Authentification probem.", cause);
    }
  }


  private static boolean unexpectedSessionAuthentificationException(Throwable cause) {
    return !(cause instanceof XFMG_DuplicateSessionException || cause instanceof XFMG_UserIsLockedException
        || cause instanceof XFMG_PasswordExpiredException || cause instanceof XFMG_UserAuthenticationFailedException);
  }

  private void tryLock(CommandControl.Operation operation, RuntimeContext runtimeContext) throws RemoteException {
    try {
      CommandControl.tryLock(operation, runtimeContext);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }

  private void unlock(CommandControl.Operation operation, RuntimeContext runtimeContext) throws RemoteException {
    try {
      CommandControl.unlock(operation, runtimeContext);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  

  /**
   * Removes a cronLokeOrder with the specified id
   * @return true if the order was successfully removed, false otherwise
   */
  public boolean removeCronLikeOrder(String user, String password, Long id) throws RemoteException {
    return removeCronLikeOrderInternally(id, authenticate(user, password));
  }


  protected boolean removeCronLikeOrderInternally(Long id, Role role) throws RemoteException {
    try {
      return getSecuredPortal().removeCronLikeOrder(id, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }

  
  public Map<Long, CronLikeOrderInformation> listCronLikeOrders(String user, String password, int maxRows) throws RemoteException {
    return listCronLikeOrdersInternally(maxRows, authenticate(user, password));
  }
  
  
  protected Map<Long, CronLikeOrderInformation> listCronLikeOrdersInternally(int maxRows, Role role) throws RemoteException {
    try {
      return getSecuredPortal().getAllCronLikeOrders(maxRows, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  

  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio,
                         String custom1, String custom2, String custom3, String custom4, AcknowledgableObject acknowledgableObject) throws XynaException, RemoteException {
    CustomStringContainer customStrings = new CustomStringContainer(custom1, custom2, custom3, custom4);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, payload);
    xocp.setAcknowledgableObject(acknowledgableObject);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticate(user, password));
  }
  
  /**
   * @return the order id of the started order
   */
  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio,
                         String custom0, String custom1, String custom2, String custom3) throws RemoteException {

    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticate(user, password));
  }


  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio,
                         String custom0, String custom1, String custom2, String custom3, String sessionId)
                  throws RemoteException {

    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, sessionId, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticate(user, password));
  }


  /**
   * @return the order id of the started order
   */
  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio)
                  throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticate(user, password));
  }


  public GeneralXynaObject startOrderSynchronously(String user, String password, GeneralXynaObject payload,
                                                   String orderType, int prio, String custom0, String custom1,
                                                   String custom2, String custom3) throws RemoteException {
    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderSynchronouslyInternally(xocp, authenticate(user, password));
  }


  public GeneralXynaObject startOrderSynchronously(String user, String password, GeneralXynaObject payload,
                                                   String orderType, int prio, String custom0, String custom1,
                                                   String custom2, String custom3, String sessionId)
                  throws RemoteException {

    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, sessionId, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderSynchronouslyInternally(xocp, authenticate(user, password));
  }


  public GeneralXynaObject startOrderSynchronously(String user, String password, GeneralXynaObject payload,
                                                   String orderType, int prio) throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderSynchronouslyInternally(xocp, authenticate(user, password));
  }


  public void addTrigger(String user, String password, String name, RemoteInputStream jarFiles,
                         String fqTriggerClassName, String[] sharedLibs, String description,
                         String startParameterDocumentation, long revision) throws RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(user, null));
    try {
      addTriggerInternally(name, jarFiles, fqTriggerClassName, sharedLibs, description, startParameterDocumentation,
                           authenticate(user, password), revision);
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void addTriggerInternally(String name, RemoteInputStream jarFiles, String fqTriggerClassName,
                                      String[] sharedLibs, String description, String startParameterDocumentation,
                                      Role role, long revision) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.TRIGGER_ADD);
    try {
      getSecuredPortal().addTrigger(name, new ZipInputStream(RemoteInputStreamClient.wrap(jarFiles)),
                                    fqTriggerClassName, sharedLibs, description, startParameterDocumentation, role, revision);
      // getMultiChannelPortal().addTrigger(name, new ZipInputStream(RemoteInputStreamClient.wrap(jarFiles)),
      // fqTriggerClassName, sharedLibs, description, startParameterDocumentation);
    } catch (XynaException e) {
      transformXynaException(e);
    } catch (IOException e) {
      throw new RemoteException(e.getMessage(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_ADD);
    }
  }

  

  public void deployTrigger(String user, String password, String nameOfTrigger, String nameOfTriggerInstance,
                            String[] startParameter, String description, long revision) throws RemoteException {

    deployTriggerInternally(nameOfTrigger, nameOfTriggerInstance, startParameter, description, authenticate(user,
                                                                                                            password), revision);

  }


  protected void deployTriggerInternally(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter,
                                         String description, Role role, long revision) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.TRIGGER_DEPLOY);
    try {
      getSecuredPortal().deployTrigger(nameOfTrigger, nameOfTriggerInstance, startParameter, description, role, revision);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_DEPLOY);
    }
  }


  public void addFilter(String user, String password, String filterName, RemoteInputStream jarFiles,
                        String fqFilterClassName, String triggerName, String[] sharedLibs, String description, long revision)
                  throws RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(user, null));
    try {
      addFilterInternally(filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs, description,
                          authenticate(user, password), revision);
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void addFilterInternally(String filterName, RemoteInputStream jarFiles, String fqFilterClassName,
                                     String triggerName, String[] sharedLibs, String description, Role role, long revision)
                  throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.FILTER_ADD);
    try {
      getSecuredPortal().addFilter(filterName, new ZipInputStream(RemoteInputStreamClient.wrap(jarFiles)),
                                   fqFilterClassName, triggerName, sharedLibs, description, role, revision);
    } catch (XynaException e) {
      transformXynaException(e);
    } catch (IOException e) {
      throw new RemoteException(e.getMessage(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_ADD);
    }
  }


  public void deployFilter(String user, String password, String filtername, String nameOfFilterInstance,
                           String nameOfTriggerInstance, String description, long revision) throws RemoteException {

    deployFilterInternally(filtername, nameOfFilterInstance, nameOfTriggerInstance, description, authenticate(user,
                                                                                                              password), revision);

  }


  protected void deployFilterInternally(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance,
                                        String description, Role role, long revision) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.FILTER_DEPLOY);
    try {
      getSecuredPortal().deployFilter(filtername, nameOfFilterInstance, nameOfTriggerInstance, description, role, revision);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_DEPLOY);
    }

  }

  @Deprecated
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                        Long startTime, Long interval, boolean enabled, String onError) throws RemoteException {

    return startCronLikeOrder(user, password, label, payload, orderType, startTime, Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null, null);
  }
  
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Long startTime, String timeZoneID, Long interval, boolean useDST, boolean enabled, String onError, String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3) throws RemoteException {

    try {
      GeneralXynaObject payloadXynaObject = null;
      if (payload != null) {
        payloadXynaObject = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      } else {
        payloadXynaObject = new Container();
      }
      CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter(label, orderType, startTime, timeZoneID, interval, useDST, enabled, OnErrorAction.valueOf(onError.toUpperCase()), cloCustom0, cloCustom1, cloCustom2, cloCustom3, payloadXynaObject);
      clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
      CronLikeOrder startedCronLikeOrder = startCronLikeOrderInternally(clocp, authenticate(user, password));
      return startedCronLikeOrder.getId();
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }

  }
  
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled, String onError, String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3) throws RemoteException {
    return startCronLikeOrder(user, password, label, payload, orderType, startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone().getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3);
  }
  
  @Deprecated
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Long startTime, Long interval, boolean enabled, String onError, String application, String version) throws RemoteException {
    return startCronLikeOrder(user, password, label, payload, orderType, startTime, Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null, null, application, version);
  }


  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Long startTime, String timeZoneID, Long interval, boolean useDST, boolean enabled,
                                 String onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                 String cloCustom3, String application, String version) throws RemoteException {

    long revision;
    try {
      revision =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement()
              .getRevision(application, version);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      throw new RemoteException("Application and/or version not found: " + application + ", " + version, e1);
    }
    try {
      GeneralXynaObject payloadXynaObject = null;
      if (payload != null) {
        payloadXynaObject = XynaObject.generalFromXml(payload, revision);
      } else {
        payloadXynaObject = new Container();
      }
      
      CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter(label, orderType, startTime, timeZoneID, interval, useDST,
                                                                                enabled, OnErrorAction.valueOf(onError.toUpperCase()), cloCustom0, cloCustom1, cloCustom2, cloCustom3,
                                                                                payloadXynaObject);
      clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
      clocp.setDestinationKey(new DestinationKey(orderType, application, version));
      CronLikeOrder startedCronLikeOrder = startCronLikeOrderInternally(clocp, authenticate(user, password));
      return startedCronLikeOrder.getId();
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled, String onError, String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3, String application, String version) throws RemoteException {
    return startCronLikeOrder(user, password, label, payload, orderType, startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone().getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, application, version);
  }


  @Deprecated //and not part of the interface?
  public CronLikeOrder startCronLikeOrder(String user, String password, GeneralXynaObject payload, String orderType,
                                        Long startTime, Long interval) throws RemoteException, XynaException {
    CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter(orderType, startTime, interval, payload);
    clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startCronLikeOrderInternally(clocp, authenticate(user, password));

  }


  protected CronLikeOrder startCronLikeOrderInternally(CronLikeOrderCreationParameter clocp, Role role)
                  throws RemoteException {
    tryLock(CommandControl.Operation.CRON_CREATE, clocp.getDestinationKey().getRuntimeContext());
    try {
      return getSecuredPortal().startCronLikeOrder(clocp, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    } finally {
      unlock(CommandControl.Operation.CRON_CREATE, clocp.getDestinationKey().getRuntimeContext());
    }
  }
  

  @Deprecated
  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      Long interval, Boolean enabled, String onError)
      throws RemoteException {
    return modifyCronLikeOrder(user, password, id, label, orderType, payload, firstStartupTime,
                               Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null, null);
  }


  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      String timeZoneID, Long interval, Boolean useDST,
                                                      Boolean enabled, String onError, String cloCustom0,
                                                      String cloCustom1, String cloCustom2, String cloCustom3)
      throws RemoteException {
    return modifyCronLikeOrder(user, password, id, label, orderType, payload, firstStartupTime, timeZoneID, interval,
                               useDST, enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, null, null);
  }


  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      String timeZoneID, Long interval, Boolean useDST,
                                                      Boolean enabled, String onError, String cloCustom0,
                                                      String cloCustom1, String cloCustom2, String cloCustom3,
                                                      String applicationName, String versionName)
      throws RemoteException {
    try {
      GeneralXynaObject payloadXynaObject = null;
      if (payload != null) {
        long revision;
        try {
          revision =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement()
                  .getRevision(applicationName, versionName);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RemoteException("Application and/or version not found: " + applicationName + ", " + versionName, e);
        }
        payloadXynaObject = XynaObject.generalFromXml(payload, revision);
      }
      if (payloadXynaObject == null) {
        //fromXml kann auch null zurückgeben
        payloadXynaObject = new Container();
      }

      DestinationKey destination = null;
      if (orderType != null) {
        destination = new DestinationKey(orderType, applicationName, versionName);
      }
      return modifyCronLikeOrderInternally(id, label, destination, payloadXynaObject, firstStartupTime, timeZoneID,
                                           interval, useDST, enabled, onError, cloCustom0, cloCustom1, cloCustom2,
                                           cloCustom3, authenticate(user, password));
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }

  }
  

  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload,
                                                      Calendar firstStartupTimeWithTimeZone, Long interval,
                                                      Boolean useDST, Boolean enabled, String onError,
                                                      String cloCustom0, String cloCustom1, String cloCustom2,
                                                      String cloCustom3) throws RemoteException {
    return modifyCronLikeOrder(user, password, id, label, orderType, payload,
                               firstStartupTimeWithTimeZone.getTimeInMillis(), firstStartupTimeWithTimeZone
                                   .getTimeZone().getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                               cloCustom2, cloCustom3);
  }


  @Deprecated
  protected CronLikeOrderInformation modifyCronLikeOrderInternally(Long id, String label, String orderType,
                                                                   GeneralXynaObject payloadXynaObject,
                                                                   Long firstStartupTime, Long interval,
                                                                   Boolean enabled, String onError, Role role)
      throws RemoteException {
    return modifyCronLikeOrderInternally(id, label, new DestinationKey(orderType), payloadXynaObject, firstStartupTime,
                                         Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null,
                                         null, null, role);
  }


  protected CronLikeOrderInformation modifyCronLikeOrderInternally(Long id, String label, DestinationKey destination,
                                                                   GeneralXynaObject payloadXynaObject,
                                                                   Long firstStartupTime, String timeZoneID,
                                                                   Long interval, Boolean useDST, Boolean enabled,
                                                                   String onError, String cloCustom0,
                                                                   String cloCustom1, String cloCustom2,
                                                                   String cloCustom3, Role role) throws RemoteException {    
    if (destination != null) {
      tryLock(CommandControl.Operation.CRON_MODIFY, destination.getRuntimeContext());
    }
    try {
      CronLikeOrder clo =
          getSecuredPortal().modifyCronLikeOrder(id, label, destination, payloadXynaObject, firstStartupTime,
                                                 timeZoneID, interval, useDST, enabled,
                                                 onError == null ? null : OnErrorAction.valueOf(onError.toUpperCase()),
                                                 cloCustom0, cloCustom1, cloCustom2, cloCustom3, role);
      CronLikeOrderInformation cloi = new CronLikeOrderInformation(clo);
      return cloi;
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    } finally {
      if (destination != null) {
        unlock(CommandControl.Operation.CRON_MODIFY, destination.getRuntimeContext());
      }
    }
  }
  
  
  protected CronLikeOrderInformation modifyCronLikeOrderInternally(Long id, String label, String orderType,
                                                                   GeneralXynaObject payloadXynaObject,
                                                                   Calendar firstStartupTimeWithTimeZone,
                                                                   Long interval, Boolean useDST, Boolean enabled,
                                                                   String onError, String cloCustom0,
                                                                   String cloCustom1, String cloCustom2,
                                                                   String cloCustom3, Role role) throws RemoteException {
    return modifyCronLikeOrderInternally(id, label, new DestinationKey(orderType), payloadXynaObject,
                                         firstStartupTimeWithTimeZone.getTimeInMillis(), firstStartupTimeWithTimeZone
                                             .getTimeZone().getID(), interval, useDST, enabled, onError, cloCustom0,
                                         cloCustom1, cloCustom2, cloCustom3, role);
  }
  

  public CronLikeOrderInformation modifyTimeControlledOrder(XynaCredentials credentials, Long id, RemoteCronLikeOrderCreationParameter clocp) throws RemoteException {
    RuntimeContext rc = null;
    boolean rcset = false;
    if (clocp.getDestinationKey() != null && clocp.getDestinationKey().getOrderType().length() > 0) {
      rc = clocp.getDestinationKey().getRuntimeContext();
      rcset = true; //achtung, rc darf hier null sein
      tryLock(CommandControl.Operation.CRON_MODIFY, rc);
    }
    try {
      if (clocp.getInputPayloadAsString() != null) {
        clocp.convertInputPayload();
      }
      CronLikeOrder clo = getSecuredPortal().modifyTimeControlledOrder(id, clocp, authenticate(credentials));
      CronLikeOrderInformation cloi = new CronLikeOrderInformation(clo);
      return cloi;
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    } finally {
      if (rcset) {
        unlock(CommandControl.Operation.CRON_MODIFY, rc);
      }
    }
  }


  public List<ManualInteractionEntry> listManualInteractionEntries(String user, String password) throws RemoteException {
    return listManualInteractionEntriesInternally(authenticate(user, password));
  }


  protected List<ManualInteractionEntry> listManualInteractionEntriesInternally(Role role) throws RemoteException {
    Map<Long, ManualInteractionEntry> mis = null;

    try {
      mis = getSecuredPortal().listManualInteractionEntries(role);
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
    }

    if (mis == null || mis.size() == 0) {
      return new ArrayList<ManualInteractionEntry>();
    }
    List<ManualInteractionEntry> result = new ArrayList<ManualInteractionEntry>();
    for (Long id : mis.keySet()) {
      ManualInteractionEntry entry = new ManualInteractionEntry();
      entry.setID(id);
      entry.setReason(mis.get(id).getReason());
      entry.setTodo(mis.get(id).getTodo());
      entry.setType(mis.get(id).getType());
      entry.setUserGroup(mis.get(id).getUserGroup());
      result.add(entry);
    }
    return result;
  }


  public void processManualInteractionEntry(String user, String password, Long id, String response)
                  throws RemoteException {

    processManualInteractionEntryInternally(id, response, authenticate(user, password));

  }


  protected void processManualInteractionEntryInternally(Long id, String response, Role role) throws RemoteException {
    try {
      getSecuredPortal().processManualInteractionEntry(id, 
                                                       ManualInteractionResponse.getManualInteractionResponseFromXmlName(response).getMDMRepresentation(),
                                                       role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public boolean hasRight(String user, String password, String rightName) throws RemoteException {
    User userObj;
    try {
      userObj = getMultiChannelPortal().authenticateHashed(user, password);
    } catch (XFMG_UserIsLockedException e) {
      logSessionAuthenticationProblem(user, e, false);
      return false;
    } catch (XFMG_UserAuthenticationFailedException e) {
      logSessionAuthenticationProblem(user, e, false);
      return false;
    } catch (PersistenceLayerException e) {
      logSessionAuthenticationProblem(user, e, false);
      return false;
    }

    if (userObj == null) {
      logSessionAuthenticationProblem(user, null, false);
      return false;
    }
    logger.debug("Resolved user to role: " + userObj.getRole());
    try {
      return getMultiChannelPortal().hasRight(rightName, userObj.getRole());
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public OrderInstanceResult search(String user, String password, OrderInstanceSelect select, int maxRows)
                  throws RemoteException {
    return searchInternally(select, maxRows, authenticate(user, password));
  }


  protected OrderInstanceResult searchInternally(OrderInstanceSelect select, int maxRows, Role role)
                  throws RemoteException {
    try {
      return getSecuredPortal().search(select, maxRows, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  public OrderInstanceResult searchOrderInstances(String user, String password, OrderInstanceSelect select, int maxRows, SearchMode searchMode)
                  throws RemoteException {
    return searchOrderInstancesInternally(select, maxRows, searchMode, authenticate(user, password));
  }


  protected OrderInstanceResult searchOrderInstancesInternally(OrderInstanceSelect select, int maxRows, SearchMode searchMode, Role role)
                  throws RemoteException {
    try {
      return getSecuredPortal().searchOrderInstances(select, maxRows, searchMode, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  @Deprecated
  public Triple<String, String, String> getAuditWithApplicationAndVersion(String user, String password, long id) throws RemoteException {
    return getAuditWithApplicationAndVersionInternally(id, authenticate(user, password));
  }

  public SerializablePair<String, RuntimeContext> getAuditWithRuntimeContext(XynaCredentials credentials, long id) throws RemoteException {
    return getAuditWithRuntimeContextInternally(id, authenticate(credentials));
  }

  public AuditInformation getAuditInformation(XynaCredentials credentials, long id) throws RemoteException {
    return getAuditInformationInternally(id, authenticate(credentials));
  }
  
  
  public String getCompleteOrder(String user, String password, long id) throws RemoteException {
    return getAuditWithApplicationAndVersionInternally(id, authenticate(user, password)).getFirst();
  }

  
  protected Triple<String, String, String> getAuditWithApplicationAndVersionInternally(long id, Role role) throws RemoteException {
    try {
      OrderInstanceDetails details = getSecuredPortal().getCompleteOrder(id, role);
      
      ExecutionType type;
      try {
        type = ExecutionType.valueOf(details.getExecutionType());
      } catch (IllegalArgumentException e) {
        type = null;
      }

      if (type == ExecutionType.SERVICE_DESTINATION) {
        return null;
      }

      String xml = details.getAuditDataAsXML();
      if (xml == null || xml.equals("")) {
        return null;
      } else {
        return new Triple<String, String, String>(xml, details.getApplicationName(), details.getVersionName());
      }
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }

  protected SerializablePair<String, RuntimeContext> getAuditWithRuntimeContextInternally(long id, Role role) throws RemoteException {
    try {
      OrderInstanceDetails details = getSecuredPortal().getCompleteOrder(id, role);
      
      ExecutionType type;
      try {
        type = ExecutionType.valueOf(details.getExecutionType());
      } catch (IllegalArgumentException e) {
        type = null;
      }
      if (type == ExecutionType.SERVICE_DESTINATION) {
        return null;
      }
      
      String xml = details.getAuditDataAsXML();
      if (xml == null || xml.length() == 0) {
        return null;
      } else {
        return SerializablePair.of(xml, details.getRuntimeContext());
      }
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }

  protected AuditInformation getAuditInformationInternally(long id, Role role) throws RemoteException {
    try {
      OrderInstanceDetails details = getSecuredPortal().getCompleteOrder(id, role);
      if (details.getAuditDataAsXML() == null || details.getAuditDataAsXML().length() == 0) {
        return null;
      }
      AuditXmlHelper xmlHelper = new AuditXmlHelper();
      EnhancedAudit audit = xmlHelper.auditFromXml(details.getAuditDataAsXML());

      ExecutionType type = ExecutionType.valueOf(details.getExecutionType());
      AuditInformation ai =
          new AuditInformation(audit, audit.getWorkflowContext() != null ? audit.getWorkflowContext() : details.getRuntimeContext(), type);
      if (type == ExecutionType.SERVICE_DESTINATION) {
        String serviceoperation = details.getOrderType().substring(details.getOrderType().lastIndexOf('.') + 1);

        String pathAndName = details.getAuditDataAsXML();
        final String SERVICE_TAG = "<Service>";
        final String SERVICE_END_TAG = "</Service>";
        int beginIndex = pathAndName.indexOf(SERVICE_TAG);
        int endIndex = pathAndName.indexOf(SERVICE_END_TAG);
        if (beginIndex < 0 || endIndex < 0) {
          throw new XynaException("No Service-Meta-Tag present");
        }
        beginIndex += SERVICE_TAG.length();
        pathAndName = pathAndName.substring(beginIndex, endIndex);
        int sliceIndex = pathAndName.substring(0, pathAndName.lastIndexOf('.')).lastIndexOf('.');
        String serviceName = pathAndName.substring(sliceIndex + 1);
        String servicepath = pathAndName.substring(0, sliceIndex);
        ai.setServiceDestinationInfo(servicepath, serviceName, serviceoperation);
      }
      return ai;
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }

  }
  

  @Deprecated
  public List<String> getMDMs(String user, String password) throws RemoteException {
    return getMDMsInternally(RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  

  protected List<String> getMDMsInternally(RuntimeContext runtimeContext, Role role) throws RemoteException {
    if (role == null) {
      return null;
    }
    
    List<File> files;
    if (runtimeContext instanceof Workspace) {
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision;
      try {
        revision = revisionManagement.getRevision(runtimeContext);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        transformXynaException(e);
        return null;
      }
      String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
      files = FileUtils.getMDMFiles(new File(savedMdmDir), new ArrayList<File>());
    } else if (runtimeContext instanceof Application) {
      files = FileUtils.getMDMFiles(runtimeContext.getName(), ((Application) runtimeContext).getVersionName());
    } else if( runtimeContext instanceof DataModel ) {
      DataModel dataModel = (DataModel)runtimeContext;
      DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
      try {
        files = dmm.getMDMFiles(dataModel.getName());
      } catch( PersistenceLayerException e ) {
        transformXynaException(e);
        return null;
      } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
        transformXynaException(e);
        return null;
      }
    } else {
      throw new UnsupportedOperationException("getMDM is not supported for runtimeContext " + runtimeContext);
    }

    List<String> xmls = new ArrayList<String>();
    
    for (File file : files) {
      try {
        xmls.add(FileUtils.readFileAsString(file));
      } catch (Ex_FileWriteException e) {
        logger.warn("Error while reading MDM",e);
        // just return as much as we can read
      }
    }
    return xmls;
  }
  
  @Deprecated
  public List<String> getMDMs(String user, String password, String application, String version) throws RemoteException {
    RuntimeContext runtimeContext;
    if (application == null || application.length() == 0) {
      runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
    } else {
      runtimeContext = new Application(application, version);
    }
    return getMDMsInternally(runtimeContext, authenticate(user, password));
  }
  
  
  public List<String> getMDMs(XynaCredentials credentials, RuntimeContext runtimeContext) throws RemoteException {
    return getMDMsInternally(runtimeContext, authenticate(credentials));
  }
  
  public boolean changePassword(String user, String password, String id, String oldPassword, String newPassword, boolean isNewPasswordHashed)
                  throws RemoteException {
    boolean passwordExpirationAllowed = user.equals(id); //das eigene Passwort darf auch geändert werden, wenn es bereits abgelaufen ist
    Role role = authenticate(user, password, passwordExpirationAllowed);
    
    return changePasswordInternally(id, oldPassword, newPassword, isNewPasswordHashed, role);
  }


  protected boolean changePasswordInternally(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed, Role role)
                  throws RemoteException {
    try {
      return getSecuredPortal().changePassword(id, oldPassword, newPassword, isNewPasswordHashed, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean changeRole(String user, String password, String id, String name) throws RemoteException {
    logger.debug("id/username: " + id + " name/rolename: " +name);
    return changeRoleInternally(id, name, authenticate(user, password));
  }


  protected boolean changeRoleInternally(String id, String name, Role role) throws RemoteException {
    try {
      return getSecuredPortal().changeRole(id, name, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean createRight(String user, String password, String rightName) throws RemoteException {
    return createRightInternally(rightName, authenticate(user, password));
  }


  protected boolean createRightInternally(String rightName, Role role) throws RemoteException {
    try {
      return getSecuredPortal().createRight(rightName, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean createRole(String user, String password, String name, String domain) throws RemoteException {
    return createRoleInternally(name, domain, authenticate(user, password));

  }


  protected boolean createRoleInternally(String name, String domain, Role role) throws RemoteException {
    try {
      return getSecuredPortal().createRole(name, domain, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean createUser(String user, String password, String id, String roleName, String newPassword,
                            boolean isPassHashed) throws RemoteException {
    return createUserInternally(id, roleName, newPassword, isPassHashed, authenticate(user, password));
  }


  protected boolean createUserInternally(String id, String roleName, String newPassword, boolean isPassHashed, Role role)
                  throws RemoteException {
    try {
      return getSecuredPortal().createUser(id, roleName, newPassword, isPassHashed, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean deleteRight(String user, String password, String rightName) throws RemoteException {

    return deleteRightInternally(rightName, authenticate(user, password));

  }


  protected boolean deleteRightInternally(String rightName, Role role) throws RemoteException {
    try {
      return getSecuredPortal().deleteRight(rightName, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean deleteRole(String user, String password, String name, String domain) throws RemoteException {

    return deleteRoleInternally(name, domain, authenticate(user, password));

  }


  protected boolean deleteRoleInternally(String name, String domain, Role role) throws RemoteException {
    try {
      return getSecuredPortal().deleteRole(name, domain, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean deleteUser(String user, String password, String id) throws RemoteException {

    return deleteUserInternally(id, authenticate(user, password));

  }


  protected boolean deleteUserInternally(String id, Role role) throws RemoteException {
    try {
      return getSecuredPortal().deleteUser(id, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public Collection<Right> getRights(String user, String password) throws RemoteException {
    return getRights(user, password, null);
  }


  public Collection<Right> getRights(String user, String password, String language) throws RemoteException {
    return getRightsInternally(authenticate(user, password), language);
  }


  protected Collection<Right> getRightsInternally(Role role, String language) throws RemoteException {
    try {
      return getSecuredPortal().getRights(role, language);
    } catch (XynaException e) {
      transformXynaException(e);
      return null; //unreachable
    }
  }
  
  
  public Collection<RightScope> getRightScopes(XynaCredentials credentials) throws RemoteException {
    return getRightScopes(credentials, null);
  }


  public Collection<RightScope> getRightScopes(XynaCredentials credentials, String language) throws RemoteException {
    try {
      return getSecuredPortal().getRightScopes(authenticate(credentials), language);
    } catch (XynaException e) {
      transformXynaException(e);
      return null; //unreachable
    }
  }


  public Collection<Role> getRoles(String user, String password) throws RemoteException {

    return getRolesInternally(authenticate(user, password));

  }


  protected Collection<Role> getRolesInternally(Role role) throws RemoteException {
    try {
      return getSecuredPortal().getRoles(role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null; //unreachable
    }
  }


  public Collection<User> getUser(String user, String password) throws RemoteException {
    return getUserInternally(authenticate(user, password));
  }


  protected Collection<User> getUserInternally(Role role) throws RemoteException {
    try {
      List<User> users = new ArrayList<User>(getSecuredPortal().getUser(role));
      for (User user : users) {
        user.clearPassword();
      }
      return users;
    } catch (XynaException e) { 
      transformXynaException(e);
      return null;
    }
  }


  public boolean grantRightToRole(String user, String password, String roleName, String right) throws RemoteException {
    return grantRightToRoleInternally(roleName, right, authenticate(user, password));
  }


  protected boolean grantRightToRoleInternally(String roleName, String right, Role role) throws RemoteException {
    try {
      return getSecuredPortal().grantRightToRole(roleName, right, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean setPassword(String user, String password, String id, String newPassword) throws RemoteException {
    return setPasswordInternally(id, newPassword, authenticate(user, password));
  }


  protected boolean setPasswordInternally(String id, String newPassword, Role role) throws RemoteException {
    try {
      // passwords send over the wire need to be already hashed
      return getSecuredPortal().setPasswordHash(id, newPassword, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean revokeRightFromRole(String user, String password, String roleName, String right)
                  throws RemoteException {
    return revokeRightFromRoleInternally(roleName, right, authenticate(user, password));
  }


  protected boolean revokeRightFromRoleInternally(String roleName, String right, Role role) throws RemoteException {
    try {
      return getSecuredPortal().revokeRightFromRole(roleName, right, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  @Deprecated
  public Map<DestinationKey, DestinationValue> getDestinations(String user, String password, String dispatcher)
                  throws XynaException, RemoteException {
    return getDestinationsInternally(dispatcher, authenticate(user, password));
  }

  
  @Deprecated
  protected Map<DestinationKey, DestinationValue> getDestinationsInternally(String dispatcher, Role role)
                  throws XynaException, RemoteException {
    DispatcherIdentification di = null; 
    try {
      di = DispatcherIdentification.valueOf(dispatcher); //FIXME keine logik in rmi channel
    } catch (IllegalArgumentException e) {
      throw new RemoteException("Invalid dispatcher '" + dispatcher + "'; " + Arrays.toString(DispatcherIdentification.values()) +" are valid names.", e);
    }
    try {
      return getSecuredPortal().getDestinations(di, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  public List<DispatcherEntry> listDestinations(String user, String password, String dispatcher) throws XynaException,
                  RemoteException {
    return listDestinationsInternally(dispatcher, authenticate(user, password));
  }


  protected List<DispatcherEntry> listDestinationsInternally(String dispatcher, Role role) throws XynaException,
                  RemoteException {
    DispatcherIdentification di = null;
    try {
      di = DispatcherIdentification.valueOf(dispatcher); // FIXME keine logik in rmi channel
    } catch (IllegalArgumentException e) {
      throw new RemoteException("Invalid dispatcher '" + dispatcher + "'; " + Arrays.toString(DispatcherIdentification.values()) +" are valid names.", e);
    }
    try {
      return getSecuredPortal().listDestinations(di, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public void removeDestination(String user, String password, String dispatcher, String dk) throws XynaException,
                  RemoteException {
    removeDestinationInternally(dispatcher, dk, authenticate(user, password));
  }


  protected void removeDestinationInternally(String dispatcher, String dk, Role role) throws XynaException,
                  RemoteException {
    DispatcherIdentification di = null; //FIXME keine logik in rmi channel
    try {
      di = DispatcherIdentification.valueOf(dispatcher);
    } catch (IllegalArgumentException e) {
      throw new RemoteException("Invalid dispatcher '" + dispatcher + "'; " + Arrays.toString(DispatcherIdentification.values()) +" are valid names.", e);
    }
    DestinationKey dkey = new DestinationKey(dk);
    CommandControl.tryLock(CommandControl.Operation.DESTINATION_REMOVE, dkey.getRuntimeContext());
    try {
      getSecuredPortal().removeDestination(di, dkey, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.DESTINATION_REMOVE,  dkey.getRuntimeContext());
    }
  }


  public void setDestination(String user, String password, String dispatcher, String dk, String dv)
                  throws XynaException, RemoteException {
    setDestinationInternally(dispatcher, dk, dv, authenticate(user, password));
  }


  protected void setDestinationInternally(String dispatcher, String dk, String dv, Role role) throws XynaException,
                  RemoteException {
    DispatcherIdentification di = null;
    try {
      di = DispatcherIdentification.valueOf(dispatcher); //FIXME keine logik in rmi channel
    } catch (IllegalArgumentException e) {
      throw new RemoteException("Invalid dispatcher '" + dispatcher + "'; " + Arrays.toString(DispatcherIdentification.values()) +" are valid names.",
                                e);
    }

    DestinationKey dkey = new DestinationKey(dk);
    // support other destinations?
    DestinationValue dvalue = new FractalWorkflowDestination(dv);
    CommandControl.tryLock(CommandControl.Operation.DESTINATION_SET,  dkey.getRuntimeContext());
    try {
      getSecuredPortal().setDestination(di, dkey, dvalue, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.DESTINATION_SET,  dkey.getRuntimeContext());
    }
  }

  public void deployMultiple(XynaCredentials credentials, Map<XMOMType, List<String>> deploymentItems, RuntimeContext runtimeContext, String creator) 
                  throws XynaException, RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    try {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(creator, null));
      try {
        getSecuredPortal().deployMultiple(deploymentItems, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, revision, authenticate(credentials));
      } finally {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    }
  }
  
  @Deprecated
  public void deployDatatype(String user, String password, String xml, Map<String, byte[]> libraries)
                  throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(user, null));
    try {
      deployDatatypeInternally(xml, libraries, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, RuntimeContext runtimeContext)
                  throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deployDatatypeInternally(xml, libraries, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

//FIXME keine logik in RMI channel
  protected void deployDatatypeInternally(String xml, Map<String, byte[]> libraries, RuntimeContext runtimeContext, Role role) throws XynaException,
                  RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DATATYPE_DEPLOY, revision);
    try {
      String fqXmlName;
      // save the DOM
      try {
        fqXmlName = saveMDMInternally(xml, runtimeContext, role);
      } catch (Throwable e) {
        throw new RemoteException("Error saving DOM", e);
      }
      
      // deploy it
      try {
        getSecuredPortal().deployDatatype(fqXmlName, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, wrapByteArraysInStreams(libraries), revision, role);
      } catch (XynaException e) {
        transformXynaException(e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DATATYPE_DEPLOY, revision);
    }
  }
  
  private Map<String, InputStream> wrapByteArraysInStreams(Map<String, byte[]> byteArrayMap) {
    Map<String, InputStream> streamedMap = null;
    if (byteArrayMap != null) {
      streamedMap = new HashMap<String, InputStream>();
      for (String filename : byteArrayMap.keySet()) {
        byte[] bytes = byteArrayMap.get(filename);
        if (bytes == null) {
          streamedMap.put(filename, null);
        } else {
          streamedMap.put(filename, new ByteArrayInputStream(bytes));
        }
      }
    }
    return streamedMap;
  }
  
  @Deprecated
  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, boolean override, String user)
                  throws XynaException, RemoteException {
    deployDatatype(credentials, xml, libraries, override, user, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, boolean override, String user,
                             RuntimeContext runtimeContext)
    throws XynaException, RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DATATYPE_DEPLOY, revision);
    try {
      Role role = authenticate(credentials);
      String fqXmlName;
      try {
        fqXmlName = getSecuredPortal().saveMDM(xml, override, user, ensureSessionCredentials(credentials, "deployDatatype").getSessionId(), revision, role);
      } catch (Throwable e) {
        throw new RemoteException("Error saving DOM", e);
      }
      try {
        getSecuredPortal().deployDatatype(fqXmlName, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, wrapByteArraysInStreams(libraries), revision, role);
      } catch (XynaException e) {
        transformXynaException(e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DATATYPE_DEPLOY, revision);
    }
  }

  @Deprecated
  public void deployException(String user, String password, String xml) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(user, null));
    try {
      deployExceptionInternally(xml, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

  public void deployException(XynaCredentials credentials, String xml, RuntimeContext runtimeContext) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deployExceptionInternally(xml, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void deployExceptionInternally(String xml, RuntimeContext runtimeContext, Role role) throws XynaException, RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_EXCEPTION_DEPLOY, revision);
    try {
      String fqXmlName;
      // save the DOM
      try {
        fqXmlName = saveMDMInternally(xml, runtimeContext, role);
      } catch (Throwable e) {
        throw new RemoteException("Error while saving Exception", e);
      }
      // deploy it
      try {
        getSecuredPortal().deployException(fqXmlName, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, revision, role);
      } catch (XynaException e) {
        transformXynaException(e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_EXCEPTION_DEPLOY, revision);
    }
  }
  
  @Deprecated
  public void deployException(XynaCredentials credentials, String xml, boolean override, String user) throws XynaException, RemoteException {
    deployException(credentials, xml, override, user, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public void deployException(XynaCredentials credentials, String xml, boolean override, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_EXCEPTION_DEPLOY, revision);
    try {
      Role role = authenticate(credentials);
      String fqXmlName;
      try {
        fqXmlName = getSecuredPortal().saveMDM(xml, override, user, ensureSessionCredentials(credentials, "deployException").getSessionId(), revision, role);
      } catch (Throwable e) {
        throw new RemoteException("Error while saving Exception", e);
      }
      try {
        getSecuredPortal().deployException(fqXmlName, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, revision, role);
      } catch (XynaException e) {
        transformXynaException(e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_EXCEPTION_DEPLOY, revision);
    }
  }

  @Deprecated
  public void deployWorkflow(String user, String password, String fqClassName) throws XynaException, RemoteException {
    deployWorkflowInternally(fqClassName, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }

  public void deployWorkflow(XynaCredentials credentials, String fqClassName, RuntimeContext runtimeContext) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deployWorkflowInternally(fqClassName, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void deployWorkflowInternally(String fqClassName, RuntimeContext runtimeContext, Role role) throws XynaException, RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    try {
      getSecuredPortal().deployWF(fqClassName, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    }
  }


  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(XynaCredentials credentials,
                                                                                         RuntimeContext runtimeContext)
      throws XynaException, RemoteException {
    Long revision = getRevision(runtimeContext);
    return listDeploymentStatusesInternally(authenticate(credentials), revision);
  }


  public HashMap<String, DeploymentStatus> listDeploymentStatuses(String user, String password) throws XynaException, RemoteException {
    return new HashMap<String, DeploymentStatus>(listDeploymentStatusesInternally(authenticate(user, password),
                                                                                  RevisionManagement.REVISION_DEFAULT_WORKSPACE)
        .get(ApplicationEntryType.WORKFLOW));
  }


  protected Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatusesInternally(Role role, Long revision) throws XynaException,
                  RemoteException {
    return getSecuredPortal().listDeploymentStatuses(role, revision);
  }

  public List<WorkflowInformation> listWorkflows(XynaCredentials credentials) throws XynaException, RemoteException {
    return getSecuredPortal().listWorkflows(authenticate(credentials));
  }
  
  @Deprecated
  public String saveMDM(final String user, String password, String xml) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(user, null));
    try {
      return saveMDMInternally(xml, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

  public String saveMDM(XynaCredentials credentials, String xml, RuntimeContext runtimeContext) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      return saveMDMInternally(xml, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected String saveMDMInternally(String xml, RuntimeContext runtimeContext, Role role) throws XynaException, RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_SAVE, revision);
    try {
      return getSecuredPortal().saveMDM(xml, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_SAVE, revision);
    }
  }
  
  
  public CronLikeOrderSearchResult searchCronLikeOrders(String user, String password, CronLikeOrderSelectImpl selectCron,
                                                            int maxRows) throws RemoteException {
    
    return searchCronLikeOrdersInternally(selectCron, maxRows, authenticate(user, password));
  }
  
  
  protected CronLikeOrderSearchResult searchCronLikeOrdersInternally(CronLikeOrderSelectImpl selectCron,
                                 int maxRows, Role role) throws RemoteException {
    
    CronLikeOrderSearchResult closr = null;
    try {
      closr = getSecuredPortal().searchCronLikeOrders(selectCron, maxRows, role);
    } catch (XynaException e) {
      logger.error("Could not retrieve Crons", e);
      transformXynaException(e);
      return null;
    }
    
    if (closr == null) {
      closr = new CronLikeOrderSearchResult(new ArrayList<CronLikeOrderInformation>(), 0);
    }
    return closr; 
  }
  
  public ManualInteractionResult searchManualInteractions(String user, String password, ManualInteractionSelect selectMI, int maxRows)
                  throws RemoteException {
    return searchManualInteractionsInternally(selectMI, maxRows, authenticate(user, password));
  }


  protected ManualInteractionResult searchManualInteractionsInternally(ManualInteractionSelect selectMI, int maxRows, Role role)
                     throws RemoteException {

     ManualInteractionResult mir = null;
     try {
       mir = getSecuredPortal().searchManualInteractionEntries(selectMI, maxRows, role);
     } catch (XynaException e) {
       logger.error("Could not retrieve MIs", e);
       transformXynaException(e);
       return null;
     }
     
     if (mir == null) {
       return new ManualInteractionResult(new ArrayList<ManualInteractionEntry>(), 0);
     } else {
       return mir;
     }
   }
    
  public Long startOrder(String user, String password, String payload, String orderType, int prio, Long relativeTimeout,
                         CustomStringContainer customs, AcknowledgableObject acknowledgableObject)
                  throws XynaException, RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);
    xocp.setAcknowledgableObject(acknowledgableObject);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }
    if (relativeTimeout != null) {
      xocp.setAbsoluteSchedulingTimeout(System.currentTimeMillis() + relativeTimeout);
      
    }
    try {
      GeneralXynaObject xo = null;
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }
    return startOrderInternally(xocp, authenticate(user, password));     
  }
  
  public Long startOrder(String user, String password, String payload, String orderType, int prio,
                         Long relativeTimeout, CustomStringContainer customs) throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }
    if (relativeTimeout != null) {
      xocp.setAbsoluteSchedulingTimeout(System.currentTimeMillis() + relativeTimeout);
      
    }
    try {
      GeneralXynaObject xo = null;
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }
    return startOrderInternally(xocp, authenticate(user, password));     
  }
  
  public List<String> startOrderSynchronously(String user, String password, String payload, String orderType, int prio,
                                              Long relativeTimeout, CustomStringContainer customs, 
                                              AcknowledgableObject acknowledgableObject) throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);   
    xocp.setAcknowledgableObject(acknowledgableObject);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }
    if (relativeTimeout != null) {
      xocp.setAbsoluteSchedulingTimeout(System.currentTimeMillis() + relativeTimeout);
    }
    try {
      GeneralXynaObject xo = null;
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }

    return startOrderSynchronouslyAndReturnOrderInternally(xocp, authenticate(user, password));
  }

  @Deprecated //was used by gui but got replaced by version with RemoteXynaOrderCreationParams
  public List<String> startOrderSynchronously(String user, String password, String payload, String orderType, int prio,
                                              Long relativeTimeout, CustomStringContainer customs)
                  throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);   
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }
    if (relativeTimeout != null) {
      xocp.setAbsoluteSchedulingTimeout(System.currentTimeMillis() + relativeTimeout);
    }
    try {
      GeneralXynaObject xo = null;
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }

    return startOrderSynchronouslyAndReturnOrderInternally(xocp, authenticate(user, password));
  }
  
  
  protected Long startOrderInternally(XynaOrderCreationParameter xocp, Role role) throws RemoteException {
    RevisionOrderControl.checkRmiClosed(xocp.getDestinationKey().getApplicationName(), xocp.getDestinationKey().getVersionName());
    try {
      xocp.setTransientCreationRole(role);
      return getSecuredPortal().startOrder(xocp, role);
    } catch (XynaRuntimeException e) {
      //wegen abwärtskompatibilität wird in XynaProcessing eine derartige exception in eine runtimeexception gewrapped
      transformXynaException(e.getXynaExceptions().get(0));
      return null;
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  protected GeneralXynaObject startOrderSynchronouslyInternally(XynaOrderCreationParameter xocp, Role role) throws RemoteException {
    try {
      RevisionOrderControl.checkRmiClosed(xocp.getDestinationKey().getApplicationName(), xocp.getDestinationKey().getVersionName());
      xocp.setTransientCreationRole(role);
      return getSecuredPortal().startOrderSynchronously(xocp, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  protected List<String> startOrderSynchronouslyAndReturnOrderInternally(XynaOrderCreationParameter xocp, Role role) throws RemoteException {
    try {
      RevisionOrderControl.checkRmiClosed(xocp.getDestinationKey().getApplicationName(), xocp.getDestinationKey().getVersionName());
      xocp.setTransientCreationRole(role);
      XynaOrderServerExtension xose = getSecuredPortal().startOrderSynchronouslyAndReturnOrder(xocp, role);
      List<String> result = new ArrayList<String>();

      result.add(Long.toString(xose.getId()));

      if (xose.getOutputPayload() != null) {
        result.add(xose.getOutputPayload().toXml());
      }

      return result;
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public SessionCredentials createSession(String user, String password) throws RemoteException {
    return createSession(user, password, true);
  }


  public SessionCredentials createSession(String user, String password, boolean force) throws RemoteException {
    return staticCreateSession(user, password, force);
  }
  
  public static SessionCredentials staticCreateSession(String user, String password, boolean force) throws RemoteException {
    Role role = authenticate(user, password);
    User userObj;
    try {
      userObj = XynaFactory.getInstance().getFactoryManagement().getUser(user);
    } catch (PersistenceLayerException e) {
      logSessionAuthenticationProblem(user, e, true);
      return null; // this is unreachable but necessary for compiling
    }
    try {
      return XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer().getNewSession(userObj, force, role);
    } catch (XynaException e) {
      logSessionAuthenticationProblem(user, e, true);
      return null; // this is unreachable but necessary for compiling
    } catch (Throwable e) {
      Department.handleThrowable(e);
      logSessionAuthenticationProblem(user, e, true);
      return null; // this is unreachable but necessary for compiling
    }
  }
  
  public SessionCredentials createUnauthorizedSession(String user, String password, String sessionUser, boolean force) throws RemoteException {
    try {
      return getSecuredPortal().createSession(new XynaUserCredentials(sessionUser, password /* administrative user's password is used to seed the token*/), 
                                              Optional.<String>empty(), force, authenticate(user, password));
    } catch (XynaException e) {
      logSessionAuthenticationProblem(user, e, true);
      return null; // this is unreachable but necessary for compiling
    } catch (RemoteException e) {
      throw e;
    } catch (Throwable e) {
      Department.handleThrowable(e);
      logSessionAuthenticationProblem(user, e, true);
      return null; // this is unreachable but necessary for compiling
    }
  }


  public boolean authorizeSession(XynaUserCredentials user, String domainOverride, XynaPlainSessionCredentials session) throws RemoteException {
    Role role;
    if (domainOverride != null && domainOverride.length() > 0) {
      Domain domain;
      try {
        domain = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getDomain(domainOverride);
      } catch (PersistenceLayerException e) {
        logger.warn("Session could not be authorized.", e);
        throw new RemoteException("Session could not be authorized.");
      }
      if (domain == null) {
        logger.warn("DomainOverride " + domainOverride + " could not be resolved.");
        throw new RemoteException("Session could not be authorized.");
      } else {
        List<UserAuthentificationMethod> methods = UserAuthentificationMethod.generateAuthenticationMethods(Collections.singletonList(domain));
        try {
          role = UserAuthentificationMethod.executeAuthentificationMethods(user.getUserName(), user.getPassword(), methods);
          //TODO wieso hier nicht die gleichen unterscheidungen wie beim authenticate bzgl der verschiedenen fehlermeldungen?
        } catch (XFMG_UserAuthenticationFailedException e) {
          logger.warn("Session could not be authorized.", e);
          throw new RemoteException("Session could not be authorized.");
        } catch (XFMG_UserIsLockedException e) {
          logger.warn("Session could not be authorized.", e);
          throw new RemoteException("Session could not be authorized.");
        }
      }
    } else {
      role = authenticate(user.getUserName(), user.getPassword());
    }
    try {
      return  getSecuredPortal().authorizeSession(session.getSessionId(), session.getToken(), role.getName(), role);
    } catch (XynaException e) {
      logger.warn("Session could not be authorized.", e);
      transformXynaException(e);
      return Boolean.FALSE;
    } catch (Throwable e) {
      Department.handleThrowable(e);
      logger.warn("Session could not be authorized.", e);
      throw new RemoteException("Session could not be authorized.");
    }
  }


  public void destroySession(String sessionId, String token) throws RemoteException {
    try {
      getSecuredPortal().quitSession(sessionId, authenticateSession(sessionId, token, false));
    } catch (XynaException e) {
      logger.error(e);
      transformXynaException(e);
    }
  }
  
  

  public void terminateSession(XynaCredentials credentials) throws RemoteException {
    destroySession(ensureSessionCredentials(credentials, "terminateSession").getSessionId(), ensureSessionCredentials(credentials, "terminateSession").getToken());
  }



  public SessionDetails getSessionDetails(String sessionId, String token) throws RemoteException {
    try {
      return getSecuredPortal().getSessionDetails(sessionId, authenticateSession(sessionId, token, true));
    } catch (XynaException e) {
      logger.error(e);
      transformXynaException(e);
      return null;
    }
  }


  public void pingSession(String sessionId, String token) throws RemoteException {
    try {
      getSecuredPortal().keepSessionAlive(sessionId, authenticateSession(sessionId, token, true));
    } catch (XynaException e) {
      logger.error(e);
      transformXynaException(e);
    }
  }


  public Role getMyRole(String user, String password) throws RemoteException {
    return authenticate(user, password);
  }


  public OrderInstanceDetails getOrderInstanceDetails(String user, String password, long id) throws RemoteException {
    return getOrderInstanceDetailsInternally(id, authenticate(user, password));
  }


  protected OrderInstanceDetails getOrderInstanceDetailsInternally(long id, Role role) throws RemoteException {
    try {
      OrderInstanceDetails oid = getSecuredPortal().getCompleteOrder(id, role);
      oid.convertAuditDataToXML();
      oid.clearAuditDataJavaObjects();
      return oid;
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public String[] scanLogForLinesOfOrder(String user, String password, long orderId, int lineOffset,
                                         int maxNumberOfLines, String... excludes) throws XynaException, RemoteException {
    return scanLogForLinesOfOrderInternally(orderId, lineOffset, maxNumberOfLines, authenticate(user, password), excludes);
  }


  protected String[] scanLogForLinesOfOrderInternally(long orderId, int lineOffset, int maxNumberOfLines, Role role, String... excludes) throws XynaException,
                  RemoteException {
    return getSecuredPortal().scanLogForLinesOfOrder(orderId, lineOffset, maxNumberOfLines, role, excludes);
  }
  
  
  public String retrieveLogForOrder(String user, String password, long orderId, int lineOffset,
                                         int maxNumberOfLines, String... excludes) throws XynaException, RemoteException {
    return retrieveLogForOrderInternally(orderId, lineOffset, maxNumberOfLines, authenticate(user, password), excludes);
  }


  protected String retrieveLogForOrderInternally(long orderId, int lineOffset, int maxNumberOfLines, Role role, String... excludes) throws XynaException,
                  RemoteException {
    return getSecuredPortal().retrieveLogForOrder(orderId, lineOffset, maxNumberOfLines, role, excludes);
  }


  public long startFrequencyControlledTask(String user, String password,
                                           FrequencyControlledTaskCreationParameter creationParameter)
                  throws XynaException, RemoteException {
    return startFrequencyControlledTaskInternally(creationParameter, authenticate(user, password));
  }
  
  
  protected long startFrequencyControlledTaskInternally(FrequencyControlledTaskCreationParameter creationParameter, Role role)
                  throws XynaException, RemoteException {
    DestinationKey dkey = null;
    if (creationParameter instanceof FrequencyControlledOrderCreationTaskCreationParameter) {
      FrequencyControlledOrderCreationTaskCreationParameter cp = (FrequencyControlledOrderCreationTaskCreationParameter) creationParameter;
      if (cp.getOrderCreationParameter().size() > 0) {
        dkey = cp.getOrderCreationParameter().get(0).getDestinationKey();
      }
    }
    creationParameter.setTransientCreationRole(role);
    if (dkey != null) {
      CommandControl.tryLock(CommandControl.Operation.FREQUENCYCONTROLLED_TASK_START, dkey.getRuntimeContext());
      try {
        return getSecuredPortal().startFrequencyControlledTask(creationParameter);
      } finally {
        CommandControl.unlock(CommandControl.Operation.FREQUENCYCONTROLLED_TASK_START, dkey.getRuntimeContext());
      }
    } else {
      CommandControl.tryLock(CommandControl.Operation.FREQUENCYCONTROLLED_TASK_START);
      try {
        return getSecuredPortal().startFrequencyControlledTask(creationParameter);
      } finally {
        CommandControl.unlock(CommandControl.Operation.FREQUENCYCONTROLLED_TASK_START);
      }
    }
  }


  public boolean cancelFrequencyControlledTask(String user, String password, long taskId) throws XynaException,
                  RemoteException {
    return cancelFrequencyControlledTaskInternally(taskId, authenticate(user, password));
  }


  protected boolean cancelFrequencyControlledTaskInternally(long taskId, Role role) throws XynaException, RemoteException {
    return getSecuredPortal().cancelFrequencyControlledTask(taskId);
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(String user, String password,
                                                                                  long taskId) throws XynaException,
                  RemoteException {
    return getFrequencyControlledTaskInformationInternally(taskId, new String[0], authenticate(user, password));
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(String user, String password,
                                                                                  long taskId, String[] selectedStats)
                  throws XynaException, RemoteException {
    return getFrequencyControlledTaskInformationInternally(taskId, selectedStats, authenticate(user, password));
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformationInternally(
                                                                                            long taskId,
                                                                                            String[] selectedStatistics,
                                                                                            Role role)
                  throws XynaException, RemoteException {
    return getSecuredPortal().getFrequencyControlledTaskInformation(taskId, selectedStatistics, role);
  }


  public Right getRight(String user, String password, String rightidentifier) throws RemoteException {
    return getRight(user, password, null, null);
  }
  
  public Right getRight(String user, String password, String rightidentifier, String language) throws RemoteException {
    return getRightInternally(rightidentifier, authenticate(user, password), language);
  }
  
  protected Right getRightInternally(String rightidentifier, Role role, String language) throws RemoteException {
    try {
      return getSecuredPortal().getRight(rightidentifier, role, language);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }
  

  public Role getRole(String user, String password, String rolename, String domain) throws RemoteException {
    return getRoleInternally(rolename, domain, authenticate(user, password));
  }

  protected Role getRoleInternally(String rolename, String domain, Role role) throws RemoteException {
    try {
      return getSecuredPortal().getRole(rolename, domain, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }
  

  public User getUser(String user, String password, String useridentifier) throws RemoteException {
    return getUserInternally(useridentifier, authenticate(user, password));
  }

  protected User getUserInternally(String useridentifier, Role role) throws RemoteException {
    try {
      User user = getSecuredPortal().getUser(useridentifier, role);
      user.clearPassword();
      return user;
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public UserSearchResult searchUsers(String user, String password, UserSelect selection, int maxRows) throws RemoteException {
    return searchUsersInternally(selection, maxRows, authenticate(user, password));
  }
  
  protected UserSearchResult searchUsersInternally(UserSelect selection, int maxRows, Role role) throws RemoteException {
    try {
      return getSecuredPortal().searchUsers(selection, maxRows, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setAliasOfRole(String user, String password, String rolename, String domainname, String newAlias)
                  throws RemoteException {
    return setAliasOfRoleInternally(rolename, domainname, newAlias, authenticate(user, password));
  }
  
  protected boolean setAliasOfRoleInternally(String rolename, String domainname, String newAlias, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setAliasOfRole(rolename, domainname, newAlias, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setDescriptionOfRole(String user, String password, String rolename, String domainname, String newDescription)
                  throws RemoteException {
    return setDescriptionOfRoleInternally(rolename, domainname, newDescription, authenticate(user, password));
  }
  
  protected boolean setDescriptionOfRoleInternally(String rolename, String domainname, String newDescription, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setDescriptionOfRole(rolename, domainname, newDescription, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setLockedStateOfUser(String user, String password, String useridentifier, boolean newState)
                  throws RemoteException {
    return setLockedStateOfUserInternally(useridentifier, newState, authenticate(user, password));
  }
  
  protected boolean setLockedStateOfUserInternally(String useridentifier, boolean newState, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setLockedStateOfUser(useridentifier, newState, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public Domain getDomain(String user, String password, String domainidentifier) throws RemoteException {
    return getDomainInternally(domainidentifier, authenticate(user, password));
  }
  
  protected Domain getDomainInternally(String domainidentifier, Role role) throws RemoteException {
    try {
      return getSecuredPortal().getDomain(domainidentifier, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public Collection<Domain> getDomains(String user, String password) throws RemoteException {
    return getDomainsInternally(authenticate(user, password));
  }
  
  protected Collection<Domain> getDomainsInternally(Role role) throws RemoteException {
    try {
      return getSecuredPortal().getDomains(role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setDomainsOfUser(String user, String password, String useridentifier, List<String> domains)
                  throws RemoteException {
    return setDomainsOfUserInternally(useridentifier, domains, authenticate(user, password));
  }
  
  protected boolean setDomainsOfUserInternally(String useridentifier, List<String> domains, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setDomainsOfUser(useridentifier, domains);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean createDomain(String user, String password, String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
                  throws RemoteException {
    return createDomainInternally(domainidentifier, type, maxRetries, connectionTimeout, authenticate(user, password));
  }
  
  protected boolean createDomainInternally(String useridentifier, DomainType type, int maxRetries, int connectionTimeout, Role role) throws RemoteException {
    try {
      return getSecuredPortal().createDomain(useridentifier, type, maxRetries, connectionTimeout, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setConnectionTimeoutOfDomain(String user, String password, String domainidentifier,
                                              int connectionTimeout) throws RemoteException {
    return setConnectionTimeoutOfDomainInternally(domainidentifier, connectionTimeout, authenticate(user, password));
  }
  
  protected boolean setConnectionTimeoutOfDomainInternally(String domainidentifier, int connectionTimeout, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setConnectionTimeoutOfDomain(domainidentifier, connectionTimeout, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setDescriptionOfRight(String user, String password, String rightidentifier,
                                       String description) throws RemoteException {
    return setDescriptionOfRight(user, password,rightidentifier, description, null);
  }

  public boolean setDescriptionOfRight(String user, String password, String rightidentifier,
      String description, String language) throws RemoteException {
    return setDescriptionOfRightInternally(rightidentifier, description, authenticate(user, password), language);
  }

  protected boolean setDescriptionOfRightInternally(String rightidentifier, String description, Role role, String language) throws RemoteException {
    try {
      return getSecuredPortal().setDescriptionOfRight(rightidentifier, description, role, language);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }
   

  public boolean setDomainSpecificDataOfDomain(String user, String password, String domainidentifier,
                                               DomainTypeSpecificData specificData) throws RemoteException {
    return setDomainSpecificDataOfDomainInternally(domainidentifier, specificData, authenticate(user, password));
  }

  protected boolean setDomainSpecificDataOfDomainInternally(String domainidentifier, DomainTypeSpecificData specificData, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setDomainSpecificDataOfDomain(domainidentifier, specificData, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }
  

  public boolean setMaxRetriesOfDomain(String user, String password, String domainidentifier, int maxRetries)
                  throws RemoteException {
    return setMaxRetriesOfDomainInternally(domainidentifier, maxRetries, authenticate(user, password));
  }
  
  protected boolean setMaxRetriesOfDomainInternally(String domainidentifier, int maxRetries, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setMaxRetriesOfDomain(domainidentifier, maxRetries, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean setDescriptionOfDomain(String user, String password, String domainidentifier, String description) throws RemoteException {
    return setDescriptionOfDomainInternally(domainidentifier, description, authenticate(user, password));
  }
  
  protected boolean setDescriptionOfDomainInternally(String domainidentifier, String description, Role role) throws RemoteException {
    try {
      return getSecuredPortal().setDescriptionOfDomain(domainidentifier, description, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public boolean deleteDomain(String user, String password, String domainidentifier) throws RemoteException {
    return deleteDomainInternally(domainidentifier, authenticate(user, password));
  }


  protected boolean deleteDomainInternally(String domainidentifier, Role role) throws RemoteException {
    try {
      return getSecuredPortal().deleteDomain(domainidentifier, role);
    } catch (XynaException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public FrequencyControlledTaskSearchResult searchFrequencyControlledTask(String user, String password,
                                                                           FrequencyControlledTaskSelect select,
                                                                           int maxRows) throws RemoteException {
    return searchFrequencyControlledTasksInternally(select, maxRows, authenticate(user, password));
  }


  protected FrequencyControlledTaskSearchResult searchFrequencyControlledTasksInternally(FrequencyControlledTaskSelect select,
                                                                                         int maxRows, Role role)
                  throws RemoteException {
    try {
      return getSecuredPortal().searchFrequencyControlledTasks(select, maxRows);
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
      throw new RemoteException();
    }
  }


  public CancelBean cancelOrder(Long id, Long timeout) throws RemoteException {
    try {
      return getMultiChannelPortal().cancelOrder(id, timeout);
    } catch (XPRC_CancelFailedException e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws RemoteException {
    try {
      return getMultiChannelPortal().cancelOrder(id, timeout, waitForTimeout);
    } catch (XPRC_CancelFailedException e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public RemoteInputStream getServiceImplTemplate(String user, String password, String fqClassNameDOM,
                                                  boolean deleteServiceImplAfterStreamClose) throws RemoteException, XynaException {
    CommandControl.tryLock(CommandControl.Operation.BUILD_SERVICETEMPLATE);
    try {
      InputStream is =
          getSecuredPortal().getServiceImplTemplate(fqClassNameDOM, deleteServiceImplAfterStreamClose, authenticate(user, password));
      return new SimpleRemoteInputStream(is);
    } finally {
      CommandControl.unlock(CommandControl.Operation.BUILD_SERVICETEMPLATE);
    }
  }


  public RemoteInputStream getTriggerImplTemplate(String user, String password, String triggerName,
                                                  boolean deleteTriggerImplAfterStreamClose) throws XynaException, RemoteException {
    CommandControl.tryLock(CommandControl.Operation.BUILD_TRIGGERTEMPLATE);
    try {
      InputStream is =
          getSecuredPortal().getTriggerImplTemplate(triggerName, deleteTriggerImplAfterStreamClose, authenticate(user, password));
      return new SimpleRemoteInputStream(is);
    } finally {
      CommandControl.unlock(CommandControl.Operation.BUILD_TRIGGERTEMPLATE);
    }
  }


  public RemoteInputStream getFilterImplTemplate(String user, String password, String filterName, String fqTriggerClassName,
                                                 boolean deleteFilterImplAfterStreamClose) throws XynaException, RemoteException {
    CommandControl.tryLock(CommandControl.Operation.BUILD_FILTERTEMPLATE);
    try {
      InputStream is =
          getSecuredPortal().getFilterImplTemplate(filterName, fqTriggerClassName, deleteFilterImplAfterStreamClose,
                                                   authenticate(user, password));
      return new SimpleRemoteInputStream(is);
    } finally {
      CommandControl.unlock(CommandControl.Operation.BUILD_FILTERTEMPLATE);
    }
  }


  public void installDeliveryItem(String user, String password, RemoteInputStream deliveryItem,
                                  RemoteOutputStream statusOutputStream, boolean forceOverwrite, boolean dontUpdateMdm,
                                  boolean verboseOutput) throws XynaException, RemoteException {
    CommandControl.tryLock(CommandControl.Operation.PACKAGE_INSTALL);
    try {
      getSecuredPortal().installDeliveryItem(RemoteInputStreamClient.wrap(deliveryItem),
                                             RemoteOutputStreamClient.wrap(statusOutputStream), forceOverwrite,
                                             dontUpdateMdm, verboseOutput, authenticate(user, password));
    } catch (IOException e) {
      throw new RemoteException(e.getMessage(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.PACKAGE_INSTALL);
    }
  }


  public void createDeliveryItem(String user, String password, RemoteInputStream packageDefininition,
                                 RemoteOutputStream deliveryItem, RemoteOutputStream statusOutputStream,
                                 boolean verboseOutput, boolean includeXynaComponents) throws XynaException,
      RemoteException {
    CommandControl.tryLock(CommandControl.Operation.PACKAGE_BUILD);
    try {
      getSecuredPortal().createDeliveryItem(RemoteInputStreamClient.wrap(packageDefininition),
                                            RemoteOutputStreamClient.wrap(deliveryItem),
                                            RemoteOutputStreamClient.wrap(statusOutputStream), verboseOutput,
                                            includeXynaComponents, authenticate(user, password));
    } catch (IOException e) {
      throw new RemoteException(e.getMessage(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.PACKAGE_BUILD);
    }
  }


  @Deprecated
  public void undeployDatatype(String user, String password, String originalFqName, boolean recursivly)
      throws RemoteException {
    undeployDatatypeInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }

  public void undeployDatatype(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext)
                  throws RemoteException {
    undeployDatatypeInternally(originalFqName, recursivly, runtimeContext, authenticate(credentials));
  }


  protected void undeployDatatypeInternally(String originalFqName, boolean recursivly, RuntimeContext runtimeContext, Role role)
      throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DATATYPE_UNDEPLOY, revision);
    try {
      getSecuredPortal().undeployMDM(originalFqName, recursivly, false, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DATATYPE_UNDEPLOY, revision);
    }
  }

  @Deprecated
  public void undeployException(String user, String password, String originalFqName, boolean recursivly)
      throws RemoteException {
    undeployExceptionInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  
  public void undeployException(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext)
                  throws RemoteException {
    undeployExceptionInternally(originalFqName, recursivly, runtimeContext, authenticate(credentials));
  }


  protected void undeployExceptionInternally(String originalFqName, boolean recursivly, RuntimeContext runtimeContext, Role role)
      throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_EXCEPTION_UNDEPLOY, revision);
    try {
      getSecuredPortal().undeployException(originalFqName, recursivly, false, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_EXCEPTION_UNDEPLOY, revision);
    }
  }

  @Deprecated
  public void undeployWorkflow(String user, String password, String originalFqName, boolean recursivly)
      throws RemoteException {
    undeployWorkflowInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }

  public void undeployWorkflow(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext)
                  throws RemoteException {
    undeployWorkflowInternally(originalFqName, recursivly, runtimeContext, authenticate(credentials));
  }


  protected void undeployWorkflowInternally(String originalFqName, boolean recursivly, RuntimeContext runtimeContext, Role role)
      throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_UNDEPLOY, revision);
    try {
      getSecuredPortal().undeployWF(originalFqName, recursivly, false, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_UNDEPLOY, revision);
    }
  }

  public void undeployXMOMObject(XynaCredentials credentials, String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, RuntimeContext runtimeContext)
                  throws RemoteException {
    Long revision = getRevision(runtimeContext);
    
    CommandControl.tryLock(CommandControl.Operation.XMOM_UNDEPLOY, revision);
    try {
      getSecuredPortal().undeployXMOMObject(originalFqName, type, dependentObjectMode, false, revision, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_UNDEPLOY, revision);
    }
  }

  @Deprecated
  public void deleteDatatype(String user, String password, String originalFqName, boolean recursivlyUndeploy,
                             boolean recursivlyDelete) throws RemoteException {
    deleteDatatypeInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  
  public void deleteDatatype(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy,
                             boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deleteDatatypeInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void deleteDatatypeInternally(String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete,
                                          RuntimeContext runtimeContext, Role role) throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DATATYPE_DELETE, revision);
    try {
      getSecuredPortal().deleteDatatype(originalFqName, recursivlyUndeploy, recursivlyDelete, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DATATYPE_DELETE, revision);
    }
  }

  @Deprecated
  public void deleteException(String user, String password, String originalFqName, boolean recursivlyUndeploy,
                              boolean recursivlyDelete) throws RemoteException {
    deleteExceptionInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  
  public void deleteException(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy,
                              boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deleteExceptionInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void deleteExceptionInternally(String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete,
                                           RuntimeContext runtimeContext, Role role) throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_EXCEPTION_DELETE, revision);
    try {
      getSecuredPortal().deleteException(originalFqName, recursivlyUndeploy, recursivlyDelete, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_EXCEPTION_DELETE, revision);
    }
  }

  @Deprecated
  public void deleteWorkflow(String user, String password, String originalFqName, boolean recursivlyUndeploy,
                             boolean recursivlyDelete) throws RemoteException {
    deleteWorkflowInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  
  public void deleteWorkflow(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy,
                             boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
    try {
      deleteWorkflowInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, runtimeContext, authenticate(credentials));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  protected void deleteWorkflowInternally(String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete,
                                          RuntimeContext runtimeContext, Role role) throws RemoteException {
    Long revision = getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DELETE, revision);
    try {
      getSecuredPortal().deleteWorkflow(originalFqName, recursivlyUndeploy, recursivlyDelete, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DELETE, revision);
    }
  }

  @Deprecated
  public XMOMDatabaseSearchResult searchXMOMDatabase(String user, String password, List<XMOMDatabaseSelect> select,
                                                     int maxRows) throws RemoteException {
    return searchXMOMDatabaseInternally(select, maxRows, RevisionManagement.DEFAULT_WORKSPACE, authenticate(user, password));
  }
  
  public XMOMDatabaseSearchResult searchXMOMDatabase(XynaCredentials credentials, List<XMOMDatabaseSelect> select,
                                                     int maxRows, RuntimeContext runtimeContext) throws RemoteException {
    return searchXMOMDatabaseInternally(select, maxRows, runtimeContext, authenticate(credentials));
  }


  protected XMOMDatabaseSearchResult searchXMOMDatabaseInternally(List<XMOMDatabaseSelect> select, int maxRows,
                                                                  RuntimeContext runtimeContext, Role role) throws RemoteException {
    Long revision = getRevision(runtimeContext);
    try {
      return getSecuredPortal().searchXMOMDatabase(select, maxRows, revision, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public ExtendedCapacityUsageInformation listExtendedCapacityInformation(String user, String password)
      throws RemoteException {
    return listExtendedCapacityInformationInternally(authenticate(user, password));
  }


  protected ExtendedCapacityUsageInformation listExtendedCapacityInformationInternally(Role role)
      throws RemoteException {
    try {
      return getSecuredPortal().listExtendedCapacityUsageInformation(role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public boolean addCapacity(String user, String password, String name, int cardinality, State state)
      throws RemoteException {
    return addCapacityInternally(name, cardinality, state, authenticate(user, password));
  }


  protected boolean addCapacityInternally(String name, int cardinality, State state, Role role) throws RemoteException {
    try {
      return getSecuredPortal().addCapacity(name, cardinality, state, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public CapacityInformation getCapacityInformation(String user, String password, String name) throws RemoteException {
    return getCapacityInternally(name, authenticate(user, password));
  }


  protected CapacityInformation getCapacityInternally(String name, Role role) throws RemoteException {
    return getSecuredPortal().getCapacityInformation(name, role);
  }


  public boolean removeCapacity(String user, String password, String name) throws RemoteException {
    return removeCapacityInternally(name, authenticate(user, password));
  }


  protected boolean removeCapacityInternally(String name, Role role) throws RemoteException {
    try {
      return getSecuredPortal().removeCapacity(name, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean changeCapacityName(String user, String password, String name, String newName) throws RemoteException {
    return changeCapacityNameInternally(name, newName, authenticate(user, password));
  }


  protected boolean changeCapacityNameInternally(String name, String newName, Role role) throws RemoteException {
    try {
      return getSecuredPortal().changeCapacityName(name, newName, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean changeCapacityCardinality(String user, String password, String name, int cardinality)
      throws RemoteException {
    return changeCapacityCardinalityInternally(name, cardinality, authenticate(user, password));
  }


  protected boolean changeCapacityCardinalityInternally(String name, int cardinality, Role role) throws RemoteException {
    try {
      return getSecuredPortal().changeCapacityCardinality(name, cardinality, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean changeCapacityState(String user, String password, String name, State state) throws RemoteException {
    return changeCapacityStateInternally(name, state, authenticate(user, password));
  }


  protected boolean changeCapacityStateInternally(String name, State state, Role role) throws RemoteException {
    try {
      return getSecuredPortal().changeCapacityState(name, state, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public boolean requireCapacityForWorkflow(String user, String password, String workflowFqName, String capacityName,
                                            int capacityCardinality) throws RemoteException {
    return requireCapacityForWorkflowInternally(workflowFqName, capacityName, capacityCardinality,
                                                authenticate(user, password));
  }


  protected boolean requireCapacityForWorkflowInternally(String workflowFqName, String capacityName,
                                                         int capacityCardinality, Role role) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    try {
      return getSecuredPortal().requireCapacityForWorkflow(workflowFqName, capacityName, capacityCardinality, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    }
  }
  
  public boolean requireCapacityForOrderType(String user, String password, String ordertypeName, String capacityName,
                                            int capacityCardinality) throws RemoteException {
    return requireCapacityForOrderTypeInternally(ordertypeName, capacityName, capacityCardinality,
                                                authenticate(user, password));
  }


  protected boolean requireCapacityForOrderTypeInternally(String ordertypeName, String capacityName,
                                                         int capacityCardinality, Role role) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    try {
      return getSecuredPortal().requireCapacityForOrderType(ordertypeName, capacityName, capacityCardinality, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    }
  }



  public boolean removeCapacityForWorkflow(String user, String password, String workflowFqName, String capacityName)
      throws RemoteException {
    return removeCapacityForWorkflowInternally(workflowFqName, capacityName, authenticate(user, password));
  }


  protected boolean removeCapacityForWorkflowInternally(String workflowFqName, String capacityName, Role role)
      throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_DELETE);
    try {
      return getSecuredPortal().removeCapacityForWorkflow(workflowFqName, capacityName, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_DELETE);
    }
  }


  public List<CapacityMappingStorable> getAllCapacityMappings(String user, String password) throws RemoteException {
    return getAllCapacityMappingsInternally(authenticate(user, password));
  }


  protected List<CapacityMappingStorable> getAllCapacityMappingsInternally(Role role) throws RemoteException {
    return getSecuredPortal().getAllCapacityMappings(role);
  }

  
  public List<SerializablePair<String, Boolean>> listTimeZones() throws RemoteException {
    String timeZoneIDs[] = TimeZone.getAvailableIDs();
    List<SerializablePair<String, Boolean>> timeZoneDSTList = new ArrayList<SerializablePair<String,Boolean>>(timeZoneIDs.length);
    
    for ( int i = 0; i < timeZoneIDs.length; i++ ) {
      TimeZone timeZone = TimeZone.getTimeZone(timeZoneIDs[i]);
      Boolean timeZoneSupportsDST = timeZone.getDSTSavings() != 0;
      SerializablePair<String, Boolean> newEntry = new SerializablePair<String, Boolean>(timeZone.getID(), timeZoneSupportsDST);
      timeZoneDSTList.add(newEntry);
    }
    
    return timeZoneDSTList;
  }

  
  public Collection<VetoInformationStorable> listVetoInformation(String user, String password) throws RemoteException {
    return listVetoInformationInternally(authenticate(user, password));
  }


  protected Collection<VetoInformationStorable> listVetoInformationInternally(Role role) throws RemoteException {
    try {
      return getSecuredPortal().listVetoInformation(role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public DispatcherEntry getDestination(String user, String password, String dispatcher, String destinationkey)
      throws XynaException, RemoteException {
    return getDestinationInternally(dispatcher, destinationkey, authenticate(user, password));
  }


  public DispatcherEntry getDestinationInternally(String dispatcher, String destinationkey, Role role)
      throws XynaException, RemoteException {
    try {
      return getSecuredPortal().getDestination(DispatcherIdentification.valueOf(dispatcher),
                                               new DestinationKey(destinationkey), role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public String getProperty(String user, String password, String key) throws RemoteException {
    return getPropertyInternally(key, authenticate(user, password));
  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String user, String password, String key)
      throws RemoteException {
    try {
      return getSecuredPortal().getPropertyWithDefaultValue(key, authenticate(user, password));
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  protected String getPropertyInternally(String key, Role role) throws RemoteException {
    try {
      return getSecuredPortal().getProperty(key, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public PropertyMap<String, String> getProperties(String user, String password) throws RemoteException {
    return getPropertiesInternally(authenticate(user, password));
  }

  
  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly(String user, String password) throws RemoteException {
    return getPropertiesWithDefaultInternally(authenticate(user, password));
  }
  
  protected Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultInternally(Role role) throws RemoteException {
    try {
      return new ArrayList<XynaPropertyWithDefaultValue>(getSecuredPortal().getPropertiesWithDefaultValuesReadOnly(role));
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  protected PropertyMap<String, String> getPropertiesInternally(Role role) throws RemoteException {
    try {
      return getSecuredPortal().getPropertiesReadOnly(role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public void setProperty(String user, String password, String key, String value) throws RemoteException {
    setPropertyInternally(key, value, authenticate(user, password));

  }


  public void setPropertyInternally(String key, String value, Role role) throws RemoteException {
    try {
      getSecuredPortal().setProperty(key, value, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void setProperty(String user, String password, XynaPropertyWithDefaultValue property) throws RemoteException {
    setPropertyInternally(property, authenticate(user, password));
  }


  public void setPropertyInternally(XynaPropertyWithDefaultValue property, Role role) throws RemoteException {
    try {
      getSecuredPortal().setProperty(property, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void removeProperty(String user, String password, String key) throws RemoteException {
    removePropertyInternally(key, authenticate(user, password));
  }


  public void removePropertyInternally(String key, Role role) throws RemoteException {
    try {
      getSecuredPortal().removeProperty(key, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void createOrdertype(String user, String password, OrdertypeParameter ordertypeParameter)
      throws RemoteException {
    createOrdertypeInternally(ordertypeParameter, authenticate(user, password));
  }


  protected void createOrdertypeInternally(OrdertypeParameter ordertypeParameter, Role role) throws RemoteException {
    tryLock(CommandControl.Operation.ORDERTYPE_CREATE, ordertypeParameter.getRuntimeContext());
    try {
      getSecuredPortal().createOrdertype(ordertypeParameter, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      unlock(CommandControl.Operation.ORDERTYPE_CREATE, ordertypeParameter.getRuntimeContext());
    }
  }


  public void modifyOrdertype(String user, String password, OrdertypeParameter ordertypeParameter)
      throws RemoteException {
    modifyOrdertypeInternally(ordertypeParameter, authenticate(user, password));
  }


  protected void modifyOrdertypeInternally(OrdertypeParameter ordertypeParameter, Role role) throws RemoteException {
    tryLock(CommandControl.Operation.ORDERTYPE_MODIFY, ordertypeParameter.getRuntimeContext());
    try {
      getSecuredPortal().modifyOrdertype(ordertypeParameter, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      unlock(CommandControl.Operation.ORDERTYPE_MODIFY, ordertypeParameter.getRuntimeContext());
    }
  }


  public void deleteOrdertype(String user, String password, OrdertypeParameter ordertypeParameter)
      throws RemoteException {
    deleteOrdertypeInternally(ordertypeParameter, authenticate(user, password));
  }


  protected void deleteOrdertypeInternally(OrdertypeParameter ordertypeParameter, Role role) throws RemoteException {
    tryLock(CommandControl.Operation.ORDERTYPE_DELETE, ordertypeParameter.getRuntimeContext());
    try {
      getSecuredPortal().deleteOrdertype(ordertypeParameter, role);
    } catch (XynaException e) {
      transformXynaException(e);
    } finally {
      unlock(CommandControl.Operation.ORDERTYPE_DELETE, ordertypeParameter.getRuntimeContext());
    }
  }


  public List<OrdertypeParameter> listOrdertypes(String user, String password) throws RemoteException {
    return listOrdertypesInternally(authenticate(user, password), null);
  }

  
  public List<OrdertypeParameter> listOrdertypes(XynaCredentials creds, RuntimeContext runtimeContext) throws RemoteException {
    return listOrdertypesInternally(authenticate(creds), runtimeContext);
  }
  
  
  
  public List<OrdertypeParameter> listOrdertypes(XynaCredentials creds, SearchOrdertypeParameter sop)
                  throws RemoteException {
    Role role = authenticate(creds);
    try {
      return getSecuredPortal().listOrdertypes(role, sop);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  protected List<OrdertypeParameter> listOrdertypesInternally(Role role, RuntimeContext runtimeContext) throws RemoteException {
    try {
      return getSecuredPortal().listOrdertypes(role, runtimeContext);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public ExtendedManualInteractionResult searchExtendedManualInteractions(String user, String password,
                                                                          ManualInteractionSelect selectMI, int maxRows)
                  throws RemoteException {
    return searchExtendedManualInteractionsInternally(selectMI, maxRows, authenticate(user, password));
  }

  
  protected ExtendedManualInteractionResult searchExtendedManualInteractionsInternally(ManualInteractionSelect selectMI, int maxRows, Role role) throws RemoteException {
    ExtendedManualInteractionResult mir = null;
    try {
      mir = getSecuredPortal().searchExtendedManualInteractionEntries(selectMI, maxRows, role);
    } catch (XynaException e) {
      logger.error("Could not retrieve MIs", e);
      transformXynaException(e);
      return null;
    }
    
    if (mir == null) {
      return new ExtendedManualInteractionResult(new ArrayList<ExtendedOutsideFactorySerializableManualInteractionEntry>(), 0);
    } else {
      return mir;
    }
  }


  public void init(Object... initParameters) {
  }
  
  
  public boolean isSessionAlive(String user, String password, String sessionId) throws RemoteException {
    return isSessionAliveInternally(authenticate(user, password), sessionId);
  }


  protected boolean isSessionAliveInternally(Role role, String sessionId) throws RemoteException {
    try {
      return getSecuredPortal().isSessionAlive(sessionId, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public void allocateAdministrativeVeto(String user, String password, String vetoName, String documentation) throws XynaException,
                  RemoteException {
    allocateAdministrativeVetoInternally(authenticate(user, password), vetoName, documentation);
  }
  
  
  protected void allocateAdministrativeVetoInternally(Role role, String vetoName, String documentation) throws RemoteException {
    try {
      getSecuredPortal().allocateAdministrativeVeto(vetoName, documentation, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void freeAdministrativeVeto(String user, String password, String vetoName) throws XynaException,
                  RemoteException {
    freeAdministrativeVetoInternally(authenticate(user, password), vetoName);
  }
  
  
  protected void freeAdministrativeVetoInternally(Role role, String vetoName) throws RemoteException {
    try {
      getSecuredPortal().freeAdministrativeVeto(vetoName, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  
  
  public void setDocumentationOfAdministrativeVeto(String user, String password, String vetoName, String documentation) throws XynaException, RemoteException {
    setDocumentationOfAdministrativeVetoInternally(authenticate(user, password), vetoName, documentation);
  }


  protected void setDocumentationOfAdministrativeVetoInternally(Role role, String vetoName, String documentation) throws XynaException, RemoteException {
    try {
      getSecuredPortal().setDocumentationOfAdministrativeVeto(vetoName, documentation, role);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public VetoSearchResult searchVetos(String user, String password, VetoSelectImpl select, int maxRows)
                  throws XynaException, RemoteException {
    return searchVetosInternally(authenticate(user, password), select, maxRows);
  }
  
  
  protected VetoSearchResult searchVetosInternally(Role role, VetoSelectImpl select, int maxRows) throws XynaException, RemoteException {
    try {
      return getSecuredPortal().searchVetos(select, maxRows, role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }
  
  
  public Map<Long, ClusterInformation> listClusterInstances() throws XynaException, RemoteException {
    try {
      return getSecuredPortal().listClusterInstances();
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public Collection<ApplicationInformation> listApplications(String user, String password) throws RemoteException {
    return listApplicationsInternally(authenticate(user, password));
  }
  
  
  protected Collection<ApplicationInformation> listApplicationsInternally(Role role) throws RemoteException {
    try {
      return getSecuredPortal().listApplications(role);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }

  public List<Capacity> listCapacitiesForOrdertype(String user, String password, String ordertype)
      throws RemoteException {
    return listCapacitiesForOrdertypeInternally(new DestinationKey(ordertype), authenticate(user, password));
  }


  public List<Capacity> listCapacitiesForDestination(XynaCredentials creds, DestinationKey destination)
      throws RemoteException {
    return listCapacitiesForOrdertypeInternally(destination, authenticate(creds));
  }
  
  protected List<Capacity> listCapacitiesForOrdertypeInternally(DestinationKey destination, Role role)
      throws RemoteException {
    return getSecuredPortal().listCapacitiesForOrderType(destination);
  }


  public Long startCronLikeOrder(XynaCredentials credentials, RemoteCronLikeOrderCreationParameter clocp) throws RemoteException {
    tryLock(CommandControl.Operation.CRON_CREATE, clocp.getDestinationKey().getRuntimeContext());
    try {
      clocp.convertInputPayload();
      return getSecuredPortal().startCronLikeOrder(clocp, authenticate(credentials)).getId();
    } catch (XynaException xe) {
      transformXynaException(xe);
      return null;
    } finally {
      unlock(CommandControl.Operation.CRON_CREATE, clocp.getDestinationKey().getRuntimeContext());
    }
  }


  public String killStuckProcess(XynaCredentials credentials, long orderId) throws XynaException,
                  RemoteException {
    try {
      KillStuckProcessBean result = getSecuredPortal().killStuckProcess(orderId, false, AbortionCause.MANUALLY_ISSUED, authenticate(credentials));
      if (result != null) {
        return result.getResultMessage();
      } else {
        return "Received empty result, kill result unknown";
      }
    } catch (XynaException xe) {
      transformXynaException(xe);
      return null;
    }
  }


  public OrderExecutionResponse startOrder(XynaCredentials credentials, RemoteXynaOrderCreationParameter rxocp) throws RemoteException {
    try {
      rxocp.convertInputPayload();
      if (credentials instanceof XynaPlainSessionCredentials) {
        rxocp.setSessionId(((XynaPlainSessionCredentials) credentials).getSessionId());
      }
      Long idOfStartedOrder = startOrderInternally(rxocp, authenticate(credentials));
      return new SuccesfullOrderExecutionResponse(idOfStartedOrder);
    } catch (RemoteException e) {
      //authentication exception werfen
      throw e;
    } catch (Throwable t) {
      return new ErroneousOrderExecutionResponse(t, defaultController);
    }
  }

  public static final ResultController defaultController = new ResultController();
  static {
    defaultController.setDefaultWrappingTypeForExceptions(WrappingType.SIMPLE);
    defaultController.setDefaultWrappingTypeForXMOMTypes(WrappingType.XML);
  }
  
  public OrderExecutionResponse startOrderSynchronously(XynaCredentials credentials, RemoteXynaOrderCreationParameter rxocp)
                  throws RemoteException {
    return startOrderSynchronously(credentials, rxocp, defaultController);
  }


  public OrderExecutionResponse startOrderSynchronously(XynaCredentials credentials,
                                                        RemoteXynaOrderCreationParameter rxocp,
                                                        ResultController controller) throws RemoteException {
    try {
      RevisionOrderControl.checkRmiClosed(rxocp.getDestinationKey().getApplicationName(), rxocp.getDestinationKey().getVersionName());
      rxocp.convertInputPayload();
      if (credentials instanceof XynaPlainSessionCredentials) {
        rxocp.setSessionId(((XynaPlainSessionCredentials) credentials).getSessionId());
      }
      Role role = authenticate(credentials);
      rxocp.setTransientCreationRole(role);
      return getSecuredPortal().startOrderSynchronouslyAndReturnOrder(rxocp, role, controller);
    } catch (RemoteException e) {
      //authentication exception werfen
      throw e;
    } catch (Throwable t) {
      return new ErroneousOrderExecutionResponse(t, controller);
    }
  }

  
  public RefactoringResult refactorXMOM(XynaCredentials credentials, RefactoringActionParameter action) throws RemoteException,
                  XynaException {
    CommandControl.tryLock(CommandControl.Operation.XMOM_REFACTORING);
    try {
      enrichRefactoringActionParameter(credentials, action);
      return getSecuredPortal().refactorXMOM(action, authenticate(credentials));
    } catch (XynaException xe) {
      transformXynaException(xe);
      return null;
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_REFACTORING);
    }
  }


  private void enrichRefactoringActionParameter(XynaCredentials credentials, RefactoringActionParameter action) throws RemoteException {
    XynaPlainSessionCredentials sessionCreds = ensureSessionCredentials(credentials, "refactorXMOM");
    String creator = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
                    .resolveSessionToUser(sessionCreds.getSessionId());
    action.setSessionId(sessionCreds.getSessionId());
    action.setUsername(creator);
  }
  

  public void publish(XynaCredentials credentials, MessageInputParameter message) throws RemoteException, XynaException {
    getSecuredPortal().publish(message, authenticate(credentials));
  }


  public boolean addSubscription(XynaCredentials credentials, MessageSubscriptionParameter subscription)
                  throws RemoteException, XynaException {
    return getSecuredPortal().addSubscription(ensureSessionCredentials(credentials, "addSubscription").getSessionId(), subscription, authenticate(credentials));
  }


  public boolean cancelSubscription(XynaCredentials credentials, Long subscriptionId) throws RemoteException,
                  XynaException {
    return getSecuredPortal().cancelSubscription(ensureSessionCredentials(credentials, "cancelSubscription").getSessionId(), subscriptionId, authenticate(credentials));
  }


  public MessageRetrievalResult fetchMessages(XynaCredentials credentials, Long lastReceivedId) throws RemoteException,
                  XynaException {
    XynaPlainSessionCredentials session = ensureSessionCredentials(credentials, "fetchMessages");
    Role role = authenticate(session, false);
    return getSecuredPortal().fetchMessages(session.getSessionId(), lastReceivedId, role);
  }

  @Deprecated
  public boolean lockXMOM(XynaCredentials credentials, String creator, String path, String type)
                  throws RemoteException, XynaException {
    return lockXMOM(credentials, creator, path, type, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public boolean lockXMOM(XynaCredentials credentials, String creator, String path, String type, RuntimeContext runtimeContext)
                  throws RemoteException, XynaException {
    Long revision = getRevision(runtimeContext);
    return getSecuredPortal().lockXMOM(ensureSessionCredentials(credentials, "lockXMOM").getSessionId(), creator, new Path(path, revision), type, authenticate(credentials));
  }

  @Deprecated
  public boolean unlockXMOM(XynaCredentials credentials, String creator, String path, String type)
                  throws RemoteException, XynaException {
    return unlockXMOM(credentials, creator, path, type, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public boolean unlockXMOM(XynaCredentials credentials, String creator, String path, String type,
                            RuntimeContext runtimeContext) throws RemoteException, XynaException {
    Long revision = getRevision(runtimeContext);
    return getSecuredPortal().unlockXMOM(ensureSessionCredentials(credentials, "unlockXMOM").getSessionId(), creator, new Path(path, revision), type, authenticate(credentials));
  }

  @Deprecated
  public void publishXMOM(XynaCredentials credentials, String creator, String path, String type,
                          String payload, Long autosaveCounter) throws RemoteException, XynaException {
    publishXMOM(credentials, creator, path, type, payload, autosaveCounter, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public void publishXMOM(XynaCredentials credentials, String creator, String path, String type,
                          String payload, Long autosaveCounter, RuntimeContext runtimeContext) throws RemoteException, XynaException {
    Long revision = getRevision(runtimeContext);
    getSecuredPortal().publishXMOM(ensureSessionCredentials(credentials, "publishXMOM").getSessionId(), creator, new Path(path, revision), type, payload, autosaveCounter, authenticate(credentials));
  }

  @Deprecated
  public String saveMDM(XynaCredentials credentials, String xml, boolean override, final String user) throws XynaException,
                  RemoteException {
    return saveMDM(credentials, xml, override, user, RevisionManagement.DEFAULT_WORKSPACE);
  }

  public String saveMDM(XynaCredentials credentials, String xml, boolean override, final String user, RuntimeContext runtimeContext) throws XynaException,
                  RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_SAVE, revision);
    try {
      return getSecuredPortal().saveMDM(xml, override, user, ensureSessionCredentials(credentials, "publishXMOM").getSessionId(), revision, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_SAVE, revision);
    }
  }
  
  @Deprecated
  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user) throws XynaException,
                  RemoteException {
    deleteXMOMObject(credentials, type, originalFqName, recursivlyUndeploy, recursivlyDelete, user, RevisionManagement.DEFAULT_WORKSPACE);
  }
  
  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user,
                               RuntimeContext runtimeContext) throws XynaException,
                  RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DELETE, revision);
    try {
      getSecuredPortal().deleteXMOMObject(type, originalFqName, recursivlyUndeploy, recursivlyDelete, user, ensureSessionCredentials(credentials, "deleteXMOMObject").getSessionId(), revision, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DELETE, revision);
    }
  }

  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName,
                               DependentObjectMode dependentObjectMode, String user,
                               RuntimeContext runtimeContext) throws XynaException, RemoteException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(runtimeContext);
    CommandControl.tryLock(CommandControl.Operation.XMOM_DELETE, revision);
    try {
      String sessionId = null;
      if (credentials instanceof XynaPlainSessionCredentials) {
        sessionId = ensureSessionCredentials(credentials, "deleteXMOMObject").getSessionId();
      }
      getSecuredPortal().deleteXMOMObject(type, originalFqName, dependentObjectMode, user, sessionId, revision, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DELETE, revision);
    }
  }


  public Long startBatchProcess(XynaCredentials credentials, BatchProcessInput input)
                  throws RemoteException, XynaException {
    CommandControl.tryLock(CommandControl.Operation.BATCH_START, input.getMasterOrder().getDestinationKey().getRuntimeContext());
    try {
      return getSecuredPortal().startBatchProcess(input, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.BATCH_START, input.getMasterOrder().getDestinationKey().getRuntimeContext());
    }
  }
  
  public BatchProcessInformation startBatchProcessSynchronous(XynaCredentials credentials, BatchProcessInput input)
      throws RemoteException, XynaException {
    CommandControl.tryLock(CommandControl.Operation.BATCH_START, input.getMasterOrder().getDestinationKey().getRuntimeContext());
    try {
      return getSecuredPortal().startBatchProcessSynchronous(input, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.BATCH_START, input.getMasterOrder().getDestinationKey().getRuntimeContext());
    }
  }
  
  public BatchProcessInformation getBatchProcessInformation(XynaCredentials credentials, Long batchProcessId) throws XynaException,
      RemoteException {
    return getSecuredPortal().getBatchProcessInformation(batchProcessId, authenticate(credentials));
  }
  
  public BatchProcessSearchResult searchBatchProcesses(XynaCredentials credentials, BatchProcessSelectImpl select,
                                                       int maxRows) throws XynaException, RemoteException {
    return getSecuredPortal().searchBatchProcesses(select, maxRows, authenticate(credentials));
  }

  public boolean cancelBatchProcess(XynaCredentials credentials, Long batchProcessId, CancelMode cancelMode) throws XynaException, RemoteException {
    return getSecuredPortal().cancelBatchProcess(batchProcessId, cancelMode, authenticate(credentials));
  }

  public boolean pauseBatchProcess(XynaCredentials credentials, Long batchProcessId) throws XynaException, RemoteException {
    return getSecuredPortal().pauseBatchProcess(batchProcessId, authenticate(credentials));
  }

  public boolean continueBatchProcess(XynaCredentials credentials, Long batchProcessId) throws XynaException, RemoteException {
    return getSecuredPortal().continueBatchProcess(batchProcessId, authenticate(credentials));
  }

  public boolean modifyBatchProcess(XynaCredentials credentials, Long batchProcessId, BatchProcessInput input) throws XynaException, RemoteException {
    BatchProcessSelectImpl select = new BatchProcessSelectImpl();
    select.whereBatchProcessId().isEqual(batchProcessId);
    BatchProcessSearchResult searchBatchProcesses = searchBatchProcesses(credentials, select,1);
    if (searchBatchProcesses.getCount() == 0) {
      throw new RuntimeException("batchprocess not found");
    }
    BatchProcessInformation bpi = searchBatchProcesses.getResult().iterator().next();
    CommandControl.tryLock(CommandControl.Operation.BATCH_MODIFY, bpi.getRuntimeContext());
    try {
      return getSecuredPortal().modifyBatchProcess(batchProcessId, input, authenticate(credentials));
    } finally {
      CommandControl.unlock(CommandControl.Operation.BATCH_MODIFY, bpi.getRuntimeContext());
    }
  }

  
  public void addTimeWindow(XynaCredentials credentials, TimeConstraintWindowDefinition definition) throws XynaException, RemoteException {
    getSecuredPortal().addTimeWindow(definition, authenticate(credentials));
  }

  public void removeTimeWindow(XynaCredentials credentials, String name, boolean force) throws XynaException, RemoteException {
    getSecuredPortal().removeTimeWindow( name, force, authenticate(credentials));
  }

  public void changeTimeWindow(XynaCredentials credentials, TimeConstraintWindowDefinition definition) throws XynaException, RemoteException {
    getSecuredPortal().changeTimeWindow(definition, authenticate(credentials));
  }

  public List<FactoryNodeStorable> getAllFactoryNodes(XynaCredentials credentials) throws RemoteException {
    return getSecuredPortal().getAllFactoryNodes(authenticate(credentials));
  }
  
  
  public void buildApplicationVersion(XynaCredentials credentials, String applicationName, String versionName, String comment) throws XynaException, RemoteException {
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_BUILD);
    try {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
      try {
        getSecuredPortal().buildApplicationVersion(applicationName, versionName, comment, authenticate(credentials));
      } catch (XynaException xe) {
        transformXynaException(xe);
      } finally {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_BUILD);
    }
  }

  public void buildApplicationVersion(XynaCredentials credentials, String applicationName, String versionName, BuildApplicationVersionParameters params) throws XynaException, RemoteException {
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_BUILD);
    try {
      getSecuredPortal().buildApplicationVersion(applicationName, versionName, params, authenticate(credentials));
    } catch (XynaException xe) {
      transformXynaException(xe);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_BUILD);
    }
  }

  @Deprecated
  public void copyApplicationIntoWorkingSet(XynaCredentials credentials, String applicationName, String versionName, String comment,
                                             boolean overrideChanges) throws XynaException, RemoteException {
    //sowohl workingset als auch quell version locken
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET);
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      try {
        getSecuredPortal()
            .copyApplicationIntoWorkingSet(applicationName, versionName, comment, overrideChanges, authenticate(credentials));
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET);
    }
  }

  public void copyApplicationIntoWorkspace(XynaCredentials credentials, String applicationName, String versionName,
                                           CopyApplicationIntoWorkspaceParameters params) throws XynaException, RemoteException {
    Long targetRevision = getRevision(params.getTargetWorkspace());
    //sowohl workspace als auch quell version locken
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, targetRevision);
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      try {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
        try {
          getSecuredPortal()
              .copyApplicationIntoWorkspace(applicationName, versionName, params, authenticate(credentials));
        } catch (XynaException xe) {
          transformXynaException(xe);
        } finally {
          XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
        }
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, targetRevision);
    }
  }


  @Deprecated
  public void clearWorkingSet(XynaCredentials credentials, final boolean ignoreRunningOrders) throws XynaException, RemoteException {
    Role role = authenticate(credentials);
    if (XynaFactory.getInstance().getFactoryManagementPortal().hasRight(UserManagement.Rights.WORKINGSET_MANAGEMENT.toString(), role)) {
      ClearworkingsetImpl cwsi = new ClearworkingsetImpl();
      Clearworkingset paras = new Clearworkingset() { //TODO generierten code anpassen, damit das schöner zu setzen ist....
  
        @Override
        public boolean getForce() {
          return ignoreRunningOrders;
        }
  
      };
      cwsi.execute(new ByteArrayOutputStream(), paras);
    } else {
      throw new XFMG_ACCESS_VIOLATION("clearWorkingSet", role.getName());
    }
  }

  public void clearWorkspace(XynaCredentials credentials, Workspace workspace, ClearWorkspaceParameters params) throws XynaException, RemoteException {
    Role role = authenticate(credentials);
    if (XynaFactory.getInstance().getFactoryManagementPortal().hasRight(UserManagement.Rights.WORKINGSET_MANAGEMENT.toString(), role)) {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(getIdentity(credentials));
      try {
        WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
        workspaceManagement.clearWorkspace(workspace, params);
      } catch (XynaException e) {
        throw transformXynaException(e);
      } finally {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
      }
    } else {
      throw transformXynaException(new XFMG_ACCESS_VIOLATION("clearWorkspace", role.getName()));
    }
  }


  public Collection<CapacityInformation> listCapacityInformation(XynaCredentials credentials) throws RemoteException {
    try {
      return getSecuredPortal().listCapacityInformation(authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public List<SharedLib> listAllSharedLibs(XynaCredentials credentials) throws RemoteException {
    return getSecuredPortal().listAllSharedLibs(authenticate(credentials));
  }

  public List<SharedLib> listSharedLibs(XynaCredentials credentials, RuntimeContext runtimeContext, boolean withContent) throws RemoteException {
    return getSecuredPortal().listSharedLibs(getRevision(runtimeContext), withContent, authenticate(credentials));
  }

  public List<WorkspaceInformation> listWorkspaces(XynaCredentials credentials, boolean includeProblems) throws RemoteException {
    try {
      return getSecuredPortal().listWorkspaces(authenticate(credentials), includeProblems);
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
      return null;
    }
  }

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(XynaCredentials credentials, boolean includeProblems) throws RemoteException {
    return getSecuredPortal().listApplicationDefinitions(authenticate(credentials), includeProblems);
  }

  public CreateWorkspaceResult createWorkspace(XynaCredentials credentials, Workspace workspace) throws RemoteException {
    return createWorkspace(credentials, workspace, null);
  }

  public CreateWorkspaceResult createWorkspace(XynaCredentials credentials, Workspace workspace, String user) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.WORKSPACE_CREATE);
    try {
      return getSecuredPortal().createWorkspace(workspace, user, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.WORKSPACE_CREATE);
    }
  }
  
  public void removeWorkspace(XynaCredentials credentials, Workspace workspace, RemoveWorkspaceParameters params) throws RemoteException {
    Long revision = getRevision(workspace);
    CommandControl.tryLock(Operation.WORKSPACE_REMOVE, revision);
    CommandControl.unlock(Operation.WORKSPACE_REMOVE, revision); //kurz danach wird writelock geholt, das kann nicht upgegraded werden

    try {
      getSecuredPortal().removeWorkspace(workspace, params, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  
  public void instantiateRepositoryAccessInstance(XynaCredentials credentials, InstantiateRepositoryAccessParameters parameters, RuntimeContext runtimeContext) throws RemoteException {
    Long revision = getRevision(runtimeContext);
    try {
      getSecuredPortal().instantiateRepositoryAccessInstance(parameters, revision, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  
  
  public DataModelResult importDataModel(XynaCredentials credentials, ImportDataModelParameters importDataModel) throws RemoteException {
    try {
      return getSecuredPortal().importDataModel(importDataModel, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }
  
  public DataModelResult removeDataModel(XynaCredentials credentials, RemoveDataModelParameters parameters) throws RemoteException {
    try {
      return getSecuredPortal().removeDataModel(parameters, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }
  
  public DataModelResult modifyDataModel(XynaCredentials credentials, ModifyDataModelParameters parameters) throws RemoteException {
    try {
      return getSecuredPortal().modifyDataModel(parameters, authenticate(credentials));
    } catch( XynaException e ) {
      throw transformXynaException(e);
    }
  }
  
  public SearchResult<?> search(XynaCredentials credentials, SearchRequestBean searchRequest ) throws RemoteException {
    try {
      return getSecuredPortal().search(searchRequest, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    } 
  }


  public void defineApplication(XynaCredentials credentials, Workspace workspace, String applicationName, String comment) throws RemoteException {
    defineApplication(credentials, workspace, applicationName, comment, null);
  }

  public void defineApplication(XynaCredentials credentials, Workspace workspace, String applicationName, String comment, String user) throws RemoteException {
    try {
      ramw.defineApplication(workspace, applicationName, comment, user, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }

  public void removeApplicationDefinition(XynaCredentials credentials, String applicationName, RemoveApplicationParameters params) throws RemoteException {
    try {
      ramw.removeApplicationDefinition(applicationName, params, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }

  public void addObjectToApplication(XynaCredentials credentials, Workspace workspace, String objectName,
                                     String applicationName, ApplicationEntryType entryType) throws RemoteException {
    try {
      ramw.addObjectToApplication(workspace, objectName, applicationName, entryType, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public String exportApplication(XynaCredentials credentials, String applicationName,
                                  String versionName, ExportApplicationBuildParameter buildParams)
                  throws RemoteException {
    try {
      return ramw.exportApplication(applicationName, versionName, buildParams, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }


  public void importApplication(XynaCredentials credentials, String fileManagementId, 
                                ImportApplicationParameter importParams) throws RemoteException {
    try {
      ramw.importApplication(fileManagementId, importParams, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }

  /**
   * @param force deprecated
   */
  public void removeObjectFromApplication(XynaCredentials credentials, Workspace workspace, String applicationName,
                                          String objectName, ApplicationEntryType entryType, boolean force) throws RemoteException {
    try {
      ramw.removeObjectFromApplication(workspace, applicationName, objectName, entryType, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public CopyCLOResult copyCronLikeOrders(XynaCredentials credentials, String applicationName, String sourceVersion,
                                          String targetVersion, String id, String[] ordertypes, boolean move,
                                          boolean global) throws RemoteException {
    try {
      return ramw.copyCronLikeOrders(applicationName, sourceVersion, targetVersion, id, ordertypes, move, global, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }


  public void copyOrderTypes(XynaCredentials credentials, String applicationName, String sourceVersion,
                             String targetVersion) throws RemoteException {
    try {
      ramw.copyOrderTypes(applicationName, sourceVersion, targetVersion, authenticate(credentials));
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }

  public SerializablePair<ArrayList<ApplicationEntryStorable>, ArrayList<ApplicationEntryStorable>> listApplicationDetails(XynaCredentials credentials,
                                                                                                                       Workspace workspace,
                                                                                                                       String applicationName,
                                                                                                                       String version,
                                                                                                                       boolean includingDependencies) throws RemoteException {
    try {
      return ramw.listApplicationDetails(workspace, applicationName, version, includingDependencies, authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }


  public List<FilterInformation> listFilters(XynaCredentials credentials) throws RemoteException {
    try {
      return getSecuredPortal().listFilterInformation(authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }


  public List<TriggerInformation> listTriggers(XynaCredentials credentials) throws RemoteException {
    try {
      return getSecuredPortal().listTriggerInformation(authenticate(credentials));
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }


  public boolean modifyTriggerInstanceStatus(XynaCredentials credentials, String triggerInstanceName, boolean enable) throws RemoteException {
    CommandControl.tryLock(CommandControl.Operation.FILTER_INSTANCE_ENABLE);
    try {
      if (enable) {
        return getSecuredPortal().enableTriggerInstance(triggerInstanceName, authenticate(credentials));
      } else {
        return getSecuredPortal().disableTriggerInstance(triggerInstanceName, authenticate(credentials));
      }
    } catch (XynaException e) {
      throw transformXynaException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_INSTANCE_ENABLE);
    }
  }

  public DeploymentMarker createDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws RemoteException {
    try {
      return getSecuredPortal().createDeploymentMarker(authenticate(credentials), marker);
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
  }

  public void deleteDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws RemoteException {
    try {
      getSecuredPortal().deleteDeploymentMarker(authenticate(credentials), marker);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  
  public void modifyDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws RemoteException {
    try {
      getSecuredPortal().modifyDeploymentMarker(authenticate(credentials), marker);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void createOrderInputSource(XynaCredentials credentials, OrderInputSourceStorable inputSource) throws RemoteException {
    try {
      getSecuredPortal().createOrderInputSource(authenticate(credentials), inputSource);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public RemoteXynaOrderCreationParameter generateOrderInput(XynaCredentials credentials, long inputSourceId)
      throws RemoteException {
    return generateOrderInput(credentials, inputSourceId, new OptionalOISGenerateMetaInformation());
  }


  public RemoteXynaOrderCreationParameter generateOrderInput(XynaCredentials credentials, long inputSourceId,
                                                             OptionalOISGenerateMetaInformation parameters)
      throws RemoteException {
    XynaOrderCreationParameter xocp;
    try {
      Role role = authenticate(credentials);
      parameters.setTransientCreationRole(role);
      xocp = getSecuredPortal().generateOrderInput(role, inputSourceId, parameters);
    } catch (XynaException e) {
      throw transformXynaException(e);
    }
    RemoteXynaOrderCreationParameter rxocp = null;
    if (xocp instanceof RemoteXynaOrderCreationParameter) {
      rxocp = (RemoteXynaOrderCreationParameter) xocp;
      if (rxocp.getInputPayloadAsXML() == null) {
        rxocp.setInputPayload(rxocp.getInputPayload());
      }
      rxocp.removeXynaObjectInputPayload();
    } else {
      rxocp = new RemoteXynaOrderCreationParameter(xocp);
    }
    return rxocp;
  }


  public List<PluginDescription> listPluginDescriptions(XynaCredentials credentials, PluginType type) throws RemoteException {
    return getSecuredPortal().listPluginDescriptions(type, authenticate(credentials));
  }


  public void modifyOrderInputSource(XynaCredentials credentials, OrderInputSourceStorable inputSource) throws RemoteException {
    try {
      getSecuredPortal().modifyOrderInputSource(authenticate(credentials), inputSource);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public void deleteOrderInputSource(XynaCredentials credentials, long inputSourceId) throws RemoteException {
    try {
      getSecuredPortal().deleteOrderInputSource(authenticate(credentials), inputSourceId);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }
  
  public PasswordExpiration getPasswordExpiration(XynaCredentials credentials, String userName) throws RemoteException {
    try {
      return getSecuredPortal().getPasswordExpiration(authenticate(credentials), userName);
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }

  public void modifyRuntimeContextDependencies(XynaCredentials credentials, RuntimeDependencyContext owner, List<RuntimeDependencyContext> newDependencies, boolean force, String user) throws XynaException,
      RemoteException {
    try {
      getSecuredPortal().modifyRuntimeContextDependencies(authenticate(credentials), owner, newDependencies, force, user);
    } catch (XynaException e) {
      transformXynaException(e);
    }
  }


  public List<SessionBasedUserContextValue> getUserContextValues(XynaCredentials credentials)
      throws RemoteException {
    authenticate(credentials);
    XynaPlainSessionCredentials sessionCredentials = ensureSessionCredentials(credentials, "setUserContextValue");
    // No need to check rights - the user is always allowed to access his contextsessionCredentials
    try {
      return getSecuredPortal().getUserContextValues(sessionCredentials.getSessionId());
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
      return null;
    }
  }


  public void setUserContextValue(XynaCredentials credentials, String key, String value) throws RemoteException {
    authenticate(credentials);
    XynaPlainSessionCredentials sessionCredentials = ensureSessionCredentials(credentials, "setUserContextValue");
    // No need to check rights - the user is always allowed to access his contextsessionCredentials
    try {
      getSecuredPortal().setUserContextValue(sessionCredentials.getSessionId(), key, value);
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
    }
  }


  public void resetUserContextValues(XynaCredentials credentials) throws RemoteException {
    authenticate(credentials);
    XynaPlainSessionCredentials sessionCredentials = ensureSessionCredentials(credentials, "setUserContextValue");
    // No need to check rights - the user is always allowed to access his contextsessionCredentials
    try {
      getSecuredPortal().resetUserContextValues(sessionCredentials.getSessionId());
    } catch (PersistenceLayerException e) {
      transformXynaException(e);
    }
  }


  public Collection<RemoteDestinationInstanceInformation> listRemoteDestinations(XynaCredentials credentials)
                  throws RemoteException {
    authenticate(credentials);
    RemoteDestinationManagement rdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
    return rdm.listRemoteDestinationInstances();
  }


}
