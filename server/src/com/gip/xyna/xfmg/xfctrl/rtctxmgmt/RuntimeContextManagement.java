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
package com.gip.xyna.xfmg.xfctrl.rtctxmgmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;


public class RuntimeContextManagement extends FunctionGroup {

  private final static String DEFAULT_NAME = "RuntimeContextManagement";
  
  private final Map<String, RuntimeContextChangeHandler> handlers = new HashMap<>();
  
  
  public RuntimeContextManagement() throws XynaException {
    super();
  }

  
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  protected void init() throws XynaException {

  }


  protected void shutdown() throws XynaException {

  }
  
  
  
  
  public boolean registerHandler(RuntimeContextChangeHandler handler) {
    synchronized (RuntimeContextManagement.class) {
      RuntimeContextChangeHandler oldHandler = handlers.get(handler.getName());
      if (oldHandler == null) {
        handlers.put(handler.getName(), handler);
        return true;
      } else {
        if (oldHandler.getVersion().isEqualOrGreaterThan(handler.getVersion())) {
          return false;
        } else {
          oldHandler.displacedByNewVersion();
          handlers.put(handler.getName(), handler);
          return true;
        }
      }
    }
  }
  
  public boolean unregisterHandler(RuntimeContextChangeHandler handler) {
    synchronized (RuntimeContextManagement.class) {
      RuntimeContextChangeHandler oldHandler = handlers.get(handler.getName());
      if (oldHandler == null) {
        return false;
      } else {
        if (oldHandler == handler) {
          handlers.remove(handler.getName());
          return true;
        } else {
          return false;
        }
      }
    }
  }
  
  public Collection<RuntimeContextChangeHandler> getHandlers() {
    synchronized (RuntimeContextManagement.class) {
      return Collections.unmodifiableCollection(new ArrayList<>(handlers.values()));
    }
  }

}
