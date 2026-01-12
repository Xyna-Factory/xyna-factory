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



import java.util.List;
import java.util.function.Function;



public class NoSynchronizerConfiguredSharedResourceSynchronizer implements SharedResourceSynchronizer {

  @Override
  public String getInstanceDescription() {
    return "No synchronizer configured for this resource type";
  }


  @Override
  public void start() {
    //ntbd
  }


  @Override
  public void stop() {
    //ntbd
  }


  @Override
  public <T> SharedResourceRequestResult<T> create(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) {
    return createResult(resource.getPath());
  }


  @Override
  public <T> SharedResourceRequestResult<T> read(SharedResourceDefinition<T> resource, List<String> ids) {
    return createResult(resource.getPath());
  }


  @Override
  public <T> SharedResourceRequestResult<T> readAll(SharedResourceDefinition<T> resource) {
    return createResult(resource.getPath());
  }


  @Override
  public <T> SharedResourceRequestResult<T> update(SharedResourceDefinition<T> resource, List<String> ids,
                                                   Function<SharedResourceInstance<T>, SharedResourceInstance<T>> update) {
    return createResult(resource.getPath());
  }


  @Override
  public <T> SharedResourceRequestResult<T> delete(SharedResourceDefinition<T> resource, List<String> ids) {
    return createResult(resource.getPath());
  }


  private <T> SharedResourceRequestResult<T> createResult(String resource) {
    IllegalStateException exception = new IllegalStateException("No SharedResourceSynchronizer configured for " + resource + ".");
    return new SharedResourceRequestResult<T>(false, exception, null);
  }


}
