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

import java.util.List;

import base.Text;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;
import xmcp.xypilot.impl.gen.util.StringUtils;

/**
 * Parses the completion returned by the ai server to a suitable mapping label.
 * It only keeps the first line and removes some common prefixes, caused by the way the prompt is built.
 */
public class MappingLabelParser implements Parser<Text, MappingModel> {

    private static final List<String> PREFIXES_TO_REMOVE = List.of(
        "it",
        "the code",
        "the code above",
        "the code does the following"
    );

    @Override
    public Text parse(String input, MappingModel dataModel) {
        // only keep the first line (if any)
        input = input.lines().findFirst().orElse("");

        // remove unwanted prefixes
        if (dataModel.getMapping().getLabel().isBlank()) {
            for (String prefix : PREFIXES_TO_REMOVE) {
                if (input.toLowerCase().startsWith(prefix)) {
                    input = input.substring(prefix.length());
                    input = input.stripLeading();
                    break;
                }
            }
        }
        return new Text(
            StringUtils.capitalize(input.stripTrailing())
        );
    }
}
