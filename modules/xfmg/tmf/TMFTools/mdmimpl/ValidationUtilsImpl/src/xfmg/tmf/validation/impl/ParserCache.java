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
package xfmg.tmf.validation.impl;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Cache;
import com.gip.xyna.utils.collections.Cache.CacheEntryCreation;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xfmg.tmf.validation.impl.builtinfunctions.ConcatFunction;
import xfmg.tmf.validation.impl.builtinfunctions.EvalFunction;
import xfmg.tmf.validation.impl.builtinfunctions.IfNullFunction;
import xfmg.tmf.validation.impl.builtinfunctions.LengthFunction;
import xfmg.tmf.validation.impl.builtinfunctions.MatchFunction;
import xfmg.tmf.validation.impl.builtinfunctions.NotFunction;
import xfmg.tmf.validation.impl.builtinfunctions.PropertyFunction;
import xfmg.tmf.validation.impl.builtinfunctions.TMFBooleanOperators;
import xfmg.tmf.validation.impl.builtinfunctions.TMFComparatorOperator;
import xfmg.tmf.validation.impl.builtinfunctions.TMFMathOperator;
import xfmg.tmf.validation.impl.builtinfunctions.WorkflowFunction;
import xfmg.tmf.validation.impl.builtinfunctions.WorkflowFunction.WorkflowInfo;
import xfmg.tmf.validation.impl.builtinfunctions.WorkflowFunction.WorkflowInfo.InputType;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;
import xfmg.tmf.validation.impl.functioninterfaces.TMFFunction;



public class ParserCache {

  private static final Cache<Long, TMFExpressionParser> cache = new Cache<>(new CacheEntryCreation<Long, TMFExpressionParser>() {

    @Override
    public TMFExpressionParser create(Long revision) {
      //ideas: regex, split, indexof
      List<TMFDirectFunction> functions = new ArrayList<>();
      functions.add(new EvalFunction());
      functions.add(new LengthFunction());
      functions.add(new ConcatFunction());
      functions.add(new NotFunction());
      functions.add(new PropertyFunction());
      functions.add(new IfNullFunction());
      functions.add(new MatchFunction());
      for (WorkflowInfo wi : findWorkflowFunctions(revision)) {
        functions.add(new WorkflowFunction(wi));
      }
      List<TMFFunction> infixFunctions = new ArrayList<>();
      //TODO ! (=not)
      infixFunctions.add(TMFMathOperator.plus());
      infixFunctions.add(TMFMathOperator.minus());
      infixFunctions.add(TMFMathOperator.multiply());
      infixFunctions.add(TMFMathOperator.divide());
      infixFunctions.add(TMFComparatorOperator.equal());
      infixFunctions.add(TMFComparatorOperator.greaterEqual());
      infixFunctions.add(TMFComparatorOperator.lesserEqual());
      //add after <=, >=, because parser would match to those (function names are tested until first match)
      infixFunctions.add(TMFComparatorOperator.greater());
      infixFunctions.add(TMFComparatorOperator.lesser());
      infixFunctions.add(TMFComparatorOperator.notEqual());
      infixFunctions.add(TMFComparatorOperator.matchLeft());
      infixFunctions.add(TMFComparatorOperator.matchRight());
      infixFunctions.add(TMFBooleanOperators.and());
      infixFunctions.add(TMFBooleanOperators.and2());
      infixFunctions.add(TMFBooleanOperators.or());
      infixFunctions.add(TMFBooleanOperators.or2());
      return new TMFExpressionParser(functions, infixFunctions);
    }
  });


  public static void removeFromCache(Long changedRevision) {
    //invalidate all revisions depending on changed revision
    for (Long revision : dependentRevisions(changedRevision)) {
      cache.removeFromCache(revision);
    }
  }


  private static Set<Long> dependentRevisions(Long changedRevision) {
    Set<Long> set = new HashSet<>();
    set.add(changedRevision);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getDependenciesRecursivly(changedRevision, set);
    return set;
  }


  public static TMFExpressionParser getParser(Long revision) {
    return cache.getOrCreate(revision);
  }


  private static final XynaPropertyString packageNameProperty =
      new XynaPropertyString("xfmg.tmf.validation.constraint.function.package", "xfmg.tmf.validation.constraint.function.workflow")
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "The package name that contains all workflows that are supposed to be used as TMF Constraint functions.");


  private static List<WorkflowInfo> findWorkflowFunctions(Long revision) {
    List<WorkflowInfo> list = new ArrayList<>();
    if (!XynaFactory.hasInstance()) {
      return list;
    }
    /*
     * find all workflows using base.text (in the version used by the revision)
     * that also are reachable from the revision and are in the right package
     * 
     */
    Long revisionDefiningBaseText = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject("base.Text", revision);
    Set<Long> revisionSet = dependentRevisions(revision);
    if (!revisionSet.contains(revisionDefiningBaseText)) {
      return list;
    }

    Set<DependencyNode> deps = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
        .getDependencies("base.Text", DependencySourceType.DATATYPE, revisionDefiningBaseText);
    String packageName = packageNameProperty.get() + ".";
    GenerationBaseCache gbCache = new GenerationBaseCache();
    return deps.stream()
        .filter(dn -> dn.getType() == DependencySourceType.WORKFLOW && revisionSet.contains(dn.getRevision())
            && dn.getUniqueName().startsWith(packageName))
        .map(dn -> createGenerationBase(dn, gbCache)).filter(wf -> isValidWorkflow(wf)).map(wf -> createWorkflowInfo(wf))
        .sorted((wi, wi2) -> Integer.valueOf(wi2.simpleWorkflowName.length()).compareTo(wi.simpleWorkflowName.length()))
        .collect(Collectors.toList());
  }


  private static WorkflowInfo createWorkflowInfo(WF wf) {
    InputType inputType;
    if (wf.getInputVars().size() == 1) {
      if (wf.getInputVars().get(0).isList()) {
        inputType = InputType.TEXT_ARRAY;
      } else {
        inputType = InputType.SINGLE_TEXT;
      }
    } else if (wf.getInputVars().size() == 2) {
      inputType = InputType.TWO_TEXT;
    } else if (wf.getInputVars().size() == 3) {
      inputType = InputType.THREE_TEXT;
    } else if (wf.getInputVars().size() == 4) {
      inputType = InputType.FOUR_TEXT;
    } else {
      throw new RuntimeException();
    }
    return new WorkflowInfo(wf.getOriginalFqName(), wf.getOriginalSimpleName(), inputType);
  }


  private static boolean isValidWorkflow(WF wf) {
    return hasValidInputs(wf) && hasValidOutputs(wf);
  }


  private static boolean hasValidOutputs(WF wf) {
    return wf.getOutputVars().size() == 1 && wf.getOutputVars().get(0).getFQClassName().equals("base.Text");
  }


  private static boolean hasValidInputs(WF wf) {
    switch (wf.getInputVars().size()) {
      case 1 : //ok
        break;
      case 2 :
      case 3 :
      case 4 :
        if (wf.getInputVars().stream().anyMatch(avar -> avar.isList())) {
          return false;
        }
        break;
      default :
        return false;
    }
    return wf.getInputVars().stream().allMatch(avar -> avar.getFQClassName().equals("base.Text"));
  }


  private static WF createGenerationBase(DependencyNode dn, GenerationBaseCache gbCache) {
    WF wf;
    try {
      wf = WF.getOrCreateInstance(dn.getUniqueName(), gbCache, dn.getRevision());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    try {
      wf.parse(true);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
      return wf;
    }
    return wf;
  }

}
