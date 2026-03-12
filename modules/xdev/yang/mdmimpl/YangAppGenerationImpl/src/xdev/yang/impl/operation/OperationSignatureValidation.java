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

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.Constants;
import xmcp.yang.OperationTableData;
import xmcp.yang.fman.OperationSignatureProblem;

public class OperationSignatureValidation {

  public List<OperationSignatureProblem> validate(XynaOrderServerExtension order, OperationTableData data) {
    List<OperationSignatureProblem> result = new ArrayList<>();
    try (Operation op = Operation.open(order, data.getOperationGroup(), data.getRuntimeContext(), data.getOperation())) {
      if (op.getRpcName() == null || op.getRpcName().isBlank()) {
        return result;
      }
      List<String> serviceInputs = op.getInputVarTypes();
      List<OperationSignatureVariable> configInputs = OperationSignatureVariable.loadSignatureEntries(op.getMeta(), Constants.VAL_LOCATION_INPUT);
      if (serviceInputs.size() < 1 || !"xmcp.yang.MessageId".equals(serviceInputs.get(0))) {
        result.add(new OperationSignatureProblem.Builder().description("First service input is not of type MessageId").instance());
      }
      if (serviceInputs.size() < 2 || !"xact.templates.NETCONF".equals(serviceInputs.get(1))) {
        result.add(new OperationSignatureProblem.Builder().description("Second service input is not of type NETCONF").instance());
      }
      for (int i = 0; i < configInputs.size(); i++) {
        String expectedType = configInputs.get(i).getFqn();
        if (serviceInputs.size() < 3 + i || !expectedType.equals(serviceInputs.get(2 + i))) {
          String desc = "Custom input " + (i + 1) + " (" + configInputs.get(i).getVarName() + ") does not match service";
          result.add(new OperationSignatureProblem.Builder().description(desc).instance());
        }
      }
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
