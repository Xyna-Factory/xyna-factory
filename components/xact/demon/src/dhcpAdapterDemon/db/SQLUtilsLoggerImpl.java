/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db;

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;

public class SQLUtilsLoggerImpl implements SQLUtilsLogger {

  private Exception lastException; 
  private long lastExceptionDate;

  private Logger logger;
  public SQLUtilsLoggerImpl(Logger logger) {
    this.logger = logger;
  }

  public void logException(Exception e) {
    lastException = e;
    lastExceptionDate = System.currentTimeMillis();
    internalLog( Level.ERROR, e.getClass().getName() + ": " + e.getMessage(), e );
  }

  public void logSQL(String sql) {
    internalLog( Level.DEBUG, sql, null );
  }

  public Exception getLastException() {
    return lastException;
  }
  
  /**
   * @return
   */
  public Date getLastExceptionDate() {
    return new Date( lastExceptionDate );
  }

  
  /**
   * Sorgt auf interessante Weise für das richtige Setzen des
aufrufenden Zeile: (keine Zeile dieser Datei, sondern der Aufrufer)
   * siehe
http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Category.html
   * http://marc.info/?l=log4j-user&m=99859247618691&w=2
   * That's why you need to provide the fully-qualified classname. If you
use the wrapper I showed, you will not have this problem. That is, the
class and method name of the wrapper caller, not the wrapper itself,
will be logged. The code which determines the logging method looks one
past the fully-qualified classname in the callstack.
   * @param level
   * @param str
   * @param t
   */
  private void internalLog( Level level, String str, Throwable t ) {
    logger.log(SQLUtils.class.getName(), level, str, t);
  }
}