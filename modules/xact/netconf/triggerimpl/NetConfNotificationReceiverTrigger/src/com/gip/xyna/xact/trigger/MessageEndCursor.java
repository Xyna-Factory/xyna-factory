/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger;


public class MessageEndCursor {

  public static final String MESSAGE_END_TOKEN = "]]>]]>";
  
  private int matchedPosition = -1;
  
  public void registerChar(char input) {
    if (matchedPosition >= 5) {
      matchedPosition = -1;
    }
    String str = String.valueOf(input);
    int checkPos = matchedPosition + 1;
    String checkStr = MESSAGE_END_TOKEN.substring(checkPos, checkPos + 1);
    if (!checkStr.equals(str)) {
      matchedPosition = -1;
      return;
    }
    matchedPosition++;
  }
  
  public boolean isMessageEndTokenFullyMatched() {
    return (matchedPosition == 5);
  }
  
}
