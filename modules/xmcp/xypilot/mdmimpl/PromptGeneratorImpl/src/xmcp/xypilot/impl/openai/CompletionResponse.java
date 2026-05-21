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
package xmcp.xypilot.impl.openai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type;
import com.gip.xyna.utils.misc.JsonParser.Position;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

public class CompletionResponse {
    public GenericResult res;

    public CompletionResponse(String resp) {
        GenericVisitor visitor = new GenericVisitor();
        JsonParser parser = new JsonParser(true);
        try {
            res = parser.parse(resp, visitor);
        } catch (InvalidJSONException | UnexpectedJSONContentException e) {
        }
    }

    public class GenericResult {

        private final Map<String, Pair<String, Type>> attributes;
        private final Map<String, Pair<List<String>, Type>> lists;
        private final Map<String, GenericResult> objects;
        private final Map<String, List<GenericResult>> objectLists;
        private final Set<String> emptyLists;

        public GenericResult(Map<String, Pair<String, Type>> attributes,
                Map<String, Pair<List<String>, Type>> lists,
                Map<String, GenericResult> objects,
                Map<String, List<GenericResult>> objectLists,
                Set<String> emptyLists) {
            this.attributes = attributes;
            this.lists = lists;
            this.objects = objects;
            this.objectLists = objectLists;
            this.emptyLists = emptyLists;
        }

        public Pair<String, Type> getAttribute(String label) {
            return attributes.get(label);
        }

        public Pair<List<String>, Type> getList(String label) {
            return lists.get(label);
        }

        public GenericResult getObject(String label) {
            return objects.get(label);
        }

        public List<GenericResult> getObjectList(String label) {
            return objectLists.get(label);
        }

        public Map<String, Pair<String, Type>> getAttributes() {
            return attributes;
        }

        public Map<String, Pair<List<String>, Type>> getLists() {
            return lists;
        }

        public Map<String, GenericResult> getObjects() {
            return objects;
        }

        public Map<String, List<GenericResult>> getObjectLists() {
            return objectLists;
        }

        public <T> T visit(JsonVisitor<T> visitor) throws UnexpectedJSONContentException {
            return visit(visitor, Collections.<String>emptyList());
        }

        public <T> T visit(JsonVisitor<T> visitor, List<String> visitationPrecedence)
                throws UnexpectedJSONContentException {
            visitByPrecedence(visitor, visitationPrecedence);
            for (String key : attributes.keySet()) {
                if (!visitationPrecedence.contains(key)) {
                    visitAttribute(key, visitor);
                }
            }
            for (String key : lists.keySet()) {
                if (!visitationPrecedence.contains(key)) {
                    visitList(key, visitor);
                }
            }
            for (String key : objects.keySet()) {
                if (!visitationPrecedence.contains(key)) {
                    visitObject(key, visitor, visitationPrecedence);
                }
            }
            for (String key : objectLists.keySet()) {
                if (!visitationPrecedence.contains(key)) {
                    visitObjectList(key, visitor, visitationPrecedence);
                }
            }
            for (String key : emptyLists) {
                if (!visitationPrecedence.contains(key)) {
                    visitEmptyList(key, visitor);
                }
            }
            return visitor.getAndReset();
        }

        private <T> void visitEmptyList(String label, JsonVisitor<T> visitor) throws UnexpectedJSONContentException {
            visitor.emptyList(label);
        }

        private <T> void visitAttribute(String label, JsonVisitor<T> visitor) throws UnexpectedJSONContentException {
            visitor.attribute(label, attributes.get(label).getFirst(), attributes.get(label).getSecond());
        }

        private <T> void visitList(String label, JsonVisitor<T> visitor) throws UnexpectedJSONContentException {
            visitor.list(label, lists.get(label).getFirst(), lists.get(label).getSecond());
        }

        private <T> void visitObject(String label, JsonVisitor<T> visitor, List<String> visitationPrecedence)
                throws UnexpectedJSONContentException {
            String adjustedLabel = label;
            if (label.equals("null")) {
                adjustedLabel = null;
            }
            JsonVisitor<?> newVisitor = visitor.objectStarts(adjustedLabel);
            Object subResult = objects.get(label).visit(newVisitor, visitationPrecedence);
            visitor.object(adjustedLabel, subResult);
        }

        private <T> void visitObjectList(String label, JsonVisitor<T> visitor, List<String> visitationPrecedence)
                throws UnexpectedJSONContentException {
            String adjustedLabel = label;
            if ("null".equals(label)) {
                adjustedLabel = null;
            }
            List<Object> subResults = new ArrayList<>();
            for (GenericResult object : objectLists.get(label)) {
                JsonVisitor<?> newVisitor = visitor.objectStarts(adjustedLabel);
                Object subResult = object.visit(newVisitor, visitationPrecedence);
                subResults.add(subResult);
            }
            visitor.objectList(adjustedLabel, subResults);
        }

        private void visitByPrecedence(JsonVisitor<?> visitor, List<String> visitationPrecedence)
                throws UnexpectedJSONContentException {
            for (String label : visitationPrecedence) {
                if (attributes.containsKey(label)) {
                    visitAttribute(label, visitor);
                } else if (lists.containsKey(label)) {
                    visitList(label, visitor);
                } else if (objects.containsKey(label)) {
                    visitObject(label, visitor, visitationPrecedence);
                } else if (objectLists.containsKey(label)) {
                    visitObjectList(label, visitor, visitationPrecedence);
                } else if (emptyLists.contains(label)) {
                    visitEmptyList(label, visitor);
                }
            }
        }

    }

    public class GenericVisitor implements JsonVisitor<GenericResult> {
        private Map<String, Pair<String, Type>> attributes;
        private Map<String, Pair<List<String>, Type>> lists;
        private Map<String, GenericResult> objects;
        private Map<String, List<GenericResult>> objectLists;
        private Set<String> emptyLists;

        public GenericVisitor() {
            attributes = new HashMap<>();
            lists = new HashMap<>();
            objects = new HashMap<>();
            objectLists = new HashMap<>();
            emptyLists = new HashSet<>();
        }

        public GenericResult get() {
            return new GenericResult(attributes, lists, objects, objectLists, emptyLists);
        }

        public GenericResult getAndReset() {
            GenericResult result = new GenericResult(attributes, lists, objects, objectLists, emptyLists);
            attributes = new HashMap<>();
            lists = new HashMap<>();
            objects = new HashMap<>();
            objectLists = new HashMap<>();
            emptyLists = new HashSet<>();
            return result;
        }

        public void currentPosition(Position position) {
        }

        public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
            return new GenericVisitor();
        }

        public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
            attributes.put(String.valueOf(label), Pair.of(value, type));
        }

        public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
            lists.put(String.valueOf(label), Pair.of(values, type));
        }

        public void object(String label, Object value) throws UnexpectedJSONContentException {
            objects.put(String.valueOf(label), (GenericResult) value);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
            objectLists.put(String.valueOf(label), (List) values);
        }

        public void emptyList(String label) throws UnexpectedJSONContentException {
            emptyLists.add(String.valueOf(label));
        }

    }
}
