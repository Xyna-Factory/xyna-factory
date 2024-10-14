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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;



public class PathMapInformation {

  private final Map<String, String> inheritFromDataModel = new HashMap<String, String>();
  private String pathKey;
  private String pathValue;

  public static PathMapInformation parse(Element metaElement) {
    Element e = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.PATHMAP);
    if (e == null) {
      return null;
    }

    PathMapInformation pmi = new PathMapInformation();

    Element pathKeyElement = XMLUtils.getChildElementByName(e, GenerationBase.EL.PATHKEY);
    if (pathKeyElement != null) {
      pmi.pathKey = XMLUtils.getTextContent(pathKeyElement);
    }
    Element pathValueElement = XMLUtils.getChildElementByName(e, GenerationBase.EL.PATHVALUE);
    if (pathValueElement != null) {
      pmi.pathValue = XMLUtils.getTextContent(pathValueElement);
    }
    List<Element> inheritFromDMEls = XMLUtils.getChildElementsByName(e, GenerationBase.EL.INHERIT_FROM_DATAMODEL);
    for (Element inheritFromDMEl : inheritFromDMEls) {
      String path = XMLUtils.getTextContent(XMLUtils.getChildElementByName(inheritFromDMEl, GenerationBase.EL.INHERIT_FROM_DATAMODEL_PATH));
      String value =
          XMLUtils.getTextContent(XMLUtils.getChildElementByName(inheritFromDMEl, GenerationBase.EL.INHERIT_FROM_DATAMODEL_VALUE));
      pmi.inheritFromDataModel.put(path, value);
    }

    return pmi;
  }


  public String getPathKey() {
    return pathKey;
  }


  public String getInheritFromDataModel(String childPath) {
    return inheritFromDataModel.get(childPath);
  }


  /**
   * null -&gt; nicht gesetzt (d.h. value wird autodetektiert)
   * leerstring -&gt; es gibt keinen value
   * pfad -&gt; pfad zu value
   */
  public String getPathValue() {
    return pathValue;
  }

}
