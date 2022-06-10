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

package com.gip.juno.ws.tools;

import java.util.Stack;


import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.xyna.utils.db.SQLUtils;

/**
 * Class that caches SQLUtils in two stacks, one for primary failover, one for secondary
 */
public class FailoverStack {

  public Stack<SQLUtils> primaryStack = new Stack<SQLUtils>();
  public Stack<SQLUtils> secondaryStack = new Stack<SQLUtils>();
  
  public SQLUtils pop(FailoverFlag flag) {
    SQLUtils ret = null;    
    if (flag == FailoverFlag.primary) {
      ret = primaryStack.pop();
    } else if (flag == FailoverFlag.secondary) {
      ret = secondaryStack.pop();
    }    
    return ret;
  }
  
  public boolean empty(FailoverFlag flag) {
    if (flag == FailoverFlag.primary) {
      return primaryStack.empty();
    } else if (flag == FailoverFlag.secondary) {
      return secondaryStack.empty(); 
    }
    return true;
  }
  
  public void push(FailoverFlag flag, SQLUtils utils) {
    if (flag == FailoverFlag.primary) {
      primaryStack.push(utils);
    } else if (flag == FailoverFlag.secondary) {
      secondaryStack.push(utils);
    }
  }
  
  public void push(SQLUtilsContainer container) {
    if (container.getFailOverFlag() == FailoverFlag.primary) {
      primaryStack.push(container.getSQLUtils());
    } else if (container.getFailOverFlag() == FailoverFlag.secondary) {
      secondaryStack.push(container.getSQLUtils());
    }
  }
  
  public SQLUtilsContainerForManagement popForAdmin(DBSchema schema, FailoverFlag flag) {
    return new SQLUtilsContainerForManagement(pop(flag), schema, flag); 
  }

  public SQLUtilsContainerForLocation popForLocation(String location, LocationSchema schema, FailoverFlag flag) {
    return new SQLUtilsContainerForLocation(pop(flag), location, flag, schema); 
  }
  
  public int size(FailoverFlag flag) {
    if (flag == FailoverFlag.primary) {
      return primaryStack.size();
    } else if (flag == FailoverFlag.secondary) {
      return secondaryStack.size();
    }
    return 0;
  }
}
