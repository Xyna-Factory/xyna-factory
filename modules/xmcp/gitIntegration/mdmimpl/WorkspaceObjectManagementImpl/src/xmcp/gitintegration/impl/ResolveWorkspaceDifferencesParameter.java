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

  private Optional<Long> entry;
  private Optional<String> resolution; //SimpleName of class

  public Optional<Long> getEntry() {
    return entry;
  }


  public void setEntry(Optional<Long> entry) {
    this.entry = entry;
  }

  public void setEntry(Long entry) {
    this.entry = Optional.ofNullable(entry);
  }

  public Optional<String> getResolution() {
    return resolution;
  }


  public void setResolution(Optional<String> resolution) {
    this.resolution = resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = Optional.ofNullable(resolution);
  }

}
