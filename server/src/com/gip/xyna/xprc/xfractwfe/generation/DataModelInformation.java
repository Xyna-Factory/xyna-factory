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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;



public class DataModelInformation {

  private final Map<String, String> values = new HashMap<String, String>();
  private String[] modelNames;

  public static DataModelInformation parse(Element metaElement) {
    Element e = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DATAMODEL);
    if (e == null) {
      return null;
    }

    DataModelInformation dmi = new DataModelInformation();
    List<Element> childElements = XMLUtils.getChildElements(e);
    List<String> modelNamesList = new ArrayList<String>();
    for (Element child : childElements) {
      String name = child.getNodeName();
      String value = XMLUtils.getTextContent(child);
      if (name.equals(GenerationBase.EL.MODELNAME)) {
        modelNamesList.add(value);
      } else {
        dmi.values.put(name, value);
      }
    }
    dmi.modelNames = modelNamesList.toArray(new String[modelNamesList.size()]);
    return dmi;
  }


  @Override
  public String toString() {
    return "DataModelInformation("+values+","+Arrays.toString(modelNames)+")";
  }
  
  public String get(String key) {
    return values.get(key);
  }


  public String[] getModelNames() {
    return modelNames;
  }

}
