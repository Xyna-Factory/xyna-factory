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

package com.gip.xyna.xfmg.xfmon.systeminfo;


public class MEMInfo {

  /*
   * These are saved as Strings to avoid treating units
   */
  private final String maxMem;
  private final String freeMem;
  private final String maxSwap;
  private final String freeSwap;


  public MEMInfo(String maxMem, String freeMem, String maxSwap, String freeSwap) {
    this.maxMem = maxMem;
    this.freeMem = freeMem;
    this.maxSwap = maxSwap;
    this.freeSwap = freeSwap;
  }


  public String getMaxMem() {
    return maxMem;
  }


  public String getFreeMem() {
    return freeMem;
  }


  public String getMaxSwap() {
    return maxSwap;
  }


  public String getFreeSwap() {
    return freeSwap;
  }

}
