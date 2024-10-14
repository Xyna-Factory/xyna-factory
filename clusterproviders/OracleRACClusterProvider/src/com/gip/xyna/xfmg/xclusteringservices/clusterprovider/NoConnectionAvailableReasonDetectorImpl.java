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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.sql.SQLException;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;


/**
 *
 */
public class NoConnectionAvailableReasonDetectorImpl implements NoConnectionAvailableReasonDetector {

  public Reason detect(SQLException sqlException) {
    
    
    int error = sqlException.getErrorCode();
    switch(error) {
    case 1017:
      return Reason.UserOrPasswordInvalid;
    case 28000:
      return Reason.UserOrPasswordInvalid;
    }
    
    String message = sqlException.getMessage();
    if( message == null ) {
      return Reason.Other;
    }
    if( message.contains ("The Network Adapter could not establish the connection")) {
      return Reason.NetworkUnreachable;
    }
    if( message.contains ("Oracle-URL")) {
      return Reason.URLInvalid;
    }
    if( message.contains ("Listener refused the connection")) {
      return Reason.ConnectionRefused;
    }
    
    return Reason.Other;
  }
  
}
