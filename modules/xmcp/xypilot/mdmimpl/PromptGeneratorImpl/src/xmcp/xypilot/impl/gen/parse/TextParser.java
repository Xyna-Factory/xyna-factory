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
package xmcp.xypilot.impl.gen.parse;

import base.Text;
import xmcp.xypilot.impl.gen.model.BaseModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;

/**
 * Parses the completion returned by the ai server to a text.
 * Removes trailing whitespaces.
 */
public class TextParser implements Parser<Text, BaseModel> {

    @Override
    public Text parse(String input, BaseModel dataModel) {
        return new Text(input.stripTrailing());
    }
}
