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
package com.gip.xyna.utils.snmp;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel;
import com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModels;

/**
 * SNMP access data.
 */
public final class SnmpAccessData {

  static Logger logger = Logger.getLogger(SnmpAccessData.class.getName());

  public static final String MD5 = "MD5";
  public static final String SHA1 = "SHA1";
  public static final String DES56 = "DES56";
  public static final String AES128 = "AES128";

  public static final String VERSION_1 = "v1";
  public static final String VERSION_2c = "v2c";
  public static final String VERSION_3 = "v3";


  private String host;
  private int port;
  private String username;
  private String community;
  private String authenticationPassword;
  private String privacyPassword;
  private String version;
  private String authenticationProtocol;
  private String privacyProtocol;
  private int[] retryIntervals;
  private String timeoutModel;
  private int[] timeoutModelData;
  private String engineId;

  private SnmpAccessData(){/*Konstruktor darf nur intern verwendet werden*/}

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    
    sb.append("SNMP").append(version).append("(");
    if (isSNMPv3()) {
      sb.append(username).append("@");
    }
    sb.append(host).append(":").append(port);
    if (isSNMPv3()) {
      if (authenticationProtocol != null) {
        sb.append(",auth=").append(authenticationProtocol);
      }
      if (privacyProtocol != null) {
        sb.append(",priv=").append(privacyProtocol);
      }
    }
    sb.append(",timeoutModel=").append(timeoutModel).append(Arrays.toString(timeoutModelData));
    sb.append(")");
    return sb.toString();
  }
  
  public static SADBuilder newSNMP(String version) {
    return new SADBuilder(version);
  }

  public static SADBuilder newSNMPv1() {
    return new SADBuilder(VERSION_1);
  }
  
  public static SADBuilder newSNMPv2c() {
    return new SADBuilder(VERSION_2c);
  }

  public static SADBuilder newSNMPv3() {
    return new SADBuilder(VERSION_3);
  }

  public static SADBuilder copy(SnmpAccessData sadOrig) {
    return new SADBuilder(sadOrig);
  }

  public static class SADBuilder {

    private SnmpAccessData sad = new SnmpAccessData();

    public SADBuilder(String version) {
      if( VERSION_1.equals(version) ) {
        sad.version = VERSION_1;
      } else if( VERSION_2c.equals(version) ) {
        sad.version = VERSION_2c;
      } else if( VERSION_3.equals(version) ) {
        sad.version = VERSION_3;
      } else {
        throw new IllegalArgumentException( "Unknown version "+ version);
      }
      sad.port = Integer.MIN_VALUE;
    }

    public SADBuilder(SnmpAccessData sadOrig) {
      sad.host                   = sadOrig.host;
      sad.port                   = sadOrig.port;
      sad.username               = sadOrig.username;
      sad.community              = sadOrig.community;
      sad.authenticationPassword = sadOrig.authenticationPassword;
      sad.privacyPassword        = sadOrig.privacyPassword;
      sad.version                = sadOrig.version;
      sad.authenticationProtocol = sadOrig.authenticationProtocol;
      sad.privacyProtocol        = sadOrig.privacyProtocol;
      sad.retryIntervals         = sadOrig.retryIntervals;
      sad.timeoutModel           = sadOrig.timeoutModel;
      sad.timeoutModelData       = sadOrig.timeoutModelData;
      sad.engineId               = sadOrig.engineId;
    }

    public SADBuilder host(String host) {
      sad.host = host;
      return this;
    }

    public SADBuilder port(int port) {
      sad.port = port;
      return this;
    }

    public SADBuilder port(String port) {
      sad.port = Integer.parseInt(port);
      return this;
    }

    public SADBuilder username(String username) {
      sad.username = username;
      return this;
    }

    public SADBuilder community(String community) {
      sad.community = community;
      return this;
    }
    
    /**
     * eindeutige id fuer diese SNMP Instanz (default = local IP)
     * siehe rfc3414
     * @param engineId
     * @return
     */
    public SADBuilder engineId(String engineId) {
      sad.engineId = engineId;
      return this;
    }

    public SADBuilder authenticationPassword(String authenticationPassword) {
      sad.authenticationPassword = authenticationPassword;
      return this;
    }

    /**
     * erlaubt: {@link #MD5}, {@link #SHA1}
     * @param protocol
     * @return
     */
    public SADBuilder authenticationProtocol(String protocol) {
      if( protocol == null ) {
        return this;
      }
      if( protocol.equalsIgnoreCase(SnmpAccessData.MD5) ) {
        sad.authenticationProtocol = SnmpAccessData.MD5;
        return this;
      }
      if( protocol.equalsIgnoreCase(SnmpAccessData.SHA1) ) {
        sad.authenticationProtocol = SnmpAccessData.SHA1;
        return this;
      }
      throw new IllegalArgumentException( "unknown protocol "+protocol );
    }

    public SADBuilder privacyPassword(String privacyPassword) {
      sad.privacyPassword = privacyPassword;
      return this;
    }

    /**
     * erlaubt: {@link #DES56}, {@link #AES128}
     * @param protocol
     * @return
     */
    public SADBuilder privacyProtocol(String protocol) {
      if( protocol == null ) {
        return this;
      }
      if( protocol.equalsIgnoreCase(SnmpAccessData.DES56) ) {
        sad.privacyProtocol = SnmpAccessData.DES56;
        return this;
      }
      if( protocol.equalsIgnoreCase(SnmpAccessData.AES128) ) {
        sad.privacyProtocol = SnmpAccessData.AES128;
        return this;
      }
      throw new IllegalArgumentException("Unknown protocol: <" + protocol + ">.");
    }

    /**
     * Sets the timeout-Model
     * Known Models: 
     * a) "interval", list of n timeouts, n requests total -> n-1 retries;
     * @param name
     * @param params
     * @return
     */
    public SADBuilder timeoutModel(String name, int ... params ) {
      sad.timeoutModel = name;
      sad.timeoutModelData = params;
      return this;
    }
    
    /**
     * Sets the timeout-Model
     * Known Models: 
     * a) "simple": constant timeouts for all retries, retries+1 requests
     * @param name
     * @param retries
     * @param timeout
     * @return
     */
    public SADBuilder timeoutModel(String name, int retries, int timeout ) {
      sad.timeoutModel = name;
      sad.timeoutModelData = new int[]{retries,timeout};
      return this;
    }
    
    /**
     * Sets the retry intervals of the PDU. The length of the csv
     * corresponds with the number of retries. Each entry in the csv
     * is the number of milliseconds of each try.
     * The default is "500,1000,2000,5000,5000".
     * It is good practice to make the interval bigger with each retry,
     * if the numbers are the same the chance of collision is higher.
     *
     * @param retryIntervals
     * @deprecated Use timeoutModel( "interval", retryIntervalsAsIntArray) instead
     * @return
     */
    public SADBuilder retryIntervals(String retryIntervals ) {
      if( retryIntervals == null || retryIntervals.length() == 0 ) {
        //sad.retryIntervals = new int[]{500,1000,2000,5000,5000};
        timeoutModel( "intrval", new int[]{500,1000,2000,5000,5000} );
        return this;
      }
      String[] retries = retryIntervals.split(",");
      int[] retryIntervalsI = new int[retries.length];
      for( int i=0;i<retries.length; ++i ) {
        retryIntervalsI[i] = Integer.parseInt(retries[i]);
      }
      timeoutModel( "intrval", retryIntervalsI );
      return this;
    }

    /**
     * Checks necessary data, build the SnmpAccessData
     * @return
     */
    public SnmpAccessData build() {
      if( sad.host == null || sad.host.length() == 0 ) {
        throw new IllegalStateException( "host not set");
      }
      if( sad.port <= 0 ) {
        throw new IllegalStateException( "port not set");
      }
      if( sad.timeoutModel == null ) {
        sad.timeoutModel = "default";
        sad.timeoutModelData = new int[]{};
      } else {
        String check = TimeoutModels.check( sad.timeoutModel, sad.timeoutModelData );
        if( check != null ) {
          throw new IllegalStateException( "Invalid timeoutModel: "+check );
        }
      }
      if ((sad.privacyProtocol != null && sad.privacyPassword == null) || (sad.privacyProtocol == null && sad.privacyPassword != null)) {
        throw new IllegalStateException("only one of privacyprotocol and privacypassword is set.");
      }
      if ((sad.privacyProtocol != null || sad.authenticationProtocol != null) && sad.username == null) {
        throw new IllegalStateException("username not set.");
      }
      return sad;
    }
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the community
   */
  public String getCommunity() {
    return community;
  }

  /**
   * @return the authenticationPassword
   */
  public String getAuthenticationPassword() {
    return authenticationPassword;
  }

  /**
   * @return the privacyPassword
   */
  public String getPrivacyPassword() {
    return privacyPassword;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  public boolean isSNMPv1() {
    return version == VERSION_1;
  }
  public boolean isSNMPv2c() {
    return version == VERSION_2c;
  }
  public boolean isSNMPv3() {
    return version == VERSION_3;
  }

  /**
   * @return the authenticationProtocol
   */
  public String getAuthenticationProtocol() {
    return authenticationProtocol;
  }

  /**
   * @return the privacyProtocol
   */
  public String getPrivacyProtocol() {
    return privacyProtocol;
  }

  
  
  public TimeoutModel getTimeoutModel() {
    return TimeoutModels.newTimeoutModel(timeoutModel,timeoutModelData);
  }
  
  /**
   * @return the retryIntervals as String
   * @deprecated
   */
  public String getRetryIntervalsString() {
    String ri = arrayToString( retryIntervals );
    return ri;
  }

  /**
   * gives out the retryIntervals; 
   * @return retryIntervals as int[]
   * @deprecated
   */
  public int[] getRetryIntervalIntArray() {
    if( retryIntervals != null ) {
      return retryIntervals.clone();
    } else {
      return null;
    }
  }

  /**
   * converts the array to comma-separated string
   * @param array
   * @return
   */
  private String arrayToString(int[] array) {
    if( array == null ) {
      return null;
    }
    if( array.length == 0 ) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    sb.append( array[0] );
    for( int i=1; i<array.length; ++i ) {
      sb.append(",").append( array[i] );
    }
    return sb.toString();
  }

  public String getEngineId() {
    return engineId;
  }
}
