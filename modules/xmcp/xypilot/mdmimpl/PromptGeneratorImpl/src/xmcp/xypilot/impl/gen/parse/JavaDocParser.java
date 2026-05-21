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

import java.util.stream.Collectors;

import xmcp.xypilot.Documentation;
import xmcp.xypilot.impl.gen.model.DomOrExceptionModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;

/**
 * Parses the completion returned by the ai server to a suitable documentation string.
 * It removes JavaDoc prefixes and unwanted tags.
 */
public class JavaDocParser implements Parser<Documentation, DomOrExceptionModel> {

    @Override
    public Documentation parse(String documentation, DomOrExceptionModel dataModel) {
        documentation = cleanUpDocumentation(documentation).stripTrailing();
        // trim leading whitespace from generated documentation if the existing documentation is empty, i.e. it is fully generated
        if (dataModel.getDomOrException().getDocumentation().isBlank()) {
            documentation = documentation.stripLeading();
        }
        return new Documentation(documentation);
    }

    /**
     * removes JavaDoc prefixes, as well as generics around "Container" and also
     * lines containing "@author"
     *
     * @param documentation
     * @return
     */
    public static String cleanUpDocumentation(String documentation) {
        return documentation.lines()
            .map((line) -> {
                return line
                        .replaceAll("^\\s*?//\\s?", "")
                        .replaceAll("^\\s*?\\*\\s?", "")
                        .replace("<p>", "")
                        .replaceAll("</?[u|o]l>", "")
                        .replace("<li>", " - ")
                        .replaceAll("Container<.*?>(\\(?)", "Container$1");
            })
            .filter((line) -> !line.contains("@author"))
            .filter((line) -> !line.contains("@version"))
            .filter((line) -> !line.contains("@since"))
            .filter((line) -> !line.contains("@date"))
            .filter((line) -> !line.contains("@generated"))
            .filter((line) -> !line.startsWith("Author:"))
            .filter((line) -> !line.startsWith("Date:"))
            .collect(Collectors.joining("\n"));
    }
}
