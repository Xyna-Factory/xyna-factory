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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



class SQLStrings {

  private SQLStrings() {
  }


  static final String SELECT_OWN_BINDING_FOR_UPDATE_SQL = "select * from " + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME
      + " where " + XynaClusterSetup.COL_BINDING + "=?" + " for update";

  static final String SELECT_ALL_BINDINGS_FOR_UPDATE_SQL = "select * from " + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME
      + " for update order by " + XynaClusterSetup.COL_BINDING;

  static final String INSERT_INTO_CLUSTERSETUP_SQL = "insert into "
      + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME.toUpperCase() + " (" + XynaClusterSetup.COL_BINDING + ", "
      + XynaClusterSetup.COL_STATE + ", " + XynaClusterSetup.COL_IS_ONLINE + ") values (?, ?, ?)";

  static final String SELECT_ONLY_BINDINGS_SQL = "select " + XynaClusterSetup.COL_BINDING + " from "
      + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME;

  static final String UPDATE_STATE_FOR_BINDING_SQL = "update " + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME + " set "
      + XynaClusterSetup.COL_STATE + "=?, " + XynaClusterSetup.COL_IS_ONLINE + "=? where " + XynaClusterSetup.COL_BINDING
      + " = ?";

  static final String DELETE_FOR_BINDING_SQL = "delete from " + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME + " where "
      + XynaClusterSetup.COL_BINDING + "=?";

}
