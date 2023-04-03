/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpTrapDemon;

import org.apache.log4j.Logger;

import snmpTrapDemon.leases.LeasesSearcher;
import snmpTrapDemon.licensemanagement.LicenseManagement;
import snmpTrapDemon.poolUsage.PoolUsageCronJob;

import com.gip.xyna.demon.DemonSnmpConfigurator;
import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.utils.db.failover.FailoverSources;
import common.FailoverOidSingleHandler;
import common.FailoverSourceOID;

/**
 * SnmpTrapDemonWorker ist zuständig für:
 * 1) Ermittlung der PoolUsage und verschicken von Traps dazu
 * 2) Suchen der Leases im Schema Audit und Ausgabe per SNMP
 * 
 */
public class SnmpTrapDemonWorker implements DemonWorker {
  
  static Logger logger = Logger.getLogger(SnmpTrapDemonWorker.class.getName());
  
  private volatile boolean running;
  private PoolUsageCronJob poolUsageCronJob;

  private LeasesSearcher leasesSearcher;
  private LicenseManagement licenseManagement;
  
  public SnmpTrapDemonWorker() {
    FailoverSources.addFailover("oid", new FailoverSourceOID() );    
    poolUsageCronJob = new PoolUsageCronJob();
    leasesSearcher = new LeasesSearcher();
    licenseManagement=new LicenseManagement();
  }
  
  public String getName() {
    return "SnmpTrapWorker";
  }

  public void logStatus(Logger statusLogger) {
    StringBuilder sb = new StringBuilder();
    sb.append("SnmpTrapDemonWorker is").append( running ? "":" not" ).append(" running");
    statusLogger.info( sb.toString() );
    leasesSearcher.logStatus( statusLogger );
    poolUsageCronJob.logStatus( statusLogger );
  }

  public void run() {
    running = true;
    poolUsageCronJob.start();
  }

  public void terminate() {
    running = false;
    poolUsageCronJob.terminate();
  }

  public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator) {
    demonSnmpConfigurator.addOidSingleHandler( leasesSearcher );
    demonSnmpConfigurator.addOidSingleHandler( licenseManagement );
    demonSnmpConfigurator.addOidSingleHandler( poolUsageCronJob.getPoolUsageSnmpTable() );
    demonSnmpConfigurator.addOidSingleHandler( poolUsageCronJob.getPoolUsageSnmpSumTable() );
    demonSnmpConfigurator.addOidSingleHandler( FailoverOidSingleHandler.getInstance() );
  }

}
