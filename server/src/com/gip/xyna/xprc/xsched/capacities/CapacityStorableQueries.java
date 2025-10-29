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
package com.gip.xyna.xprc.xsched.capacities;



import java.util.List;

import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xsched.CapacityStorable;



public class CapacityStorableQueries {

  // immer sortieren wegen deadlock gefahren
  private static final String loadAllQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " order by " + ClusteredStorable.COL_BINDING + " asc";
  private static final String loadOwnBindingQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + ClusteredStorable.COL_BINDING + " = ? order by "
          + ClusteredStorable.COL_BINDING + " asc";
  private static final String loadOwnBindingForUpdateQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + ClusteredStorable.COL_BINDING + " = ? order by "
          + ClusteredStorable.COL_BINDING + " asc for update";
  private static final String loadOwnBindingByNameQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + ClusteredStorable.COL_BINDING + " = ? and "
          + CapacityStorable.COL_NAME + " = ?"; //braucht keine sortierung, weil nur eine zeile
  private static final String loadOwnBindingByNameForUpdateQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + ClusteredStorable.COL_BINDING + " = ? and "
          + CapacityStorable.COL_NAME + " = ? for update"; // braucht keine sortierung, weil nur eine zeile
  private static final String loadAllBindingsByNameQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + CapacityStorable.COL_NAME + " = ? order by "
          + ClusteredStorable.COL_BINDING + " asc";
  private static final String loadAllBindingsByNameForUpdateQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " where " + CapacityStorable.COL_NAME + " = ? order by "
          + ClusteredStorable.COL_BINDING + " asc for update";
  private static final String loadAllForUpdateQuerySqlString =
      "select * from " + CapacityStorable.TABLE_NAME + " order by " + ClusteredStorable.COL_BINDING + " asc for update";


  private PreparedQuery<CapacityStorable> loadByBindingQuery;
  private PreparedQuery<CapacityStorable> loadByBindingForUpdateQuery;
  private PreparedQuery<CapacityStorable> loadByBindingAndNameQuery;
  private PreparedQuery<CapacityStorable> loadByBindingAndNameForUpdateQuery;
  private PreparedQuery<CapacityStorable> loadAllBindingsByNameQuery;
  private PreparedQuery<CapacityStorable> loadAllBindingsByNameForUpdateQuery;
  private PreparedQuery<CapacityStorable> loadAllQuery;
  private PreparedQuery<CapacityStorable> loadAllForUpdateQuery;


  public void init(ODSConnection defCon) throws PersistenceLayerException {
    loadByBindingAndNameForUpdateQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadOwnBindingByNameForUpdateQuerySqlString,
                                                        CapacityStorable.reader, CapacityStorable.TABLE_NAME), true);
    loadByBindingAndNameQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadOwnBindingByNameQuerySqlString, CapacityStorable.reader,
                            CapacityStorable.TABLE_NAME), true);
    loadAllBindingsByNameQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadAllBindingsByNameQuerySqlString, CapacityStorable.reader,
                            CapacityStorable.TABLE_NAME), true);

    loadAllBindingsByNameForUpdateQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadAllBindingsByNameForUpdateQuerySqlString,
                                                        CapacityStorable.reader,
                                                        CapacityStorable.TABLE_NAME), true);
    loadByBindingForUpdateQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadOwnBindingForUpdateQuerySqlString, CapacityStorable.reader,
                                                        CapacityStorable.TABLE_NAME), true);
    loadByBindingQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadOwnBindingQuerySqlString, CapacityStorable.reader,
                            CapacityStorable.TABLE_NAME), true);
    loadAllQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadAllQuerySqlString, CapacityStorable.reader,
                            CapacityStorable.TABLE_NAME), true);
    loadAllForUpdateQuery =
        defCon.prepareQuery(new Query<CapacityStorable>(loadAllForUpdateQuerySqlString, CapacityStorable.reader, 
                            CapacityStorable.TABLE_NAME), true);

  }


  public List<CapacityStorable> loadAllByBinding(ODSConnection con, int binding) throws PersistenceLayerException {
    return con.query(loadByBindingQuery, new Parameter(binding), -1);
  }


  public List<CapacityStorable> loadAllByBindingForUpdate(ODSConnection con, int binding)
      throws PersistenceLayerException {
    return con.query(loadByBindingForUpdateQuery, new Parameter(binding), -1);
  }


  public List<CapacityStorable> loadAllByName(ODSConnection con, String name) throws PersistenceLayerException {
    return con.query(loadAllBindingsByNameQuery, new Parameter(name), -1);
  }


  public List<CapacityStorable> loadAllByNameForUpdate(ODSConnection con, String name) throws PersistenceLayerException {
    return con.query(loadAllBindingsByNameForUpdateQuery, new Parameter(name), -1);
  }


  public CapacityStorable getByBindingAndName(ODSConnection con, int binding, String capName)
      throws PersistenceLayerException {
    return con.queryOneRow(loadByBindingAndNameQuery, new Parameter(binding, capName));
  }


  public List<CapacityStorable> loadByBindingAndNameForUpdate(ODSConnection con, int binding, String capName)
      throws PersistenceLayerException {
    return con.query(loadByBindingAndNameForUpdateQuery, new Parameter(binding, capName), -1);
  }


  public List<CapacityStorable> loadAllForUpdate(ODSConnection con) throws PersistenceLayerException {
    return con.query(loadAllForUpdateQuery, new Parameter(), -1);
  }


  public List<CapacityStorable> loadAll(ODSConnection con) throws PersistenceLayerException {
    return con.query(loadAllQuery, new Parameter(), -1);
  }


}
