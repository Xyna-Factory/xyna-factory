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
package xmcp.gitintegration.storage;



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

import xmcp.gitintegration.FactoryXmlIgnoreEntry;



public class FactoryXmlIgnoreEntryStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(FactoryXmlIgnoreEntryStorable.class);
  }


  public void addFactoryXmlIgnoreEntry(String configTyp, String value) {
    try {
      buildExecutor().execute(new AddFactoryXmlIgnoreEntry(configTyp, value));
    } catch (Exception e) {
      throw new RuntimeException("Could not add user to Repository");
    }
  }


  public List<FactoryXmlIgnoreEntry> listAllFactoryXmlIgnoreEntries() {
    try {
      return buildExecutor().execute(new ListAllFactoryXmlIgnoreEntries());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void removeFactoryXmlIgnoreEntry(String configType, String value) {
    try {
      buildExecutor().execute(new RemoveFactoryXmlIgnoreEntry(configType, value));
    } catch (PersistenceLayerException e) {
      new RuntimeException(e);
    }
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(FactoryXmlIgnoreEntryStorable.class);
  }


  private static class AddFactoryXmlIgnoreEntry implements WarehouseRetryExecutableNoResult {

    private String configType;
    private String value;


    public AddFactoryXmlIgnoreEntry(String configType, String value) {
      this.configType = configType;
      this.value = value;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      FactoryXmlIgnoreEntryStorable storable = new FactoryXmlIgnoreEntryStorable(configType, value);
      con.persistObject(storable);
    }

  }

  private static class ListAllFactoryXmlIgnoreEntries implements WarehouseRetryExecutableNoException<List<FactoryXmlIgnoreEntry>> {

    @Override
    public List<FactoryXmlIgnoreEntry> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return con.loadCollection(FactoryXmlIgnoreEntryStorable.class).stream().map(x -> convert(x)).collect(Collectors.toList());
    }

  }

  private static class RemoveFactoryXmlIgnoreEntry implements WarehouseRetryExecutableNoResult {

    private String configType;
    private String value;


    public RemoveFactoryXmlIgnoreEntry(String configType, String value) {
      this.configType = configType;
      this.value = value;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      FactoryXmlIgnoreEntryStorable storable = new FactoryXmlIgnoreEntryStorable(configType, value);
      con.deleteOneRow(storable);
    }
  }


  private static FactoryXmlIgnoreEntry convert(FactoryXmlIgnoreEntryStorable storable) {

    if (storable == null) {
      throw new RuntimeException("Can't convert null value to FactoryXmlIgnoreEntry");
    }

    FactoryXmlIgnoreEntry.Builder result = new FactoryXmlIgnoreEntry.Builder();
    result.configType(storable.getConfigtype());
    result.value(storable.getValue());

    return result.instance();
  }
}
