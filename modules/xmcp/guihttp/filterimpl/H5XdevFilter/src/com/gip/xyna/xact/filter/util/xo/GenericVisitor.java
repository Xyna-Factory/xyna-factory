/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xact.filter.util.xo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.Position;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;


public class GenericVisitor implements JsonVisitor<GenericResult> {

  protected Map<String, Pair<String, Type>> attributes;
  protected Map<String, Pair<List<String>, Type>> lists;
  protected Map<String, GenericResult> objects;
  protected Map<String, List<GenericResult>> objectLists;
  protected Set<String> emptyLists;
  
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
    objectLists.put(String.valueOf(label), (List) values);
  }

  public void emptyList(String label) throws UnexpectedJSONContentException {
    emptyLists.add(String.valueOf(label));
  }
  
  
}
