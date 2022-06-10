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

import com.gip.xyna.utils.exceptions.XynaException;


public class OrderInstanceTest extends TestCase {

  private OrderInstance newOrderInstance(long id, String orderType) {
    OrderInstance oi = new OrderInstance(id);
    oi.setOrderType(orderType);
    return oi;
  }

  public void testInitialization() {
    OrderInstance wfi = newOrderInstance(new Long(123), "testWorkflowFqString1");
    assertEquals("Unexpected instance ID", 123, wfi.getId());
    assertEquals("Unexpected instance name", "testWorkflowFqString1", wfi.getOrderType());
    assertEquals("Unexpected default instance status", OrderInstanceStatus.INITIALIZATION, wfi.getStatusAsEnum());
    assertEquals("Unexpected default parent ID", -1, wfi.getParentId());
  }


  public void testSerializability() throws XynaException {

    // create outputstream that does not write anywhere
    OutputStream nullOutputStream = new OutputStream() {
      public void write(int b) throws IOException {
        return;
      }
    };
    
    OrderInstance wfi = newOrderInstance(new Long(123), "testWorkflowFqString2");
    wfi.setStatus(OrderInstanceStatus.FINISHED_CLEANUP);

    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(nullOutputStream);
    }
    catch (IOException e) {
      fail("Could not create object output stream");
    }

    try {
      oos.writeObject(wfi);
    }
    catch (NotSerializableException e2) {
      fail("Could not write " + OrderInstance.class.getName() + " to object output stream: " + e2.getMessage());
    }
    catch (IOException e) {
      fail(e.getClass().getName() + " while writing object output stream");
    }

  }

}
