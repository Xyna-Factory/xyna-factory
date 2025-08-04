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



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import core.exception.ExceptionService;
import xfmg.tmf.validation.ConstraintV2;
import xfmg.tmf.validation.ConstraintValidationResultV2;



public class DependencyGraphExecutor {

  public List<ConstraintValidationResultV2> execute(List<? extends ConstraintV2> constraints, Predicate<ConstraintV2> condition,
                                                    Predicate<ConstraintV2> rule) {
    List<ConstraintValidationResultV2> results = new ArrayList<>();

    Set<String> allConstraints = new HashSet<>();
    Set<ConstraintV2> waitingForDependency = new HashSet<>();
    for (ConstraintV2 c : constraints) {
      if (!allConstraints.add(c.getName())) {
        throw new RuntimeException("There are multiple Constraints with the same name <" + c.getName() + ">.");
      }
      waitingForDependency.add(c);
    }
    //validate
    for (ConstraintV2 c : constraints) {
      for (String dep : c.getDependencies()) {
        if (!allConstraints.contains(dep)) {
          throw new RuntimeException("Constraint <" + c.getName() + "> has a dependency on an unknown Constraint with name <" + dep + ">.");
        }
      }
    }

    Set<String> success = new HashSet<>();
    Set<String> failedOrDependentOnFailed = new HashSet<>();
    while (waitingForDependency.size() > 0) {
      boolean progress = false;
      Iterator<ConstraintV2> it = waitingForDependency.iterator();
      while (it.hasNext()) {
        ConstraintV2 c = it.next();
        List<String> deps = c.getDependencies();
        if (success.containsAll(deps)) {
          try {
            if (condition.test(c)) {
              if (rule.test(c)) {
                success.add(c.getName());
              } else {
                results.add(result(c, "RULE"));
                failedOrDependentOnFailed.add(c.getName());
              }
            } else {
              results.add(result(c, "CONDITION"));
              failedOrDependentOnFailed.add(c.getName());
            }
          } catch (RuntimeException e) {
            results.add(result(c, "ERROR", e));
            failedOrDependentOnFailed.add(c.getName());
          }
          it.remove();
          progress = true;
        } else if (!Collections.disjoint(failedOrDependentOnFailed, deps)) {
          //deps contain at least on failed (or dependentOnFailed) constraint
          failedOrDependentOnFailed.add(c.getName());
          results.add(result(c, "SKIPPED"));
          it.remove();
          progress = true;
        } else {
          //check again later
        }
      }

      if (!progress) {
        //circular dependencies
        throw new RuntimeException("The Constraint dependencies are circular: "
            + waitingForDependency.stream().map(c -> c.getName()).collect(Collectors.toList()));
      }
    }

    return results;
  }


  private ConstraintValidationResultV2 result(ConstraintV2 c, String failureType) {
    return result(c, failureType, null);
  }


  private ConstraintValidationResultV2 result(ConstraintV2 c, String failureType, RuntimeException e) {
    ConstraintValidationResultV2.Builder b = new ConstraintValidationResultV2.Builder()
        .condition(c.getCondition())
        .rule(c.getRule())
        .name(c.getName())
        .description(c.getDescription())
        .dependencies(c.getDependencies())
        .failureType(failureType);
    if (e != null) {
      b.exceptionDetails(ExceptionService.getExceptionDetails(e));
    }
    return b.instance();
  }


}
