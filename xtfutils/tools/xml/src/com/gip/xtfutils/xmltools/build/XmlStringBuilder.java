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

package com.gip.xtfutils.xmltools.build;

import java.util.*;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.XmlBuilder.*;
import com.gip.xtfutils.xmltools.build.XmlStringBuilder.NamespaceMap.RegisterNamespaceReturnType;



/*
 * FIXME: Bei Sibling-Elementen mit gleichem namespace, der ungleich nsp von parent,
 * wird der nsp nur im ersten Element definiert
 *
 */
public class XmlStringBuilder extends XmlBuilder {


  public interface XsbBasicNode {
    public void write(Writer w, int level, NamespaceMap nspMap);
  }

  public static class XsbElementNode extends ElementNode implements XsbBasicNode {

    public XsbElementNode(String name)  {
      super(name);
    }
    public XsbElementNode(String name, XmlNamespace nsp) {
      super(name, nsp);
    }
    public void addInnerText(String text) {
      TextNode t = new XsbTextNode(text);
      this.addChild(t);
    }
    public void write(Writer w, int level, NamespaceMap nspMap) {
      w.indent(level);
      w.append("<");
      RegisterNamespaceReturnType resp = RegisterNamespaceReturnType.ALREADY_REGISTERED;
      if (this.getNamespace() != null) {
        resp = nspMap.register(this.getNamespace());
        //TODO: check if prefix is null, generate prefix by map if necessary?
        w.append(this.getNamespace().getPrefix()).append(":");
      }
      w.append(_tagName);
      for (XmlAttribute attr : _attributes) {
        w.append(" ").append(attr.getName()).append("=\"").append(attr.getValue()).append("\"");
      }
      if (resp == RegisterNamespaceReturnType.REGISTERED_FIRST_TIME) {
        w.append(" ").append("xmlns:").append(this.getNamespace().getPrefix()).append("=\"");
        w.append(this.getNamespace().getUri()).append("\"");
      }
      if (this.getNamespaceDeclarations().size() > 0) {
        for (XmlNamespace nsp2 : this.getNamespaceDeclarations()) {
          nspMap.register(nsp2);
          w.append(" ").append("xmlns:").append(nsp2.getPrefix()).append("=\"");
          w.append(nsp2.getUri()).append("\"");
        }
      }
      if (_children.size() < 1) {
        w.append("/>");
        return;
      }
      w.append(">");
      if ((_children.size() == 1) && (_children.get(0) instanceof XsbTextNode)) {
        w.append(((XsbTextNode)_children.get(0)).getText());
      }
      else {
        for (BasicNode node : _children) {
          if (node instanceof XsbBasicNode) {
            w.newline();
            ((XsbBasicNode) node).write(w, level + 1, nspMap);
          }
        }
        w.nindent(level);
      }
      w.append("</");
      if (this.getNamespace() != null) {
        w.append(this.getNamespace().getPrefix()).append(":");
      }
      w.append(_tagName).append(">");
    }
  }



  public static class XsbTextNode extends TextNode implements XsbBasicNode {
    public XsbTextNode(String text) {
      super(text);
    }

    public void write(Writer w, int level, NamespaceMap nspMap) {
      w.indent(level).append(_text);
    }
  }



  public static class Writer {
    protected StringBuilder _s = new StringBuilder();
    protected String _tab = "";
    protected int tabWidth = 2;

    public Writer() { initTab(); }
    protected void initTab() {
      for (int i = 0; i < tabWidth; i++) { _tab += " "; }
    }
    public Writer append(String text) {
      _s.append(text);
      return this;
    }
    public Writer newline() {
      _s.append("\n");
      return this;
    }
    public Writer nindent(int level) {
      _s.append("\n");
      for (int i = 0; i < level; i++) { _s.append(_tab); }
      return this;
    }
    public Writer indent(int level) {
      for (int i = 0; i < level; i++) { _s.append(_tab); }
      return this;
    }
    public String toString() {
      return _s.toString();
    }
    public String buildString() {
      return _s.toString();
    }
  }

  public static class NamespaceMap {
    public enum RegisterNamespaceReturnType { ALREADY_REGISTERED, REGISTERED_FIRST_TIME }
    protected Map<String, Boolean> _definedNamespaces = new HashMap<String, Boolean>();
    RegisterNamespaceReturnType register(XmlNamespace nsp) {
      if (nsp == null) { throw new IllegalArgumentException("Namespace to register may not be null."); }
      Boolean val = _definedNamespaces.put(nsp.getUri(), true); //returns null or previous value in map
      if (val != null) {
        return RegisterNamespaceReturnType.ALREADY_REGISTERED;
      }
      return RegisterNamespaceReturnType.REGISTERED_FIRST_TIME;
    }
  }


  public String buildXmlString() {
    if (_root == null) { return ""; }
    if (!(_root instanceof XsbBasicNode)) { return ""; }
    XsbBasicNode casted = (XsbBasicNode) _root;
    Writer writer = new Writer();
    casted.write(writer, 0, new NamespaceMap());
    return writer.buildString();
  }


  @Override
  public String buildXmlString(EncodingName encoding) {
    //FIXME
    return buildXmlString();
  }

  @Override
  public ElementNode createElementNode(String name, XmlNamespace nsp) {
    return new XsbElementNode(name, nsp);
  }

  @Override
  protected TextNode createTextNode(String text) {
    return new XsbTextNode(text);
  }
}
