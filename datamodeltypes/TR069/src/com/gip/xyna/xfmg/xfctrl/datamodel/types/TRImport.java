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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.TR069Tools;


/**
 *
 */
public class TRImport {

  private String reference;
  private Set<String> dataTypes;
  private Set<String> models;
  private Set<String> components;
  
  
  
  public TRImport(String file) {
    this.reference = TR069Tools.createReference(file);
    this.dataTypes = new HashSet<String>();
    this.models = new HashSet<String>();
    this.components = new HashSet<String>();
  }

  public void addDataType(String name) {
    dataTypes.add(name);
  }

  public void addModel(String name) {
    models.add(name);
  }

  public void addComponent(String name) {
    components.add(name);
  }

  public String getReference() {
    return reference;
  }

  public Set<String> getModels() {
    return models;
  }

  public Set<String> getComponents() {
    return components;
  }
}
