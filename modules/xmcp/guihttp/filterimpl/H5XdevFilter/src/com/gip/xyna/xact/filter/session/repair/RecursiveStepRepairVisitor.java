/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xact.filter.session.repair;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import com.gip.xyna.xact.filter.session.gb.StepMap.RecursiveVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.RepairEntry;



public class RecursiveStepRepairVisitor extends RecursiveVisitor {

  private List<BiFunction<Step, Boolean, List<RepairEntry>>> genericStepRepairs;
  private List<BiFunction<StepFunction, Boolean, List<RepairEntry>>> stepFunctionRepairs;
  private List<BiFunction<StepThrow, Boolean, List<RepairEntry>>> stepThrowRepairs;
  private List<BiFunction<StepChoice, Boolean, List<RepairEntry>>> stepChoiceRepairs;
  private List<BiFunction<StepMapping, Boolean, List<RepairEntry>>> stepMappingRepairs;
  private List<BiFunction<WFStep, Boolean, List<RepairEntry>>> wfStepRepairs;
  private List<BiFunction<StepCatch, Boolean, List<RepairEntry>>> stepCatchRepairs;
  private List<BiFunction<StepParallel, Boolean, List<RepairEntry>>> stepParallelRepairs;
  private List<BiFunction<StepForeach, Boolean, List<RepairEntry>>> stepForeachRepairs;

  private final List<RepairEntry> repairEntries;
  private final boolean apply;


  //HashMap<Class<T>, BiFunction<T, Boolean, List<RepairEntry>>> .. ?

  public RecursiveStepRepairVisitor(boolean apply) {
    repairEntries = new ArrayList<RepairEntry>();
    stepFunctionRepairs = new ArrayList<BiFunction<StepFunction, Boolean, List<RepairEntry>>>();
    genericStepRepairs = new ArrayList<BiFunction<Step, Boolean, List<RepairEntry>>>();
    stepThrowRepairs = new ArrayList<BiFunction<StepThrow, Boolean, List<RepairEntry>>>();
    stepChoiceRepairs = new ArrayList<BiFunction<StepChoice, Boolean, List<RepairEntry>>>();
    stepMappingRepairs = new ArrayList<BiFunction<StepMapping, Boolean, List<RepairEntry>>>();
    wfStepRepairs = new ArrayList<BiFunction<WFStep, Boolean, List<RepairEntry>>>();
    stepCatchRepairs = new ArrayList<BiFunction<StepCatch, Boolean, List<RepairEntry>>>();
    stepParallelRepairs = new ArrayList<BiFunction<StepParallel,Boolean,List<RepairEntry>>>();
    stepForeachRepairs = new ArrayList<BiFunction<StepForeach, Boolean, List<RepairEntry>>>();
    this.apply = apply;
  }


  public void addStepFunctionRepair(BiFunction<StepFunction, Boolean, List<RepairEntry>> func) {
    stepFunctionRepairs.add(func);
  }


  public void addGenericStepRepair(BiFunction<Step, Boolean, List<RepairEntry>> func) {
    genericStepRepairs.add(func);
  }


  public void addStepThrowRepair(BiFunction<StepThrow, Boolean, List<RepairEntry>> func) {
    stepThrowRepairs.add(func);
  }


  public void addStepChoiceRepair(BiFunction<StepChoice, Boolean, List<RepairEntry>> func) {
    stepChoiceRepairs.add(func);
  }


  public void addStepMappingRepair(BiFunction<StepMapping, Boolean, List<RepairEntry>> func) {
    stepMappingRepairs.add(func);
  }


  public void addWFStepRepair(BiFunction<WFStep, Boolean, List<RepairEntry>> func) {
    wfStepRepairs.add(func);
  }


  public void addStepCatchRepair(BiFunction<StepCatch, Boolean, List<RepairEntry>> func) {
    stepCatchRepairs.add(func);
  }
  
  public void addStepParallelRepair(BiFunction<StepParallel, Boolean, List<RepairEntry>> func) {
    stepParallelRepairs.add(func);
  }
  
  public void addStepForeachRepair(BiFunction<StepForeach, Boolean, List<RepairEntry>> func) {
    stepForeachRepairs.add(func);
  }


  @Override
  public void visit(Step step) {

    //can't repair StepAssign
    if (step instanceof StepAssign) {
      return;
    }

    for (BiFunction<Step, Boolean, List<RepairEntry>> func : genericStepRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
  }


  @Override
  public boolean beforeRecursion(Step parent, Collection<Step> children) {
    return false;
  }


  @Override
  public void visitStepChoice(StepChoice step) {
    for (BiFunction<StepChoice, Boolean, List<RepairEntry>> func : stepChoiceRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepChoice(step);
  }


  @Override
  public void visitStepFunction(StepFunction step) {
    for (BiFunction<StepFunction, Boolean, List<RepairEntry>> func : stepFunctionRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepFunction(step);
  }


  @Override
  public void visitStepThrow(StepThrow step) {
    for (BiFunction<StepThrow, Boolean, List<RepairEntry>> func : stepThrowRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepThrow(step);
  }


  @Override
  public void visitStepMapping(StepMapping step) {
    for (BiFunction<StepMapping, Boolean, List<RepairEntry>> func : stepMappingRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepMapping(step);
  }


  @Override
  public void visitScopeStep(ScopeStep step) {
    if (step instanceof WFStep) {
      for (BiFunction<WFStep, Boolean, List<RepairEntry>> func : wfStepRepairs) {
        repairEntries.addAll(func.apply((WFStep) step, apply));
      }
    }
    super.visitScopeStep(step);
  }

  
  @Override
  public void visitStepForeach(StepForeach step) {
    for (BiFunction<StepForeach, Boolean, List<RepairEntry>> func : stepForeachRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepForeach(step);
  }

  @Override
  public void visitStepCatch(StepCatch step) {
    for (BiFunction<StepCatch, Boolean, List<RepairEntry>> func : stepCatchRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepCatch(step);
  }
  
  @Override
  public void visitStepParallel(StepParallel step) {
    for (BiFunction<StepParallel, Boolean, List<RepairEntry>> func : stepParallelRepairs) {
      repairEntries.addAll(func.apply(step, apply));
    }
    super.visitStepParallel(step);
  }
  


  public List<RepairEntry> getResult() {
    return repairEntries;
  }
}
