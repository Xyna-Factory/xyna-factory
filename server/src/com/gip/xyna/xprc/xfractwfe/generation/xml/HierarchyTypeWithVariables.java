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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.List;


public abstract class HierarchyTypeWithVariables {

  protected XmomType type;
  protected XmomType basetype;
  protected Meta meta;
  protected List<Variable> variables;
  
  
  public abstract String toXML();
  
  
  public static abstract class HierarchyTypeWithVariablesBuilder<T extends HierarchyTypeWithVariables> {
    
    public abstract HierarchyTypeWithVariablesBuilder<T> basetype(XmomType basetype);
    
    public abstract HierarchyTypeWithVariablesBuilder<T> variable(Variable variable);
    
    public abstract T build();
  }
  
  
}
