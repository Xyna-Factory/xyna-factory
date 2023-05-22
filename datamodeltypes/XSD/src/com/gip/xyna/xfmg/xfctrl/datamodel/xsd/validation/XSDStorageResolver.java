/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.validation;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;



public class XSDStorageResolver implements LSResourceResolver {
  
  
  private Map<String, byte[]> fileNameContentMap;
  
  public XSDStorageResolver(Map<String, byte[]> fileNameContentMap) {
    this.fileNameContentMap = fileNameContentMap;
  }

  public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
    LSInputImpl concreteLSInput = new LSInputImpl();
    String processedSystemId = preprocessNameSpace(systemId);
    for (String fileName : fileNameContentMap.keySet()) {
      if (fileName.endsWith(processedSystemId)) {
        concreteLSInput.setByteStream(new ByteArrayInputStream(fileNameContentMap.get(fileName)));
        return concreteLSInput;
      }
    }
    return null;
  }

  
  private String preprocessNameSpace(String namespaceURI) {
    String processedNameSpace = namespaceURI;
    if (namespaceURI.lastIndexOf("..") > 0) {
      processedNameSpace = namespaceURI.substring(namespaceURI.lastIndexOf("..") + 3);
    }
    return processedNameSpace;
  }

}
