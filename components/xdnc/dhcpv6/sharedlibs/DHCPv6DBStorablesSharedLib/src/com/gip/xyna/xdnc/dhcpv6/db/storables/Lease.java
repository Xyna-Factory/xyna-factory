package com.gip.xyna.xdnc.dhcpv6.db.storables;

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
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6AddressUtil;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6SubnetUtil;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.CompositeIndex;
import com.gip.xyna.xnwh.persistence.CompositeIndices;
import com.gip.xyna.xnwh.persistence.IndexTypeComposite;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xsor.protocol.XSORPayload;
import com.gip.xyna.xsor.protocol.XSORPayloadInformation;

@Persistable(tableName = Lease.TABLENAME, primaryKey = Lease.COL_IP)
@CompositeIndices(indices={@CompositeIndex(value=Lease.COL_SUPERPOOLID+",MAX("+Lease.COL_RESERVATIONTIME+","+Lease.COL_EXPIRATIONTIME+")",type=IndexTypeComposite.ORDERED_LEX),
                           //@CompositeIndex(value=Lease.COL_MAC+","+Lease.COL_IAID,type=IndexTypeComposite.UNIQUE),
                           @CompositeIndex(value=Lease.COL_MAC,type=IndexTypeComposite.HASH),
                           @CompositeIndex(value=Lease.COL_SUPERPOOLID,type=IndexTypeComposite.HASH)})
@XSORPayloadInformation(recordSize=Lease.RECORDSIZE, uniqueId=42)
public class Lease extends Storable<Lease> implements Comparable<Lease>, XSORPayload {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(Lease.class);
  
  public static final int RECORDSIZE = 5231;
  public static final String TABLENAME = "leasestable";
  public static final String COL_IP = "ip";
  public static final String COL_PREFIXLENGTH = "prefixlength";
  public static final String COL_MAC = "mac";
  //public static final String COL_MACASNUM = "macAsNum";
  public static final String COL_STARTTIME = "startTime";
  public static final String COL_RESERVATIONTIME = "reservationEnd";
  public static final String COL_IAID = "iaid";
  public static final String COL_TYPE = "type";
  public static final String COL_CMREMOTEID = "cmremoteid";
  public static final String COL_VENDORSPECINFO = "vendorSpecificInformation";
  public static final String COL_DPPINSTANCE = "dppInstance";
  
  public static final String COL_PREFERREDLIFETIME = "preferredlifetime";
  public static final String COL_VALIDLIFETIME = "validlifetime";
  public static final String COL_HARDWARETYPE = "hardwaretype";
  public static final String COL_DUIDTIME = "duidtime";
  public static final String COL_DYNDNSZONE = "dynDnsZone";
  
  public static final String COL_SUPERPOOLID = "superpoolid";
  public static final String COL_BINDING = "binding";
  public static final String COL_EXPIRATIONTIME ="expirationTime";
  
  public static final String COL_CMTSREMOTEID = "cmtsremoteid";
  public static final String COL_CMTSRELAYID = "cmtsrelayid";
  public static final String COL_CMTSIP = "cmtsip";
  
  private static final long serialVersionUID = 1L;
  //public static final int IPv6ADDRESSLENGTH = 128;//Prefixlength f�r komplette IPv6-Adresse
  //public static final String COL_PK = "leaseID";
  
  final static String CHARSET_ISO_8859_15_IDENTIFIER= "ISO-8859-15";// 8bit Encoding;
  final static Charset CHARSET_ISO_8859_15 = Charset.forName(CHARSET_ISO_8859_15_IDENTIFIER);
  

  private static class ReaderLease implements ResultSetReader<Lease> {

    public Lease read(ResultSet rs) throws SQLException {
      Lease ps = new Lease();
      Lease.fillByResultSet(ps, rs);
      return ps;
    }

  }


  public static final ResultSetReader<Lease> reader = new ReaderLease();


  private transient IPv6SubnetUtil subnet;
  private transient IPv6AddressUtil addressUtil;

  @Column(name = COL_IP)
  private String ip;

  
  @Column(name = COL_CMTSREMOTEID, size=128)
  private String cmtsremoteid;

  @Column(name = COL_CMTSRELAYID, size=128)
  private String cmtsrelayid;

  @Column(name = COL_CMTSIP, size=128)
  private String cmtsip;

  @Column(name = COL_PREFIXLENGTH)
  private int prefixlength;// 128 -> IPv6-Adresse, sonst Prefix

  @Column(name = COL_MAC, size=128)//, index = IndexType.UNIQUE)
  private String mac;
  
//  @Column(name = COL_MACASNUM, index = IndexType.UNIQUE)
//  private long macAsNum;

  @Column(name = COL_STARTTIME)
  private long startTime;

  @Column(name = COL_RESERVATIONTIME)
  private long reservationEnd;

//  @Column(name = COL_PK)
//  long leaseID;

  @Column(name = COL_IAID, size=128)
  private String iaid;

  @Column(name = COL_TYPE, size=128)
  private String type;

  @Column(name = COL_CMREMOTEID, size=128)
  private String cmremoteId;

  @Column(name = COL_VENDORSPECINFO, size=4096)
  private String vendorSpecificInformation;

  @Column(name = COL_DPPINSTANCE, size=128)
  private String dppInstance;

  @Column(name = COL_PREFERREDLIFETIME)
  private long preferredlifetime;

  @Column(name = COL_VALIDLIFETIME)
  private long validlifetime;

  @Column(name = COL_HARDWARETYPE)
  private long hardwaretype;

  @Column(name = COL_DUIDTIME)
  private long duidtime;

  @Column(name = COL_DYNDNSZONE, size=128)
  private String dyndnszone;

  @Column(name = COL_SUPERPOOLID)
  private long superpoolid;

  @Column(name = COL_BINDING, size=1)
  private String binding;

  @Column(name = COL_EXPIRATIONTIME)
  private long expirationtime;

  public Lease(IPv6SubnetUtil ip) {
    this.ip = ip.asLongString();
    this.prefixlength = ip.getPrefixLength();
    this.subnet = null;
  }
  
  public Lease(IPv6AddressUtil ip) {
    this.ip = ip.asLongString();
    this.prefixlength = 128;
    this.subnet = null;
  }

  public Lease() {
  }


  
  public long getExpirationtime() {
    return expirationtime;
  }

  
  public void setExpirationtime(long expirationtime) {
    this.expirationtime = expirationtime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getReservationEnd() {
    return reservationEnd;
  }

  public void setReservationTime(long reservationEnd) {
    this.reservationEnd = reservationEnd;
  }

//  public long getLeaseID() {
//    return leaseID;
//  }

//  public void setLeaseID(long leaseID) {
//    this.leaseID=leaseID;
//  }

  public int getPrefixlength() {
    return prefixlength;
  }
  
  public void setPrefixlength(int prefixlength) {
    this.prefixlength=prefixlength;
  }
  
  
  public String getIaid() {
    return iaid;
  }
  
  public void setIaid(String iaid) {
    this.iaid = iaid;
  }
  
  
  public String getBinding() {
    return binding;
  }

  
  public void setBinding(String binding) {
    this.binding = binding;
  }

  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
  
  public String getCmtsremoteid() {
    return cmtsremoteid;
  }

  
  public void setCmtsremoteid(String cmtsremoteid) {
    this.cmtsremoteid = cmtsremoteid;
  }

  
  public String getCmtsrelayid() {
    return cmtsrelayid;
  }

  
  public void setCmtsrelayid(String cmtsrelayid) {
    this.cmtsrelayid = cmtsrelayid;
  }

  
  public String getCmtsip() {
    return cmtsip;
  }

  
  public void setCmtsip(String cmtsip) {
    this.cmtsip = cmtsip;
  }

  public String getCMRemoteId() {
    return cmremoteId;
  }
  
  public void setCMRemoteId(String cmremoteId) {
    this.cmremoteId = cmremoteId;
  }

  public String getVendorSpecificInformation() {
    return vendorSpecificInformation;
  }
  
  public void setVendorSpecificInformation(String vendorSpecificInformation) {
    this.vendorSpecificInformation = vendorSpecificInformation;
  }
  
  public String getDppInstance() {
    return dppInstance;
  }
  
  public void setDppInstance(String dppInstance) {
    this.dppInstance = dppInstance;
  }
  
  public long getPreferredLifetime()
  {
    return preferredlifetime;
  }

  public void setPreferredLifetime(long preferredlifetime)
  {
    this.preferredlifetime = preferredlifetime;
  }

  public long getValidLifetime()
  {
    return validlifetime;
  }

  public void setValidLifetime(long validlifetime)
  {
    this.validlifetime = validlifetime;
  }

  public long getHardwareType()
  {
    return hardwaretype;
  }

  public void setHardwareType(long hardwaretype)
  {
    this.hardwaretype = hardwaretype;
  }

  public long getDUIDTime()
  {
    return duidtime;
  }

  public void setDUIDTime(long duidtime)
  {
    this.duidtime = duidtime;
  }
  
  public String getDynDnsZone()
  {
    return dyndnszone;
  }

  public void setDynDnsZone(String dyndnszone)
  {
     this.dyndnszone=dyndnszone;
  }

  public long getSuperPoolID(){
    return superpoolid;
  }

  public void setSuperPoolID(long superpoolid)
  {
     this.superpoolid=superpoolid;
  }


  
  public static void fillByResultSet(Lease lease, ResultSet rs) throws SQLException {
    
    //lease.ip = rs.getLong(COL_IP);
    lease.ip = rs.getString(COL_IP);
    lease.mac = rs.getString(COL_MAC);
    //lease.macAsNum = rs.getLong(COL_MACASNUM);
    lease.prefixlength = rs.getInt(COL_PREFIXLENGTH);
    lease.startTime = rs.getLong(COL_STARTTIME);
    lease.reservationEnd = rs.getLong(COL_RESERVATIONTIME);
    //lease.leaseID = rs.getLong(COL_PK);
    lease.iaid = rs.getString(COL_IAID);
    lease.type = rs.getString(COL_TYPE);
    lease.cmremoteId = rs.getString(COL_CMREMOTEID);
    lease.vendorSpecificInformation = rs.getString(COL_VENDORSPECINFO);
    lease.dppInstance = rs.getString(COL_DPPINSTANCE);
    lease.preferredlifetime = rs.getLong(COL_PREFERREDLIFETIME);
    lease.validlifetime = rs.getLong(COL_VALIDLIFETIME);
    lease.hardwaretype = rs.getLong(COL_HARDWARETYPE);
    lease.duidtime = rs.getLong(COL_DUIDTIME);
    lease.dyndnszone = rs.getString(COL_DYNDNSZONE);
    lease.superpoolid = rs.getLong(COL_SUPERPOOLID);
    lease.binding = rs.getString(COL_BINDING);
    lease.expirationtime = rs.getLong(COL_EXPIRATIONTIME);
    
    lease.cmtsip = rs.getString(COL_CMTSIP);
    lease.cmtsrelayid = rs.getString(COL_CMTSRELAYID);
    lease.cmtsremoteid = rs.getString(COL_CMTSREMOTEID);

    lease.subnet = null; // calculated lazily
  }


  public String getIp() {
    return ip;
  }


  public String getMac() {
    return mac;
  }
  
//  public long getMacAsNum() {
//    return macAsNum;
//  }


  @Override
  public ResultSetReader<? extends Lease> getReader() {
    return reader;
  }


  @Override
  public <U extends Lease> void setAllFieldsFromData(U data) {
    Lease leaseData = (Lease) data;
    ip = leaseData.ip;
    mac = leaseData.mac;
    //macAsNum = leaseData.macAsNum;
    prefixlength = leaseData.prefixlength;
    startTime = leaseData.startTime;
    reservationEnd = leaseData.reservationEnd;
    //leaseID = leaseData.leaseID;
    iaid = leaseData.iaid;
    type = leaseData.type;
    cmremoteId = leaseData.cmremoteId;
    vendorSpecificInformation = leaseData.vendorSpecificInformation;
    dppInstance = leaseData.dppInstance;
    preferredlifetime = leaseData.preferredlifetime;
    validlifetime = leaseData.validlifetime;
    hardwaretype = leaseData.hardwaretype;
    duidtime = leaseData.duidtime;
    dyndnszone = leaseData.dyndnszone;
    superpoolid=leaseData.superpoolid;
    binding=leaseData.binding;
    expirationtime=leaseData.expirationtime;
    
    cmtsip=leaseData.cmtsip;
    cmtsrelayid = leaseData.cmtsrelayid;
    cmtsremoteid = leaseData.cmremoteId;

    subnet = null; // created lazily
  }


  public int compareTo(Lease o) {
    if (!(o instanceof Lease)) {
      throw new RuntimeException();
    }
    Lease otherIp = (Lease) o;
    if (otherIp.prefixlength != prefixlength){
      throw new RuntimeException("Error comparing lease - prefix length does not match");
    }

    //falls prefixlength != 128, werden in den zu vergleichenden Adressen die verbleibenden Stellen auf 0 gesetzt
    IPv6AddressUtil otherUtil =
        IPv6SubnetUtil.calculateIPv6PrefixAddress(otherIp.getAddressUtil(), otherIp.prefixlength);
    IPv6AddressUtil util = IPv6SubnetUtil.calculateIPv6PrefixAddress(getAddressUtil(), prefixlength);

    int diff = util.compareTo(otherUtil);
    if (diff < 0)
      return -1;
    if (diff > 0)
      return 1;
    return 0;

  }


  IPv6AddressUtil getAddressUtil() {
    if (addressUtil == null) {
      addressUtil = IPv6AddressUtil.parse(ip);
    }
    return addressUtil;
  }


  public void add(long diff) {

    IPv6AddressUtil util = getAddressUtil();
    if (prefixlength == 128){
      //convert diff to IPv6 representation
      BigInteger bigint = BigInteger.valueOf(diff);
      IPv6AddressUtil diffUtil;
      if (diff >= 0) {
        diffUtil = IPv6AddressUtil.parse(bigint);
      } else {
        diffUtil = IPv6AddressUtil.parse(bigint.negate());
      }     
      IPv6AddressUtil newUtil;
      if (diff >= 0) {
        newUtil = IPv6AddressUtil.plus(util, diffUtil);
      } else {
        newUtil = IPv6AddressUtil.minus(util, diffUtil);
      }
      ip = newUtil.asLongString();
      addressUtil = null;//damit bei n�chster Anfrage mit passender IP neu erzeugt wird
    } else {
      //falls prefixlength != 128, werden die verbleibenden Stellen auf 0 gesetzt
      IPv6AddressUtil prefixUtil = IPv6SubnetUtil.calculateIPv6PrefixAddress(util, prefixlength);
      //Prefix
      // Variante 1: in einer Schleife 
      int exponent = (128-prefixlength);
      if (diff >=0){
        for (int i = 0; i < diff; i++){
          prefixUtil = prefixUtil.incrementBit(exponent);
        }
      } else {
        for (int i = 0; i < (-diff); i++){
          prefixUtil = prefixUtil.decrementBit(exponent);
        }
      }
      ip = prefixUtil.asLongString();
      addressUtil = null;
    }
    subnet = null;

  }
  

  public void setMac(String mac) {
    mac=mac.replaceAll(":", "");
    this.mac = mac;
  }
  
//  public void setMacAsNum(long macAsNum) {
//    this.macAsNum = macAsNum;
//  }


  @Override
  public Lease clone() {
    Lease clone = new Lease();
    clone.setAllFieldsFromData(this);
    return clone;
  }


  public boolean equalsKey(Lease other)
  {
    return(this.ip.equals(other.ip)&&this.prefixlength==other.prefixlength);
  }
  
  @Override
  public Object getPrimaryKey() {
    return ip;
    //return leaseID;
  }


  IPv6SubnetUtil getSubnet() {
    if (subnet == null) {
      subnet = IPv6SubnetUtil.createFromIPv6Address(ip, prefixlength);
    }
    return subnet;
  }
  
   
  public void copyIntoByteArray(byte[] data, int offset) {
    Arrays.fill(data, offset, offset+RECORDSIZE, (byte)0);//Notwendig ??
    System.arraycopy(ipToByteA(ip), 0, data, offset, 16);
    setInt(data,offset+16,prefixlength);
    if (mac != null && !mac.equals("")) System.arraycopy(macToByteA(mac), 0, data, offset+20, 6);
    setLong(data,offset+26,startTime);
    setLong(data,offset+34,reservationEnd);
    setString(data,offset+42,binding,1);
    setLong(data,offset+45,expirationtime);
    if (iaid != null) setString(data,offset+53,iaid,128);
    if (type != null) setString(data,offset+183,type,128);
    if (vendorSpecificInformation != null) setString(data,offset+313,vendorSpecificInformation,4096);
    if (dppInstance != null) setString(data,offset+4411,dppInstance,128);
    setLong(data,offset+4541,preferredlifetime);
    setLong(data,offset+4549,validlifetime);
    setLong(data,offset+4557,hardwaretype);
    setLong(data,offset+4565,duidtime);
    if (dyndnszone != null) setString(data,offset+4573,dyndnszone,128);
    setLong(data,offset+4703,superpoolid);
    if (cmtsremoteid != null) setString(data,offset+4711,cmtsremoteid,128);
    if (cmtsrelayid != null) setString(data,offset+4841,cmtsrelayid,128);
    if (cmtsip != null) setString(data,offset+4971,cmtsip,128);
    if (cmremoteId != null) setString(data,offset+5101,cmremoteId,128);
  }
 
 
  public XSORPayload copyFromByteArray(byte[] ba, int offset) {     
    Lease lease=new Lease();
    lease.ip=byteAToIp(Arrays.copyOfRange(ba, offset, offset+16));
    lease.prefixlength=getInt(ba,offset+16);
    lease.mac=byteAToMac(Arrays.copyOfRange(ba, offset+20, offset+20+6));
    lease.startTime=getLong(ba,offset+26);
    lease.reservationEnd=getLong(ba,offset+34);
    lease.binding=getString(ba,offset+42);
    lease.expirationtime=getLong(ba,offset+45);
    lease.iaid=getString(ba,offset+53);
    lease.type=getString(ba,offset+183);
    lease.vendorSpecificInformation=getString(ba,offset+313);
    lease.dppInstance=getString(ba,offset+4411);
    lease.preferredlifetime=getLong(ba,offset+4541);
    lease.validlifetime=getLong(ba,offset+4549);
    lease.hardwaretype=getLong(ba,offset+4557);
    lease.duidtime=getLong(ba,offset+4565);
    lease.dyndnszone=getString(ba,offset+4573);
    lease.superpoolid=getLong(ba,offset+4703);
    lease.cmtsremoteid=getString(ba,offset+4711);
    lease.cmtsrelayid=getString(ba,offset+4841);
    lease.cmtsip=getString(ba,offset+4971);
    lease.cmremoteId=getString(ba,offset+5101);
    return lease;
  }


  private String getString(byte[] data, int offset) {
    int length = ((data[offset] & 0x000000FF) << 8) + ((data[offset + 1] & 0x000000FF));
    byte[] b = Arrays.copyOfRange(data, offset + 2, offset + 2 + length);
    return new String(b, CHARSET_ISO_8859_15);
  }
  
  // the implementation below seems to be slightly faster (current ~ 45k-70k nanoSecs | below 27k-40k nanoSec) but is not throughly tested
  /*private String getString(byte[] data, int offset) {
    int length = ((data[offset] & 0x000000FF) << 8) + ((data[offset + 1] & 0x000000FF));
    CharBuffer buffer = CHARSET_ISO_8859_15.decode(ByteBuffer.wrap(data, offset + 2, length));
    return buffer.toString();
  }*/

 
  private long getLong(byte[] data, int offset) {
    return 
    ((data[offset] & 0x00000000000000FFl) << 56)     
    + ((data[offset + 1] & 0x00000000000000FFl) << 48) 
    + ((data[offset + 2] & 0x00000000000000FFl) << 40) 
    + ((data[offset + 3] & 0x00000000000000FFl) << 32)
    + ((data[offset+4] & 0x00000000000000FFl) << 24) 
    + ((data[offset + 5] & 0x00000000000000FFl) << 16) 
    + ((data[offset + 6] & 0x00000000000000FFl) << 8) 
    + (data[offset + 7] & 0x00000000000000FFl);
  }
 
  private int getInt(byte[] data, int offset) {
    return ((data[offset] & 0x000000FF) << 24) + 
       ((data[offset + 1] & 0x000000FF) << 16) + 
       ((data[offset + 2] & 0x000000FF) << 8) + 
       (data[offset + 3] & 0x000000FF);
  }
 
  public byte[] pkToByteArray(Object o) {
    return ipToByteA((String)o);
  }
 
  public Object byteArrayToPk(byte[] ba) {
    return byteAToIp(ba);
  }
 
   private void setString(byte[] data, int i, String value, int length) {
    byte[] toCopy = null;
    try{
      if (value==null){
        throw new RuntimeException("null value not supported in setString");
      }
      toCopy = value.getBytes(CHARSET_ISO_8859_15);
    } catch (Exception e){
      logger.error("got Exception setting String value:",e);
      throw new IllegalCharsetNameException("Charset "+CHARSET_ISO_8859_15_IDENTIFIER+" not found.");        
    }
    int len=Math.min(toCopy.length, length);
    System.arraycopy(toCopy, 0, data, i+2, len);
    Arrays.fill(data, i+2+len,i+2+length,(byte)0);
    data[i] = (byte) (len >>> 8);
    data[i + 1] = (byte) (len);
  }

 
 
  private void setLong(byte[] data, int i, long startTime2) {
    data[i+0] = (byte) (startTime2 >>> 56);
    data[i+1] = (byte) (startTime2 >>> 48);
    data[i+2] = (byte) (startTime2 >>> 40);
    data[i+3] = (byte) (startTime2 >>> 32);
    data[i+4] = (byte) (startTime2 >>> 24);
    data[i+5] = (byte) (startTime2 >>> 16);
    data[i+6] = (byte) (startTime2 >>> 8);
    data[i+7] = (byte) startTime2;
  }
 
 
  private void setInt(byte[] data, int i, int prefixlength2) {
    data[i+0] = (byte) (prefixlength2 >>> 24);
    data[i+1] = (byte) (prefixlength2 >>> 16);
    data[i+2] = (byte) (prefixlength2 >>> 8);
    data[i+3] = (byte) (prefixlength2 >>> 0);      
  }
 
  public static byte[] ipToByteA(String ip){
    byte[] ba=new byte[16];
    ip=ip.replaceAll(":", "");
    for(int i=0;i<16;i++){
      ba[i]=(byte)Integer.parseInt(ip.substring(i*2, i*2+2), 16);
    }
    return ba;
  }
  
  
  public static byte[] macToByteA(String mac){
    byte[] ba=new byte[6];
    for(int i=0;i<6;i++){
      ba[i]=(byte)Integer.parseInt(mac.substring(i*2, i*2+2), 16);
    }
    return ba;
  }
 
  
  private final static char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  
  
  public static String byteAToMac(byte[] ba){
    boolean allZeros = true;
    char[] macAsHexBytes = new char[ba.length*2];
    for (int i = 0; i < ba.length; i++) {
      int v = ba[i] & 0xff;
      int w = v >> 4;
      macAsHexBytes[i*2] =hexchars[w];
      if (allZeros && w != 0) {
        allZeros = false;
      }
      w = v & 0xf;
      macAsHexBytes[(i*2)+1] = hexchars[w];
      if (allZeros && w != 0) {
        allZeros = false;
      }
    }
    if (allZeros) {
      return ""; 
    } else {
      return new String(macAsHexBytes);
    }
  }
  
  
  public static String byteAToIp(byte[] ba){
    char[] ipAsHexBytes = new char[(ba.length*2)+7];
    int fillerInsertions = 0;
    for (int i = 0; i < ba.length; i++) {
      if (i != 0 && i % 2 == 0) {
        ipAsHexBytes[(i*2)+fillerInsertions] = ':';
        fillerInsertions++;
      }
      int v = ba[i] & 0xff;
      ipAsHexBytes[(i*2)+fillerInsertions] = hexchars[v >> 4];
      ipAsHexBytes[(i*2)+1+fillerInsertions] = hexchars[v & 0xf];
    }
    return new String(ipAsHexBytes);
  }
  
  
}
