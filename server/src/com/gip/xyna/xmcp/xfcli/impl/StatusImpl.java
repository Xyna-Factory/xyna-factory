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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Status;



public class StatusImpl extends XynaCommandImplementation<Status> {

  private static final Logger logger = CentralFactoryLogging.getLogger(StatusImpl.class);


  public void execute(OutputStream statusOutputStream, Status payload) throws XynaException {
    ReturnCode rc = XynaFactory.getStatusCodeSLESLike();
    if (logger.isDebugEnabled()) {
      logger.debug("Sending current state code <" + rc.getCode() + "> to CLI");
    }
    /*
     * ACHTUNG: verboseflag wird im xynafactory.sh in extendedstatus-aufruf umgewandelt
     */
    writeEndToCommandLine(statusOutputStream, rc);
  }

}
