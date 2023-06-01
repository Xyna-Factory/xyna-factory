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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.utils.db.SQLUtils;

public class DefaultValidationStrategy implements ValidationStrategy {

  private final AtomicLong validationInterval;
  
  public DefaultValidationStrategy(long validationInterval) {
    this.validationInterval = new AtomicLong(validationInterval);
  }

  public boolean isValidationNecessary(long currentTime, long lastcheck) {
    return currentTime - lastcheck >= validationInterval.get();
  }

  public Exception validate(Connection con) {
    return validateConnection(con);
  }

  public void setValidationInterval(long validationInterval) {
    this.validationInterval.set(validationInterval);
  }
  
  public long getValidationInterval() {
   return this.validationInterval.get();
  }

  private Exception validateConnection(Connection con) {
    SQLUtils checkUtils = createCheckUtils(con);
    try {
      checkUtils.queryInt("select 1 from dual", null);
      return checkUtils.getLastException();
    } catch (RuntimeException e) {
      return e;
    }
  }

  private SQLUtils createCheckUtils(Connection con) {
    SQLUtils checkUtils = new SQLUtils(con);
    checkUtils.setQueryTimeout(10);
    return checkUtils;
  }

  public boolean rebuildConnectionAfterFailedValidation() {
    return true;
  }

}
