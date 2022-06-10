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
 * Checks that value in input data is null or not defined.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class NullConstraint implements Constraint {

    public static final String COMPARISON_METHOD = "null";

    private final String key;

    public NullConstraint(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key may not be null.");
        }
        this.key = key;
    }

    public final String getKey() {
        return key;
    }

    public final String getComparisonValue() {
        return "";
    }

    public final boolean isMatch(final InputData inputData) {
        String value = inputData.getValue(key);
        return value == null;
    }

    public String getComparisonMethod() {
        return COMPARISON_METHOD;
    }

    @Override
    public String toString() {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NullConstraint other = (NullConstraint) obj;
        if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }
}
