/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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

package common;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler.SnmpCommand;

public class FailoverOidSingleHandler implements OidSingleHandler {
  
  final private static String FAILOVER_OID=".1.3.6.1.4.1.28747.1.13.5";
  final private static String STRING_VALUE[]={"both down","normal","failover"};
  final private static String[] data={"","1",STRING_VALUE[1],"1",STRING_VALUE[1],"1",STRING_VALUE[1],};

  static final Logger logger=Logger.getLogger(FailoverOidSingleHandler.class.getName() );  
  
  private FailoverOidSingleHandler(){/*private Constructor, internal use*/}
  private static FailoverOidSingleHandler uniqueInstance=null;
  public synchronized static FailoverOidSingleHandler getInstance(){
    if(uniqueInstance==null){
      uniqueInstance=new FailoverOidSingleHandler();
    }
    return uniqueInstance;    
  }
  
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! (oid.startsWith(FAILOVER_OID))) {
      return false;
    }
    
    switch( snmpCommand ) {
    case GET: 
      if(";1;2;3;4;5;6;".indexOf(oid.getIndex(10))>=0){
        return true;
      }
      return false;
      
    case GET_NEXT: return false;
    case SET:
      if(";1;3;5;".indexOf(oid.getIndex(10))>=0){
        return true;
      } 
      return false;
    default: return false;
    }
  }

  public VarBind get(OID oid, int i) {
    int index=Integer.parseInt(oid.getIndex(10));
    if (index%2==0){
      return VarBind.newVarBind(oid.getOid(), data[index]);
    } else {
      return VarBind.newVarBind(oid.getOid(), Integer.parseInt(data[index]));
    }
  }

  public VarBind getNext(OID oid, VarBind varBind, int i) {
    throw new UnsupportedOperationException("");
  }

  public void set(OID oid, VarBind varBind, int i) {
    int index =Integer.parseInt(oid.getIndex(10));
    if(! (varBind.getValue() instanceof Integer)){
      throw new SnmpRequestHandlerException( RequestHandler.INVALID_VALUE, i );
    }
    int value=Integer.parseInt(""+varBind.getValue());
    if(value<0 || value > 2){
      throw new SnmpRequestHandlerException( RequestHandler.BAD_VALUE, i );
    }
    data[index]=""+value;
    data[index+1]=STRING_VALUE[value];
  }

  /**
   * @param oid
   * @return
   */
  public boolean isFailover(OID oid) {
    int index=Integer.parseInt(oid.getIndex(10));
    return "2".equals( data[index] );
  }
  
  @Override
  public void inform(OID arg0, int arg1) {
    //ignore inform
    logger.debug("ingnore inform oid="+arg0.getOid());
  }



  @Override
  public void trap(OID arg0, int arg1) {
    //ignore trap
    logger.debug("ingnore trap oid="+arg0.getOid());
  }

}
