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

import com.gip.xyna.templateprovider.InputData;

/**
 * Evaluates constraints.
 */
public final class ConstraintsEvaluator {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 16777215;

    private final Set<Constraint> constraints;
    private final Set<Set<Constraint>> groupedConstraints; //gruppiert pro Constraint-Key
    private final int score;

    public ConstraintsEvaluator(final Set<Constraint> constraints, final int score) {
        if (constraints == null) {
            throw new IllegalArgumentException("Constraints may not be null.");
        } else if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("Invalid score value: <" + score + ">.");
        }
        Map<String, Set<Constraint>> constraintsMap = new HashMap<String, Set<Constraint>>();
        for (Constraint constraint : constraints) {
            if (constraint == null) {
                throw new IllegalArgumentException("Found null value in constraints set: <" + constraints + ">.");
            }
            if (!constraintsMap.containsKey(constraint.getKey())) {
                constraintsMap.put(constraint.getKey(), new HashSet<Constraint>());
            }
            Set<Constraint> constraintsSet = constraintsMap.get(constraint.getKey());
            constraintsSet.add(constraint);
        }
        Set<Constraint> allConstraints = new HashSet<Constraint>();
        Set<Set<Constraint>> constraintsList = new HashSet<Set<Constraint>>();
        for (Set<Constraint> constraintsSet : constraintsMap.values()) {
            allConstraints.addAll(constraintsSet);
            constraintsList.add(Collections.unmodifiableSet(constraintsSet));
        }
        this.constraints = Collections.unmodifiableSet(allConstraints);
        this.groupedConstraints = Collections.unmodifiableSet(constraintsList);
        this.score = score;
    }

    public Set<Constraint> getConstraints() {
        return this.constraints;
    }

    public int getScore() {
        return this.score;
    }

    /**
     * gibt score zurück, falls alle gruppen mindestens eine zutreffende constraint haben.
     * D.h. die Gruppen werden ge-UND-et, die Gruppenmitglieder ge-ODER-t.
     * @return
     */
    public int evaluateMatchedConstraints(final InputData inputData) {
        for (Set<Constraint> constraintsSet : groupedConstraints) {
            boolean noMatch = true;
            for (Constraint constraint : constraintsSet) {
                if (constraint.isMatch(inputData)) {
                    noMatch = false;
                    break;
                }
            }
            if (noMatch) {
                return -1;
            }
        }
        return score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConstraintsEvaluator:{constraints:<");
        sb.append(this.constraints);
        sb.append(">,score:<");
        sb.append(this.score);
        sb.append(">}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + constraints.hashCode();
        result = prime * result + groupedConstraints.hashCode();
        result = prime * result + score;
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
        ConstraintsEvaluator other = (ConstraintsEvaluator) obj;
        if (!constraints.equals(other.constraints)) {
            return false;
        }
        if (!groupedConstraints.equals(other.groupedConstraints)) {
            return false;
        }
        if (score != other.score) {
            return false;
        }
        return true;
    }
}
