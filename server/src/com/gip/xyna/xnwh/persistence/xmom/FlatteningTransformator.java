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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;

//transformiert nun auch von varNames -> colNames, umbennen?
public class FlatteningTransformator {

  private static class IFormulaTransformWrapper implements IFormula {
    private final List<Accessor> values;
    private final String formula;

    public IFormulaTransformWrapper(IFormula formula, XMOMStorableStructureInformation info, long revision) {
      values = formula.getValues();
      this.formula = transformXFL(info, formula.getFormula(), revision);
    }

    public List<Accessor> getValues() {
      return values;
    }

    public String getFormula() {
      return formula;
    }
  }
  
  
  private static class TemporaryVariableContext implements VariableContextIdentification {

    public VariableInfo createVariableInfo(Variable v, boolean follow) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
      throw new RuntimeException();
    }

    public TypeInfo getTypeInfo(String originalXmlName) {
      throw new RuntimeException();
    }

    public Long getRevision() {
      throw new RuntimeException();
    }

    public VariableInfo createVariableInfo(TypeInfo resultType) {
      throw new RuntimeException();
    }

  }
  
  private static class TransformXFLVisitor extends IdentityCreationVisitor {

    private static class XMOMStorableStructureInfoWrapper {

      private final StorableStructureInformation info;
      private final int correspondingStackIndexOfFirstVariablePart;
      private boolean isCastStart;


      private XMOMStorableStructureInfoWrapper(StorableStructureInformation info, int stackDepth) {
        this.info = info;
        this.correspondingStackIndexOfFirstVariablePart = stackDepth;
      }
    }


    private final Stack<XMOMStorableStructureInfoWrapper> structureStack = new Stack<XMOMStorableStructureInfoWrapper>();
    private final Stack<String> variablePartStack = new Stack<>();
    private final Set<Long> reachableRevisions;
    private int transformed = 0;
    private int castStackCount = 0;

    public TransformXFLVisitor(XMOMStorableStructureInformation info, long revision) {
      if (XMOMPersistenceOperationAlgorithms.allowReResolution.get()) {
        XMOMStorableStructureInformation reresolvedInfo = info.reresolveMergedClone();
        this.structureStack.add(new XMOMStorableStructureInfoWrapper(reresolvedInfo, 0));  
      } else {
        this.structureStack.add(new XMOMStorableStructureInfoWrapper(info, 0));  
      }
      this.reachableRevisions = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, this.reachableRevisions);
      this.reachableRevisions.add(revision);
    }


    public void variablePartStarts(VariableAccessPart part) {
      variablePartStack.add(part.getName());
      transformed++;
      String transformedName = transform();
      if (transformedName != null) {
        transformed = 0;
        sb.append(".").append(transformedName);
        if (part.isMemberVariableAccess() && 
            part.getIndexDef() != null) {
          appendIndexDefStart();
        }
      }
    }


    private String transform() {
      String relativePath = getRelativePath();
      Set<StorableColumnInformation> columnInfo = structureStack.peek().info.getColumnInfoAcrossHierarchy(); 
      for (StorableColumnInformation sci : columnInfo) {
        if (sci.isFlattened() && 
            sci.getPath().equals(relativePath)) {
          addReferencedStorableToStack(sci);
          return sci.getColumnName();
        } else if (!sci.isFlattened() && 
                   relativePath.equals(sci.getVariableName())) {
          addReferencedStorableToStack(sci);
          return sci.getColumnName();
        }
      }
      return null;
    }


    private void addReferencedStorableToStack(StorableColumnInformation sci) {
      if (sci.isStorableVariable()) {
        if (sci.isList() && sci.getStorableVariableInformation().isReferencedList()) {
          //die hilfstabelle für die listenwertigkeit überspringen
          sci = sci.getStorableVariableInformation().getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
        }
        structureStack.add(new XMOMStorableStructureInfoWrapper(sci.getStorableVariableInformation(), variablePartStack.size()));
      }
    }


    private String getRelativePath() {
      StringBuilder relativePath = new StringBuilder();
      for (int i = structureStack.peek().correspondingStackIndexOfFirstVariablePart; i < variablePartStack.size(); i++) {
        if (i > structureStack.peek().correspondingStackIndexOfFirstVariablePart) {
          relativePath.append(".");
        }
        relativePath.append(variablePartStack.get(i));
      }
      return relativePath.toString();
    }


    public void variablePartEnds(VariableAccessPart part) {
      if (part.getIndexDef() != null) {
        if (transformed == 0) {
          appendIndexDefEnd();
        } else {
          throw new RuntimeException();
        }
      }
    }


    public void variablePartSubContextEnds(VariableAccessPart p) {
      if (transformed > 0) {
        for (int i = structureStack.peek().correspondingStackIndexOfFirstVariablePart; i < variablePartStack.size(); i++) {
          sb.append(".").append(variablePartStack.get(i));
        }
        transformed = 0;
      }
      
      if (castStackCount <= 0) {
        variablePartStack.pop();
        if (structureStack.peek().correspondingStackIndexOfFirstVariablePart > variablePartStack.size()) {
          structureStack.pop();
        }
      }
    }
    
    public void allPartsOfVariableFinished(Variable variable) {
      super.allPartsOfVariableFinished(variable);
    }
    
    public void functionStarts(FunctionExpression fe) {
      if (isCast(fe)) {
        structureStack.peek().isCastStart = true;
        castStackCount++;
      }
      super.functionStarts(fe);
    }
    

    public void functionEnds(FunctionExpression fe) {
      if (isCast(fe)) {
        castStackCount--;
        castTo(((FunctionExpression.CastExpression)fe).getDynamicTypeName());
      }
      super.functionEnds(fe);
    }
    


    private void castTo(String dynamicTypeName) {
      StorableStructureInformation ssi = structureStack.peek().info;
      for (StorableStructureInformation subEntry : ssi.getSubEntriesRecursivly()) {
        if (subEntry.getFqXmlName().equals(dynamicTypeName)) {
          XMOMStorableStructureInfoWrapper wrapper = new XMOMStorableStructureInfoWrapper(subEntry, structureStack.peek().correspondingStackIndexOfFirstVariablePart);
          XMOMStorableStructureInfoWrapper previous = structureStack.pop();
          wrapper.isCastStart = previous.isCastStart;
          structureStack.push(wrapper);
        }
      }
    }


    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      super.allPartsOfFunctionFinished(fe);
      if (isCast(fe)) {
        if (castStackCount > 0) { // is already decremented in functionEnds
          for (int i = structureStack.size() - 1; i > 0; i--) {
            if (structureStack.get(i).isCastStart) {
              structureStack.get(i).isCastStart = false;
              break;
            }
          }
        } else {
          while (!structureStack.peek().isCastStart) {
            structureStack.pop();
            if (!variablePartStack.isEmpty()) {
              variablePartStack.pop();
            }
          }
        }
      }
    }
    
    static boolean isCast(FunctionExpression fe) {
      return fe.getFunction().getName().equals(Functions.CAST_FUNCTION_NAME);
    }

  }

  
  private static String transformXFL(XMOMStorableStructureInformation info, String xfl, long revision) {
    if (xfl == null || xfl.trim().length() == 0) {
      return xfl;
    }
    String xfl2 = xfl.replaceAll(Pattern.quote("[]"), "");
    try {
      ModelledExpression me = ModelledExpression.parse(new TemporaryVariableContext(), xfl2, new PersistenceExpressionVisitors.QueryFunctionStore());
      TransformXFLVisitor vis = new TransformXFLVisitor(info, revision);
      me.visitTargetExpression(vis);
      String transformation = vis.getXFLExpression() + (xfl.endsWith("*") ? "*" : "");
      return transformation;
    } catch (XPRC_ParsingModelledExpressionException e) {
      //ersetzte[] in der position in der fehlermeldung berücksichtigen
      int cnt = 0;
      for (int i = 0; i < e.getPosition(); i++) {
        if (xfl2.charAt(i) == '[') {
          cnt++;
        }
      }
      e.setPosition(e.getPosition() + 2 * cnt);
      throw new RuntimeException(e);
    }
  }


  public IFormula transformFormula(IFormula formula, XMOMStorableStructureInformation info, long revision) {
    return new IFormulaTransformWrapper(formula, info, revision);
  }


  public SelectionMask transformSelectionMask(SelectionMask selectionMask, XMOMStorableStructureInformation info, long revision) {
    List<String> transformedCols = new ArrayList<String>();
    if (selectionMask.columns != null) {
      for (String col : selectionMask.columns) {
        transformedCols.add(transformXFL(info, col, revision));
      }
    }
    SelectionMask transformedMask = new SelectionMask(selectionMask.roottype, transformedCols);
    return transformedMask;
  }


  public QueryParameter transformQueryParameter(QueryParameter queryParameter, XMOMStorableStructureInformation info, long revision) {
    SortCriterion[] transformedSortCriteria;
    if (queryParameter.getSortCriterions() == null) {
      transformedSortCriteria = null;
    } else {
      transformedSortCriteria = new SortCriterion[queryParameter.getSortCriterions().length];
      for (int i = 0; i < queryParameter.getSortCriterions().length; i++) {
        transformedSortCriteria[i] =
            new SortCriterion(transformXFL(info, queryParameter.getSortCriterions()[i].getCriterion(), revision),
                              queryParameter.getSortCriterions()[i].isReverse());
      }
    }
    QueryParameter transformedQueryParameter =
        new QueryParameter(queryParameter.getMaxObjects(), queryParameter.queryAcrossHistory(), transformedSortCriteria);
    return transformedQueryParameter;
  }
  
  
  public List<String> transformPaths(List<String> paths, XMOMStorableStructureInformation info, long revision) {
    List<String> transformedPaths = new ArrayList<String>();
    for (String path : paths) {
      transformedPaths.add(transformXFL(info, path, revision));
    }
    return transformedPaths;
  }

}
