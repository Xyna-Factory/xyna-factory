/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/**
 *  1.2 workflows und datentypen erhalten ein throw-xynaexception tag für abwärtskompatibilität. zukünftig werden throws genau definiert sein.
 */
public class UpdateMDMThrows1_3 extends MDMUpdate {

  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.2");
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.3");
  }

  @Override
  protected void update(Document doc) throws XynaException {   
    Element root = doc.getDocumentElement();
    List<Element> serviceElements;
    if (root.getTagName().equals(GenerationBase.EL.SERVICE)) {
      serviceElements = new ArrayList<Element>();
      serviceElements.add(root);
    } else {
      serviceElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.SERVICE);
    }    
    for (Element serviceElement : serviceElements) {
      List<Element> operations = XMLUtils.getChildElementsByName(serviceElement, GenerationBase.EL.OPERATION);
      for (Element operation : operations) {
        List<Element> throwsElements = XMLUtils.getChildElementsByName(operation, GenerationBase.EL.THROWS);
        if (throwsElements.size() == 0) {
          Element throwsElement = doc.createElement(GenerationBase.EL.THROWS);
          
          //insert-stelle herausfinden: nach output
          Element nextElement = null;
          
          Element outputElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.OUTPUT);
          if (outputElement == null) {
            Element inputElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.INPUT);
            if (inputElement == null) {
              List<Element> childElements = XMLUtils.getChildElements(operation);
              if (childElements.size() > 0) {
                nextElement = childElements.get(0); 
              }
            } else {              
              nextElement = XMLUtils.getNextElementSibling(inputElement); 
            }
          } else {
            nextElement = XMLUtils.getNextElementSibling(outputElement);
          }
          
          //nun wird das throws element tatsächlich angefügt
          if (nextElement == null) {
            operation.appendChild(throwsElement);
          } else {
            operation.insertBefore(throwsElement, nextElement);
          }
          
          Element exceptionElement = doc.createElement(GenerationBase.EL.EXCEPTION);
          throwsElement.appendChild(exceptionElement);
        }        
      }
    }
  }
  

}
