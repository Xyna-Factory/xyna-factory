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

  private static final SharedResourceDefinition<Long> resource = new KryoSerializedSharedResourceDefinition<Long>(
      IDGenerator.XYNA_IDGENERATION_SR, Long.class);

  private class IdBlock {

    public AtomicLong nextId;
    public long blockEnd; // exclusive

    private IdBlock(long blockStart, long blockEnd) {
      this.nextId = new AtomicLong(blockStart);
      this.blockEnd = blockEnd;
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
    ids.putIfAbsent(realm, new IdBlock(0, 0));
    IdBlock idBlock = ids.get(realm);

    long id = idBlock.nextId.getAndIncrement();
    if (id < idBlock.blockEnd) {
      // fast path: ID is within the current block, return it
      return id;
    }
    synchronized (idBlock) {
      // double-check if another thread has already fetched a new block
      if (idBlock.nextId.get() >= idBlock.blockEnd) {
        SharedResourceRequestResult<Long> result = sharedResourceManagement.read(resource, Arrays.asList(realm));
        if (!result.isSuccess()) {
          throw new RuntimeException(
              String.format("Failed to read shared resource %s for realm %s: %s", IDGenerator.XYNA_IDGENERATION_SR,
                  realm, result.getException().getMessage()),
              result.getException());
        }
        List<SharedResourceInstance<Long>> resources = result.getResources();
        long blockSize = blockSizes.getOrDefault(realm, DEFAULT_BLOCK_SIZE);
        long created = System.currentTimeMillis();
        if (resources.isEmpty()) {
          idBlock.blockEnd = blockSize;
          SharedResourceInstance<Long> instance = new SharedResourceInstance<>(realm, created, idBlock.blockEnd);
          sharedResourceManagement.create(resource, Arrays.asList(instance));
        } else {
          sharedResourceManagement.update(resource, Arrays.asList(realm), current -> {
            idBlock.nextId.set(current.getValue());
            idBlock.blockEnd = current.getValue() + blockSize;
            SharedResourceInstance<Long> instance = new SharedResourceInstance<>(realm, created, idBlock.blockEnd);
            return instance;
          });
        }
      }
      return idBlock.nextId.getAndIncrement();
    }
  }

  @Override
  public long getIdLastUsedByOtherNode(String realm) {
    throw new UnsupportedOperationException(
        "Method 'getIdLastUsedByOtherNode' is not supported using shared resources");
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
