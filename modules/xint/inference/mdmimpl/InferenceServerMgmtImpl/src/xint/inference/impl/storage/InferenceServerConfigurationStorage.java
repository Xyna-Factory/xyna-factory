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

import xint.inference.InferenceServerConfiguration;
import xint.inference.InferenceServerConfigurationCreationData;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;



public class InferenceServerConfigurationStorage {


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(InferenceServerConfigurationStorable.class);
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(InferenceServerConfigurationStorable.class);
  }


  public List<InferenceServerConfiguration> loadAllEntries() {
    try {
      return buildExecutor().execute(new LoadAllEntries());
    } catch (Exception e) {
      throw new RuntimeException("Could not load inference server configurations");
    }
  }


  public void persistEntry(InferenceServerConfigurationCreationData entry) {
    try {
      buildExecutor().execute(new PersistEntry(entry));
    } catch (Exception e) {
      throw new RuntimeException("Could not persist inference server configuration");
    }
  }


  public void deleteEntry(InferenceServerConfiguration entry) {
    try {
      buildExecutor().execute(new DeleteEntry(entry));
    } catch (Exception e) {
      throw new RuntimeException("Could not persist inference server configuration");
    }
  }


  private static class DeleteEntry implements WarehouseRetryExecutableNoResult {

    private final InferenceServerConfiguration entry;


    public DeleteEntry(InferenceServerConfiguration entry) {
      this.entry = entry;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      InferenceServerConfigurationStorable storable = new InferenceServerConfigurationStorable();
      storable.setId(entry.getInternalId());
      con.deleteOneRow(storable);
    }
  }

  private static class PersistEntry implements WarehouseRetryExecutableNoResult {

    private final InferenceServerConfigurationCreationData entry;


    public PersistEntry(InferenceServerConfigurationCreationData entry) {
      this.entry = entry;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      InferenceServerConfigurationStorable storable = convert(entry);
      con.persistObject(storable);
    }

  }


  private static class LoadAllEntries implements WarehouseRetryExecutableNoException<List<InferenceServerConfiguration>> {

    @Override
    public List<InferenceServerConfiguration> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<InferenceServerConfiguration> result = new ArrayList<>();
      Collection<InferenceServerConfigurationStorable> collection = con.loadCollection(InferenceServerConfigurationStorable.class);
      for (InferenceServerConfigurationStorable item : collection) {
        InferenceServerConfiguration converted = convert(item);
        result.add(converted);
      }
      return result;
    }

  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
        .storable(InferenceServerConfigurationStorable.class);
  }


  private static InferenceServerConfigurationStorable convert(InferenceServerConfigurationCreationData entry) {
    InferenceServerConfigurationStorable result = new InferenceServerConfigurationStorable();
    String[] parts = entry.getId().split("/");
    String additionals = entry.getAdditionalParameters() == null ? null : entry.getAdditionalParameters().replace("\n", " ");
    result.setAdditionalParameters(additionals);
    result.setContextWindowSize(entry.getContextWindowSize());
    result.setDescription(entry.getDescription());
    result.setId(XynaFactory.getInstance().getIDGenerator().getUniqueId(InferenceServerMgmtServiceOperationImpl.ID_GENERATOR_REALM));
    result.setModel(entry.getModel());
    result.setPort(entry.getPort());
    result.setServerVersion(parts[1]);
    result.setType(parts[0]);
    return result;
  }


  private static InferenceServerConfiguration convert(InferenceServerConfigurationStorable item) {
    InferenceServerConfiguration.Builder builder = new InferenceServerConfiguration.Builder();
    builder.additionalParameters(item.getAdditionalParameters());
    builder.contextWindowSize(item.getContextWindowSize());
    builder.description(item.getDescription());
    builder.id(String.format("%s/%s", item.getType(), item.getServerVersion()));
    builder.internalId(item.getId());
    builder.model(item.getModel());
    //pid is not persistend in the storable
    builder.port(item.getPort());
    builder.serverVersion(item.getServerVersion());
    builder.serverType(item.getType());
    return builder.instance();
  }
}
