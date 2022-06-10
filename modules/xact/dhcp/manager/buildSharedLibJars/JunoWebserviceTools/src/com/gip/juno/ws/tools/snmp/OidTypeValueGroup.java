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


package com.gip.juno.ws.tools.snmp;

import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;


public class OidTypeValueGroup {

  protected String oid = null;

  protected String type = null;

  protected String value = null;

  /**
   * expects input string in the format "<oid> <type> <value>"
   */
  public OidTypeValueGroup(String input) throws JunoSnmpToolsException {
    String[] parts = input.split("\\s");
    if (parts.length != 3) {
      throw new JunoSnmpToolsException("Input String as wrong format, expected <oid> <type> <value>");
    }
    oid = parts[0].trim();
    type = parts[1].trim();
    value = parts[2].trim();
  }


  public VarBind toVarBind() throws JunoSnmpToolsException {
    if (type.equals("i")) {
      try {
        int intVal = Integer.parseInt(value);
        return new IntegerVarBind(oid, intVal);
      }
      catch (NumberFormatException e) {
        throw new JunoSnmpToolsException("Supplied value for snmp set command is no integer: " + value);
      }
    }
    else if (type.equals("s")) {
      return new StringVarBind(oid, value);
    }
    throw new JunoSnmpToolsException("Value type for snmp set command is not supported: " + type);
  }


  public String toString() {
    return "OidTypeValueGroup { oid: " + oid + ", type: " + type + ", value: " + value + " }";
  }

}
