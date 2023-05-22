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
package com.gip.xyna.xprc.xsched.orderseries;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;

/**
 *
 */
public interface OSMCache {

  public static class SearchResult {
    public enum Type {
      NotFound, OtherBinding, Found;
    }
    
    private Type type;
    private long id;
    private int binding;
    private String correlationId;
    
    private SearchResult(Type type, long id, int binding, String correlationId) {
      this.type = type;
      this.id = id;
      this.binding = binding;
      this.correlationId = correlationId;
    }

    public Type getType() {
      return type;
    }
    public int getBinding() {
      return binding;
    }
    public long getId() {
      return id;
    }
    public String getCorrelationId() {
      return correlationId;
    }
    
    public static SearchResult notFound() {
      return new SearchResult( Type.NotFound, 0, 0, null );
    }
    public static SearchResult found(long id, int binding, String correlationId, int ownBinding) {
      return new SearchResult( 
        ownBinding == binding ? Type.Found : Type.OtherBinding,
        id, binding, correlationId );
    }
    
    @Override
    public String toString() {
      return "SearchResult("+type+","+id+","+binding+")";
    }

  }

  
  /**
   * @param correlationId
   * @return
   */
  SeriesInformationStorable get(String correlationId);

  SeriesInformationStorable refresh(String correlationId);
  
  /**
   * @param orderId
   */
  SearchResult search(long orderId);


  /**
   * @param sis
   */
  void update(SeriesInformationStorable sis);


  /**
   * @param sis
   * @throws XPRC_DUPLICATE_CORRELATIONID 
   * @throws XNWH_GeneralPersistenceLayerException 
   */
  void insert(SeriesInformationStorable sis) throws XPRC_DUPLICATE_CORRELATIONID, XNWH_GeneralPersistenceLayerException;


  /**
   * @param correlationId
   */
  void lock(String correlationId);


  /**
   * @param correlationId
   */
  void unlock(String correlationId);


  /**
   * @param correlationId
   * @return
   */
  boolean tryLock(String correlationId);


  /**
   * @param correlationId
   */
  void remove(String correlationId);

  /**
   * @param id
   */
  void remove(long id) throws XNWH_GeneralPersistenceLayerException;



}
