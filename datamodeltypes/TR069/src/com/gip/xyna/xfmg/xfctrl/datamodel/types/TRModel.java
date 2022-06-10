/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.util.ArrayList;
import java.util.List;



/**
 *
 */
public class TRModel extends TRObjectContainer {

  private String version;
  private String fqName;
  private String label;
  private List<String> componentNames;

  public TRModel(TRDataModelDefinition document, String fqName) {
    super( document, "" );
    this.fqName = fqName;
    this.componentNames = new ArrayList<String>();
    parseFqName(fqName);
  }
  
  public TRModel(TRDocument document, TRModel baseModel) {
    super(document, baseModel);
    this.fqName = baseModel.getFqName();
    this.componentNames = new ArrayList<String>(baseModel.getComponentNames());
    parseFqName(fqName);
  }

  @Override
  public String toString() {
    return "TRModel("+fqName+", "+getObjectCount()+" objects)";
  }
  
  private void parseFqName(String fqName) {
    int idx = fqName.indexOf(':');
    if( idx > 0 ) {
      this.name = fqName.substring(0,idx);
      this.version = fqName.substring(idx+1);
    } else {
      this.name = fqName;
      this.version = "0";
    }
  }

  public void migrateFqName(String fqName) {
    this.fqName = fqName;
    parseFqName(fqName);
  }

  public String getFqName() {
    return fqName;
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
    
  public String getVersion() {
    return version;
  }

  
  public String getPrettyName(boolean withVersionAndDocument) {
    if( withVersionAndDocument ) {
      return "model "+name+":"+version+" in document "+document.getName();
    } else {
      return "model "+name;
    }
  }

  public void addComponent(TRComponent component) {
    componentNames.add(component.getName());
  }

  public List<String> getComponentNames() {
    return componentNames;
  }




}
