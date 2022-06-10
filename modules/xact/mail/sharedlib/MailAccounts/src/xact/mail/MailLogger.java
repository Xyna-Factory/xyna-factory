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
package xact.mail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MailLogger extends OutputStream {
  
  private StringBuilder buffer;
  private Logger logger;
  private Level level;
  
  public MailLogger(Logger logger, Level level) {
    this.buffer = new StringBuilder();
    this.logger = logger;
    this.level = level;
  }

  @Override
  public void write(int b) throws IOException {
    if( b != '\n') {
      buffer.append((char)b);
    } else {
      //leider hilft hier als callerFQCN PrintStream, MailLogger und OutputStream nicht...
      logger.log(PrintStream.class.getCanonicalName(), level, buffer.toString(), null);
       buffer.setLength(0);
    }
  }
  
  public static PrintStream createLogging(Logger logger, Level level) {
    return new PrintStream(new MailLogger(logger,level) );
  }

}
