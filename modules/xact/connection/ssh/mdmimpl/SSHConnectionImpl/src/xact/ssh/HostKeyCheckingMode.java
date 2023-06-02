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


public enum HostKeyCheckingMode {
  YES("yes", Yes.class), NO("no", No.class), ASK("ask", Ask.class);
  
  private final String stringRepresentation;
  private final Class<? extends HostKeyChecking> xynaRepresentation;
  
  private HostKeyCheckingMode(String stringRepresentation, Class<? extends HostKeyChecking> xynaRepresentation) {
    this.stringRepresentation = stringRepresentation;
    this.xynaRepresentation = xynaRepresentation;
  }
  
  
  public String getStringRepresentation() {
    return stringRepresentation;
  }
  
  
  public static <C extends HostKeyChecking> HostKeyCheckingMode getByXynaRepresentation(C xynaRepresentation) {
    if (xynaRepresentation != null) {
      for (HostKeyCheckingMode mode : values()) {
        if (mode.xynaRepresentation.isInstance(xynaRepresentation)) {
          return mode;
        }
      }
    }
    return HostKeyCheckingMode.YES;
  }
  
}
