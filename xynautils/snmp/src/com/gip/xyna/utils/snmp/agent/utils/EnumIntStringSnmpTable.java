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
package com.gip.xyna.utils.snmp.agent.utils;


import com.gip.xyna.utils.snmp.NextOID;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.NextOID.NoNextOIDException;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * EnumIntStringSnmpTable ist die Implementation eines OidSingleHandlers, die sich um die 
 * Doppel-Tabellen mit Int und String-Eintraegen kuemmert, die einen gut lesbaren Walk ergeben.
 * 
 * Die Tabelle erlaubt derzeit nur Leseoperationen.
 * 
 * Beispiel:
 * oidBase.1.1 = Int1
 * oidBase.1.2 = IntB
 * oidBase.2.1 = String1
 * oidBase.2.2 = StringB
 * 
 * Die einzelnen Zeilen in der Tabelle werden durch {@code addLeaf} in die EnumIntStringSnmpTable
 * eingehaengt.  
 * Dis ist fuer {@link IntSource} sehr einfach, ansonsten muss das Interface 
 * {@link EnumIntStringSnmpTable.Leaf} verwendet werden. 
 * 
 * EnumIntStringSnmpTable verwendet einen enum, um die richtige und lueckenlose Reihenfolge der 
 * Zeilen sicherzustellen.
 */
public class EnumIntStringSnmpTable<Index extends Enum<Index>> extends ChainedOidSingleHandler {

  protected OID oidBase;
  protected int oidBaseLength;
  protected int getSetLength;
  protected int indexPos;
  private Leaf[] leaves;
  protected int leavesSize;
  protected NextOID nextOID;
  private ChainedOidSingleHandler nextOHS;
  
  /**
   * Ueber dieses Interface koennen einzelne Tabellenzeilen in die EnumIntStringSnmpTable 
   * eingetragen werden.
   *
   */
  public static interface Leaf {
    /**
     * Darstellung als Integer
     * @return 
     */
    public Integer asInt();
    /**
     * Darstellung als String
     * @return
     */
    public String asString();
  }
  
  public static interface WriteableLeaf extends Leaf {
    /**
     * Kann der Leaf derzeit gesetzt werden? 
     * @return
     */
    public boolean canSet();
    /**
     * Setzt den Integer-Value
     * @param value
     * @return false bei fehlerhaftem Value
     */
    public boolean fromInt( Integer value );
    /**
     * Setzt den String-Value
     * @param value
     * @return false bei fehlerhaftem Value
     */
    public boolean fromString( String value );
    
  }
  
  
  
  /**
   * Implementierung eines Leaf, der einen IntSource als Datenquelle benutzt
   *
   */
  private static class IntSourceLeaf implements Leaf {

    private String message;
    private IntSource intSource;

    /**
     * Konstruktor
     * @param message
     * @param intSource
     */
    public IntSourceLeaf(String message, IntSource intSource) {
      this.message = message;
      this.intSource = intSource;
    }

    public Integer asInt() {
      return Integer.valueOf( intSource.getInt() );
    }
    
    public String asString() {
      return message+": "+asInt();
    }
  }
  
  private static class StringIntLeaf implements Leaf {
    private String s;
    private Integer i;
    public StringIntLeaf(String s, Integer i) {
      this.s=s;
      this.i=i;
    }
    public Integer asInt() {
      return i;
    }
    public String asString() {
      return s;
    }
  }

  /**
   * @param indexType
   * @param oidBase
   */
  public EnumIntStringSnmpTable(Class<Index> indexType, OID oidBase) {
    this(indexType,oidBase,ChainedOidSingleHandler.WALK_END);
  }
  
  /**
   * @param indexType
   * @param oidBase
   * @param nextOHS
   */
  public EnumIntStringSnmpTable(Class<Index> indexType, OID oidBase, ChainedOidSingleHandler nextOHS) {
    leavesSize = indexType.getEnumConstants().length;
    leaves = new Leaf[leavesSize];
    this.oidBase = oidBase;
    oidBaseLength = oidBase.length();
    getSetLength = oidBase.length() + 2;
    nextOID = new NextOID( 2, leavesSize );
    this.nextOHS = nextOHS;
    indexPos = oidBaseLength+1;
  }

  /**
   * @param index
   * @param message
   * @param pc
   */
  public void addLeaf(Index index, String message, IntSource intSource) {
    leaves[index.ordinal()] = new IntSourceLeaf(message,intSource);
  }
  
  /**
   * @param index
   * @param stringValue
   * @param intValue
   */
  public void addLeaf(Index index, String stringValue, Integer intValue ) {
    leaves[index.ordinal()] = new StringIntLeaf(stringValue,intValue);
  }
 
  /**
   * @param index
   * @param leaf
   */
  public void addLeaf(Index index, Leaf leaf) {
    leaves[index.ordinal()] = leaf;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler#startOID()
   */
  public OID startOID() {
    return oidBase;
  }
  
  /**
   * @param oid
   * @return
   */
  private Leaf getLeaf(OID oid) {
    int index = Integer.parseInt(oid.getIndex(indexPos))-1;
    if( index >= leavesSize ) {
      return null;
    }
    return leaves[index];
  }
  
  /**
   * @param oid
   * @return
   */
  private WriteableLeaf getWriteableLeaf(OID oid) {
    Leaf leaf = getLeaf(oid);
    if( leaf instanceof WriteableLeaf ) {
      return (WriteableLeaf)leaf;
    }
    return null;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    String type = oid.getIndex(oidBaseLength);
    Leaf leaf = getLeaf(oid);
    if( leaf == null ) {
      throw new SnmpRequestHandlerException( RequestHandler.NO_SUCH_NAME, i );
    }
    if( type.equals("1") ) {
      return new IntegerVarBind( oid.getOid(), leaf.asInt() );
    } else if( type.equals("2") ) {
      return new StringVarBind( oid.getOid(), leaf.asString() );
    } else {
      throw new SnmpRequestHandlerException( RequestHandler.NO_SUCH_NAME, i );
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    try {
      OID next = getNextOID(oid);
      return get( oidBase.append(next), i );
    }
    catch ( NextOID.NoNextOIDException e ) {
      return nextOHS.getNext( nextOHS.startOID(), varBind, i);
    }
  }

  /**
   * @param oid
   * @return
   * @throws NoNextOIDException 
   */
  protected OID getNextOID(OID oid) throws NoNextOIDException {
    return nextOID.getNext( oid.subOid(oidBaseLength) );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#matches(com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler.SnmpCommand, com.gip.xyna.utils.snmp.OID)
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! oid.startsWith(oidBase) ) {
      return false;
    }
    switch( snmpCommand ) {
    case GET: 
      return oid.length()==getSetLength;
    case GET_NEXT: 
      return true;
    case SET:
      if( oid.length()==getSetLength ) {
        WriteableLeaf leaf = getWriteableLeaf(oid);
        return leaf != null && leaf.canSet();
      } else {
        return false;
      }
    default: return false;
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#set(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public void set(OID oid, VarBind varBind, int i) {
    String type = oid.getIndex(oidBaseLength);
    WriteableLeaf leaf = getWriteableLeaf(oid);
    if( leaf == null ) {
      throw new SnmpRequestHandlerException( RequestHandler.NO_SUCH_NAME, i );
    }
    if( type.equals("1") ) { //Integer wird benoetigt
      if( varBind instanceof IntegerVarBind ) {
        Integer value = Integer.valueOf( ((IntegerVarBind)varBind).intValue() );
        if( leaf.fromInt(value) ) {
          return;
        } else {
          throw new SnmpRequestHandlerException( RequestHandler.INVALID_VALUE, i );
        }
      }
    } else { //String wird benoetigt
      if( varBind instanceof StringVarBind ) {
        String value = ((StringVarBind)varBind).stringValue();
        if( leaf.fromString(value) ) {
          return;
        } else {
          throw new SnmpRequestHandlerException( RequestHandler.INVALID_VALUE, i );
        }
      }
    }
    //erwarteter String oder Integer wurde nicht gefunden
    throw new SnmpRequestHandlerException( RequestHandler.BAD_VALUE, i ); 
  }


}
