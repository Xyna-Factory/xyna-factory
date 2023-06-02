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



import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.XynaDevelopmentFactoryRMI;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveRMI;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteOutputStream;



public interface XynaRMIChannelBase
    extends
      Remote,
      XynaProcessingRMI,
      ManualInteractionRMI,
      TriggerManagementRMI,
      OrderArchiveRMI,
      XynaOperatorControlRMI,
      XynaDevelopmentFactoryRMI,
      XynaFactoryManagementPortalRMI,
      XynaFactoryControlRMI {

  public static final String RMI_NAME = "XynaRMIChannel";
  
  // those 4 are part of the XynaMultiChannelPortal-Department, move into own remote interface? 
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void publish(XynaCredentials credentials, MessageInputParameter message)  throws RemoteException, XynaException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public boolean addSubscription(XynaCredentials credentials, MessageSubscriptionParameter subscription) throws RemoteException, XynaException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public boolean cancelSubscription(XynaCredentials credentials, Long subscriptionId) throws RemoteException, XynaException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public MessageRetrievalResult fetchMessages(XynaCredentials credentials, Long lastReceivedId) throws RemoteException, XynaException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<String> getMDMs(String user, String password) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<String> getMDMs(String user, String password, String application, String version) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<String> getMDMs(XynaCredentials credentials, RuntimeContext runtimeContext) throws RemoteException;
  
  //TODO there should be a better place to put this
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public String[] scanLogForLinesOfOrder(String user, String password, long orderId, int lineOffset,
                                         int maxNumberOfLines, String... excludes) throws XynaException,
      RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public String retrieveLogForOrder(String user, String password, long orderId, int lineOffset, int maxNumberOfLines,
                                    String... excludes) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public RemoteInputStream getServiceImplTemplate(String user, String password, String fqClassNameDOM,
                                                  boolean deleteServiceImplAfterStreamClose) throws RemoteException,
      XynaException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public RemoteInputStream getTriggerImplTemplate(String user, String password, String triggerName,
                                                  boolean deleteTriggerImplAfterStreamClose) throws XynaException,
      RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public RemoteInputStream getFilterImplTemplate(String user, String password, String filterName,
                                                 String fqTriggerClassName, boolean deleteFilterImplAfterStreamClose)
      throws XynaException, RemoteException;


  /**
   * Create a delivery item
   * @param packageDefinition an InputStream containing the package definition information.
   * @param deliveryItem an OutputStream to which the package content is written
   * @param statusOutputStream an output stream to which status information is written subsequently
   * @param verboseOutput specifies whether extra debug output should be written to the status output stream
   * @param includeXynaComponents specifies whether Xyna components should be exported as well
   */
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void createDeliveryItem(String user, String password, RemoteInputStream packageDefinition,
                                 RemoteOutputStream deliveryItem, RemoteOutputStream statusOutputStream,
                                 boolean verboseOutput, boolean includeXynaComponents) throws XynaException,
      RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void installDeliveryItem(String user, String password, RemoteInputStream deliveryItem,
                                  RemoteOutputStream statusOutputStream, boolean forceOverwrite, boolean dontUpdateMdm,
                                  boolean verboseOutput) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.DATA_MODEL, action=Action.insert, checks=1)
  public DataModelResult importDataModel(XynaCredentials credentials, ImportDataModelParameters params) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DATA_MODEL, action=Action.insert, checks=1)
  public DataModelResult removeDataModel(XynaCredentials credentials, RemoveDataModelParameters parameters) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DATA_MODEL, action=Action.insert, checks=1)
  public DataModelResult modifyDataModel(XynaCredentials xynaCredentials, ModifyDataModelParameters parameters) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.SEARCH, action=Action.list, checks=1)
  public SearchResult<?> search(XynaCredentials credentials, SearchRequestBean searchRequest ) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<PluginDescription> listPluginDescriptions(XynaCredentials credentials, PluginType type) throws RemoteException;

}
