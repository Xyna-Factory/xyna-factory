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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.LocalExpressionVariable;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.InvalidInvocationException;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.StepBasedType;


public class StepBasedIdentification implements VariableContextIdentification {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(StepBasedIdentification.class);
  
  final Step step;
  
  
  /**
   * id und pfad -pärchen von variablen
   * idsAndPaths[0][<varNum>] = id
   * idsAndPaths[1][<varNum>] = path
   * 
   * id entspricht einer eindeutigen id
   * pfad ist ein ausdruck der art <memberVarName>.<memberVarName>....etc
   */
  private final String[][] idsAndPaths;
  private VariableIdentification vi;
  
  public StepBasedIdentification(Step step) {
    this.step = step;
    idsAndPaths = getIdsAndPaths();
  }

  public String getPathTo(VariableIdentification var) {
    return var.getScopeGetter(step.getParentScope()) ;
  }

  private String[][] getIdsAndPaths() {
    List<String> ids = new ArrayList<String>();
    List<String> paths = new ArrayList<String>();
    for (String s : step.getInputVarIds()) {
      ids.add(s);
    }
    for (String s : step.getInputVarPaths()) {
      paths.add(s);
    }
    if (step instanceof StepMapping) {
      for (String s : ((StepMapping) step).getLocalVarIds()) {
        ids.add(s);
      }
      for (String s : ((StepMapping) step).getLocalVarPaths()) {
        paths.add(s);
      }
    }
    if (!step.isExecutionDetached()) {
      for (String s : step.getOutputVarIds()) {
        ids.add(s);
      }
      for (String s : step.getOutputVarPaths()) {
        paths.add(s);
      }
    }
    return new String[][] {ids.toArray(new String[ids.size()]), paths.toArray(new String[paths.size()])};
  }


  public VariableIdentification identifyVariable(String varId) throws XPRC_InvalidVariableIdException {
    return step.getParentScope().identifyVariable(varId);
  }

  public VariableIdentification tryIdentificationByMappingVarNum(int varNum) throws XPRC_InvalidVariableIdException {
    if (step instanceof StepMapping) {
      StepMapping mapping = (StepMapping) step;
      AVariable aVar = mapping.getVariable(varNum);
      if (aVar != null) {
        VariableIdentification vi = new VariableIdentification();
        vi.variable = aVar;
        return vi;
      } else {
        throw new XPRC_InvalidVariableIdException(String.valueOf(varNum));
      }
    } else {
      throw new XPRC_InvalidVariableIdException(String.valueOf(varNum));
    }
  }
  

  public static class VariableInfoPathMap implements VariableInfo {

    private final VariableIdentification vi;
    private final StepBasedIdentification sbi;
    private final String fqDataModelName;
    private final Variable v;


    public VariableInfoPathMap(VariableIdentification vi, StepBasedIdentification stepBasedIdentification, String fqDataModelName,
                               Variable v) {
      this.vi = vi;
      this.sbi = stepBasedIdentification;
      this.fqDataModelName = fqDataModelName;
      this.v = v;
    }


    public VariableInfo follow(List<VariableAccessPart> parts, int depth) {
      return this;
    }


    public TypeInfo getTypeInfo(boolean ignoreList) {
      return new TypeInfo(PrimitiveType.STRING);
    }


    public String getJavaCodeForVariableAccess() {
      return sbi.getPathTo(vi) + vi.getVariable().getVarName() + ".";
    }


    public String getVarName() {
      throw new RuntimeException("unsupported");
    }


    public String getPath(long uniqueId) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (VariableAccessPart p : v.getParts()) {
        if (first) {
          first = false;
        } else {
          sb.append(".");
        }
        sb.append(p.getName());
        if (p.getIndexDef() != null) {
          sb.append("[\\\"");
          if (p.getIndexDef() instanceof LiteralExpression) {
            sb.append(((LiteralExpression) p.getIndexDef()).getValueEscapedForJava());
          } else if (p.getIndexDef() instanceof LocalExpressionVariable) {
            sb.append("\"+");
            sb.append(((LocalExpressionVariable) p.getIndexDef()).getUniqueVariableName(uniqueId));
            sb.append("+\"");
          } else {
            throw new RuntimeException("unsupported index def type: " + p.getIndexDef());
          }
          sb.append("\\\"]");
        }
      }
      return sb.toString();
    }


    public String getDataModel() {
      return fqDataModelName;
    }


    public String getVarNameOfValue() {
      DOM dom = (DOM) vi.getVariable().getDomOrExceptionObject();
      Triple<AVariable, AVariable, String> triple = GenerationBase.traversePathMapHierarchyToFindValue(dom, dom, "");
      
      if (triple == null) {
        return null;
      }
      
      return triple.getFirst().getVarName();
    }

    public String getLocalPathOfValue() {
      DOM dom = (DOM) vi.getVariable().getDomOrExceptionObject();
      Triple<AVariable, AVariable, String> triple = GenerationBase.traversePathMapHierarchyToFindValue(dom, dom, "");
      
      if (triple == null) {
        return null;
      }
      
      int len = dom.getMemberVars().get(0).getVarName().length() + 1;
      
      return triple.getThird().substring(len);
    }


    public String getPathSetter() {
      if (vi.getVariable().getDomOrExceptionObject().getMemberVars().get(0).isList()) {
        return "addPath";
      } else {
        return "setPath";
      }
    }


    public void castTo(TypeInfo type) {
      throw new RuntimeException("unsupported");
    }
  }



  public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
    String varId = idsAndPaths[0][v.getVarNum()];
    vi = identifyVariable(varId);
    if (step instanceof StepMapping) {
      StepMapping sm = (StepMapping) step;
      if (sm.getDataModel(varId) != null && vi.getVariable().getDomOrExceptionObject() instanceof DOM) {
        //manche der in-/output variablen können pathmaps sein.        
        DOM dom = (DOM) vi.getVariable().getDomOrExceptionObject();
        if (dom.getPathMapInformation() != null) {
          //variable ist pathmap
          return new VariableInfoPathMap(vi, this, sm.getDataModel(varId), v);
        }
      }
    }

    StepBasedVariable vid = new StepBasedVariable(idsAndPaths[1][v.getVarNum()], vi, this);
    if(followAccessParts) {
      vid.follow(v.getParts(), v.getParts().size() -1);
    }
    return vid;
  }

  public TypeInfo getTypeInfo(String originalXmlName) {
    GenerationBase gb = step.creator.getFromCache(originalXmlName);
    if (gb == null) {
      return null;
    } else {
      return new TypeInfo(new StepBasedVariable.StepBasedType((DomOrExceptionGenerationBase)gb, this), false);
    }
  }

  public Long getRevision() {
    return step.creator.revision;
  }

  public VariableInfo createVariableInfo(final TypeInfo type) {
    return new VariableInfo() {

      private AVariable follow(DomOrExceptionGenerationBase dom, String memberVarName) throws XPRC_InvalidVariableMemberNameException {
        if (dom == null) {
          throw new XPRC_InvalidVariableMemberNameException("<not a complex type>", memberVarName);
        }
        List<AVariable> memberVars = dom.getAllMemberVarsIncludingInherited();
        for (AVariable memberVar : memberVars) {
          if (memberVar.getVarName().equals(memberVarName)) {
            return memberVar;
          }
        }
        throw new XPRC_InvalidVariableMemberNameException(dom.getFqClassName(), memberVarName);
      }
      
      //FIXME diese methode ist von StepBasedVariable kopiert
      public AVariable checkInstanceInvocation(DOM datatypeWithOperations, VariableInstanceFunctionIncovation instanceMethod) {
        String fqName = datatypeWithOperations.getOriginalFqName();
        if (!datatypeWithOperations.exists()) {
          throw new RuntimeException(fqName + " not found.");
        }
        while (datatypeWithOperations != null) {
          for (List<Operation> serviceEntries : datatypeWithOperations.getServiceNameToOperationMap().values()) {
            for (Operation operation : serviceEntries) {
              if (operation.getName().equals(instanceMethod.getName()) && !operation.isStatic()) {
                List<AVariable> resultValues = operation.getOutputVars();

                List<TypeInfo> typesOfInputVars = new ArrayList<TypeInfo>();
                for (AVariable aVar : operation.getInputVars()) {
                  typesOfInputVars.add(getTypeInfo(aVar, false));
                }
                instanceMethod.setInputParameterTypes(typesOfInputVars);
                if (resultValues.size() > 1) {
                  throw new RuntimeException("Instance methods with more than 1 result are not supported: " + instanceMethod.getName());
                } else if (resultValues.size() == 0) {
                  // what to do, return null? (would throw NPW on getVariableType)
                } else {
                  resultValues.get(0).markAsFunctionResult();
                  return resultValues.get(0);
                }
              }
            }
          }
          datatypeWithOperations = datatypeWithOperations.getSuperClassGenerationObject(); //im supertype schauen
        }
        throw new InvalidInvocationException(instanceMethod.getName(), fqName);
      }
      
      //FIXME diese methode ist von StepBasedVariable kopiert
      private TypeInfo getTypeInfo(AVariable var, boolean ignoreList) {
        if (!ignoreList && var.isList()) {
          return new TypeInfo(BaseType.LIST);
        }
        if (var.getDomOrExceptionObject() != null) {
          return new TypeInfo(new StepBasedType(var.getDomOrExceptionObject(), StepBasedIdentification.this), false);
        } else if (var.getJavaTypeEnum() != null) {
          return new TypeInfo(BaseType.valueOfJavaName(var.getJavaType()));
        } else {
          logger.debug("Unknown type of variable name=" + var.getVarName() + ", id=" + var.getId() + ", children=" + var.getChildren().size());
          return new TypeInfo(BaseType.STRING);
        }
      }
      
      public VariableInfo follow(List<VariableAccessPart> parts, int depth) throws XPRC_InvalidVariableMemberNameException {
        if (type == null ||
            !(type.getModelledType() instanceof StepBasedType)) {
          return null;
        }
        StepBasedType sbt = (StepBasedType) type.getModelledType();
        DomOrExceptionGenerationBase dom = sbt.getGenerationType();
        AVariable av = null;
        for (int i = 0; i <= depth; i++) {
          VariableAccessPart part = parts.get(i);
          if (part.isMemberVariableAccess()) {            
            av = follow(av == null ? dom : av.getDomOrExceptionObject(), part.getName());
            dom = av.getDomOrExceptionObject();
          } else {
            if (!(dom instanceof DOM)) {
              throw new RuntimeException("Invocation of instance methods only supported on data types.");
            }
            av = checkInstanceInvocation((DOM) dom, (VariableInstanceFunctionIncovation) part);
            dom = av.getDomOrExceptionObject();
          }
        }
        return null;
      }

      public TypeInfo getTypeInfo(boolean ignoreList) {
        return type;
      }

      public String getJavaCodeForVariableAccess() {
        throw new RuntimeException("unsupported");
      }

      public String getVarName() {
        throw new RuntimeException("unsupported");
      }

      public void castTo(TypeInfo type) {
      }
      
    };
  }
  
  
}
