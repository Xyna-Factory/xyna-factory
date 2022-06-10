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
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.ActiveMQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.OracleAQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.WebSphereMQConnectData;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Registerqueue;



public class RegisterqueueImpl extends XynaCommandImplementation<Registerqueue> {

  public void execute(OutputStream statusOutputStream, Registerqueue payload) throws XynaException {
    try {
      checkParameter("uniqueName", payload.getUniqueName(), statusOutputStream);
      checkParameter("externalName", payload.getExternalName(), statusOutputStream);
      checkParameter("queueType", payload.getQueueType(), statusOutputStream);
    }
    catch (Exception e) {
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
    if (qtype == QueueType.ORACLE_AQ) {
      if (connectParams.length != 3) {
        writeLineToCommandLine(statusOutputStream, "Error: Connect parameter missing.");
        throw new IllegalArgumentException();
      }
      checkParameter("userName", connectParams[0], statusOutputStream);
      checkParameter("password", connectParams[1], statusOutputStream);
      checkParameter("jdbcUrl", connectParams[2], statusOutputStream);
      OracleAQConnectData connectData = new OracleAQConnectData();
      connectData.setUserName(connectParams[0]);
      connectData.setPassword(connectParams[1]);
      connectData.setJdbcUrl(connectParams[2]);

      XynaFactory.getInstance().getFactoryManagement().registerQueue(payload.getUniqueName(),
                  payload.getExternalName(), QueueType.ORACLE_AQ, connectData);
    }
    else if (qtype == QueueType.WEBSPHERE_MQ) {
      if (connectParams.length != 4) {
        writeLineToCommandLine(statusOutputStream, "Error: Wrong number of connect parameters.");
        throw new IllegalArgumentException();
      }
      WebSphereMQConnectData connectData = new WebSphereMQConnectData();
      connectData.setQueueManager(checkParameter("queueManager", connectParams[0], statusOutputStream));
      connectData.setHostname(checkParameter("hostname", connectParams[1], statusOutputStream));
      connectData.setPort(checkParameter("port", connectParams[2], statusOutputStream));
      connectData.setChannel(checkParameter("channel", connectParams[3], statusOutputStream));

      XynaFactory.getInstance().getFactoryManagement().registerQueue(payload.getUniqueName(),
                  payload.getExternalName(), QueueType.WEBSPHERE_MQ, connectData);
    }
    else if (qtype == QueueType.ACTIVE_MQ) {
      if (connectParams.length != 2) {
        writeLineToCommandLine(statusOutputStream, "Error: Wrong number of connect parameters.");
        throw new IllegalArgumentException();
      }
      ActiveMQConnectData connectData = new ActiveMQConnectData();
      connectData.setHostname(checkParameter("hostname", connectParams[0], statusOutputStream));
      String portVal = checkParameter("port", connectParams[1], statusOutputStream);
      try {
        connectData.setPort(Integer.parseInt(portVal));
      }
      catch (Exception e) {
        writeLineToCommandLine(statusOutputStream, "Error: Cannot parse int: " + portVal);
        throw new IllegalArgumentException();
      }

      XynaFactory.getInstance().getFactoryManagement().registerQueue(payload.getUniqueName(),
                  payload.getExternalName(), QueueType.ACTIVE_MQ, connectData);
    }
  }


  private String checkParameter(String name, String value, OutputStream statusOutputStream) {
    if (isNullOrEmpty(value)) {
      writeLineToCommandLine(statusOutputStream, "Error: Argument " + name + " is missing or empty.");
      throw new IllegalArgumentException();
    }
    return value;
  }


  private boolean isNullOrEmpty(String s) {
    if (s == null) {
      return true;
    }
    if (s.length() < 1) {
      return true;
    }
    return false;
  }

}
