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

import java.util.HashMap;
import java.util.Map;

/**
 * Counter<T> zaehlt die Vorkommen des Objekts T
 * 
 * @param <T>
 */
public class Counter<T> {

   /**
    * Zaehlendes Objekt in der HashMap
    */
   private class CounterInt {
      int counter = 0;

      public void inc() {
         ++counter;
      }

      public int getCount() {
         return counter;
      }
   }

   private HashMap<T, CounterInt> map = new HashMap<T, CounterInt>();

   /**
    * Zaehlen eines weiteren Objekts key
    * 
    * @param key
    */
   public void count(T key) {
      if (!map.containsKey(key)) {
         map.put(key, new CounterInt());
      }
      map.get(key).inc();
   }

   /**
    * @param key
    * @return Anzahl des Objekts key
    */
   public int getCount(T key) {
      if (map.containsKey(key)) {
         return map.get(key).getCount();
      }
      return 0;
   }

   /**
    * @return Kopie des Speichers: Hashmap<gezähltes Object,Anzahl>
    */
   public HashMap<T, Integer> getCopyOfMap() {
      HashMap<T, Integer> copy = new HashMap<T, Integer>(map.size());
      for (Map.Entry<T, CounterInt> entry : map.entrySet()) {
         copy.put(entry.getKey(), entry.getValue().getCount());
      }
      return copy;
   }

   /**
    * @return Darstellung aller gezählten Objekte
    */
   public String toString() {
      String str = "";
      for (T key : map.keySet()) {
         str += map.get(key).getCount() + "* " + key + "\n";
      }
      return str;
   }

}