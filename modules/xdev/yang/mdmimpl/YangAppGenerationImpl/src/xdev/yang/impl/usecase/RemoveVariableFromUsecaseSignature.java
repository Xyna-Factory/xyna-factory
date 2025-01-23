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



import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import xmcp.yang.UseCaseTableData;
import xmcp.yang.fman.UsecaseSignatureEntry;



public class RemoveVariableFromUsecaseSignature {

  private static Logger _logger = Logger.getLogger(RemoveVariableFromUsecaseSignature.class);
  
  public void removeVariable(XynaOrderServerExtension order, UseCaseTableData usecase, UsecaseSignatureEntry signature) {
    String fqn = usecase.getUsecaseGroup();
    String workspace = usecase.getRuntimeContext();
    String usecaseName = usecase.getUseCase();

    try (Usecase uc = Usecase.open(order, fqn, workspace, usecaseName)) {
      Document meta = uc.getMeta();
      Element signatureElement = UsecaseSignatureVariable.loadSignatureElement(meta, signature.getLocation());
      List<Element> signatureEntryElements = UsecaseSignatureVariable.loadSignatureEntryElements(meta, signature.getLocation());
      Element signatureEntryToRemove = signatureEntryElements.get(signature.getIndex());
      signatureElement.removeChild(signatureEntryToRemove);
      
      uc.updateMeta();
      uc.deleteInput(signature.getIndex());
      // handle problem that input variable names will be automatically changed by xyna factory
      uc.updateImplementation("return null;");
      uc.save();
      uc.deploy();
    } catch (Exception e) {
      _logger.error(e.getMessage(), e);
      return;
    }
    try (Usecase uc = Usecase.open(order, fqn, workspace, usecaseName)) {
      Document meta = uc.getMeta();      
      UsecaseImplementationProvider implProvider = new UsecaseImplementationProvider();
      // adjust implementation java code to changed input variable names
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
