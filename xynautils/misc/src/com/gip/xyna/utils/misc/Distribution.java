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

import java.util.ArrayList;
import java.util.Collections;

public class Distribution<T extends Comparable<? super T>> {

   private ArrayList<T> memory = new ArrayList<T>();
   private boolean analyzed = false;

   /**
    * @param object
    */
   public synchronized void add(T object) {
      memory.add(object);
      analyzed = false;
   }

   private void analyze() {
      if (!analyzed) {
         Collections.sort(memory);
         analyzed = true;
      }
   }

   /**
    * @return Anzahl der Messung
    */
   public int size() {
      return memory.size();
   }

   /**
    * @return kleinster Messwert
    */
   public T getMinimum() {
      analyze();
      return memory.get(0);
   }

   /**
    * @return größter Messwert
    */
   public T getMaximum() {
      analyze();
      return memory.get(memory.size() - 1);
   }

   /**
    * @return Median der Messwerte
    */
   public T getMedian() {
      analyze();
      return memory.get(memory.size() / 2);
   }

   /**
    * @return 1. Decil der Messwerte (10% der Messwerte sind kleiner)
    */
   public T get1Decil() {
      analyze();
      return memory.get(memory.size() / 10);
   }

   /**
    * @return 9. Decil der Messwerte (10% der Messwerte sind größer)
    */
   public T get9Decil() {
      analyze();
      return memory.get((9 * memory.size()) / 10);
   }

   /**
    * Ausgabe von Minimum, Median und Maximum
    */
   public String toString() {
      if (memory.size() > 0) {
         return memory.size() + " Messungen, min: " + getMinimum()
               + ", median: " + getMedian() + ", max: " + getMaximum();
      }
      return memory.size() + " Messungen";

   }

}