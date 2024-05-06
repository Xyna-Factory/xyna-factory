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
package xmcp.xypilot.impl.gen.model;

import xmcp.xypilot.impl.gen.util.GeneratorUtils;

/**
 * Data models are accessed in prompt templates to fill in dynamic data, e.g. the class name of the data type.
 * The base model provides access to the GeneratorUtils containing useful functions for the prompt generation.
 * More specific models can be derived from this base model to provide additional data.
 */
public class BaseModel {

    private GeneratorUtils utils;

    public BaseModel() {
        this.utils = new GeneratorUtils();
    }

    public GeneratorUtils getUtils() {
        return utils;
    }

}
