/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xmcp.gitintegration.impl.tracking;



public class CollectingTracker implements OperationTracker {

  private final StringBuilder infoMessages = new StringBuilder();
  private final StringBuilder errorMessages = new StringBuilder();


  @Override
  public void trackInfo(String message) {
    infoMessages.append(message).append("\n");
  }


  @Override
  public void trackError(String message) {
    errorMessages.append(message).append("\n");
  }


  public String getInfoMessages() {
    return infoMessages.toString();
  }


  public String getErrorMessages() {
    return errorMessages.toString();
  }

}
