/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;

public class UpdateSetInterlinkRMIProperties extends UpdateJustVersion {

  public UpdateSetInterlinkRMIProperties(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  @Override
  protected void update() throws XynaException {
    super.update();
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(XynaPropertyStorable.class);
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        try {
          XynaPropertyStorable propHost = new XynaPropertyStorable(XynaProperty.RMI_HOSTNAME_REGISTRY.getPropertyName());
          con.queryOneRow(propHost);
          XynaPropertyStorable propHostIL = new XynaPropertyStorable(XynaProperty.RMI_IL_HOSTNAME_REGISTRY.getPropertyName());
          propHostIL.setPropertyValue(propHost.getPropertyValue());
          con.persistObject(propHostIL);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //dann nichts tun
        }

        try {
          XynaPropertyStorable propPort = new XynaPropertyStorable(XynaProperty.RMI_PORT_REGISTRY.getPropertyName());
          con.queryOneRow(propPort);
          XynaPropertyStorable propPortIL = new XynaPropertyStorable(XynaProperty.RMI_IL_PORT_REGISTRY.getPropertyName());
          propPortIL.setPropertyValue(propPort.getPropertyValue());
          con.persistObject(propPortIL);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //dann nichts tun
        }

        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      ods.unregisterStorable(XynaPropertyStorable.class);
    }
  }

}
