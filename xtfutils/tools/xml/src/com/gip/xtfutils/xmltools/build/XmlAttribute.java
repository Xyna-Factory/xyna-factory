/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import com.gip.xtfutils.xmltools.XmlNamespace;

public class XmlAttribute {

  public static class AttributeBuilderWithName {
    private String name = null;
    public AttributeBuilderWithName(String name) { this.name = name; }
    public XmlAttribute setValue(String val) {
      return new XmlAttribute(this.name, val);
    }
  }

  protected String _name = null;
  protected String _value = null;
  protected XmlNamespace _nsp = null;

  protected XmlAttribute(String name, String value) {
    _name = name;
    _value = value;
  }

  protected XmlAttribute(String name, String value, XmlNamespace nsp) {
    _name = name;
    _value = value;
    _nsp = nsp;
  }

  public XmlAttribute() {}

  public String getName() { return _name; }

  public String getValue() { return _value; }

  public XmlNamespace getNamespace() { return _nsp; }

  public static XmlAttribute.AttributeBuilderWithName setName(String name) {
    XmlAttribute.AttributeBuilderWithName ret = new AttributeBuilderWithName(name);
    return ret;
  }

  public XmlAttribute setNamespace(XmlNamespace nsp) {
    _nsp = nsp;
    return this;
  }

  public boolean hasNamespace() {
    return (_nsp != null);
  }

}
