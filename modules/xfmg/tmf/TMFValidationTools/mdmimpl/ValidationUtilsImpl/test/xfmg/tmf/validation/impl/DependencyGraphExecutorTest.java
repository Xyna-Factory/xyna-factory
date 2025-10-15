/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl;



import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import xfmg.tmf.validation.ConstraintV2;
import xfmg.tmf.validation.ConstraintValidationResultV2;
import xfmg.tmf.validation.JSONPathExpressionV2;



public class DependencyGraphExecutorTest {

  @Test
  public void testDependencyOK() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "nok", "");
    addConstraint(constraints, "2", "", "ok", "ok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "1", "CONDITION");
    checkNotContains(results, "2");
  }


  @Test
  public void testDependencySkipNOK() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "ok", "nok");
    addConstraint(constraints, "2", "", "nok", "ok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "2", "CONDITION");
    checkContains(results, "1", "SKIPPED");
  }


  @Test
  public void testDependencyRuleFailed() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "nok", "ok");
    addConstraint(constraints, "2", "", "ok", "nok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "2", "RULE");
    checkContains(results, "1", "SKIPPED");
  }
  

  @Test
  public void testDependencySkipAfterRuleFailure() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "ok", "ok");
    addConstraint(constraints, "2", "", "ok", "nok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "2", "RULE");
    checkContains(results, "1", "SKIPPED");
  }


  @Test
  public void testCyclicDep() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "ok", "ok");
    addConstraint(constraints, "2", "1", "ok", "ok");
    addConstraint(constraints, "3", "", "ok", "ok");
    try {
      List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
          .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
      fail("Expected cyclic dependency detected");
    } catch (RuntimeException e) {
      //ok
    }
  }
  
  @Test
  public void testCyclicDep2() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "2", "ok", "ok");
    addConstraint(constraints, "2", "3", "ok", "ok");
    addConstraint(constraints, "4", "", "ok", "ok");
    addConstraint(constraints, "3", "1", "ok", "ok");
    try {
      List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
          .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
      fail("Expected cyclic dependency detected");
    } catch (RuntimeException e) {
      //ok
    }
  }

  @Test
  public void testInvalidDependency() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "4", "ok", "ok");
    addConstraint(constraints, "2", "1", "ok", "ok");
    try {
      List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
          .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
      fail("Expected invalid dependency detected");
    } catch (RuntimeException e) {
      //ok
    }
  }

  @Test
  public void testDuplicateConstraint() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "", "ok", "ok");
    addConstraint(constraints, "2", "", "ok", "ok");
    addConstraint(constraints, "1", "", "ok", "ok");
    addConstraint(constraints, "3", "", "ok", "ok");
    try {
      List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
          .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
      fail("Expected duplicate constraint detected");
    } catch (RuntimeException e) {
      //ok
    }
  }


  @Test
  public void testRootRuleNotOkPropagates() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    /*
     * 3 -> 2 -
     * |       \
     *  --> 4 ----> 1
     */
    addConstraint(constraints, "1", "2,3,4", "ok", "ok");
    addConstraint(constraints, "2", "3", "ok", "nok");
    addConstraint(constraints, "3", "", "ok", "nok");
    addConstraint(constraints, "4", "3", "ok", "ok");
    addConstraint(constraints, "5", "", "ok", "ok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "2", "SKIPPED");
    checkContains(results, "3", "RULE");
    checkContains(results, "1", "SKIPPED");
    checkContains(results, "4", "SKIPPED");
    checkNotContains(results, "5");
  }


  @Test
  public void testMultipleDependenciesSkippedPartially() {
    List<ConstraintV2> constraints = new ArrayList<ConstraintV2>();
    addConstraint(constraints, "1", "", "ok", "ok");
    addConstraint(constraints, "2", "", "nok", "ok");
    addConstraint(constraints, "3", "", "ok", "nok");
    addConstraint(constraints, "4", "1", "ok", "ok");
    addConstraint(constraints, "4a", "1", "ok", "nok");
    addConstraint(constraints, "4b", "1", "nok", "ok");
    addConstraint(constraints, "5", "2", "ok", "ok");
    addConstraint(constraints, "6", "3", "ok", "ok");
    addConstraint(constraints, "7", "1,2", "ok", "ok");
    addConstraint(constraints, "8", "1,3", "ok", "ok");
    addConstraint(constraints, "9", "2,3", "ok", "ok");
    addConstraint(constraints, "10", "4", "ok", "ok");
    addConstraint(constraints, "11", "4,1", "ok", "ok");
    addConstraint(constraints, "12", "4,2", "ok", "ok");
    addConstraint(constraints, "13", "4,3", "ok", "ok");
    addConstraint(constraints, "14", "4a", "ok", "ok");
    addConstraint(constraints, "15", "4b", "ok", "ok");
    List<ConstraintValidationResultV2> results = new DependencyGraphExecutor()
        .execute(constraints, c -> c.getCondition().getExpression().equals("ok"), c -> c.getRule().getExpression().equals("ok"));
    checkContains(results, "2", "CONDITION");
    checkContains(results, "3", "RULE");
    checkContains(results, "4a", "RULE");
    checkContains(results, "4b", "CONDITION");
    checkContains(results, "5", "SKIPPED");
    checkContains(results, "6", "SKIPPED");
    checkContains(results, "7", "SKIPPED");
    checkContains(results, "8", "SKIPPED");
    checkContains(results, "9", "SKIPPED");
    checkContains(results, "12", "SKIPPED");
    checkContains(results, "13", "SKIPPED");
    checkContains(results, "14", "SKIPPED");
    checkContains(results, "15", "SKIPPED");
    checkNotContains(results, "1");
    checkNotContains(results, "4");
    checkNotContains(results, "10");
    checkNotContains(results, "11");
  }

  private void checkNotContains(List<ConstraintValidationResultV2> results, String id) {
    if (results.stream().anyMatch(r -> r.getName().equals(id))) {
      fail("Expected constraint " + id + " to not fail");
    }
  }


  private void checkContains(List<ConstraintValidationResultV2> results, String id, String failureType) {
    if (!results.stream().anyMatch(r -> r.getName().equals(id) && r.getFailureType().equals(failureType))) {
      fail("Expected constraint " + id + " to fail with failureType " + failureType);
    }
  }


  private void addConstraint(List<ConstraintV2> constraints, String id, String deps, String condition, String rule) {
    List<String> dependencies = new ArrayList<String>();
    if (deps.length() > 0) {
      for (String s : deps.split(",", -1)) {
        dependencies.add(s);
      }
    }
    constraints.add(new ConstraintV2.Builder().name(id).dependencies(dependencies)
        .condition(new JSONPathExpressionV2.Builder().expression(condition).instance())
        .rule(new JSONPathExpressionV2.Builder().expression(rule).instance()).instance());
  }

}
