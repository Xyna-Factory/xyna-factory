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
package com.gip.xyna.xsor.interconnect;

import java.util.Hashtable;

import com.gip.xyna.xsor.common.Interconnectable;


public abstract class InterconnectableStore {

  protected volatile Hashtable<Integer, Interconnectable> registeredXSORMemory;//Fuer Callback zu den CCMemories


  public InterconnectableStore() {
    registeredXSORMemory = new Hashtable<Integer, Interconnectable>();
  }


  /**
   * Registrieren von XSORMemories unter einem typspezifischen (clusterübergreifend identischen) Dispatching-Codes
   */
  public synchronized void register(Interconnectable interconnectable) {
    Hashtable<Integer, Interconnectable> registeredXSORMemoryTemp =
        new Hashtable<Integer, Interconnectable>(registeredXSORMemory);
    registeredXSORMemoryTemp.put(interconnectable.getObjectType(), interconnectable);
    registeredXSORMemory = registeredXSORMemoryTemp;
  }
  
  
  public synchronized void unregister(Interconnectable interconnectable) {
    Hashtable<Integer, Interconnectable> registeredXSORMemoryTemp =
                    new Hashtable<Integer, Interconnectable>(registeredXSORMemory);
    registeredXSORMemoryTemp.remove(interconnectable.getObjectType());
    registeredXSORMemory = registeredXSORMemoryTemp;
  }


}
