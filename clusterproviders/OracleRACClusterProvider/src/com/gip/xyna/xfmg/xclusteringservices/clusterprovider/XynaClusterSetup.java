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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;


public class XynaClusterSetup {


  public static final String CLUSTER_SETUP_TABLE_NAME = "xynaclustersetup";
  public static final String COL_BINDING = "binding";
  public static final String COL_STATE = "state";
  public static final String COL_IS_ONLINE = "isonline";


  static ResultSetReader<XynaClusterSetup> reader = new ClusterSetupResultSetReader();


  private static class ClusterSetupResultSetReader implements ResultSetReader<XynaClusterSetup> {

    public XynaClusterSetup read(ResultSet rs) throws SQLException {
      int binding = rs.getInt(XynaClusterSetup.COL_BINDING);
      String state = rs.getString(XynaClusterSetup.COL_STATE);
      boolean online = rs.getBoolean(COL_IS_ONLINE);
      return new XynaClusterSetup(binding, state, online);
    }

  }


  public int binding;
  public String state;
  public boolean online;


  public XynaClusterSetup(int binding, String state, boolean online) {
    this.binding = binding;
    this.state = state;
    this.online = online;
  }


  public int getBinding() {
    return binding;
  }


  public ClusterState getState() {
    return ClusterState.valueOf(state);
  }


  public boolean isOnline() {
    return online;
  }

}
