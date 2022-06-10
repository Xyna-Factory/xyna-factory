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
 * Verwaltet Zahlen/Indizes von 1-n, n ist nicht fest
 * Unterscheidet zwischen freien und vergebenen Zahlen. Wenn eine Zahl zurückgegeben wurde, wird sie in der Zukunft wieder als frei herausgegeben.
 * 
 * Nutzen: Gewährleisten, dass man eindeutige Zahlen vergeben kann ohne sich um Overflow kümmern zu müssen, wie man es müsste, wenn man z.B. einfach
 * nur einen Integer hochzählt.
 * 
 * Speicherverbrauch: n ints (n ist Maximum der jemals gleichzeitig benötigten Zahlen).
 * 
 * Naive Implementierung wäre ein Set oder eine Liste von freien ints zu pflegen. Das ist aber von der Performance her deutlich teurer als diese Implementierung.
 * Dafür hat es den Vorteil, dass es unter Umständen weniger Speicher benötigt.
 */
public class ReusableIndexProvider {
  /*
   * TODO: konfigurierbar: statische größe (kein growth)
   *       growth erst, wenn alle zahlen herausgegeben worden sind, und nicht eine zahl zuvor
   */

  private final float growthFactor;
  /*
   * head zeigt auf den nächsten freien index. 
   * der value im array ist der index des übernächsten frei indizes usw.
   * somit ergibt sich eine verkettete liste von freien indizes.
   * tail ist der letzte frei index.
   * die werte in allen nicht freien indizes des arrays sind egal.
   */
  private int[] vals;
  private int head;
  private int tail;


  public ReusableIndexProvider(float growthFactor, int initialSize) {
    this.growthFactor = growthFactor;
    vals = new int[initialSize];
    for (int i = 0; i < vals.length; i++) {
      vals[i] = i + 1;
    }
    head = 0;
    tail = vals.length - 1;
  }


  public synchronized int getNextFreeIdx() {
    if (head == tail) {
      //grow: die eingetragenen indizes sind alle egal.
      int l = vals.length;
      vals = new int[(int) (l * growthFactor)];
      int l2 = vals.length;
      for (int i = l; i < l2 - 1; i++) {
        vals[i] = i + 1;
      }
      //tail ans ende setzen
      tail = l2 - 1;
      //head zeigt auf das erste neue element
      vals[head] = l;
    }
    
    //kette am head verkürzen
    int idx = head;
    head = vals[idx];
    return idx;
  }


  public synchronized void returnIdx(int idx) {
    //kette am tail verlängern
    vals[tail] = idx;
    tail = idx;
  }
}
