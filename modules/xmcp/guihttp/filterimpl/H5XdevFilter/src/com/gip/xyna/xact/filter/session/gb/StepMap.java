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
package com.gip.xyna.xact.filter.session.gb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import com.gip.xyna.utils.collections.WrappedMap;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.StepVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

public class StepMap extends WrappedMap<String, Step> {

  public StepMap(WF workflow) {
    super(new HashMap<String, Step>());
    addStep(workflow.getWfAsStep());
  }

  public void addStep(Step step) {
    step.visit( new MapBuilderVisitor(true) );
  }

  public void removeStep(Step step) {
    step.visit( new MapBuilderVisitor(false) );
  }

  public void updateStep(Step step) {
    removeStep(step);
    addStep(step);
  }

  public Step getParentStep(String stepId) {
    ParentFinderVisitor pf = new ParentFinderVisitor(stepId);
    get(stepId).getParentScope().visit( pf );
    return pf.getParent();
  }
  


  
  
  
  
  private class ParentFinderVisitor extends RecursiveVisitor {

    private String stepId;
    private Step parent;

    public ParentFinderVisitor(String stepId) {
      this.stepId = stepId;
    }

    @Override
    public void visit(Step step) {
    }

    @Override
    public boolean beforeRecursion(Step parent, Collection<Step> children) {
      for( Step child : children ) {
        String childStepId = ObjectId.createStepId(child).getBaseId();
        if (Objects.equals(childStepId, stepId)) {
          this.parent = parent;
          return false;
        }
      }
      return true;
    }
    
    public Step getParent() {
      return parent;
    }
  }
  
  private class MapBuilderVisitor extends RecursiveVisitor {
    private boolean add;

    public MapBuilderVisitor(boolean add) {
      this.add = add;
    }

    @Override
    public void visit(Step step) {
      if (step instanceof StepAssign) {
        return;
      }
      
      String stepId = ObjectId.createStepId(step).getBaseId();
      if( add ) {
        put(stepId, step);
      } else {
        remove(stepId);
      }
    }

    @Override
    public boolean beforeRecursion(Step parent, Collection<Step> children) {
      return true;
    }
  }
  
  
  
  
  
  

  public static abstract class CommonStepVisitor implements StepVisitor {
    public abstract void visit( Step step );
    @Override
    public void visitStepSerial( StepSerial step ) { visit(step); }
    @Override
    public void visitStepMapping( StepMapping step ) { visit(step); }
    @Override
    public void visitStepFunction( StepFunction step ) { visit(step); }
    @Override
    public void visitStepCatch( StepCatch step ) { visit(step); }
    @Override
    public void visitStepAssign( StepAssign step ) { visit(step); }
    @Override
    public void visitStepForeach( StepForeach step ) { visit(step); }
    @Override
    public void visitStepParallel(StepParallel step) { visit(step); }
    @Override
    public void visitStepChoice(StepChoice step) { visit(step); }
    @Override
    public void visitStepThrow(StepThrow step) { visit(step); }
    @Override
    public void visitStepRetry( StepRetry step ) { visit(step); }
    @Override
    public void visitScopeStep(ScopeStep step) {visit(step);}
  }

  
  public static abstract class RecursiveVisitor extends CommonStepVisitor {
    public abstract void visit( Step step );
    public abstract boolean beforeRecursion( Step parent, Collection<Step> children );
    
    private void visitRecursion(Step step, Collection<Step> children) {
      visit(step);
      if( beforeRecursion(step, children ) ) {
        for( Step child : children ) {
          child.visit( this );
        }
      }
    }
    
    @Override
    public void visitStepSerial( StepSerial step ) { 
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitStepParallel(StepParallel step) { 
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitStepForeach(StepForeach step) { 
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitScopeStep(ScopeStep step) { 
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitStepChoice(StepChoice step) {
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitStepCatch(StepCatch step) {
      visitRecursion(step, step.getChildSteps() );
    }

    @Override
    public void visitStepFunction(StepFunction step) {
      visitRecursion(step, step.getChildSteps() );
    }

  }

}
