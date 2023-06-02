/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package xmcp.processmonitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

public class Dataflow {

  public static Pair<AVariable, Step> determineSource(ScopeStep parentScope, AVariable parameter, Step stepWithParameterInput, String sourceId,
                                                List<Integer> foreachIndices, int retryCounter) {
    /*
     * von einer variable zur nächsten hangeln, bis man an einem output eines schrittes angekommen ist
     * man muss das rekursiv machen, weil in den auditdetails nur die ausgeführten schritte angezeigt werden, aber nicht choices etc
     * falls man auf den output einer choice verbinden wollen würde (wie im alten flash monitor), muss man die rekursion reduzieren/weglassen.
     * 
     * findSource(X)
     *   1: X=workflow-output
     *   2: X=step-input
     *   3: X=assign-input
     *   4: X=step-output in lokalem catchblock
     *   5: X=workflow-output in globalem catchblock
     *   1+4+5:
     *      Fall A: -> assign -> assign-input -> rekursion: findSource(assign-input)
     *   2+3: 
     *      Fall A: -> choice-output -> assign -> assign-input -> rekursion: findSource(assign-input)
     *      Fall B: -> step-output/workflow-input -> END
     * 
     * wie identifiziert man eine step-output-variable?
     * 1. id genügt für eindeutigkeit
     * 
     * source von objekt innerhalb von Foreach kann auf objekt innerhalb von Foreach oder von vorne dran zeigen.
     * source von objekt kann nie von außerhalb von Foreach auf innerhalb von Foreach zeigen. nur auf output von foreach-scope.
     * 
     * 
     * die parameter haben alle ids+source/target-ids gesetzt. 
     * die werden beim parsen (Step.parseParameter) des audit-xmls von den step-inputs/outputs übernommen.
     */

    String id = parameter.getId();

    /*
     * workflow-output: sourceId von parameter ist stepid von assign
     *   in assign zugehörige inputid suchen -> rekursion
     * stepfunction/mapping/assign-input: id von globaler variable
     *   suche, wo id zugewiesen wird (stepid steht als sourceid am dataobjekt. aber da können mehrere sourceids stehen
     *   (was derzeit nicht unterstützt wird -> lieber über alle steps iterieren und entsprechenden output suchen, output-/targetvarids = x?)
     *     1) stepfunction/stepmapping
     *       return (step, outputvar)
     *     2) assign von stepchoice
     *       im assign zugehörige inputid suchen -> rekursion
     *     3) workflow-input
     *       return (wfstep, wfinputvar)
     *     4) foreach-outputlist.refid
     *       ??
     */
    if (stepWithParameterInput instanceof WFStep) {
      //die inputs des wf-outputs findet man immer über das globale assign am ende des workflows
      WFStep wfStep = (WFStep) stepWithParameterInput;
      StepAssign stepAssign = (StepAssign) wfStep.getChildStep().getChildSteps().get(wfStep.getChildStep().getChildSteps().size()-1);
      AVariable inputVar = stepAssign.getInputVars().get(indexOf(id, stepAssign.getOutputVarIds()));
      return determineSource(parentScope, inputVar, stepAssign, inputVar.getId(), foreachIndices, retryCounter);
    }

    if (stepWithParameterInput instanceof StepFunction || stepWithParameterInput instanceof StepMapping
        || stepWithParameterInput instanceof StepAssign || stepWithParameterInput instanceof StepForeach
        || stepWithParameterInput instanceof StepChoice) {
      //suchen wo sourceId zugewiesen wird
      for (Step step : allSteps(parentScope)) {
        if (step instanceof StepCatch) {
          continue;
        }
        int foreachDepth = calculateForeachDepth(step);
        if (foreachIndices != null && foreachDepth > foreachIndices.size()) {
          continue;
        }
        List<Integer> foreachIndicesOfStep = foreachIndicesForDepth(foreachIndices, foreachDepth);
        /*
         * TODO das gleiche für retrycounter... die können auch verschachtelt sein. dazu muss xyna das aber auch in den parametern richtig unterstützen
         * 
         * wann muss man den retrycounter von != -1 auf -1 setzen? wenn man beim "weiterhangeln" den bereich verlässt, wo das retry passiert.
         * bei lokalem retry also wenn man schritte vor dem fehlgeschlagenen service betrachtet.
         * 
         * befindet sich ein schritt nicht in einem catchblock, muss retrycounter sich auf globalen retry beziehen
         * befindet sich ein schritt in einem catchblock, gibt es folgende fälle:
         * 1) es gibt globalen retry und der retrycounter bezieht sich darauf
         * 2) es gibt lokalen retry und der retrycounter bezieht sich darauf
         * 3) es gibt globalen UND lokalen retry (dann müsste es 2 retrycounter geben)
         * 4)-6) es gibt zwar retry-schritte, aber die sind nicht angesprungen 
         * 
         */
        int retryDepth = calculateRetryDepth(step);
        int retryCounterForStep = retryCounter;
        if (retryCounter >= 0 && retryDepth == 0) {
          retryCounterForStep = -1;
        }
        if (!(step instanceof StepForeach) && step.getParameter(foreachIndicesOfStep, retryCounterForStep) == null) {
          //schritt weiter unten oder schritt in choice-lane, die nicht ausgeführt wurde, etc
          continue;
        }
        if (step instanceof WFStep) {// wf.input.targetid
          //inputs von wf checken
          for (int i = 0; i < step.getInputVars().size(); i++) {
            String inputId = step.getInputVars().get(i).getId();
            if (inputId.equals(sourceId)) {
              return Pair.of(step.getInputVars().get(i), step);
            }
          }
        } else if (step instanceof StepChoice) {
          //assigns haben keine parameterlist. deshalb findet man bei choices das passende assign nur dadurch, dass man die parameterlisten pro lane in stepchoice betrachtet
          StepChoice choice = (StepChoice) step;
          for (int i = 0; i < choice.getComplexCaseNames().size(); i++) {
            if (choice.laneHasParameter(i, foreachIndicesOfStep, retryCounterForStep)) {
              StepSerial ss = (StepSerial) choice.getChildSteps().get(i);
              StepAssign sa = (StepAssign) ss.getChildSteps().get(ss.getChildSteps().size()-1);
              int idx = indexOf(id, sa.getOutputVarIds());
              if (idx >= 0) {
                AVariable inputVar = sa.getInputVars().get(idx);
                return determineSource(parentScope, inputVar, sa, inputVar.getId(), foreachIndicesOfStep, retryCounterForStep);
              }
            }
          }
        } else if (step instanceof StepForeach) {
          //ist vielleicht die Input-Liste des Foreaches zu verbinden?
          for (int i = 0; i<step.getInputVarIds().length; i++) {
            String inputId = ((StepForeach) step).getInputVarsSingle()[i].getId();
            if (inputId.equals(sourceId)) {
              AVariable inputList = step.getInputVars().get(i);
              return determineSource(parentScope,inputList, step, step.getInputVarIds()[i], foreachIndicesOfStep, retryCounterForStep);
            }
          }
        } else {
          //outputs von step checken
          for (int i = 0; i < step.getOutputVarIds().length; i++) {
            String outputId = step.getOutputVarIds()[i];
            if (outputId.equals(sourceId)) { 
              if (step instanceof StepFunction || step instanceof StepMapping) {// step.target.refid / step.output.targetid
                return Pair.of(step.getOutputVars().get(i), step);
              }
              throw new RuntimeException("unsupported step type: " + step.getClass().getName());
            }
          }
        }

      }
    }
    return null; // dann konstant vorbelegt
  }

  public static String createDataflowId(Step step, AVariable var) {
    String stepId = (step != null) ? step.getStepId() : null;
    String varId = (var != null) ? var.getId() : null;
    
    if (stepId == null) {
      return varId;
    } else if (varId == null) {
      return stepId;
    } else {
      return "step" + stepId + "-var" + varId;
    }
  }

  public static List<Step> collectAllSteps(Step rootStep) {
    List<Step> steps = new ArrayList<Step>();
    steps.add(rootStep);
    
    if (rootStep.getChildSteps() != null) {
      for (Step childStep : rootStep.getChildSteps()) {
        steps.addAll(collectAllSteps(childStep));
      }
    }
    
    return steps;
  }

  //FIXME
  private static int calculateRetryDepth(Step step) {
    if (step.getFirstParameter() == null || step.getFirstParameter().getRetryCounter() < 0) {
      return 0;
    }
    return 1;
  }

  /*
   * gibt die tiefe D in dem sinne zurück, dass step-parameter bzgl dieses steps D foreachindices beinhalten.
   * d.h. toplevel step => 0
   *     step innerhalb von foreach => 1
   *     step innerhalb von foreach innerhalb von foreach => 2
   *     usw  
   */
  private static int calculateForeachDepth(Step step) {
    ScopeStep scope = step.getParentScope();
    int depth = -1;
    while (scope != null) {
      depth ++;
      scope = scope.getParentScope();
    }
    if (depth == -1) {
      depth = 0;
    }
    return depth;
  }

  private static List<Integer> foreachIndicesForDepth(List<Integer> foreachIndices, int foreachDepth) {
    if (foreachIndices == null || foreachDepth == 0) {
      return null;
    }
    if (foreachIndices.size() == foreachDepth) {
      return foreachIndices;
    }
    List<Integer> ret = new ArrayList<>();
    for (int i = 0; i<foreachDepth; i++) {
      ret.add(foreachIndices.get(i));
    }
    return ret;
  }

  private static Set<Step> allSteps(ScopeStep scope) {
    if (scope instanceof WFStep) {
      return ((WFStep)scope).getAllStepsRecursively();
    }
    return ((WFStep)scope.getParentWFObject().getWfAsStep()).getAllStepsRecursively();
  }

  private static int indexOf(String idToFind, String[] varIds) {
    if (idToFind == null) {
      return -1;
    }

    for (int varNo = 0; varNo < varIds.length; varNo++) {
      if (idToFind.equals(varIds[varNo])) {
        return varNo;
      }
    }

    return -1;
  }

}
