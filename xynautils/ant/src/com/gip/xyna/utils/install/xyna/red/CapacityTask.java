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
package com.gip.xyna.utils.install.xyna.red;



import org.apache.tools.ant.BuildException;



/**
 */
public abstract class CapacityTask extends SOAPTask {

  protected static final String NAMESPACE_MSGS = "http://www.gip.com/xyna/1.5/xsd/factorymanager/capacitymanager/messages/1.1";
  protected static final String NAMESPACE_OPERATION = "http://www.gip.com/xyna/1.5/wsdl/factorymanager/capacitymanager/service/1.1";

  private String name;


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.xfbb.SOAPTask#getService()
   */
  @Override
  protected String getService() {
    return "/CapacityManager/CapacityManager";
  }


  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }


  /**
   * @return the name
   */
  protected String getName() {
    if ((name == null) || name.equals("")) {
      throw new BuildException("Parameter 'name' not set.");
    }
    return name;
  }

}
