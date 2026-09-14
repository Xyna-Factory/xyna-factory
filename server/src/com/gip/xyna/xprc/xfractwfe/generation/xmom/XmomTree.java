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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNodeInfo.CloneMode;

public class XmomTree {

  private final XmomNodeInfo root;

  public XmomTree(XmomNodeInfo root) {
    this.root = root;
  }

  
  public XmomNodeInfo getRoot() {
    return root;
  }
  
  public XmomPointer getRootPointer() {
    return XmomPointer.getRoot(this);
  }

  public IdMapping getIdMapping() {
    return root.getIdMapping();
  }
  
  public XmomTree doClone() {
    IdMapping idMapping = new IdMapping();
    return new XmomTree(this.root.doClone(idMapping));
  }
  
  
  public XmomTree doCloneWithoutIgnoreAndEmpty() {
    IdMapping idMapping = new IdMapping();
    return new XmomTree(this.root.doCloneWithoutIgnoreAndEmpty(idMapping));
  }
  
  
}
