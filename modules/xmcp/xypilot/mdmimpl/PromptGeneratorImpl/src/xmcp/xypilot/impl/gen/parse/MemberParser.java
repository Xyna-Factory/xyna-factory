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

import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;

import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.impl.gen.model.DomOrExceptionModel;
import xmcp.xypilot.impl.gen.parse.json.JsonLineParser;
import xmcp.xypilot.impl.gen.parse.json.MemberJsonHandler;
import xmcp.xypilot.impl.gen.pipeline.Parser;

/**
 * Parses the completion returned by the ai server to a list of member variables.
 * The completion is a (possibly invalid) json string, which is parsed line by line.
 */
public class MemberParser implements Parser<List<MemberVariable>, DomOrExceptionModel> {

    @Override
    public List<MemberVariable> parse(String input, DomOrExceptionModel dataModel) {
        return parseMembers(input, dataModel.getDomOrException(), dataModel.getAvailableVariableTypes());
    }

    /**
     * Parses the members described by the json string. If a member can't be parsed
     * properly, it is skipped.
     * -> worst case an empty list is returned
     *
     * @param json
     * @param dom
     * @param availableTypes
     * @return
     */
    public static List<MemberVariable> parseMembers(String json, DomOrExceptionGenerationBase dom, List<String> availableTypes) {
        MemberJsonHandler handler = new MemberJsonHandler(dom, availableTypes);
        JsonLineParser.parse(json, handler);
        return handler.getMembers();
    }
}
