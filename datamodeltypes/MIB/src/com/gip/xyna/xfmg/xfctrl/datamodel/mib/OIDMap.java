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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import java.util.List;

import com.gip.xyna.utils.collections.trees.SimpleTreeMap;


/**
 *
 */
public class OIDMap<V> extends SimpleTreeMap<OID,V> {

  
  public V put(String oid, V value) {
    return put( new OID(oid), value);
  }

  public List<OID> getChildren(String oid, boolean recursively) {
    return getChildren(new OID(oid), recursively);
  } 

  public OID getParent(String oid) {
    return getParent(new OID(oid));
  }
  
}
