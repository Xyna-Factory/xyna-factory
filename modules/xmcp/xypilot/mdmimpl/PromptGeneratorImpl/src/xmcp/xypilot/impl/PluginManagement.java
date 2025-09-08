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
package xmcp.xypilot.impl;



import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.forms.plugin.Plugin;
import xmcp.xypilot.impl.Generation.GenerationInterface;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;



public class PluginManagement {
  
  public static final String DEFAULT_PLUGIN = "default:";
  public static final String MAPPING_LABEL_PLUGIN = "mapingLabel:";
  public static final String MAPPING_ASSIGNMENT_PLUGIN = "mapingAssignment:";

  private static Logger logger = Logger.getLogger("XyPilot");
  private static final Map<String, GenerationButton> pluginEntryNameAndPath = createPluginNameAndPaths();

  private static final String GEN_BTN_FQN = "xmcp.xypilot.GetGenerateButtonDefinition";
  private static final String GEN_BTN_MAPPING_LABEL = "xmcp.xypilot.GenerateMappingLabel";
  private static final String GEN_BTN_MAPPING_ASSIGNMENTS = "xmcp.xypilot.GenerateMappingAssignments";

  private static Map<String, GenerationButton> createPluginNameAndPaths() {
    Map<String, GenerationButton> result = new HashMap<>();
    Generation gen = new Generation();
    createGenerationButton(result, "DTDocu", "modeller/datatype/documentation", gen::genDatatypeDocu, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "DTMem", "modeller/datatype/members", gen::genDatatypeVariables, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "DTMemDocu", "modeller/datatype/members/documentation", gen::genDatatypeVarDocu, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "DTMeth", "modeller/datatype/methods", gen::genMethods, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "DTMethDocu", "modeller/datatype/methods/documentation", gen::genDatatypeMethodDocu, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "DTMethImpl", "modeller/datatype/methods/implementation", gen::genDatatypeMethodImpl, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "EXDocu", "modeller/exception/documentation", gen::genExceptionDocu, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "EXMem", "modeller/exception/members", gen::genExceptionVariables, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "EXMemDocu", "modeller/exception/members/documentation", gen::genExceptionVarDocu, GEN_BTN_FQN, DEFAULT_PLUGIN);
    createGenerationButton(result, "EXMess", "modeller/exception/message", gen::genExceptionMessages, GEN_BTN_FQN, DEFAULT_PLUGIN);
    
    createGenerationButton(result, "Xypilot: Generate Assignments", "modeller/workflow/mapping", gen::genMappingAssignments, GEN_BTN_MAPPING_ASSIGNMENTS, MAPPING_ASSIGNMENT_PLUGIN);
    createGenerationButton(result, "Xypilot: Generate Label", "modeller/workflow/mapping", gen::genMappingLabel, GEN_BTN_MAPPING_LABEL, MAPPING_LABEL_PLUGIN);
    return result;
  }


  private static void createGenerationButton(Map<String, GenerationButton> map, String name, String path, GenerationInterface method, String fqn, String type) {
    map.put(type + path, new GenerationButton(name, path, method, fqn));
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
    for (GenerationButton btn : pluginEntryNameAndPath.values()) {
      builder.definitionWorkflowFQN(btn.getWorkflowFqn());
      builder.navigationEntryName(btn.getName());
      builder.navigationEntryLabel(btn.getName());
      builder.path(btn.getPath());
      consumer.accept(builder.instance().clone());
    }

    builder.definitionWorkflowFQN("xmcp.xypilot.GetManageXyPilotConfigDefinition");
    builder.navigationEntryName("XyPilot Config");
    String entryLabel = "XyPilot Config";
    if (rtc instanceof Application) {
      entryLabel = entryLabel + " " + ((Application) rtc).getVersion();
    }

    builder.navigationEntryLabel(entryLabel);
    builder.path("manager");
    consumer.accept(builder.instance().clone());

    builder.definitionWorkflowFQN("xmcp.xypilot.DefineCodeSuggestionGenerationButton");
    builder.navigationEntryName("DTMethImpl");
    builder.navigationEntryLabel("DTMethImpl");
    builder.path("modeller/datatype/methods/implementation");
    consumer.accept(builder.instance().clone());

    builder.definitionWorkflowFQN("xmcp.xypilot.DefineCodeSuggestionGenerationButton");
    builder.navigationEntryName("SGMethImpl");
    builder.navigationEntryLabel("SGMethImpl");
    builder.path("modeller/servicegroup/methods/implementation");
    consumer.accept(builder.instance().clone());

    builder.definitionWorkflowFQN("xmcp.xypilot.DefineMethodImplementationPanel");
    builder.navigationEntryName("XyPilot Code Suggestion");
    builder.navigationEntryLabel("XyPilot Code Suggestion");
    builder.navigationIconName("build");
    builder.path("datatypes/rightnav");
    consumer.accept(builder.instance().clone());
  }


  public void generate(XynaOrderServerExtension correlatedXynaOrder, Context context, String type) {
    pluginEntryNameAndPath.get(type + context.getLocation()).execute(correlatedXynaOrder, context);
  }


  public static class GenerationButton {

    private final String name;
    private final String path;
    private final String workflowFqn;
    private final GenerationInterface method;

    
    public GenerationButton(String name, String path, GenerationInterface method, String workflowFqn) {
      this.name = name;
      this.path = path;
      this.method = method;
      this.workflowFqn = workflowFqn;
    }


    public String getName() {
      return name;
    }


    public String getPath() {
      return path;
    }

    
    public String getWorkflowFqn() {
      return workflowFqn;
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
