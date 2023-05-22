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

package com.gip.xyna.xnwh.persistence.memory;



import java.util.concurrent.locks.ReadWriteLock;



public final class MemoryRowLock {

  final private LockWithUnderlyingData sustainedLock;
  final private ReadWriteLock temporaryLock;


  public MemoryRowLock(ReadWriteLock temporaryLock, LockWithUnderlyingData sustainedLock) {
    this.temporaryLock = temporaryLock;
    this.sustainedLock = sustainedLock;
  }


  /**
   * lesender und schreibender zugriff auf zeilen. stellt sicher, dass:<br>
   * <ul>
   * <li>alle spalten einer zeilen sind in sich konsistent, d.h. zwei udpates auf verschiedene spalten und ein select
   * auf eine zeile kommen sich nicht gegenseitig in die quere.</li>
   * <li>die evaluierung von whereclauses ist konsistent mit der r�ckgabe von objekten.</li>
   * </ul>
   * <br> dieses lock heisst temporary, weil es nur innerhalb eines transaktionsschritts
   * wirkt.
   */
  public ReadWriteLock temporaryLock() {
    return temporaryLock;
  }


  /**
   * exklusives lock, dass von "select for updates", normalen updates oder deletes betroffene zeilen lockt. dieses lock
   * wird bei normalen selects nicht �berpr�ft, d.h. diese funktionieren weiter. das lock heisst sustained, weil es �ber
   * einen transaktionsschritt hinaus anh�lt.
   */
  public LockWithUnderlyingData sustainedLock() {
    return sustainedLock;
  }


}
