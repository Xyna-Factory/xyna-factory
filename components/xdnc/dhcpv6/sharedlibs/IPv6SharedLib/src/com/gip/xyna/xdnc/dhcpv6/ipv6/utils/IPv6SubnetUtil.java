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
package com.gip.xyna.xdnc.dhcpv6.ipv6.utils;

/**
 * IPv6SubnetUtil liefert eine Darstellung eines IPv6-Subnetzes und bietet
 * einige Hilfsfunktionen f�r Manipulationen.
 *
 * IPv6Subnet ist immutable und threadsafe.
 *
 */
public class IPv6SubnetUtil implements IPv6Util{

  private IPv6AddressUtil network;
  private int prefixLength;


  /**
   * einziger, privater Konstruktor
   * @param network
   * @param prefixLength
   */
  private IPv6SubnetUtil(IPv6AddressUtil network, int prefixLength) {
    this.network = network;
    this.prefixLength = prefixLength;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj ) {
    if( obj == null ) return false;
    if( ! (obj instanceof IPv6SubnetUtil) ) return false;
    IPv6SubnetUtil other = (IPv6SubnetUtil)obj;
    return prefixLength==other.prefixLength && network.equals(other.network);
  }


  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * network.hashCode() + prefixLength;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return asShortString();
  }


  /**
   * @return
   */
  public String asLongString() {
    return network.asLongString()+"/"+prefixLength;
  }


  /**
   * @return
   */
  public String asShortString() {
    return network.asShortString()+"/"+prefixLength;
  }


  /**
   * IPv6SubnetParseException
   */
  public static class IPv6SubnetParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public IPv6SubnetParseException(String message) {
      super(message);
    }
  }


  /**
   * Parst einen IPv6Subnet-String in /-Notation, z.B. a::0/126
   * @param string
   * @return
   * @throws IPv6SubnetParseException
   */
  public static IPv6SubnetUtil parse(String string) {
    int pos = string.indexOf('/');
    if( pos == -1 ) {
      throw new IPv6SubnetParseException("IPv6-Check: no prefix-length found");
    }
    IPv6AddressUtil network = IPv6AddressUtil.parse( string.substring(0,pos) );
    int prefixLength = 0;
    try {
      prefixLength = Integer.parseInt( string.substring(pos+1) );
      checkPrefixLength(prefixLength);
    }
    catch (NumberFormatException e) {
      throw new IPv6SubnetParseException("IPv6-Check: no prefix-length found");
    }
    checkInterfaceIdentifierIsZero(network,prefixLength);
    //Anzahl der ben�tigten Nullen am Ende, damit dies ein Netzwerk sein kann
    int neededZeros = 128 - prefixLength;
    //vorhandene Nullen
    int zeros = network.numberOfTrailingZeros();
    if (zeros < neededZeros) {
      throw new IPv6SubnetParseException("IPv6-Check: prefix too small with prefix-length "+prefixLength);
    }
    return new IPv6SubnetUtil( network, prefixLength );
  }


  /**
   * Erzeugt ein IPv6SubnetUtil
   * @param network
   * @param prefixLength
   * @return
   * @throws IPv6SubnetParseException
   */
  public static IPv6SubnetUtil create(String network, int prefixLength) {
    IPv6AddressUtil ip=IPv6AddressUtil.parse(network);
    return create(ip, prefixLength);
  }


  /**
   * Erstellt das IPv6Subnet anhand einer
   * IPv6-Adresse die innerhalb des Subnets liegen darf,
   * es darf nat�rlich auch die IPv6-Subnetz-Adresse sein.
   * @param ipv6Address
   * @param prefixLength
   * @return
   */
  public static IPv6SubnetUtil createFromIPv6Address(String ipv6Address, int prefixLength) {
    return createFromIPv6Address(IPv6AddressUtil.parse(ipv6Address), prefixLength);
  }


  /**
   * Erstellt das IPv6Subnet anhand einer IPv6-Adresse die innerhalb des Subnets liegen darf, es darf nat�rlich auch die
   * IPv6-Subnetz-Adresse sein.
   */
  public static IPv6SubnetUtil createFromIPv6Address(IPv6AddressUtil ipv6Address, int prefixLength) {
    checkPrefixLength(prefixLength);
    IPv6AddressUtil net = calculateIPv6PrefixAddress(ipv6Address, prefixLength);
    checkInterfaceIdentifierIsZero(net, prefixLength);
    return new IPv6SubnetUtil(net, prefixLength);
  }


  /**
   * Berechnet die Prefix/Subnetz-Addresse von einer IPv6Adresse und gegebener PrefixLength.
   */
  public static IPv6AddressUtil calculateIPv6PrefixAddress(IPv6AddressUtil ipv6Address, int prefixLen) {
    for (int i = 0; i < (128 - prefixLen); i++) {
      ipv6Address = IPv6AddressUtil.parse(ipv6Address.asBigInteger().clearBit(i));
    }
    return ipv6Address;
  }


  /**
   * Erzeugt ein IPv6SubnetUtil
   */
  public static IPv6SubnetUtil create(IPv6AddressUtil network, int prefixLength) {
    checkPrefixLength(prefixLength);
    checkInterfaceIdentifierIsZero(network,prefixLength);
    return new IPv6SubnetUtil( network, prefixLength );
  }


  /**
   * Ist network ein passendes Subnet? (Sind alle Bits im Ger�teTeil 0?)
   */
  private static void checkInterfaceIdentifierIsZero(IPv6AddressUtil network, int prefixLength) {
    //Anzahl der ben�tigten Nullen am Ende, damit dies ein InterfaceIdentifier 0 sein kann
    int neededZeros = 128 - prefixLength;
    //vorhandene Nullen
    int zeros = network.numberOfTrailingZeros();
    if( zeros < neededZeros ) {
      throw new IPv6SubnetParseException("IPv6-Check: prefix too small with prefix-length "+prefixLength);
    }
  }


  /**
   * Ist PrefixLength g�ltig?
   */
  private static void checkPrefixLength(int prefixLength) {
    if( prefixLength <0 || prefixLength > 128 ) {
      throw new IPv6SubnetParseException( "IPv6-Check: invalid prefix length");
    }
  }


  /**
   * Liefert erste IP im Subnet
   */
  public IPv6AddressUtil calculateFirstIP() {
    return network;
  }


  /**
   * Liefert letzte IP im Subnet
   */
  public IPv6AddressUtil calculateLastIP() {
    return nextNetwork().decrement();
  }


  /**
   * Liefert das darauffolgende Subnetz mit gleicher PrefixLength
   * @return
   */
  public IPv6AddressUtil nextNetwork() {
    return network.incrementBit(128-prefixLength);
  }


  /**
   * Liefert das vorhergehende Subnetz mit gleicher PrefixLength
   * @return
   */
  public IPv6AddressUtil previousNetwork() {
    return network.decrementBit(128-prefixLength);
  }


  /**
   * Liefert das n�chste Network, welche die angegebenen PrefixL�nge hat
   * @param prefixLength
   * @return
   */
  public IPv6AddressUtil nextNetwork(int prefixLength) {
    //als erstes das eigene Subnetz verlassen:
    IPv6AddressUtil next = nextNetwork();
    int ntz = next.numberOfTrailingZeros(); //-> entspricht Position des letzten gesetzten Bits
    while( ntz < 128 - prefixLength ) {
      //solange das letzte Bit inkrementieren, bis next zur geforderten prefixLength passt
      next = next.incrementBit( ntz );
      ntz  = next.numberOfTrailingZeros();
    }
    return next;
  }


  /**
   * Liefert das vorhergehende Network, welche die angegebenen PrefixL�nge hat
   * @param prefixLength
   * @return
   */
  public IPv6AddressUtil previousNetwork(int prefixLength) {
    //als erstes das eigene Subnetz verlassen:
    IPv6AddressUtil prev = previousNetwork();
    int ntz = prev.numberOfTrailingZeros(); //-> entspricht Position des letzten gesetzten Bits
    while( ntz < 128 - prefixLength ) {
      //solange das letzte Bit dekrementieren, bis prev zur geforderten prefixLength passt
      prev = prev.decrementBit( ntz );
      ntz  = prev.numberOfTrailingZeros();
    }
    return prev;
  }


  /**
   * Liefert die PrefixLength
   */
  public int getPrefixLength() {
    return prefixLength;
  }


  /**
   * Liefert die Subnetz-Adresse
   * @return
   */
  public IPv6AddressUtil getNetwork() {
    return network;
  }


  /**
   * Liegt subnet in diesem Subnet?
   * @param subnet
   * @return
   */
  public boolean contains(IPv6SubnetUtil subnet) {
    if( prefixLength > subnet.prefixLength ) {
      return false; //Subnet muss gr��eren Prefix haben
    }
    return contains( subnet.network ); //Basisadresse des Subnet muss in this enthalten sein
  }


  /**
   * Liegt IP in diesem Subnet?
   * @param ip
   * @return
   */
  public boolean contains(IPv6AddressUtil ip ) {
    if( network.compareTo(ip) > 0 ) {
      return false; //IP-Adresse darf nicht kleiner sein
    }
    if( ip.compareTo(nextNetwork()) >= 0 ) {
      return false; //IP-Adresse darf nicht im NextNetwork oder dar�ber liegen
    }
    return true;
  }


}


