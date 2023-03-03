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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.templateprovider.util.StringToMapUtil;

/**
 * Constraints parser.
 */
public final class ConstraintsParser {

    private ConstraintsParser() {
    }

    public static Set<Constraint> parseToSetOfConstraints(final String value) {
        if (value == null) {
            //throw new IllegalArgumentException("Value may not be null.");
          return Collections.EMPTY_SET;
        }
        Set<Constraint> constraints = new HashSet<Constraint>();
        Map<String,Set<String>> map = StringToMapUtil.toMapOfSets(value);
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            for (String comparisonMethodAndValue : entry.getValue()) {
                String[] parts = comparisonMethodAndValue.split(":", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Key <" + key
                            + "> has an invalid comparison method and value value: <" + comparisonMethodAndValue
                            + ">");
                }
                String comparisonMethod = parts[0];
                String comparisonValue = parts[1];
                constraints.add(ConstraintFactory.create(key, comparisonMethod, comparisonValue));
            }
        }
        return constraints;
    }

    public static String parseToString(final Set<Constraint> constraints) {
        if (constraints == null) {
            throw new IllegalArgumentException("Constraints may not be null.");
        }
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        for (Constraint constraint : constraints) {
            if (constraint == null) {
                throw new IllegalArgumentException("Found null constraint in: <" + constraints + ">.");
            }
            String key = constraint.getKey();
            if (!map.containsKey(key)) {
                map.put(key, new HashSet<String>());
            }
            StringBuilder value = new StringBuilder();
            value.append(constraint.getComparisonMethod());
            value.append(":");
            value.append(constraint.getComparisonValue());
            map.get(key).add(value.toString());
        }
        return StringToMapUtil.toString(map);
    }
}
