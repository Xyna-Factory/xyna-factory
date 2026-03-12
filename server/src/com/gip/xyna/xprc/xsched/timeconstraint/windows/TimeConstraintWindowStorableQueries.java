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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.List;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;


public class TimeConstraintWindowStorableQueries {

  private static final String loadAllByNameQuerySqlString =
      "select * from " + TimeConstraintWindowStorable.TABLE_NAME + " where " + TimeConstraintWindowStorable.COL_NAME + " = ? order by "
          + TimeConstraintWindowStorable.COL_SUB_ID + " asc";
  private static final String loadAllQuerySqlString =
      "select * from " + TimeConstraintWindowStorable.TABLE_NAME + " order by " + TimeConstraintWindowStorable.COL_NAME + " , "
          + TimeConstraintWindowStorable.COL_SUB_ID + " asc";

  
  private PreparedQuery<TimeConstraintWindowStorable> loadAllByNameQuery;
  private PreparedQuery<TimeConstraintWindowStorable> loadAllQuery;
  
  public void init(ODSConnection defCon) throws PersistenceLayerException {
    loadAllByNameQuery =
        defCon.prepareQuery(new Query<TimeConstraintWindowStorable>(loadAllByNameQuerySqlString,
            TimeConstraintWindowStorable.reader, TimeConstraintWindowStorable.TABLE_NAME), true);
    loadAllQuery =
        defCon.prepareQuery(new Query<TimeConstraintWindowStorable>(loadAllQuerySqlString,
            TimeConstraintWindowStorable.reader, TimeConstraintWindowStorable.TABLE_NAME), true);
  }
  
  public List<TimeConstraintWindowStorable> loadAllByName(ODSConnection con, String name) throws PersistenceLayerException {
    return con.query(loadAllByNameQuery, new Parameter(name), -1);
  }

  public List<TimeConstraintWindowStorable> loadAll(ODSConnection con) throws PersistenceLayerException {
    return con.query(loadAllQuery, new Parameter(), -1);
  }

  
}
