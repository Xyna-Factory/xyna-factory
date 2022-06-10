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

package com.gip.xyna.xact.trigger;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;


/**
 *
 */
public class XynaPropertyHandler extends AbstractOidSingleHandler {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaPropertyHandler.class);

  public final static OID base = new OID("1.3.6.1.4.1.28747.1.11.4.1"); //TODO aus konstanten zusammenbauen - konstanten für departments etc anlegen.

  public VarBind get(OID oid, int i) {
    //oid => string
    OID post = oid.subOid(base.length());
    String propName = post.decodeToString();
    String propValue = XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty(propName);
    if (propValue == null) {
      //property existiert nicht
      throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }
    logger.debug("got value " + propValue + " for key " + propName);
    return new StringVarBind(oid.toString(), propValue);
  }

  private int lastModifiedCount = -1;
  
  /**
   * bei unterschiedlichlangen strings ist der kürzere string "vor" dem längeren string.
   * bei gleichlangen strings wird der string "normal" mit string.compareto() verglichen
   */
  private Comparator<String> comparator = new Comparator<String>() {

    public int compare(String o1, String o2) {
      //negativ, falls o1 < o2
      if (o1 == null) {
        if (o2 == null) {
          return 0;
        }
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      int l1 = o1.length();
      int l2 = o2.length();
      if (l1 != l2) {
        return l1-l2;
      }      
      return o1.compareTo(o2);
    }
    
  };
  private SortedMap<String, String> sortedProps;

  public VarBind getNext(OID oid, VarBind vb, int i) {
    //lexikalische reihenfolge erstes nach oid beantworten (achtung, erster oidpart ist laenge! danach sortieren)
    //map cachen, damit nicht andauernd neu sortiert werden muss
    PropertyMap<String, String> props = XynaFactory.getInstance().getXynaMultiChannelPortal().getPropertiesReadOnly();
    int modifiedCount = props.getModifiedCount();
    if (modifiedCount > lastModifiedCount) {
      sortedProps = new TreeMap<String, String>(comparator);
      sortedProps.putAll(props);
      lastModifiedCount = modifiedCount;
    }
    //oid => string
    OID post = oid.subOid(base.length());
    if (post.length() == 0) {
      return new StringVarBind(base.append(OID.encodeFromString(sortedProps.firstKey())).toString(), sortedProps.get(sortedProps.firstKey()));
    }
    String propName = post.decodeToString();
    //propName testen
    if (!sortedProps.containsKey(propName)) {
      //property existiert nicht
      throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }
    //getnextprop
    SortedMap<String, String> tailMap = sortedProps.tailMap(propName);
    Iterator<String> it = tailMap.keySet().iterator();
    int idx = 0;
    String currentKey = null; //2 mal iterator.next aufrufen
    while (idx <= 1 && it.hasNext()) {
      currentKey = it.next();
      idx ++;
    }
    if (idx <= 1) {
      //kein naechstes gefunden
      return new StringVarBind(new OID("1.3").toString(), ""); //end of mib, weil 1.3 wird hier mit no_such_name fehler beantwortet
    }
    OID next = base.append(OID.encodeFromString(currentKey));
    String propValue = tailMap.get(currentKey);
    return new StringVarBind(next.toString(), propValue);
  }


  public boolean matches(SnmpCommand command, OID oid) {      
    return oid.startsWith(base);
  }
  

}
