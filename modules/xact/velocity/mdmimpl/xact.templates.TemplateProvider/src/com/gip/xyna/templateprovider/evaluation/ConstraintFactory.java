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
package com.gip.xyna.templateprovider.evaluation;

import com.gip.xyna.templateprovider.evaluation.constraints.BeginsWithConstraint;
import com.gip.xyna.templateprovider.evaluation.constraints.EndsWithConstraint;
import com.gip.xyna.templateprovider.evaluation.constraints.EqualsConstraint;
import com.gip.xyna.templateprovider.evaluation.constraints.NullConstraint;
import com.gip.xyna.templateprovider.evaluation.constraints.RegexpConstraint;

/**
 * Constraint factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class ConstraintFactory {

    private ConstraintFactory() {
    }

    public static Constraint create(final String key, final String comparisonMethod, final String comparisonValue) {
        if (comparisonMethod.equals(BeginsWithConstraint.COMPARISON_METHOD)) {
            return new BeginsWithConstraint(key, comparisonValue);
        } else if (comparisonMethod.equals(EndsWithConstraint.COMPARISON_METHOD)) {
            return new EndsWithConstraint(key, comparisonValue);
        } else if (comparisonMethod.equals(EqualsConstraint.COMPARISON_METHOD)) {
            return new EqualsConstraint(key, comparisonValue);
        } else if (comparisonMethod.equals(NullConstraint.COMPARISON_METHOD)) {
            if (!comparisonValue.equals("")) {
                throw new IllegalArgumentException(
                        "Expected comparison value to be empty string for comparision method <"
                        + NullConstraint.COMPARISON_METHOD + ">, but was: <" + comparisonValue + ">.");
            }
            return new NullConstraint(key);
        } else if (comparisonMethod.equals(RegexpConstraint.COMPARISON_METHOD)) {
            return new RegexpConstraint(key, comparisonValue);
        }
        throw new IllegalArgumentException("Unknown comparison method: <" + comparisonMethod + ">.");
    }
}
