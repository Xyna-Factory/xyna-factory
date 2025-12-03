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
package com.gip.xyna.xact.filter.util;



import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.cache.CachedXynaObjectJsonBuilder;
import com.gip.xyna.xact.filter.session.cache.CachedXynaObjectVisitor;
import com.gip.xyna.xact.filter.session.cache.JsonBuilderCache;
import com.gip.xyna.xact.filter.session.cache.JsonVisitorCache;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.StepMap.RecursiveVisitor;
import com.gip.xyna.xact.filter.util.xo.GenericResult;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;

import xmcp.processmodeller.datatypes.Error;
import xmcp.processmodeller.datatypes.ErrorKeyValuePair;



public class Utils {

  public static final Integer MAX_RECURSIVE_PARENT_SEARCH = 1000;
  private static final Logger logger = CentralFactoryLogging.getLogger(Utils.class);
  public static final String APP_NAME = "GuiHttp";
  private static final RevisionManagement revisionManagement =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private static final JsonBuilderCache builderCache = new JsonBuilderCache();
  private static final JsonVisitorCache visitorCache = new JsonVisitorCache();
  public static final String EXCEPTION_BASE_TYPE = "core.exception.XynaException";
  public static final String EXCEPTION_BASE_TYPE_GUI = "core.exception.XynaExceptionBase";


  private Utils() {
    // nothing
  }

  public static int getHierarchyDepth(Step step) {
    int hierarchyDepth = 0;
    while (!(step instanceof WFStep) && hierarchyDepth < MAX_RECURSIVE_PARENT_SEARCH) {
      step = step.getParentStep();
      hierarchyDepth++;
    }

    return hierarchyDepth;
  }

  public static Operation getOperationByIndex(DOM dom, int index) {
    OperationInformation[] operationInformations = dom.collectOperationsOfDOMHierarchy(true);
    return operationInformations[index].getOperation();
  }

  public static Integer getOperationIndex(Operation operation) {
    OperationInformation[] operationInformations = operation.getParent().collectOperationsOfDOMHierarchy(true);
    for (int i = 0; i < operationInformations.length; i++) {
      if(operationInformations[i].getOperation().getParent().equals(operation.getParent())
          && operationInformations[i].getOperation().equals(operation)) {
        return i;
      }
    }
    return null;
  }

  public static RuntimeContext getGuiHttpRtc() {
    ApplicationManagementImpl am = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    String highestRunningVersion = am.getHighestVersion(APP_NAME, true);
    if(highestRunningVersion == null) {
      return new Workspace(APP_NAME);
    }
    return new Application(APP_NAME, highestRunningVersion);
  }

  public static String xoToJson(GeneralXynaObject generalXynaObject) {
    try {
      return xoToJson(generalXynaObject, getGuiHttpRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }

  public static String xoToJson(GeneralXynaObject generalXynaObject, long revision) {
    StringWriter writer = new StringWriter();
    JsonBuilder jsonBuilder = new JsonBuilder(writer);
    XynaObjectJsonBuilder builder = H5XdevFilter.USE_CACHE.get() ?
      new CachedXynaObjectJsonBuilder(revision, jsonBuilder, builderCache, visitorCache) :
      new XynaObjectJsonBuilder(revision, jsonBuilder);
    builder.build(generalXynaObject);
    return writer.toString();
  }

  public static String xoToJson(GeneralXynaObject generalXynaObject, long revision, long[] backupRevisions) {
    StringWriter writer = new StringWriter();
    JsonBuilder jsonBuilder = new JsonBuilder(writer);
    XynaObjectJsonBuilder builder = H5XdevFilter.USE_CACHE.get() ?
        new CachedXynaObjectJsonBuilder(revision, backupRevisions, jsonBuilder, builderCache, visitorCache) :
        new XynaObjectJsonBuilder(revision, backupRevisions, jsonBuilder);
    builder.build(generalXynaObject);
    return writer.toString();
  }

  public static Long getRtcRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return revisionManagement.getRevision(rtc);
  }

  public static Long getWorkspaceRtcRevision(String workspace) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return revisionManagement.getRevision(null, null, workspace);
  }

  public static xmcp.processmodeller.datatypes.RuntimeContext getModellerRtc(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY{
    RuntimeContext context = revisionManagement.getRuntimeContext(revision);
    if(context instanceof Application) {
      Application application = (Application)context;
      xmcp.processmodeller.datatypes.Application result = new xmcp.processmodeller.datatypes.Application();
      result.setName(application.getName());
      result.setVersion(application.getVersionName());
      return result;
    } else if (context instanceof Workspace) {
      Workspace workspace = (Workspace)context;
      xmcp.processmodeller.datatypes.Workspace result = new xmcp.processmodeller.datatypes.Workspace();
      result.setName(workspace.getName());
      return result;
    }
    return null;
  }

  public static xprc.xpce.RuntimeContext getXpceRtc(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getXpceRtc(revisionManagement.getRuntimeContext(revision));
  }

  public static xprc.xpce.RuntimeContext getXpceRtc(RuntimeContext revisionmgmtRtc) {
    if (revisionmgmtRtc instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application) {
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application application = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)revisionmgmtRtc;
      return new xprc.xpce.Application(application.getName(), application.getVersionName());
    } else {
      return new xprc.xpce.Workspace(revisionmgmtRtc.getName());
    }
  }


  public static GeneralXynaObject convertJsonToGeneralXynaObjectUsingGuiHttp(String json) {
    long revision = -1;
    try {
      revision = getGuiHttpRevision();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return convertJsonToGeneralXynaObject(json, revision);
  }


  public static AVariable convertJsonToAVariable(String json, long revision) {
    if (json == null) {
      return null;
    }
    try {
      JsonParser jp = new JsonParser();
      GenericResult genericResult = jp.parse(json, new ListTypeAwareVistor());
      Util.distributeMetaInfo(genericResult, revision);
      AVariableObjectVisitor visitor = new AVariableObjectVisitor();
      AVariable result = genericResult.visit(visitor, Collections.singletonList(XynaObjectVisitor.META_TAG));
      return result;
    } catch(Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }


  public static GeneralXynaObject convertJsonToGeneralXynaObject(String json) {
    return convertJsonToGeneralXynaObject(json, null);
  }

  public static GeneralXynaObject convertJsonToGeneralXynaObject(String json, Long revision) {
    if(json == null)
      return null;
    try {
      JsonParser jp = new JsonParser();
      GenericResult genericResult = jp.parse(json, new ListTypeAwareVistor());
      Util.distributeMetaInfo(genericResult, revision);
      XynaObjectVisitor xov = H5XdevFilter.USE_CACHE.get() ?
        new CachedXynaObjectVisitor(visitorCache) :
        new XynaObjectVisitor();
      genericResult.visit(xov, Collections.singletonList(XynaObjectVisitor.META_TAG));
      return xov.getAndReset();
    } catch (InvalidJSONException | UnexpectedJSONContentException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static Error error(String message) {
    Error e = new Error();
    e.setMessage(message);
    return e;
  }

  public static Error error(XynaException[] xynaExceptions) {
    return error(xynaExceptions[0]);
  }

  public static Error error(String message, Throwable throwable) {
    Error error = error(throwable);
    error.setMessage(message);
    return error;
  }


  public static Error error(Throwable throwable) {
    logError(throwable);
    Error error = new Error();
    error.setExceptionMessage(throwable.getMessage());
    if( throwable instanceof XynaException ) {
      error.setErrorCode(((XynaException)throwable).getCode());
    }

    if (!H5XdevFilter.SUPPRESS_STACKTRACES.get()) {
      StringBuilder sb = new StringBuilder();
      appendException(sb, throwable);
      error.addToParams(new ErrorKeyValuePair("stackTrace", sb.toString()));
    }
    return error;
  }

  public static void logError(Throwable throwable) {
    logError(throwable.getMessage(), throwable);
  }

  public static void logError(String msg, Throwable throwable) {
    if (logger.isTraceEnabled()) {
      if (throwable == null) {
        logger.trace(msg);
      } else {
        logger.trace(msg, throwable);
      }
    } else if (logger.isDebugEnabled()) {
      logger.debug(msg);
    }
  }

  private static void appendException(StringBuilder sb, Throwable throwable) {
    if( throwable != null ) {
      sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage());
      sb.append("\n\n");
      StringWriter sw = new StringWriter();
      throwable.printStackTrace( new PrintWriter( sw ) );
      sb.append(sw);
    } else {
      sb.append("null");
    }
  }

  public static Long getGuiHttpRevision() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return revisionManagement.getRevision(getGuiHttpRtc());
  }

  /**
   * if lane has no child steps then remove it from wrapping stepParallel and check stepParallel validity
   */
  public static void removeWrapperWhenObsolete(GenerationBaseObject gbo, StepSerial lane) {
    if (lane.getChildSteps().size() > 0) {
      return;
    }

    Step wraper = lane.getParentStep();
    if ( !(wraper instanceof StepParallel) ) {
      return;
    }

    // remove now empty lane from StepParallel
    StepParallel stepParallel = (StepParallel)wraper;
    List<Step> lanes = stepParallel.getChildSteps();
    lanes.remove(lane);

    removeWrapperIfSingleLane(gbo.getStepMap(), stepParallel);
  }

  public static void cleanupStepParallels(StepMap stepMap) {
    // find all StepParallels
    List<StepParallel> stepParallels = new ArrayList<>();
    stepMap.forEach((id, step) -> {
      if (step instanceof StepParallel) {
        stepParallels.add((StepParallel) step);
      }
    });

    for (StepParallel stepParallel : stepParallels) {
      // remove empty lanes

      List<Step> lanes = stepParallel.getChildSteps();
      List<Step> emptyLanes = new ArrayList<>();
      for (Step lane : lanes) {
        if (lane instanceof StepSerial && lane.getChildSteps().isEmpty()) {
          emptyLanes.add(lane);
        }
      }

      for (Step emptyLane : emptyLanes) {
        lanes.remove(emptyLane);
      }

      stepMap.updateStep(stepParallel);

      // remove StepParallel in case it only has one lane
      removeWrapperIfSingleLane(stepMap, stepParallel);
    }
  }

  /**
   * Check stepPrallel for validity and remove it when necessary (only one line remains)
   */
  public static void removeWrapperIfSingleLane(StepMap stepMap, StepParallel stepParallel) {
    List<Step> lanes = stepParallel.getChildSteps();

    if (lanes.size() == 1) {
      // StepParallel has only one remaining lane -> remove StepParallel and add the steps in the lane to the StepParallel's parent
      StepSerial remainingLane = (StepSerial) lanes.get(0);
      StepSerial parent = (StepSerial) stepParallel.getParentStep();
      int indexInParent = parent.getChildSteps().indexOf(stepParallel);
      stepMap.removeStep(parent);
      parent.getChildSteps().addAll(indexInParent, remainingLane.getChildSteps());
      parent.getChildSteps().remove(stepParallel);

      // move all variables from the remaining lane to the parent
      for (AVariable v : remainingLane.getVariablesAndExceptions()) {
        parent.addVar(v);
      }

      stepMap.addStep(parent);
    } else {
      stepMap.updateStep(stepParallel);
    }
  }

  public static List<SharedLib> getSharedLibs(long rootRevision) {
    List <SharedLib> sharedLibs = new ArrayList<SharedLib>();
    Set<Long> visibleRevisions = new HashSet<>();
    Set<String> includedLibNames = new HashSet<>(); //do not add the same shared library from another revision
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(rootRevision, visibleRevisions);
    visibleRevisions.add(rootRevision);

    for (Long revision : visibleRevisions) {
      List<SharedLib> sharedLibsCurRev = XynaFactory.getInstance().getXynaMultiChannelPortal().listSharedLibs(revision);
      if(sharedLibsCurRev == null) {
        continue;
      }
      for(SharedLib candidate : sharedLibsCurRev) {
        if(!includedLibNames.contains(candidate.getName())) {
          includedLibNames.add(candidate.getName());
          sharedLibs.add(candidate);
        }
      }
    }

    return sharedLibs;
  }

  private static class FindGlobalConstVarVisitor extends RecursiveVisitor{
    private AVariable result;
    private String idToFind;


    public FindGlobalConstVarVisitor(String idToFind) {
      this.idToFind = idToFind;
    }

    @Override
    public void visit(Step step) {

      if(!(step instanceof ScopeStep))
        return;

      try {
        VariableIdentification vi = ((ScopeStep)step).identifyVariable(idToFind);
        result = vi.getVariable();
      }
      catch(Exception e) {
        //variable may be in a different scope
      }

    }

    @Override
    public boolean beforeRecursion(Step parent, Collection<Step> children) {
      return true;
    }


    public AVariable getResult() {
      return result;
    }

  }

  public static AVariable getGlobalConstVar(String id, Step wfStep) {
    AVariable result = null;
    FindGlobalConstVarVisitor visitor = new FindGlobalConstVarVisitor(id);

    wfStep.visit(visitor);

    result = visitor.getResult();

    if(result == null) {
      throw new RuntimeException("Global id '" + id + "' not found.");
    }


    return result;
  }

  public static void updateScopeOfSubSteps(Step step) {
    RecursiveVisitor v = new RecursiveVisitor() {

      private ScopeStep scope = step.getParentScope();

      @Override
      public void visit(Step step) {
        step.setParentScope(scope);
        logger.debug("updated Scope of " + step + " to " + scope);
      }

      @Override
      public void visitScopeStep(ScopeStep step) {
        //stop here - do not update beyond this step
        step.setParentScope(scope);
        logger.debug("stop updating scopes. we found a ScopeStep: " + step);
      }


      @Override
      public boolean beforeRecursion(Step parent, Collection<Step> children) {
        return true;
      }
    };

    step.visit(v);
  }


  public static boolean variableExists(AVariable avar) {

    if (avar == null) {
      return false;
    }

    //anyType
    if (avar.getDomOrExceptionObject() == null) {
      return true;
    }

    if (!avar.getDomOrExceptionObject().exists()) {
      return false;
    }

    return true;
  }


  public static boolean isValidWorkflowReference(DOM dom, Operation operation, WF wf) {

    List<DomOrExceptionGenerationBase> operationInputs =  new ArrayList<DomOrExceptionGenerationBase>();
    List<DomOrExceptionGenerationBase> operationOutputs = new ArrayList<DomOrExceptionGenerationBase>();
    List<DomOrExceptionGenerationBase> wfInputs = new ArrayList<DomOrExceptionGenerationBase>();
    List<DomOrExceptionGenerationBase> wfOutputs = new ArrayList<DomOrExceptionGenerationBase>();

    operationInputs.add(dom);
    for (AVariable var : operation.getInputVars()) {
      operationInputs.add(var.getDomOrExceptionObject());
    }

    for (AVariable var : operation.getOutputVars()) {
      operationOutputs.add(var.getDomOrExceptionObject());
    }

    for (AVariable var : wf.getInputVars()) {
      wfInputs.add(var.getDomOrExceptionObject());
    }

    for (AVariable var : wf.getOutputVars()) {
      wfOutputs.add(var.getDomOrExceptionObject());
    }

    if (operationInputs.size() != wfInputs.size() || operationOutputs.size() != wfOutputs.size()) {
      return false;
    }

    for (int i = 0; i < operationInputs.size(); i++) {
      DomOrExceptionGenerationBase operationInput = operationInputs.get(i);
      DomOrExceptionGenerationBase wfInput = wfInputs.get(i);

      if (!DomOrExceptionGenerationBase.isSuperClass(wfInput, operationInput)) {
        return false;
      }
    }

    for (int i = 0; i < operationOutputs.size(); i++) {
      DomOrExceptionGenerationBase operationOutput = operationOutputs.get(i);
      DomOrExceptionGenerationBase wfOutput = wfOutputs.get(i);

      if (!DomOrExceptionGenerationBase.isSuperClass(operationOutput, wfOutput)) {
        return false;
      }
    }

    return true;
  }

  public static  List<AVariable> getServiceInputVars(StepFunction step) {
    Service service = step.getService();
    if (service.getWF() != null) {
      return service.getWF().getInputVars();
    } else if (service.getDom() != null) {
      return getDOMServiceInputVars(step);
    } else {
      return service.getInputVars();
    }
  }


  public static  List<AVariable> getServiceOutputVariables(StepFunction step) {
    Service service = step.getService();
    if (service.getWF() != null) {
      return service.getWF().getOutputVars();
    } else if (service.getDom() != null) {
      return getDOMServiceOutputVars(step);
    } else {
      return service.getOutputVars();
    }
  }


  public static  List<AVariable> getDOMServiceInputVars(StepFunction step) {
    Operation operation = null;

    try {
      operation = getDOMServiceOperation(step);
    } catch (XPRC_OperationUnknownException e) {
      return Collections.emptyList();
    }

    List<AVariable> result = new ArrayList<AVariable>();

    //add 'this'
    if (!operation.isStatic()) {
      DatatypeVariable dtv = new DatatypeVariable(step.getService().getDom());
      dtv.replaceDomOrException(step.getService().getDom(), "");
      ServiceVariable sv = new ServiceVariable(dtv);
      result.add(sv);
    }

    result.addAll(operation.getInputVars());
    return result;
  }


  public static  List<AVariable> getDOMServiceOutputVars(StepFunction step) {
    Operation operation = null;

    try {
      operation = getDOMServiceOperation(step);
    } catch (XPRC_OperationUnknownException e) {
      return Collections.emptyList();
    }

    return operation.getOutputVars();
  }


  public static Operation getDOMServiceOperation(StepFunction step) throws XPRC_OperationUnknownException {
    Service service = step.getService();
    String operationName = step.getOperationName();
    Operation operation = null;
    operation = service.getDom().getOperationByName(operationName);
    return operation;
  }


  public static int clipIndex(int index, List<? extends Object> list) {
    int clippedIndex = index;
    if (index < 0) {
      clippedIndex = list.size();
    } else if (index > list.size()) {
      clippedIndex = list.size();
    }

    return clippedIndex;
  }

  private static Pattern numberPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

  public static boolean isNumeric(String string) {
    if (string == null) {
      return false;
    } else {
      return numberPattern.matcher(string).matches();
    }
  }

  public static SearchRequestBean createReferencesSearchRequestBean(String selection, HashMap<String, String> filters) {
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
    srb.setMaxRows(-1);
    srb.setSelection(selection);
    if(filters != null) {
      srb.setFilterEntries(filters);
    }
    return srb;
  }


  public static void writeMetaData(JsonBuilder jb, String fqn, boolean includeRtc) {
    jb.addObjectAttribute(Tags.META); {
      jb.addStringAttribute(Tags.FQN, fqn);

      if (includeRtc) {
        jb.addObjectAttribute(Tags.RTC); {
          getRtcXmomContainers().toJson(jb);
        } jb.endObject();
      }
    } jb.endObject();
  }


  private static RuntimeContextJson getRtcXmomContainers() {
    return new RuntimeContextJson(getGuiHttpRtc());
  }


  public static Set<GenerationBase> getSubTypes(String fqn, GenerationBaseCache commonCache, Long rootRev) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    if (!fqn.equals(DatatypeVariable.ANY_TYPE)) {
      DomOrExceptionGenerationBase gb = (DomOrExceptionGenerationBase)GenerationBase.getOrCreateInstance(fqn, commonCache, rootRev);
      return gb.getSubTypes(commonCache);
    }

    // get all data types and exception types
    WorkflowDatabase workflowDatabase = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();

    // filter out the ones that are visible from the given RTC

    Set<GenerationBase> subTypes = new HashSet<>();
    Set<Long> visibleRevs = new HashSet<>();
    visibleRevs.add(rootRev);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(rootRev, visibleRevs);

    HashMap<Long, List<String>> dtsByRev = workflowDatabase.getDeployedDatatypes();
    for (long rev : dtsByRev.keySet()) {
      if (visibleRevs.contains(rev)) {
        for (String visibleFqn : dtsByRev.get(rev)) {
          if (isExcludedType(visibleFqn) ) {
            continue;
          }

          try {
            DOM dom = DOM.getOrCreateInstance(visibleFqn, commonCache, rev);
            dom.parseGeneration(true /*deployed*/, false, false);
            subTypes.add(dom);
          } catch (Exception e) {
            logger.warn("Could not parse type " + visibleFqn, e);
          }
        }
      }
    }

    HashMap<Long, List<String>> exceptionsByRev = workflowDatabase.getDeployedExceptions();
    for (long rev : exceptionsByRev.keySet()) {
      if (visibleRevs.contains(rev)) {
        for (String visibleFqn : exceptionsByRev.get(rev)) {
          if (isExcludedType(visibleFqn) ) {
            continue;
          }

          try {
            ExceptionGeneration exceptionGeneration = ExceptionGeneration.getOrCreateInstance(visibleFqn, commonCache, rev);
            exceptionGeneration.parseGeneration(true /*deployed*/, false, false);
            subTypes.add(exceptionGeneration);
          } catch (Exception e) {
            logger.warn("Could not parse type " + visibleFqn, e);
          }
        }
      }
    }

    return subTypes;
  }


  public static boolean isSubtypeOf(String fqnSubClass, String fqnSuperClass, Long rootRev) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    Set<GenerationBase> subTypes = getSubTypes(fqnSuperClass, new GenerationBaseCache(), rootRev);
    for (GenerationBase subType : subTypes) {
      if (subType.getOriginalFqName().equals(fqnSubClass)) {
        return true;
      }
    }

    return false;
  }


  public static boolean isExcludedType(String fqn) {
    return GenerationBase.CORE_EXCEPTION.equals(fqn) ||
        EXCEPTION_BASE_TYPE.equals(fqn) ||
        EXCEPTION_BASE_TYPE_GUI.equals(fqn);
  }

}