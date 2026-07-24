/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.python;


public class PythonMdmConfig {
  
  private boolean genImpl = false;
  private boolean genTypeHints = false;
  private boolean genDocu = false;
  
  public boolean isGenImpl() {
    return genImpl;
  }
  
  public PythonMdmConfig genImpl(boolean genImpl) {
    this.genImpl = genImpl;
    return this;
  }
  
  public boolean isGenTypeHints() {
    return genTypeHints;
  }
  
  public PythonMdmConfig genTypeHints(boolean genTypeHints) {
    this.genTypeHints = genTypeHints;
    return this;
  }
  
  public boolean isGenDoku() {
    return genDocu;
  }
  
  public PythonMdmConfig genDocu(boolean genDoku) {
    this.genDocu = genDoku;
    return this;
  }
  
}
