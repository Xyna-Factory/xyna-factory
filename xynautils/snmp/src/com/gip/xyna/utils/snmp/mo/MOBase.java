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
package com.gip.xyna.utils.snmp.mo;


import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.snmp.exception.ParamNotSetException;
import com.gip.xyna.utils.snmp.exception.ParameterReadException;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * Tight binding between a Data-Object and a MO-Object, 
 * which knows the OID and 
 * how to check the correct value of the Data-Object and
 * how to transform the data for sending to or receiving from the snmp-agent. 
 * 
 * Base-Class for the MO{Object}-classes (MOInteger, MOString)
 * 
 */
public abstract class MOBase {

  static Logger logger = Logger.getLogger(MOBase.class.getName());

  protected MO mo;
  protected String index;
  private boolean valueSet = true; //default is "false" even though no data is set.

  public MOBase(MO mo) {
    this.mo = mo;
  }

  /**
   * @return the complete OID including an optional index
   */
  public String getOid() {
    return mo.getOid()+index;
  }

  /**
   * @return a new VarBind-Object for a Get-Request
   */
  public VarBind varBindGet() {
    return VarBind.newVarBind( getOid(), null );
  }

  /**
   * @return a new VarBind-Object for a Set-Request (doing all necessary data-transformations specified in the mo) 
   */
  public VarBind varBindSet() {
    if( mo.isTypeInt() ) {
      return new IntegerVarBind( getOid(), getIntValue() );
    }
    if( mo.isTypeString() ) {
      return new StringVarBind( getOid(), getStringValue() ); 
    }
    throw new IllegalStateException("Unknown MO-Type");
  }


  /**
   * Reads and stores the data from the given VarBind (doing all necessary data-transformations specified in the mo)
   * @param varBind
   */
  public void read(final VarBind varBind) {
    if( !varBind.getObjectIdentifier().equals(getOid()) ) {
      logger.error(varBind.getObjectIdentifier() + " " + getOid());
      throw new IllegalStateException( "Unexpected OID in answer, expected "+getOid()+", got "+varBind.getObjectIdentifier() );
    }
    if( varBind instanceof IntegerVarBind ) {
      setIntValue(((IntegerVarBind) varBind).intValue());
      return;
    }
    if( varBind instanceof StringVarBind ) {
      setStringValue( ((StringVarBind)varBind).stringValue() );
      return;
    }
    throw new IllegalStateException("Unknown VarBind-Type");
  }


  /**
   * Reads the optional data from the given parameters. 
   * @param data
   * @param key
   * @throws ParameterReadException when the MO-check of the data fails
   * @throws UnsupportedOperationException when no String can be set
   */
  public void readOptional(final Map<String,Object> data, final String key) {
    try {
      read(data,key);
    } catch( ParamNotSetException e ) {
      valueSet = false; //no data is set
    }
  }

  /**
   * Marks that no data is set
   */
  public void unsetValue() {
    valueSet = false; //no data is set
  }

  /**
   * Reads the data from the given parameters. 
   * @param data
   * @param key
   * @throws ParamNotSetException when no data is found for the given key
   * @throws ParameterReadException when the MO-check of the data fails
   * @throws UnsupportedOperationException when no String can be set or mapped
   */
  public abstract void read(final Map<String,Object> data, final String key);

  /**
   * Sets the index (suffix for the OID)
   * @param index
   * @return
   */
  public MOBase index(@SuppressWarnings("hiding") final String index) {
    this.index = index;
    return this;
  }

  //internally use only
  protected abstract int getIntValue();
  protected abstract void setIntValue(int value);

  protected abstract String getStringValue();
  protected abstract void setStringValue(String value);

  /**
   * Returns false for optional data, which should not be sent to the snmp agent
   * @return 
   */
  public boolean hasValueSet() {
    return valueSet;
  }

  /**
   * Sets the data directly
   * @param value
   * @return
   * @throws ParameterReadException when the MO-check of the data fails
   * @throws UnsupportedOperationException when no int can be set or mapped
   */
  public MOBase setValue(final int value) {
    setIntValue(value);
    return this;
  }

  /**
   * Sets the data directly
   * @param value
   * @return
   * @throws ParameterReadException when the MO-check of the data fails
   * @throws UnsupportedOperationException when no String can be set or mapped
   */
  public MOBase setValue(final String value) {
    setStringValue(value);
    return this;
  }
}
