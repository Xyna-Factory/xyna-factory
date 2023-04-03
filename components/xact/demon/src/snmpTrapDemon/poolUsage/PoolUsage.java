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
package snmpTrapDemon.poolUsage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReaderFactory;
import com.gip.xyna.utils.db.ResultSetReaderFunction;
import com.gip.xyna.utils.db.SQLUtils;

/**
 * PoolUsage ist für das Ermitteln der PoolUsage zuständig
 *
 */
public class PoolUsage {
  
  static String EXCLUSION_INTERVALL_ERROR="Error in pool.exclusions, exclusion intervall '%s' ignored, reason '%s'";

  /**
   * 
   */
  public static final int POOL_SUM_OFFSET = 1000000;

  static Logger logger = Logger.getLogger(PoolUsage.class.getName());

  private SQLUtils sqlUtils;

  /**
   * Konstruktor
   * @param sqlUtils
   */
  public PoolUsage(SQLUtils sqlUtils) {
    this.sqlUtils = sqlUtils;
  }

  /**
   * Suche aller Pools, Eintragen in Tabelle PoolSize
   */
  public void searchPools() {
    logger.debug( "searchPools" );
    //bisherige Einträge löschen
    sqlUtils.executeDML( "DELETE FROM poolsize", null );
    //neue Einträge suchen, bearbeiten und eintragen
    sqlUtils.query( 
         "SELECT p.poolID, sn.sharedNetworkID, p.pooltypeID, sg.standortGruppeID, p.rangeStart, p.rangeStop, p.exclusions"
        +"  FROM dhcp.pool p"
        +"  INNER JOIN dhcp.subnet s ON s.subnetID = p.subnetID"
        +"  INNER JOIN dhcp.sharednetwork sn ON sn.sharedNetworkID = s.sharedNetworkID"
        +"  INNER JOIN dhcp.standort st ON st.standortID = sn.standortID"
        +"  INNER JOIN dhcp.standortgruppe sg ON sg.standortGruppeID = st.standortGruppeID",
        null,
        new PoolSizeInserter( sqlUtils )
    );
    //neue Einträge committen
    sqlUtils.commit();
  }
    
  /**
   * Hilfsobjekt zum Eintragen der Tabelle PoolSize
   *
   */
  private static class PoolSizeInserter implements ResultSetReaderFunction {
    private SQLUtils sqlUtils;

    public PoolSizeInserter(SQLUtils sqlUtils) {
      this.sqlUtils = sqlUtils;
    }

    public boolean read(ResultSet rs) throws SQLException {
      int poolID = rs.getInt(1);
      int sharedNetworkID = rs.getInt(2);
      int poolTypeID = rs.getInt(3);
      int standortGruppeID = rs.getInt(4);
      long poolSize = 0;
      long rangeStart = 0;
      long rangeStop = 0;
      
      try {
        IPAddress rStart = IPAddress.parse( rs.getString(5) );
        IPAddress rStop = IPAddress.parse( rs.getString(6) );
        rangeStart = rStart.toLong();
        rangeStop  = rStop.toLong();
        String exclusions=rs.getString(7);
        
        if( rangeStart > rangeStop ) {
          logger.error( "Invalid poolRanges for poolID "+poolID+ " will be swapped" );
          rangeStart = rStop.toLong();
          rangeStop  = rStart.toLong();
        }
        
        poolSize = rangeStop - rangeStart +1;
        
        ArrayList<IPAddress[]>exclusionRanges=parseExclusionRanges(exclusions,rStart.toString(),rStop.toString());
        /*ggf. Checks machen*/

        for(IPAddress[] exclusionIntervall:exclusionRanges){
          long intervallSize=exclusionIntervall[1].toLong()-exclusionIntervall[0].toLong()+1;
          poolSize = poolSize-intervallSize;
        }

        //Im Poolbereich können alle Adressen benutzt werden.
      } catch( RuntimeException e ) {
        logger.error( "Invalid poolRange for poolID "+poolID, e );
      }
      if( poolSize < 0 ) {
        logger.error( "Invalid poolSize "+poolSize+" for poolID "+poolID);
        poolSize = -1;
      }
      
      logger.debug( poolID+" "+sharedNetworkID+" "+poolTypeID );
      sqlUtils.executeDML( 
          "INSERT INTO poolsize (poolID,sharedNetworkID,poolTypeID,standortGruppeID,rangeStart,rangeStop,size) VALUES(?,?,?,?,?,?,?)",
          new Parameter(poolID,sharedNetworkID,poolTypeID,standortGruppeID,rangeStart,rangeStop,poolSize )
          );
      return true; //weiterlesen
    }
    
  }
  
  static private ArrayList<IPAddress[]> parseExclusionRanges(String exclusions, String rangeStart, String rangeStop) {
    IPAddress[] poolRange=new IPAddress[]{IPAddress.parse(rangeStart),IPAddress.parse(rangeStop)};

    ArrayList<IPAddress[]> exclusionRanges=new ArrayList<IPAddress[]>();
    if (exclusions!=null && !exclusions.trim().equals("")){
      String[] exclusionIntervall=exclusions.split(",");
      for(int i=0;i<exclusionIntervall.length;i++){
        String oneIntervall=exclusionIntervall[i].trim();
        try {
          IPAddress[] candidate=new IPAddress[2]; 
          if (oneIntervall.indexOf("-")>0){
            String[] limits=oneIntervall.split("-");
            candidate[0]=IPAddress.parse(limits[0].trim());
            candidate[1]=IPAddress.parse(limits[1].trim());
            if(candidate[0].toLong()>candidate[1].toLong()){//Grenzen vertauscht
              candidate[0]=IPAddress.parse(limits[1].trim());
              candidate[1]=IPAddress.parse(limits[0].trim());
            }
          } else {// only one address
            candidate[0]=IPAddress.parse(exclusionIntervall[i].trim());
            candidate[1]=candidate[0];
          }
          //Test auf Overlap
          if (overlaps( candidate, exclusionRanges)){
            logger.error(String.format(EXCLUSION_INTERVALL_ERROR,exclusionIntervall[i].trim(),"overlaps previous intervall" ));
          } else if (! includes(poolRange, candidate)){
            logger.error(String.format(EXCLUSION_INTERVALL_ERROR,exclusionIntervall[i].trim(),"not inside pool" ));
          } else{
            exclusionRanges.add(candidate);
          }
        } catch (Exception e){
          logger.error(String.format(EXCLUSION_INTERVALL_ERROR,exclusionIntervall[i].trim(),"syntax error <"+exclusions+ ">"+e.getMessage() ));
        } 
      }


    }
    return exclusionRanges;
  }
  
  private static boolean includes(IPAddress[] poolRange, IPAddress[] candidate) {
    if (candidate[0].toLong() > poolRange[0].toLong() && candidate[0].toLong() > poolRange[1].toLong()){
      return false;
    }
    if (candidate[1].toLong() > poolRange[0].toLong() && candidate[1].toLong() > poolRange[1].toLong()){
      return false;
    }
    if (candidate[0].toLong() < poolRange[0].toLong() && candidate[0].toLong() < poolRange[1].toLong()){
      return false;
    }
    if (candidate[1].toLong() < poolRange[0].toLong() && candidate[1].toLong() < poolRange[1].toLong()){
      return false;
    }
    return true;
  }

  private static boolean overlaps(IPAddress[] exclusion, ArrayList<IPAddress[]> candidate) {
    for(IPAddress[] oneCandidate:candidate){
      if(overlapsSingle(exclusion, oneCandidate)){
        return true;
      }
    }
    return false;   
  }
    
  private static boolean overlapsSingle(IPAddress[] exclusion, IPAddress[] candidate) {
    if (candidate[0].toLong()>exclusion[1].toLong() && candidate[0].toLong()>exclusion[0].toLong() && candidate[1].toLong()>exclusion[1].toLong() && candidate[1].toLong()>exclusion[0].toLong()){
      return false;
    }
    if (candidate[0].toLong()<exclusion[1].toLong() && candidate[0].toLong()<exclusion[0].toLong() && candidate[1].toLong()<exclusion[1].toLong() && candidate[1].toLong()<exclusion[0].toLong()){
      return false;
    }
    return true;
  }

  /**
   * Berechnung der Ausnutzung aller Pools
   */
  public void calculateUsage() {
    logger.debug( "calculateUsage" );
    
    //neue Transaktion beginnen, um aktuelle Daten zu lesen
    sqlUtils.rollback();
    
     //Exclusions einlesen
    ArrayList<String[]> exclusionString=(sqlUtils.query( "SELECT p.exclusions,p.rangeStart,p.rangeStop FROM dhcp.pool p where isdeployed='yes' and useforstatistics='yes'",   null, ResultSetReaderFactory.getStringArrayReader(3)));    
    String exclusionCondition=""; 
    for(String[] oneExclusion:exclusionString){
      ArrayList<IPAddress[]>exclusionRanges =PoolUsage.parseExclusionRanges(oneExclusion[0],oneExclusion[1],oneExclusion[2]);
      for(IPAddress[] exclusionIntervall:exclusionRanges){
        if (exclusionCondition.length()==0){
          exclusionCondition="WHERE";
        } else {
          exclusionCondition=exclusionCondition+" AND";
        }
        exclusionCondition=exclusionCondition+" NOT(ipnum>="+exclusionIntervall[0].toLong()+" AND ipnum<="+exclusionIntervall[1].toLong()+")";
      }
    }

    boolean xynaactive = false;
    boolean iscactive = false;
    String xynaactivestring=DemonProperties.getProperty("db.snmpTrap.view.xynaactive", "false");
    String iscactivestring=DemonProperties.getProperty("db.snmpTrap.view.iscactive", "false");

    if(xynaactivestring.equalsIgnoreCase("true")) // Xyna aktiviert, dazu passende Tabellen auslesen.
    {
      xynaactive = true;
    }
    
    if(iscactivestring.equalsIgnoreCase("true")) // Xyna aktiviert, dazu passende Tabellen auslesen.
    {
      iscactive = true;
    }
    
    //Auslesen aller vergebenen IpAdressen
    ListCounter<Long> leasesCounter = new ListCounter<Long>();
    if(iscactive)
    {
      String leasesView=DemonProperties.getProperty("db.snmpTrap.view.leases", "leases");
      leasesCounter.addAll( sqlUtils.query( "SELECT ipNum FROM "+leasesView+" "+exclusionCondition,   null, ResultSetReaderFactory.getLongReader() ) );
      String leasesCpeView=DemonProperties.getProperty("db.snmpTrap.view.leasescpe", "leasescpe");
      leasesCounter.addAll( sqlUtils.query( "SELECT ipNum FROM "+leasesCpeView+" "+exclusionCondition,null, ResultSetReaderFactory.getLongReader() ) );
    }
    
    if(xynaactive)
    {
      String xynaleasesView=DemonProperties.getProperty("db.snmpTrap.view.leasesxyna", "leases");
      leasesCounter.addAll( sqlUtils.query( "SELECT ipNum FROM "+xynaleasesView+" "+exclusionCondition,   null, ResultSetReaderFactory.getLongReader() ) );
    }
    
    //bisherige Einträge löschen
    sqlUtils.executeDML( "DELETE FROM poolusage", null );

    //und neue Zählerstände eintragen
    sqlUtils.query( "SELECT poolID,sharedNetworkID,poolTypeID,standortGruppeID,size,rangeStart,rangeStop FROM poolsize", null, new PoolUsageInserter(sqlUtils,leasesCounter) );
    
    //Inaktive Pools und aus der Statistik ausgeschlossene Pools zaehlen nicht
    sqlUtils.executeDML("UPDATE poolusage SET size=0, used=0 WHERE poolID in (SELECT poolId from dhcp.pool WHERE not useforstatistics='yes' or not isdeployed='yes' or useforstatistics is null or isdeployed is null)",null);

    
    //summieren
    sqlUtils.executeDML( 
        "INSERT INTO poolusage (poolID,sharedNetworkID,poolTypeID,standortGruppeID,size,used,usedFraction)"
        +"  SELECT "+POOL_SUM_OFFSET+"+sharedNetworkID*100+poolTypeID, sharedNetworkID, poolTypeID, standortGruppeID, SUM(size), SUM(used), 0 "
        +"    FROM poolusage"
        +"    GROUP BY sharedNetworkID, poolTypeID, standortGruppeID",
        null );

    //Bruchteil berechnen
    sqlUtils.executeDML(
        "UPDATE poolusage SET usedFraction = if (size>0,ROUND(used/size,2),0)",
        null );
  
    //neue Einträge committen
    sqlUtils.commit();
  }

  /**
   * Hilfsobjekt zum Eintragen der Tabelle PoolUsage
   *
   */
  private static class PoolUsageInserter implements ResultSetReaderFunction {
    private SQLUtils sqlUtils;
    private ListCounter<Long> leasesCounter;

    public PoolUsageInserter(SQLUtils sqlUtils, ListCounter<Long> leasesCounter) {
      this.sqlUtils = sqlUtils;
      this.leasesCounter = leasesCounter;
    }

    public boolean read(ResultSet rs) throws SQLException {
      int poolID = rs.getInt(1);
      int sharedNetworkID = rs.getInt(2);
      int poolTypeID = rs.getInt(3);
      int standortGruppeID = rs.getInt(4);
      long poolSize = rs.getLong(5);
      long rangeStart = rs.getLong(6);
      long rangeStop = rs.getLong(7);

      long used = -1; 
      //double usedFraction = 1.*used/poolSize;

      if( poolSize >= 0 ) {
        used = leasesCounter.countBetween(rangeStart,rangeStop);
        //usedFraction = 1.*used/poolSize;
      }

      sqlUtils.executeDML( 
          "INSERT INTO poolusage (poolID,sharedNetworkID,poolTypeID,standortGruppeID,size,used,usedFraction) VALUES(?,?,?,?,?,?,?)",
          new Parameter(poolID,sharedNetworkID,poolTypeID,standortGruppeID,poolSize,used,0. )
          );
      
      return true; //weiter lesen
    }
    
  }
  
  /**
   * 
   */
  public PoolUsageTable readPoolUsageTable() {
    PoolUsageTable put = new PoolUsageTable();
    put.read(sqlUtils);
    return put;
  }  
  
    /**
   * 
   */
  public PoolUsageTable readPoolUsageSumTable() {
    PoolUsageTable put = new PoolUsageTable();
    put.readSum(sqlUtils);
    return put;
  }  

  
}
  
