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
package com.gip.xyna.utils.snmp.varbind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Variable binding list.
 *
 */
public class VarBindList implements Iterable<VarBind> {

  private final List<VarBind> variableBindings = new ArrayList<VarBind>();

  private String receivedFromHost = null;
  private String snmpVersion = null;
  
  public void add(final VarBind variableBinding) {
    if (variableBinding == null) {
      throw new IllegalArgumentException("Variable binding may not be null.");
    }
     variableBindings.add(variableBinding);
  }

  /**
   * Gets variable binding at given index.
   * @param index index of variable binding to get.
   * @return variable binding at given index.
   */
  public VarBind get(final int index) {
    return variableBindings.get(index);
  }

  /**
   * Returns number of variable bindings in the sequence.
   * @return number of variable bindings in sequence.
   */
  public int size() {
    return variableBindings.size();
  }

  @Override
  public String toString() {
    return variableBindings.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((variableBindings == null) ? 0 : variableBindings.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    VarBindList other = (VarBindList) obj;
    if( !checkEquals( variableBindings, other.variableBindings ) ) {
      return false;
    }
    if( !checkEquals( receivedFromHost, other.receivedFromHost ) ) {
      return false;
    }
    if( !checkEquals( snmpVersion, other.snmpVersion ) ) {
      return false;
    }
    return true;
  }

  /**
   * @param o
   * @param other
   * @return
   */
  private boolean checkEquals(Object o, Object other) {
    if(o == null) {
      return other == null;
    } else {
      return o.equals( other );
    }
  }

  public Iterator<VarBind> iterator() {
    return variableBindings.iterator();
  }

  /**
   * @return the receivedFromHost
   */
  public String getReceivedFromHost() {
    return receivedFromHost;
  }

  /**
   * @param receivedFromHost the receivedFromHost to set
   */
  public void setReceivedFromHost(String receivedFromHost) {
    this.receivedFromHost = receivedFromHost;
  }

  /**
   * @return the snmpVersion
   */
  public String getSnmpVersion() {
    return snmpVersion;
  }

  /**
   * @param snmpVersion the snmpVersion to set
   */
  public void setSnmpVersion(String snmpVersion) {
    this.snmpVersion = snmpVersion;
  }

 
}
