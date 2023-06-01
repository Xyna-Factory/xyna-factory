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



import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;



/**
 * RMIChannelImple Extension with Methods that support a SessionBased authentication
 */
public class RMIChannelImplSessionExtension extends RMIChannelImpl implements XynaRMIChannel {

  public RMIChannelImplSessionExtension() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
   //siehe super init  
  }


  /**
   * @return the order id of the started order
   */
  public Long sessionStartOrder(String sessionId, String token, GeneralXynaObject payload, String orderType, int prio,
                                String custom0, String custom1, String custom2, String custom3) throws RemoteException {
    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, sessionId, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticateSession(sessionId, token, true));

  }


  /**
   * @return the order id of the started order 0
   */
  public Long sessionStartOrder(String sessionId, String token, GeneralXynaObject payload, String orderType, int prio)
                  throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, sessionId, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderInternally(xocp, authenticateSession(sessionId, token, true));
  }


  public GeneralXynaObject sessionStartOrderSynchronously(String sessionId, String token, GeneralXynaObject payload,
                                                          String orderType, int prio, String custom0, String custom1,
                                                          String custom2, String custom3) throws RemoteException {
    CustomStringContainer customStrings = new CustomStringContainer(custom0, custom1, custom2, custom3);      
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, customStrings, sessionId, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderSynchronouslyInternally(xocp, authenticateSession(sessionId, token, true));
  }


  public GeneralXynaObject sessionStartOrderSynchronously(String sessionId, String token, GeneralXynaObject payload,
                                                          String orderType, int prio) throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return startOrderSynchronouslyInternally(xocp, authenticateSession(sessionId, token, true));
  }


  public void sessionAddTrigger(String sessionId, String token, String name, RemoteInputStream jarFiles,
                                String fqTriggerClassName, String[] sharedLibs, String description,
                                String startParameterDocumentation, long revision) throws RemoteException {
    try {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
      try {
        getSecuredPortal().addTrigger(name, new ZipInputStream(RemoteInputStreamClient.wrap(jarFiles)),
                                      fqTriggerClassName, sharedLibs, description, startParameterDocumentation,
                                      authenticateSession(sessionId, token, true), revision);
      } finally {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
      }
    } catch (XynaException e) {
      transformXynaException(e);
    } catch (IOException e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void sessionDeployTrigger(String sessionId, String token, String nameOfTrigger, String nameOfTriggerInstance,
                                   String[] startParameter, String description, long revision) throws RemoteException {

    deployTriggerInternally(nameOfTrigger, nameOfTriggerInstance, startParameter, description,
                            authenticateSession(sessionId, token, true), revision);

  }


  public void sessionAddFilter(String sessionId, String token, String filterName, RemoteInputStream jarFiles,
                               String fqFilterClassName, String triggerName, String[] sharedLibs, String description, long revision)
                  throws RemoteException {

    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
    try {
      addFilterInternally(filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs, description,
                          authenticateSession(sessionId, token, true), revision);
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }

  }


  public void sessionDeployFilter(String sessionId, String token, String filtername, String nameOfFilterInstance,
                                  String nameOfTriggerInstance, String description, long revision) throws RemoteException {
    deployFilterInternally(filtername, nameOfFilterInstance, nameOfTriggerInstance, description,
                           authenticateSession(sessionId, token, true), revision);

  }


  @Deprecated
  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Long startTime, Long interval, boolean enabled, String onError)
      throws RemoteException {
    return sessionStartCronLikeOrder(sessionId, token, label, payload, orderType, startTime,
                                     Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null,
                                     null);
  }
  
  
  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Long startTime, String timeZoneID, Long interval, boolean useDST,
                                        boolean enabled, String onError, String cloCustom0, String cloCustom1,
                                        String cloCustom2, String cloCustom3) throws RemoteException {
    try {
      GeneralXynaObject payloadXynaObject = null;
      if (payload != null) {
        payloadXynaObject = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      } else {
        payloadXynaObject = new Container();
      }
      CronLikeOrderCreationParameter clocp =
          new CronLikeOrderCreationParameter(label, orderType, startTime, timeZoneID, interval, useDST, enabled,
                                             OnErrorAction.valueOf(onError.toUpperCase()), cloCustom0, cloCustom1,
                                             cloCustom2, cloCustom3, payloadXynaObject);
      clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
      
      CronLikeOrder startedCronLikeOrder = startCronLikeOrderInternally(clocp, authenticateSession(sessionId, token, true));
      return startedCronLikeOrder.getId();
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled,
                                        String onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3) throws RemoteException {
    return sessionStartCronLikeOrder(sessionId, token, label, payload, orderType,
                                     startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone()
                                         .getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                     cloCustom2, cloCustom3);
  }
  

  @Deprecated
  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Long startTime, Long interval)
      throws RemoteException, XynaException {
    return sessionStartCronLikeOrder(sessionId, token, payload, orderType, startTime, Constants.DEFAULT_TIMEZONE,
                                     interval, false, null, null, null, null);
  }
  

  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Long startTime, String timeZoneID,
                                                            Long interval, boolean useDST, String cloCustom0,
                                                            String cloCustom1, String cloCustom2, String cloCustom3)
      throws RemoteException, XynaException {
    CronLikeOrderCreationParameter clocp =
        new CronLikeOrderCreationParameter(orderType, startTime, timeZoneID, interval, useDST, cloCustom0, cloCustom1,
                                           cloCustom2, cloCustom3, payload);
    clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    CronLikeOrder clo = startCronLikeOrderInternally(clocp, authenticateSession(sessionId, token, true));
    CronLikeOrderInformation cloi = new CronLikeOrderInformation(clo);
    return cloi;
  }
  

  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Calendar startTimeWithTimeZone,
                                                            Long interval, boolean useDST, String cloCustom0,
                                                            String cloCustom1, String cloCustom2, String cloCustom3)
      throws RemoteException, XynaException {
    return sessionStartCronLikeOrder(sessionId, token, payload, orderType, startTimeWithTimeZone.getTimeInMillis(),
                                     startTimeWithTimeZone.getTimeZone().getID(), interval, useDST, cloCustom0,
                                     cloCustom1, cloCustom2, cloCustom3);
  }


  @Deprecated
  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             Long interval, boolean enabled, String onError)
      throws XynaException, RemoteException {
    return sessionModifyCronLikeOrder(sessionId, token, id, label, payload, orderType, startTime,
                                      Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null,
                                      null);
  }


  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             String timeZoneID, Long interval, boolean useDST,
                                                             boolean enabled, String onError, String cloCustom0,
                                                             String cloCustom1, String cloCustom2, String cloCustom3)
      throws XynaException, RemoteException {
    return sessionModifyCronLikeOrder(sessionId, token, id, label, payload, orderType, startTime, timeZoneID, interval,
                                      useDST, enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, null,
                                      null);
  }


  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             String timeZoneID, Long interval, boolean useDST,
                                                             boolean enabled, String onError, String cloCustom0,
                                                             String cloCustom1, String cloCustom2, String cloCustom3,
                                                             String applicationName, String versionName)
      throws XynaException, RemoteException {
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
      return modifyCronLikeOrderInternally(id, label, destination,
                                           payloadXynaObject, startTime, timeZoneID, interval, useDST, enabled,
                                           onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3,
                                           authenticateSession(sessionId, token, true));
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }
  }


  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType,
                                                             Calendar startTimeWithTimeZone, Long interval,
                                                             boolean useDST, boolean enabled, String onError,
                                                             String cloCustom0, String cloCustom1, String cloCustom2,
                                                             String cloCustom3) throws XynaException, RemoteException {
    return sessionModifyCronLikeOrder(sessionId, token, id, label, payload, orderType,
                                      startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone()
                                          .getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                      cloCustom2, cloCustom3);
  }


  public boolean sessionRemoveCronLikeOrder(String sessionId, String token, Long id) throws RemoteException {
    return removeCronLikeOrderInternally(id, authenticateSession(sessionId, token, true));
  }


  public Map<Long, CronLikeOrderInformation> sessionListCronLikeOrders(String sessionId, String token, int maxRows) throws RemoteException {
    return listCronLikeOrdersInternally(maxRows, authenticateSession(sessionId, token, true));
  }
  

  public List<ManualInteractionEntry> sessionListManualInteractionEntries(String sessionId, String token)
                  throws RemoteException {
    return listManualInteractionEntriesInternally(authenticateSession(sessionId, token, true));
  }

  public void sessionProcessManualInteractionEntry(String sessionId, String token, Long id, String response)
                  throws RemoteException {
    processManualInteractionEntryInternally(id, response, authenticateSession(sessionId, token, true));
  }


  public boolean sessionHasRight(String sessionId, String token, String rightName) throws RemoteException {
    Role role = authenticateSession(sessionId, token, true);


    if (role == null) {
      logger.warn("Could not authenticate session '" + sessionId + "'");
      return false;
    }
    try {
      return getMultiChannelPortal().hasRight(rightName, role.getName());
    } catch (XynaException e) {
      transformXynaException(e);
      return false;
    }
  }


  public OrderInstanceResult sessionSearch(String sessionId, String token, OrderInstanceSelect select, int maxRows)
                  throws RemoteException {
    return searchInternally(select, maxRows, authenticateSession(sessionId, token, true));
  }
  
  
  public OrderInstanceResult sessionSearchOrderInstances(String sessionId, String token, OrderInstanceSelect select, int maxRows, SearchMode searchMode)
                  throws RemoteException {
    return searchOrderInstancesInternally(select, maxRows, searchMode, authenticateSession(sessionId, token, true));
  }
  
  
  public String sessionGetCompleteOrder(String sessionId, String token, long id) throws RemoteException {
    return getAuditWithApplicationAndVersionInternally(id, authenticateSession(sessionId, token, true)).getFirst();
  }
  
  
  public Triple<String, String, String> sessionGetAuditWithApplicationAndVersion(String sessionId, String token, long id) throws RemoteException {
    return getAuditWithApplicationAndVersionInternally(id, authenticateSession(sessionId, token, true));
  }

  @Deprecated
  public List<String> sessionGetMDMs(String sessionId, String token) throws RemoteException {
    return getMDMsInternally(RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionId, token, true));
  }
  
  @Deprecated
  public List<String> sessionGetMDMs(String sessionId, String token, String application, String version) throws RemoteException {
    RuntimeContext runtimeContext;
    if (application == null || application.length() == 0) {
      runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
    } else {
      runtimeContext = new Application(application, version);
    }
    return getMDMsInternally(runtimeContext, authenticateSession(sessionId, token, true));
  }



  public boolean sessionChangePassword(String sessionId, String token, String id, String oldPassword, String newPassword, boolean isNewPasswordHashed)
                  throws RemoteException {

    return changePasswordInternally(id, oldPassword, newPassword, isNewPasswordHashed, authenticateSession(sessionId, token, true));
  }


  public boolean sessionChangeRole(String sessionId, String token, String id, String name) throws RemoteException {

    return changeRoleInternally(id, name, authenticateSession(sessionId, token, true));

  }


  public boolean sessionCreateRight(String sessionId, String token, String rightName) throws RemoteException {

    return createRightInternally(rightName, authenticateSession(sessionId, token, true));

  }


  public boolean sessionCreateRole(String sessionId, String token, String name, String domain) throws RemoteException {

    return createRoleInternally(name, domain, authenticateSession(sessionId, token, true));

  }


  public boolean sessionCreateUser(String sessionId, String token, String id, String roleName, String newPassword,
                                   boolean isPassHashed) throws RemoteException {
    return createUserInternally(id, roleName, newPassword, isPassHashed, authenticateSession(sessionId, token, true));

  }


  public boolean sessionDeleteRight(String sessionId, String token, String rightName) throws RemoteException {
    return deleteRightInternally(rightName, authenticateSession(sessionId, token, true));

  }


  public boolean sessionDeleteRole(String sessionId, String token, String name, String domain) throws RemoteException {

    return deleteRoleInternally(name, domain, authenticateSession(sessionId, token, true));

  }


  public boolean sessionDeleteUser(String sessionId, String token, String id) throws RemoteException {

    return deleteUserInternally(id, authenticateSession(sessionId, token, true));

  }


  public Collection<Right> sessionGetRights(String sessionId, String token) throws RemoteException {
    return sessionGetRights(sessionId, token, null);
  }

  public Collection<Right> sessionGetRights(String sessionId, String token, String language) throws RemoteException {
    return getRightsInternally(authenticateSession(sessionId, token, true), language);
  }


  public Collection<Role> sessionGetRoles(String sessionId, String token) throws RemoteException {

    return getRolesInternally(authenticateSession(sessionId, token, true));

  }


  public Collection<User> sessionGetUser(String sessionId, String token) throws RemoteException {

    return getUserInternally(authenticateSession(sessionId, token, true));

  }


  public boolean sessionGrantRightToRole(String sessionId, String token, String roleName, String right)
                  throws RemoteException {

    return grantRightToRoleInternally(roleName, right, authenticateSession(sessionId, token, true));

  }


  public boolean sessionSetPassword(String sessionId, String token, String id, String newPassword)
                  throws RemoteException {

    return setPasswordInternally(id, newPassword, authenticateSession(sessionId, token, true));
  }


  public boolean sessionRevokeRightFromRole(String sessionId, String token, String roleName, String right)
                  throws RemoteException {
    return revokeRightFromRoleInternally(roleName, right, authenticateSession(sessionId, token, true));
  }

  
  @Deprecated
  public Map<DestinationKey, DestinationValue> sessionGetDestinations(String sessionId, String token, String dispatcher)
                  throws XynaException, RemoteException {
    return getDestinationsInternally(dispatcher, authenticateSession(sessionId, token, true));
  }
  
  
  public List<DispatcherEntry> sessionListDestinations(String sessionId, String token, String dispatcher)
                  throws XynaException, RemoteException {
    return listDestinationsInternally(dispatcher, authenticateSession(sessionId, token, true));
  }
  
  
  public DispatcherEntry sessionGetDestination(String sessionid, String token, String dispatcher, String destinationkey)
                  throws XynaException, RemoteException {
    return getDestinationInternally(dispatcher, destinationkey, authenticateSession(sessionid, token, true));
  }


  public void sessionRemoveDestination(String sessionId, String token, String dispatcher, String dk)
                  throws XynaException, RemoteException {

    removeDestinationInternally(dispatcher, dk, authenticateSession(sessionId, token, true));
  }


  public void sessionSetDestination(String sessionId, String token, String dispatcher, String dk, String dv)
                  throws XynaException, RemoteException {
    setDestinationInternally(dispatcher, dk, dv, authenticateSession(sessionId, token, true));
  }

  @Deprecated
  public void sessionDeployDatatype(String sessionId, String token, String xml, Map<String, byte[]> libraries)
                  throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
    try {
      deployDatatypeInternally(xml, libraries, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionId, token, true));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

  @Deprecated
  public void sessionDeployException(String sessionId, String token, String xml) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
    try {
      deployExceptionInternally(xml, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionId, token, true));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }

  @Deprecated
  public void sessionDeployWorkflow(String sessionId, String token, String fqClassName) throws XynaException,
                  RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
    try {
      deployWorkflowInternally(fqClassName, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionId, token, true));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  public HashMap<String, DeploymentStatus> sessionListDeploymentStatuses(String sessionId, String token) throws XynaException,
      RemoteException {
    return new HashMap<String, DeploymentStatus>(listDeploymentStatusesInternally(authenticateSession(sessionId, token, true),
                                                                                  RevisionManagement.REVISION_DEFAULT_WORKSPACE)
        .get(ApplicationEntryType.WORKFLOW));
  }

  @Deprecated
  public String sessionSaveMDM(String sessionId, String token, String xml) throws XynaException, RemoteException {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new XynaMultiChannelPortal.Identity(null, sessionId));
    try {
      return saveMDMInternally(xml, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionId, token, true));
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }
  
  
  public ManualInteractionResult sessionSearchManualInteractions(String sessionId, String token,
                                                                 ManualInteractionSelect selectMI, int maxRows)
                  throws RemoteException {
    return searchManualInteractionsInternally(selectMI, maxRows, authenticateSession(sessionId, token, true));
  }


  public Long sessionStartOrder(String sessionId, String token, String payload, String orderType, int prio,
                                Long relativeTimeout, CustomStringContainer customs) throws RemoteException {
    
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    xocp.setSessionId(sessionId);
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }
    Long absoluteTimeout = null;
    if (relativeTimeout != null) {
      absoluteTimeout = System.currentTimeMillis() + relativeTimeout;
      xocp.setAbsoluteSchedulingTimeout(absoluteTimeout);
    }    
    GeneralXynaObject xo = null;
    try {
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }
    
    return startOrderInternally(xocp, authenticateSession(sessionId, token, true));
  }


  public List<String> sessionStartOrderSynchronously(String sessionId, String token, String payload, String orderType,
                                                     int prio, Long relativeTimeout, CustomStringContainer customs)
                  throws RemoteException {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType, prio);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    Long absoluteTimeout = null;
    if (relativeTimeout != null) {
      absoluteTimeout = System.currentTimeMillis() + relativeTimeout;
      xocp.setAbsoluteSchedulingTimeout(absoluteTimeout);
    }    
    xocp.setSessionId(sessionId);
    if (customs != null) {
      xocp.setCustomStringContainer(customs);
    }    
    GeneralXynaObject xo = null;
    try {
      if (payload != null) {
        xo = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        xocp.setInputPayload(xo);
      }
    } catch (XynaException e) {
      transformXynaException(e);
    }

    return startOrderSynchronouslyAndReturnOrderInternally(xocp, authenticateSession(sessionId, token, true)); 
  }


  public OrderInstanceDetails sessionGetOrderInstanceDetails(String sessionid, String token, long id)
                  throws RemoteException {
    return getOrderInstanceDetailsInternally(id, authenticateSession(sessionid, token, true));

  }


  public String[] sessionScanLogForLinesOfOrder(String sessionid, String token, long orderId, int lineOffset,
                                                int maxNumberOfLines, String... excludes) throws XynaException,
                  RemoteException {
    return scanLogForLinesOfOrderInternally(orderId, lineOffset, maxNumberOfLines, authenticateSession(sessionid, token, true), excludes);
  }
  
  
  public String sessionRetrieveLogForOrder(String sessionid, String token, long orderId, int lineOffset,
                                           int maxNumberOfLines, String... excludes) throws XynaException, RemoteException {
    return retrieveLogForOrderInternally(orderId, lineOffset, maxNumberOfLines, authenticateSession(sessionid, token, true), excludes);
  }


  public boolean sessionCancelFrequencyControlledTask(String sessionid, String token, long taskId)
                  throws XynaException, RemoteException {
    return cancelFrequencyControlledTaskInternally(taskId, authenticateSession(sessionid, token, true));
  }


  public FrequencyControlledTaskInformation sessionGetFrequencyControlledTaskInformation(String sessionid,
                                                                                         String token, long taskId)
                  throws XynaException, RemoteException {
    return getFrequencyControlledTaskInformationInternally(taskId, null, authenticateSession(sessionid, token, true));
  }


  public FrequencyControlledTaskInformation sessionGetFrequencyControlledTaskInformation(String sessionid,
                                                                                         String token, long taskId,
                                                                                         String[] selectedStatistics)
                  throws XynaException, RemoteException {
    return getFrequencyControlledTaskInformationInternally(taskId, selectedStatistics, authenticateSession(sessionid, token, true));
  }


  public long sessionStartFrequencyControlledTask(String sessionid, String token,
                                                  FrequencyControlledTaskCreationParameter creationParameter)
                  throws XynaException, RemoteException {
    return startFrequencyControlledTaskInternally(creationParameter, authenticateSession(sessionid, token, true));
  }


  public User sessionGetUser(String sessionid, String token, String useridentifier) throws RemoteException {
    return getUserInternally(useridentifier, authenticateSession(sessionid, token, true));
  }


  public UserSearchResult sessionSearchUsers(String sessionid, String token, UserSelect selection, int maxRows)
                  throws RemoteException {
    try {
      logger.debug("searchUsers: " + selection.getSelectString() + " - " + maxRows);
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RemoteException("searchUsers", e);
    }
    for (int i=0; i<selection.getParameter().size(); i++) {
      logger.debug(selection.getParameter().get(i));
    }  
    return searchUsersInternally(selection, maxRows, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetAliasOfRole(String sessionid, String token, String roleidentifier, String domainname, String newAlias)
                  throws RemoteException {
    return setAliasOfRoleInternally(roleidentifier, domainname, newAlias, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetDescriptionOfRole(String sessionid, String token, String roleidentifier,
                                             String domainname, String newDescription) throws RemoteException {
    return setDescriptionOfRoleInternally(roleidentifier, domainname, newDescription, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetLockedStateOfUser(String sessionid, String token, String useridentifier, boolean newState)
                  throws RemoteException {
    return setLockedStateOfUserInternally(useridentifier, newState, authenticateSession(sessionid, token, true));
  }


  public boolean sessionCreateDomain(String sessionid, String token, String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
                  throws RemoteException {
    return createDomainInternally(domainidentifier, type, maxRetries, connectionTimeout, authenticateSession(sessionid, token, true));
  }


  public Domain sessionGetDomain(String sessionid, String token, String domainidentifier) throws RemoteException {
    return getDomainInternally(domainidentifier, authenticateSession(sessionid, token, true));
  }


  public Collection<Domain> sessionGetDomains(String sessionid, String token) throws RemoteException {
    return getDomainsInternally(authenticateSession(sessionid, token, true));
  }


  public Role sessionGetRole(String sessionid, String token, String rolename, String domainname) throws RemoteException {
    return getRoleInternally(rolename, domainname, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetConnectionTimeoutOfDomain(String sessionid, String token, String domainidentifier,
                                                     int connectionTimeout) throws RemoteException {
    return setConnectionTimeoutOfDomainInternally(domainidentifier, connectionTimeout, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetDescriptionOfRight(String sessionid, String token, String rightidentifier,
                                              String description) throws RemoteException {
    return sessionSetDescriptionOfRight(sessionid, token, rightidentifier, description, null);
  }

  public boolean sessionSetDescriptionOfRight(String sessionid, String token, String rightidentifier,
      String description, String language) throws RemoteException {
    return setDescriptionOfRightInternally(rightidentifier, description, authenticateSession(sessionid, token, true), language);
  }


  public boolean sessionSetDomainSpecificDataOfDomain(String sessionid, String token, String domainidentifier,
                                                      DomainTypeSpecificData specificData) throws RemoteException {
    return setDomainSpecificDataOfDomainInternally(domainidentifier, specificData, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetDomainsOfUser(String sessionid, String token, String useridentifier, List<String> domains)
                  throws RemoteException {
    return setDomainsOfUserInternally(useridentifier, domains, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetMaxRetriesOfDomain(String sessionid, String token, String domainidentifier, int maxRetries)
                  throws RemoteException {
    return setMaxRetriesOfDomainInternally(domainidentifier, maxRetries, authenticateSession(sessionid, token, true));
  }


  public boolean sessionSetDescriptionOfDomain(String sessionid, String token, String domainidentifier,
                                               String description) throws RemoteException {
    return setDescriptionOfDomainInternally(domainidentifier, description, authenticateSession(sessionid, token, true));
  }


  public Right sessionGetRight(String sessionid, String token, String rightidentifier) throws RemoteException {
    return sessionGetRight(sessionid, token, rightidentifier, null);
  }


  public Right sessionGetRight(String sessionid, String token, String rightidentifier, String language) throws RemoteException {
    return getRightInternally(rightidentifier, authenticateSession(sessionid, token, true), language);
  }


  public boolean sessionDeleteDomain(String sessionid, String token, String domainidentifier) throws RemoteException {
    return deleteDomainInternally(domainidentifier, authenticateSession(sessionid, token, true));
  }


  public FrequencyControlledTaskSearchResult sessionSearchFrequencyControlledTasks(String sessionid,
                                                                                   String token,
                                                                                   FrequencyControlledTaskSelect selection,
                                                                                   int maxRows) throws RemoteException {
    return searchFrequencyControlledTasksInternally(selection, maxRows, authenticateSession(sessionid, token, true));
  }


  public void sessionUndeployDatatype(String sessionid, String token, String originalFqName, boolean recursivly)
                  throws RemoteException {
    undeployDatatypeInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
    
  }


  public void sessionUndeployException(String sessionid, String token, String originalFqName, boolean recursivly)
                  throws RemoteException {
    undeployExceptionInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
    
  }


  public void sessionUndeployWorkflow(String sessionid, String token, String originalFqName, boolean recursivly)
                  throws RemoteException {
    undeployWorkflowInternally(originalFqName, recursivly, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
    
  }

  @Deprecated
  public void sessionDeleteDatatype(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                    boolean recursivlyDelete) throws RemoteException {
    deleteDatatypeInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
  }

  @Deprecated
  public void sessionDeleteException(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                     boolean recursivlyDelete) throws RemoteException {
    deleteExceptionInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
  }

  @Deprecated
  public void sessionDeleteWorkflow(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                    boolean recursivlyDelete) throws RemoteException {
    deleteWorkflowInternally(originalFqName, recursivlyUndeploy, recursivlyDelete, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
  }
  

  public XMOMDatabaseSearchResult sessionSearchXMOMDatabase(String sessionid, String token,
                                                            List<XMOMDatabaseSelect> selects, int maxRows)
                        throws RemoteException {
    return searchXMOMDatabaseInternally(selects, maxRows, RevisionManagement.DEFAULT_WORKSPACE, authenticateSession(sessionid, token, true));
  }


  public boolean sessionAddCapacity(String sessionid, String token, String name, int cardinality, State state)
                  throws RemoteException {
    return addCapacityInternally(name, cardinality, state, authenticateSession(sessionid, token, true));
  }


  public boolean sessionRemoveCapacity(String sessionid, String token, String name) throws RemoteException {
    return removeCapacityInternally(name, authenticateSession(sessionid, token, true));
  }


  public boolean sessionChangeCapacityName(String sessionid, String token, String name, String newName)
                  throws RemoteException {
    return changeCapacityNameInternally(name, newName, authenticateSession(sessionid, token, true));
  }


  public boolean sessionChangeCapacityCardinality(String sessionid, String token, String name, int cardinality)
                  throws RemoteException {
    return changeCapacityCardinalityInternally(name, cardinality, authenticateSession(sessionid, token, true));
  }


  public boolean sessionChangeCapacityState(String sessionid, String token, String name, State state)
                  throws RemoteException {
    return changeCapacityStateInternally(name, state, authenticateSession(sessionid, token, true));
  }


  public boolean sessionRequireCapacityForWorkflow(String sessionid, String token, String workflowFqName,
                                                   String capacityName, int capacityCardinality) throws RemoteException {
    return requireCapacityForWorkflowInternally(workflowFqName, capacityName, capacityCardinality, authenticateSession(sessionid, token, true));
  }


  public boolean sessionRemoveCapacityForWorkflow(String sessionid, String token, String workflowFqName,
                                                  String capacityName) throws RemoteException {
    return removeCapacityForWorkflowInternally(workflowFqName, capacityName, authenticateSession(sessionid, token, true));
  }


  public CapacityInformation sessionGetCapacityInformation(String sessionid, String token, String capacityName)
                  throws RemoteException {
    return getCapacityInternally(capacityName, authenticateSession(sessionid, token, true));
  }


  public List<CapacityMappingStorable> sessionGetAllCapacityMappings(String sessionid, String token)
                  throws RemoteException {
    return getAllCapacityMappingsInternally(authenticateSession(sessionid, token, true));
  }


  public ExtendedCapacityUsageInformation sessionListExtendedCapacityInformation(String sessionid, String token)
                  throws RemoteException {
    return listExtendedCapacityInformationInternally(authenticateSession(sessionid, token, true));
  }
  

  public Collection<VetoInformationStorable> sessionListVetoInformation(String sessionid, String token)
                  throws RemoteException {
    return listVetoInformationInternally(authenticateSession(sessionid, token, true));
  }


  public String sessionGetProperty(String sessionid, String token, String key) throws RemoteException {
    return getPropertyInternally(key, authenticateSession(sessionid, token, true));
  }

  public Collection<XynaPropertyWithDefaultValue> sessionGetPropertiesWithDefaultValuesReadOnly(String sessionid, String token) throws RemoteException {
    return getPropertiesWithDefaultInternally(authenticateSession(sessionid, token, true));
  }
  

  public PropertyMap<String, String> sessionGetProperties(String sessionid, String token) throws RemoteException {
    return getPropertiesInternally(authenticateSession(sessionid, token, true));
  }


  public void sessionSetProperty(String sessionid, String token, String key, String value) throws RemoteException {
    setPropertyInternally(key, value, authenticateSession(sessionid, token, true));
  }


  public void sessionSetProperty(String sessionid, String token, XynaPropertyWithDefaultValue property) throws RemoteException {
    setPropertyInternally(property, authenticateSession(sessionid, token, true));
  }


  public void sessionRemoveProperty(String sessionid, String token, String key) throws RemoteException {
    removePropertyInternally(key, authenticateSession(sessionid, token, true));
  }


  public void sessionCreateOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
                  throws RemoteException {
    createOrdertypeInternally(ordertypeParameter, authenticateSession(sessionid, token, true));
  }


  public void sessionModifyOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
                  throws RemoteException {
    modifyOrdertypeInternally(ordertypeParameter, authenticateSession(sessionid, token, true));
  }


  public void sessionDeleteOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
                  throws RemoteException {
    deleteOrdertypeInternally(ordertypeParameter, authenticateSession(sessionid, token, true));
  }


  public List<OrdertypeParameter> sessionListOrdertypes(String sessionid, String token) throws RemoteException {
    return listOrdertypesInternally(authenticateSession(sessionid, token, true), null);
  }


  public ExtendedManualInteractionResult sessionSearchExtendedManualInteractions(String sessionId, String token,
                                                                                 ManualInteractionSelect selectMI,
                                                                                 int maxRows) throws RemoteException {
    return searchExtendedManualInteractionsInternally(selectMI, maxRows, authenticateSession(sessionId, token, true));
  }


  public boolean sessionIsSessionAlive(String sessionid, String token, String otherSessionId) throws RemoteException {
    return isSessionAliveInternally(authenticateSession(sessionid, token, true), otherSessionId);
  }


  public void sessionAllocateAdministrativeVeto(String sessionid, String token, String vetoName, String documentation) throws XynaException,
                  RemoteException {
    allocateAdministrativeVetoInternally(authenticateSession(sessionid, token, true), vetoName, documentation);
  }


  public void sessionFreeAdministrativeVeto(String sessionid, String token, String vetoName) throws XynaException,
                  RemoteException {
    freeAdministrativeVetoInternally(authenticateSession(sessionid, token, true), vetoName);
  }
  
  
  public void sessionSetDocumentationOfAdministrativeVeto(String sessionid, String token, String vetoName, String documentation) throws XynaException, RemoteException {
    setDocumentationOfAdministrativeVetoInternally(authenticateSession(sessionid, token, true), vetoName, documentation);
  }


  public VetoSearchResult sessionSearchVetos(String sessionid, String token, VetoSelectImpl select, int maxRows)
                  throws XynaException, RemoteException {
    return searchVetosInternally(authenticateSession(sessionid, token, true), select, maxRows);
  }


  public Collection<ApplicationInformation> sessionListApplications(String sessionid, String token)
                  throws RemoteException {
    return listApplicationsInternally(authenticateSession(sessionid, token, true));
  }


  @Deprecated
  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Long startTime, Long interval, boolean enabled, String onError,
                                        String application, String version) throws RemoteException {
    return sessionStartCronLikeOrder(sessionid, token, label, payload, orderType, startTime,
                                     Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError, null, null, null,
                                     null, application, version);
  }


  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Long startTime, String timeZoneID, Long interval, boolean useDST,
                                        boolean enabled, String onError, String cloCustom0, String cloCustom1,
                                        String cloCustom2, String cloCustom3, String application, String version)
      throws RemoteException {

    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement().getRevision(application, version);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      throw new RemoteException("Application and/or version not found: " + application + ", version", e1);
    }

    try {
      GeneralXynaObject payloadXynaObject = null;
      if (payload != null) {
        payloadXynaObject = XynaObject.generalFromXml(payload, revision);
      } else {
        payloadXynaObject = new Container();
      }
      CronLikeOrderCreationParameter clocp =
          new CronLikeOrderCreationParameter(label, orderType, startTime, timeZoneID, interval, useDST, enabled,
                                             OnErrorAction.valueOf(onError.toUpperCase()), cloCustom0, cloCustom1,
                                             cloCustom2, cloCustom3, payloadXynaObject);
      clocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
      clocp.setDestinationKey(new DestinationKey(orderType, application, version));
      CronLikeOrder startedCronLikeOrder = startCronLikeOrderInternally(clocp, authenticateSession(sessionid, token, true));
      return startedCronLikeOrder.getId();
    } catch (XynaException e) {
      transformXynaException(e);
      return null;
    }

  }
  

  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled,
                                        String onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3, String application, String version) throws RemoteException {
    return sessionStartCronLikeOrder(sessionid, token, label, payload, orderType,
                                     startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone()
                                         .getID(), interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                     cloCustom2, cloCustom3, application, version);
  }


  public CronLikeOrderSearchResult sessionSearchCronLikeOrders(String sessionId, String token,
                                                               CronLikeOrderSelectImpl selectCron, int maxRows)
      throws RemoteException {
    return searchCronLikeOrdersInternally(selectCron, maxRows, authenticateSession(sessionId, token, true));
  }


  public List<Capacity> sessionListCapacitiesForOrdertype(String sessionid, String token, String ordertype)
      throws RemoteException {
    return listCapacitiesForOrdertypeInternally(new DestinationKey(ordertype), authenticateSession(sessionid, token, true));
  }


  
}
