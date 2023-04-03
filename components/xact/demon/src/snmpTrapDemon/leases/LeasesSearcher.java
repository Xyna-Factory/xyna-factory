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
package snmpTrapDemon.leases;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import snmpTrapDemon.poolUsage.IPAddress;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.failover.Failover;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler.SnmpCommand;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;

import dhcpAdapterDemon.db.SQLUtilsLoggerImpl;

/**
 * Sucht in der audit.leases-Tabelle nach den angefragten Daten. 
 * 
 * Gefragt werden kann 
 * 1) für eine IP (in 4 Teilen)
 *    a) MAC               unter OID .1.3.6.1.4.1.28747.1.13.3.2.1.1.5 index(4)
 *    b) RemoteId          unter OID .1.3.6.1.4.1.28747.1.13.3.2.1.1.6 index(4)
 *    c) MAC, RemoteId     unter OID .1.3.6.1.4.1.28747.1.13.3.2.1.1.7 index(4)
 * 2) für eine MAC (in 2 Teilen)
 *    a) IP                unter OID .1.3.6.1.4.1.28747.1.13.3.2.2.1.3 index(2)
 *    b) RemoteId          unter OID .1.3.6.1.4.1.28747.1.13.3.2.2.1.4 index(2)
 *    c) IP, RemoteId      unter OID .1.3.6.1.4.1.28747.1.13.3.2.2.1.5 index(2)
 *    d) CMTS              unter OID .1.3.6.1.4.1.28747.1.13.3.2.2.1.6 index(2)
 *    e) IP, CMTS          unter OID .1.3.6.1.4.1.28747.1.13.3.2.2.1.7 index(2)
 * 3) für eine MAC (in 6 Teilen)
 *    a) IP                unter OID .1.3.6.1.4.1.28747.1.13.3.2.3.1.7 index(6)
 *    b) RemoteId          unter OID .1.3.6.1.4.1.28747.1.13.3.2.3.1.8 index(6)
 *    c) IP, RemoteId      unter OID .1.3.6.1.4.1.28747.1.13.3.2.3.1.9 index(6)
 *    d) CMTS              unter OID .1.3.6.1.4.1.28747.1.13.3.2.3.1.10 index(6)
 *    e) IP, CMTS          unter OID .1.3.6.1.4.1.28747.1.13.3.2.3.1.11 index(6)
 * 4) für eine RemoteId (in 2 Teilen)
 *    a) Liste von MAC     unter OID .1.3.6.1.4.1.28747.1.13.3.2.4.1.4 index(2)
 *    a) Liste von IP      unter OID .1.3.6.1.4.1.28747.1.13.3.2.4.1.5 index(2)
 *    a) Liste von MAC, IP unter OID .1.3.6.1.4.1.28747.1.13.3.2.4.1.6 index(2)
 * 5) für eine RemoteId (in 6 Teilen)
 *    a) Liste von MAC     unter OID .1.3.6.1.4.1.28747.1.13.3.2.5.1.8 index(6)
 *    a) Liste von IP      unter OID .1.3.6.1.4.1.28747.1.13.3.2.5.1.9 index(6)
 *    a) Liste von MAC, IP unter OID .1.3.6.1.4.1.28747.1.13.3.2.5.1.10 index(6)
 *    
 * Eine IP wird dabei in 4 Teilen übergeben:
 *      (192.168.179.11 -> 192.168.179.11)
 * Eine MAC oder RemoteId kann in 2 oder 6 Teilen übergeben werden:
 *      (0c:07:74:bf:b5:c0 -> 788340.12563904 oder 12.7.116.191.181.192)
 * 
 * Beispiele: 
 * 1.a) 1.3.6.1.4.1.28747.1.13.3.2.1.1.5.192.168.179.11 MAC          für IP 192.168.179.11
 *   b) 1.3.6.1.4.1.28747.1.13.3.2.1.1.6.192.168.179.11 RemoteId     für IP 192.168.179.11
 *   c) 1.3.6.1.4.1.28747.1.13.3.2.1.1.7.192.168.179.11 MAC,RemoteId für IP 192.168.179.11
 * 2.a) 1.3.6.1.4.1.28747.1.13.3.2.2.1.3.788340.12563904 IP          für MAC 0c:07:74:bf:b5:c0
 *   b) 1.3.6.1.4.1.28747.1.13.3.2.2.1.4.788340.12563904 RemoteId    für MAC 0c:07:74:bf:b5:c0
 *   c) 1.3.6.1.4.1.28747.1.13.3.2.2.1.5.788340.12563904 IP,RemoteId für MAC 0c:07:74:bf:b5:c0
 * 3.a) 1.3.6.1.4.1.28747.1.13.3.2.3.1.7.12.7.116.191.181.192 IP          für MAC 0c:07:74:bf:b5:c0
 *   b) 1.3.6.1.4.1.28747.1.13.3.2.3.1.8.12.7.116.191.181.192 RemoteId    für MAC 0c:07:74:bf:b5:c0
 *   c) 1.3.6.1.4.1.28747.1.13.3.2.3.1.9.12.7.116.191.181.192 IP,RemoteId für MAC 0c:07:74:bf:b5:c0
 * 4. ) 1.3.6.1.4.1.28747.1.13.3.2.4.1.4.290.17493                   für RemoteId 000122004455
 * 5. ) 1.3.6.1.4.1.28747.1.13.3.2.5.1.9.0.1.34.0.68.85              für RemoteId 000122004455
 * 
 * 
 */
public class LeasesSearcher implements OidSingleHandler {
  static Logger logger = Logger.getLogger(LeasesSearcher.class.getName());
 
  public static final OID OID_LEASE    = new OID(".1.3.6.1.4.1.28747.1.13.3.2");
  public static final OID OID_WALK_END = new OID(".1.3.6.1.4.1.28747.1.13.3.3");
  public static final int OID_LEASE_INDEX = 11;
  private static final long MAX_CACHE_AGE = 10000; //maximales Alter der gecachten Daten, bevor sie neu gelesen werden müssen
  private static final int MAX_CACHE_SIZE = 20; //maximale Cache-Größe
  
  private AtomicInteger callCounter = new AtomicInteger(0);
  private SQLUtils sqlUtils;
  private FailoverDBConnectionData dbConData;
  private Failover failover;
  private CacheMap<LeasesData,LeasesDataList> cacheSearch;
  private SQLUtilsLoggerImpl sqlUtilsLogger;
  private CmtsNameSearcher cmtsNameSearcher = new CmtsNameSearcher(this);
  
  private static class LeasesData {

    private String host;
    private Long ip;
    private String remoteId;
    private String unique;

    /**
     * @param host
     * @param ip
     * @param remoteId
     */
    public LeasesData(String host, Long ip, String remoteId) {
      this.host = host;
      this.ip = ip;
      this.remoteId = remoteId;
      this.unique = host+"-"+ip+"-"+remoteId;
    }
    
    @Override
    public String toString() {
      return unique;
    }
   
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return unique.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if( obj instanceof LeasesData) {
        return unique.equals( ((LeasesData)obj).unique );
      } else {
        return false;
      }
    }

    /**
     * @return the host
     */
    public String getHost() {
      return host;
    }
    
    /**
     * @return the ip
     */
    public Long getIp() {
      return ip;
    }
    
    /**
     * @return the remoteId
     */
    public String getRemoteId() {
      return remoteId == null ? "null" : remoteId;
    }
        
    private static class Reader implements ResultSetReader<LeasesData> {
      
      
      private static String[] search(String sel, String where, String orderBy) {
        String whereAndOrderBy=where;
        if(orderBy.length()>0){
          whereAndOrderBy=where+" "+orderBy;          
        }
        
        
        
        String leasesview=DemonProperties.getProperty("db.snmpTrap.view.leases", "leases");
        String leasescpeview=DemonProperties.getProperty("db.snmpTrap.view.leasescpe", "leasescpe");  
        String xynaleasesView=DemonProperties.getProperty("db.snmpTrap.view.leasesxyna", "leases");


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

        
        if(xynaactive && !iscactive) // nur Xyna
        {
          return new String[]{"SELECT "+sel+" FROM "+xynaleasesView+" WHERE "+whereAndOrderBy};
        }
        
        
        if(xynaactive && iscactive) // Xyna zusaetzlich aktiviert, auch dazu passende Tabellen auslesen.
        {

          return new String[]{"SELECT "+sel+" FROM "+xynaleasesView+" WHERE "+whereAndOrderBy,
                              "SELECT "+sel+" FROM "+leasescpeview+" WHERE "+whereAndOrderBy,
                              "SELECT "+sel+" FROM "+leasesview+" WHERE "+whereAndOrderBy,
                          };
        
        }
        if(!xynaactive && iscactive)
        {
          return  new String[]{"SELECT "+sel+" FROM "+leasescpeview+" WHERE "+whereAndOrderBy,
                               "SELECT "+sel+" FROM "+leasesview+" WHERE "+whereAndOrderBy,
                          };
          
        }
        if(!xynaactive && !iscactive)
        {
          logger.warn("Neither xyna nor isc active. Using isc anyway ...");
        }
        
        return new String[]{"SELECT "+sel+" FROM "+leasescpeview+" WHERE "+whereAndOrderBy,
                            "SELECT "+sel+" FROM "+leasesview+" WHERE "+whereAndOrderBy
        };
      }
      private static final String[] SEARCH_MAC      = search("host,ipNum,remoteId","host=?","ORDER BY STR_TO_DATE(endTime,'%Y/%m/%d %H:%i:%s') DESC");
      private static final String[] SEARCH_IP       = search("host,ipNum,remoteId","ipNum=?","ORDER BY STR_TO_DATE(endTime,'%Y/%m/%d %H:%i:%s') DESC");
      private static final String[] SEARCH_REMOTEID = search("host,ipNum,remoteId","remoteId=?", "ORDER BY ipNum");
      
      public LeasesData read(ResultSet rs) throws SQLException {
        String host = rs.getString(1);
        Long ip = Long.valueOf(  rs.getLong(2) );
        String remoteId = rs.getString(3);
        return new LeasesData(host,ip,remoteId);
      }
      
    }

    /**
     * @return
     */
    public String getIpString() {
     return IPAddress.parse(ip).toString();
    }

    
  }
  
  
  private static class LeasesQueryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public LeasesQueryException(String message) {
      super(message);
    }
  }

  private class LeasesDataList extends AbstractList<LeasesData> {

    ArrayList<LeasesData> data;
    private long searchDate;
    
    /**
     * @param lds
     */
    public LeasesDataList(ArrayList<LeasesData> lds) {
      this.data = lds;
      this.searchDate = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see java.util.AbstractList#get(int)
     */
    @Override
    public LeasesData get(int index) {
      return data.get(index);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
      return data.size();
    }

    /**
     * @return the searchDate
     */
    public long getSearchDate() {
      return searchDate;
    }
    
  }
    
  /**
   * Ermittlung, nach was der SNMP-Request fragt
   *
   */
  private static enum RequestType {
    IP(4,false),
    MAC2(2,false),
    MAC6(6,false),
    REMOTEID2(2,true),
    REMOTEID6(6,true);
    private int len;
    private boolean getNextAllowed;

    private RequestType( int len, boolean getNextAllowed ) {
      this.len = len;
      this.getNextAllowed = getNextAllowed;
    }

    /**
     * Ermittlung des Request aus der Oid
     * @param oid
     * @return
     */
    public static RequestType parse(OID oid) {
      int idx = Integer.parseInt( oid.getIndex( OID_LEASE_INDEX ) );
      if( idx > RequestType.class.getEnumConstants().length ) {
        return null; 
      }
      return RequestType.class.getEnumConstants()[idx-1];
    }

    /**
     * Prüfung auf korrekte OID-Länge
     * @param oid
     * @return
     */
    public boolean checkLength(OID oid) {
      int oidLength = oid.length();
      if( getNextAllowed ) {
        return oidLength == OID_LEASE_INDEX +3+len || oidLength == OID_LEASE_INDEX +3+len +1; //getNext-Index
      } else {
        return oidLength == OID_LEASE_INDEX +3+len;
      }
    }
    
    /**
     * @return the getNextAllowed
     */
    public boolean isGetNextAllowed() {
      return getNextAllowed;
    }
  }

  private static enum ResultType {
    IP {          @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getIpString(); } },
    IP_REMOTEID{  @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getIpString()+", "+ld.getRemoteId(); } },
    HOST{         @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getHost(); } },
    HOST_REMOTEID{@Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getHost()+", "+ld.getRemoteId(); } },
    REMOTEID{     @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getRemoteId(); } },
    HOST_IP{      @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getHost()+", "+ld.getIpString(); } },
    CMTS{         @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return cns.getCmts(ld.getIp()); } },
    IP_CMTS{      @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return ld.getIpString()+", "+cns.getCmts(ld.getIp()); } },
    INVALID{      @Override public String selectResult( LeasesData ld, CmtsNameSearcher cns ) { return "Internal error"; } };
    
    public abstract String selectResult( LeasesData leasesData, CmtsNameSearcher cmtsNameSearcher );
  }
  
  private static class CmtsNameSearcher {
    private LeasesSearcher leasesSearcher;
    private ArrayList<Pool> pools;
    private long lastSearch;
    private static final long MAX_POOL_AGE = 600000; //alle 10 min neu suchen 
    
    public CmtsNameSearcher(LeasesSearcher leasesSearcher) {
      this.leasesSearcher = leasesSearcher;
    }

    /**
     * @param ip
     * @return
     * @throws LeasesQueryException 
     */
    private synchronized String getCmts(Long ip) {
      if( pools == null || lastSearch < System.currentTimeMillis() - MAX_POOL_AGE ) {
        pools = leasesSearcher.searchPools();
      }
      //Suchen nach CMTS in den Pools
      for( Pool p : pools ) {
        if( p.rangeStart <= ip && p.rangeStop >= ip ) {
          return p.cmtsName;
        }
      }
      return "no cmts for "+ip + " found";
    }

  }
 
  public LeasesSearcher() {
    DBConnectionData cd = DemonProperties.getDBProperty( "snmpTrap" );
    dbConData = DemonProperties.getFailoverDBProperty( "snmpTrap", cd );
    
    failover = dbConData.createNewFailover();  
    sqlUtilsLogger= new SQLUtilsLoggerImpl(logger);
    cacheSearch = new CacheMap<LeasesData,LeasesDataList>(MAX_CACHE_SIZE);
  }
  
 


  private SQLUtils getSqlUtils() {
    boolean isValid = false;
    if( sqlUtils != null ) {
      try {
        isValid = sqlUtils.getConnection().isValid(0);
      } catch (SQLException e) {
        sqlUtilsLogger.logException(e);
        isValid = false;
      }
    }
    if( isValid ) {
      sqlUtils = failover.checkAndRecreate( sqlUtils, sqlUtilsLogger );
    } else {
      sqlUtils = failover.recreateSQLUtils(sqlUtils, sqlUtilsLogger);
    }
    return sqlUtils;
  }

 

  /**
   * @param requestType
   * @param intIndex
   * @return
   */
  private ResultType selectResultType(RequestType requestType, int index) {
    switch( requestType ) {
    case IP: 
      if( index == 5 ) return ResultType.HOST;
      if( index == 6 ) return ResultType.REMOTEID;
      if( index == 7 ) return ResultType.HOST_REMOTEID;
      break;
    case MAC2:
      if( index == 3 ) return ResultType.IP;
      if( index == 4 ) return ResultType.REMOTEID;
      if( index == 5 ) return ResultType.IP_REMOTEID;
      if( index == 6 ) return ResultType.CMTS;
      if( index == 7 ) return ResultType.IP_CMTS;
      break;
    case MAC6:
      if( index == 7 ) return ResultType.IP;
      if( index == 8 ) return ResultType.REMOTEID;
      if( index == 9 ) return ResultType.IP_REMOTEID;
      if( index == 10) return ResultType.CMTS;
      if( index == 11) return ResultType.IP_CMTS;
      break;     
    case REMOTEID2:
      if( index == 4 ) return ResultType.HOST;
      if( index == 5 ) return ResultType.IP;
      if( index == 6 ) return ResultType.HOST_IP;
      break;
    case REMOTEID6:
      if( index == 8 ) return ResultType.HOST;
      if( index == 9 ) return ResultType.IP;
      if( index == 10) return ResultType.HOST_IP;
      break;
    }
    return ResultType.INVALID;
  }


  /**
   * @param requestType
   * @param subOid
   * @return
   */
  private LeasesData parseRequestType(RequestType requestType, OID subOid) {
    String host = null;
    Long ip = null;
    String remoteId = null;
    switch( requestType ) {
    case IP:
      ip = Long.valueOf(IPAddress.parse(subOid.getOid().substring(1)).toLong());
      break;
    case MAC2:
    case MAC6:
      host = MacOidConverter.toMac( subOid ).toHex();
      break;
    case REMOTEID2:
    case REMOTEID6:
      remoteId = MacOidConverter.toMac( subOid ).toHex();
      break;
    }
    return new LeasesData( host, ip, remoteId );
  }

  /**
   * @param requestType
   * @param hirQuery
   * @return
   * @throws LeasesQueryException 
   */
  private synchronized LeasesDataList query(RequestType requestType, LeasesData ldQuery) {
    //Suche in Cache, oder Query in DB und anschließendes Cachen
    LeasesDataList ldl = cacheSearch.get(ldQuery); 
    if( ldl != null ) {
      if( ldl.getSearchDate() > System.currentTimeMillis() - MAX_CACHE_AGE ) {
        //nur gecachte Daten verwenden, wenn das Alter jünger ist als MAX_CACHE_AGE
        return ldl; 
      }
    }    
    
    //Nun in DB suchen
    String[] sql = null;
    Parameter parameter = null;
    switch( requestType ) {
    case IP:
      sql = LeasesData.Reader.SEARCH_IP;
      parameter = new Parameter( ldQuery.getIp());
      break;
    case MAC2:
    case MAC6:
      sql = LeasesData.Reader.SEARCH_MAC;
      parameter = new Parameter( ldQuery.getHost() );
      break;
    case REMOTEID2:
    case REMOTEID6:
      sql = LeasesData.Reader.SEARCH_REMOTEID;
      parameter = new Parameter( ldQuery.getRemoteId() );
      break;
    }
    ArrayList<LeasesData> lds = null;
    
    for(String oneSql:sql){
    
      lds=getSqlUtils().query( oneSql, parameter, new LeasesData.Reader() );

      if( sqlUtils.getLastException() != null ) {
        String msg = sqlUtils.getLastException().getMessage();
        sqlUtils = failover.recreateSQLUtils( sqlUtils, sqlUtilsLogger );
        throw new LeasesQueryException(msg);
      } else {
        sqlUtils.rollback();
      }
      if( lds == null ) {
        throw new LeasesQueryException( "ArrayList is null" );
      }
      if (lds.size()>0){
        break;
      }
    }
    
    ldl = new LeasesDataList( lds );
    
    if( ldl.size() > 0  ) {
      //nur cachen, falls kein Fehler vorliegt
      cacheSearch.put(ldQuery, ldl);
    }
    return ldl;
  }
     
  /**
   * @return 
   * @throws LeasesQueryException 
   * 
   */
  public ArrayList<Pool> searchPools() {
    String sql = "SELECT ps.rangeStart, ps.rangeStop, sn.sharedNetwork "
      + "  FROM poolsize ps "
      +"  INNER JOIN dhcp.sharednetwork sn ON ps.sharednetworkID = sn.sharednetworkID";
    ArrayList<Pool> pools = getSqlUtils().query( sql, null, new Pool.Reader() );
    if( sqlUtils.getLastException() != null ) {
      String msg = sqlUtils.getLastException().getMessage();
      failover.recreateSQLUtils( sqlUtils, sqlUtilsLogger );
      throw new LeasesQueryException(msg);
    } else {
      sqlUtils.rollback();
    }
    return pools;
  }
  
  private static class Pool {
    private long rangeStart;
    private long rangeStop;
    private String cmtsName;

    /**
     * @param rangeStart
     * @param rangeStop
     * @param cmtsName
     */
    public Pool(long rangeStart, long rangeStop, String cmtsName) {
      this.rangeStart = rangeStart;
      this.rangeStop = rangeStop;
      this.cmtsName = cmtsName;
    }

    private static class Reader implements ResultSetReader<Pool> {
      public Pool read(ResultSet rs) throws SQLException {
        return new Pool( rs.getLong(1), rs.getLong(2), rs.getString(3) );
      }

    }
    
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#matches(com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler.SnmpCommand, com.gip.xyna.utils.snmp.OID)
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! oid.startsWith(OID_LEASE) ) {
      return false;
    }
    if( oid.length() < OID_LEASE_INDEX+3 ) {
      return false;
    }
    RequestType requestType = RequestType.parse(oid);
    if( requestType == null ) {
      return false;
    }
    switch( snmpCommand ) {
    case GET: return requestType.checkLength( oid );
    case GET_NEXT: return requestType.isGetNextAllowed();
    default: return false;
    }
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    return getInternal( oid, i, false );
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    return getInternal( oid, i, true );
  }

  /**
   * @param oid
   * @param i
   * @param getNext
   * @return
   */
  private VarBind getInternal(OID oid, int i, boolean getNext) {  
    
    RequestType requestType = RequestType.parse(oid);
    ResultType resultType = selectResultType( requestType, oid.getIntIndex(OID_LEASE_INDEX+2) );
    if( resultType == ResultType.INVALID ) {
      throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }
    
    OID subOid = oid.subOid(OID_LEASE_INDEX+3);
    LeasesData ldQuery = null;
    
    int index;
    if( subOid.length() == requestType.len ) {
      ldQuery = parseRequestType( requestType, subOid );
      index = 0;
    } else {
      ldQuery = parseRequestType( requestType, subOid.subOid(0,requestType.len) ); //Index abschneiden
      index = subOid.getIntIndex(requestType.len);
    }
     
    OID responseOid = oid;
    if( getNext ) {
      int oidLenghtWithoutIndex = OID_LEASE_INDEX+3+requestType.len;
      responseOid = oid.subOid(0,oidLenghtWithoutIndex).append(index+1);
    }
    
    LeasesDataList ldl;
    try {
      ldl = query( requestType, ldQuery );
    } catch (LeasesQueryException e) {
      logger.error( "Fehler bei Suche nach "+ldQuery, e );
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, i);
    }
    
    if( ldl.size() == 0 ) {
      //Keine Daten gefunden
      if( getNext ) {
        //wichtig: Exception werfen, damit der Walk terminiert
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
      } else {
        return new StringVarBind( responseOid.getOid(), "" ); //Keine Daten gefunden, eigentlich auch NO_SUCH_NAME        
      }
    }
    if( index >= ldl.size() ) {
      if( getNext ) {
        return new StringVarBind( OID_WALK_END.getOid(), "" );
      } else {
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
      }
    }
    
    String result;
    try {
      result = resultType.selectResult( ldl.get(index), cmtsNameSearcher );
    } catch (LeasesQueryException e) {
      logger.error( "Fehler bei Suche nach CMTS-Name "+ldQuery, e );
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, i);
    }
    
    return new StringVarBind( responseOid.getOid(), result );
  }
    
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#set(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public void set(OID oid, VarBind varBind, int i) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param statusLogger
   */
  public void logStatus(Logger statusLogger) {
    StringBuilder sb = new StringBuilder();
    sb.append("LeasesSearcher was called ").append(callCounter.get()).append(" times");
    Exception e = sqlUtilsLogger.getLastException();
    if( e != null ) {
      sb.append(", last exception was \"").append(e.getMessage()).append("\"");
    }
    statusLogger.info(sb.toString());
  }




  @Override
  public void inform(OID arg0, int arg1) {
    //ignore inform
    logger.debug("ingnore inform oid="+arg0.getOid());    
  }



  @Override
  public void trap(OID arg0, int arg1) {
    //ignore trap
    logger.debug("ingnore trap oid="+arg0.getOid());
  }
  
}
