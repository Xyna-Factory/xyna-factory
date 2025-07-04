/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.yang.impl;


import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xact.templates.Document;
import xmcp.yang.MessageId;
import xmcp.yang.ProtocolOperationsServiceOperation;
import xmcp.yang.YangMappingCollection;
import xmcp.yang.codedservice.CSCloseSession;
import xmcp.yang.codedservice.CSCopyConfig;
import xmcp.yang.codedservice.CSDeleteConfig;
import xmcp.yang.codedservice.CSEditConfig;
import xmcp.yang.codedservice.CSGenerateNextMessageId;
import xmcp.yang.codedservice.CSGet;
import xmcp.yang.codedservice.CSGetConfig;
import xmcp.yang.codedservice.CSKillSession;
import xmcp.yang.codedservice.CSLock;
import xmcp.yang.codedservice.CSUnlock;
import xmcp.yang.netconf.EditConfigInputData;
import xmcp.yang.netconf.NetConfFilter;
import xmcp.yang.netconf.NetConfSessionId;
import xmcp.yang.netconf.NetConfSource;
import xmcp.yang.netconf.NetConfTarget;


public class ProtocolOperationsServiceOperationImpl implements ExtendedDeploymentTask, ProtocolOperationsServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  
  @Override
  public Document closeSession(MessageId messageId) {
    return new CSCloseSession().execute(messageId);
  }

  
  @Override
  public Document copyConfig(MessageId messageId, NetConfTarget netConfTarget8, NetConfSource netConfSource9) {
    return new CSCopyConfig().execute(messageId, netConfTarget8, netConfSource9);
  }

  
  @Override
  public Document deleteConfig(MessageId messageId, NetConfTarget netConfTarget10) {
    return new CSDeleteConfig().execute(messageId, netConfTarget10);
  }

  
  @Override
  public Document editConfig(MessageId messageId, EditConfigInputData data, YangMappingCollection config) {
    return new CSEditConfig().execute(messageId, data, config);
  }

  
  @Override
  public Document get(MessageId messageId, NetConfFilter netConfFilter13) {
    return new CSGet().execute(messageId, netConfFilter13);
  }

  
  @Override
  public Document getConfig(MessageId messageId, NetConfSource source1, NetConfFilter filter2) {
    return new CSGetConfig().execute(messageId, source1, filter2);
  }

  
  @Override
  public Document killSession(MessageId messageId, NetConfSessionId netConfSessionId14) {
    return new CSKillSession().execute(messageId, netConfSessionId14);
  }

  
  @Override
  public Document lock(MessageId messageId, NetConfTarget netConfTarget11) {
    return new CSLock().execute(messageId, netConfTarget11);
  }

  
  @Override
  public Document unlock(MessageId messageId, NetConfTarget netConfTarget12) {
    return new CSUnlock().execute(messageId, netConfTarget12);
  }

  @Override
  public MessageId generateNextMessageId() {
    return new CSGenerateNextMessageId().execute();
  }

}
