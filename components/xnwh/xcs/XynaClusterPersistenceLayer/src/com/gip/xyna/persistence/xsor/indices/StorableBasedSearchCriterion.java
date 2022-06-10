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
package com.gip.xyna.persistence.xsor.indices;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.ComparisionAlgorithm;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableBasedSearchCriterion extends SearchCriterion {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(StorableBasedSearchCriterion.class);
  
  private boolean containsColumnCriterionEqualPrimaryKey;
 
  public StorableBasedSearchCriterion(List<ColumnCriterion> columns) {
    super(columns);
    containsColumnCriterionEqualPrimaryKey = false;
    for (ColumnCriterion columnCriterion : columns) {
      if (columnCriterion.hasPrimaryKeyInEqualComparison()) {
        containsColumnCriterionEqualPrimaryKey = true;
      }
    }
  }

  @Override
  public boolean fits(Object candidate, SearchParameter parameter) {
    if (!(candidate instanceof Storable)) {
      return false;
    }
    Storable castedCandidate = (Storable)candidate;
    for (ColumnCriterion criterion : this.getColumns()) {
      Object actualValue = castedCandidate.getValueByColString(criterion.getColumnName());
      int parameterIndex = criterion.getMappingToSearchParameter();
      if (parameterIndex > parameter.size()) {
        logger.warn("SearchCriterion did contain a mapping to a searchParameter that is beyond it's bounds.");
        return false;
      }
      SearchValue searchValue = parameter.getSearchValue(parameterIndex);
      if (!evaluateComparison(actualValue, searchValue.getValue(), criterion.getOperator(), searchValue.getComparisionAlgorithm())) {
        return false;
      }
    }
    return true;
  }
  
  
  private static boolean evaluateComparison(Object actualValue, Object expectedValue, SearchColumnOperator operator, ComparisionAlgorithm algorithm) {
    if (actualValue == null || expectedValue == null) {
      return false; //throw something in either cases?
    }
    switch (operator) {
      case EQUALS :
        return algorithm.areEqual(actualValue, expectedValue);
      case IN : 
        // we expect the expectedValue to actually be multiple values, how will they be wrapped, let's assume it's an array
        if (expectedValue instanceof Object[]) {
          for (int arrayIndex = 0; arrayIndex<((Object[])expectedValue).length; arrayIndex++) {
            if (evaluateComparison(actualValue, ((Object[])expectedValue)[arrayIndex], SearchColumnOperator.EQUALS, algorithm)) {
              return true;
            }
          }
        } else if (expectedValue.getClass().isArray()) { // this will catch Arrays of primitive types...and is assumed to be slower then the algorithm above 
          int arrayLength = Array.getLength(expectedValue);
          for (int arrayIndex = 0; arrayIndex<arrayLength; arrayIndex++) {
            Object singleExpectedValue = Array.get(expectedValue, arrayIndex);
            if (evaluateComparison(actualValue, singleExpectedValue, SearchColumnOperator.EQUALS, algorithm)) {
              return true;
            }
          }
          return false;
        } else { // TODO do we wan't to allow that? (with that=sending a unwrapped IN-Parameter if there is only one)
          return evaluateComparison(actualValue, expectedValue, SearchColumnOperator.EQUALS, algorithm);
        }
        return false;
      case LIKE :
        String sqlLike = expectedValue.toString();
        String likePattern = convertSqlLikeToRegExp(sqlLike);
        String actualValueAsString = actualValue.toString();
        return Pattern.matches(likePattern, actualValueAsString);
      case GREATER :
            return algorithm.compare(actualValue, expectedValue) > 0;
      case GREATER_EQUALS :
        return algorithm.compare(actualValue, expectedValue) >= 0; 
      case SMALLER :
        return algorithm.compare(actualValue, expectedValue) < 0;
      case SMALLER_EQUALS :
        return algorithm.compare(actualValue, expectedValue) <= 0;
      default :
        throw new UnsupportedOperationException("Unknown operation"); // FIXME other error
    }
  }
  
  
  private static String convertSqlLikeToRegExp(String sqlLike) {
    //replace . with \\.
    String pattern = sqlLike.replaceAll("\\.", "\\.");
    //replace % with .*
    pattern = pattern.replaceAll("%", ".*");
    // Start & End
    StringBuilder patternBuilder = new StringBuilder();
    patternBuilder.append("^").append(pattern).append("$");
    return patternBuilder.toString();
  }

  @Override
  public boolean containsPrimaryKeyInEqualComparision() {
    return containsColumnCriterionEqualPrimaryKey;
  }

}
