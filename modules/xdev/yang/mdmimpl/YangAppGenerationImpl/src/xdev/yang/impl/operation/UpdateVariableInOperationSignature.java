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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.operation.implementation.ImplementationProvider;
import xdev.yang.impl.operation.implementation.OpImplProviderSelection;
import xmcp.yang.OperationTableData;
import xmcp.yang.fman.OperationSignatureEntry;


public class UpdateVariableInOperationSignature {

  private static Logger _logger = Logger.getLogger(UpdateVariableInOperationSignature.class);

  public void updateVariable(XynaOrderServerExtension order, OperationTableData operation, OperationSignatureEntry signature) {
    String fqn = operation.getOperationGroup();
    String workspace = operation.getRuntimeContext();
    String operationName = operation.getOperation();

    try (Operation uc = Operation.open(order, fqn, workspace, operationName)) {
      Document meta = uc.getMeta();
      List<OperationSignatureVariable> list = OperationSignatureVariable.loadSignatureEntries(meta, signature.getLocation().toLowerCase());
      if (signature.getIndex() >= list.size()) {
        throw new IllegalArgumentException("Variable index cannot be matched to input variable: " + signature.getIndex());
      }
      OperationSignatureVariable updated = new OperationSignatureVariable(signature.getFqn(), signature.getVariableName());
      OperationSignatureVariable.overwriteSignatureEntryAtIndex(meta, signature.getLocation().toLowerCase(), signature.getIndex(), updated);

      uc.updateMeta();
      for (int i = uc.getInputVarNames().size() - 1; i >= 0; i--) {
        uc.deleteInput(i);
      }
      for (int i = 0; i < list.size(); i++) {
        if (i == signature.getIndex()) {
          uc.addInput(signature.getVariableName(), signature.getFqn());
        }
        else {
          OperationSignatureVariable var = list.get(i);
          uc.addInput(var.getVarName(), var.getFqn());
        }
      }
      ImplementationProvider implProvider = new OpImplProviderSelection().selectProvider(meta);
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
