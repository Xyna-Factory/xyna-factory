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
package com.gip.xyna.xnwh.sharedresources;



import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;



public class SharedResourceTypeStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(SharedResourceTypeStorable.class);
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(SharedResourceTypeStorable.class);
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(SharedResourceTypeStorable.class);
  }


  public List<SharedResourceTypeStorable> listAllTypes() {
    try {
      return buildExecutor().execute(new ListAllTypes());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void storeType(String sharedResourceTypeIdentifier, String synchronizerInstanceIdentifier) {
    try {
      buildExecutor().execute(new StoreType(new SharedResourceTypeStorable(sharedResourceTypeIdentifier, synchronizerInstanceIdentifier)));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class ListAllTypes implements WarehouseRetryExecutableNoException<List<SharedResourceTypeStorable>> {

    @Override
    public List<SharedResourceTypeStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return con.loadCollection(SharedResourceTypeStorable.class).stream().collect(Collectors.toList());
    }

  }

  private static class StoreType implements WarehouseRetryExecutableNoResult {

    private String sharedResourceTypeIdentifier;
    private String synchronizerInstanceIdentifier;


    public StoreType(SharedResourceTypeStorable type) {
      this.sharedResourceTypeIdentifier = type.getPrimaryKey().toString();
      this.synchronizerInstanceIdentifier = type.getSynchronizerInstanceIdentifier();
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      SharedResourceTypeStorable type = new SharedResourceTypeStorable(sharedResourceTypeIdentifier, synchronizerInstanceIdentifier);
      con.persistObject(type);
    }
  }

}
