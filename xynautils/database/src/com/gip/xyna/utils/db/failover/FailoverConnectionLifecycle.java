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
package com.gip.xyna.utils.db.failover;

import com.gip.xyna.utils.db.SQLUtils;

/**
 * FailoverConnectionLifecycle knows how to initialize and finalize a sqlUtils-instance.
 *
 */
public interface FailoverConnectionLifecycle {
  
  /**
   * called, when a new Connection is made 
   * @param sqlUtils
   */
  public void initialize(SQLUtils sqlUtils);
  
  /**
   * called, when a Connection has to be closed.
   * After this call to sqlUtils.rollback() and sqlUtils.closeConnection() is made.
   * @param sqlUtils
   */
  public void finalize(SQLUtils sqlUtils);
  
}
