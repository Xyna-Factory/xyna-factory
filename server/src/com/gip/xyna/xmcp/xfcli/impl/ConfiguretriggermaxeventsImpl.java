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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Configuretriggermaxevents;



public class ConfiguretriggermaxeventsImpl extends XynaCommandImplementation<Configuretriggermaxevents> {

  public void execute(OutputStream statusOutputStream, Configuretriggermaxevents payload) throws XynaException {
    long maxNumberEvents = Long.parseLong(payload.getMaxEvents());
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    CommandControl.tryLock(CommandControl.Operation.TRIGGER_MODIFY, revision);
    try {
      XynaFactory.getInstance().getActivation().getActivationTrigger().configureTriggerMaxEvents(payload.getTriggerInstanceName(), maxNumberEvents,
                                                                                                 payload.getAutoReject(), revision);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_MODIFY, revision);
    }
  }

}
