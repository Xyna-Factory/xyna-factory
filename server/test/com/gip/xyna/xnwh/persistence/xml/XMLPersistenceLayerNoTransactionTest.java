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
package com.gip.xyna.xnwh.persistence.xml;


import junit.framework.AssertionFailedError;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;

public class XMLPersistenceLayerNoTransactionTest extends XMLPersistenceLayerTest {


  @Override
  public TransactionMode getTransactionMode() {
    return TransactionMode.NO_TRANSACTION;
  }
  
  
  @Override
  public void testTryInfluencePersistingObject() {
    try {
      super.testTryInfluencePersistingObject();
      fail("XMLPersistenceLayer with transactionMode " + getTransactionMode() + " is not supposed to pass this test.");
    } catch (AssertionFailedError e) {
      // aye
    } catch (Exception e) {
      if (e instanceof XNWH_GeneralPersistenceLayerException && 
          e.getCause() != null && 
          e.getCause() instanceof XPRC_XmlParsingException) {
        // that's why we're never used anymore /sigh
      } else if (e instanceof XNWH_GeneralPersistenceLayerException && 
                 e.getCause() != null &&
                 e.getCause() instanceof XNWH_GeneralPersistenceLayerException && 
                 e.getCause().getCause() != null &&
                 e.getCause().getCause() instanceof XPRC_XmlParsingException) {
        // ...maybe?
      } else if (e instanceof XNWH_GeneralPersistenceLayerException && 
                      e.getCause() != null &&
                      e.getCause() instanceof XNWH_GeneralPersistenceLayerException && 
                      e.getCause().getCause() != null &&
                      e.getCause().getCause() instanceof Ex_FileAccessException) {
        // granted...that might happen as well /pout
      } else {
        e.printStackTrace();
        fail("unexpected error: ");
      }
    }
  }


}
