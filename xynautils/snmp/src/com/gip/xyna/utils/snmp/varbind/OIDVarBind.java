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
package com.gip.xyna.utils.snmp.varbind;

/**
 * Variable binding with type string.
 *
 */
public final class OIDVarBind extends VarBind {

  private final String value;

  /**
   * Creates a new string variable binding.
   * @param objectIdentifier object identifier to use.
   * @param value value to use.
   * @throws IllegalArgumentException of value is null.
   */
  public OIDVarBind(final String objectIdentifier, final String value) {
    super(objectIdentifier);
    if (value == null) {
      throw new IllegalArgumentException("Value may not be null.");
    }
    this.value = value;
  }

  @Override
  public Object getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.getObjectIdentifier());
    sb.append(" := \"");
    sb.append(this.value);
    sb.append("\"");
    return sb.toString();
  }

  public String stringValue() {
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
