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

package com.gip.xyna.xnwh.persistence;



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;



public interface ODS extends ODSAdministration {


  public static final int FUTURE_EXECUTION_ID__PERSISTENCE_LAYER_INSTANCES =
      XynaFactory.getInstance().getFutureExecution().nextId();

  public static final int FUTURE_EXECUTION_ID__PREINIT_XML_PERSISTENCE_LAYER =
      XynaFactory.getInstance().getFutureExecutionForInit().nextId();
  
  public static final int FUTURE_EXECUTION_ID__PREINIT_PERSISTENCE_LAYER_INSTANCES =
    XynaFactory.getInstance().getFutureExecutionForInit().nextId();


  /**
   * ruft {@link #openConnection(ODSConnectionType)} mit ODSConnectionType.DEFAULT auf
   */
  public ODSConnection openConnection();


  /**
   * requests �ber diese connection gehen an alle konfigurierten persistenzschichten, deren type gleich dem
   * �bergebenen ist.
   */
  public ODSConnection openConnection(ODSConnectionType conType);


  public <T extends Storable<?>> ClusterProvider getClusterInstance(ODSConnectionType conType, Class<T> storableClass);


  public <T extends Storable<?>> long getClusterInstanceId(ODSConnectionType conType, Class<T> storableClass);

}
