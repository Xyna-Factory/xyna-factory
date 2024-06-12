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
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import java.util.ArrayList;
import java.util.List;

import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.XypilotUserConfig;
import xmcp.xypilot.impl.config.XypilotUserConfigStorage;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.metrics.Code;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.RuntimeContext;
import xmcp.xypilot.Documentation;
import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.Mapping;
import xmcp.xypilot.MappingAssignment;
import xmcp.xypilot.MemberReference;
import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.PromptGeneratorServiceOperation;

public class PromptGeneratorServiceOperationImpl implements ExtendedDeploymentTask, PromptGeneratorServiceOperation {

//    private static Logger logger = Logger.getLogger("XyPilot");
    private PluginManagement pluginManagement = new PluginManagement();

    public void onDeployment() throws XynaException {
      pluginManagement.registerPlugins(getOwnRtc());
      XypilotUserConfigStorage.init();
    }


    public void onUndeployment() throws XynaException {
      pluginManagement.unregisterPlugins(getOwnRtc());
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

    
    @Override
    public Documentation generateDatatypeDocumentation(XynaOrderServerExtension correlatedOrder, XMOMItemReference xmomItemReference) {
//      try {
//        DomModel model = DataModelLocator.getDomModel(xmomItemReference, correlatedOrder);
//        Pipeline<Documentation, DomModel> pipeline = PipelineLocator.getPipeline("dom-documentation");
//        Documentation doc = pipeline.run(model).firstChoice();
//        FilterCallbackInteractionUtils.updateDomDocu(doc, correlatedOrder, xmomItemReference);
//        return doc;
//      } catch (Throwable e) {
//        logger.warn("Couldn't generate documentation", e);
//        return new Documentation("");
//      }
      return new Documentation("");
    }

    @Override
    public Documentation generateExceptionDocumentation(XMOMItemReference xmomItemReference) {
        try {
            // get the data model
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, null);

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
            DomMethodModel model = DataModelLocator.getDomMethodModel(memberReference.getItem(), null, "");

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
            DomVariableModel model = DataModelLocator.getDomVariableModel(memberReference.getItem(), null, "");

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
            ExceptionVariableModel model = DataModelLocator.getExceptionVariableModel(memberReference.getItem(), null, "");

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
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, null);

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
//        try {
//            // get the data model
//            DomModel model = DataModelLocator.getDomModel(xmomItemReference, null);
//
//            // load the pipeline
//            Pipeline<List<? extends MemberVariable>, DomModel> pipeline = PipelineLocator.getPipeline("dom-variables");
//
//            // run the pipeline on the model
//            return pipeline.run(model).firstChoice();
//        } catch (Throwable e) {
//            logger.warn("Couldn't generate members", e);
//            return new ArrayList<MemberVariable>();
//        }
      return new ArrayList<MemberVariable>();
    }


    @Override
    public List<? extends MethodDefinition> generateDatatypeMethods(XMOMItemReference xmomItemReference) {
//        try {
//            // get the data model
//            DomModel model = DataModelLocator.getDomModel(xmomItemReference, null);
//
//            // load the pipeline
//            Pipeline<List<? extends MethodDefinition>, DomModel> pipeline = PipelineLocator.getPipeline("dom-methods");
//
//            // run the pipeline on the model
//            return pipeline.run(model).firstChoice();
//        } catch (Throwable e) {
//            logger.warn("Couldn't generate methods", e);
//            return new ArrayList<MethodDefinition>();
//        }
      return new ArrayList<MethodDefinition>();
    }


    @Override
    public Text generateMappingLabel(MemberReference memberReference) {
//        try {
//            // get the data model
//            MappingModel model = DataModelLocator.getMappingModel(memberReference);
//
//            // load the pipeline
//            Pipeline<Text, MappingModel> pipeline = PipelineLocator.getPipeline("mapping-label");
//
//            // run the pipeline on the model
//            return pipeline.run(model).firstChoice();
//        } catch (Throwable e) {
//            logger.warn("Couldn't generate mapping label.", e);
//            return new Text("");
//        }
      return new Text("");
    }


    @Override
    public Mapping generateMappingAssignments(MemberReference memberReference) {
//        try {
//            // get the data model
//            MappingModel model = DataModelLocator.getMappingModel(memberReference);
//
//            // load the pipeline
//            Pipeline<Mapping, MappingModel> pipeline = PipelineLocator.getPipeline("mapping-assignments");
//
//            // run the pipeline on the model
//            return pipeline.run(model).firstChoice();
//        } catch (Throwable e) {
//            logger.warn("Couldn't generate mapping assignments.", e);
//            return new Mapping("", new ArrayList<MappingAssignment>());
//        }
      return new Mapping("", new ArrayList<MappingAssignment>());
    }


    @Override
    public List<? extends Code> generateDatatypeMethodImplementation(MemberReference memberReference) {
        try {
            logger.debug("Fetching data model for " + memberReference.getMember());
            // get the data model
            DomMethodModel model = DataModelLocator.getDomMethodModel(memberReference.getItem(), null, "");
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
            ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, null);

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
      pluginManagement.generate(correlatedXynaOrder, context);
    }


    @Override
    public XypilotUserConfig loadUserConfig(XynaOrderServerExtension arg0) {
      Generation generation = new Generation();
      try {
        return generation.getConfigFromOrder(arg0);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }


    @Override
    public void storeUserConfig(XynaOrderServerExtension correlatedXynaOrder, XypilotUserConfig config) {
      XypilotUserConfigStorage storage = new XypilotUserConfigStorage();
      String sessionId = correlatedXynaOrder.getSessionId();
      String user = XynaFactory.getInstance().resolveSessionToUser(sessionId);
      config.unversionedSetUser(user);
      try {
        storage.storeConfig(config);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
}
