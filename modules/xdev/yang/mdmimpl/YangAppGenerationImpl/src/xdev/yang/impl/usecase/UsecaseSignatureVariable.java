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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.utils.XMLUtils;

import xdev.yang.impl.Constants;


public class UsecaseSignatureVariable {

  private String fqn;
  private String varName;
  
  
  public UsecaseSignatureVariable(String fqn, String varName) {
    this.fqn = fqn;
    this.varName = varName;
  }
  

  public static Element loadSignatureElement(Document document, String location) {
    List<Element> signatureElements = XMLUtils.getChildElementsByName(document.getDocumentElement(), Constants.TAG_SIGNATURE);
    for (Element signatureElement : signatureElements) {
      if (location.equals(signatureElement.getAttribute(Constants.ATT_SIGNATURE_LOCATION))) {
        return signatureElement;
      }
    }
    throw new RuntimeException("No " + location + " signature found.");
  }
  

  public static List<Element> loadSignatureEntryElements(Document document, String location) {
    Element signatureElement = loadSignatureElement(document, location);
    return XMLUtils.getChildElementsByName(signatureElement, Constants.TAG_SIGNATURE_ENTRY);
  }
  
  public static List<UsecaseSignatureVariable> loadSignatureEntries(Document document, String location) {
    List<Element> signatureEntryElements = loadSignatureEntryElements(document, location);
    List<UsecaseSignatureVariable> result = new ArrayList<UsecaseSignatureVariable>();
    for(Element signatureEntryElement : signatureEntryElements) {
      String fqn = signatureEntryElement.getAttribute(Constants.ATT_SIGNATURE_ENTRY_FQN);
      String varName = signatureEntryElement.getAttribute(Constants.ATT_SIGNATURE_ENTRY_VARNAME);
      UsecaseSignatureVariable entry = new UsecaseSignatureVariable(fqn, varName);
      result.add(entry);
    }
    return result;
  }

  public static void overwriteSignatureEntryAtIndex(Document document, String location, int index, 
                                                    UsecaseSignatureVariable newValues) {
    List<Element> signatureEntryElements = loadSignatureEntryElements(document, location);
    if (index >= signatureEntryElements.size()) {
      throw new IllegalArgumentException("Could not find xml element 'SignatureEntry' with list index " + index +
                                         " at location attribute " + location);
    }
    Element elem = signatureEntryElements.get(index);
    newValues.updateNode(elem);
  }


  public void updateNode(Element e) {
    e.setAttribute(Constants.ATT_SIGNATURE_ENTRY_FQN, fqn);
    e.setAttribute(Constants.ATT_SIGNATURE_ENTRY_VARNAME, varName);
  }

  public void createAndAddElement(Document meta, String location) {
    Element signatureElement = loadSignatureElement(meta, location);
    Element newEntryNode = meta.createElement(Constants.TAG_SIGNATURE_ENTRY);
    updateNode(newEntryNode);
    signatureElement.appendChild(newEntryNode);
  }


  public String getFqn() {
    return fqn;
  }


  public String getVarName() {
    return varName;
  }
}
