/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xint.inference.impl.storage;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xint.inference.RequestHistoryEntry;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;



public class InferenceServerManagementRequestHistoryStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(InferenceServerManagementRequestHistoryStorable.class);
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(InferenceServerManagementRequestHistoryStorable.class);
  }


  public List<RequestHistoryEntry> loadAllEntries() {
    try {
      return buildExecutor().execute(new LoadAllEntries());
    } catch (Exception e) {
      throw new RuntimeException("Could not load request history entry");
    }
  }


  public void persistEntry(long requestId, String description) {
    RequestHistoryEntry.Builder builder = new RequestHistoryEntry.Builder();
    builder.description(description);
    builder.requestid(requestId);
    builder.timestamp(System.currentTimeMillis());
    RequestHistoryEntry entry = builder.instance();
    try {
      buildExecutor().execute(new PersistEntry(entry));
    } catch (Exception e) {
      throw new RuntimeException("Could not persist entry");
    }
  }


  public void clearHistory() {
    try {
      buildExecutor().execute(new ClearHistory());
    } catch (Exception e) {
      throw new RuntimeException("Could not clear request history", e);
    }
  }


  private static class ClearHistory implements WarehouseRetryExecutableNoResult {

    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteAll(InferenceServerManagementRequestHistoryStorable.class);
    }

  }

  private static class PersistEntry implements WarehouseRetryExecutableNoResult {

    private final RequestHistoryEntry entry;


    public PersistEntry(RequestHistoryEntry entry) {
      this.entry = entry;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      InferenceServerManagementRequestHistoryStorable converted = convert(entry);
      con.persistObject(converted);
    }

  }

  private static class LoadAllEntries implements WarehouseRetryExecutableNoException<List<RequestHistoryEntry>> {

    @Override
    public List<RequestHistoryEntry> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<RequestHistoryEntry> result = new ArrayList<>();
      Collection<InferenceServerManagementRequestHistoryStorable> data =
          con.loadCollection(InferenceServerManagementRequestHistoryStorable.class);
      for (InferenceServerManagementRequestHistoryStorable item : data) {
        RequestHistoryEntry converted = convert(item);
        result.add(converted);
      }

      result.sort((x, y) -> Long.compare(y.getTimestamp(), x.getTimestamp()));

      return result;
    }

  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
        .storable(InferenceServerManagementRequestHistoryStorable.class);
  }


  private static InferenceServerManagementRequestHistoryStorable convert(RequestHistoryEntry entry) {
    InferenceServerManagementRequestHistoryStorable result = new InferenceServerManagementRequestHistoryStorable();
    result.setDescription(entry.getDescription());
    result.setId(XynaFactory.getInstance().getIDGenerator().getUniqueId(InferenceServerMgmtServiceOperationImpl.ID_GENERATOR_REALM));
    result.setRequestId(entry.getRequestid());
    result.setTimestamp(entry.getTimestamp());
    return result;
  }


  private static RequestHistoryEntry convert(InferenceServerManagementRequestHistoryStorable item) {
    RequestHistoryEntry.Builder builder = new RequestHistoryEntry.Builder();
    builder.timestamp(item.getTimestamp());
    builder.requestid(item.getRequestId());
    builder.description(item.getDescription());
    return builder.instance();
  }
}
