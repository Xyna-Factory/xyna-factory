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

package com.gip.xyna.xsor.protocol;

/**
 * An object implementing XSORPayload is a ValueObject that is unique identified within it's table through it it's primaryKey 
 * After the invocation of copyIntoByteArray from an instance A, a copyFromByteArray should generate an instance B with exactly the same values
 * even in corner cases like null-values (which would need to be encoded into the byte[])
 * If this is not the case it will lead to errors if there exist indices on those values 
 * ben�tigt annotation {@link XSORPayloadInformation}
 */
public interface XSORPayload {

  /* getPrimaryKey()-Methode wird aus dem Storable uebernommen */
  Object getPrimaryKey();

  /* getTableName()-Methode wird aus dem Storable uebernommen */
  String getTableName();  

  /**
   * schreibe die serialisierte Form der Daten mit dem offset ins byte[] ba
   * REENTRANT, da quasistatisch
   */
  void copyIntoByteArray(byte[] ba, int offset);//in bind copyToXC copies all data from local Objects to XCMemory

  /**
   * erzeugt aus der serialisierten Form der Daten im byte[] ba mit dem offset eine Kopie von XC-Payload
   * REENTRANT, da quasistatisch
   * @param ba
   * @param offset
   * @return
   */
  XSORPayload copyFromByteArray(byte[] ba, int offset);

  /**
   * wandelt das PrimaryKey Object in ein byte[] um 
   * @param o
   * @return
   * REENTRANT, da quasistatisch
   **/
  byte[] pkToByteArray(Object o);
  
  /**
   * wandelt eine byte[]-Repr�sentation in ein PK-Objekt um 
   * @param o
   * @return
   * REENTRANT, da quasistatisch
   **/
  Object byteArrayToPk(byte[] ba);

}
