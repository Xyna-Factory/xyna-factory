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
package com.gip.xyna.xact.trigger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.xprc.XynaOrder;

/**
 * methoden sollten null zur�ckgeben, falls der requesthandler nicht zust�ndig ist.
 * interruptedexception sollte geworfen werden, wenn der requesthandler den
 * request erfolgreich behandelt hat, aber keine antwort verschickt werden soll
 */
public interface XynaOrderSnmpRequestHandler {

  public XynaOrder snmpGet(VarBindList vbl) throws XynaException, InterruptedException;

  public XynaOrder snmpGetNext(VarBindList vbl) throws XynaException, InterruptedException;

  public XynaOrder snmpInform(OID informOid, VarBindList vbl) throws XynaException, InterruptedException;

  public XynaOrder snmpSet(VarBindList vbl) throws XynaException, InterruptedException;

  public XynaOrder snmpTrap(OID trapOid, VarBindList vbl) throws XynaException, InterruptedException;

}
