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
package xmcp.xypilot.impl.gen.parse.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;

import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.impl.gen.util.ParserUtils;
import xmcp.xypilot.impl.gen.util.TypeUtils;

public class MemberJsonHandler implements JsonLineHandler {
    // the primitive type to use if no other type matches
    private static final String DEFAULT_TYPE = "String";

    private MemberVariable member;
    private ArrayList<MemberVariable> members = new ArrayList<>();

    private DomOrExceptionGenerationBase dom;
    private Set<String> availableTypes;
    private Map<String, String> toFqn;

    // whether the parsed line does not belong to a member
    private boolean notAMember = false;

    public MemberJsonHandler(DomOrExceptionGenerationBase dom, List<String> availableTypes) {
        this.dom = dom;
        this.availableTypes = new HashSet<>(availableTypes);
        this.toFqn = TypeUtils.simpleToFqnMap(this.availableTypes);
    }


    public List<MemberVariable> getMembers() {
        return members;
    }


    @Override
    public void objectStart(Optional<String> key) {
        if (key.isEmpty()) {
            addMemberIfValid(); // add the previous member if not already done, e.g. if not closed with '}'
            createNextMember();
        }
    }


    @Override
    public void arrayStart(Optional<String> key) {
        // starts some other array, we are not interested in
        if (!key.isPresent() || !key.get().equals("members")) {
            notAMember = true;
        }
    }


    @Override
    public void arrayEnd() {
        notAMember = false;
    }


    @Override
    public void objectEnd() {
        addMemberIfValid();
    }


    @Override
    public void property(String key, String value) {
        if (member != null) {
            switch (key) {
                case "name":
                    member.setName(ParserUtils.getQuoted(value));
                    break;
                case "type":
                case "fqn":
                    String fqn = TypeUtils.getFqn(ParserUtils.getQuoted(value), toFqn);
                    if (TypeUtils.isPrimitiveType(fqn)) {
                        setPrimitiveType(fqn);
                    } else if (availableTypes.contains(fqn)) {
                        setReferenceType(fqn);
                    } else {
                        setPrimitiveType(DEFAULT_TYPE);
                    }
                    break;
                case "isList":
                    member.setIsList(Boolean.parseBoolean(value));
                    break;
                case "documentation":
                    String documentation = ParserUtils.getQuoted(value);
                    if (documentation != null) {
                        member.setDocumentation(ParserUtils.unescapeString(documentation));
                    }
                    break;
            }
        }
    }


    @Override
    public void endParsing() {
        addMemberIfValid(); // in case last member not closed
    }


    private void createNextMember() {
        member = new MemberVariable();
        setPrimitiveType(DEFAULT_TYPE);
    }


    private boolean hasEquivalentMember(MemberVariable member) {
        return members.stream().anyMatch((m) -> m.getName().equals(member.getName()))
            || dom.getMemberVars().stream().anyMatch((v) -> v.getVarName().equals(member.getName()));
    }


    private void addMemberIfValid() {
        // requires at least a name to be included in the list of members
        if (!notAMember && member != null && member.getName() != null && !member.getName().isEmpty() && !hasEquivalentMember(member)) {
            members.add(member);
        }
        member = null;
    }


    private void setReferenceType(String type) {
        member.setReferenceType(type);
        member.setPrimitiveType(null);
    }


    private void setPrimitiveType(String type) {
        member.setReferenceType(null);
        member.setPrimitiveType(type);
    }

}
