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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.base.ForEachScope;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;


public class ForEachScopeStep extends ScopeStep {

  public static final String VARNAME_forEachIndex = "forEachIndex";
  
  private static final String _METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX_ORIG = "getForEachIndex";
  protected static final String METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX;
  
  private XynaPropertySupport xynaPropertySupport;

  
  static {
    //methoden namen auf diese art gespeichert k�nnen von obfuscation tools mit "refactored" werden.
    // ForEachScope
    try {
      METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX = ForEachScope.class.getDeclaredMethod(_METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX_ORIG + " not found", e);
    }
  }


  public ForEachScopeStep(ScopeStep parentScope, AVariable[] localInputVars, AVariable[] localOutputVars, GenerationBase creator) {
    super(parentScope, localInputVars, localOutputVars, creator);
    this.xynaPropertySupport = new XynaPropertySupport(creator, getClassName());
  }


  public ForEachScopeStep(AVariable[] localInputVars, AVariable[] localOutputVars, GenerationBase creator) {
    super(localInputVars, localOutputVars, creator);
    this.xynaPropertySupport = new XynaPropertySupport(creator, getClassName());
  }
  
  
  private XynaPropertySupport discoverOwnXynaProperties() {
    
    List<Step> allLocalSteps = getAllLocalSubSteps(true);
    
    xynaPropertySupport.checkForXynaProperties(this.getServiceVariables());

    for (Step s : allLocalSteps) {
      if (s instanceof ForEachScopeStep) {
        continue;
      }
      xynaPropertySupport.checkForXynaProperties(s.getServiceVariables());
    }
    return xynaPropertySupport;
  }

  protected List<XynaPropertySupport> discoverXynaPropertySupportRecursively() {
    List<XynaPropertySupport> back = new ArrayList<>();
    List<Step> allLocalSteps = getAllLocalSubSteps(true);
    
    back.add(discoverOwnXynaProperties());
    
    for(Step s: allLocalSteps) {
      if(s instanceof ForEachScopeStep) {
        back.addAll(((ForEachScopeStep) s).discoverXynaPropertySupportRecursively());
      }
    }
    return back;
  }
  
  @Override
  protected void generateJavaAdditionalInitializeMemberVars(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    xynaPropertySupport.generateXynaPropertyAssignment(cb);
  }
  
  @Override
  protected void generateJavaAdditionalReadWriteObject(CodeBuffer cb, Set<String> importedClassesFqStrings, boolean read ) {
    xynaPropertySupport.generateJavaReadWriteObject(cb, read);
  }

  @Override
  protected void generateJavaClassHeader(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    cb.addLine("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(), "<",
               getParentScope().getClassName(), "> implements ", ForEachScope.class.getSimpleName(), " {");
    cb.addLB();
    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calculateSerialVersionUID()), "L");
    cb.addLB();
  }

  protected long calculateSerialVersionUID() {
    long ret = super.calculateSerialVersionUID();
    List<Pair<String, String>> types = new ArrayList<Pair<String,String>>();
    types.add(Pair.of(VARNAME_forEachIndex, Integer.class.getName()));
    ret = 31 * ret + GenerationBase.calcSerialVersionUID(types);
    return ret;
  }
  
  @Override
  protected void generateJavaConstructor(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    cb.addLine("public ", getClassName(), "(", Integer.class.getName(), " forEachIndex) {");
    cb.addLine("super(" + getIdx(), ")");
    cb.addLine("this.", VARNAME_forEachIndex, " = forEachIndex");
    cb.addLine("}").addLB();
  }


  @Override
  protected void generateMemberVariableDeclarations(CodeBuffer cb, HashSet<String> importedClassesFqStrings, Set<ExceptionVariable> transientExceptionVariableNames) {
    super.generateMemberVariableDeclarations(cb, importedClassesFqStrings, transientExceptionVariableNames);
    cb.addLine("private ", Integer.class.getName(), " ", VARNAME_forEachIndex);
  }


  @Override
  protected void generateJavaStepMethods(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException {
    super.generateJavaStepMethods(cb, importedClassesFqStrings);
    cb.addLine("public ", Integer.class.getName(), " ", METHODNAME_FOREACH_SCOPE_GET_FOREACH_INDEX, "() {");
    cb.addLine("return ", VARNAME_forEachIndex);
    cb.addLine("}").addLB();
  }
  
  
  @Override
  public VariableIdentification identifyVariable(String id) throws XPRC_InvalidVariableIdException {
    //search through StepForeach.outputVarsSingle
    StepForeach parent = (StepForeach)getParentStep();
    AVariable[] avar = parent.getOutputVarsSingle();
    
    for(int i=0; i<avar.length; i++) {
      if(avar[i] != null && avar[i].getId() == id) {
        VariableIdentification vi = new VariableIdentification();
        vi.variable = avar[i];
        vi.scope = this;
        return vi;
      }
    }
    
    return super.identifyVariable(id);
  }
  

}
