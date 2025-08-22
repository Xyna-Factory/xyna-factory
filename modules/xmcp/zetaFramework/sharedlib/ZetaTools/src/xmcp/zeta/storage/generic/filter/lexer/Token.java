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

package xmcp.zeta.storage.generic.filter.lexer;

// erbt von filter elem?
public abstract class Token {
  
  // attr. orig input string (für quote-blöcke)
  private final String originalInput;

  
  public Token(String originalInput) {
    this.originalInput = originalInput;
  }


  // get orig input string
  public String getOriginalInput() {
    return originalInput;
  }


  // is finished: return false
  public boolean isFinished() {
    return false;
  }
  
}
