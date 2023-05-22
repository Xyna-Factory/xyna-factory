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
package com.gip.xyna.utils.db.types;

/**
 * Basis f�r beliebige Objekte, die in der DB als String gespeichert werden k�nnen.
 * �ber dieses Interface ist das Eintragen in Parameter m�glich.
 *
 * @param <T>
 */
public interface StringSerializable<T> {

  /**
   * Ausgabe als String zur Speicherung in der DB
   * @return
   */
  public String serializeToString();
  
  /**
   * Lesen des Strings und Konversion in ein neues Objekt
   * (eigentlich sollte diese Methode static sein, dies l�sst sich jedoch nicht im Interface vorschreiben)
   * @param string
   * @return
   */
  public T deserializeFromString(String string);
  
}
