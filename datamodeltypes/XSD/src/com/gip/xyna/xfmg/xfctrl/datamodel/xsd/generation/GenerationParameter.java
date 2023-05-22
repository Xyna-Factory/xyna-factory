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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation;

import java.util.EnumMap;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator.LabelCustomization;


/**
 * ben�tigte Parameter zur Konfiguration der Datentyp-Generierung
 */
public interface GenerationParameter {
  
  public String getBasePath();
   
  public EnumMap<LabelCustomization, String> getLabelCustomization();

  public String getPathCustomization();
  
  public String getDataModelName();

  public boolean isGenerationOptions_expandChoice();
  
  public boolean isGenerationOptions_namedSimpleTypes();
  
  public boolean isGenerationOptions_includeHiddenTypes();
  
  public boolean isOverwrite();

  //public String getFQModelName();

  public boolean isGenerateDataModelInfo();

  public String getDataModelTypeName();
  
}
