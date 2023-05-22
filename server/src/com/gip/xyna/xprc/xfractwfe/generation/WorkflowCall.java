/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;

/**
 * es gibt unterschiedliche auspr�gungen: {@link WorkflowCallInService}, {@link WorkflowCallServiceReference}
 */
public abstract class WorkflowCall extends Operation {

  protected String wfClassName;
  protected String wfFQClassName;
  protected WF wf;


  public WorkflowCall(DOM parent) {
    super(parent);
  }


  public String getWfFQClassName() {
    return wfFQClassName;
  }


  public WF getWf() {
    return wf;
  }


  protected void getImports(Set<String> imports) {
    //der referenzierte workflow wird hier nicht ben�tigt. das compile im generationbase wird f�r ihn separat aufgerufen
    imports.add(XynaFactory.class.getName());
    imports.add(FractalProcessStep.class.getName());
    for (AVariable v : wf.getInputVars()) {
      imports.add(v.getFQClassName());
    }
    for (AVariable v : wf.getOutputVars()) {
      imports.add(v.getFQClassName());
    }
    for (AVariable v : wf.getAllThrownExceptions()) {
      imports.add(v.getFQClassName());
    }
  }


  @Override
  protected ArrayList<WF> getDependentWFs() {
    ArrayList<WF> l = new ArrayList<WF>();
    l.add(wf);
    return l;
  }


}
