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

package com.gip.xyna.xnwh.persistence.local;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.memory.MemoryPersistenceLayerTransactionTestCase;
import com.gip.xyna.xnwh.persistence.memory.TestStorable;


public class XynaLocalMemoryPersistenceLayerTest extends MemoryPersistenceLayerTransactionTestCase {


  private PersistenceLayer persistencelayer;


  @Override
  public void setUp() throws Exception {

    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();

    EasyMock.replay(fexec);
    
    XynaFactoryBase xynaFactory = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xynaFactory.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xynaFactory.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.replay(xynaFactory);
    XynaFactory.setInstance(xynaFactory);

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
  public int test_13_getCountForOutOfMemoryOnInserts() {
    return 100000;
  }


  @Override
  public int test_14_getCountForOutOfMemoryOnUpdates() {
    return 100000;
  }

}
