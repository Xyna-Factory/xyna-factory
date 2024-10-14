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

package com.gip.xyna.xact.filter.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

public class ExpressionUtils {

  public static void cleanUpMappingInputsAndFilterConditions(List<String> filterConditions, StepMapping stepMapping) {
    if (filterConditions == null) {
      filterConditions = new ArrayList<String>();
    }
    
    List<Integer> variableIndicesToRemove = calculateVariableIndicesToRemove(filterConditions, stepMapping);

    if (variableIndicesToRemove.size() == 0) {
      return; //quick exit: nothing to do
    }
    
    if (stepMapping.isConditionMapping()) {
      for (int i = 0; i < filterConditions.size(); i++) {
        filterConditions.set(i, unescapeQueryExpression(filterConditions.get(i)));
      }
    }
    
    List<List<XFLLexem>> itemsPerCondition = lexFilterConditions(filterConditions);

    //remove from back to front
    for (int i = variableIndicesToRemove.size() - 1; i >= 0; i--) {
      int indexToRemove = variableIndicesToRemove.get(i);
      //remove inputs from mapping
      // -1 here, since %0% is supposed to be the output => %1% is first input
      stepMapping.getInputConnections().removeInputConnection(indexToRemove - 1);
      stepMapping.getInputVars().remove(indexToRemove - 1);
    }
    
    //lower indices
    for (int j = 0; j < filterConditions.size(); j++) {
      String updatedCondition = updateExpressionIndices(itemsPerCondition.get(j), variableIndicesToRemove);
      if (stepMapping.isConditionMapping()) {
        updatedCondition = escapeQueryExpression(updatedCondition);
      }
      filterConditions.set(j, updatedCondition);
    }
  }
  
  
  private static List<List<XFLLexem>> lexFilterConditions(List<String> filterConditions) {
    if (filterConditions == null) {
      return new ArrayList<List<XFLLexem>>();
    }
    
    List<List<XFLLexem>> result = new ArrayList<List<XFLLexem>>(filterConditions.size());
    for (String filterCondition : filterConditions) {
      if(filterCondition == null) {
        result.add(Collections.emptyList());
        continue;
      }
      List<XFLLexem> conditionLexed = XFLLexer.lex(filterCondition, true);
      result.add(conditionLexed);
    }

    return result;
  }

  

  private static String updateExpressionIndices(List<XFLLexem> list, List<Integer> variableIndicesToRemove) {
    StringBuilder sb = new StringBuilder();
    
    for(XFLLexem lexem : list) {
      if(lexem.getType() == TokenType.VARIABLE) {
        int currentIndex = Integer.parseInt(lexem.getToken().substring(1, lexem.getToken().length() -1));
        long toDecrease = variableIndicesToRemove.stream().filter(x -> x < currentIndex).count();
        sb.append("%");
        sb.append((currentIndex - toDecrease));
        sb.append("%");
      } else {
        sb.append(lexem.getToken());
      }
    }
    
    return sb.toString();
  }


  private static boolean variableStillRelevant(int index, List<List<XFLLexem>> filterConditions) {
    String targetString = "%" + index + "%";
    for(List<XFLLexem> filterCondition : filterConditions) {
      for(XFLLexem lexem : filterCondition) {
        if(lexem.getType() == TokenType.VARIABLE && lexem.getToken().equals(targetString)) {
          return true;
        }
      }
    }
    return false;
  }
  
  
  private static List<String> readExpressions(StepChoice choice){
    List<CaseInfo> cases = choice.getHandledCases();
    List<String> expressions = new ArrayList<String>();
    String outerExpression = choice.getOuterCondition();
    int formulaDelimiterLength = StepChoice.FORMULA_GUI_DELIMITER.length();

    if(choice.getDistinctionType() == DistinctionType.ConditionalBranch) {
      int indexOfDelimiter = StepChoice.calcIndexOfFormulaDelimiter(choice.getOuterCondition());
      int firstPartOfChoiceConditionLength = indexOfDelimiter;
      int lastPartOfChoiceConditionLength = outerExpression.length() - firstPartOfChoiceConditionLength - formulaDelimiterLength;

      expressions.add(outerExpression.substring(0, firstPartOfChoiceConditionLength));
      expressions.add(outerExpression.substring(firstPartOfChoiceConditionLength + formulaDelimiterLength));
    
      for (CaseInfo c : cases) {
        //c.getComplexName() = firstPartOfChoiceCondition + actual expression + lastPartOfChoiceExpression
        String premise = c.getComplexName();
        
        //default branch is null
        if(premise == null) {
          continue;
        }
        
        //if there is nothing on case, the outer condition is omitted - add empty expression
        if(premise.length() == 0) {
          expressions.add("");
          continue;
        }
        int endIndex = premise.length() - lastPartOfChoiceConditionLength;
        expressions.add(premise.substring(firstPartOfChoiceConditionLength, endIndex));
      }
      
    } else {
      int firstPartOfChoiceConditionLength = choice.getOuterCondition().length();
      expressions.add(outerExpression.substring(0, firstPartOfChoiceConditionLength));
    }


    return expressions;
  }


  private static String updateOuterExpression(StepChoice choice, List<List<XFLLexem>> conditions, List<Integer> indicesToRmv) {
    String updatedExpression = null;
    if (choice.getDistinctionType() == DistinctionType.ConditionalBranch) {
      String updatedExpressionStart = updateExpressionIndices(conditions.get(0), indicesToRmv);
      String updatedExpressionEnd = updateExpressionIndices(conditions.get(1), indicesToRmv);
      updatedExpression = updatedExpressionStart + StepChoice.FORMULA_GUI_DELIMITER + updatedExpressionEnd;
    } else {
      updatedExpression = updateExpressionIndices(conditions.get(0), indicesToRmv);
    }

    choice.replaceOuterCondition(updatedExpression);
    return updatedExpression;
  }
  
  
  private static void lowerIndices(StepChoice choice, List<List<XFLLexem>> conditions, List<Integer> indicesToRmv) {
    String updatedExpression = updateOuterExpression(choice, conditions, indicesToRmv);
    choice.replaceOuterCondition(updatedExpression);

    int indexOfDelimiter = StepChoice.calcIndexOfFormulaDelimiter(choice.getOuterCondition());

    if (indexOfDelimiter == -1) {
      indexOfDelimiter = 0; //conditional choice has no delimiter
    }
    
    String updatedOuterConditionStart = updatedExpression.substring(0, indexOfDelimiter);
    String updatedOuterConditionEnd = updatedExpression.substring(indexOfDelimiter);


    //lower indices in cases
    int skipedOuterConditions = choice.getDistinctionType() == DistinctionType.ConditionalBranch ? 2 : 1;
    for (int j = skipedOuterConditions; j < conditions.size(); j++) {
      updatedExpression = updateExpressionIndices(conditions.get(j), indicesToRmv);
      updatedExpression = updatedOuterConditionStart + updatedExpression + updatedOuterConditionEnd;
      choice.replaceExpression(j - skipedOuterConditions, updatedExpression);
    }
  }
  
  

  public static String unescapeQueryExpression(String expression) {
    return expression.replaceAll("(?<!\\\\)\\\\\"", "\\\"");
    
  }
  
  public static String escapeQueryExpression(String expression) {
   return expression.replaceAll("(?<!\\\\)\\\"", "\\\\\"");
  }

  
  public static List<Integer> calculateVariableIndicesToRemove(List<String> filterConditions, StepMapping stepMapping){
    List<Integer> variableIndicesToRemove = new ArrayList<Integer>(); //ordered
    
    List<String> copy = new ArrayList<String>();
    
    if (stepMapping.isConditionMapping() && filterConditions != null) {
      for (int i = 0; i < filterConditions.size(); i++) {
        String copyCondition = unescapeQueryExpression(filterConditions.get(i));
        copy.add(copyCondition);
      }
    } else if (filterConditions == null) {
      copy = null;
    }
    
    
    List<List<XFLLexem>> itemsPerConfition = lexFilterConditions(copy);
    //filterConditions is the list form stepFunction
    for(int i=1; i<stepMapping.getInputVars().size()+1; i++) {
      if(!variableStillRelevant(i, itemsPerConfition)) {
        variableIndicesToRemove.add(i);
      }
    }
    return variableIndicesToRemove;
  }
  
  public static List<Integer> calculateVariableIndicesToRemove(StepChoice choice){
    List<String> expressions = readExpressions(choice);
    List<List<XFLLexem>> itemsPerCondition = lexFilterConditions(expressions);
    List<Integer> variableIndicesToRemove = new ArrayList<Integer>(); //ordered

    //find variable indices to remove
    for(int i=0; i<choice.getInputVars().size(); i++) {
      if(!variableStillRelevant(i, itemsPerCondition)) {
        variableIndicesToRemove.add(i);
      }
    }

    return variableIndicesToRemove;
  }
  
  public static void cleanUpChoiceInputsAndConditions(StepChoice choice) {
    List<String> expressions = readExpressions(choice);
    List<List<XFLLexem>> itemsPerCondition = lexFilterConditions(expressions);
    List<Integer> variableIndicesToRemove = calculateVariableIndicesToRemove(choice);
    
    if(variableIndicesToRemove.size() == 0) {
      return; //quick exit: nothing to do
    }
    
    //remove from back to front
    for (int i = variableIndicesToRemove.size() - 1; i >= 0; i--) {
      int indexToRemove = variableIndicesToRemove.get(i);
      //remove inputs from choice
      choice.getInputConnections().removeInputConnection(indexToRemove);
      choice.getInputVars().remove(indexToRemove);
    }

    lowerIndices(choice, itemsPerCondition, variableIndicesToRemove);
  }
}
