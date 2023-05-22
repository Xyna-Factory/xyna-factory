/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Date;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Uptime;



public class UptimeImpl extends XynaCommandImplementation<Uptime> {

  public void execute(OutputStream statusOutputStream, Uptime payload) throws XynaException {
    writeLineToCommandLine(statusOutputStream, "Server started: "
        + Constants.defaultUTCSimpleDateFormat().format(new Date(XynaFactory.STARTTIME)));
    writeLineToCommandLine(statusOutputStream, "Uptime: "
        + ListconnectionpoolinfoImpl.calcTimeDiff(System.currentTimeMillis(), XynaFactory.STARTTIME));

  }

}
