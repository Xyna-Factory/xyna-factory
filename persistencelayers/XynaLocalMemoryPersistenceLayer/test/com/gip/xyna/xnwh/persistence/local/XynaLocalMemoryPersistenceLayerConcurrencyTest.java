/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xnwh.persistence.local;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.memory.MemoryPersistencelayerMassiveConcurrencyTestCase;
import com.gip.xyna.xnwh.persistence.memory.TestStorable;


public class XynaLocalMemoryPersistenceLayerConcurrencyTest extends MemoryPersistencelayerMassiveConcurrencyTestCase {


  private PersistenceLayer persistencelayer;


  @Override
  public void setUp() throws Exception {
    persistencelayer = new XynaLocalMemoryPersistenceLayer();
    persistencelayer.init(null, "");
    PersistenceLayerConnection con = getPersistenceLayer().getConnection();
    con.addTable(TestStorable.class, false, null);
    con.closeConnection();
  }


  @Override
  public void tearDown() throws Exception {
    persistencelayer.shutdown();
    persistencelayer = null;
  }


  @Override
  public PersistenceLayer getPersistenceLayer() {
    return persistencelayer;
  }


  @Override
  public int getCountForConcurrentInsertAndDeleteAndSelect() {
    return 1000;
  }


  @Override
  public int getCountForConcurrentQueryForUpdate() {
    return 100;
  }

}
