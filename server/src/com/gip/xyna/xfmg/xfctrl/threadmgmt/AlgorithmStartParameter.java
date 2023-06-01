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
package com.gip.xyna.xfmg.xfctrl.threadmgmt;

import java.util.Collections;
import java.util.List;

public class AlgorithmStartParameter {

  private final boolean allowRestart;
  private final List<String> startParams;
  
  
  public AlgorithmStartParameter(boolean allowRestart) {
    this(allowRestart, Collections.emptyList());
  }
  
  public AlgorithmStartParameter(boolean allowRestart, List<String> startParams) {
    this.allowRestart = allowRestart;
    this.startParams = startParams;
  }

  public boolean isRestartAllowed() {
    return allowRestart;
  }
  
  public List<String> getStartParameter() {
    return startParams;
  }

}
