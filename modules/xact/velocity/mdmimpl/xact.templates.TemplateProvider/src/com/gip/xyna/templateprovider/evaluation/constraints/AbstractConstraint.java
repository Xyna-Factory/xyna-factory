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
package com.gip.xyna.templateprovider.evaluation.constraints;

import com.gip.xyna.templateprovider.InputData;
import com.gip.xyna.templateprovider.evaluation.Constraint;

/**
 * Abstract constraint.
 */
public abstract class AbstractConstraint implements Constraint {

    private final String key;
    private final String comparisonValue;

    public AbstractConstraint(final String key, final String comparisonValue) {
        if (key == null) {
            throw new IllegalArgumentException("Key may not be null.");
        } else if (comparisonValue == null) {
            throw new IllegalArgumentException("Comparison value may not be null.");
        }
        this.key = key;
        this.comparisonValue = comparisonValue;
    }

    public final String getKey() {
        return key;
    }

    public final String getComparisonValue() {
        return comparisonValue;
    }

    public final boolean isMatch(final InputData inputData) {
      String value = inputData.getValue(key);
      if (value == null) {
          return false;
      }
      return isMatch(value);
    }

    protected abstract boolean isMatch(final String value);

    @Override
    public final String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Constraint:{");
        sb.append(this.getKey());
        sb.append(":");
        sb.append(this.getComparisonMethod());
        sb.append(":");
        sb.append(this.getComparisonValue());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + comparisonValue.hashCode();
        result = prime * result + key.hashCode();
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractConstraint other = (AbstractConstraint) obj;
        if (!comparisonValue.equals(other.comparisonValue)) {
            return false;
        }
        if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }
}
