package com.gip.xyna.idgeneration;



import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;



public class IdGenerationAlgorithmUsingBlocksAndSharedResources implements IdGenerationAlgorithm {

  private SharedResourceManagement sharedResourceManagement;

  private final ConcurrentHashMap<String, IdBlock> ids = new ConcurrentHashMap<>();
  private final Map<String, Long> blockSizes = new HashMap<>();

  private static final long DEFAULT_BLOCK_SIZE = 1000;

  private static final SharedResourceDefinition<Long> resource =
      new KryoSerializedSharedResourceDefinition<Long>(IDGenerator.XYNA_IDGENERATION_SR, Long.class);


  private class IdBlock {

    private long nextId;
    private long blockEnd; // exclusive


    private IdBlock(long blockStart, long blockEnd) {
      this.nextId = blockStart;
      this.blockEnd = blockEnd;
    }


    public boolean hasNext() {
      return nextId < blockEnd;
    }


    public long getNext() {
      if (!hasNext()) {
        throw new IllegalStateException("No more IDs available in the current block");
      }
      return nextId++;
    }
  }


  public IdGenerationAlgorithmUsingBlocksAndSharedResources() {
  }


  @Override
  public void init() throws PersistenceLayerException {
    sharedResourceManagement = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  @Override
  public void shutdown() throws PersistenceLayerException {
  }


  @Override
  public long getUniqueId(String realm) {

    // if a previously allocated block has remaining IDs, use them
    IdBlock idBlock = ids.get(realm);
    if (idBlock != null && idBlock.hasNext()) {
      return idBlock.getNext();
    }

    // otherwise, allocate a new block starting from the last allocated ID (or 0 if no block has been allocated yet)
    SharedResourceRequestResult<Long> result = sharedResourceManagement.read(resource, Arrays.asList(realm));
    if (result.isSuccess()) {
      List<SharedResourceInstance<Long>> resources = result.getResources();
      long blockSize = blockSizes.getOrDefault(realm, DEFAULT_BLOCK_SIZE);
      long created = System.currentTimeMillis();
      if (resources.isEmpty()) {
        idBlock = new IdBlock(0, blockSize);
        SharedResourceInstance<Long> instance = new SharedResourceInstance<>(realm, created, idBlock.blockEnd);
        sharedResourceManagement.create(resource, Arrays.asList(instance));
      } else {
        AtomicLong blockStart = new AtomicLong();
        AtomicLong blockEnd = new AtomicLong();
        sharedResourceManagement.update(resource, Arrays.asList(realm), current -> {
          blockStart.set(current.getValue());
          blockEnd.set(blockStart.get() + blockSize);
          SharedResourceInstance<Long> instance = new SharedResourceInstance<>(realm, created, blockEnd.get());
          return instance;
        });
        idBlock = new IdBlock(blockStart.get(), blockEnd.get());
      }
      ids.put(realm, idBlock);
      return idBlock.getNext();
    } else {
      throw new RuntimeException(String.format("Failed to read shared resource %s for realm %s: %s", IDGenerator.XYNA_IDGENERATION_SR,
                                               realm, result.getException().getMessage()),
                                 result.getException());
    }

  }


  @Override
  public long getIdLastUsedByOtherNode(String realm) {
    throw new UnsupportedOperationException("Method 'getIdLastUsedByOtherNode' is not supported using shared resources");
  }


  @Override
  public void storeLastUsed(String realm) {
    throw new UnsupportedOperationException("Method 'storeLastUsed' is not supported using shared resources");
  }


  @Override
  public void setBlockSize(String realm, long blockSize) {
    blockSizes.put(realm, blockSize);
  }
}
