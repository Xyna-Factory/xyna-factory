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
package xmcp.xypilot.impl;



import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.forms.plugin.Plugin;
import xmcp.xypilot.impl.Generation.GenerationInterface;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.RuntimeContext;



public class PluginManagement {

  private static Logger logger = Logger.getLogger("XyPilot");
  private static final Map<String, GenerationButton> pluginEntryNameAndPath = createPluginNameAndPaths();


  private static Map<String, GenerationButton> createPluginNameAndPaths() {
    Map<String, GenerationButton> result = new HashMap<>();
    Generation generation = new Generation();
    createGenerationButton(result, "DTDocu", "modeller/datatype/documentation", generation::genDatatypeDocu);
    return result;
  }


  private static void createGenerationButton(Map<String, GenerationButton> map, String name, String path, GenerationInterface method) {
    map.put(path, new GenerationButton(name, path, method));
  }


  public void registerPlugins(RuntimeContext rtc) {
    managePlugins(rtc, xmcp.forms.plugin.PluginManagement::registerPlugin);
  }


  public void unregisterPlugins(RuntimeContext rtc) {
    managePlugins(rtc, xmcp.forms.plugin.PluginManagement::unregisterPlugin);
  }


  private void managePlugins(RuntimeContext rtc, Consumer<Plugin> consumer) {
    Plugin.Builder builder = new Plugin.Builder();
    builder.pluginRTC(rtc);
    builder.definitionWorkflowFQN("xmcp.xypilot.GetGenerateButtonDefinition");
    for (GenerationButton btn : pluginEntryNameAndPath.values()) {
      builder.navigationEntryName(btn.getName());
      builder.navigationEntryLabel(btn.getName());
      builder.path(btn.getPath());
      consumer.accept(builder.instance());
    }

    builder.definitionWorkflowFQN("xmcp.xypilot.GetManageXyPilotConfigDefinition");
    builder.navigationEntryName("XyPilot Config");
    builder.navigationEntryLabel("XyPilot Config");
    builder.path("manager");
    consumer.accept(builder.instance());
  }


  public void generate(XynaOrderServerExtension correlatedXynaOrder, Context context) {
    pluginEntryNameAndPath.get(context.getLocation()).execute(correlatedXynaOrder, context);
  }


  public static class GenerationButton {

    private final String name;
    private final String path;
    private final GenerationInterface method;


    public GenerationButton(String name, String path, GenerationInterface method) {
      this.name = name;
      this.path = path;
      this.method = method;
    }


    public String getName() {
      return name;
    }


    public String getPath() {
      return path;
    }


    public void execute(XynaOrderServerExtension order, Context c) {
      try {
        method.apply(order, c);
      } catch (Exception e) {
        logger.warn("Exception during generation.", e);
      }
    }
  }
}
