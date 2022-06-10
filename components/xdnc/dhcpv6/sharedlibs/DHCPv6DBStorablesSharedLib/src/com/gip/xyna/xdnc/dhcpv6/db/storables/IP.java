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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6AddressUtil;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6SubnetUtil;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ResultSetReader;

public class IP {

  private static final Logger logger = CentralFactoryLogging.getLogger(IP.class);
  
  private final static int FFFF = 0xFFFF;

  private static class ReaderIP implements ResultSetReader<IP> {

    public IP read(ResultSet rs) throws SQLException {
      IP ps = new IP();
      IP.fillByResultSet(ps, rs);
      return ps;
    }

  };


  public static final String COL_IP = "ip";
  public static final String COL_PK = "primaryKey";
  public static final String COL_PREFIXLENGTH = "prefixlength";
  private static final long serialVersionUID = 1L;

  private static final ReaderIP reader = new ReaderIP();


  // caching
  private transient IPv6SubnetUtil subnetUtil;
  private transient IPv6AddressUtil addressUtil;


  @Column(name = COL_IP)
  private String ip;
  
  @Column(name = COL_PK)
  long primaryKey;
  
  @Column(name = COL_PREFIXLENGTH)
  private int prefixlength;// 128 -> IP-Adresse, sonst Prefix

  public static void fillByResultSet(IP ps, ResultSet rs) throws SQLException {
    ps.ip = rs.getString(COL_IP);
    ps.prefixlength = rs.getInt(COL_PREFIXLENGTH);
    ps.subnetUtil = null;
    ps.addressUtil = null;
  }


  public IP() {
  }


//  public static void main(String[] args) {
////    long l = 0 + 255 + (255 << 8) + (255 << 16) + ((0l + 255) << 24);    
////    System.out.println(Long.toString(l, 2));
////
////    Inet6Address ipv6;
////    try {
////      ipv6 = (Inet6Address) Inet6Address.getByName("1234:1::AB22");
////      System.out.println("ipv6: " +ipv6);
////    } catch (UnknownHostException e) {
////      // TODO Auto-generated catch block
////      e.printStackTrace();
////    }
//    
//    IP versuchsip = new IP("1200:0000:aaaa:0280::02e1:0000", 112);    
////    System.out.println("alte Adresse = " +versuchsip.ip);
////    versuchsip.add(-100);
////    System.out.println("neue Adresse = " +versuchsip.ip);
//    
//    IP otherVersuchsip = new IP("1200:0000:aaaa:0280::0280:0000",112);
//    System.out.println("Differenz = " +versuchsip.minus(otherVersuchsip));
//  }
  
 
  public IP(String ipstring, int prefixlength){
    this.ip = ipstring;
    this.prefixlength = prefixlength;
    this.subnetUtil = null;
    this.addressUtil = null;
  }

  
  public String getIp() {
    return ip;
  }
  
  public int getPrefixlength() {
    return prefixlength;
  }


  IPv6SubnetUtil getSubnet() {
    if (subnetUtil == null) {
      subnetUtil = IPv6SubnetUtil.createFromIPv6Address(ip, prefixlength);
    }
    return subnetUtil;
  }


  IPv6AddressUtil getAddressUtil() {
    if (addressUtil == null) {
      addressUtil = IPv6AddressUtil.parse(ip);
    }
    return addressUtil;
  }


  public int compareTo(IP o) {

    //bei prefixlength = 128 ist start==end, ansonsten ist "end" die letzte ip des subnets.
    //gibt 1 zurück, falls this.start > other.end
    //gibt -1 zurück, falls this.end < other.start
    //gibt ansonsten 0 zurück.

    IPv6SubnetUtil otherSubnet;
    if (o instanceof IP) {
      IP otherIp = (IP) o;
      otherSubnet = otherIp.getSubnet();
    } else {
      throw new RuntimeException();
    }

    IPv6SubnetUtil thisSubnet = getSubnet();
    IPv6AddressUtil thisStart = thisSubnet.calculateFirstIP();

    IPv6AddressUtil otherEnd = otherSubnet.calculateLastIP();
    if (otherEnd.compareTo(thisStart) < 0) {
      return 1;
    }
    IPv6AddressUtil thisEnd = thisSubnet.calculateLastIP();
    IPv6AddressUtil otherStart = otherSubnet.calculateFirstIP();
    if (otherStart.compareTo(thisEnd) > 0) {
      return -1;
    }
    return 0;
  }


  public void add(BigInteger diff) {

    IPv6AddressUtil util = getAddressUtil();
    if (prefixlength == 128) {
      //convert diff to IPv6 representation
      IPv6AddressUtil diffUtil;
      boolean diffBiggerThanZero = diff.compareTo(BigInteger.ZERO) >= 0;
      if (diffBiggerThanZero) {
        diffUtil = IPv6AddressUtil.parse(diff);
      } else {
        diffUtil = IPv6AddressUtil.parse(diff.negate());
      }
      IPv6AddressUtil newUtil;
      if (diffBiggerThanZero) {
        newUtil = IPv6AddressUtil.plus(util, diffUtil);
      } else {
        newUtil = IPv6AddressUtil.minus(util, diffUtil);
      }
      ip = newUtil.asLongString();
    } else {
      //falls prefixlength != 128, werden die verbleibenden Stellen auf 0 gesetzt
      IPv6AddressUtil prefixUtil = IPv6SubnetUtil.calculateIPv6PrefixAddress(util, prefixlength);
      //Prefix
      // Variante 1: in einer Schleife 
      int exponent = (128 - prefixlength);
      BigInteger factor = BigInteger.valueOf(2).pow(exponent);

      boolean diffBiggerThanZero = diff.compareTo(BigInteger.ZERO) >= 0;
      IPv6AddressUtil diffUtil;
      if (diffBiggerThanZero) {
        diff = diff.multiply(factor); //fixme
        diffUtil = IPv6AddressUtil.parse(diff);
      } else {
        diff = diff.negate().multiply(factor); //fixme
        diffUtil = IPv6AddressUtil.parse(diff);
      }
      IPv6AddressUtil newUtil;
      if (diffBiggerThanZero) {
        newUtil = IPv6AddressUtil.plus(prefixUtil, diffUtil);
      } else {
        newUtil = IPv6AddressUtil.minus(prefixUtil, diffUtil);
      }

      ip = newUtil.asLongString();
    }
    subnetUtil = null;
    addressUtil = null;
  }


  /**
   * Soll die Anzahl der im Intervall enthaltenen IP-Adressen bzw. Prefixes zurueckgeben (Grösse des Intervals inkl.
   * beider Randwerte wäre minus()+1)
   */
  public BigInteger minus(IP otherIp) {
    if (otherIp.prefixlength != prefixlength) {
      throw new RuntimeException("Error subtracting IPv6 addresses/prefixes - prefix length does not match");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("## Subtracting IP " + otherIp.ip + " from " + this.ip);
    }
    IPv6AddressUtil otherUtil = otherIp.getAddressUtil();
    IPv6AddressUtil util = getAddressUtil();
    if (prefixlength == 128) {
      try {
        IPv6AddressUtil newUtil = IPv6AddressUtil.minus(util, otherUtil);
        return newUtil.asBigInteger();
      } catch (ArithmeticException e) {
        logger.warn("got exception with this=" + this + " and otherIp=" + otherIp, e);
        throw e;
      }
    } else {
      //falls prefixlength != 128, werden die verbleibenden Stellen auf 0 gesetzt
      IPv6AddressUtil prefixUtil = IPv6SubnetUtil.calculateIPv6PrefixAddress(util, prefixlength);
      IPv6AddressUtil otherPrefixUtil = IPv6SubnetUtil.calculateIPv6PrefixAddress(otherUtil, otherIp.prefixlength);
      IPv6AddressUtil newUtil = IPv6AddressUtil.minus(prefixUtil, otherPrefixUtil);

      long div = (long) Math.pow(2, (128 - prefixlength));
      BigInteger nrPrefixes = newUtil.asBigInteger().divide(BigInteger.valueOf(div));

      return nrPrefixes;
    }

  }


  public String toString() {
    return ip + "/" + prefixlength;
  }


  void setIP(String newIp) {
    this.ip = newIp;
    this.addressUtil = null;
    this.subnetUtil = null;
  }


  void setIP(IPv6SubnetUtil subnetUtil, IPv6AddressUtil addressUtil) {
    this.addressUtil = addressUtil;
    this.subnetUtil = subnetUtil;
    this.ip = addressUtil.asLongString();
  }


  void setPrefixLength(int newPrefixLength) {
    this.subnetUtil = null;
    this.prefixlength = newPrefixLength;
  }

}
