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
package xdnc.dhcpv6.cluster;

import com.gip.xyna.xdnc.dhcp.DHCPClusterManagement;
import com.gip.xyna.xdnc.dhcp.DHCPClusterManagements;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;


public class DHCPv6ClusterManagement implements Clustered {

  public DHCPv6ClusterManagement() {
  }
  
  private long clusterInstanceId = -1;
  private DHCPClusterManagement clusterManagement;
  
  public boolean isClustered() {
    return clusterInstanceId > -1;
  }

  public long getClusterInstanceId() {
    return clusterInstanceId;
  }

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    this.clusterInstanceId = clusterInstanceId;
    //dhcpv6 ist die id dieses clustermanagements. im filter sollte man das gleiche angeben.
    clusterManagement = DHCPClusterManagements.get("dhcpv6", clusterInstanceId);
  }

  public void disableClustering() {
  }

  public String getName() {
    return DHCPv6ClusterManagement.class.getSimpleName();
  }

  public DHCPClusterManagement getClusterMgmt() {
    if (clusterManagement == null) {
      throw new RuntimeException("accessed too early. cluster is not yet configured for this component.");
    }
    return clusterManagement;
  }
  
}
