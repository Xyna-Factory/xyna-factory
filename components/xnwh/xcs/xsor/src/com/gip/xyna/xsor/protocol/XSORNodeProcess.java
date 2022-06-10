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

package com.gip.xyna.xsor.protocol;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.persistence.PersistenceStrategy.XSORPayloadPersistenceBean;



public class XSORNodeProcess {
  
  private static final Logger logger=Logger.getLogger(XSORNodeProcess.class.getName());
  
  public static void create(XSORPayloadPersistenceBean bean, XSORMemory xsorMemory) {
    XSORPayload payload = bean.getXSORPayload();;
    int objectIndex = xsorMemory.freeListGet();
    if (xsorMemory.testPkIndexPut(payload.getPrimaryKey(), objectIndex)){
      
    }
    payload.copyIntoByteArray(xsorMemory.getData(objectIndex), xsorMemory.getOffsetBytes(objectIndex));
    xsorMemory.setState(objectIndex, 'S');
    xsorMemory.updateChecksumAndModificationtimeAndReleaseTime(objectIndex, bean.getModificationTime(), bean.getReleaseTime());
    xsorMemory.updateIndex(objectIndex, payload, -1, null);

  }

}
