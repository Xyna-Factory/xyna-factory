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

import com.gip.xyna.utils.collections.Pair;


/**
 *
 */
public class TRObjectReference {

  private String fqName;
  private String componentName;


  public TRObjectReference(TRObject object) {
    this.fqName = object.getFqName();
  }

  public TRObjectReference(TRObject object, TRComponent component) {
    this.fqName = object.getFqName();
    this.componentName = component.getName();
  }
  
  public TRObjectReference(TRObjectReference object, String componentName) {
    this.fqName = object.fqName;
    this.componentName = componentName.trim();
  }
  
  @Override
  public String toString() {
    return "TRObjectReference("+componentName+","+fqName+")";
  }
    
  public Pair<TRObjectContainer, TRObject> getObjectFrom(TRObjectContainer container) {
    
    if( componentName != null ) {
      //System.err.println("getObjectFrom "+componentName+" "+ fqName + " from "+ container);
      
      TRComponent component = container.getDocument().getComponent(componentName);
      if( component != null ) {
        
        return Pair.of((TRObjectContainer)component,component.getObject(fqName));
      } else {
        System.err.println("Component "+componentName+" missing");
      }
    } else {
      return Pair.of(container, container.getObject(fqName));
    }
    return null;
  }

}
