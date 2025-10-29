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
package com.gip.xyna.xprc.xsched.vetos;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public class VetoSearch {

  
  private static PreparedQueryCache cache = new PreparedQueryCache();
  
  
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    try {
      return searchInternally(select, maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      cache.clear();
      return searchInternally(select, maxRows);
    }
  }
  
  
  private VetoSearchResult searchInternally(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    String selectString;
    ResultSetReader<VetoInformationStorable> reader = select.getReader();
    String selectCountString;
    Parameter paras = select.getParameter();
    try {
      selectString = select.getSelectString();
      selectCountString = select.getSelectCountString();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("Problem with select statement: " + e.getMessage(), e);
    }
    int countAll = 0;
    List<VetoInformationStorable> viss = new ArrayList<VetoInformationStorable>();

    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<VetoInformationStorable> query = cache.getQueryFromCache(selectString, con, reader, VetoInformationStorable.TABLE_NAME);

      viss.addAll(con.query(query, paras, maxRows));
      if (maxRows >= 0 && viss.size() >= maxRows) {
        PreparedQuery<? extends OrderCount> queryCount = cache.getQueryFromCache(selectCountString, con,
                                                                                 OrderCount.getCountReader(),
                                                                                 VetoInformationStorable.TABLE_NAME);
        OrderCount count = con.queryOneRow(queryCount, paras);
        countAll = count.getCount();
      } else {
        countAll = viss.size();
      }
    } finally {
      con.closeConnection();
    }
    return new VetoSearchResult(viss, countAll);
  }

}
