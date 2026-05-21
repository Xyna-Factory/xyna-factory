/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.gen.template;

import java.util.Arrays;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class DirectiveParameters {

    private Map<String, TemplateModel> parameters;


    public static class Def<T extends TemplateModel> {
        public String name;
        public Class<T> type;
        public boolean required;

        public Def(String name, Class<T> type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }
    }


    public DirectiveParameters(Map<String, TemplateModel> parameters) {
        this.parameters = parameters;
    }


    public void validate(Def<?> ...paramDefs) throws TemplateModelException {
        // check required and type
        for (Def<?> paramDef : paramDefs) {
            // check required
            if (paramDef.required && !parameters.containsKey(paramDef.name)) {
                throw new TemplateModelException("Missing required parameter '" + paramDef.name + "'");
            }
            // check type
            if (parameters.containsKey(paramDef.name)) {
                TemplateModel value = parameters.get(paramDef.name);
                if (!paramDef.type.isInstance(value)) {
                    throw new TemplateModelException("The \"" + paramDef.name + "\" parameter must be a " + paramDef.type.getSimpleName());
                }
            }
        }
        // check for unsupported parameters
        for (String name : parameters.keySet()) {
            Arrays.asList(paramDefs).stream()
                .filter(def -> def.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new TemplateModelException("Unsupported parameter '" + name + "'"));
        }
    }


    @SuppressWarnings("unchecked")
    public <T extends TemplateModel> T get(String name) {
        return (T) parameters.get(name);
    }


    @SuppressWarnings("unchecked")
    public <T extends TemplateModel> T get(Def<T> paramDef) {
        return (T) parameters.get(paramDef.name);
    }

    @SuppressWarnings("unchecked")
    public <T extends TemplateModel> T getOrDefault(String name, T defaultValue) {
        return (T) parameters.getOrDefault(name, defaultValue);
    }


    @SuppressWarnings("unchecked")
    public <T extends TemplateModel> T getOrDefault(Def<T> paramDef, T defaultValue) {
        return (T) parameters.getOrDefault(paramDef.name, defaultValue);
    }
}
