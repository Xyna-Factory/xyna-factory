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
package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xsched.VetoManagement;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;

public class XynaSchedulerCustomisationVetos {

  private VetoCache vetoCache;

  public void listExtendedSchedulerInfo(StringBuilder sb) {
    VetoManagement vm = XynaFactory.getInstance().getProcessing().getXynaScheduler().getVetoManagement();
    sb.append("VetoManagement ").append( vm.showInformation() ).append("\n");
  }

  public void beginScheduling(long schedulingRunNumber) {
    if( vetoCache != null ) {
      vetoCache.beginScheduling(schedulingRunNumber);
    }
  }
  
  public void endScheduling() {
    if( vetoCache != null ) {
      vetoCache.endScheduling();
    }
  }

  public void setVetoCache(VetoCache vetoCache) {
    this.vetoCache = vetoCache;
  }

}
