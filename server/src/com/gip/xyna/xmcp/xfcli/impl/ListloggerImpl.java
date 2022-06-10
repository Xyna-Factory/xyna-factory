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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listlogger;



public class ListloggerImpl extends XynaCommandImplementation<Listlogger> {

  public void execute(OutputStream statusOutputStream, Listlogger payload) throws XynaException {
    Map<String, Level> loggerList = CentralFactoryLogging.listLogger(!payload.getVerbose());
    
    List<String> loggerOutputStringList = new ArrayList<String>();    
    for (Entry<String, Level> logger : loggerList.entrySet()) {
      if (logger.getKey().length() == 0) {
        continue;
      }
      if (logger.getValue() != null) {
        StringBuilder loggerOutputStringBuilder = new StringBuilder();
        loggerOutputStringList.add(loggerOutputStringBuilder.append(logger.getKey())
                                                            .append(": ")
                                                            .append(logger.getValue())
                                                            .toString());
      } else {
        loggerOutputStringList.add(logger.getKey());
      }
    }
    Collections.sort(loggerOutputStringList);
    StringBuilder listLoggerOutput = new StringBuilder("Listing information for ")
                                                         .append(loggerOutputStringList.size())
                                                         .append(" loggers:\n\n");
    for (String loggerString : loggerOutputStringList) {
      listLoggerOutput.append(loggerString)
                      .append("\n");
    }
    writeLineToCommandLine(statusOutputStream, listLoggerOutput.toString());
  }

}
