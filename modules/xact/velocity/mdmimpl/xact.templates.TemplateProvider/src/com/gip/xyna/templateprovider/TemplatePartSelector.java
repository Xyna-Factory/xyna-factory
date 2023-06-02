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
package com.gip.xyna.templateprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Template part selector.
 */
public final class TemplatePartSelector {

    private final List<TemplatePart> templateParts;

    public TemplatePartSelector(final List<TemplatePart> templateParts) {
        if (templateParts == null) {
            throw new IllegalArgumentException("Template parts set may not be null.");
        } else if (templateParts.size() < 1) {
            throw new IllegalArgumentException("Template parts set may not be empty.");
        }
        List<TemplatePart> parts = new ArrayList<TemplatePart>();
        for (TemplatePart part : templateParts) {
            if (part == null) {
                throw new IllegalArgumentException("Null value found in template part list: <" + templateParts + ">");
            }
            parts.add(part);
        }
        this.templateParts = Collections.unmodifiableList(parts);
    }

    public TemplatePart getBestMatch(final InputData inputData) throws SelectionFailureException {
        List<TemplatePart> bestMatches = new ArrayList<TemplatePart>();
        int currentScore = -1;
        for (TemplatePart part : templateParts) {
            int score = part.getConstraintsEvaluator().evaluateMatchedConstraints(inputData);
            if (score > -1 && score >= currentScore) {
                if (bestMatches.size() == 0) {
                    bestMatches.add(part);
                    currentScore = score;
                } else if (currentScore == score) {
                    bestMatches.add(part);
                } else  {
                    bestMatches = new ArrayList<TemplatePart>();
                    bestMatches.add(part);
                    currentScore = score;
                }
            }
        }
        if (bestMatches.size() > 1) {
            throw new SelectionFailureException("Found more than one match with score <" + currentScore
                    + "> for inputData: <" + inputData + ">.");
        } else if (bestMatches.size() < 1) {
            throw new SelectionFailureException("Found no matches for: <" + inputData + ">.");
        }
        return bestMatches.get(0);
    }
}
