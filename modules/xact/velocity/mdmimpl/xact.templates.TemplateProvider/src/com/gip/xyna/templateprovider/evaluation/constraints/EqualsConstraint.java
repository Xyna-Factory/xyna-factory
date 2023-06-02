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
package com.gip.xyna.templateprovider.evaluation.constraints;

/**
 * Checks if value in input data equals given string.
 */
public final class EqualsConstraint extends AbstractConstraint {

    public static final String COMPARISON_METHOD = "equals";

    public EqualsConstraint(final String key, final String comparisonValue) {
        super(key, comparisonValue);
    }

    protected boolean isMatch(final String value) {
        return value.equals(this.getComparisonValue());
    }

    public String getComparisonMethod() {
        return COMPARISON_METHOD;
    }
}
