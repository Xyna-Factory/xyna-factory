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
package dhcpAdapterDemon;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.demon.DemonPersistency;
import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.persistency.PersistableCounter;

import dhcpAdapterDemon.SocketMaster.SocketReaderConsumer;
import dhcpAdapterDemon.db.AuditDBFiller;
import dhcpAdapterDemon.db.CpeAuditDBFiller;
import dhcpAdapterDemon.db.DnsDBFiller;
import dhcpAdapterDemon.db.LeaseLogDBFiller;
import dhcpAdapterDemon.types.Database;
import snmpTrapDemon.poolUsage.IPAddress;
import snmpTrapDemon.poolUsage.Subnet;

public class DhcpDbStack implements SocketReaderConsumer {
  
  final static Logger logger = Logger.getLogger(DhcpDbStack.class);
    
  private AuditDBFiller auditDBFiller;
  private DnsDBFiller dnsDBFiller;
  private LeaseLogDBFiller leaseLogDBFiller; 
  private CpeAuditDBFiller cpeAuditDBFiller1;
  private CpeAuditDBFiller cpeAuditDBFiller2;

  private PersistableCounter pcRequested;
  private PersistableCounter pcSucceeded;
  private PersistableCounter pcFailed;
  
  private ArrayList<Subnet> mtaSubnets = new ArrayList<Subnet>();
  private ArrayList<Subnet> nonCpeSubnets = new ArrayList<Subnet>();
  
  private boolean dodns;
  private boolean dolegalintercept;
 
  
  public static enum Counters {
    REQUESTED,SUCCEEDED,FAILED;
  }

  public DhcpDbStack(AuditDBFiller auditDBFiller, DnsDBFiller dnsDBFiller, LeaseLogDBFiller leaseLogDBFiller, CpeAuditDBFiller cpeAuditDBFiller1, CpeAuditDBFiller cpeAuditDBFiller2) {
    
    pcRequested = new PersistableCounter("dhcpDbStack.requested");
    pcSucceeded = new PersistableCounter("dhcpDbStack.succeeded");
    pcFailed = new PersistableCounter("dhcpDbStack.failed");
    DemonPersistency dp = DemonPersistency.getInstance();
    dp.registerPersistable(pcRequested);
    dp.registerPersistable(pcSucceeded);
    dp.registerPersistable(pcFailed);
    
    dodns = DemonProperties.getBooleanProperty("db.doDNS", true);
    dolegalintercept = DemonProperties.getBooleanProperty("db.doLegalIntercept", true);

    
    this.auditDBFiller = auditDBFiller;
    this.dnsDBFiller = dnsDBFiller;
    this.leaseLogDBFiller = leaseLogDBFiller;
    this.cpeAuditDBFiller1 = cpeAuditDBFiller1;
    this.cpeAuditDBFiller2 = cpeAuditDBFiller2;
    if(dodns)dnsDBFiller.fillMtaSubnets( mtaSubnets );
    cpeAuditDBFiller1.fillNonCpeSubnets( nonCpeSubnets );
  }

  public void addWork( Database db, DhcpData dhcpData) {
    if( logger.isTraceEnabled() ) { logger.trace( "will be inserted in " + db ); }
    try {
      switch( db ) {
      case AUDIT: 
        if( logger.isTraceEnabled())logger.trace("startAUDIT case");
        if(dhcpData.getDiscover()!=null&&dhcpData.getDiscover().startsWith("0x"))
        {
          auditDBFiller.add( (DhcpData)dhcpData.clone());
          dhcpData.setDiscover(null);
        }
        if( logger.isTraceEnabled())logger.trace("dhcpData discover "+dhcpData.getDiscover());
        if( logger.isTraceEnabled())logger.trace("Adding to AuditDBFiller second time ...");
        dhcpData.setDiscover(null);
        auditDBFiller.add( dhcpData );
        if( logger.isTraceEnabled())logger.trace("Second time added...");
        break;
      case DNS: 
        dnsDBFiller.add( dhcpData );
        break;
      case LEASELOG: 
        leaseLogDBFiller.add( dhcpData );
        break;
      case CPEAUDIT1: 
        if( logger.isTraceEnabled())logger.trace("startCPEAUDIT1 case");
        if(dhcpData.getDiscover()!=null&&dhcpData.getDiscover().startsWith("0x"))
        {
          cpeAuditDBFiller1.add( (DhcpData)dhcpData.clone() );
          dhcpData.setDiscover(null);
        }
        if( logger.isTraceEnabled())logger.trace("dhcpData discover "+dhcpData.getDiscover());
        if( logger.isTraceEnabled())logger.trace("Adding to cpeAuditDBFiller1 second time ...");
        dhcpData.setDiscover(null);
        cpeAuditDBFiller1.add( dhcpData );
        if( logger.isTraceEnabled())logger.trace("Second time added...");

        break;
      case CPEAUDIT2: 
        if( logger.isTraceEnabled())logger.trace("startCPEAUDIT2 case");
        if(dhcpData.getDiscover()!=null&&dhcpData.getDiscover().startsWith("0x"))
        {
          cpeAuditDBFiller2.add( (DhcpData)dhcpData.clone() );
          dhcpData.setDiscover(null);
        }
        if( logger.isTraceEnabled())logger.trace("dhcpData discover "+dhcpData.getDiscover());
        if( logger.isTraceEnabled())logger.trace("Adding to cpeAuditDBFiller2 second time ...");
        dhcpData.setDiscover(null);
        cpeAuditDBFiller2.add( dhcpData );
        if( logger.isTraceEnabled())logger.trace("Second time added...");
        break;
      }
    } catch( Exception e ) {
      logger.error( "Exception while adding work for "+db, e);
    }
  }

  public void consume(String data) {
    pcRequested.increment();
    DhcpData dhcpData = null;
    try {
      dhcpData = DhcpData.readFromString( data );
      NDC.push( dhcpData.getChaddr().toString() );
      if( logger.isTraceEnabled() ) {
        logger.trace("Order in, " + dhcpData);
      } else if( logger.isDebugEnabled() ) {
        logger.debug("Order in");
      }
      
      
      
      
      //Eintrag ins DNS nur für MTAs
      if( isMTA(dhcpData) && dodns) {
        addWork( Database.DNS, dhcpData );
      }

      //Eintrag in die LeaseLog-DB
      if(dolegalintercept) {
        addWork( Database.LEASELOG, dhcpData );
      }

      //Eintrag in die Audit-DB verteilen:
      if( isCPE(dhcpData) ) {
        addWork( Database.CPEAUDIT1, dhcpData );
        addWork( Database.CPEAUDIT2, dhcpData );
      } else {
        addWork( Database.AUDIT, dhcpData );
      }
      
      pcSucceeded.increment();
    } catch (IOException e) {
      logger.error( "Failed to process DhcpData \""+data+"\"", e);
      pcFailed.increment();
    } finally {
      NDC.pop();
    }
  }

  private boolean isMTA(DhcpData dhcpData) {
    String type = dhcpData.getType();
    if( type != null && type.length() > 0 ) {
      return type.startsWith("pktc");
    }
    
    //es kann nicht direkt erkannt werden, ob dies ein MTA ist
    //daher versuchen, dies anhand der IP zu erkennen
    IPAddress ip = dhcpData.getCiaddr();
    for( Subnet subnet : mtaSubnets  ) {
      if( subnet.contains(ip) ) return true;
    }
      
    return false; //es kann nicht erkannt werden, ob dies ein MTA ist
  }
  
  private boolean isCPE(DhcpData dhcpData) {
    IPAddress ip = dhcpData.getCiaddr();
    for( Subnet subnet : nonCpeSubnets  ) {
      if( subnet.contains(ip) ) return false;
    }
    return true; //dies ist wohl ein CPE
  }
  
  public int estimatedLength() {
    return 1000;
  }

  public String getDataSuffix() {
    return "\teol\n";
  }

  public PersistableCounter getCounter(Counters counters) {
    switch( counters ) {
    case REQUESTED: return pcRequested;
    case SUCCEEDED: return pcSucceeded;
    case FAILED:    return pcFailed;
    }
    throw new IllegalArgumentException();
  }
  


  
}