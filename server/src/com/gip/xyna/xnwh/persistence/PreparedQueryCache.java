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

package com.gip.xyna.xnwh.persistence;



import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;



/**
 * queries f�r zb memory persistencelayer sind sehr teuer zu erstellen (generierte klasse, compile etc).
 */
public class PreparedQueryCache {

  //cache
  private Map<Object, Search> cachedSearches = new HashMap<Object, Search>();


  private static class Search {

    private PreparedQuery<?> query;
    private long lastUse;

  }
  
  public interface SQLStringGenerator {

    public String generateSQLString();
  }


  private final long cachetimeout;
  private final long cachecheck_interval;
  private AtomicLong lastcacheTimeoutCheck = new AtomicLong(System.currentTimeMillis());


  public PreparedQueryCache() {
    this(24 * 60 * 60 * 1000L, 60 * 60 * 1000L);
  }


  public PreparedQueryCache(long cacheTimeout, long cacheCheckInterval) {
    this.cachetimeout = cacheTimeout;
    this.cachecheck_interval = cacheCheckInterval;
  }


  public <E> PreparedQuery<E> getQueryFromCache(final String sqlString, ODSConnection con, ResultSetReader<E> reader)
      throws PersistenceLayerException {
    return getQueryFromCache(sqlString + con.getConnectionType(), new SQLStringGenerator() {

      public String generateSQLString() {
        return sqlString;
      }

    }, con, reader);
  }


  /**
   * achtung, diese queries h�ren nicht auf �nderungen an persistencelayers und werden durch �nderungen an dem
   * zugeh�rigen persistencelayer ung�ltig. Das ist aber nicht so schlimm, weil man die
   * {@link XNWH_IncompatiblePreparedObjectException} fangen kann und dann den cache einfach leeren mit {@link #clear()}.
   */
  public <E> PreparedQuery<E> getQueryFromCache(Object keyForQueryIdentification,
                                                SQLStringGenerator sqlStringGenerator, ODSConnection con,
                                                ResultSetReader<E> reader) throws PersistenceLayerException {

    Search search = cachedSearches.get(keyForQueryIdentification);
    if (search == null) {
      synchronized (cachedSearches) {
        search = cachedSearches.get(keyForQueryIdentification);
        if (search == null) {
          PreparedQuery<E> query = con.prepareQuery(new Query<E>(sqlStringGenerator.generateSQLString(), reader));
          search = new Search();
          search.query = query;
          cachedSearches.put(keyForQueryIdentification, search);
        }
      }
    }

    long now = System.currentTimeMillis();
    search.lastUse = now;
    if (now - lastcacheTimeoutCheck.get() > cachecheck_interval) {
      // TODO the following code should be performed asynchronously by an extra thread to save a
      // little time if many queries exist within the cache 
      //check old queries:
      synchronized (cachedSearches) {
        if (now - lastcacheTimeoutCheck.get() > cachecheck_interval) {
          lastcacheTimeoutCheck.set(now);
          Iterator<Search> it = cachedSearches.values().iterator();
          while (it.hasNext()) {
            Search s = it.next();
            if (now - s.lastUse > cachetimeout) {
              it.remove();
            }
          }
        }
      }
    }

    return (PreparedQuery<E>) search.query;
  }


  public void clear() {
    synchronized (cachedSearches) {
      //holzhammermethode
      cachedSearches.clear();
    }
  }


}
