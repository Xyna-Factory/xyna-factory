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
package com.gip.xyna.persistence.xsor;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xnwh.persistence.memory.sqlparsing.Condition;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.Operator;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ParsedQuery;



abstract class ConditionTreeNode {
  
  abstract void parse(Condition subPart, AtomicInteger paramMarker);


  abstract List<List<ConditionTreeLeafNode>> convertToLeafNodesInDNF();


  public static ConditionTreeNode parseIntoTree(ParsedQuery parsedQuery) {
    boolean noSubConditions = parsedQuery.getWhereClause().getConditions().isEmpty();
    ConditionTreeNode root = noSubConditions ? new ConditionTreeLeafNode() : new ConditionTreeIntermidiateNode();
    root.parse(parsedQuery.getWhereClause(), new AtomicInteger(0));

    return root;
  }

  private static boolean allOperatorsAreEqual(List<Operator> operators) {
    for (int i = 0; i < operators.size(); i++) {
      if (!operators.get(i).toJava().equals(operators.get(i).toJava())) {
        return false;
      }
    }
    return true;
  }


  static class ConditionTreeIntermidiateNode extends ConditionTreeNode {

    private String comparision;
    private List<ConditionTreeNode> children = new ArrayList<ConditionTreeNode>();

    List<ConditionTreeNode> getChildren() {
      return children;
    }

    @Override
    void parse(Condition condition, AtomicInteger paramMarker) {
      if (condition.getOperators().size() > 0) {
        this.comparision = condition.getOperators().get(0).toJava();
      } else {
        // root node
        this.comparision = "&&";
      }
      List<Condition> subConditions = condition.getConditions();
      for (Condition subCondition : subConditions) {
        ConditionTreeNode child;
        if (subCondition.getConditions().size() > 1) {
          if (!allOperatorsAreEqual(subCondition.getOperators())) {
            throw new RuntimeException("Currently not supported");
          }
          child = new ConditionTreeIntermidiateNode();
        } else {
          child = new ConditionTreeLeafNode();
        }
        children.add(child);
        child.parse(subCondition, paramMarker);
      }
    }


    @Override
    List<List<ConditionTreeLeafNode>> convertToLeafNodesInDNF() {
      if (comparision.equals("||")) {
        List<List<ConditionTreeLeafNode>> childResults = new ArrayList<List<ConditionTreeLeafNode>>();
        for (ConditionTreeNode child : children) {
          childResults.addAll(child.convertToLeafNodesInDNF());
        }
        return childResults;
      } else {
        List<List<ConditionTreeLeafNode>> endResult = new ArrayList<List<ConditionTreeLeafNode>>();
        List<List<List<ConditionTreeLeafNode>>> childResults = new ArrayList<List<List<ConditionTreeLeafNode>>>();
        int globalSize = 0;
        int[] combinationIndex = new int[children.size()];
        int[] combinationSizes = new int[children.size()];
        for (int i = 0; i < children.size(); i++) {
          List<List<ConditionTreeLeafNode>> childResult = children.get(i).convertToLeafNodesInDNF();
          if (i == 0) {
            globalSize = childResult.size();
          } else {
            globalSize *= childResult.size();
          }
          combinationSizes[i] = childResult.size();
          combinationIndex[i] = 0;
          childResults.add(childResult);
        }
        for (int iteration = 0; iteration < globalSize; iteration++) {
          List<ConditionTreeLeafNode> currentResult = new ArrayList<ConditionTreeLeafNode>();
          for (int combination = 0; combination < combinationIndex.length; combination++) {
            currentResult.addAll(childResults.get(combination).get(combinationIndex[combination]));
          }
          for (int combination = combinationIndex.length - 1; combination >= 0; combination--) {
            if (combinationIndex[combination] + 1 >= combinationSizes[combination]) {
              combinationIndex[combination] = 0;
            } else {
              combinationIndex[combination]++;
              break;
            }
          }
          endResult.add(currentResult);
        }
        return endResult;
      }
    }


  }

  static class ConditionTreeLeafNode extends ConditionTreeNode {

    String colName;
    String comparision;
    int paramMapping;
    String fixedValue = null;


    @Override
    void parse(Condition condition, AtomicInteger paramMarker) {
      if (condition.getExpr1() != null) {
        colName = condition.getExpr1();
        comparision = condition.getConditionOperator().getSql();
        paramMapping = paramMarker.getAndIncrement();
        String expr2 = condition.getExpr2();
        if (!expr2.equals("?")) {
          fixedValue = expr2;
        }
      }
    }


    @Override
    List<List<ConditionTreeLeafNode>> convertToLeafNodesInDNF() {
      return Collections.singletonList(Collections.singletonList(this));
    }

  }
}
