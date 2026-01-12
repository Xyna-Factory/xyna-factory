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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;



public class SharedResourceManagement extends Section {

  public static final String DEFAULT_NAME = "SharedResourceManagement";
  private final SharedResourcePortal sharedResourcePortal;

  /**
   * maps from synchronizerTypeName to factory
   */
  private final Map<String, SharedResourceSynchronizerFactory> synchronizerFactories;

  /**
   * maps from synchronizerInstanceIdentifier to synchronizerInstance
   */
  private final Map<String, SharedResourceSynchronizer> synchronizerInstances;

  /**
   * maps from sharedResourceType to synchronizerInstanceIdentifier. 
   * Includes all known sharedResourceTypes including entries with no synchronizer configured
   */
  private final Map<String, String> sharedResourceToSynchronizerMap;


  public SharedResourceManagement() throws XynaException {
    super();
    sharedResourcePortal = new SharedResourcePortal(() -> new FactorySharedResourceRequestRecorder());
    synchronizerFactories = new HashMap<>();
    synchronizerInstances = new HashMap<>();
    sharedResourceToSynchronizerMap = new ConcurrentHashMap<>();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("SharedResourceManagement.loadSynchronizers", "SharedResourceManagement.loadSynchronizers")
        .after(PersistenceLayerInstances.class).execAsync(this::loadSynchronizers);
    fExec.addTask("SharedResourceManagement.instantiateSynchronizers", "SharedResourceManagement.instantiateSynchronizers")
        .after("SharedResourceManagement.loadSynchronizers").execAsync(this::instantiateSynchronizers);
    fExec.addTask(SharedResourceManagement.class, "configureSharedResourceTypes").after("SharedResourceManagement.instantiateSynchronizers")
        .execAsync(this::configureSRTypes);
  }


  private void loadSynchronizers() {
  }


  private void instantiateSynchronizers() {
  }


  private void configureSRTypes() {
  }


  public List<PluginDescription> listSharedResourceSynchronizerDescriptions() {
    List<PluginDescription> result = new ArrayList<>();
    for (SharedResourceSynchronizerFactory factory : synchronizerFactories.values()) {
      result.add(factory.getDescription());
    }
    result.sort((x, y) -> x.getName().compareTo(y.getName()));
    return result;
  }


  /**
   * Registers the given type of shared resource. As a result, this type will be listed in the output
   * of listSharedResourceTypes.
   */
  public void addSharedResource(String type) {
    sharedResourceToSynchronizerMap.putIfAbsent(type, null);
  }


  /**
   * returns a sorted list of all known SharedResourceTypes including entries that are not configured to
   * a SharedResourceSynchronizer.
   */
  public List<SharedResourceTypeStorable> listSharedResourceTypes() {
    List<SharedResourceTypeStorable> result = new ArrayList<>(sharedResourceToSynchronizerMap.size());
    for (String resource : sharedResourceToSynchronizerMap.keySet()) {
      result.add(new SharedResourceTypeStorable(resource, sharedResourceToSynchronizerMap.get(resource)));
    }
    result.sort((x, y) -> x.getSharedResourceTypeIdentifier().compareTo(y.getSharedResourceTypeIdentifier()));
    return result;
  }


  /**
   * returns a sorted list of all synchronizer instances with their description
   */
  public List<NamedSharedResourceSynchronizerDescription> listSynchronizerInstanceDescriptions() {
    List<NamedSharedResourceSynchronizerDescription> result = new ArrayList<>();
    for (Entry<String, SharedResourceSynchronizer> entry : synchronizerInstances.entrySet()) {
      result.add(new NamedSharedResourceSynchronizerDescription(entry.getKey(), entry.getValue().getInstanceDescription()));
    }
    result.sort((x, y) -> x.getName().compareTo(y.getName()));
    return result;
  }


  /**
   * Sets the configuration of the given resource. If synchronizerInstanceIdentifier is null, the
   * resource configuration is deleted.
   */
  public void configureSharedResourceType(String resource, String synchronizerInstanceIdentifier) {
    if (synchronizerInstanceIdentifier == null) {
      sharedResourcePortal.configureSharedResource(resource, null);
      return;
    }
    SharedResourceSynchronizer synchronizer = synchronizerInstances.get(synchronizerInstanceIdentifier);
    if (synchronizer == null) {
      throw new IllegalArgumentException("No SharedResourceSynchronizer '" + synchronizerInstanceIdentifier + "' configured");
    }
    sharedResourcePortal.configureSharedResource(resource, synchronizer);
  }


  /**
   * creates the given SharedResourceInstances
   * @param <T> type of the shared resource
   * @param resource definition of the shared resource
   * @param data shared resource instances to create
   * @return if creation was successful, isSuccess() returns true. If an error occurred, isSuccess() returns false and the
   * exception is accessible using getException(). getResources() will always return null.
   */
  public <T> SharedResourceRequestResult<T> create(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) {
    return sharedResourcePortal.create(resource, data);
  }


  /**
   * Reads the resource instances for the given ids
   * @param <T> type of the shared resource
   * @param resource definition of the shared resource
   * @param ids which shared resource instances to read
   * @return if the read was successful, isSuccess() returns true and getResources() contains the shared resource instances. 
   * If an error occurred, isSuccess() returns false and the exception is accessible using getException(). If some or all of
   * the resources were not present, isSuccess() returns true, but getResources() returns a shorter list than ids.
   */
  public <T> SharedResourceRequestResult<T> read(SharedResourceDefinition<T> resource, List<String> ids) {
    return sharedResourcePortal.read(resource, ids);
  }


  /**
   * Reads all resource instances for the given resource type
   * @param <T> type of the shared resource
   * @param resource definition of the shared resource
   * @return if the read was successful, isSuccess() returns true and getResources() contains all shared resource instances for this type. 
   * If an error occurred, isSuccess() returns false and the exception is accessible using getException().
   */
  public <T> SharedResourceRequestResult<T> readAll(SharedResourceDefinition<T> resource) {
    return sharedResourcePortal.readAll(resource);
  }


  /**
   * Updates the resource instances identified by ids by applying the update function. First, all relevant shared resource instances are
   * loaded. If any resource instance was not found, the update fails. Afterwards, the update function is called for each resource instance.
   * Finally, the updated shared resource instances are persisted. An exception during the update will result in no changes being applyed to
   * any of the involved shared resource instances.  
   * @param <T> type of the shared resource
   * @param resource  definition of the shared resource
   * @param ids which resource instances to update
   * @param update how to update a resource instance. Will be called once for each resource instance. May return null to abort the update.
   * @return if the update was successful, isSuccess() returns true.
   * If an error occurred, isSuccess() returns false and the exception is accessible using getException().
   * If not all shared resource instances were found, the update fails.
   */
  public <T> SharedResourceRequestResult<T> update(SharedResourceDefinition<T> resource, List<String> ids,
                                                   Function<SharedResourceInstance<T>, SharedResourceInstance<T>> update) {
    return sharedResourcePortal.update(resource, ids, update);
  }


  /**
   * Deletes the resource instances identified by ids.
   * @param <T> type of the shared resource
   * @param resource  definition of the shared resource
   * @param ids which resource instances to delete
   * @return if the delete was successful, isSuccess() returns true.
   * If an error occurred, isSuccess() returns false and the exception is accessible using getException().
   * If not all shared resource instances were found, the delete still succeeds. getResources() will always return null.
   */
  public <T> SharedResourceRequestResult<T> delete(SharedResourceDefinition<T> resource, List<String> ids) {
    return sharedResourcePortal.delete(resource, ids);
  }
}
