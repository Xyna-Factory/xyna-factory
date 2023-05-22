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

package com.gip.xyna.xnwh.xclusteringservices;

import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface;


public class XynaClusteringServices extends Section {


  public static final String DEFAULT_NAME = "Xyna Clustering Services";


  private ClusterLockingInterface clusterLockingInterface;


  public XynaClusteringServices() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    clusterLockingInterface = new ClusterLockingInterface();
    deployFunctionGroup(clusterLockingInterface);
  }


  public ClusterLockingInterface getClusterLockingInterface() {
    return clusterLockingInterface;
  }

}
