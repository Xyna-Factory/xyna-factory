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

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.Constants;
import xmcp.yang.OperationTableData;

public class OperationSignatureFixer {

  public void fixSignature(XynaOrderServerExtension order, OperationTableData data) {
    try(Operation op = Operation.open(order, data.getOperationGroup(), data.getRuntimeContext(), data.getOperation())) {
      List<String> serviceInputs = op.getInputVarTypes();
      for (int i = serviceInputs.size() - 1; i >= -2; i--) {
        op.deleteInput(i);
      }
      op.addInput("MessageId", "xmcp.yang.MessageId");
      op.addInput(Constants.RPC_NETCONF_INPUT_NAME, "xact.templates.NETCONF");
      
      List<OperationSignatureVariable> configInputs = OperationSignatureVariable.loadSignatureEntries(op.getMeta(), Constants.VAL_LOCATION_INPUT);
      for(int i=0; i<configInputs.size(); i++) {
        op.addInput(configInputs.get(i).getVarName(), configInputs.get(i).getFqn());
      }
      op.save();
      op.deploy();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
}
