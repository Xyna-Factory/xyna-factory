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

package com.gip.xyna.xfmg.xfmon.systeminfo;



public class CPUInfo {

  private final int cpuId;
  private final String vendorName;
  private final String modelName;
  private final int cpuMhz;


  public CPUInfo(int cpuId, String vendorName, String modelName, int mhz) {
    this.cpuId = cpuId;
    this.vendorName = vendorName;
    this.modelName = modelName;
    this.cpuMhz = mhz;
  }


  public int getCpuID() {
    return cpuId;
  }


  public String getVendorName() {
    return vendorName;
  }


  public String getModelname() {
    return modelName;
  }


  public int getCpuMhz() {
    return cpuMhz;
  }


  public String toString() {
    return "CPU " + cpuId + " ### Vendor: " + vendorName + ", Model Name: " + modelName + ", MHz: " + cpuMhz + " ###";
  }

}
