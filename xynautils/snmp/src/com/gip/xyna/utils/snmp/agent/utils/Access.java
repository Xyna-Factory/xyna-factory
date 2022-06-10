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
package com.gip.xyna.utils.snmp.agent.utils;

/**
 * Sammlung zweier Interfaces, ueber die der EnumOidSingleHandler erkennen kann,
 * ob die Daten zu der OID geaendert oder nur gelesen werden koennen.
 *
 */
public class Access {
  
  interface Type<Value> {
    /**
     * Liefert die angefragten Daten
     * @return
     */
    public Value get();
  }
  
  /**
   * Nur ReadOnly-Zugriff ist moeglich
   *
   * @param <Value>
   */
  public interface ReadOnly<Value> extends Type<Value> { //nichts zu ergaenzen
  }

  /**
   * Schreibender Zugriff ist moeglich
   *
   * @param <Value>
   */
  public interface ReadWrite<Value> extends Type<Value> {
    /**
     * Setzt die uebergebenen Daten
     * @param value
     */
    public void set(Value value);
  }

}
