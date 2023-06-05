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
package com.gip.xyna.xmcp.xfcli.undisclosed;



import java.io.IOException;
import java.io.Writer;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;



public class PrintOrderTimings implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    if (allArgs.getArgCount() > 0 ) {
      long id = Long.valueOf(allArgs.getArg(0));
      CentralFactoryLogging.printOrderTimingForOrder(writer(clw), id);
    } else {
      CentralFactoryLogging.printAllOrderTimings(writer(clw));
    }
  }


  private Writer writer(final CommandLineWriter clw) { //FIXME CommandLineWriter extends Writer?
    return new Writer() {

      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        clw.writeToCommandLine(String.valueOf(cbuf, off, len));
      }


      @Override
      public void flush() throws IOException {

      }


      @Override
      public void close() throws IOException {

      }

    };
  }
}
