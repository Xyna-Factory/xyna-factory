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

package xdev.yang.impl.operation.implementation;

import org.w3c.dom.Document;

import xdev.yang.impl.operation.OperationAssignmentUtils;


public class OpImplProviderSelection {

  public ImplementationProvider selectProvider(Document meta) {
    String rpcName = OperationAssignmentUtils.readRpcName(meta);
    if (rpcName != null) {
      return new RpcImplementationProvider();
    }
    String tagName = OperationAssignmentUtils.readTagName(meta);
    if (tagName == null) {
      throw new IllegalArgumentException("Missing meta data in xmom: Tag name");
    }
    return new YangMappingImplementationProvider();
  }
  
}
