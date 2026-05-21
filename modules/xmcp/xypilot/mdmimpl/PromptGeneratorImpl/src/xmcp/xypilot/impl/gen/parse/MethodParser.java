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

import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.parse.json.JsonLineParser;
import xmcp.xypilot.impl.gen.parse.json.MethodJsonHandler;
import xmcp.xypilot.impl.gen.pipeline.Parser;

/**
 * Parses the completion returned by the ai server to a list of method definitions.
 * The completion is a (possibly invalid) json string, which is parsed line by line.
 */
public class MethodParser implements Parser<List<MethodDefinition>, DomModel> {

    @Override
    public List<MethodDefinition> parse(String input, DomModel dataModel) {
        return parseMethods(input, dataModel.getDom(), dataModel.getAvailableParameterTypes(), dataModel.getAvailableExceptionTypes());
    }

    /**
     * Parses the methods described by the json string. If a method can't be parsed
     * properly, it is skipped.
     * -> worst case an empty list is returned
     *
     * @param json
     * @param dom
     * @param availableParameterTypes
     * @param availableExceptionTypes
     * @return
     */
    public static List<MethodDefinition> parseMethods(String json, DOM dom, List<String> availableParameterTypes, List<String> availableExceptionTypes) {
        MethodJsonHandler handler = new MethodJsonHandler(dom, availableParameterTypes, availableExceptionTypes);
        JsonLineParser.parse(json, handler);
        return handler.getMethods();
    }
}
