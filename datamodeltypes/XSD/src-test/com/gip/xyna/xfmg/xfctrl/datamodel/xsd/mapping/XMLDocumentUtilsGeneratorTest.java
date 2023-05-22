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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.EnumMap;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator.LabelCustomization;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;


public class XMLDocumentUtilsGeneratorTest {
  
  public static void main(String[] args) {
    GenerationParameter gp = new GenerationParameter() {
      
      public boolean isOverwrite() {
        return true;
      }
      
      public boolean isGenerationOptions_expandChoice() {
        return true;
      }
      
      public boolean isGenerateDataModelInfo() {
        return true;
      }
      
      public String getPathCustomization() {
        return "";
      }
      
      
      public EnumMap<LabelCustomization, String> getLabelCustomization() {
        return null;
      }
      
      
      public String getFQModelName() {
        return "Wald";
      }
      
      
      public String getDataModelTypeName() {
        return "XSD";
      }
      
      
      public String getDataModelName() {
        return "XMDM2";
      }
      
      public String getBasePath() {
        return "base";
      }

      public boolean isGenerationOptions_namedSimpleTypes() {
        return false;
      }
      
      public boolean isGenerationOptions_includeHiddenTypes() {
        return false;
      }

    };
    XMLDocumentUtilsGenerator utils = new XMLDocumentUtilsGenerator(gp);
    utils.createConstants();
    Datatype dt = utils.createXML();
    System.out.println(dt.toXML());
  }

}
