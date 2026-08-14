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


public class FqName {

  private final String fqn;
  private final String path;
  private final String typename;
  

  public FqName(String fqname) {
    if (fqname == null) {
      throw new IllegalArgumentException("Fq-name is null.");
    }
    this.fqn = fqname.trim();
    this.path = this.fqn.substring(0, this.fqn.lastIndexOf("."));
    this.typename = this.fqn.substring(this.fqn.lastIndexOf(".") + 1, this.fqn.length());
  }
  
  public String getFqName() {
    return fqn;
  }

  
  public String getPath() {
    return path;
  }

  
  public String getTypename() {
    return typename;
  }
  
}
