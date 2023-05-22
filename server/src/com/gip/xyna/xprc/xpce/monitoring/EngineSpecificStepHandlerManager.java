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

package com.gip.xyna.xprc.xpce.monitoring;

import java.util.List;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;


public interface EngineSpecificStepHandlerManager {

  public List<Handler> getHandlers(ProcessStepHandlerType ht, XynaOrderServerExtension xose);


  public void addHandler(DestinationKey destinationKey, DestinationValue dv, ProcessStepHandlerType ht, Handler handler);


  public void removeHandler(DestinationKey destinationKey, DestinationValue dv, ProcessStepHandlerType ht, Handler handler);
  
  
  public void addFactory(String name, DynamicStepHandlerFactory factory);
  
  public void removeFactory(String name);
  
  
  public static interface DynamicStepHandlerFactory {
    
    public Handler createHandler(XynaOrderServerExtension xose, ProcessStepHandlerType ht);
    
    public Object extractCachingParameter(XynaOrderServerExtension xose);
    
  }

}
