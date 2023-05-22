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
package com.gip.xyna.utils.snmp.manager;

import java.util.Iterator;

import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;

/**
 * SnmpWalker acts like an interator and walks over the given Oid.
 *
 */
public class SnmpWalker implements Iterator<VarBind> {
  private boolean hasNext;
  private VarBindList vbl;
  private SnmpContext snmpContext;
  private String caller;
  private String oid;

  /**
   * constructor
   * @param snmpContext the snmpContext handles the getNext-requests
   * @param oid the oid to walk over
   * @param caller for logging purpose
   */
  public SnmpWalker(SnmpContext snmpContext, String oid, String caller ) {
    this.snmpContext = snmpContext;
    this.caller = caller;
    this.oid = oid;
    vbl = new VarBindList();
    vbl.add( new NullVarBind(oid) );
    hasNext = true;
    vbl = getNext();
  }


  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return hasNext;
  }



  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public VarBind next() {
    VarBind vbNext = vbl.get(0);
    vbl = getNext();
    return vbNext;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * does the handling of snmpContext.getNext
   * @return
   */
  private VarBindList getNext() {
    if( hasNext() ) {
      VarBindList vbsQ = new VarBindList();
      vbsQ.add( new NullVarBind( vbl.get(0).getObjectIdentifier() ) );
      vbl = snmpContext.getNext( vbl, caller );
      if( ! vbl.get(0).getObjectIdentifier().startsWith(oid+".")) {
        //FIXME es gibt noch andere kennzeichen fuer hasNext=false
        //      vgl org.snmp4j.util.TreeUtils.TreeRequest.onResponse
        hasNext = false;
      }
    }
    return vbl;
  }

}
