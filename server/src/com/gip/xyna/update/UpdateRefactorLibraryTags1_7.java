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



import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class UpdateRefactorLibraryTags1_7 extends MDMUpdate {

  Logger logger = CentralFactoryLogging.getLogger(Update.class);
  
  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.6");
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.7");
  }
  



  @Override
  protected void update(Document doc) throws XynaException {
    Element root = doc.getDocumentElement();
    //Prüfen ob Datentype mit Service-Operationen
    // die (shared-)libraries aller Operationen sammeln
    // entsprechend als XML-Liste neu schreiben 
    Set<String> libs = new HashSet<String>();
    Set<String> sharedLibs = new HashSet<String>();
    
    if (root.getTagName().equals(GenerationBase.EL.DATATYPE)) { 
      List<Element> serviceElems = XMLUtils.getChildElementsByName(root, GenerationBase.EL.SERVICE);
      if (serviceElems != null && serviceElems.size() > 0) {
        for (Element service : serviceElems) {
          List<Element> operationElems = XMLUtils.getChildElementsByName(service, GenerationBase.EL.OPERATION);
          if (operationElems != null && operationElems.size() > 0) {
            for (Element operation : operationElems) {
              Element sourceCode = XMLUtils.getChildElementByName(operation, GenerationBase.EL.SOURCECODE);
              if (sourceCode != null) {
                Element libraryElem = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.LIBRARIES);
                if (libraryElem != null) {
                  String libText = libraryElem.getTextContent();
                  if (libText != null && !"".equals(libText.trim())) {
                    libs.addAll(Arrays.asList(libText.split(":")));
                  }
                  //delete old lib-tag
                  sourceCode.removeChild(libraryElem);                  
                }
                Element sharedLibElem = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.SHAREDLIB);
                if (sharedLibElem != null) {
                  String sharedLibText = sharedLibElem.getTextContent();
                  if (sharedLibText != null && !"".equals(sharedLibText.trim())) {
                    sharedLibs.addAll(Arrays.asList(sharedLibText.split(":")));
                  }
                  //delete old sharedLib-tag
                  sourceCode.removeChild(sharedLibElem);
                }
              }
            }            
          }          
        }
        //append new lib-tags
        if (libs != null && libs.size() > 0) {
          for (String lib : libs) {
            Element refactoredLibElem = doc.createElement(GenerationBase.EL.LIBRARIES);
            refactoredLibElem.setTextContent(lib);
            root.insertBefore(refactoredLibElem, serviceElems.get(0));
          }
        }
        //append new sharedLib-tags
        if (sharedLibs != null && sharedLibs.size() > 0) {
          for (String sharedLib : sharedLibs) {
            Element refactoredSharedLibElem = doc.createElement(GenerationBase.EL.SHAREDLIB);
            refactoredSharedLibElem.setTextContent(sharedLib);
            root.insertBefore(refactoredSharedLibElem, serviceElems.get(0));
          }
        }
      }     
    }
  }

}
