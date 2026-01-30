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



public class SharedResourceSynchronizerStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(SharedResourceSynchronizerInstanceStorable.class);
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(SharedResourceSynchronizerInstanceStorable.class);
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
        .storable(SharedResourceSynchronizerInstanceStorable.class);
  }


  public List<SharedResourceSynchronizerInstance> listAllInstances() {
    try {
      return buildExecutor().execute(new ListAllInstances());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void storeInstance(SharedResourceSynchronizerInstance instance) {
    if (instance == null) {
      throw new IllegalArgumentException("Cannot store null instance");
    }
    try {
      buildExecutor().execute(new StoreInstance(instance));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class ListAllInstances implements WarehouseRetryExecutableNoException<List<SharedResourceSynchronizerInstance>> {

    @Override
    public List<SharedResourceSynchronizerInstance> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return con.loadCollection(SharedResourceSynchronizerInstanceStorable.class).stream().map(x -> convert(x))
          .collect(Collectors.toList());
    }

  }

  private static class StoreInstance implements WarehouseRetryExecutableNoResult {

    private final SharedResourceSynchronizerInstance instance;


    public StoreInstance(SharedResourceSynchronizerInstance instance) {
      this.instance = instance;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      SharedResourceSynchronizerInstanceStorable storable =
          new SharedResourceSynchronizerInstanceStorable(instance.getInstanceName(), instance.getTypeName(), instance.getConfiguration(),
                                                         instance.getStatus());
      con.persistObject(storable);
    }
  }


  private static SharedResourceSynchronizerInstance convert(SharedResourceSynchronizerInstanceStorable storable) {
    SharedResourceSynchronizerInstance instance = new SharedResourceSynchronizerInstance();
    instance.setInstanceName(storable.getInstanceName());
    instance.setTypeName(storable.getSynchronizerTypeName());
    instance.setConfiguration(storable.getConfiguration());
    try {
      SharedResourceSynchronizerInstance.Status status = SharedResourceSynchronizerInstance.Status.valueOf(storable.getStatus());
      instance.setStatus(status);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid status value: " + storable.getStatus(), e);
    }
    return instance;
  }
}
