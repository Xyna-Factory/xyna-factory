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
package com.gip.xyna.templateprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Generates different templates depending on type and input data.
 */
public final class TypeAwareTemplateGenerator {

    private final Map<String, TemplateGenerator> templateGenerators;

    public TypeAwareTemplateGenerator(final List<TemplatePart> templateParts) {
        if (templateParts == null) {
            throw new IllegalArgumentException("Template parts may not be null.");
        }
        this.templateGenerators = createGeneratorsMap(templateParts);
    }

    private static Map<String, TemplateGenerator> createGeneratorsMap(final List<TemplatePart> parts) {
        Map<String, List<TemplatePart>> lookupTable = new Hashtable<String, List<TemplatePart>>();
        for (TemplatePart part : parts) {
            if (part == null) {
                throw new IllegalArgumentException("Template parts contains null.");
            }
            if (!lookupTable.containsKey(part.getTemplateType())) {
                lookupTable.put(part.getTemplateType(), new ArrayList<TemplatePart>());
            }
            lookupTable.get(part.getTemplateType()).add(part);
            // TODO performance: do something like this:
//            List<TemplatePart> listOfParts = lookupTable.get(part.getTemplateType());
//            if (listOfParts == null) {
//              listOfParts = new ArrayList<TemplatePart>();
//              lookupTable.put(part.getTemplateType(), listOfParts);
//            }
//            listOfParts.add(part);
        }
        Map<String, TemplateGenerator> result = new HashMap<String, TemplateGenerator>();
        for (Map.Entry<String, List<TemplatePart>> entry : lookupTable.entrySet()) {
            try {
                result.put(entry.getKey(), new TemplateGenerator(entry.getValue()));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Failed to create template generator for template type <"
                        + entry.getKey() + ">.", e);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public String generateTemplate(final String templateType, final InputData inputData)
            throws GenerationFailureException, UnknownTemplateTypeException {
        if (templateType == null) {
            throw new IllegalArgumentException("Template type may not be null.");
        } else if (inputData == null) {
            throw new IllegalArgumentException("Input data may not be null.");
        }
        TemplateGenerator generator = templateGenerators.get(templateType);
        if (generator == null) {
            throw new UnknownTemplateTypeException("No template parts defined for template type: <" + templateType + ">.");
        }
        try {
            return generator.generateTemplate(inputData);
        } catch (Exception e) {
            throw new GenerationFailureException("Failed to generate template of template type <" + templateType
                    + ">.", e);
        }
    }
}
