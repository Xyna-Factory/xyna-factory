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

import java.util.Arrays;

public final class IPAddress {

  /**
   * Diese Exception wird von IPAddress.parse(...) geworfen, wenn aus den übergebenen
   * Daten keine gültige IP-Adresse erstellt werden kann
   * 
   */
  public static class InvalidIPAddressException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidIPAddressException(String message) {
      super(message);
    }
    
  }
  
  private int[] parts;
  private String ip;
  private long ipl = -1;
  
  private IPAddress( int[] parts ) {
    this.parts = parts;
  }
  
  @Override
  public String toString() {
    if( ip == null ) {
      StringBuilder sb = new StringBuilder();
      sb.append(parts[0]);
      sb.append('.').append(parts[1]);
      sb.append('.').append(parts[2]);
      sb.append('.').append(parts[3]);
      ip = sb.toString();
    }
    return ip;
  }
  
  /**
   * liefert die IpAdresse als unsigned Integer, wegen unsigned als long
   * @return
   */
  public long toLong() {
    if( ipl == -1 ) {
      ipl = parts[0];
      ipl <<= 8;
      ipl += parts[1];
      ipl <<= 8;
      ipl += parts[2];
      ipl <<= 8;
      ipl += parts[3];
    }
    return ipl;
  }
  
  /**
   * liefert die IpAdresse in ihre 4 Teile zerlegt
   * @return
   */
  public int[] toInts() {
    return parts.clone();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if( ! ( obj instanceof IPAddress ) ) {
      return false;
    }
    return toLong() == ((IPAddress)obj).toLong();
  }
  
  @Override
  public int hashCode() {
    return (int) toLong();
  }
  
  /**
   * Bau einer IPAddress aus dem angegebenen String 
   * @param ip
   * @return
   * @throws InvalidIPAddressException
   */
  public static IPAddress parse( String ip ) throws InvalidIPAddressException {
    String[] partsS = ip.split( "\\." );
    if ( partsS.length != 4 ) {
      throw new InvalidIPAddressException("Invalid Ip-Address "+ip );
    }
    int[] partsI = new int[4];
    for( int i=0; i<4; ++i ) {
      int b = Integer.parseInt( partsS[i] );
      partsI[i] = Integer.parseInt( partsS[i] );
      if( (b < 0) || (b > 255) ) {
        throw new InvalidIPAddressException("Invalid Ip-Address "+ip );
      }
      partsI[i] = b;
    }
    return new IPAddress( partsI );
  }
  
  /**
   * Bau einer IPAddress aus dem angegebenen numerischen Ip-Addresse 
   * @param ip
   * @return
   * @throws InvalidIPAddressException
   */
  public static IPAddress parse( long ip ) throws InvalidIPAddressException {
    int[] partsI = new int[4];
    long ipl = ip;
    partsI[3] = (int) (ipl & 0xFF);
    ipl >>= 8;
    partsI[2] = (int) (ipl & 0xFF);
    ipl >>= 8;
    partsI[1] = (int) (ipl & 0xFF);
    ipl >>= 8;
    partsI[0] = (int) (ipl & 0xFF);
    ipl >>= 8;
    if( ipl > 0 ) {
      throw new InvalidIPAddressException("Invalid Ip-Address "+ip );
    }
    return new IPAddress( partsI );
  }
  
  /**
   * Bau einer IPAddress aus den vier Teilen 
   * @param ipParts
   * @return
   * @throws InvalidIPAddressException
   */
  public static IPAddress parse( int[] ipParts ) throws InvalidIPAddressException {
    int[] partsI = ipParts.clone();
    if ( partsI.length != 4 ) {
      throw new InvalidIPAddressException("Invalid Ip-Address "+ Arrays.toString(ipParts) );
    }
    for( int i=0; i<4; ++i ) {
      if( (partsI[i] < 0) || (partsI[i] > 255) ) {
        throw new InvalidIPAddressException("Invalid Ip-Address "+Arrays.toString(ipParts) );
      }
    }
    return new IPAddress( partsI ); 
  }
  
 
  
}
