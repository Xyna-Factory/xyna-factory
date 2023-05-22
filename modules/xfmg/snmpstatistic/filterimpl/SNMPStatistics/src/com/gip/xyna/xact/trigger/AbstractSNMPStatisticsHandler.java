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

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;



//TESTING WITH :
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.1.1 JVM used memory
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.1.2 JVM free memory
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.2.\"ka.test.TestWfMI\".1  success
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.2.\"ka.test.TestWfMI\".2  //error + timeout
//snmpgetnext -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.3.\"ThreadPool.Cleanup\".1  //completed
//snmpget -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2.3.\"ThreadPool.Cleanup\".2  //rejected
//ThreadPool.Planning
//ExecutionPrio1 ... ExecutionPrio10
//snmpwalk -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.2
//snmpwalk -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.3
//snmpwalk -v 2c -c public $HOST:1500 1.3.6.1.4.1.28747.1.11.4.4

public abstract class AbstractSNMPStatisticsHandler extends AbstractOidSingleHandler {
  
  private final AbstractSNMPStatisticsHandler nextHandlerInWalk;
  private static Logger logger = CentralFactoryLogging.getLogger(AbstractSNMPStatisticsHandler.class);
  
  public AbstractSNMPStatisticsHandler(AbstractSNMPStatisticsHandler nextHandler) {
    nextHandlerInWalk = nextHandler;
  }
  
  protected SortedSet<OID> oidWalk = new TreeSet<OID>(); // FIXME once the deprecated classes are gone, this should be private
  
  // TODO: nachschauen, was f�r andere orderTypes existieren, die nicht aufgerufen wurden um dort 0 zur�ckzugeben
  // TODO: die call/finished/error/timeout stats in die Statistics Klasse packen
  protected TreeMap<String, Integer> mapStringInteger = new TreeMap<String, Integer>();
  protected TreeMap<Integer, String> mapIntegerString = new TreeMap<Integer, String>();
  protected int previousMapCount = 0;
  
  protected abstract void updateMap(int i);
  
  public abstract VarBind get(OID oid, int i);

  protected abstract OID getBase();
  
  public VarBind getNext(OID oid, VarBind vb, int i) {
    updateMap(i);

    // oid => string
    OID post = oid; //.subOid(getBase().length());
    OID next = null;

    SortedSet<OID> currentOidWalk;
    synchronized (oidWalk) {
      currentOidWalk = oidWalk;
    }

    if (post.length() == 0) {
      if (currentOidWalk.size() > 0) {
        return get(currentOidWalk.first(), i);
      } else {
        return new StringVarBind(new OID("1.3").toString(), "");
      }
    }

    // get next stat
    SortedSet<OID> tailSet = currentOidWalk.tailSet(post);
    Iterator<OID> it = tailSet.iterator();

    if (currentOidWalk.contains(post)) {
      // 2 mal iterator.next aufrufen
      if (it.hasNext()) {
        it.next();
        if (it.hasNext()) {
          next = it.next();
        } else {
          next = null;
        }
      } else {
        next = null;
      }
    } else {
      // statistic does not exist => find the next bigger one
      if (it.hasNext()) {
        next = it.next();
      }
    }

    if (next == null) {
      if (nextHandlerInWalk != null) {
        return nextHandlerInWalk.getNext(nextHandlerInWalk.getBase(), vb, i);
      }
      // there is no next => end of mib => "no such name" due to "1.3"
      return new StringVarBind(new OID("1.3").toString(), "");
    }

    VarBind getResult = null;
    try {
      getResult = get(next, i);
    }
    catch (Exception e) {
      // Kann z.B. auftreten, wenn kurz vorher eine Kapazitaet geloescht wurde
      logger.debug("Get oid failed: " + next.getOid());
    }
    if (getResult == null) {
      // oid ueberspringen
      return getNext(next, vb, i);
    }
    return getResult;
  }


  protected void setOIDWalk(SortedSet<OID> newWalk) {
    synchronized (this.oidWalk) {
      this.oidWalk = newWalk;
    }
  }


  public boolean matches(SnmpCommand command, OID oid) {
    return ( command.equals(SnmpCommand.GET) || command.equals(SnmpCommand.GET_NEXT) ) && oid.startsWith(getBase());
  }
}
