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

package com.gip.xyna.xprc.xprcods.abandonedorders;



import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;



public abstract class AbandonedOrderDetectionRule<T extends AbandonedOrderDetails> {
  
  protected Logger logger = CentralFactoryLogging.getLogger(getClass());
  
  private boolean deepSearch;
  
  public AbandonedOrderDetectionRule(boolean deepSearch) {
    this.deepSearch = deepSearch;
  }
  
  
  public abstract List<T> detect(int maxrows) throws PersistenceLayerException;
  public abstract void resolve(T information) throws ResolveForAbandonedOrderNotSupported;
  public abstract void forceClean(AbandonedOrderDetails information);
  public abstract void forceCleanFamily(AbandonedOrderDetails information);
  public abstract String describeProblem(T information);
  public abstract String getShortName();
  public abstract String describeSolution();
  
  
  @SuppressWarnings("unchecked")
  public final Class<T> getDetailClassType() {
    Type type = getClass().getGenericSuperclass();
    ParameterizedType paramType = (ParameterizedType) type;
    return (Class<T>) paramType.getActualTypeArguments()[0];
 
  }
  
  public final boolean isDeepSearch() {
    return deepSearch;
  }
  
}
