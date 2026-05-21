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
package xmcp.xypilot.impl.gen.template.preprocess;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateDirectiveModel;

public interface TemplatePreprocessor {

    public Reader process(Reader reader) throws IOException;

    public default Map<String, Class<? extends TemplateDirectiveModel>> requiredDirectives() {
        return Map.of();
    }

    public default List<Class<? extends TemplatePreprocessor>> dependencies() {
        return List.of();
    }
}
