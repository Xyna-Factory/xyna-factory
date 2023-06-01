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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Registerqueue;



public class RegisterqueueImpl extends XynaCommandImplementation<Registerqueue> {

  public void execute(OutputStream statusOutputStream, Registerqueue payload) throws XynaException {
    try {
      QueueManagement.checkParameter("uniqueName", payload.getUniqueName());
      QueueManagement.checkParameter("externalName", payload.getExternalName());
      QueueManagement.checkParameter("queueType", payload.getQueueType());
    }
    catch (Exception e) {
      writeLineToCommandLine(statusOutputStream, e.getMessage());
      throw new IllegalArgumentException(e);
    }
    QueueType qtype = null;
    try {
      qtype = QueueType.valueOf(payload.getQueueType());
    }
    catch (Exception e) {
      writeLineToCommandLine(statusOutputStream, "Error: Unknown queue type " + payload.getQueueType());
      throw new IllegalArgumentException("", e);
    }
    String[] connectParams = payload.getConnectParameters();
    if ((connectParams == null) || (connectParams.length < 1)) {
      writeLineToCommandLine(statusOutputStream, "Error: Connect parameter missing.");
      throw new IllegalArgumentException();
    }
    
    try {
      QueueConnectData connectData = QueueManagement.createQueueConnectData(qtype, connectParams);
      XynaFactory.getInstance().getFactoryManagement().registerQueue(payload.getUniqueName(), payload.getExternalName(),
                                                                     qtype, connectData);
    } catch (IllegalArgumentException e) {
      writeLineToCommandLine(statusOutputStream, e.getMessage());
      throw e;
    }
  }

}
