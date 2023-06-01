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
package com.gip.xyna.demon.snmp;

import java.util.ArrayList;

import com.gip.xyna.demon.DemonSnmpConfigurator;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;


/**
 * Diese Klasse ist dazu gedacht, den im Demon eingebauten SNMP-Agenten so zu erweitern, 
 * dass er auch zu anderen OIDs antworten kann.
 *
 */
public class DemonSnmpExtension {

  ArrayList<OidSingleHandler> oidHandlers = new ArrayList<OidSingleHandler>();
  
  public void add( OidSingleHandler oidSingleHandler ) {
    oidHandlers.add( oidSingleHandler );
  }

  public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator) {
    for( OidSingleHandler osh : oidHandlers ) {
      demonSnmpConfigurator.addOidSingleHandler(osh );
    }
  }
  
}
