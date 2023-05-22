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
package com.gip.xyna.xprc.xfractwfe.formula;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedIdentification.VariableInfoPathMap;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.InvalidInvocationException;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;



public class Variable {
  

  private int lastIdx;
  private final int firstIdx;
  private final int varNum; // entspricht %<varNum>%
  private final List<VariableAccessPart> parts;
  private Expression indexDef;
  private VariableContextIdentification variableContext;
  private VariableInfo varInfo;

  public Variable(VariableContextIdentification variableContext, String varNum, List<VariableAccessPart> parts, int firstIdx, int lastIdx, Expression indexDefExpr) {
    this.firstIdx = firstIdx;
    this.lastIdx = lastIdx;
    this.parts = parts == null ? new ArrayList<VariableAccessPart>() : parts;
    this.varNum = Integer.valueOf(varNum);
    this.indexDef = indexDefExpr;
    this.variableContext = variableContext;
  }


  public int getLastIdx() {
    return lastIdx;
  }
 

  public boolean isList() throws XPRC_InvalidVariableMemberNameException {
    TypeInfo ti = getVariableType(false);
    return ti.isList();
  }


  public void validate() throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
    varInfo = variableContext.createVariableInfo(this, true);
  }
  
  public VariableInfo getRootInfo() throws XPRC_InvalidVariableIdException{
    try {
      return variableContext.createVariableInfo(this, false);
    } catch (XPRC_InvalidVariableMemberNameException e) {
      throw new RuntimeException(e);
    }

  }


  public VariableInfo getBaseVariable() {
    return varInfo;
  }

  /**
   * zum i-ten variable-accesspart folgen (i = 0: dem ersten folgen...)
   */
  public VariableInfo follow(int i) throws XPRC_InvalidVariableMemberNameException {
    return varInfo.follow(parts, i);
  }

  public VariableInfo getFollowedVariable() throws XPRC_InvalidVariableMemberNameException {
    if (varInfo == null) {
      try {
        validate();
      } catch (XPRC_InvalidVariableIdException e) {
        throw new RuntimeException(e);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }
    return varInfo.follow(parts, parts.size() - 1);
  }


  public TypeInfo getVariableType(boolean ignoreList) throws XPRC_InvalidVariableMemberNameException {
    VariableInfo var = getFollowedVariable();
    return var.getTypeInfo(ignoreList);
  }
  
  public boolean isPrototype() throws XPRC_InvalidVariableIdException {
    if (variableContext instanceof StepBasedIdentification) {
      VariableIdentification varId = ((StepBasedIdentification)variableContext).tryIdentificationByMappingVarNum(varNum);
      if (varId != null &&
          varId.getVariable() != null) {
        return varId.getVariable().isPrototype();
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * ist letzter part mit listen-access (nicht: ist letzter part listenwertig vom typ her)
   * 
   * bsp: a.b.c[3] -&gt; true
   *      a[2] -&gt; true
   *      a.b[4].c -&gt; false
   *      a[2].b -&gt; false
   */
  public boolean lastPartOfVariableHasListAccess() {
    return (parts.size() > 0 && parts.get(parts.size() - 1).getIndexDef() != null)
        || (parts.size() == 0 && indexDef != null);
  }


  public TypeInfo getTypeOfExpression() throws XPRC_InvalidVariableMemberNameException {
    /*
     * a.b, type = List Of B -> TypeInfo=LIST
     * a.b, type = B -> TypeInfo=B
     * a.b, type = String -> TypeInfo=String
     * a.b, type = List of String -> TypeInfo=LIST
     * a.b[0], type = List of B -> TypeInfo=B
     * a.b[0], type = List of String -> TypeInfo=String
     */
    return getVariableType(lastPartOfVariableHasListAccess());
  }


  /**
   * erzeugt getter f�r die variable bis zu der pfad-tiefe der durch depth angegeben ist
   */
  public String toJavaCodeGetter(int depth, boolean withListAccess, long uniqueId) {
    VariableInfo vi = getBaseVariable();
    
    StringBuilder getter = new StringBuilder( vi.getJavaCodeForVariableAccess() );
    
    buildGetterForOwnIndexAndAccessParts(depth, withListAccess, indexDef, parts, getter, uniqueId);

    return getter.toString();
  }
  

  static void buildGetterForOwnIndexAndAccessParts(int depth, boolean withListAccess, Expression indexDef, List<VariableAccessPart> parts, StringBuilder getter, long uniqueId) {
    if (indexDef != null && depth >= -1) { //Basiselement ist Liste, daher "get(index)" erg�nzen
      if( depth == -1 && !withListAccess) {
        //Listenzugriff auf das Basiselement, daher kein get(index)
      } else {        
        getter.append(".get(").append(getIndex(indexDef, uniqueId)).append(")");
      }
    }
    for (int i = 0; i <= depth; i++) {
      VariableAccessPart p = parts.get(i);
      if (p.getName() == null || p.getName().trim().length() == 0) {
        continue;
      }
      boolean lastElement = i + 1 > depth;
      getter.append(".").append(GenerationBase.buildGetter(p.getName())).append("()");
      if (withListAccess || !lastElement) {
        if (p.getIndexDef() != null) {
          getter.append(".get(").append(getIndex(p.getIndexDef(), uniqueId)).append(")");
        }
      }
    }
  }


  private static String getIndex(Expression indexDef, long uniqueId) {
    if (indexDef instanceof LiteralExpression) {
      return ((LiteralExpression) indexDef).getValue();
    } else if (indexDef instanceof LocalExpressionVariable) {
      return ((LocalExpressionVariable) indexDef).getUniqueVariableName(uniqueId);
    } else {
      throw new RuntimeException("unsupported index definition: " + indexDef);
    }
  }


  /**
   * erzeugt setter f�r die variable bis zu der pfad-tiefe der durch depth angegeben ist
   * @param depth maximaler index von parts, der noch ber�cksichtigt werden soll
   * @param setterOfWholeList spielt nur eine rolle, falls variable listenwertig
   *   true -&gt; setList(value)
   *   false -&gt; getList().set(indexDef, value)
   * @param value
   */
  public String toJavaCodeSetter(int depth, boolean setterOfWholeList, String value, long uniqueId) {

    StringBuilder setter = new StringBuilder();
    setter.append(toJavaCodeGetter(depth - 1, true, uniqueId));

    if (depth >= 0) {
      VariableAccessPart p = parts.get(depth);
      if (p.getIndexDef() != null && !setterOfWholeList) {
        //nach list casten, weil die definition immer ? extends <type> ist und damit unver�nderlich.
        setter.insert(0, "((List)");
        setter.append(".").append(GenerationBase.buildGetter(p.getName())).append("()");
        setter.append(")");
        setter.append(".set(");
        if (p.getIndexDef() instanceof LiteralExpression) {
          setter.append(((LiteralExpression) p.getIndexDef()).getValue());
        } else if (p.getIndexDef() instanceof LocalExpressionVariable) {
          setter.append(((LocalExpressionVariable) p.getIndexDef()).getUniqueVariableName(uniqueId));
        } else {
          throw new RuntimeException();
        }
        setter.append(", ").append(value).append(")");
      } else {
        setter.append(".")
              .append(GenerationBase.buildSetter(p.getName()))
              .append("(").append(value).append(")");
      }
    } else {
      if (indexDef != null && !setterOfWholeList) {
        //nach list casten, weil die definition immer ? extends <type> ist und damit unver�nderlich.
        setter.insert(0, "((List)");
        setter.append(").set(");
        if (indexDef instanceof LiteralExpression) {
          setter.append(((LiteralExpression) indexDef).getValue());
        } else if (indexDef instanceof LocalExpressionVariable) {
          setter.append(((LocalExpressionVariable) indexDef).getUniqueVariableName(uniqueId));
        } else {
          throw new RuntimeException();
        }
        setter.append(", ").append(value).append(")");
      } else {
        setter.append(" = ").append(value);
      }
    }

    return setter.toString();
  }



  private TypeInfo targetType = TypeInfo.UNKOWN;


  public void setTargetType(TypeInfo type) throws XPRC_InvalidVariableMemberNameException {
    TypeInfo ti = getTypeOfExpression();
    if (type.isModelledType() && ti.isModelledType()) {
      //targettype auf originaltype �ndern, falls kompatibel
      boolean isSuperClass = type.getModelledType().isSuperClassOf(ti.getModelledType());
      if (isSuperClass) {
        this.targetType = ti;
        return;
      }
    }
    this.targetType = type;
  }


  public TypeInfo getTargetType() {
    return targetType;
  }


  public Expression getIndexDef() {
    return indexDef;
  }
  
  
  public void setIndexDef(Expression indexDef) {
    this.indexDef = indexDef;
  }


  public List<VariableAccessPart> getParts() {
    return parts;
  }


  public int getVarNum() {
    return varNum;
  }


  public int getFirstIdx() {
    return firstIdx;
  }


  public boolean isPathMap() {
    if (varInfo == null) {
      try {
        validate();
      } catch (XPRC_InvalidVariableIdException e) {
        return false;
      } catch (XPRC_InvalidVariableMemberNameException e) {
        return false;
      } catch (InvalidInvocationException e) {
        return false;
      }
    }
    return varInfo instanceof VariableInfoPathMap;
  }


  public void visit(Visitor visitor) {
    visitor.variableStarts(this);
    if (this.getIndexDef() != null) {
      visitor.indexDefStarts(this.getIndexDef());
      getIndexDef().visit(visitor);
      visitor.indexDefEnds(this.getIndexDef());
    }
    visitor.variableEnds(this);
    for (int i = 0; i < parts.size(); i++) {
      VariableAccessPart part = parts.get(i);
      part.visit(visitor, i == parts.size() - 1);
    }
    for (int i = this.getParts().size() - 1; i >= 0; i--) {
      visitor.variablePartSubContextEnds(this.getParts().get(i));
    }
    visitor.allPartsOfVariableFinished(this);
  }
  

}
