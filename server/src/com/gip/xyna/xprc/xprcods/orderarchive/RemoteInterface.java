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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;



public interface RemoteInterface {

  public Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> searchConnectionType(OrderInstanceSelect select,
                                                                                                             int maxRows,
                                                                                                             long startTime,
                                                                                                             SearchMode searchMode,
                                                                                                             ODSConnectionType connectionType,
                                                                                                             List<OrderInstance> selectedPreCommittedOrdersFromDEFAULT)
      throws PersistenceLayerException;


  public int sendCountQueryForConnectionType(OrderInstanceSelect select, ODSConnectionType connectionType) throws PersistenceLayerException;


  public OrderInstanceDetails getCompleteOrder(long id) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

}
