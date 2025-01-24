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



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.Text;
import xmcp.xypilot.CodeAnalysisResult;
import xmcp.xypilot.CodeSuggestion;
import xmcp.xypilot.Documentation;
import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.Mapping;
import xmcp.xypilot.MemberReference;
import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.MetricEvaluationResult;
import xmcp.xypilot.NoXyPilotUserConfigException;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.XypilotUserConfig;
import xmcp.xypilot.impl.config.XypilotUserConfigStorage;
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
import xmcp.xypilot.metrics.Metric;
import xmcp.xypilot.metrics.Score;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.Workspace;



public class Generation {

  private XMOMItemReference buildItemFromContext(Context context) {
    XMOMItemReference.Builder builder = new XMOMItemReference.Builder();
    builder.fqName(context.getFQN());
    builder.workspace(((Workspace)context.getRuntimeContext()).getName());
    return builder.instance();
  }

  private String createCorrelation(XMOMItemReference ref, String type) {
    return String.format("%s-%s-WS:\"%s\"", type, ref.getFqName(), ref.getWorkspace());
  }


  private void publishUpdateMessage(XMOMItemReference xmomItemReference, String type) throws XynaException {
    String correlation = createCorrelation(xmomItemReference, type);
    MessageInputParameter message = new MessageInputParameter("Xyna", "Process Modeller Autosaves", correlation, "", null, false);
    XynaFactory.getInstance().Publish(message);
  }


  public void genDatatypeDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    DomModel model = DataModelLocator.getDomModel(xmomItemReference, order);
    Pipeline<Documentation, DomModel> pipeline = PipelineLocator.getPipeline(config, "dom-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateDomDocu(doc, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "DataType");
  }
  
  public void genDatatypeVariables(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    DomModel model = DataModelLocator.getDomModel(xmomItemReference, order);
    Pipeline<List<? extends MemberVariable>, DomModel> pipeline = PipelineLocator.getPipeline(config, "dom-variables");
    List<? extends MemberVariable> vars = pipeline.run(model, config.getUri()).firstChoice();
    FilterCallbackInteractionUtils.addDomVars(vars, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "DataType");
  }
  
  public void genDatatypeVarDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    DomVariableModel model = DataModelLocator.getDomVariableModel(xmomItemReference, order, context.getObjectId());
    Pipeline<Documentation, DomVariableModel> pipeline = PipelineLocator.getPipeline(config, "dom-variable-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateDomVarDocu(doc, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "DataType");
  }
  
  public void genMethods(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    DomModel model = DataModelLocator.getDomModel(xmomItemReference, order);
    Pipeline<List<? extends MethodDefinition>, DomModel> pipeline = PipelineLocator.getPipeline(config, "dom-methods");
    List<? extends MethodDefinition> methods = pipeline.run(model, config.getUri()).firstChoice();
    FilterCallbackInteractionUtils.addDomMethods(methods, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "DataType");
  }

  public void genDatatypeMethodDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    String type = DataModelLocator.datatypesTypeName;
    DomMethodModel model = DataModelLocator.getDomMethodModel(xmomItemReference, order, context.getObjectId(), type);
    Pipeline<Documentation, DomMethodModel> pipeline = PipelineLocator.getPipeline(config, "dom-method-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateDomVarDocu(doc, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "DataType");
  }
  
  public void genDatatypeMethodImpl(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    String type = DataModelLocator.datatypesTypeName;
    DomMethodModel model = DataModelLocator.getDomMethodModel(xmomItemReference, order, context.getObjectId(), type);
    Pipeline<Code, DomMethodModel> pipeline = PipelineLocator.getPipeline(config, "dom-method-implementation");
    Code code = pipeline.run(model, config.getUri()).firstChoice();
    code.unversionedSetText(model.getLatestImplementation() + code.getText());
    FilterCallbackInteractionUtils.updateDomMethodImpl(code, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "DataType");
  }

  public void genExceptionDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, order);
    Pipeline<Documentation, ExceptionModel> pipeline = PipelineLocator.getPipeline(config, "exception-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateExceptionDocu(doc, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "ExceptionType");
  }
  
  public void genExceptionMessages(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, order);
    Pipeline<List<ExceptionMessage>, ExceptionModel> pipeline = PipelineLocator.getPipeline(config, "exception-messages");
    List<ExceptionMessage> excMess = pipeline.run(model, config.getUri()).firstChoice();
    FilterCallbackInteractionUtils.updateExceptionMessages(excMess, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "ExceptionType");
  }
  
  public void genExceptionVariables(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    ExceptionModel model = DataModelLocator.getExceptionModel(xmomItemReference, order);
    Pipeline<List<? extends MemberVariable>, ExceptionModel> pipeline = PipelineLocator.getPipeline(config, "exception-variables");
    List<? extends MemberVariable> vars = pipeline.run(model, config.getUri()).firstChoice();
    FilterCallbackInteractionUtils.addExceptionVars(vars, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "ExceptionType");
  }
  
  public void genExceptionVarDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    ExceptionVariableModel model = DataModelLocator.getExceptionVariableModel(xmomItemReference, order, context.getObjectId());
    Pipeline<Documentation, ExceptionVariableModel> pipeline = PipelineLocator.getPipeline(config, "exception-variable-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateExceptionVarDocu(doc, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "ExceptionType");
  }

  public XypilotUserConfig getConfigFromOrder(XynaOrderServerExtension order) throws PersistenceLayerException, NoXyPilotUserConfigException {
    String sessionId = order.getSessionId();
    String user = XynaFactory.getInstance().resolveSessionToUser(sessionId);
    XypilotUserConfigStorage storage = new XypilotUserConfigStorage(order);
    XypilotUserConfig config = storage.loadConfig(user);
    if(config == null) {
      throw new NoXyPilotUserConfigException(user);
    }
    return config;
  }
  
  public void genServiceGroupMethodDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    String type = DataModelLocator.serviceGroupsTypeName;
    DomMethodModel model = DataModelLocator.getDomMethodModel(xmomItemReference, order, context.getObjectId(), type);
    Pipeline<Documentation, DomMethodModel> pipeline = PipelineLocator.getPipeline(config, "dom-method-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    doc.unversionedSetText(model.getLatestDocumentation() + doc.getText());
    FilterCallbackInteractionUtils.updateDomVarDocu(doc, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "DataType");
  }

  public void genServiceGroupMethodImpl(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    String type = DataModelLocator.serviceGroupsTypeName;
    DomMethodModel model = DataModelLocator.getDomMethodModel(xmomItemReference, order, context.getObjectId(), type);
    Pipeline<Code, DomMethodModel> pipeline = PipelineLocator.getPipeline(config, "dom-method-implementation");
    Code code = pipeline.run(model, config.getUri()).firstChoice();
    code.unversionedSetText(model.getLatestImplementation() + code.getText());
    FilterCallbackInteractionUtils.updateDomMethodImpl(code, order, xmomItemReference, context.getObjectId());
    publishUpdateMessage(xmomItemReference, "DataType");
  }
  
  
  public void genMappingLabel(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    MemberReference.Builder builder = new MemberReference.Builder();
    builder.member(context.getObjectId()).item(xmomItemReference);
    MemberReference memberReference = builder.instance();
    MappingModel model = DataModelLocator.getMappingModel(memberReference, order);
    Pipeline<Text, MappingModel> pipeline = PipelineLocator.getPipeline(config, "mapping-label");
    Text text = pipeline.run(model, config.getUri()).firstChoice();
    String labelAreaId = String.format("labelArea%s", context.getObjectId().substring(4)); // ObjectId = "step[ID]"
    FilterCallbackInteractionUtils.updateMappingLabel(text.getText(), order, memberReference.getItem(), labelAreaId);
    publishUpdateMessage(xmomItemReference, "Workflow");
  }
  
  public void genMappingAssignments(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    MemberReference.Builder builder = new MemberReference.Builder();
    builder.member(context.getObjectId()).item(xmomItemReference);
    MemberReference memberReference = builder.instance();
    MappingModel model = DataModelLocator.getMappingModel(memberReference, order);
    Pipeline<Mapping, MappingModel> pipeline = PipelineLocator.getPipeline(config, "mapping-assignments");
    Mapping mapping = pipeline.run(model, config.getUri()).firstChoice();
    var expressions = model.getMapping().getRawExpressions();
    int lastAssignmentId = model.getMapping().getRawExpressions().size() - 1;
    String exp = lastAssignmentId > -1 ? expressions.get(lastAssignmentId) : "";
    FilterCallbackInteractionUtils.updateMappingAssignments(mapping, order, xmomItemReference, context.getObjectId(), lastAssignmentId, exp);
    publishUpdateMessage(xmomItemReference, "Workflow");
  }

  public List<? extends CodeSuggestion> genCodeSuggestions(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    String type = DataModelLocator.datatypesTypeName;
    DomMethodModel model = DataModelLocator.getDomMethodModel(xmomItemReference, order, context.getObjectId(), type);
    Pipeline<Code, DomMethodModel> pipeline = PipelineLocator.getPipeline(config, "dom-method-implementation");
    List<Code> code = pipeline.run(model, config.getUri()).choices();
    if (config.getMaxSuggestions() < code.size()) {
      code = code.subList(0, config.getMaxSuggestions());
    }
    var metrics = config.getSelectedMetricList().stream().filter(x -> x.getSelected()).map(x -> x.getMetric()).collect(Collectors.toList());
    return evaludateCodeSuggestions(code, metrics);
  }

  
  private List<? extends CodeSuggestion> evaludateCodeSuggestions(List<Code> codes, List<? extends Metric> metrics) {
    CodeAnalysisResult.Builder codeAnalysisResultBuilder = new CodeAnalysisResult.Builder();
    MetricEvaluationResult.Builder metricResultBuilder;
    List<MetricEvaluationResult> metricResults = new ArrayList<>(codes.size());
    List<Score> scores;
    for(Metric metric : metrics) {
      metric.init();
      scores = new ArrayList<>(codes.size());
      for(Code code: codes) {
        Score score = metric.computeScore(code);
        scores.add(score);
      }
      @SuppressWarnings("unchecked")
      List<Score> normalizedScores = (List<Score>) metric.normalizeScores(scores);
      metricResultBuilder = new MetricEvaluationResult.Builder();
      metricResultBuilder.metric(metric).scores(scores).normalizedScores(normalizedScores);
      metricResults.add(metricResultBuilder.instance());
    }
    codeAnalysisResultBuilder.codes(codes).metricResults(metricResults);
    return codeAnalysisResultBuilder.instance().buildSuggestions();
  }

  @FunctionalInterface
  public interface GenerationInterface {

    void apply(XynaOrderServerExtension order, Context context) throws Exception;
  }
}
