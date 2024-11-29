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
package xdev.yang.impl.usecase;



import org.w3c.dom.Document;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xmcp.yang.UseCaseTableData;
import xmcp.yang.fman.UsecaseSignatureEntry;



public class AddVariableToUsecaseSignature {

  public void addVariable(XynaOrderServerExtension order, UseCaseTableData usecase, UsecaseSignatureEntry signature) {
    String fqn = usecase.getUsecaseGroup();
    String workspace = usecase.getRuntimeContext();
    String usecaseName = usecase.getUseCase();

    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspace, usecaseName);
    if (meta == null) {
      return;
    }

    UsecaseSignatureVariable variable = new UsecaseSignatureVariable(signature.getFqn(), signature.getVariableName());
    variable.createAndAddElement(meta.getSecond(), signature.getLocation().toLowerCase());

    try (Usecase uc = Usecase.open(order, fqn, workspace, usecaseName)) {
      String xml = XMLUtils.getXMLString(meta.getSecond().getDocumentElement(), false);
      uc.updateMeta(xml, meta.getFirst());
      uc.addInput(signature.getVariableName(), signature.getFqn());
      uc.save();
      uc.deploy();
    } catch (Exception e) {
    }
  }
}
