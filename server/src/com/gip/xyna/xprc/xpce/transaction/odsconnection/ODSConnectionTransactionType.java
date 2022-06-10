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
package com.gip.xyna.xprc.xpce.transaction.odsconnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_FailedToOpenTransaction;
import com.gip.xyna.xprc.xpce.transaction.TransactionType;
import com.gip.xyna.xprc.xpce.transaction.parameter.OperationPrevention;


public class ODSConnectionTransactionType implements TransactionType<ODSConnectionTransaction> {

  public static final String TRANSACTION_TYPE_NAME = "ODSConnectionTransactionType";
  
  //namen der keys für die parameter
  public final static String KEY_CONNECTION_TYPE = "connectionType";
  public final static String KEY_FQ_STORABLE_NAMES = "storables";
  public final static String KEY_FQ_XMOM_STORABLE_NAMES = "xmomStorables";
  public final static String KEY_ROOT_REVISION = "rootRevision";
  
  @SuppressWarnings("unchecked")
  public ODSConnectionTransaction openTransaction(OperationPrevention operationPrevention, Map<String, String> params)
                  throws XPRC_FailedToOpenTransaction {
    String conType = params.get(KEY_CONNECTION_TYPE);
    ODSConnectionType odsConType;
    if (conType != null &&
        conType.length() > 0) {
      odsConType = ODSConnectionType.getByString(conType); 
    } else {
      odsConType = ODSConnectionType.DEFAULT;
    }
    
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ODSConnection con = ods.openConnection(odsConType);
    
    List<Class<? extends Storable<?>>> storableClasses = new ArrayList<>();
    
    String serializedStorables = params.get(KEY_FQ_STORABLE_NAMES);
    if (serializedStorables != null &&
        serializedStorables.length() > 0) {
      StringSerializableList<String> storables = StringSerializableList.separator(String.class);
      storables.deserializeFromString(params.get(KEY_FQ_STORABLE_NAMES));
      
      for (String storableName : storables) {
        try {
          Class<?> clazz = Class.forName(storableName);
          if (Storable.class.isAssignableFrom(clazz)) {
            ((List)storableClasses).add(clazz);        
          }
        } catch (ClassNotFoundException e) {
          throw new XPRC_FailedToOpenTransaction(TRANSACTION_TYPE_NAME, e);
        }
      }
    }
    
    String serializedXmomStorables = params.get(KEY_FQ_XMOM_STORABLE_NAMES);
    if (serializedXmomStorables != null &&
        serializedXmomStorables.length() > 0) {
      StringSerializableList<String> xmomStorables = StringSerializableList.separator(String.class); 
      xmomStorables.deserializeFromString(params.get(KEY_FQ_XMOM_STORABLE_NAMES));
      
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      Long rootRevision = Long.parseLong(params.get(KEY_ROOT_REVISION));
      for (String xmomStorableName : xmomStorables) {
        long rev = rcdm.getRevisionDefiningXMOMObjectOrParent(xmomStorableName, rootRevision);
        XMOMStorableStructureInformation info =
                        XMOMStorableStructureCache.getInstance(rev).getStructuralInformation(xmomStorableName);
        if (info == null) {
          throw new XPRC_FailedToOpenTransaction(TRANSACTION_TYPE_NAME, new RuntimeException("Storable " + xmomStorableName + " can not be found in rev" + rev));
        }
        storableClasses.add(info.getStorableClass());
      }
    }
    
    try {
      con.ensurePersistenceLayerConnectivity(storableClasses);
    } catch (PersistenceLayerException e) {
      throw new XPRC_FailedToOpenTransaction(TRANSACTION_TYPE_NAME, e);
    }
    
    return new ODSConnectionTransaction(con);
  }
  
  

}
