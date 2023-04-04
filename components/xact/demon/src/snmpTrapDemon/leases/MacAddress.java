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

public class MacAddress {

  private int[] parts;
  private int hash;
  private String macString;
  
  private MacAddress() {/*kein Konstruktor*/}
  
  public MacAddress(int[] parts) {
    this.parts = parts;
  }
 
  public static MacAddress parse(int[] parts) {
    int[] loc = parts.clone();
    checkParts( loc );
    return new MacAddress( loc );
  }
  
  /**
   * Wandelt einen Hex-String der Form cc:cc:cc:cc:cc:cc in eine MacAddress
   * @param mac
   * @return
   */
  public static MacAddress parse(String mac) {
    int[] loc = parseToInts(mac);
    checkParts( loc );
    return new MacAddress( loc );
  }
  
  public static MacAddress parse(long mac) {
    int[] parts = new int[6];
    parts[5] = (int)  mac       & 255;
    parts[4] = (int) (mac >> 8) & 255;
    parts[3] = (int) (mac >>16) & 255;
    parts[2] = (int) (mac >>24) & 255;
    parts[1] = (int) (mac >>32) & 255;
    parts[0] = (int) (mac >>40) & 255;
    if( mac >>48 != 0 ) {
      throw new IllegalArgumentException("mac could not be parsed");
    }
    return new MacAddress( parts );
  }
  
  /**
   * Wandelt einen Hex-String der Form 000000000000 in eine MacAddress
   * @param mac
   * @return
   */
  public static MacAddress parseHex(String mac) {
    if( mac == null ) {
      throw new IllegalArgumentException("mac is null");
    }
    int[] parts = new int[6];
    for( int i=0;i<6; ++i ) {
      parts[i] = Integer.parseInt( mac.substring(2*i, 2*i+2), 16 );
    }
    return new MacAddress( parts );
  }

  @Override
  public String toString() {
    if( macString == null ) {
      StringBuilder sb = new StringBuilder();
      sb.append( parts[0] <16? "0":"").append( Integer.toHexString( parts[0] ) );
      for( int i=1; i<6; ++i ) {
        sb.append(':');
        sb.append( parts[i] <16? "0":"").append( Integer.toHexString( parts[i] ) );
      }
      macString = sb.toString();
    }
    return macString;
  }

  /**
   * Ausgabe als Hex-String, (Beispiel: "000000000000")
   * @return
   */
  public String toHex() {
    StringBuilder sb = new StringBuilder();
    sb.append( parts[0] <16? "0":"").append( Integer.toHexString( parts[0] ) );
    for( int i=1; i<6; ++i ) {
      sb.append( parts[i] <16? "0":"").append( Integer.toHexString( parts[i] ) );
    }
    return sb.toString();   
  }
  
  /**
   * Ausgabe als int-Array
   * @return
   */
  public int[] toIntArray() {
    return parts.clone();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if( ! ( obj instanceof MacAddress ) ) {
      return false;
    }
    int[] oparts = ((MacAddress)obj).parts;
    for (int i=0; i<6; i++) {
      if( parts[i]!=oparts[i] ) return false;
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      for (int i=0; i<6; i++) {
        h = 31*h + parts[i];
      }
      hash = h;
    }
    return h;
  }

  private static int[] parseToInts( String mac ) {
    if( mac == null ) {
      throw new IllegalArgumentException("mac is null");
    }
    String[] macParts = mac.split( ":" );
    if( macParts.length != 6 ) {
      throw new IllegalArgumentException("mac could not be parsed");
    }
    int[] parts = new int[6];
    for( int i=0;i<6; ++i ) {
      parts[i] = Integer.parseInt( macParts[i], 16 );
    }
    checkParts( parts );
    return parts;
  }
  
  private static void checkParts(int[] parts) {
    for( int i=0;i<6; ++i ) {
      if( parts[i] > 255 || parts[i] <0 ) {
        throw new IllegalArgumentException("invalid mac part");
      }
    }
  }

  
}
