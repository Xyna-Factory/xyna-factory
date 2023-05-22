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

package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class ArrayUtils {

  public static <T> T[] addToArray(T[] array, int index, T newElement) {
    List<T> tmpList = new ArrayList<T>(Arrays.asList(array));
    tmpList.add(index, newElement);
    return tmpList.toArray(array);
  }

  public static String[] removeFromStringArray(String[] array, int index) {
    ArrayList<String> tmpList = new ArrayList<String>(Arrays.asList(array));
    tmpList.remove(index);
    return tmpList.toArray(new String[array.length - 1]);
  }


  public static Boolean[] removeFromBoolArray(Boolean[] array, int index) {
    ArrayList<Boolean> tmpList = new ArrayList<Boolean>(Arrays.asList(array));
    tmpList.remove(index);
    return tmpList.toArray(new Boolean[array.length - 1]);
  }

}
