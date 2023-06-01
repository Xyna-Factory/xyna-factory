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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.misc.StringParameter.Unmatched;


public class DataModelParameters implements Serializable{

  private static final long serialVersionUID = 1L;

  private String dataModelType;
  private String dataModelName;
  private String dataModelVersion;
  private String dataModelFqName;
  private Map<String,Object> parameters;
  private Unmatched unmatchedKeys = Unmatched.Error;
  
  public DataModelParameters(String dataModelType, String dataModelName, String dataModelVersion) {
    this.dataModelType = dataModelType;
    this.dataModelName = dataModelName;
    this.dataModelVersion = dataModelVersion;
  }

  //konstruktor aus abwärtskompatibilitätsgründen xtf
  public DataModelParameters(String dataModelType, String dataModelName) {
    this.dataModelType = dataModelType;
    this.dataModelName = dataModelName;
  }

  public String getDataModelType() {
    return dataModelType;
  }
  
  public void setDataModelType(String dataModelType) {
    this.dataModelType = dataModelType;
  }
  
  public String getDataModelName() {
    return dataModelName;
  }
  
  public void setDataModelName(String dataModelName) {
    this.dataModelName = dataModelName;
  }
  
  public String getDataModelVersion() {
    return dataModelVersion;
  }
  
  public void setDataModelVersion(String dataModelVersion) {
    this.dataModelVersion = dataModelVersion;
  }
  
  public String getDataModelFqName() {
    return dataModelFqName;
  }
  
  public void setDataModelFqName(String dataModelFqName) {
    this.dataModelFqName = dataModelFqName;
  }
 
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, ? extends Object> params) {
    this.parameters = new HashMap<String,Object>(params);
  }
  
  public void addParameters(Map<String, ? extends Object> params) {
    if( parameters == null ) {
      parameters = new HashMap<String,Object>(params);
    } else {
      parameters.putAll(params);
    }
  }
 
  public void addParameter(String key, Object value) {
    if( value != null ) {
      if( parameters == null ) {
        parameters = new HashMap<String,Object>();
      }
      parameters.put(key, value);
    }
  }
  
  
  public Unmatched getUnmatchedKeys() {
    return unmatchedKeys;
  }
  
  public void setUnmatchedKeys(Unmatched unmatchedKeys) {
    this.unmatchedKeys = unmatchedKeys;
  }

}
