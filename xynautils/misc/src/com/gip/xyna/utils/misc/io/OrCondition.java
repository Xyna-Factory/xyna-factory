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
package com.gip.xyna.utils.misc.io;

import com.gip.xyna.utils.misc.io.Condition;

public class OrCondition implements Condition {
   Condition c1;
   Condition c2;

   public OrCondition(Condition c1, Condition c2) {
      this.c1 = c1;
      this.c2 = c2;
   }

   public boolean check(String string) {
      return c1.check(string) || c2.check(string);
   }

   public String getWaitForDescription() {
      return "(" + c1.getWaitForDescription() + " or "
            + c2.getWaitForDescription() + ")";
   }
}
