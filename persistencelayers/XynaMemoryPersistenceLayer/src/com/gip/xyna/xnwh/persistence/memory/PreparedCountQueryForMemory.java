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

package com.gip.xyna.xnwh.persistence.memory;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;



public abstract class PreparedCountQueryForMemory<E> implements IPreparedQueryForMemory<E> {


  private static final Pattern CONTAINS_WHERE_PATTERN = Pattern.compile("^.*\\sWHERE\\s.*", Pattern.CASE_INSENSITIVE);
  private static Pattern removedEscapedQuotationMarks = Pattern.compile("\"");

  private Query<E> query;
  private final PersistenceLayer persistenceLayer;


  public PreparedCountQueryForMemory(Query<E> query, PersistenceLayer persistenceLayer) {
    this.query = query;
    this.persistenceLayer = persistenceLayer;
  }


  protected MemoryBaseResultCountSet getNewResultSet() {
    return new MemoryBaseResultCountSet();
  }


  public PersistenceLayer getPersistenceLayer() {
    return this.persistenceLayer;
  }


  /**
   * achtung, nachdem diese methode aufgerufen wird, m�ssen die angesammelten readlocks wieder freigegeben werden!!
   * (rs.unlockReadLocks();) TODO: in diese methode mit reinnehmen und daf�r den dazwischen auszuf�hrenden code
   * �bergeben (irgendein handler oder sowas)
   */
  public <T extends Storable, X extends MemoryRowData<T>> IMemoryBaseResultSet execute(TableObject<T, X> t,
                                                                                       Parameter p, boolean forUpdate,
                                                                                       int maxRows)
      throws PersistenceLayerException {

    final Parameter escapedParams = escapeParameters(p);
    
    if (forUpdate) {
      throw new XNWH_GeneralPersistenceLayerException("Count queries may not contain a \"for update\" clause");
    }

    // performance
    if (!CONTAINS_WHERE_PATTERN.matcher(query.getSqlString()).matches()) {
      MemoryBaseResultCountSet rs = getNewResultSet();
      rs.setCount(t.getSize(getPersistenceLayer()));
      return rs;
    }

    List<MemoryRowData<T>> copyOfRowData = new ArrayList<MemoryRowData<T>>(t.getSize(getPersistenceLayer()));

    final Lock tableReadLock = t.getTableLock().readLock();
    tableReadLock.lock();
    try {
      Iterator<? extends MemoryRowData<T>> it = t.iterator(getPersistenceLayer());
      while (it.hasNext()) {
        MemoryRowData<T> rd = it.next();
        copyOfRowData.add(rd);
      }
    } finally {
      tableReadLock.unlock();
    }

    int count = 0;

    // select => loop
    Iterator<MemoryRowData<T>> it = copyOfRowData.iterator();
    while (it.hasNext()) {
      MemoryRowData<T> rd = it.next();
      if (checkWhereClauseProxy(rd, escapedParams)) {
        count++;
      }
    }

    MemoryBaseResultCountSet rs = getNewResultSet();
    rs.setCount(count);
    return rs;
  }


  private <T extends Storable> boolean checkWhereClauseProxy(MemoryRowData<T> rd,
                                                             Parameter p) {
    try {
      return checkWhereClause(rd.getData(getPersistenceLayer()), p);
    } catch (UnderlyingDataNotFoundException e) {
      return false;
    }
  }


  protected abstract boolean checkWhereClause(Storable s, Parameter p);


  public String getTable() {
    return query.getTable();
  }
  
  public ResultSetReader<? extends E> getReader() {
    return query.getReader();
  }
  
  public String toString() {
    return query.getSqlString();
  }
  
  private Parameter escapeParameters(Parameter p) {
    if (p == null) {
      return new Parameter();
    }

    boolean[] isLike = query.getLikeParameters();

    Parameter escapedParams = new Parameter();
    for (int i = 0; i < p.size(); i++) {
      Object next = p.get(i);
      if (next instanceof String) {
        escapedParams.add(SelectionParser.escapeParams((String) next, isLike[i], new PreparedQueryForMemory.EscapeForMemory()));
      } else {
        escapedParams.add(next);
      }
    }
    return escapedParams;
  }
  
}
