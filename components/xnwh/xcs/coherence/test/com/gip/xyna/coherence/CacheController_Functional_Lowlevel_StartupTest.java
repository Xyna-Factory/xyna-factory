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

package com.gip.xyna.coherence;



import java.util.List;

import junit.framework.TestCase;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;



public class CacheController_Functional_Lowlevel_StartupTest extends TestCase {

  public void test01_CreateCN1_Create1_CreateCN2_Read2() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payload1Restored2 = cc2.read(objectId);
    assertEquals(payload1, payload1Restored2);

    cc1.shutdown();
    cc2.shutdown();
  }

}
