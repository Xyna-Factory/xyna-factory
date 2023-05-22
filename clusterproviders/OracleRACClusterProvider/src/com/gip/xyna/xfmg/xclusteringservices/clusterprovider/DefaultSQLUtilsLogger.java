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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xnwh.persistence.XynaSQLUtilsLogger;



public class DefaultSQLUtilsLogger extends XynaSQLUtilsLogger {

  public interface DBProblemHandler {

    public void dbNotReachable();
    
    public void setException(Exception exception);
  }
  
  private DBProblemHandler dbProblemHandler;

  public DefaultSQLUtilsLogger(Logger logger, DBProblemHandler dbProblemHandler, Level loglevel) {
    super(logger, SQLUtils.class.getName(), loglevel);
    this.dbProblemHandler = dbProblemHandler;
  }

  /**
   * sorgt daf�r, dass bei jedem sql fehler eine RuntimeException geworfen wird.
   */
  public void logException(Exception e) {
    internalLog(Level.ERROR, "", e);
    dbProblemHandler.setException(e);
    throw new SQLRuntimeException(e);
  }


}
