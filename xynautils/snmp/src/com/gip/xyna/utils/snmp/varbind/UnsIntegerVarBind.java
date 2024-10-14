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
package com.gip.xyna.utils.snmp.varbind;

/**
 * Variable binding with type unsigned integer. (32bit)
 *
 */
public final class UnsIntegerVarBind extends VarBind {

  private final long value;

  /**
   * Creates a new VarBind of type integer.
   * @param objectIdentifier object identifier to use.
   * @param value value to use.
   * @throws IllegalArgumentException if value is null.
   */
  public UnsIntegerVarBind(final String objectIdentifier, final Long value) {
    super(objectIdentifier);
    if (value == null) {
      throw new IllegalArgumentException("Value may not be null.");
    }
    this.value = checkValue( value.longValue() );
  }

  private long checkValue( long v ) {
    if( v < 0 || v >= 0x100000000L ) {
      throw new IllegalArgumentException( "not a 32 bit unsigned integer");
    }
    return v;
  }

  /**
   * Creates a new VarBind of type integer.
   * @param objectIdentifier object identifier to use.
   * @param value value to use.
   */
  public UnsIntegerVarBind(final String objectIdentifier, final long value) {
    super(objectIdentifier);
    this.value = checkValue( value );
  }

  @Override
  public Object getValue() {
    return Long.valueOf( this.value );
  }

  public long longValue() {
    return value;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.varbind.VarBind#convert(com.gip.xyna.utils.snmp.varbind.VarBindTypeConverter)
   */
  @Override
  public <V> V convert( VarBindTypeConverter<V>  converter ) {
    return converter.convert( this );
  }

}
