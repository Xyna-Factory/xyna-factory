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
package com.gip.xyna.utils.misc.io;

public class WellFormedXmlCondition implements Condition {

   public WellFormedXmlCondition() {
   }

   public boolean check(String screen) {
      int xmlindex = screen.indexOf("<?xml");
      if (xmlindex == -1)
         return false;
      int starttagindex = screen.indexOf("<", xmlindex + 1);
      if (starttagindex == -1)
         return false;
      int starttagendindex = screen.indexOf(">", starttagindex);
      if (starttagendindex == -1)
         return false;
      String starttag = screen.substring(starttagindex, starttagendindex + 1);
      String endtag = "</" + starttag.substring(1);
      return (screen.indexOf(endtag, starttagendindex + 1) >= 0);
   }

   public String getWaitForDescription() {
      return "a wellformed xml ";
   }

}