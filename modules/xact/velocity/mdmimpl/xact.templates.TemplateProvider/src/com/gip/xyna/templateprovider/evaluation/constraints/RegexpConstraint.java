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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regular-expression constraint.
 */
public final class RegexpConstraint extends AbstractConstraint {

    public static final String COMPARISON_METHOD = "regexp";

    private final Pattern pattern;

    public RegexpConstraint(final String key, final String comparisonValue) {
        super(key, comparisonValue);
        try {
            pattern = Pattern.compile(comparisonValue);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Malformed regular expression: <" + comparisonValue + ">.", e);
        }
    }

    protected boolean isMatch(final String value) {
        return pattern.matcher(value).matches();
    }

    public String getComparisonMethod() {
        return COMPARISON_METHOD;
    }
}
