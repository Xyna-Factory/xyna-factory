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
package com.gip.xyna.xnwh.persistence;


public enum IndexTypeComposite {
  
  /**
   * unterst�tzt nur queries bzgl gleichheit
   */
  HASH,
  
  /**
   * unterst�tzt nur queries bzgl gleichheit
   * im Gegensatz zu dem einfachen Hash-Type bedeutet diese Kennzeichnung allerdings das die Spalten-Kombination eindeutig einen Datensatz indentifiziert
   * und das somit das �berschreiben bestehender Eintr�ge verhindert werden soll
   */
  UNIQUE,
  
  /**
   * unterst�tzt queries bzgl "kleiner" oder "gr��er" bzgl der ordnung, das kann sowas sein wie
   * col1 = 'a' AND col2 = 'b' AND col3 &lt; 'c', aber nicht
   * col1 &lt; 'a' AND col2 = 'b' AND col3 &lt; 'c',
   * wenn die ordnung definiert ist �ber
   * (col1, col2, col3).
   */
  ORDERED_LEX
  
}
