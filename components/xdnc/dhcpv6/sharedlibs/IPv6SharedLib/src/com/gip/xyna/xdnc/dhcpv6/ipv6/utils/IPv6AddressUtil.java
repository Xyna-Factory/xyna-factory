/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xdnc.dhcpv6.ipv6.utils;

import java.math.BigInteger;
import java.util.regex.Pattern;


/**
 * IPv6AddressUtil liefert eine Darstellung einer IPv6-Addresse und bietet
 * einige Hilfsfunktionen für Manipulationen.
 *
 * IPv6Address ist immutable und threadsafe.
 *
 */
public class IPv6AddressUtil implements IPv6Util{


  private static final Pattern SPLIT_AT_COLON_PATTERN = Pattern.compile(":");
  private static final Pattern CONTAINS_DOUBLE_COLON_PATTERN = Pattern.compile(".*::.*");
  private static final Pattern REPLACE_PERCENT_SIGNS_PATTERN = Pattern.compile("\\%");


  private final static int FFFF = 0xFFFF;
  private int[] parts;


  /**
   * einziger, privater Konstruktor
   */
  private IPv6AddressUtil(int[] parts) {
    this.parts = parts;
  }


  @Override
  public boolean equals(Object obj ) {
    if( obj == null ) return false;
    if( ! (obj instanceof IPv6AddressUtil) ) return false;
    int[] otherParts = ((IPv6AddressUtil)obj).parts;
    for( int i=0;i<8; ++i ) {
      if( parts[i] != otherParts[i] ) return false;
    }
    return true;
  }


  @Override
  public int hashCode() {
    int hash = 1;
    for( int p : parts ) {
      hash = 31 * hash + p;
    }
    return hash;
  }


  @Override
  public String toString() {
    return asShortString();
  }


  /**
   * Vergleich zweier IPv6Addressen:
   * Liefert negative Zahl, wenn this < other; 0 bei this.equals(other) und positive Zahle bei this > other
   */
  public int compareTo(IPv6AddressUtil other) {
    for( int i=0; i<8; ++i ) {
      int diff =  parts[i] - other.parts[i]; //es kann kein Überlauf geben, daher ok
      if( diff != 0 ) {
        return diff;
      }
    }
    return 0;
  }


  /**
   * IPv6AddressParseException
   */
  public static class IPv6AddressParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public IPv6AddressParseException(String message) {
      super(message);
    }
  }


  /**
   * Parsen einer IPv6Address
   */
  public static IPv6AddressUtil parse(String string) {

    if (string == null) {
      throw new IPv6AddressParseException("IPv6-Check: invalid string: null");
    }

    int pos = string.indexOf("::", 0);
    if (pos >= 0) {
      if (string.indexOf("::", pos + 1) >= 0) {
        // Extra nochmal checken, da dies mit dem Check weiter unten nicht gefunden wird: 1234:20::400::
        throw new IPv6AddressParseException("IPv6-Check: multiple double-colons found");
      }
    }

    String[] stringParts = SPLIT_AT_COLON_PATTERN.split(string);
    if (stringParts.length != 8) {
      if (stringParts.length > 8) {
        throw new IPv6AddressParseException("IPv6-Check: invalid block count > 8");
      }
      if (!CONTAINS_DOUBLE_COLON_PATTERN.matcher(string).matches()) {
        throw new IPv6AddressParseException("IPv6-Check: invalid block count < 8");
      }
    }

    boolean nullBlock = false; //existiert ein leerer Block wie in "10ab:2::1234:abcd"
    int[] parts = new int[8];
    for (int i = 0, p = 0; i < stringParts.length; ++i, ++p) {
      if (stringParts[i].length() != 0) {
        //Block gefunden, Zahl parsen
        try {
          int block = Integer.parseInt(stringParts[i], 16);
          if (block < 0 || block > FFFF) {
            throw new IPv6AddressParseException("IPv6-Check: invalid number in block");
          }
          parts[p] = block;
        } catch (NumberFormatException e) {
          throw new IPv6AddressParseException("IPv6-Check: no number in block");
        }
      } else {
        //leerer Block
        if (nullBlock) {
          throw new IPv6AddressParseException("IPv6-Check: multiple double-colons found");
        }
        nullBlock = true;
        p += 8 - stringParts.length; //auffüllen
      }
    }
    return new IPv6AddressUtil(parts);
  }


  /**
   * Parsen einer IP in Form einer großen 128-Bit-Zahl
   * @param val
   * @return
   */
  public static IPv6AddressUtil parse(BigInteger val) {
    int[] parts = new int[8];
    BigInteger div = BigInteger.valueOf( 0x10000 );
    for( int p=7; p>=0; --p ) {
      BigInteger[] dar = val.divideAndRemainder(div);
      val      = dar[0];
      parts[p] = dar[1].intValue();
    }
    if( val.equals(BigInteger.ZERO) ) {
      return new IPv6AddressUtil(parts);
    } else {
      throw new IPv6AddressParseException("value too large");
    }
  }


  /**
   * Ausgabe in der kurzen Form. Bsp: "2001:db8:0:8d3:0:8a2e:70:7344"
   * Aufeinanderfolgende Nullen werden jedoch nicht durch "::" eingespart.
   * @return
   */
  public String asShortString() {
    StringBuilder sb = new StringBuilder();
    appendHexString( sb, parts[0], false );
    for( int i=1; i<8; ++i ) {
      sb.append(":");
      appendHexString( sb, parts[i], false );
    }
    return sb.toString();
  }


  /**
   * Ausgabe in der langen Form mit jeweils vier Stellen. Bsp: "2001:0db8:0000:08d3:0000:8a2e:0070:7344"
   * @return
   */
  public String asLongString() {
    StringBuilder sb = new StringBuilder();
    appendHexString( sb, parts[0], true );
    for( int i=1; i<8; ++i ) {
      sb.append(":");
      appendHexString( sb, parts[i], true );
    }
    return sb.toString();
  }


  /**
   * Hängt die Zahl i als HexString an sb an
   * @param sb
   * @param i
   * @param leadingZeros falls true: Leftpadding mit 0
   */
  private void appendHexString(StringBuilder sb, int i, boolean leadingZeros) {
    String hex = Integer.toHexString(i);
    if (leadingZeros) {
      switch (hex.length()) {
        case 1: sb.append("000"); break;
        case 2: sb.append("00");  break;
        case 3: sb.append("0");   break;
        case 4: break;
        default: throw new IllegalStateException("unexpected string length for hex '"+hex+"'" );
      }
    }
    sb.append(hex);
  }


  /**
   * Ausgabe als lange Zahl, bis zu 39 Stellen
   * @return
   */
  public String asNumberString() {
    return asBigInteger().toString();
  }


  /**
   * Ausgabe als lange Zahl, bis zu 39 Stellen
   * @return
   */
  public BigInteger asBigInteger() {
    BigInteger bi = BigInteger.ZERO;
    bi = bi.add( BigInteger.valueOf( ((long)parts[0] << 16 ) +  parts[1] ) );
    bi = bi.shiftLeft( 32 );
    bi = bi.add( BigInteger.valueOf( ((long)parts[2] << 16 ) +  parts[3] ) );
    bi = bi.shiftLeft( 32 );
    bi = bi.add( BigInteger.valueOf( ((long)parts[4] << 16 ) +  parts[5] ) );
    bi = bi.shiftLeft( 32 );
    bi = bi.add( BigInteger.valueOf( ((long)parts[6] << 16 ) +  parts[7] ) );
    return bi;
  }


  /**
   * Inkrementieren der IP um 1
   * @return
   */
  public IPv6AddressUtil increment() {
    int[] partsInc = parts.clone();
    partsInc[7] += 1; //increment
    return new IPv6AddressUtil( carryIncrement( partsInc ) );
  }


  /**
   * Dekrementieren der IP um 1
   * @return
   */
  public IPv6AddressUtil decrement() {
    int[] partsDec = parts.clone();
    partsDec[7] -= 1; //decrement
    return new IPv6AddressUtil( carryDecrement( partsDec ) );
  }


  /**
   * Inkementieren des Bits bit.
   * Entspricht einer Addition der Zweierpotenz, 2^(128-bit)
   * @param bit
   * @return
   */
  public IPv6AddressUtil incrementBit(int bit) {
    if( bit<0 || bit >= 128 ) {
      throw new IllegalArgumentException("invalid bit");
    }
    int[] partsInc = parts.clone();
    int div = bit/16;
    int mod = bit%16;
    //System.err.println( div + " " + mod );
    partsInc[7-div] += (1<<mod); //Inkrement
    return new IPv6AddressUtil( carryIncrement( partsInc ) );
  }


  /**
   * Dekementieren des Bits bit.
   * Entspricht einer Subtraktion der Zweierpotenz, 2^(128-bit)
   * @param bit
   * @return
   */
  public IPv6AddressUtil decrementBit(int bit) {
    if( bit<0 || bit >= 128 ) {
      throw new IllegalArgumentException("invalid bit");
    }
    int[] partsDec = parts.clone();
    int div = bit/16;
    int mod = bit%16;
    //System.err.println( div + " " + mod );
    partsDec[7-div] -= (1<<mod); //Dekrement
    return new IPv6AddressUtil( carryDecrement( partsDec ) );
  }


  /**
   * Berücksichtigung des Übertrags bei Increments
   * @param partsInc
   * @return
   */
  private int[] carryIncrement(int[] partsInc) {
    boolean carry = false;
    for( int p=7; p>=0; --p ) {
      int val     = partsInc[p] + (carry?1:0);  //aktuellen Wert, evtl. erhöhen;
      carry       = val > FFFF;                 //weiterer Übertrag nötig?
      partsInc[p] = val - (carry ? FFFF+1 : 0); //Wert setzen, bei Übertrag aktuellen Wert erniedrigen
    }
    return partsInc;
  }


  /**
   * Berücksichtigung des Übertrags bei Decrements
   * @param partsDec
   * @return
   */
  private int[] carryDecrement(int[] partsDec) {
    boolean carry = false;
    for( int p=7; p>=0; --p ) {
      int val     = partsDec[p] - (carry?1:0);  //aktuellen Wert, evtl. erniedrigen
      carry       = val < 0;                    //weiterer Übertrag nötig?
      partsDec[p] = val + (carry ? FFFF+1 : 0); //Wert setzen, bei Übertrag aktuellen Wert erhöhen
    }
    return partsDec;
  }


  /**
   * Liefert die Anzahl der 0 am Geräte-Teil-Ende
   * @return
   */
  public int numberOfTrailingZeros() {
    int count = 0;
    for( int p=7; p>=0; --p ) {
      if( parts[p] == 0 ) {
        count += 16;
      } else {
        return count + Integer.numberOfTrailingZeros( parts[p] );
      }
    }
    return count;
  }


  /**
   * Liefert die Anzahl der führenden Nullen (Start Netzwerk-Teil)
   * @return
   */
  public int numberOfLeadingZeros() {
    int count = 0;
    for( int p=0; p<8; ++p ) {
      if( parts[p] == 0 ) {
        count += 16;
      } else {
        return count + Integer.numberOfLeadingZeros( parts[p] )-16;
      }
    }
    return count;
  }


  /**
   * Setzt Block auf angebenenen Wert.
   * Blocknummerierung ist von links (0) nach rechts (7)
   * @param block 0-7
   * @param value 0-0xFFFF (65535)
   * @return neue IPv6Address
   */
  public IPv6AddressUtil setBlock(int block, int value) {
    if (block < 0 || block > 7) {
      throw new IllegalArgumentException("invalid block: "+block+", (possible values: [0-7])");
    }
    if (value < 0 || value > FFFF) {
      throw new IllegalArgumentException("invalid value");
    }
    int[] partsNew = parts.clone();
    partsNew[block] = value;
    return new IPv6AddressUtil(partsNew);
  }


  /**
   * Summe zweier IP-Adressen
   * @param summand1
   * @param summand2
   * @return
   * @throws ArithmeticException wenn Summe keine IP-Adresse ergibt
   */
  public static IPv6AddressUtil plus( IPv6AddressUtil summand1, IPv6AddressUtil summand2 ) throws ArithmeticException {
    int[] parts = new int[8];
    boolean carry = false;
    for( int p=7; p>=0; --p ) {
      parts[p] = summand1.parts[p] + summand2.parts[p] + (carry?1:0);
      if( parts[p] > FFFF ) {
        parts[p] -= (FFFF+1);
        carry = true;
      } else {
        carry = false;
      }
    }
    if (carry) {
      throw new ArithmeticException("Sum too large");
    }
    return new IPv6AddressUtil(parts);
  }


  /**
   * Differenz zweier IP-Adressen
   * @throws ArithmeticException Wenn subtrahend > minuend, also Differenz negativ wäre
   */
  public static IPv6AddressUtil minus( IPv6AddressUtil minuend, IPv6AddressUtil subtrahend ) throws ArithmeticException {
    int[] parts = new int[8];
    boolean carry = false;
    for (int p = 7; p >= 0; --p) {
      parts[p] = minuend.parts[p] - subtrahend.parts[p] - (carry ? 1 : 0);
      if (parts[p] < 0) {
        parts[p] += (FFFF + 1);
        carry = true;
      }
      else {
        carry = false;
      }
    }
    if (carry) {
      throw new ArithmeticException("subtrahend > minuend");
    }
    return new IPv6AddressUtil(parts);
  }


  /**
   * Konvertiert einen IPv6-Such-String zu einem Suchstring in Lang-Form.
   * Dabei darf der IPv6-String Wildcards enthalten.</BR>
   * Beispiele:</BR>
   * 0::0:011*:12_3:0 => 0000:0000:0000:0000:0000:011*:12_3:0000</BR>
   * 0::0:011*        => 0000:0000:0000:0000:0000:0000:0000:011*</BR>
   * 1:*              => 0001:*</BR>
   * 0::0::1*         => Fehler!</BR>
   *
   * Wird für die Suche von IPv6-Adressen in der DB benötigt.
   * @param ipv6Prefix
   * @return
   * @throws IPv6AddressParseException (Runtime-Exceptions)
   */
  public static String convertSearchStr2LongSearchStr(String string) throws Exception {
    String retVal = string;

    if (string == null || string.length() == 0) {
      throw new IPv6AddressParseException("IPv6-Check: invalid string: null");
    }

    int pos = string.indexOf("::", 0);
    if (pos >= 0) {
      if (string.indexOf("::", pos + 1) >= 0) {
        // Extra nochmal checken, da dies mit dem Check weiter unten nicht gefunden wird: 1234:20::400::
        throw new IPv6AddressParseException("IPv6-Check: multiple double-colons found");
      }
    }

    String[] stringParts = SPLIT_AT_COLON_PATTERN.split(string);
    if (stringParts.length != 8) {
      if (stringParts.length > 8) {
        throw new IPv6AddressParseException("IPv6-Check: invalid block count > 8");
      }
    }

    string = REPLACE_PERCENT_SIGNS_PATTERN.matcher(string).replaceAll("*"); // macht es später ein wenig einfacher
    boolean nullBlock = false; // existiert ein leerer Block wie in "10ab:2::1234:abcd"
    String[] parts = new String[8];
    for (int i = 0, p = 0; i < stringParts.length; ++i, ++p) {
      if (stringParts[i].length() != 0) {
        // Block gefunden
        parts[p] = stringParts[i];
      } else {
        // leerer Block
        if (nullBlock) {
          throw new IPv6AddressParseException("IPv6-Check: multiple double-colons found");
        }
        nullBlock = true;
        p += 8 - stringParts.length; // auffüllen
      }
    }

    retVal="";
    String lastChar="";
    boolean breackByLastAsterisk=false;

    // jetzt nochmal alle Parts durchgehen und ggf. auffüllen:
    for (int j=0; j<parts.length; j++) {

      if (lastChar.equals("*")) { // NUR für diesen Fall:   '1:*' => '0001:*'
        breackByLastAsterisk=true;
        for (int k=j; k<parts.length; k++) {
          if (parts[k]!=null) {
            breackByLastAsterisk=false;
          }
        }
        if (breackByLastAsterisk==true) {
          break;
        }
      }

      if (parts[j]==null) {
        parts[j]="0000"; // kommt vor, wenn mit '::' im Suchstring ist
      }

      if (parts[j].indexOf("_")>=0 ||
          parts[j].indexOf("*")>=0 ) {
        // nichts machen, da Wildcard gesetzt!

      }
      else {
        switch (parts[j].length()) {
          case 0: parts[j]="0000";          break;
          case 1: parts[j]="000"+parts[j];  break;
          case 2: parts[j]="00"+parts[j];   break;
          case 3: parts[j]="0"+parts[j];    break;
          case 4: break;
          default: throw new IllegalStateException("unexpected string length for block '"+parts[j]+"'" );
        }
      }
      lastChar=""+parts[j].toCharArray()[parts[j].length()-1];
      retVal=retVal+parts[j]+":";
    }
    return retVal.substring(0, retVal.length()-1);
  }


}


