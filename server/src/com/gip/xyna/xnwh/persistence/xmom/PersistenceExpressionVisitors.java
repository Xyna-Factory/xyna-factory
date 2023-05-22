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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.AliasDictionary;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceOperationAlgorithms.TypeLinkCollectionVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.AcceptAllMergeFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.AndOperator;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.EqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FollowableType;
import com.gip.xyna.xprc.xfractwfe.formula.Function;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.CastExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionParameterTypeDefinition;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.NotEqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.OrOperator;
import com.gip.xyna.xprc.xfractwfe.formula.SupportedFunctionStore;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo.ModelledType;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.EmptyVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.InferOriginalTypeVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.InitVariablesVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.MakeTypeDefinitionConsistentVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.OperationInfo;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;


public class PersistenceExpressionVisitors {

  private final static Logger logger = Logger.getLogger(PersistenceExpressionVisitors.class);
  
  protected static abstract class StorableColumnIdentifingVisitor extends EmptyVisitor {

    protected Stack<StorableColumnInformation> variableIdentification = new Stack<StorableColumnInformation>();
    protected final XMOMStorableStructureInformation rootInfo; // this should be part of variableIdentification
    
    protected StorableColumnIdentifingVisitor(XMOMStorableStructureInformation info) {
      this.rootInfo = info;
    }
    
    public void variableStarts(Variable variable) {
      variableIdentification.clear();
    }

    public abstract void variableEnds(Variable variable);

    public void variablePartStarts(VariableAccessPart part) {
      StorableStructureInformation currentRoot;
      if (variableIdentification.isEmpty()) {
        currentRoot = rootInfo;
      } else {
        currentRoot = variableIdentification.peek().getStorableVariableInformation();
        if (currentRoot.isReferencedList()) {
          currentRoot = currentRoot.getReferencedStorable();
        }
      }
      StorableColumnInformation column = currentRoot.getColumnInfoByName(part.getName());
      if (column == null) {
        throw new RuntimeException("Membervariable " + part.getName() + " does not exist in " + currentRoot.getFqXmlName());
      }
      variableIdentification.push(column);
    }

  }
  
  
  protected static class QueryBuildingVisitor extends StorableColumnIdentifingVisitor {
    
    private final List<CastCondition> casts;
    private final List<CastCondition> allCasts;
    private Stack<FunctionExpression> functionStack = new Stack<FunctionExpression>();
    private UnfinishedWhereClause whereClause = new UnfinishedWhereClause();
    private boolean expectingIndex = false;
    private static final Pattern unescapedStarPattern = Pattern.compile("\\\\*[*]");
    private static final Pattern unescapedQuotesPattern = Pattern.compile("\\\\*[\"]");
    private static final Pattern unescapedWildcardPattern = Pattern.compile("\\\\*[%]");
    
    private final Parameter parameter;
    
    QueryBuildingVisitor(XMOMStorableStructureInformation info, Parameter parameter) {
      super(info);
      whereClause.append(" WHERE ");
      this.parameter = parameter;
      casts = new ArrayList<>();
      allCasts = new ArrayList<>();
    }

    
    public void expression2ArgsStarts(Expression2Args expression) {
      whereClause.append("(");
    }
    
    public void functionEnds(FunctionExpression fe) {
      functionStack.pop();
      if (isCastOrTypeof(fe)) {
        Expression typeName;
        Expression accessPart;
        if (isCast(fe)) {
          typeName = fe.getSubExpressions().get(0);
          accessPart = fe.getSubExpressions().get(1);
        } else {
          typeName = fe.getSubExpressions().get(1);
          accessPart = fe.getSubExpressions().get(0);
        }
        if (typeName instanceof LiteralExpression &&
            accessPart instanceof FollowableType) {
          CastCondition cc = new CastCondition();
          LiteralExpression le = (LiteralExpression)typeName;
          FollowableType sva = (FollowableType)accessPart;
          List<String> access = retrieveAccessPathRecursivly(sva);
          cc.accessPath = access;
          cc.typename = le.getValue();
          cc.isCast = isCast(fe);
          casts.add(cc);
        }
      }
    }
    
    
    static List<String> retrieveAccessPathRecursivly(FollowableType ft) {
      List<String> access = new ArrayList<>();
      if (ft instanceof CastExpression) { // nested casts
        FollowableType subFt = (FollowableType) ((CastExpression) ft).getSubExpressions().get(1);
        access.addAll(retrieveAccessPathRecursivly(subFt));
      }
      for (int i = 0; i < ft.getAccessPathLength(); i++) {
        access.add(ft.getAccessPart(i).getName());
      }
      return access;
    }
    


    static boolean isCast(FunctionExpression fe) {
      return fe.getFunction().getName().equals(Functions.CAST_FUNCTION_NAME);
    }
    
    private boolean isInstanceOf(FunctionExpression fe) {
      return fe.getFunction().getName().equals(Functions.TYPE_OF_FUNCTION_NAME);
    }
    
    static boolean isCastOrTypeof(FunctionExpression fe) {
      return fe.getFunction().getName().equals(Functions.CAST_FUNCTION_NAME) ||
             fe.getFunction().getName().equals(Functions.TYPE_OF_FUNCTION_NAME);
    }

    private boolean lastFunctionInStackIsGlob() {
      return !functionStack.isEmpty() && functionStack.peek().getFunction().getName().equals(QueryFunctionStore.GLOB_FUNCTION_NAME);
    }
    
    private boolean lastFunctionInStackIsTypeof() {
      return !functionStack.isEmpty() && functionStack.peek().getFunction().getName().equals(Functions.TYPE_OF_FUNCTION_NAME);
    }
    
    private boolean lastFunctionInStackIsCast() {
      return !functionStack.isEmpty() && functionStack.peek().getFunction().getName().equals(Functions.CAST_FUNCTION_NAME);
    }
    
    private boolean lastFunctionInStackIsCastOrTypeof() {
      return !functionStack.isEmpty() && 
             (functionStack.peek().getFunction().getName().equals(Functions.CAST_FUNCTION_NAME) ||
              functionStack.peek().getFunction().getName().equals(Functions.TYPE_OF_FUNCTION_NAME));
    }
    

    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      if (lastFunctionInStackIsGlob() && parameterCnt == 0) {
        whereClause.append(" LIKE ");
      }
    }
    

    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
    }

    public void functionStarts(FunctionExpression fe) {
      if (fe.getJavaCode().equals("null")) {
        whereClause.append("NULL ");
      }
      functionStack.push(fe);
      if (isCastOrTypeof(fe)) {
        whereClause.append("(");
      }
    }

    public void allPartsOfVariableFinished(Variable variable) {
      if (variableIdentification.isEmpty()) {
        return;
      }
      StorableColumnInformation column = variableIdentification.pop();
      StorableColumnInformation targetColumn = column;
      List<StorableColumnInformation> accessPath = new ArrayList<StorableColumnInformation>(variableIdentification);
      if (column.isStorableVariable()) {
        if (!column.isList()) {
          if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
            //wenn variableparts mit referenz-spalte enden, kann die bedingung nur vom typ "=null" oder "!=null" sein. dann ist es korrekt, 
            //dies einfach auf der ref-spalte zu tun.
            targetColumn = column.getCorrespondingReferenceIdColumn();
          } else if (column.getStorableVariableType() == StorableVariableType.EXPANSION) {
            accessPath.add(column);
            targetColumn = column.getStorableVariableInformation().getColInfoByVarType(VarType.EXPANSION_PARENT_FK);
          }
        } else {
          if (!lastFunctionInStackIsCastOrTypeof()) {
            if (column.getStorableVariableType() == StorableVariableType.EXPANSION && 
                column.getStorableVariableInformation().isSyntheticStorable()) {
              // resolve to value column of synthetic list 
              accessPath.add(column);
              targetColumn = column.getStorableVariableInformation().getColInfoByVarType(VarType.DEFAULT); // all other columns have an infrastructure purpose
            } else {
              // entspricht where liste is leer -> wir k�nnen noch kein length auf listen
              throw new RuntimeException("Condition on list valued members not supported in this way (" + column.getVariableName() + ").");
            }
          }
        }
      }
      if (lastFunctionInStackIsCastOrTypeof()) {
        // re-push column in case of additional parts
        variableIdentification.push(column);
      } else {
        List<StorableColumnInformation> expandedAccessPath = expandSyntheticColumns(accessPath, targetColumn);
        whereClause.append(expandedAccessPath, targetColumn);
      }
    }
    
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      if (isCast(fe) &&
          (functionStack.size() <= 0 ||
           !isInstanceOf(functionStack.firstElement()))) {
        if (variableIdentification.isEmpty()) {
          return;
        }
        StorableColumnInformation column = variableIdentification.pop();
        StorableColumnInformation targetColumn = column;
        List<StorableColumnInformation> accessPath = new ArrayList<StorableColumnInformation>(variableIdentification);
        if (column.isStorableVariable()) {
          if (!column.isList()) {
            if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
              //wenn variableparts mit referenz-spalte enden, kann die bedingung nur vom typ "=null" oder "!=null" sein. dann ist es korrekt, 
              //dies einfach auf der ref-spalte zu tun.
              targetColumn = column.getCorrespondingReferenceIdColumn();
            } else if (column.getStorableVariableType() == StorableVariableType.EXPANSION) {
              accessPath.add(column);
              targetColumn = column.getStorableVariableInformation().getColInfoByVarType(VarType.EXPANSION_PARENT_FK);
            }
          } else {
            if (column.getPrimitiveType() != null) {
              accessPath.add(column);
              targetColumn = column.getStorableVariableInformation().getColInfoByVarType(VarType.DEFAULT);
            } else {
              //entspricht where liste is leer -> wir k�nnen noch kein length auf listen
              throw new RuntimeException("Condition on list valued members not supported in this way (" + column.getVariableName() + ").");
            }
          }
        }
        List<StorableColumnInformation> expandedAccessPath = expandSyntheticColumns(accessPath, targetColumn);
        if (lastFunctionInStackIsCast()) { // there is another cast around us
          variableIdentification.push(column);
        } else {
          whereClause.append(expandedAccessPath, targetColumn);          
        }
      }
    }

    private List<StorableColumnInformation> expandSyntheticColumns(List<StorableColumnInformation> accessPath,
                                                                   StorableColumnInformation targetColumn) {
      List<StorableColumnInformation> expandedAccessPath = new ArrayList<XMOMStorableStructureCache.StorableColumnInformation>();
      for (StorableColumnInformation sci : accessPath) {
        boolean doExpand = false;
        expandedAccessPath.add(sci);
        if (sci.isList() &&
            sci.isStorableVariable() &&
            sci.getStorableVariableType() == StorableVariableType.EXPANSION &&
            sci.getStorableVariableInformation().isSyntheticStorable()) {
          if (sci == accessPath.get(accessPath.size() - 1)) { // last part
            if (targetColumn.getParentStorableInfo() != sci.getParentStorableInfo()) {
              doExpand = true;
            }
          } else {
            doExpand = true;
          }
        }
        if (doExpand) {
          StorableStructureInformation syntheticSsi = sci.getStorableVariableInformation();
          StorableColumnInformation forwardFK = syntheticSsi.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
          if (forwardFK != null) {
            expandedAccessPath.add(forwardFK);
          }
        }
      }
      return expandedAccessPath;
    }


    public void expression2ArgsEnds(Expression2Args expression) {
      whereClause.append(")");
    }


    public void literalExpression(LiteralExpression expression) {
      if (expectingIndex ||
          !lastFunctionInStackIsCastOrTypeof()) {
        whereClause.append("?");
        if (isGlobOnBooleanColumn()) {
          parameter.add(adjustLiteralExpressionForGlobOnBooleanColumn(expression));
        } else {
          TypeInfo relevantType = expression.getTargetType();
          if (relevantType.isBaseType()) {
            switch (relevantType.getBaseType()) {
              case BOOLEAN_OBJECT :
              case BOOLEAN_PRIMITIVE :
                parameter.add(Boolean.valueOf(expression.getValue()));
                break;
              case DOUBLE_OBJECT :
              case DOUBLE_PRIMITIVE :
                parameter.add(Double.valueOf(expression.getValue()));
                break;
              case FLOAT_OBJECT :
              case FLOAT_PRIMITIVE :
                parameter.add(Float.valueOf(expression.getValue()));
                break;
              case INT_OBJECT :
              case INT_PRIMITIVE :
                parameter.add(Integer.valueOf(expression.getValue()));
                break;
              case LONG_OBJECT :
              case LONG_PRIMITIVE :
                parameter.add(Long.valueOf(expression.getValue()));
                break;
              case STRING :
                String v = expression.getValue();
                if (lastFunctionInStackIsGlob()) {
                  //TODO: das stimmt so nicht unbedingt, wenn die parameter von functions noch irgendwie anderweitig bearbeitet werden
                  //     beispiel: glob(%0%.a, "1"+"*")
                  //     dann m�sste man aber eh solche funktionen noch separat auswerten, bevor man sie als parameter weitergibt!
                  
                  v = applyGlobEscapes(v);
                }
                parameter.add(v);
                break;
              default :
                throw new RuntimeException("unexpected type: " + relevantType.getBaseType());
            }
          } else if (relevantType.isNull()) {
            parameter.add(null);
          } else if (relevantType.isAnyNumber()) {
            parameter.add(expression.getValue());
          } else {
            throw new RuntimeException("unsupported literal expression: " + expression.getValue() + ", type=" + relevantType);
          }
        }
      }
    }


    /**
     *
     * escapes "
     * escapes %
     * changes * to %, unless * is escaped
     * removes escaping from *
     * doubles escaped backslashes for SelectionParser
     */
    private String applyGlobEscapes(String v) {
      
      Matcher m = unescapedQuotesPattern.matcher(v);
      // " -> \"
      v = m.replaceAll(this::addBackslashForUnescaped);

      m = unescapedWildcardPattern.matcher(v);
      // % -> \%
      v = m.replaceAll(this::addBackslashForUnescaped);

      m = unescapedStarPattern.matcher(v);
      //  * -> % (in reesacpeAndReplaceLast)
      // \* -> * (in removeLastEsape)
      v = m.replaceAll(x -> isEscaped(x.group()) ? removeLastEscape(x.group()) : reescapeAndReplaceLast(x.group(), "%"));
      
      //duplicate "\\\\" to "\\\\\\\\" for SelectionParser.
      v = v.replaceAll("\\\\\\\\", "\\\\\\\\\\\\\\\\");
      
      return v;
    }


    private String removeLastEscape(String s) {
      return s.length() > 2 ? s.subSequence(0, s.length() - 1) + s.substring(2, s.length()) : s;
    }


    private String reescapeAndReplaceLast(String s, String replacement) {
      return s.subSequence(0, s.length() - 1) + s.substring(0, s.length() - 1) + "%";
    }


    private String addBackslashForUnescaped(MatchResult x) {
      if (isEscaped(x.group())) {
        return x.group().subSequence(0, x.group().length() - 1) + x.group();
      }
      //add escape character
      return x.group(0).substring(0, x.group(0).length() - 1) + "\\\\\\" + x.group(0);
    }


    //s is escaped if length is off
    //s contains \\*[<charToEscape>]
    //s = \* denotes an escaped *
    //s = \\* denotes a backslash followed by star that is not escaped
    private boolean isEscaped(String s) {
      return s.length() % 2 == 0;
    }


    private String adjustLiteralExpressionForGlobOnBooleanColumn(LiteralExpression expression) {
      String booleanGlobRegExp = expression.getValue().replaceAll("[*]", ".*");
      boolean evaluatesToTrue = false;
      boolean evaluatesToFalse = false;
      try {
        evaluatesToTrue = Pattern.compile(booleanGlobRegExp, Pattern.CASE_INSENSITIVE).matcher("true").matches();
        evaluatesToFalse = Pattern.compile(booleanGlobRegExp, Pattern.CASE_INSENSITIVE).matcher("false").matches();
      } catch (PatternSyntaxException e) {
        // evalute to no_match
      }
      if (evaluatesToTrue) {
        if (evaluatesToFalse) {
          return "%";
        } else {
          return "1";
        }
      } else {
        if (evaluatesToFalse) {
          return "0";
        } else {
          return "no_match";
        }
      }
    }


    public void notStarts(Not not) {
      whereClause.append(" NOT ");
    }

    public void notEnds(Not not) {
    }

    public void operator(Operator operator) {
      whereClause.append(" ");
      if (operator instanceof OrOperator) {
        generateTypeCasts();
        whereClause.append("OR");
      } else if (operator instanceof AndOperator) {
        whereClause.append("AND");
      } else if (operator instanceof EqualsOperator) {
        whereClause.append("=");
      } else if (operator instanceof NotEqualsOperator) {
        whereClause.append("!=");
      } else {
        whereClause.append(operator.toJavaCode());
      }
      whereClause.append(" ");
      
    }

    public void variableEnds(Variable variable) {
    }


    public void variablePartStarts(VariableAccessPart part) {
      super.variablePartStarts(part);
      if (!variableIdentification.isEmpty()) {
        StorableColumnInformation column = variableIdentification.peek(); 
        if (part.getIndexDef() != null) {
          //die index-spalte ist immer im n�chsten storable definiert (helper-storable oder expansiv)
          StorableStructureInformation ssi = column.getStorableVariableInformation();
          StorableColumnInformation idxCol = ssi.getColInfoByVarType(VarType.LIST_IDX);
          whereClause.append(new ArrayList<StorableColumnInformation>(variableIdentification), idxCol);
          whereClause.append(" = ");
        }
      }
    }


    public void variablePartEnds(VariableAccessPart part) {
      if (part.getIndexDef() != null) {
        whereClause.append(" AND ");
      }
    }
    
    
    public UnfinishedWhereClause getWhereClause() {
      return whereClause;
    }


    public void indexDefStarts() {
      expectingIndex = true;
    }
    
    public void indexDefEnds() {
      expectingIndex = false;
    }
    
    
    private boolean isGlobOnBooleanColumn() {
      if (lastFunctionInStackIsGlob() && 
          whereClause.columns.size() > 0) {
        PrimitiveType typeOfLastColumn = whereClause.columns.get(whereClause.columns.size() - 1).getColumn().getPrimitiveType();
        if (typeOfLastColumn != null &&
            (typeOfLastColumn == PrimitiveType.BOOLEAN || typeOfLastColumn == PrimitiveType.BOOLEAN_OBJ)) {
          return true;
        }
      }
      return false;
    }


    private void generateTypeCasts() {
      if (casts.size() > 0) {
        Map<String, Set<String>> castMap = new HashMap<>();
        for (CastCondition cc : casts) {
          if (cc.isCast) {
            String accessPath = StringUtils.joinStringArray(cc.accessPath.toArray(new String[0]), ".");
            Set<String> casts = castMap.get(accessPath);
            if (casts == null) {
              casts = new HashSet<>();
              castMap.put(accessPath, casts);
            }
            casts.add(cc.typename);
          }
        }
        Iterator<CastCondition> conditionIter = casts.iterator();
        while (conditionIter.hasNext()) {
          CastCondition cast = conditionIter.next();
          List<StorableColumnInformation> accessPath = new ArrayList<>();
          StorableColumnInformation typenameColumn;
          StorableStructureInformation rootStructure = rootInfo;
          // re-resolve for type information
          XMOMStorableStructureInformation root = XMOMStorableStructureCache.getInstance(rootStructure.getDefiningRevision()).getStructuralInformation(rootStructure.getFqXmlName());
          StorableStructureInformation structure = root;
          if (castMap.containsKey("")) {
            String narrowedType = tryToNarrowType(structure, castMap.get(""));
            if (narrowedType == null) {
              logger.warn("Failed to adjust rootType '" + structure.getFqXmlName() + "' to it's casted types: " + castMap.get(""));
              continue;
            }
            structure = findTypeRecursivly(structure, narrowedType);
          }
          String joinedAccessPath = "";
          for (String part : cast.accessPath) {
            joinedAccessPath += part;
            accessPath.add(structure.getColumnInfoByNameAcrossHierachy(part));
            structure = structure.getColumnInfoByNameAcrossHierachy(part).getStorableVariableInformation();
            if (structure.isSynthetic) {
              StorableColumnInformation forwardFK = structure.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
              accessPath.add(forwardFK);
              structure = forwardFK.getStorableVariableInformation();
            }
            if (castMap.containsKey(joinedAccessPath)) {
              String narrowedType = tryToNarrowType(structure, castMap.get(joinedAccessPath));
              if (narrowedType == null) {
                logger.warn("Failed to adjust type '" + structure.getFqXmlName() + "' to it's casted types: " + castMap.get(joinedAccessPath));
                continue;
              }
              structure = findTypeRecursivly(structure, narrowedType);
            }
            joinedAccessPath += ".";
          }
          List<String> typeAndAllSubs = collectSubtypes(structure, cast.typename);
          typenameColumn = structure.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME);
          String typeCondition = "";
          if (typeAndAllSubs.size() > 1) {
            typeCondition += " IN (";
            for (int i = 0; i < typeAndAllSubs.size(); i++) {
              typeCondition += "\'"+ typeAndAllSubs.get(i) +"\'";
              if (i+1 < typeAndAllSubs.size()) {
                typeCondition += ", ";
              }
            }
            typeCondition += ")) ";
          } else {
            typeCondition += " = \'"+ typeAndAllSubs.get(0) +"\') ";
          }
          if (cast.isCast &&
              whereClause.columns.size() > 0) { // typeof is it's own modelled condition and not inline
            if (!whereClause.currentActiveBuilder.toString().endsWith(" AND ")) {
              whereClause.append(" AND ");
            }
          } else {
            if (whereClause.currentActiveBuilder.length() > 0 &&
                whereClause.currentActiveBuilder.charAt(whereClause.currentActiveBuilder.length() - 1) == ')') {
              whereClause.currentActiveBuilder.deleteCharAt(whereClause.currentActiveBuilder.length() - 1);
              typeCondition += ")";
            }
          }
          if (conditionIter.hasNext()) {
            typeCondition += " AND ";
          }
          whereClause.append(accessPath, typenameColumn);
          whereClause.append(typeCondition);
          
        }
        allCasts.addAll(casts);
        casts.clear();
      }
    }
    
    private String tryToNarrowType(StorableStructureInformation root, Set<String> casts) {
      if (casts.size() <= 0) {
        throw new RuntimeException("No cast type given!");
      } else if (casts.size() == 1) {
        return casts.iterator().next();
      } else {
        Map<String, StorableStructureInformation> candidates = root.getSubEntriesRecursivly().stream().collect(Collectors.toMap(s -> s.getFqXmlName(), s -> s));
        candidates.put(root.getFqXmlName(), root);
        StorableStructureInformation candidate = root;
        for (String cast : casts) {
          if (!candidates.containsKey(cast)) {
            throw new RuntimeException("Invalid cast");
          }
          
          if (!cast.equals(candidate.getFqXmlName())) {
            StorableStructureInformation castedType = candidates.get(cast);
            Map<String, StorableStructureInformation> castedTypeSupers = castedType.getSuperEntriesRecursivly().stream().collect(Collectors.toMap(s -> s.getFqXmlName(), s -> s));
            Map<String, StorableStructureInformation> candidateSupers = candidate.getSuperEntriesRecursivly().stream().collect(Collectors.toMap(s -> s.getFqXmlName(), s -> s));
            if (castedTypeSupers.containsKey(candidate.getFqXmlName())) {
              // new candidate
              candidate = castedType;
            } else if (candidateSupers.containsKey(castedType.getFqXmlName())) {
              // preserve candidate
            } else {
              // could just trigger directly returning an empty result as conditions on different subtables can never be fullfilled
              // throw new RuntimeException("Diverging extension paths! " + candidate.getFqXmlName() + " <-> " + castedType.getFqXmlName()); 
              return null;
            }
          }
        }
        return candidate.getFqXmlName();
      }
    }


    private List<String> collectSubtypes(StorableStructureInformation structure, String typename) {
      StorableStructureInformation type = findTypeRecursivly(structure, typename);
      if (type != null) {
        return collectSubTypesRecursivly(type);
      } else {
        return new ArrayList<>(); 
      }
    }
    

    private StorableStructureInformation findTypeRecursivly(StorableStructureInformation structure, String typename) {
      if (structure.getSuperEntriesRecursivly().stream().collect(Collectors.mapping(StorableStructureInformation::getFqXmlName, Collectors.toSet())).contains(typename)) {
        // downcast to parent
        return structure;
      }
      if (structure.getFqXmlName().equals(typename)) {
        return structure;
      }
      if (structure.getSubEntries() != null) {
        for (StorableStructureIdentifier subEntry : structure.getSubEntries()) {
          StorableStructureInformation type = findTypeRecursivly(subEntry.getInfo(), typename);
          if (type != null) {
            return type;
          }
        }
      }
      return null;
    }
    
    
    private List<String> collectSubTypesRecursivly(StorableStructureInformation type) {
      List<String> types = new ArrayList<>();
      types.add(type.getFqXmlName());
      for (StorableStructureIdentifier subEntry : type.getSubEntries()) {
        types.addAll(collectSubTypesRecursivly(subEntry.getInfo()));
      }
      return types;
    }


    public List<CastCondition> getAllCasts() {
      return allCasts;
    }
    
    public Parameter getParameter() {
      return parameter;
    }
    
  }
  
  public static class CastCondition {
    List<String> accessPath;
    String typename;
    boolean isCast;
  }
  
  
  protected static class UpdateBuildingVisitor extends StorableColumnIdentifingVisitor {

    private QualifiedStorableColumnInformation column;
    private StringBuilder primaryKeyListSuffix = new StringBuilder();
    private boolean needsLike;
    private List<Integer> idxs = new ArrayList<Integer>();
    private boolean expectingIndex = false;
    
    protected UpdateBuildingVisitor(XMOMStorableStructureInformation info) {
      super(info);
    }
    
    
    public boolean needsLike() {
      return needsLike;
    }
    
    
    public QualifiedStorableColumnInformation getColumns() {
      return column;
    }
    
    
    public String getPrimaryKeyListSuffix() {
      return primaryKeyListSuffix.toString();
    }
    
    
    public List<Integer> getIdxs() {
      return idxs;
    }
    

    @Override
    public void indexDefStarts() {
      expectingIndex = true;
    }
    
    @Override
    public void indexDefEnds() {
      expectingIndex = false;
    }
    
    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      column = new QualifiedStorableColumnInformation(variableIdentification.pop(), variableIdentification);  
    }
    
    
    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      column = new QualifiedStorableColumnInformation(variableIdentification.pop(), variableIdentification);  
    }


    @Override
    public void literalExpression(LiteralExpression expression) {
      if (expectingIndex) {
        primaryKeyListSuffix.append('#').append(expression.getValue());
        idxs.add(Integer.parseInt(expression.getValue()));
      }
    }


    
    @Override
    public void variablePartStarts(VariableAccessPart part) {
      if (!variableIdentification.isEmpty()) {
        StorableColumnInformation column = variableIdentification.peek();
        boolean isReference = column.isStorableVariable() && column.getStorableVariableType() == StorableVariableType.REFERENCE;
        boolean isReferencedList = column.isStorableVariable() && column.isList() && column.getStorableVariableInformation().isReferencedList();
        if (isReference || isReferencedList) {
          needsLike = false;
          primaryKeyListSuffix.setLength(0);
        }
      }
      super.variablePartStarts(part);
      if (!variableIdentification.isEmpty()) {
        StorableColumnInformation column = variableIdentification.peek();
        if (column.isList() && part.getIndexDef() == null) {
          primaryKeyListSuffix.append("#%");
          needsLike = true;
          idxs.add(0); // TODO should be documented 
        }
      }
    }


    @Override
    public void variableEnds(Variable variable) {
      
    }
    
    @Override
    public void functionStarts(FunctionExpression fe) {
      super.functionStarts(fe);
    }
    
    @Override
    public void functionEnds(FunctionExpression fe) {
      if (QueryBuildingVisitor.isCast(fe)) {
        Expression accessPart = fe.getSubExpressions().get(1);
        if (accessPart instanceof FollowableType) {
          FollowableType sva = (FollowableType)accessPart;
          List<String> access = QueryBuildingVisitor.retrieveAccessPathRecursivly(sva);
          
          StorableStructureInformation structure = rootInfo;
          if (!variableIdentification.isEmpty()) {
            structure = variableIdentification.get(variableIdentification.size() - 1).getStorableVariableInformation();
          }
          
          List<StorableColumnInformation> accessPath = new ArrayList<>();
          for (String part : access) {
            accessPath.add(structure.getColumnInfoByNameAcrossHierachy(part));
            structure = structure.getColumnInfoByNameAcrossHierachy(part).getStorableVariableInformation();
            if (structure.isSynthetic) {
              StorableColumnInformation forwardFK = structure.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
              accessPath.add(forwardFK);
              structure = forwardFK.getStorableVariableInformation();
            }
          }
          
          StorableColumnInformation lastColumn = accessPath.get(accessPath.size() - 1);
          variableIdentification.add(lastColumn);
        }
      }
    }
  }
  
  
  
  protected static class XMOMStorableStructureCacheVariableContextIdentification implements VariableContextIdentification {

    private final XMOMStorableStructureInformation infoForVarNum0; //%0% zugeh�rige info
    
    public XMOMStorableStructureCacheVariableContextIdentification(XMOMStorableStructureInformation info) {
      this.infoForVarNum0  = info;
    }
    
    public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException,
        XPRC_InvalidVariableMemberNameException {
      if (v.getVarNum() != 0) {
        throw new XPRC_InvalidVariableIdException("" + v.getVarNum());
      }
      return new XMOMStorableStructureCacheVariableInfoRoot(infoForVarNum0);
    }

    public TypeInfo getTypeInfo(final String originalXmlName) {
      TypeLinkCollectionVisitor visitor = new TypeLinkCollectionVisitor();
      // infoForVarNum0 is already merged, re-resolve it for type information
      XMOMStorableStructureInformation rootStructure = infoForVarNum0.reresolveMergedClone();
      rootStructure.traverse(visitor);
      Map<String, Collection<StorableColumnInformation>> types = visitor.getTypes();
      if (types.containsKey(originalXmlName)) {
        if (types.get(originalXmlName).size() > 1) {
          logger.debug("XMOMStorableStructureCacheVariableContextIdentification identified several types for " + originalXmlName + ": #" + types.get(originalXmlName).size());
        }
        StorableColumnInformation ssi = types.get(originalXmlName).iterator().next();
        //clone column and set referenced Storable to casted type
        StorableColumnInformation clone = ssi.clone();
        for (StorableStructureInformation subStructure : ssi.getStorableVariableInformation().getSubEntriesRecursivly()) {
          if (subStructure.getFqXmlName().equals(originalXmlName)) {
            clone.setStorableVariableInformation(new XMOMStorableStructureCache.DirectStorableStructureIdentifier(subStructure));
          }
        }
        return new TypeInfo(new StorableStructureType(clone), ssi.isList());
      } else {
        logger.debug("Type not found! rootStructure: " + rootStructure.getFqXmlName() + " types: " + types);
        return null;
      }
    }

    public Long getRevision() {
      throw new RuntimeException();
    }

    public VariableInfo createVariableInfo(TypeInfo resultType) {
      if (resultType != null &&
          resultType.isModelledType() &&
          resultType.getModelledType() != null &&
          resultType.getModelledType() instanceof StorableStructureType ||
          resultType.getModelledType() instanceof XMOMStorableStructureType) {
        if (resultType.getModelledType() instanceof StorableStructureType) {
          StorableStructureType type = (StorableStructureType) resultType.getModelledType();
          return new XMOMStorableStructureCacheVariableInfoChild(type.structureLink);
        } else {
          XMOMStorableStructureType type = (XMOMStorableStructureType) resultType.getModelledType();
          return new XMOMStorableStructureCacheVariableInfoRoot((XMOMStorableStructureInformation) type.type);  
        }
      } else {
        return null;
      }
    }
    
  }
  
  
private static class StorableStructureType implements ModelledType {
    
  private final StorableColumnInformation structureLink;
    
    StorableStructureType(StorableColumnInformation structureLink) {
      this.structureLink = structureLink;
    }

    @Override
    public String getFqClassName() {
      return structureLink.getStorableVariableInformation().getFqClassNameForDatatype();
    }

    @Override
    public boolean isSuperClassOf(ModelledType otherType) {
      // TODO
      return true;
    }

    @Override
    public String getFqXMLName() {
      return structureLink.getStorableVariableInformation().getFqXmlName();
    }

    @Override
    public String getSimpleClassName() {
      return GenerationBase.getSimpleNameFromFQName(getFqClassName());
    }

    @Override
    public List<VariableInfo> getAllMemberVarsIncludingInherited() {
      Collection<StorableColumnInformation> columns = structureLink.getStorableVariableInformation().generateMergedClone(new AcceptAllMergeFilter()).getAllRelevantStorableColumnsForDatatypeReaderRecursivly();
      List<VariableInfo> varInfos = new ArrayList<>();
      for (StorableColumnInformation sci : columns) {
        varInfos.add(new XMOMStorableStructureCacheVariableInfoChild(sci));
      }
      return varInfos;
    }

    @Override
    public List<OperationInfo> getAllInstanceOperationsIncludingInherited() {
      throw new RuntimeException("unsupported");
    }

    @Override
    public String generateEmptyConstructor() throws XPRC_InvalidPackageNameException {
      throw new RuntimeException("unsupported");
    }

    @Override
    public boolean isAbstract() {
      return structureLink.getStorableVariableInformation().isAbstract();
    }

    @Override
    public Set<ModelledType> getSubTypesRecursivly() {
      Set<ModelledType> types = new HashSet<>();
      Set<StorableStructureInformation> ssis = structureLink.getStorableVariableInformation().getSubEntriesRecursivly();
      for (StorableStructureInformation ssi : ssis) {
        StorableColumnInformation sci = structureLink.clone();
        sci.setStorableVariableInformation(XMOMStorableStructureCache.identifierOf(ssi));
        types.add(new StorableStructureType(sci));
      }
      return types;
    }
    
  }
  
  
  private static class XMOMStorableStructureCacheVariableInfoRoot implements VariableInfo {
    
    private final XMOMStorableStructureInformation info;

    public XMOMStorableStructureCacheVariableInfoRoot(XMOMStorableStructureInformation info) {
      this.info = info;
    }


    public VariableInfo follow(List<VariableAccessPart> parts, int depth) {
      StorableStructureInformation ssi = info;
      StorableColumnInformation sci = null;
      for (int i = 0; i <= depth; i++) {
        if (sci != null) {
          ssi = sci.getStorableVariableInformation();
        }
        VariableAccessPart p = parts.get(i);
        sci = ssi.getColumnInfoByName(p.getName());
      }
      if (sci == null) {
        return this; //depth == -1?
      }
      return new XMOMStorableStructureCacheVariableInfoChild(sci);
    }

    public TypeInfo getTypeInfo(boolean ignoreList) {
      return new TypeInfo(new XMOMStorableStructureType(info), false);
    }

    public String getJavaCodeForVariableAccess() {
      throw new RuntimeException("unsupported");
    }

    public String getVarName() {
      throw new RuntimeException("unsupported");
    }

    public void castTo(TypeInfo type) {
      throw new RuntimeException("unsupported");
    }
    
  }
  
  
  private static class XMOMStorableStructureCacheVariableInfoChild implements VariableInfo {
    private final StorableColumnInformation col;
    
    private XMOMStorableStructureCacheVariableInfoChild(StorableColumnInformation col) {
      this.col = col;
    }

    public VariableInfo follow(List<VariableAccessPart> parts, int depth) {
      StorableStructureInformation ssi = col.getStorableVariableInformation();
      StorableColumnInformation sci = null;
      for (int i = 0; i <= depth; i++) {
        if (sci != null) {
          ssi = sci.getStorableVariableInformation();
        }
        VariableAccessPart p = parts.get(i);
        sci = ssi.getColumnInfo(p.getName());
      }
      if (sci == null) {
        return this; //depth == -1?
      }
      return new XMOMStorableStructureCacheVariableInfoChild(sci);
    }

    public TypeInfo getTypeInfo(boolean ignoreList) {
      if (!ignoreList && col.isList()) {
        return new TypeInfo(BaseType.LIST);
      }
      if (!col.isStorableVariable()) {
        return new TypeInfo(col.getPrimitiveType());
      } else {
        if (col.getStorableVariableType() == StorableVariableType.EXPANSION) {
          return new TypeInfo(new StorableStructureType(col), false);
        } else {
          return new TypeInfo(new XMOMStorableStructureType(col.getStorableVariableInformation()), false);
        }
      }
    }

    public String getJavaCodeForVariableAccess() {
      throw new RuntimeException("unsupported");
    }

    public String getVarName() {
      return col.getColumnName();
    }
    
    public void castTo(TypeInfo type) {
    }
    
  }
  
  
  private static class XMOMStorableStructureType implements ModelledType {
    
    private final StorableStructureInformation type;
    
    private XMOMStorableStructureType(StorableStructureInformation type) {
      this.type = type;
    }

    public String getFqClassName() {
      return type.getFqClassNameForDatatype();
    }
    
    public String getFqXMLName() {
      return type.getFqXmlName();
    }
    
    public boolean isSuperClassOf(ModelledType otherType) {
      if (otherType instanceof XMOMStorableStructureType) {
        XMOMStorableStructureType otherTypeSSI = (XMOMStorableStructureType) otherType;
        return type.isSuperTypeOf(otherTypeSSI.type);
      }
      throw new RuntimeException();
    }

    public String getSimpleClassName() {
      throw new RuntimeException("unsupported");
    }

    public List<VariableInfo> getAllMemberVarsIncludingInherited() {
      Set<StorableColumnInformation> columns = type.getColumnInfoAcrossHierarchy();
      List<VariableInfo> varInfos = new ArrayList<>();
      for (StorableColumnInformation column : columns) {
        if (column.isStorableVariable()) {
          varInfos.add(new XMOMStorableStructureCacheVariableInfoChild(column));
        } else {
          varInfos.add(new PrimitiveVariableInfo(column));
        }
      }
      return varInfos;
    }

    @Override
    public int hashCode() {
      return type.getTableName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof XMOMStorableStructureType)) {
        return false;
      }
      return type.getTableName().equals(((XMOMStorableStructureType) obj).type.getTableName());
    }

    public List<OperationInfo> getAllInstanceOperationsIncludingInherited() {
      throw new RuntimeException("unsupported");
    }

    public String generateEmptyConstructor() {
      throw new RuntimeException("unsupported");
    }

    public boolean isAbstract() {
      return false;
    }

    @Override
    public Set<ModelledType> getSubTypesRecursivly() {
      Set<ModelledType> types = new HashSet<>();
      Set<StorableStructureInformation> ssis = type.getSubEntriesRecursivly();
      for (StorableStructureInformation ssi : ssis) {
        types.add(new XMOMStorableStructureType(ssi));
      }
      return types;
    }

  }
  
  
  private static class PrimitiveVariableInfo implements VariableInfo {
    
    private StorableColumnInformation column;
    private TypeInfo typeOverwrite;
    
    public PrimitiveVariableInfo(StorableColumnInformation column) {
      this.column = column;
    }
    
    public VariableInfo follow(List<VariableAccessPart> parts, int depth)
        throws XPRC_InvalidVariableMemberNameException {
      throw new RuntimeException("Primitives can not be followed");
    }

    public TypeInfo getTypeInfo(boolean ignoreList) {
      if (typeOverwrite != null) {
        return typeOverwrite;
      } else {
        return new TypeInfo(column.getPrimitiveType());
      }
    }

    public String getJavaCodeForVariableAccess() {
      throw new RuntimeException("Primitive getJavaCodeForVariableAccess for PersistenceAccess");
    }

    public String getVarName() {
      return column.getColumnName();
    }

    public void castTo(TypeInfo type) {
      if (type.isPrimitive()) {
        typeOverwrite = type;
      } else {
        throw new RuntimeException("Primitive can not be cast to complex");
      }
    }
    
  }
  
  
  public static class QueryFunctionStore implements SupportedFunctionStore {
    
    protected final static String GLOB_FUNCTION_NAME = "glob";
    protected final static String GLOB_FUNCTION_REPRESENTATION = "GLOB_FUNCTION_REPRESENTATION";

    private static volatile Set<Function> functions; 

    public Set<Function> getSupportedFunctions() {
      if (functions == null) {
        Set<Function> supportedFunctions = new HashSet<Function>();
        supportedFunctions.add(new Function(GLOB_FUNCTION_NAME, new TypeInfo(BaseType.BOOLEAN_PRIMITIVE),  new FunctionParameterTypeDefinition() {

          public TypeInfo getType(int parameterCnt) {
            if (parameterCnt == 0 || parameterCnt == 1) {
              return new TypeInfo(BaseType.STRING);
            }
            throw new RuntimeException();
          }

          public int numberOfParas() {
            return 2;
          }

          public int numberOfOptionalParas() {
            return 0;
          }
        }, GLOB_FUNCTION_REPRESENTATION));
        supportedFunctions.add(new Function("null", TypeInfo.NULL, new FunctionParameterTypeDefinition() {

          public TypeInfo getType(int parameterCnt) {
            throw new RuntimeException();
          }


          public int numberOfParas() {
            return 0;
          }


          public int numberOfOptionalParas() {
            return 0;
          }
        }, "null"));
        supportedFunctions.add(Functions.getFunction(Functions.CAST_FUNCTION_NAME));
        supportedFunctions.add(Functions.getFunction(Functions.TYPE_OF_FUNCTION_NAME));
        
        functions = supportedFunctions;
      }
      
      return functions;
    }
    
  }
  
  
  static FormulaParsingResult EMPTY_FORMULA_PARSING_RESULT = new FormulaParsingResult(new UnfinishedWhereClause() {
    String finishWhereClause(AliasDictionary dictionary) { return ""; };
  }, Collections.<CastCondition>emptyList());
  
  protected static class FormulaParsingResult {
    
    private final UnfinishedWhereClause whereClause;
    private final List<CastCondition> parsedCasts;
    
    FormulaParsingResult(UnfinishedWhereClause whereClause, List<CastCondition> parsedCasts) {
      this.whereClause = whereClause;
      this.parsedCasts = parsedCasts;
    }
    
    public List<QualifiedStorableColumnInformation> getColumnsFromConditions() {
      return whereClause.columns;
    }
    
    public String getSqlString(AliasDictionary dictionary) {
      String whereClauseString = whereClause.finishWhereClause(dictionary);
      whereClauseString = whereClauseString.replaceAll("!= NULL ", "IS NOT NULL "); //FIXME IS NULL-behandlung ist hier nicht sch�n aufgehoben...
      whereClauseString = whereClauseString.replaceAll("= NULL ", "IS NULL ");
      return whereClauseString;
    }
    
    public UnfinishedWhereClause getWhere() {
      return whereClause;
    }
    
    public List<CastCondition> getCasts() {
      return parsedCasts;
    }

  }
  
  
  protected static class UpdateParsingResult {
    
    private final QualifiedStorableColumnInformation column;
    private final String primaryKeyListSuffix;
    private final boolean needsLike;
    private List<Integer> idx;
    
    UpdateParsingResult(QualifiedStorableColumnInformation column, String primaryKeyListSuffix, boolean needsLike, List<Integer> idx) {
      this.column = column;
      this.primaryKeyListSuffix = primaryKeyListSuffix;
      this.needsLike = needsLike;
      this.idx = idx;
    }

    
    public QualifiedStorableColumnInformation getColumn() {
      return column;
    }

    /**
     * enth�lt nur listen indizes seit dem letzten xmom storable 
     */
    public String getPrimaryKeyListSuffix() {
      return primaryKeyListSuffix;
    }

    
    public boolean needsLike() {
      return needsLike;
    }
    
    /**
     * alle indizes
     */
    public List<Integer> getListIndizesForRootObject() {
      return idx;
    }
    
  }
  
  
  protected static class UnfinishedWhereClause {
    
    private StringBuilder currentActiveBuilder = new StringBuilder();
    final List<String> strings;
    final List<QualifiedStorableColumnInformation> columns;
    
    UnfinishedWhereClause() {
      this.strings = new ArrayList<String>();
      this.columns = new ArrayList<QualifiedStorableColumnInformation>();
    }
    
    UnfinishedWhereClause append(String string) {
      currentActiveBuilder.append(string);
      return this;
    }
    
    UnfinishedWhereClause append(List<StorableColumnInformation> path, StorableColumnInformation target) {
      strings.add(currentActiveBuilder.toString());
      currentActiveBuilder = new StringBuilder();
      columns.add(new QualifiedStorableColumnInformation(target, path));
      return this;
    }
    
    String finishWhereClause(AliasDictionary dictionary) {
      strings.add(currentActiveBuilder.toString());
      StringBuilder whereBuilder = new StringBuilder();
      Iterator<QualifiedStorableColumnInformation> columnIterator = columns.iterator();
      for (String string : strings) {
        whereBuilder.append(string);
        if (columnIterator.hasNext()) {
          QualifiedStorableColumnInformation column = columnIterator.next();
          String tableName = null;
          if (dictionary != null) {
            tableName = dictionary.getTableAlias(column.getAccessPath());  
          }
          if (tableName == null) {
            whereBuilder.append(column.getColumn().getParentStorableInfo().getTableName());
          } else {
            whereBuilder.append(tableName);
          }
          whereBuilder.append('.')
                      .append(column.getColumn().getColumnName());
        }
      }
      return whereBuilder.toString();
    }
    
  }
  
  
  protected static FormulaParsingResult parseFormula(IFormula condition, XMOMStorableStructureInformation info, Parameter params) {
    if (condition.getFormula() == null || condition.getFormula().length() <= 0) {
      return EMPTY_FORMULA_PARSING_RESULT;
    } else {
      PersistenceExpressionVisitors.QueryBuildingVisitor qbv = new PersistenceExpressionVisitors.QueryBuildingVisitor(info, params);
      visitAfterDefaultVisitorStack(qbv, condition.getFormula(), info);
      qbv.generateTypeCasts();
      return new FormulaParsingResult(qbv.getWhereClause(), qbv.getAllCasts());
    }
  }
  
  
  private static void visitAfterDefaultVisitorStack(Visitor visitor, String expression, XMOMStorableStructureInformation info) {
    VariableContextIdentification vci = new PersistenceExpressionVisitors.XMOMStorableStructureCacheVariableContextIdentification(info);
    ModelledExpression me;
    try {
      me = ModelledExpression.parse(vci, expression, new PersistenceExpressionVisitors.QueryFunctionStore());
    } catch (XPRC_ParsingModelledExpressionException e) {
      throw new RuntimeException("", e);
    }
    InitVariablesVisitor iv = new InitVariablesVisitor();
    me.visitTargetExpression(iv);
    InferOriginalTypeVisitor iotv = new InferOriginalTypeVisitor();
    me.visitTargetExpression(iotv);
    MakeTypeDefinitionConsistentVisitor mdcv = new MakeTypeDefinitionConsistentVisitor();
    mdcv.setAllowStringComparison();
    me.visitTargetExpression(mdcv);
    me.visitTargetExpression(visitor);
  }
  
  
  protected static UpdateParsingResult parseUpdatePath(String updatePath, XMOMStorableStructureInformation info) {
    PersistenceExpressionVisitors.UpdateBuildingVisitor ubv = new PersistenceExpressionVisitors.UpdateBuildingVisitor(info);
    visitAfterDefaultVisitorStack(ubv, updatePath, info);
    return new UpdateParsingResult(ubv.getColumns(), ubv.getPrimaryKeyListSuffix(), ubv.needsLike(), ubv.getIdxs());
  }

}
