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
package com.gip.xyna.xact.filter.xmom.datatypes.json;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;

import xmcp.forms.plugin.PluginManagement;
import xmcp.yggdrasil.plugin.Context;
import xmcp.yggdrasil.plugin.GuiDefiningWorkflow;
import xmcp.yggdrasil.plugin.Plugin;



public class GuiHttpPluginManagement {

  private HashMap<String, Plugin.Builder> builders;

  private static final String subscriptionProduct = "zeta";
  private static final String subscriptionContext = "plugin";
  private static final String subscriptionSessionId = "guihttppluginmanagement";
  private static final String threadName = "GuiHttpPluginManagement";
  private static Logger logger = CentralFactoryLogging.getLogger(GuiHttpPluginManagement.class);
  private static GuiHttpPluginManagement instance;
  private final MessageBusManagementPortal messageBusManagementPortal;
  private Thread messageBusThread;
  private Long lastReceivedId = -1l;


  public static GuiHttpPluginManagement getInstance() {
    if (instance != null) {
      return instance;
    }
    synchronized (GuiHttpPluginManagement.class) {
      if (instance == null) {
        instance = new GuiHttpPluginManagement();
      }
    }
    return instance;
  }


  private GuiHttpPluginManagement() {
    builders = new HashMap<>();
    messageBusManagementPortal = XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement();
    MessageSubscriptionParameter subscription = new MessageSubscriptionParameter(1l, subscriptionProduct, subscriptionContext, ".*");
    messageBusManagementPortal.addSubscription(subscriptionSessionId, subscription);
    start();
  }


  private void monitorMessageBus() {
    while (GuiHttpPluginManagement.getInstance().messageBusThread == Thread.currentThread()) {
      try {
        MessageRetrievalResult result = messageBusManagementPortal.fetchMessages(subscriptionSessionId, lastReceivedId);
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
          updatePluginData();
        }
        Thread.sleep(60000); //1 minute
      } catch (OutOfMemoryError | InterruptedException t) {
        Department.handleThrowable(t);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Messagebus monitoring thread '" + threadName + "' (" + Thread.currentThread() + ") finished.");
    }
  }


  public void updatePluginData() {
    logger.debug("updating plugin data");
    builders.clear();
    List<? extends xmcp.forms.plugin.Plugin> plugins = PluginManagement.listPlugins();
    HashMap<String, List<GuiDefiningWorkflow>> definingWorkflows = new HashMap<>();
    for (xmcp.forms.plugin.Plugin plugin : plugins) {
      String path = plugin.getPath();
      builders.putIfAbsent(path, new Plugin.Builder());
      definingWorkflows.putIfAbsent(path, new ArrayList<GuiDefiningWorkflow>());
      GuiDefiningWorkflow.Builder gdwBuilder = new GuiDefiningWorkflow.Builder();
      gdwBuilder.fQN(plugin.getDefinitionWorkflowFQN());
      gdwBuilder.runtimeContext(plugin.getPluginRTC());
      definingWorkflows.get(path).add(gdwBuilder.instance());
    }

    for (String path : builders.keySet()) {
      Plugin.Builder builder = builders.get(path);
      builder.guiDefiningWorkflow(definingWorkflows.get(path));
      builders.put(path, builder);
    }
  }


  public Plugin createPlugin(Context context) {
    Plugin.Builder builder = builders.get(context.getLocation());
    if (builder == null) {
      return null;
    }

    builder.context(context);

    return builder.instance().clone();
  }


  public void stop() {
    Thread oldThread = messageBusThread;
    messageBusThread = null;
    try {
      if (oldThread != null) {
        oldThread.interrupt();
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("interrupting messageBusThread '" + threadName + "' ("+ oldThread + ") resulted in exception: " + e);
      }
    }
  }
  
  public void start() {
    updatePluginData();
    if(messageBusThread != null) {
      return;
    }
    messageBusThread = new Thread(this::monitorMessageBus, threadName);
    messageBusThread.start();
    if(logger.isDebugEnabled()) {
      logger.debug("Started new messageBusThread: " + messageBusThread);
    }
  }
}
