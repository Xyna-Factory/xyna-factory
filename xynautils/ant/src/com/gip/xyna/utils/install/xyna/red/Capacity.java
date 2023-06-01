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
package com.gip.xyna.utils.install.xyna.red;



import org.apache.tools.ant.BuildException;



/**
 */
public class Capacity {

  private String name;
  private long usage = -1;


  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }


  /**
   * @return the name
   */
  public String getName() {
    if ((name == null) || name.equals("")) {
      throw new BuildException("Parameter 'name' not set.");
    }
    return name;
  }


  /**
   * @param usage the usage to set
   */
  public void setUsage(long usage) {
    this.usage = usage;
  }


  /**
   * @return the usage
   */
  public long getUsage() {
    if (usage < 0) {
      throw new BuildException("Parameter 'usage' not set.");
    }
    return usage;
  }

}
