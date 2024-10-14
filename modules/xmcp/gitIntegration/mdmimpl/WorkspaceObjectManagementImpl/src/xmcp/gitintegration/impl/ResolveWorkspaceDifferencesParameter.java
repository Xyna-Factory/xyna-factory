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
package xmcp.gitintegration.impl;



import java.util.Optional;



public class ResolveWorkspaceDifferencesParameter {

  private long workspaceDifferenceListId;
  private Optional<Long> entry;
  private Optional<String> resolution; //SimpleName of class
  private boolean all;
  private boolean close;


  public long getWorkspaceDifferenceListId() {
    return workspaceDifferenceListId;
  }


  public void setWorkspaceDifferenceListId(long workspaceDifferenceListId) {
    this.workspaceDifferenceListId = workspaceDifferenceListId;
  }


  public Optional<Long> getEntry() {
    return entry;
  }


  public void setEntry(Optional<Long> entry) {
    this.entry = entry;
  }


  public Optional<String> getResolution() {
    return resolution;
  }


  public void setResolution(Optional<String> resolution) {
    this.resolution = resolution;
  }


  public boolean getAll() {
    return all;
  }


  public void setAll(boolean all) {
    this.all = all;
  }


  public boolean getClose() {
    return close;
  }


  public void setClose(boolean close) {
    this.close = close;
  }
}
