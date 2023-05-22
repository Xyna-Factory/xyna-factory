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
 * Variable binding type converter.
 *
 */
public interface VarBindTypeConverter<V> {


  /**
   * Converts from VarBind of type null.
   * @param objectIdentifier Object identifier.
   * @return Object representing a null variable binding.
   */
  V convert( NullVarBind vb );

  /**
   * Converts from VarBind of type integer.
   * @param objectIdentifier Object identifier.
   * @param value integer value.
   * @return Object representing an integer variable binding.
   */
  V convert( IntegerVarBind vb );
  
  /**
   * Converts from VarBind of type unsigned integer.
   * @param objectIdentifier Object identifier.
   * @param value String value.
   * @return Object representing a unsigned integer variable binding.
   */
  V convert( UnsIntegerVarBind vb );

  /**
   * Converts from VarBind of type string.
   * @param objectIdentifier Object identifier.
   * @param value String value.
   * @return Object representing a string variable binding.
   */
  V convert( StringVarBind vb );

  /**
   * Converts from VarBind of type oid.
   * @param objectIdentifier Object identifier.
   * @param value String value.
   * @return Object representing a oid variable binding.
   */
  V convert(OIDVarBind vb);

  /**
   * Converts from Varbind of type byte[]
   * @param objectIdentifier
   * @param value
   * @return Object representing a ByteArray variable binding
   */
  V convert(ByteArrayVarBind byteArrayVarBind);

  V convert(IpAddressVarBind ipAddressVarBind);
  
  V convert(Counter64VarBind vb);
  
  V convert(TimeTicksVarBind vb);
  
  V convert(Gauge32VarBind vb);
  
  V convert(Counter32VarBind vb);
}
