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
package com.gip.xyna.update;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class UpdateJarsOfSNMPTrigger extends UpdateJustVersion {

  public UpdateJarsOfSNMPTrigger(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  @Override
  public void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(TriggerStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
    
      List<TriggerStorable> triggers = (List<TriggerStorable>) con
                      .loadCollection(TriggerStorable.class);

      
      for(TriggerStorable trigger : triggers) {
        if(trigger.getJarFiles().contains("xynautils-snmp") || trigger.getJarFiles().contains("SNMP4J.jar")) {
          logger.debug("Found SNMP trigger ... update Jars and sharedLibs");
          String []jars = trigger.getJarFiles().split(":");
          String []sharedLibs = trigger.getSharedLibs().split(":");
          if(sharedLibs == null) {
            sharedLibs = new String[0];
          }
          StringBuilder jarString = new StringBuilder();
          for(String jar : jars) {
            if(!jar.contains("xynautils-snmp") && !jar.contains("SNMP4J.jar") && jar.length() > 0) {
              jarString.append(jar).append(":");
            }
          }
          trigger.setJarFiles(jarString.toString());
          StringBuilder sharedLibString = new StringBuilder();
          for(String sharedLib : sharedLibs) {
            if(sharedLib.length() > 0 && !sharedLib.contains("snmplibs")) {
              jarString.append(sharedLib).append(":");
            }
          }
          sharedLibString.append("snmplibs").append(":");
          trigger.setSharedLibs(sharedLibString.toString());
        }
      }

      con.persistCollection(triggers);
      
      con.commit();
      
    } catch(Exception e) {
      throw new RuntimeException("Can't convert Filter/Trigger.", e);
    } finally {
      try {
        con.closeConnection();
      } catch(PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
      ods.unregisterStorable(TriggerStorable.class);
    }    
  }

}
