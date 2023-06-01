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

public class Gauge32VarBind extends VarBind {

  private final Long value;

  /**
   * Creates a new VarBind of type Gauge32.
   * @param objectIdentifier object identifier to use.
   * @param value value to use.
   * @throws IllegalArgumentException if value is null.
   */
  public Gauge32VarBind(final String objectIdentifier, final Long value) {
    super(objectIdentifier);
    if (value == null) {
      throw new IllegalArgumentException("Value may not be null.");
    }
    this.value = value;
  }

  /**
   * Creates a new VarBind of type Gauge32.
   * @param objectIdentifier object identifier to use.
   * @param value value to use.
   */
  public Gauge32VarBind(final String objectIdentifier, final long value) {
    super(objectIdentifier);
    this.value = Long.valueOf(value);
  }

  @Override
  public Object getValue() {
    return this.value;
  }

  public int longValue() {
    return value.intValue();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.varbind.VarBind#convert(com.gip.xyna.utils.snmp.varbind.VarBindTypeConverter)
   */
  @Override
  public <V> V convert( VarBindTypeConverter<V>  converter ) {
    return converter.convert(this);
  }
}
