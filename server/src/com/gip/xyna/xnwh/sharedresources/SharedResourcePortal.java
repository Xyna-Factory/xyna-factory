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
package com.gip.xyna.xnwh.sharedresources;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class SharedResourcePortal {

  private static final Logger logger = CentralFactoryLogging.getLogger(SharedResourcePortal.class);

  private final NoSynchronizerConfiguredSharedResourceSynchronizer defaultSynchronizer;
  private final Map<String, SharedResourceSynchronizer> synchronizers;
  private final SharedResourceRequestRecorderFactory recorderFactory;


  public SharedResourcePortal(SharedResourceRequestRecorderFactory recorderFactory) {
    defaultSynchronizer = new NoSynchronizerConfiguredSharedResourceSynchronizer();
    synchronizers = new HashMap<>();
    this.recorderFactory = recorderFactory;
  }


  public void configureSharedResource(String type, SharedResourceSynchronizer synchronizer) {
    if (synchronizer != null) {
      synchronizers.put(type, synchronizer);
      if (logger.isInfoEnabled()) {
        logger.info("Configured " + type + " to " + synchronizer.getInstanceDescription());
      }
    } else {
      SharedResourceSynchronizer oldSynchronizer = synchronizers.remove(type);
      if (logger.isInfoEnabled()) {
        if (oldSynchronizer != null) {
          logger.info("Deleted configuration of " + type + " from synchronizer " + oldSynchronizer.getInstanceDescription());
        } else {
          logger.info("Deleted configuration of " + type + ". There was no synchronizer configured.");
        }
      }
    }
  }


  private <T> SharedResourceRequestResult<T> makeRequest(String type, String path, RequestFunction<T> func) {
    SharedResourceSynchronizer synchronizer = synchronizers.getOrDefault(path, defaultSynchronizer);
    SharedResourceRequestRecorder recorder = recorderFactory.createRecoder();
    SharedResourceRequestResult<T> result = null;
    recorder.recordStartRequest(type, path, synchronizer.getInstanceDescription());
    try {
      result = func.apply(synchronizer);
    } catch (Exception e) {
      result = new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      recorder.recordEndRequest(result);
    }
    return result;
  }


  public <T> SharedResourceRequestResult<T> create(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) {
    return makeRequest("create", resource.getPath(), s -> s.create(resource, data));
  }


  public <T> SharedResourceRequestResult<T> read(SharedResourceDefinition<T> resource, List<String> ids) {
    return makeRequest("read", resource.getPath(), s -> s.read(resource, ids));
  }


  public <T> SharedResourceRequestResult<T> readAll(SharedResourceDefinition<T> resource) {
    return makeRequest("readAll", resource.getPath(), s -> s.readAll(resource));
  }


  public <T> SharedResourceRequestResult<T> update(SharedResourceDefinition<T> resource, List<String> ids,
                                                   Function<SharedResourceInstance<T>, SharedResourceInstance<T>> update) {
    return makeRequest("update", resource.getPath(), s -> s.update(resource, ids, update));
  }


  public <T> SharedResourceRequestResult<T> delete(SharedResourceDefinition<T> resource, List<String> ids) {
    return makeRequest("delete", resource.getPath(), s -> s.delete(resource, ids));
  }


  private interface RequestFunction<T> {

    SharedResourceRequestResult<T> apply(SharedResourceSynchronizer synchronizer);
  }
}
