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
package com.gip.xyna.xprc.xfractwfe.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo.ModelledType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.StepBasedType;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.OperationInfo;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;


public class FunctionExpression extends Expression {

  private int idx;
  private final List<Expression> subExpressions;
  protected List<VariableAccessPart> parts;
  private Expression indexDef; //inhalt von []
  private final Function f;

  static class InvalidNumberOfFunctionParametersException extends Exception {

    private static final long serialVersionUID = 1L;
    
  }


  public FunctionExpression(int firstIdx, int lastIdx, Function f, List<Expression> subExpressions, Expression indexDef) throws InvalidNumberOfFunctionParametersException {
    this(firstIdx, lastIdx, f, subExpressions, new ArrayList<VariableAccessPart>(), indexDef);
  }
  
  
  public FunctionExpression(int firstIdx, int lastIdx, Function f, List<Expression> subExpressions, List<VariableAccessPart> accessParts, Expression indexDef) throws InvalidNumberOfFunctionParametersException {
    super(firstIdx);
    this.idx = lastIdx;
    this.f = f;
    this.subExpressions = new ArrayList<Expression>();
    if (subExpressions != null) {
      this.subExpressions.addAll(subExpressions);
    }
    this.parts = accessParts;
    this.indexDef = indexDef;
    FunctionParameterTypeDefinition parameterTypeDef = f.getParameterTypeDef();
    if (parameterTypeDef.numberOfParas() > -1) {
      if (subExpressions == null && parameterTypeDef.numberOfParas() == 0) {
        
      } else {
        if (subExpressions == null || 
            subExpressions.size() < parameterTypeDef.numberOfParas() ||
            subExpressions.size() > parameterTypeDef.numberOfParas() + parameterTypeDef.numberOfOptionalParas()) {
          throw new InvalidNumberOfFunctionParametersException();
        }
      }
    } 
  }


  public List<Expression> getSubExpressions() {
    return subExpressions;
  }


  public String getJavaCode() {
    return f.getJavaCode();
  }


  public TypeInfo getParameterTypeDef(int parameterCnt) {
    return f.getParameterTypeDef().getType(parameterCnt);
  }
  
  
  public Function getFunction() {
    return f;
  }


  public List<VariableAccessPart> getParts() {
    return parts;
  }
  
  
  public void setAccessParts(List<VariableAccessPart> accessParts) {
    this.parts = accessParts;
  }


  @Override
  public int getLastIdx() {
    return idx;
  }


  @Override
  public void setLastIdx(int idx) {
    this.idx = idx;
  }
  
  
  public Expression getIndexDef() {
    return indexDef;
  }
  
  public void setIndexDef(Expression indexDef) {
    this.indexDef = indexDef;
  }
  
  public void validate() throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
  }
  
  public TypeInfo getResultType() {
    return f.getResultType();
  }
  
  public static abstract class DynamicResultTypExpression extends FunctionExpression {

    protected final VariableContextIdentification varCon;
    
    public DynamicResultTypExpression(FunctionExpression fe, VariableContextIdentification varCon)
                    throws InvalidNumberOfFunctionParametersException {
      this(fe.getFirstIdx(), fe.getLastIdx(), fe.getFunction(), fe.getSubExpressions(), fe.getParts(), fe.getIndexDef(), varCon);
    }
    
    public DynamicResultTypExpression(int firstIdx, int lastIdx, Function f, List<Expression> subExpressions,
                                      List<VariableAccessPart> accessParts, Expression indexDef, VariableContextIdentification varCon)
                    throws InvalidNumberOfFunctionParametersException {
      super(firstIdx, lastIdx, f, subExpressions, accessParts, indexDef);
      this.varCon = varCon;
    }
    
    @Override
    public TypeInfo getResultType() {
      return varCon.getTypeInfo(getDynamicTypeName());
    }
    
    public abstract String getDynamicTypeName();
    
    public abstract XMOMType getDynamicTypeType();
    
    public void validate() throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
      VariableInfo vi = varCon.createVariableInfo(getResultType());
      vi.follow(parts, parts.size()-1);
    }
  }
  
  
  private static XMOMType determineType(DynamicResultTypExpression dynamicTypeExpression) {
    TypeInfo ti = dynamicTypeExpression.getResultType();
    if (ti != null &&
        ti.isModelledType() &&
        ti.getModelledType() instanceof StepBasedType) {
      return XMOMType.getXMOMTypeByGenerationInstance(((StepBasedType)ti.getModelledType()).getGenerationType());
    }
    try {
      return XMOMType.getXMOMTypeByRootTag(GenerationBase.retrieveRootTag(dynamicTypeExpression.getDynamicTypeName(), dynamicTypeExpression.varCon.getRevision(), true, true));
    } catch (Ex_FileAccessException e) {
      return null;
    } catch (XPRC_XmlParsingException e) {
      return null;
    }
  }
  
  
  public static class NewExpression extends DynamicResultTypExpression {
    
    public NewExpression(FunctionExpression fe, VariableContextIdentification varCon)
                    throws InvalidNumberOfFunctionParametersException {
      super(fe, varCon);
    }

    @Override
    public String getDynamicTypeName() {
      return ((LiteralExpression)getSubExpressions().get(0)).getValue();
    }

    @Override
    public XMOMType getDynamicTypeType() {
      return determineType(this);
    }
    
  }
  
  
  public static class CastExpression extends DynamicResultTypExpression implements FollowableType {
    
    private final FollowableType wrappedType;
    private final LiteralExpression typeIdentifier;
    private Boolean overwriteIgnoreList;
    private boolean resolveSubTypesLocally;

    public CastExpression(FunctionExpression function, int subExpressionIndexOfInnerFollowable, int subExpressionIndexOfTypeId, VariableContextIdentification varCon)
                    throws InvalidNumberOfFunctionParametersException {
      this(function, function.getSubExpressions(), subExpressionIndexOfInnerFollowable, subExpressionIndexOfTypeId, varCon);
    }
    
    public CastExpression(FunctionExpression function, List<Expression> subExpressions, int subExpressionIndexOfInnerFollowable, int subExpressionIndexOfTypeId, VariableContextIdentification varCon)
                    throws InvalidNumberOfFunctionParametersException {
      super(function.getFirstIdx(), function.getLastIdx(), function.getFunction(), subExpressions, function.getParts(), function.getIndexDef(), varCon);
      Expression exp = subExpressions.get(subExpressionIndexOfInnerFollowable);
      if (!(exp instanceof FollowableType)) {
        throw new IllegalArgumentException(exp + " is not followable!");
      }
      wrappedType = (FollowableType) exp;
      exp = subExpressions.get(subExpressionIndexOfTypeId);
      if (!(exp instanceof LiteralExpression)) {
        throw new IllegalArgumentException(exp + " is not followable!");
      }
      typeIdentifier = (LiteralExpression) exp;
    }

    public CastExpression(FunctionExpression function, List<Expression> subExpressions, int subExpressionIndexOfInnerFollowable, int subExpressionIndexOfTypeId, VariableContextIdentification varCon, Boolean overwriteIgnoreList, boolean resolveSubTypesLocally)
        throws InvalidNumberOfFunctionParametersException {
      this(function, subExpressions, subExpressionIndexOfInnerFollowable, subExpressionIndexOfTypeId, varCon);
      this.overwriteIgnoreList = overwriteIgnoreList;
      this.resolveSubTypesLocally = resolveSubTypesLocally;
    }

    public boolean isPathMap() {
      return wrappedType.isPathMap();
    }

    public int getAccessPathLength() {
      return parts.size() + wrappedType.getAccessPathLength();
    }

    public int getVarNum() {
      return wrappedType.getVarNum();
    }

    public VariableAccessPart getAccessPart(int index) {
      if (index >= wrappedType.getAccessPathLength()) {
        return parts.get(index - wrappedType.getAccessPathLength());
      } else {
        return wrappedType.getAccessPart(index);
      }
    }
    
    public Expression getWrappedAccessPath() {
      return (Expression) wrappedType;
    }

    public TypeInfo getTypeOfExpression() throws XPRC_InvalidVariableMemberNameException {
      TypeInfo typeInfo = getFollowedVariable().getTypeInfo(true);
      return typeInfo;
    }

    public boolean lastPartOfVariableHasListAccess() {
      if (parts.size() <= 0) {
        if (getIndexDef() != null) {
          return true;
        } else {
          return wrappedType.lastPartOfVariableHasListAccess(); 
        }
      } else {
        return parts.get(parts.size() - 1).getIndexDef() != null;
      }
    }


    public String toJavaCodeSetter(int depth, boolean setterOfWholeList, String uniqueVarName, long uniqueId) {
      // what if we are not responsible for the setter
      String wrappedGetter = toJavaCodeGetter(depth - 1, true, uniqueId);
      if (depth == wrappedType.getAccessPathLength()) {
        wrappedGetter = wrapInFunctionInvocation(wrappedGetter);
      }
      StringBuilder setter = new StringBuilder(wrappedGetter);
      
      if (depth >= 0) {
        VariableAccessPart p = getAccessPart(depth);
        if (p.getIndexDef() != null && !setterOfWholeList) {
          //nach list casten, weil die definition immer ? extends <type> ist und damit unveränderlich.
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
          setter.append(", ").append(uniqueVarName).append(")");
        } else {
          setter.append(".")
                .append(GenerationBase.buildSetter(p.getName()))
                .append("(").append(uniqueVarName).append(")");
        }
      } else {
        /*
         * depth == 0 bedeutet, dass es um die rootvariable geht
         * es könnte theoretisch auch sein, dass der cast um die rootvariable die indexdef hat. das ist aber derzeit nicht unterstützt 
         * und wird bei den transformationen der mappings (fillVars) verhindert
         * also
         * cast(%0%[i]).a.b.c  vs  cast(%0%)[i].a.b.c
         */
        
        Expression indexDef = getRootVariable().getIndexDef();
        if (indexDef != null && !setterOfWholeList) {
          //nach list casten, weil die definition immer ? extends <type> ist und damit unveränderlich.
          setter.insert(0, "((List)");
          setter.append(").set(");
          if (indexDef instanceof LiteralExpression) {
            setter.append(((LiteralExpression) indexDef).getValue());
          } else if (indexDef instanceof LocalExpressionVariable) {
            setter.append(((LocalExpressionVariable) indexDef).getUniqueVariableName(uniqueId));
          } else {
            throw new RuntimeException("unsupported type " + indexDef.getClass().getName());
          }
          setter.append(", ").append(uniqueVarName).append(")");
        } else {
          setter.append(" = ").append(uniqueVarName);
        }
      }

      return setter.toString();
    }
    

    public String toJavaCodeGetter(int depth, boolean withListAccess, long uniqueId) {
      int wrappedTypeAPL = wrappedType.getAccessPathLength();
      if (depth >= wrappedTypeAPL - 1) {
        int localPart = depth - wrappedTypeAPL;
        String getter = wrappedType.toJavaCodeGetter(wrappedTypeAPL - 1, withListAccess || localPart > -1 , uniqueId);
        StringBuilder sb; 
        if (!withListAccess && 
            localPart == - 1 &&
            wrappedType.lastPartOfVariableHasListAccess()) {
          sb = new StringBuilder(getter);
        } else {
          sb = new StringBuilder(wrapInFunctionInvocation(getter));
        }
        Variable.buildGetterForOwnIndexAndAccessParts(localPart, withListAccess, getIndexDef(), parts, sb, uniqueId);
        return sb.toString();
      } else {
        //depth endet vollständig im wrappedType
        return wrappedType.toJavaCodeGetter(depth, withListAccess, uniqueId);
      }
    }
    
    
    private String wrapInFunctionInvocation(String toWrap) {
      String className = typeIdentifier.getValueEscapedForJava();
      if (GenerationBase.isReservedServerObjectByFqOriginalName(className)) {
        className = GenerationBase.getReservedClass(className).getName();
      } else {
        try {
          className = GenerationBase.transformNameForJava(className);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException("Casted type is invalid: " + className, e);
        }
      }
      StringBuilder sb = new StringBuilder();
      sb.append(getFunction().getJavaCode())
        .append("(")
        .append(className).append(".class")
        .append(", ")
        .append(toWrap)
        .append(")");
      return sb.toString();
    }
    

    public VariableInfo follow(int pathDepth) throws XPRC_InvalidVariableMemberNameException {
      /*
       * typumwandlung auf den gecasteten typ
       * beispiel:
       * follow depth=-1 cast(%1%.x.y, resulttype).a.b
       * -> wrappedtype hat noch accesspath, deshalb muss man nicht den resulttyp zurückgeben
       * follow depth=-1 cast(%1%, resulttype).a.b
       * -> wrappedtype wird zu resulttyp umgewandelt
       * 
       * für depth>-1 analog.
       */
      if (pathDepth < 0) {
        //-1 -> typ von rootvariable
        VariableInfo varInfo = wrappedType.follow(pathDepth);
        if (wrappedType instanceof CastExpression) {          
          return varInfo;
        } else if (wrappedType.getAccessPathLength() == 0) {
          varInfo.castTo(getResultType());
          return varInfo;
        } else {
          return varInfo;
        }
      }
      if (pathDepth >= wrappedType.getAccessPathLength()) {
        return followLocally(pathDepth - wrappedType.getAccessPathLength());
      } else {
        VariableInfo varInfo = wrappedType.follow(pathDepth);
        if (wrappedType.getAccessPathLength() - 1 == pathDepth) {
          //man will genau den typ des accesspart-index, der durch den cast gecastet wird
          varInfo.castTo(getResultType());
          return varInfo;
        } else {
          return varInfo;
        }
      }
    }

    public int getPartIndex(VariableAccessPart part) {
      for (int i = 0; i<getParts().size(); i++) {
        if (part == getParts().get(i)) {
          return i + wrappedType.getAccessPathLength();
        }
      }
      return wrappedType.getPartIndex(part);
    }
    
    public SingleVarExpression getRootVariable() {
      FollowableType t = wrappedType;
      while (t instanceof CastExpression) {
        t = ((CastExpression) wrappedType).wrappedType;
      }
      return (SingleVarExpression) t;
    }


    private VariableInfo followLocally(int pathDepth) throws XPRC_InvalidVariableMemberNameException {
      TypeInfo currentTypeInfo = getResultType();
      if (currentTypeInfo == null || 
          !currentTypeInfo.isModelledType()) {
        //FIXME andere exception
        throw new XPRC_InvalidVariableMemberNameException("UNKNOWN", "?");
      }
      pathDescent: for (int i = 0; i <= pathDepth; i++) {
        VariableAccessPart currentPart = parts.get(i);
        if (currentPart.isMemberVariableAccess()) {
          List<VariableInfo> members = currentTypeInfo.getModelledType().getAllMemberVarsIncludingInherited();
          for (VariableInfo memberInfo : members) {
            if (memberInfo == null || 
                memberInfo.getVarName() == null) {
              continue;
            }
            if (memberInfo.getVarName().equals(currentPart.getName())) {
              if (i == pathDepth) {
                return memberInfo;
              } else {
                if (overwriteIgnoreList != null) {
                  currentTypeInfo = memberInfo.getTypeInfo(overwriteIgnoreList);
                } else {
                  currentTypeInfo = memberInfo.getTypeInfo(currentPart.getIndexDef() != null);
                }
                continue pathDescent;
              }
            }
          }
          throw new XPRC_InvalidVariableMemberNameException(currentTypeInfo.getJavaName(), currentPart.getName());
        } else {
          List<OperationInfo> operations = currentTypeInfo.getModelledType().getAllInstanceOperationsIncludingInherited();
          for (OperationInfo operation : operations) {
            if (operation.getOperationName().equals(currentPart.getName())) {
              // TODO verify result size?
              VariableInfo result = operation.getResultTypes().get(0);
              if (i == pathDepth) {
                return result;
              } else {
                currentTypeInfo = result.getTypeInfo(currentPart.getIndexDef() != null);
                continue pathDescent;
              }
            }
          }
          //FIXME andere exception
          throw new XPRC_InvalidVariableMemberNameException(currentTypeInfo.getJavaName(), currentPart.getName());
        }
      }
      throw new XPRC_InvalidVariableMemberNameException(currentTypeInfo.getJavaName(), "?");
    }


    public VariableInfo getFollowedVariable() throws XPRC_InvalidVariableMemberNameException {
      return follow(getAccessPathLength() - 1);
    }
    
    
    public XMOMType getDynamicTypeType() {
      return determineType(this);
    }
    
    public TypeInfo getOriginalType() throws XPRC_InvalidVariableMemberNameException {
      return getFollowedVariable().getTypeInfo(lastPartOfVariableHasListAccess());
    }

    public String getDynamicTypeName() {
      return typeIdentifier.getValue();
    }

    
    @Override
    public TypeInfo getResultType() {
      if (resolveSubTypesLocally) {
        try {
          VariableInfo varToCast = wrappedType.getFollowedVariable();
          TypeInfo typeToCast = varToCast.getTypeInfo(true);
          if (typeToCast.isModelledType()) {
            ModelledType modelledTypeToCast = typeToCast.getModelledType();
            Set<ModelledType> types = modelledTypeToCast.getSubTypesRecursivly();
            if (types.size() > 0) {
              for (ModelledType type : types) {
                if (type.getFqXMLName().equals(getDynamicTypeName())) {
                  return new TypeInfo(type, false);
                }
              }
            } else {
              // FIXME assume it's a merged clone for now
              return new TypeInfo(modelledTypeToCast, false);
            }
          }
        } catch (XPRC_InvalidVariableMemberNameException e) {
          // TODO just log?
          throw new RuntimeException(e);
        }
      }
      return super.getResultType();
    }
  }


  @Override
  public void visit(Visitor visitor) {
    if (f.hasCustomVisitationPattern()) {
      f.getCustomVisitationPattern().visit(this, visitor);
    } else {
      DEFAULT_VISITATION_PATTERN.visit(this, visitor);
    }
    
  }
  
  
  static DefaultFunctionVisitationPattern DEFAULT_VISITATION_PATTERN = new DefaultFunctionVisitationPattern();
  
  static class DefaultFunctionVisitationPattern implements FunctionVisitationPattern {

    public void visit(FunctionExpression function, Visitor visitor) {
      visitor.functionStarts(function);
      for (int i = 0; i < function.getSubExpressions().size(); i++) {
        Expression subExpr = function.getSubExpressions().get(i);
        visitor.functionSubExpressionStarts(function, i);
        subExpr.visit(visitor);
        visitor.functionSubExpressionEnds(function, i);
      }
      visitor.functionAllSubExpressionsEnds(function);
      if (function.getIndexDef() != null) {
        visitor.indexDefStarts(function.getIndexDef());
        function.getIndexDef().visit(visitor);
        visitor.indexDefEnds(function.getIndexDef());
      }
      visitor.functionEnds(function);
      for (int i = 0; i < function.getParts().size(); i++) {
        VariableAccessPart part = function.getParts().get(i);
        part.visit(visitor, i == function.getParts().size() - 1);
      }
      for (int i = function.getParts().size() - 1; i >= 0; i--) {
        visitor.variablePartSubContextEnds(function.getParts().get(i));
      }
      visitor.allPartsOfFunctionFinished(function);
    }
    
  }


}
