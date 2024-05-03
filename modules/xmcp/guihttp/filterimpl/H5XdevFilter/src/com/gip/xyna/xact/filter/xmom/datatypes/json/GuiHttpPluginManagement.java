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

import xmcp.forms.plugin.PluginManagement;
import xmcp.yggdrasil.plugin.Context;
import xmcp.yggdrasil.plugin.GuiDefiningWorkflow;
import xmcp.yggdrasil.plugin.Plugin;



public class GuiHttpPluginManagement {

  private HashMap<String, Plugin.Builder> builders;

  private static GuiHttpPluginManagement instance;


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
    updatePluginData();
  }


  public void updatePluginData() {
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
}
