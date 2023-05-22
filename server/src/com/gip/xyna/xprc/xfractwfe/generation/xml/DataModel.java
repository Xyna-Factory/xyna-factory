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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;


/**
 *
 */
public class DataModel implements XmlAppendable {
 
  private String modelName;
  private XmomType baseModel;
  private XmlAppendable dataModelSpecifics;
  
  public DataModel() {
  }

  public void appendXML(XmlBuilder xml) {
    xml.startElement(EL.DATAMODEL);{
      if( dataModelSpecifics != null ) {
        dataModelSpecifics.appendXML(xml);
      }
      xml.optionalElement(EL.MODELNAME, modelName );
      if( baseModel != null ) {
        xml.element(EL.BASEMODEL, baseModel.getFQTypeName() );
      }
    }xml.endElement(EL.DATAMODEL);
  }
  
  public static DataModel modelName(String modelName, XmlAppendable specifics) {
    DataModel dm = new DataModel();
    dm.modelName = modelName;
    dm.dataModelSpecifics = specifics;
    return dm;
  }

  public static DataModel baseModel(XmomType baseModel, XmlAppendable specifics) {
    DataModel dm = new DataModel();
    dm.baseModel = baseModel;
    dm.dataModelSpecifics = specifics;
    return dm;
  }
  
  public XmlAppendable getDataModelSpecifics() {
    return dataModelSpecifics;
  }
}
