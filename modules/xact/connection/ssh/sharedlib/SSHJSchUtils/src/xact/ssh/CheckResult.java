/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package xact.ssh;

import com.jcraft.jsch.HostKeyRepository;


public enum CheckResult {
  
  OK(HostKeyRepository.OK),
  CHANGED(HostKeyRepository.CHANGED),
  NOT_INCLUDED(HostKeyRepository.NOT_INCLUDED);

  private final int numericRepresentation;
  
  private CheckResult(int numericRepresentation) {
    this.numericRepresentation = numericRepresentation;
  }
  
  
  public int getNumericRepresentation() {
    return numericRepresentation;
  }
  
  
  public static CheckResult getByNumericRepresentation(int numericRepresentation) {
    for (CheckResult checkResult : values()) {
      if (checkResult.numericRepresentation == numericRepresentation) {
        return checkResult;
      }
    }
    throw new IllegalArgumentException("Invalid numericRepresentation: " + numericRepresentation);
  }
  
}
