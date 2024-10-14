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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates templates based on input data. Template type is ignored.
 */
public final class TemplateGenerator {

    public static final String MAIN_TEMPLATE_PART_NAME = "Main";
    public static final String SUB_TEMPLATE_PART_NAME_PREFIX = "macro";

    private final Map<String, TemplatePartSelector> templatePartLookup;

    public TemplateGenerator(final List<TemplatePart> templateParts) {
        if (templateParts == null) {
            throw new IllegalArgumentException("Template parts may not be null.");
        }
        this.templatePartLookup = createLookupTable(templateParts);        
    }

    private static Map<String, TemplatePartSelector> createLookupTable(final List<TemplatePart> templateParts) {
        Map<String, List<TemplatePart>> lookup = new HashMap<String, List<TemplatePart>>();
        for (TemplatePart part : templateParts) {
            if (part == null) {
                throw new IllegalArgumentException("Template parts may not contain null.");
            } else if (!lookup.containsKey(part.getPartName())) {
                lookup.put(part.getPartName(), new ArrayList<TemplatePart>());
            }
            lookup.get(part.getPartName()).add(part);
        }
        if (!lookup.containsKey(MAIN_TEMPLATE_PART_NAME)) {
            throw new IllegalArgumentException("No <" + MAIN_TEMPLATE_PART_NAME + "> template defined.");
        }
        Map<String, TemplatePartSelector> result = new HashMap<String, TemplatePartSelector>();
        for (Map.Entry<String, List<TemplatePart>> entry : lookup.entrySet()) {
            for (TemplatePart part : entry.getValue()) {
                for (String partName : part.getReferencedParts()) {
                    if (!lookup.containsKey(partName)) {
                        throw new IllegalArgumentException("No template part with name <" + partName
                                + "> found, but referenced from: <" + part + ">.");
                    }
                }
            }
            result.put(entry.getKey(), new TemplatePartSelector(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    public String generateTemplate(final InputData inputData) throws GenerationFailureException {
        TemplatePart mainTemplate;
        try {
            mainTemplate = this.templatePartLookup.get(MAIN_TEMPLATE_PART_NAME).getBestMatch(inputData);
        } catch (SelectionFailureException e) {
            throw new GenerationFailureException("No matching <" + MAIN_TEMPLATE_PART_NAME + "> template part found.", e);
        }
        StringBuilder sb = new StringBuilder();
        Set<String> includedTemplates = new HashSet<String>();
        addSubTemplates(mainTemplate, inputData, sb, includedTemplates);
        sb.append(mainTemplate.getContent());
        return sb.toString();
    }

    private void addSubTemplates(final TemplatePart templatePart, final InputData inputData,
            final StringBuilder sb, final Set<String> includedTemplates) throws GenerationFailureException {
        for (String templateName : templatePart.getReferencedParts()) {
            if (!includedTemplates.contains(templateName)) {
                TemplatePart subTemplate;
                try {
                    subTemplate = this.templatePartLookup.get(templateName).getBestMatch(inputData);
                } catch (SelectionFailureException e) {
                    throw new GenerationFailureException("Found no template part with name <" + templateName
                            + "> that matches input data.", e);                    
                }
                includedTemplates.add(templateName);
                // TODO externalize strings '#macro' and '#end'
                sb.append("#macro( " + SUB_TEMPLATE_PART_NAME_PREFIX + templateName + " )\n");
                sb.append(subTemplate.getContent());
                sb.append("#end\n");
                addSubTemplates(subTemplate, inputData, sb, includedTemplates);
            }
        }
    }
}
