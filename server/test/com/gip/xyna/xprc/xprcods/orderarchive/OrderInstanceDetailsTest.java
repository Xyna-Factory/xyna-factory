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

package com.gip.xyna.xprc.xprcods.orderarchive;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;


public class OrderInstanceDetailsTest extends TestCase {
  
  private static final Logger logger = Logger.getLogger(OrderInstanceDetailsTest.class.getName());

  private OrderInstanceDetails newOrderInstanceDetails(long id, String orderType) {
    OrderInstanceDetails oid = new OrderInstanceDetails(id);
    oid.setOrderType(orderType);
    return oid;
  }
  
  public void testInitialization() {
    OrderInstanceDetails wfiDetails = newOrderInstanceDetails((long) 10203, "testOrderInstanceDetailsString1");
    assertNotNull(wfiDetails.getStartTime());
    assertEquals(OrderInstanceStatus.INITIALIZATION, wfiDetails.getStatusAsEnum());
  }


  public void testSerializability() throws XynaException {

    // create outputstream that does not write anywhere
    OutputStream nullOutputStream = new OutputStream() {
      public void write(int b) throws IOException {
        return;
      }
    };

    OrderInstanceDetails wfiDetails = newOrderInstanceDetails((long) 10203, "testOrderInstanceDetailsString2");
    wfiDetails.setLastUpdate(12345);
    wfiDetails.addException(new XynaException("testException"));
    wfiDetails.setMonitoringLevel(3);
    wfiDetails.setExecutionType(ExecutionType.JAVA_DESTINATION);
    wfiDetails.setStatus(OrderInstanceStatus.WAITING_FOR_VETO);

    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(nullOutputStream);
    }
    catch (IOException e) {
      fail("Could not create object output stream");
    }

    try {
      oos.writeObject(wfiDetails);
    }
    catch (NotSerializableException e2) {
      logger.error("", e2);
      fail("Could not write " + OrderInstanceDetails.class.getName() + " to object output stream");
    }
    catch (IOException e) {
      fail(e.getClass().getName() + " while writing object output stream");
    }

  }

}
