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

import base.Text;
import base.math.IntegerNumber;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xfmg.xopctrl.XynaUserSession;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.model.DomVariableModel;
import xmcp.xypilot.impl.gen.model.ExceptionModel;
import xmcp.xypilot.impl.gen.model.ExceptionVariableModel;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.pipeline.Pipeline;
import xmcp.xypilot.impl.gen.util.FilterCallbackInteractionUtils;
import xmcp.xypilot.impl.locator.DataModelLocator;
import xmcp.xypilot.impl.locator.PipelineLocator;
import xmcp.xypilot.metrics.Code;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.RuntimeContext;
import xmcp.forms.plugin.Plugin;
import xmcp.xypilot.Documentation;
import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.Mapping;
import xmcp.xypilot.MappingAssignment;
import xmcp.xypilot.MemberReference;
import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.PromptGenerator;
import xmcp.xypilot.PromptGeneratorServiceOperation;

public class PromptGeneratorServiceOperationImpl implements ExtendedDeploymentTask, PromptGeneratorServiceOperation {

    private static Logger logger = Logger.getLogger("XyPilot");

    private static final Map<String, GenerationButton> pluginEntryNameAndPath = createPluginNameAndPaths();
    
    private static Map<String, GenerationButton> createPluginNameAndPaths() {
      Map<String, GenerationButton> result = new HashMap<>();
      createGenerationButton(result, "DTDocu", "modeller/datatype/documentation", PromptGeneratorServiceOperationImpl::genDatatypeDocu);
      return result;
    }


    private static void createGenerationButton(Map<String, GenerationButton> map, String name, String path,
                                               BiConsumer<XynaOrderServerExtension, Context> method) {
      map.put(path, new GenerationButton(name, path, method));
    }


    public void onDeployment() throws XynaException {
      managePlugins(xmcp.forms.plugin.PluginManagement::registerPlugin);
    }


    public void onUndeployment() throws XynaException {
      managePlugins(xmcp.forms.plugin.PluginManagement::unregisterPlugin);
    }


    public Long getOnUnDeploymentTimeout() {
      return null;
    }


    public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
      return null;
    }


    private RuntimeContext getOwnRtc() {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      Long revision = clb.getRevision();
      return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
    }

    private void managePlugins(Consumer<Plugin> consumer) {
      Plugin.Builder builder = new Plugin.Builder();
      builder.pluginRTC(getOwnRtc());
      builder.definitionWorkflowFQN("xmcp.xypilot.GetGenerateButtonDefinition");
      for(String nameAndPath : pluginEntryNameAndPath.keySet()) {
        builder.navigationEntryName(pluginEntryNameAndPath.get(nameAndPath).getName());
        builder.navigationEntryLabel(pluginEntryNameAndPath.get(nameAndPath).getName());
        builder.path(pluginEntryNameAndPath.get(nameAndPath).getPath());
        consumer.accept(builder.instance());
      }
    }


    private String createCorrelation(XMOMItemReference ref, String type) {
      return String.format("%s-%s-WS:\"%s\"", type, ref.getFqName(), ref.getWorkspace());
    }
    
    private void publishUpdateMessage(XMOMItemReference xmomItemReference, String type) throws XynaException {
      String correlation = createCorrelation(xmomItemReference, type);
      MessageInputParameter message = new MessageInputParameter("Xyna", "Process Modeller Autosaves", correlation, "", null, false);
      XynaFactory.getInstance().Publish(message);
    }
    
    
    private static XMOMItemReference buildItemFromContext(Context context) {
      XMOMItemReference.Builder builder = new XMOMItemReference.Builder();
      builder.fqName(context.getFQN());
      builder.workspace(((xprc.xpce.Workspace)context.getRuntimeContext()).getName());
      return builder.instance();
    }
    
    @Override
    public Documentation generateDatatypeDocumentation(XynaOrderServerExtension correlatedOrder, XMOMItemReference xmomItemReference) {
      try {
        DomModel model = DataModelLocator.getDomModel(xmomItemReference, correlatedOrder);
        Pipeline<Documentation, DomModel> pipeline = PipelineLocator.getPipeline("dom-documentation");
        Documentation doc = pipeline.run(model).firstChoice();
        FilterCallbackInteractionUtils.updateDomDocumentation(doc, correlatedOrder, xmomItemReference);
        publishUpdateMessage(xmomItemReference, "DataType");
        return doc;
      } catch (Throwable e) {
        logger.warn("Couldn't generate documentation", e);
        return new Documentation("");
      }
    }
    
    private static void genDatatypeDocu(XynaOrderServerExtension order, Context context) {
      PromptGenerator.generateDatatypeDocumentation(order, buildItemFromContext(context));
    }

    @Override
    public Documentation generateExceptionDocumentation(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference);

            // load the pipeline
            Pipeline<Documentation, ExceptionModel> pipeline = PipelineLocator.getPipeline("exception-documentation");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate documentation", e);
            return new Documentation("");
        }
    }


    @Override
    public Documentation generateDatatypeMethodDocumentation(MemberReference memberReference) {
        try {
            // get the data model
            DomMethodModel model = DataModelLocator.getDomMethodModel(memberReference, null);

            // load the pipeline
            Pipeline<Documentation, DomMethodModel> pipeline = PipelineLocator.getPipeline("dom-method-documentation");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate member documentation", e);
            return new Documentation("");
        }
    }


    @Override
    public Documentation generateDatatypeVariableDocumentation(MemberReference memberReference) {
        try {
            // get the data model
            DomVariableModel model = DataModelLocator.getDomVariableModel(memberReference, null);

            // load the pipeline
            Pipeline<Documentation, DomVariableModel> pipeline = PipelineLocator.getPipeline("dom-variable-documentation");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate member documentation", e);
            return new Documentation("");
        }
    }


    @Override
    public Documentation generateExceptionVariableDocumentation(MemberReference memberReference) {
        try {
            // get the data model
            ExceptionVariableModel model = DataModelLocator.getExceptionVariableModel(memberReference);

            // load the pipeline
            Pipeline<Documentation, ExceptionVariableModel> pipeline = PipelineLocator.getPipeline("exception-variable-documentation");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate member documentation", e);
            return new Documentation("");
        }
    }

    @Override
    public List<? extends MemberVariable> generateExceptionVariables(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference);

            // load the pipeline
            Pipeline<List<? extends MemberVariable>, ExceptionModel> pipeline = PipelineLocator.getPipeline("exception-variables");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate members", e);
            return new ArrayList<MemberVariable>();
        }
    }


    @Override
    public List<? extends MemberVariable> generateDatatypeVariables(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            DomModel model = DataModelLocator.getDomModel(xmomItemReference, null);

            // load the pipeline
            Pipeline<List<? extends MemberVariable>, DomModel> pipeline = PipelineLocator.getPipeline("dom-variables");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate members", e);
            return new ArrayList<MemberVariable>();
        }
    }


    @Override
    public List<? extends MethodDefinition> generateDatatypeMethods(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            DomModel model = DataModelLocator.getDomModel(xmomItemReference, null);

            // load the pipeline
            Pipeline<List<? extends MethodDefinition>, DomModel> pipeline = PipelineLocator.getPipeline("dom-methods");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate methods", e);
            return new ArrayList<MethodDefinition>();
        }
    }


    @Override
    public Text generateMappingLabel(MemberReference memberReference) {
        try {
            // get the data model
            MappingModel model = DataModelLocator.getMappingModel(memberReference);

            // load the pipeline
            Pipeline<Text, MappingModel> pipeline = PipelineLocator.getPipeline("mapping-label");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate mapping label.", e);
            return new Text("");
        }
    }


    @Override
    public Mapping generateMappingAssignments(MemberReference memberReference) {
        try {
            // get the data model
            MappingModel model = DataModelLocator.getMappingModel(memberReference);

            // load the pipeline
            Pipeline<Mapping, MappingModel> pipeline = PipelineLocator.getPipeline("mapping-assignments");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate mapping assignments.", e);
            return new Mapping("", new ArrayList<MappingAssignment>());
        }
    }


    @Override
    public List<? extends Code> generateDatatypeMethodImplementation(MemberReference memberReference) {
        try {
            logger.debug("Fetching data model for " + memberReference.getMember());
            // get the data model
            DomMethodModel model = DataModelLocator.getDomMethodModel(memberReference, null);
            logger.debug("Generating implementation for " + model.getTargetMethod().getName());
            // load the pipeline
            Pipeline<Code, DomMethodModel> pipeline = PipelineLocator.getPipeline("dom-method-implementation");

            // run the pipeline on the model
            return pipeline.run(model).choices();
        } catch (Throwable e) {
            logger.warn("Couldn't generate implementation", e);
            return new ArrayList<Code>();
        }
    }


    @Override
    public List<? extends ExceptionMessage> generateExceptionMessages(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference);

            // load the pipeline
            Pipeline<List<ExceptionMessage>, ExceptionModel> pipeline = PipelineLocator.getPipeline("exception-messages");

            // run the pipeline on the model
            return pipeline.run(model).firstChoice();
        } catch (Throwable e) {
            logger.warn("Couldn't generate exception messages", e);
            return new ArrayList<ExceptionMessage>();
        }
    }


    @Override
    public Text getXML(XMOMItemReference xmomItemReference, XynaUserSession session) {
        return new Text("");
    }


    @Override
    public void generate(XynaOrderServerExtension correlatedXynaOrder, Context context) {
      pluginEntryNameAndPath.get(context.getLocation()).execute(correlatedXynaOrder, context);
    }
    
    private static class GenerationButton {
      private final String name;
      private final String path;
      private final BiConsumer<XynaOrderServerExtension, Context> method;
      
      public GenerationButton(String name, String path, BiConsumer<XynaOrderServerExtension, Context> method) {
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
        method.accept(order, c);
      }
    }

}
