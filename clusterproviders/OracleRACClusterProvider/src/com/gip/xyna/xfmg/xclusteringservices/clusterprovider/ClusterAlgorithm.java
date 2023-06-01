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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterAlgorithmAbstract.ClusterSetupRowsForUpdate;


/**
 *
 */
public interface ClusterAlgorithm {

  /**
   * @param sqlUtils
   * @param interconnect
   * @param binding 
   */
  ClusterState createCluster(SQLUtils sqlUtils, RemoteInterfaceForClusterStateChangesImplAQ interconnect, int binding);

  /**
   * @param sqlUtils
   * @param rows
   * @param newBinding 
   * @param interconnect 
   */
  ClusterState join(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, int newBinding, RemoteInterfaceForClusterStateChangesImplAQ interconnect);

  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect 
   */
  ClusterState restorePrepare(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect);

  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect 
   */
  ClusterState restoreConnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect);

  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect
   * @param newState
   */
  ClusterState changeClusterState(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState);
  
  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect
   * @param newState
   */
  ClusterState disconnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState);

  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect
   */
  ClusterState shutdown(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect);

  /**
   * @param sqlUtils
   * @param rows
   */
  ClusterState leaveCluster(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows);

  /**
   * @param sqlUtils
   * @param rows
   * @param interconnect
   * @return
   */
  ClusterState connect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows,
                       RemoteInterfaceForClusterStateChangesImplAQ interconnect);


}
