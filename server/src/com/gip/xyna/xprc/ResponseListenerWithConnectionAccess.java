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
package com.gip.xyna.xprc;

import com.gip.xyna.xnwh.persistence.ODSConnection;

public abstract class ResponseListenerWithConnectionAccess extends ResponseListener {

  private static final long serialVersionUID = 1L;


  /**
   * Gets the DEFAULT connection
   * @return The default connection container for use within the Listener
   */
  protected ODSConnection getDefaultConnection() {
    return super.getDefaultConnection();
  }


  /**
   * Gets the HISTORY connection
   * @return The history connection container for use within the Listener
   */
  protected  ODSConnection getHistoryConnection() {
    return super.getHistoryConnection();
  }
  
}
