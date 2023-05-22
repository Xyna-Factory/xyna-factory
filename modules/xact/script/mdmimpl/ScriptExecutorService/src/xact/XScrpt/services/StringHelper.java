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

package xact.XScrpt.services;

import java.util.StringTokenizer;


public class StringHelper {


  /**
   * Hilfsmethode, liefert ein Array mit den Kommando Elementen: Alles was nach einem ',' steht wird als ein Element
   * betrachtet und landet im letzten Array Element. Alles davor wird an whitespace gesplittet und kommt in eigene Array
   * Elemente
   */
  static String[] splitCmd(String s) {
    int n, i;
    String[] a;
    String cmd = null;
    String arg = null;

    if (-1 != (n = s.indexOf(','))) {
      cmd = s.substring(0, n).trim();
      arg = s.substring(n + 1).trim();
    } else {
      cmd = s;
    }

    StringTokenizer st = new StringTokenizer(cmd);
    a = new String[st.countTokens() + (arg != null ? 1 : 0)];
    for (i = 0; st.hasMoreTokens(); i++) {
      a[i] = st.nextToken();
    }
    if (arg != null)
      a[i] = arg;

    return a;
  }

}
