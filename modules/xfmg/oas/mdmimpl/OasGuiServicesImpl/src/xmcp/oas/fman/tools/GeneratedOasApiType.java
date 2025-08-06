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


package xmcp.oas.fman.tools;


public class GeneratedOasApiType {

  private final XmomType xmom;

  public GeneratedOasApiType(XmomType xmom) {
    if (xmom == null) {
      throw new IllegalArgumentException("Xmom type is null.");
    }
    this.xmom = xmom;
  }
  
  public GeneratedOasApiType(String fqn) {
    this(new XmomType(fqn));
  }
  
  public XmomType getXmomType() {
    return xmom;
  }
  
  public String getFqName() {
    return xmom.getFqName();
  }
  
}
