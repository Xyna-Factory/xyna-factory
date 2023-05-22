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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;

import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class XMOMCommonValueParser {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(XMOMCommonValueParser.class);
  
  
  public static CommonValues tryParsingCommonValues(String originalFqName, XMOMDatabaseType type, Long revision) {
    switch (type) {
      case DATATYPE :
      case WORKFLOW :
      case FORMDEFINITION :
        return tryParsingCommonValuesFromRoot(originalFqName, revision);
      case EXCEPTION :
        return tryParsingCommonValuesFromException(originalFqName, revision);
      default :
        return null;
    }
  }
  
  
  
  private static CommonValues tryParsingCommonValuesFromException(String originalFqName, Long revision) {
    try {
      Document d = XMLUtils.parse(GenerationBase.getFileLocationOfXmlName(originalFqName, revision) + ".xml", true);
      Element rootElement = d.getDocumentElement();
      List<Element> exceptionElements = XMLUtils.getChildElementsByName(rootElement, "ExceptionType");
      if (exceptionElements != null && exceptionElements.size() > 0) {
        return tryParsingCommonValuesFromElement(originalFqName, exceptionElements.get(0), revision);
      } else {
        return null;
      }
    } catch (Throwable t) {
      logger.debug("Error on fallback parsing during workflow registration", t);
      return null;
    }
  }
  
  
  private static CommonValues tryParsingCommonValuesFromRoot(String originalFqName, Long revision) {
    try {
      Document d = XMLUtils.parse(GenerationBase.getFileLocationOfXmlName(originalFqName, revision) + ".xml", true);
      Element rootElement = d.getDocumentElement();
      return tryParsingCommonValuesFromElement(originalFqName, rootElement, revision);
    } catch (Throwable t) {
      logger.debug("Error on fallback parsing during workflow registration", t);
      return null;
    }
  }
  
  private static CommonValues tryParsingCommonValuesFromElement(String originalFqName, Element e, Long revision) {
    return new CommonValues(originalFqName,
                            e.getAttribute(GenerationBase.ATT.LABEL),
                            e.getAttribute(GenerationBase.ATT.TYPEPATH),
                            e.getAttribute(GenerationBase.ATT.TYPENAME),
                            revision);
  }
  
  
  
  static class CommonValues {
    String fqname;
    String label;
    String path;
    String name;
    Long revision;
    
    CommonValues(String fqname, String label, String path, String name, Long revision) {
      this.fqname = fqname;
      // see comment in XMOMDatabaseEntry constructor
      if (label == null || label.length() <= 0) {
        this.label = name.toLowerCase();
      } else {
        this.label = label.toLowerCase();
      }
      this.path = path;
      this.name = name;
      this.revision = revision;
    }
  }
  
}
