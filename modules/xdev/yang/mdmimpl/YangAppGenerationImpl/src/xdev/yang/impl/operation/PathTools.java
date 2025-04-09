/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.operation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.model.api.stmt.Action;
import org.yangcentral.yangkit.model.api.stmt.Anydata;
import org.yangcentral.yangkit.model.api.stmt.Anyxml;
import org.yangcentral.yangkit.model.api.stmt.Case;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Input;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.LeafList;
import org.yangcentral.yangkit.model.api.stmt.Notification;
import org.yangcentral.yangkit.model.api.stmt.Output;
import org.yangcentral.yangkit.model.api.stmt.Rpc;
import org.yangcentral.yangkit.model.api.stmt.YangList;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;

import xdev.yang.impl.YangStatementTranslator.YangStatementTranslation;


public class PathTools {

  public boolean identifiersAreEqual(String id1, String id2) {
    return removeOptionalPrefix(id1).equals(removeOptionalPrefix(id2));
  }
  
  
  private String removeOptionalPrefix(String id) {
    if (id == null) { return ""; }
    if (!id.contains(":")) { return id; }
    return id.substring(id.indexOf(":") + 1);
  }

  
  public Optional<YangStatement> navigateToPath(YangElement nodeIn, List<QName> path, int index) {
    if (nodeIn == null) { return Optional.empty(); }
    if (path == null) { return Optional.empty(); }
    if (index >= path.size()) { return Optional.empty(); }
    if (!(nodeIn instanceof YangStatement)) { return Optional.empty(); }
    YangStatement node = (YangStatement) nodeIn;
    String namespace = YangStatementTranslation.getNamespace(node);
    String localname = YangStatementTranslation.getLocalName(node);
    boolean isPathNode = isPathNode(node); 
    if (isPathNode) {
      QName qname = path.get(index);
      if (identifiersAreEqual(localname, qname.getLocalName()) &&
          Objects.equals(namespace, qname.getNamespace().toString())) {
        if (index == path.size() - 1) {
          return Optional.ofNullable(node);
        }
      }
    }
    List<YangElement> sublist = YangStatementTranslation.getSubStatements(node);
    if (sublist == null) { return Optional.empty(); }
    int newIndex = index;
    if (isPathNode) {
      newIndex++;
    }
    for (YangElement elem : sublist) {
      Optional<YangStatement> result = navigateToPath(elem, path, newIndex);
      if (!result.isEmpty()) {
        return result;
      }
    }
    return Optional.empty();
  }
  
  
  /* from RFC 7950;
     schema node: A node in the schema tree.  One of action, container,
     leaf, leaf-list, list, choice, case, rpc, input, output,
     notification, anydata, and anyxml.
   */
  public boolean isPathNode(YangElement elem) {
      if (elem instanceof Action) { return true; }
      else if (elem instanceof Container) { return true; }
      else if (elem instanceof Leaf) { return true; }
      else if (elem instanceof LeafList) { return true; }
      else if (elem instanceof YangList) { return true; }
      else if (elem instanceof Case) { return true; }
      else if (elem instanceof Rpc) { return true; }
      else if (elem instanceof Input) { return true; }
      else if (elem instanceof Output) { return true; }
      else if (elem instanceof Notification) { return true; }      
      else if (elem instanceof Anyxml) { return true; }
      else if (elem instanceof Anydata) { return true; }
      return false;
  }
  
}
