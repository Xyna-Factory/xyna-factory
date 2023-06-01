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

package com.gip.xyna.coherence.standalone;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;


public class TestCoherencePayload extends CoherencePayload {

  private static final long serialVersionUID = 5900676535861706203L;

  private final String text;


  public TestCoherencePayload(String text) {
    this.text = text;
  }


  public String toString() {
    return super.toString() + " content: \"" + text + "\"";
  }

}
