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
package com.gip.xyna.xnwh.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/** 
 * erlaubt die definition mehrerer indizes zu einem {@link Storable}.
 * 
 * Beispiel:
 * <code>
 * @CompositeIndices(
  indices = {              
      @CompositeIndex(type = IndexTypeComposite.HASH, 
                      value = CapacityStorable.COL_NAME + "," + CapacityStorable.COL_STATE),
      @CompositeIndex(type = IndexTypeComposite.ORDERED_LEX, 
                      value = CapacityStorable.COL_CARDINALITY + "," + CapacityStorable.COL_NAME + "," + CapacityStorable.COL_ID),
      @CompositeIndex(type = IndexTypeComposite.HASH, 
                      value = "MAX(" + CapacityStorable.COL_NAME + "," + CapacityStorable.COL_STATE + ")")
  }
)
</code>
//alternativen: statische methode gibt array zurück => man kann statische methoden nicht vererben?!, keine laufzeit- oder compilesicherheit (methodenname falsch geschrieben?)

 */
public @interface CompositeIndices {

  public CompositeIndex[] indices();
  
}
