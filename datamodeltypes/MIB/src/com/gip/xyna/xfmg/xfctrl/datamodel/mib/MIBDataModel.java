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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.DataModel;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class MIBDataModel implements XmlAppendable {
 
  private String oid;
  private String rootOid;
  private String snmpType;
  
  public MIBDataModel() {
  }


  public void appendXML(XmlBuilder xml) {
    xml.optionalElement(EL.MODELOID, oid);
    xml.optionalElement(EL.MODELROOTOID, rootOid);
    xml.optionalElement(EL.MODELTYPE, snmpType );
 }
  
  public static DataModel modelName(String rootOid, String modelName) {
    return DataModel.modelName(modelName, MIBDataModel.mib_rootOid(rootOid));
  }
  
  public static DataModel baseModel(String rootOid, XmomType baseModel) {
    return DataModel.baseModel(baseModel, MIBDataModel.mib_rootOid(rootOid));
  }

  public static DataModel oid_type(String oid, String snmpType) {
    return DataModel.modelName( null, MIBDataModel.mib_oidType(oid,snmpType) );
  }

  public static DataModel rootOid_type(String rootOid, String snmpType) {
    return DataModel.modelName( null, MIBDataModel.mib_rootOidType(rootOid,snmpType) );
  }
  
  public static DataModel oid(String oid) {
    return DataModel.modelName( null, MIBDataModel.mib_oid(oid) );
  }

  public static DataModel rootOid(String rootOid) {
    return DataModel.modelName( null, MIBDataModel.mib_rootOid(rootOid) );
  }

  private static MIBDataModel mib_oid(String oid) {
    MIBDataModel mdm = new MIBDataModel();
    mdm.oid = oid;
    return mdm;
  }

  private static MIBDataModel mib_rootOid(String rootOid) {
    MIBDataModel mdm = new MIBDataModel();
    mdm.rootOid = rootOid;
    return mdm;
  }
  
  private static MIBDataModel mib_oidType(String oid, String snmpType) {
    MIBDataModel mdm = new MIBDataModel();
    mdm.oid = oid;
    mdm.snmpType = snmpType;
    return mdm;
  }

  private static MIBDataModel mib_rootOidType(String rootOid, String snmpType) {
    MIBDataModel mdm = new MIBDataModel();
    mdm.rootOid = rootOid;
    mdm.snmpType = snmpType;
    return mdm;
  }

  
  
}
