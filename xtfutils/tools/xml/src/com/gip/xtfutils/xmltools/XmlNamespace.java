/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xtfutils.xmltools;

public class XmlNamespace {

  public static class NamespaceBuilderWithUri {
    private String uri = null;
    public NamespaceBuilderWithUri(String name) { this.uri = name; }
    public XmlNamespace setPrefix(String val) {
      return new XmlNamespace(val, this.uri);
    }
  }

  //public static XmlNamespace NO_NAMESPACE = new XmlNamespace("", "");

  private String prefix = null;
  private String uri = null;

  protected XmlNamespace(String prefix, String name) {
    this.uri = name;
    this.prefix = prefix;
  }
  public XmlNamespace() {}
  public static XmlNamespace.NamespaceBuilderWithUri setUri(String name) {
    XmlNamespace.NamespaceBuilderWithUri ret = new NamespaceBuilderWithUri(name);
    return ret;
  }
  public String getPrefix() {
    return prefix;
  }
  public String getUri() {
    return uri;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof XmlNamespace)) { return false; }
    XmlNamespace nsp = (XmlNamespace) obj;
    boolean prefixEqual = false;
    boolean nameEqual = false;
    if (uri == null) {
      if (nsp.getUri() == null) {
        nameEqual = true;
      }
    }
    else {
      nameEqual = uri.equals(nsp.getUri());
    }
    if (prefix == null) {
      if (nsp.getPrefix() == null) {
        prefixEqual = true;
      }
    }
    else {
      prefixEqual = prefix.equals(nsp.getPrefix());
    }
    return (nameEqual && prefixEqual);
  }


  @Override
  public String toString() {
    return "XmlNamespace{ prefix=" + prefix + ", uri=" + uri + "} ";
  }

}
