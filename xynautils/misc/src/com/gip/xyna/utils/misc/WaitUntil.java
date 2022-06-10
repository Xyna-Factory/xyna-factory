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
package com.gip.xyna.utils.misc;

/**
 * WaitUntil: Warten bis eine Bedingung erfuellt ist oder ein Timeout erreicht
 * wurde.
 * 
 * Die Bedingung wird durch die abstrakte Funktion until() realisiert, die bei
 * der konkreten Verwendung noch geschrieben werden muss. Dies geht sehr einfach
 * in einer anonymem Klasse, wie im Beispiel unten. Nach Aufruf von waitNow()
 * wird dann in regelmaessigen Abstaenden (sleep) die Funktion until()
 * aufgerufen. Dies geschieht solange, bis entweder die Bedingung erfuellt wurde
 * oder der Timeout erreicht wurde. Die Rueckgabe von waitNow gibt an, ob die
 * Bedingung zuletzt erfuellt wurde. Im Konstruktor werden Timeout und die
 * Wartezeit (sleep) definiert.
 * 
 * 
 * Beispiel: Queue queue = ...; //Queue fuellen ... //Warten, bis jemand anderes
 * die Queue geleert hat: new WaitUntil( 20000 ){ public boolean until(){ return
 * queue.isEmtpy(); } }.waitNow();
 */
public abstract class WaitUntil {
   private int timeout;
   private int sleep;

   /**
    * Setzt den Timout, Sleep ist timeout/1000 oder minimal 10 ms
    * 
    * @param timeout
    */
   public WaitUntil(int timeout) {
      this.timeout = timeout;
      sleep = timeout > 10000 ? timeout / 1000 : 10;
   }

   /**
    * Setzt den Timout und Sleep
    * 
    * @param timeout
    * @param sleep
    */
   public WaitUntil(int timeout, int sleep) {
      this.timeout = timeout;
      this.sleep = sleep;
   }

   public abstract boolean until();

   public boolean waitNow() {
      long waitStart = System.currentTimeMillis();
      boolean waitFinished = until();
      while (!waitFinished && System.currentTimeMillis() < waitStart + timeout) {
         try {
            Thread.sleep(sleep);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         waitFinished = until();
      }
      return waitFinished;
   }

}
