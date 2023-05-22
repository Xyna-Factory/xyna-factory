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
 * Variable binding with type null.
 *
 */
public final class NullVarBind extends VarBind {

  private int syntax;
  
  /**
   * Creates a new VarBind with type null.
   * @param objectIdentifier object identifier to use.
   */
  public NullVarBind(final String objectIdentifier) {
    super(objectIdentifier);
  }

  public NullVarBind(String oid, int syntax) {
    super(oid);
    this.syntax = syntax; //wichtig wegen fehlersyntax. vgl org.snmp4j.smi.SMIConstants
  }

  @Override
  public Object getValue() {
    return null;
  }
  
  public int getSyntax() {
    return syntax;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.varbind.VarBind#convert(com.gip.xyna.utils.snmp.varbind.VarBindTypeConverter)
   */
  @Override
  public <V> V convert( VarBindTypeConverter<V>  converter ) {
    return converter.convert( this );
  }

}
