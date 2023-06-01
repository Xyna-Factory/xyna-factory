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

package com.gip.xyna.xfmg;



import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xods.components.Components;
import com.gip.xyna.xfmg.xopctrl.XynaOperatorControl;



public abstract class XynaFactoryManagementBase extends Department implements XynaFactoryManagementPortal {

  public XynaFactoryManagementBase() throws XynaException {
    super();
  }
  
  public abstract Components getComponents();

  public abstract XynaFactoryControl getXynaFactoryControl();

  public abstract XynaOperatorControl getXynaOperatorControl();

  public abstract XynaClusteringServicesManagementInterface getXynaClusteringServicesManagement();

  public abstract XynaExtendedStatusManagement getXynaExtendedStatusManagement();
}
