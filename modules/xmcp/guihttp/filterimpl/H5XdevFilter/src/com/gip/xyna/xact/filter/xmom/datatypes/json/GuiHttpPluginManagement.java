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
import com.gip.xyna.xact.filter.xmom.PluginPaths;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;

import xmcp.forms.plugin.PluginManagement;
import xmcp.yggdrasil.plugin.Context;
import xmcp.yggdrasil.plugin.ContextMenuEntry;
import xmcp.yggdrasil.plugin.ContextMenuPlugin;
import xmcp.yggdrasil.plugin.GuiDefiningWorkflow;
import xmcp.yggdrasil.plugin.Plugin;
import xmcp.yggdrasil.plugin.PluginBase;



public class GuiHttpPluginManagement {

  private HashMap<String, PluginBase> guiPlugins;

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
    guiPlugins = new HashMap<>();
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
    guiPlugins.clear();
    List<? extends xmcp.forms.plugin.Plugin> zetaPluginEntries = PluginManagement.listPlugins();
    HashMap<String, List<GuiDefiningWorkflow>> definingWorkflows = new HashMap<>();
    HashMap<String, List<ContextMenuEntry>> menuEntries = new HashMap<>();
    for (xmcp.forms.plugin.Plugin pluginEntry : zetaPluginEntries) {
      String path = pluginEntry.getPath();
      if (path == null || path.isBlank()) {
        logger.warn("Missing path name for Plugin " + pluginEntry.getNavigationEntryName() + ", using default path \"manager\"");
        path = "manager";
      }
      if (path.equals(PluginPaths.location_workflow_mapping)) {
        guiPlugins.putIfAbsent(path, new ContextMenuPlugin());
        menuEntries.putIfAbsent(path, new ArrayList<ContextMenuEntry>());
        ContextMenuEntry.Builder cmeBuilder = new ContextMenuEntry.Builder();
        cmeBuilder.navigationEntryLabel(pluginEntry.getNavigationEntryLabel());
        cmeBuilder.navigationEntryName(pluginEntry.getNavigationEntryName());
        cmeBuilder.navigationIconName(pluginEntry.getNavigationIconName());
        cmeBuilder.fQN(pluginEntry.getDefinitionWorkflowFQN());
        menuEntries.get(path).add(cmeBuilder.instance());
      }
      else {
        guiPlugins.putIfAbsent(path, new Plugin());
        definingWorkflows.putIfAbsent(path, new ArrayList<GuiDefiningWorkflow>());
        GuiDefiningWorkflow.Builder gdwBuilder = new GuiDefiningWorkflow.Builder();
        gdwBuilder.fQN(pluginEntry.getDefinitionWorkflowFQN());
        gdwBuilder.runtimeContext(pluginEntry.getPluginRTC());
        definingWorkflows.get(path).add(gdwBuilder.instance());
      }
    }

    for (String path : guiPlugins.keySet()) {
      if (path.equals(PluginPaths.location_workflow_mapping)) {
        ContextMenuPlugin contextMenuPlugin = new ContextMenuPlugin();
        contextMenuPlugin.unversionedSetMenuEntry(menuEntries.get(path));
        guiPlugins.put(path, contextMenuPlugin);
      }
      else {
        Plugin plugin = new Plugin();
        plugin.unversionedSetGuiDefiningWorkflow(definingWorkflows.get(path));
        guiPlugins.put(path, plugin);
      }
    }
  }


  @SuppressWarnings("unchecked")
  public <T extends PluginBase> T createPlugin(Context context) {
    T guiPlugin = (T) guiPlugins.get(context.getLocation());
    if (guiPlugin == null) {
      return null;
    }

    guiPlugin.unversionedSetContext(context);

    return (T) guiPlugin.clone();
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
