/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.operation.implementation.RpcImplementationProvider;
import xmcp.yang.OperationTableData;
import xmcp.yang.fman.OperationSignatureEntry;



public class RemoveVariableFromOperationSignature {

  private static Logger _logger = Logger.getLogger(RemoveVariableFromOperationSignature.class);
  
  public void removeVariable(XynaOrderServerExtension order, OperationTableData operation, OperationSignatureEntry signature) {
    String fqn = operation.getOperationGroup();
    String workspace = operation.getRuntimeContext();
    String operationName = operation.getOperation();

    try (Operation uc = Operation.open(order, fqn, workspace, operationName)) {
      Document meta = uc.getMeta();
      Element signatureElement = OperationSignatureVariable.loadSignatureElement(meta, signature.getLocation());
      List<Element> signatureEntryElements = OperationSignatureVariable.loadSignatureEntryElements(meta, signature.getLocation());
      Element signatureEntryToRemove = signatureEntryElements.get(signature.getIndex());
      signatureElement.removeChild(signatureEntryToRemove);

      uc.updateMeta();
      uc.deleteInput(signature.getIndex());
      RpcImplementationProvider implProvider = new RpcImplementationProvider();
      String newImpl = implProvider.createImpl(meta, uc.getInputVarNames());
      uc.updateImplementation(newImpl);
      uc.save();
      uc.deploy();
    }
    catch (Exception e) {
      _logger.error(e.getMessage(), e);
    }
  }

}
