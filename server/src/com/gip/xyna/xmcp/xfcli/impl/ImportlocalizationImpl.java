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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Localization;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importlocalization;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;



public class ImportlocalizationImpl extends XynaCommandImplementation<Importlocalization> {

  

  public void execute(OutputStream statusOutputStream, Importlocalization payload) throws XynaException {
    
    List<Localization> coll = XMLPersistenceLayer.parseFileToStorables(new File(payload.getFilename()), Localization.class);
   
    ODSImpl ods = ODSImpl.getInstance();
    ODSConnection conHist = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      writeLineToCommandLine(statusOutputStream, "Copying " + coll.size() + " entries ...");
      
      Collection<Localization> collHist = conHist.loadCollection(Localization.class);
      
      List<Localization> store = checkForUpdate( coll, collHist, payload.getOverwriteExistingData());
      writeLineToCommandLine(statusOutputStream, "Found " + collHist.size() + " existing entries, copying "+ store.size() + " entries ...");
      conHist.persistCollection(store);
      
      conHist.commit();
    } finally {
      conHist.closeConnection();
    }
    writeLineToCommandLine(statusOutputStream, "Import finished.");
  }
  
  private List<Localization> checkForUpdate(List<Localization> coll, Collection<Localization> collHist, boolean overwriteExistingData) throws XynaException {
    
    Map<String, Localization> oldData = new HashMap<String, Localization>();
    for (Localization l : collHist) {
      String key = l.getType() + "_" + l.getLanguage() + "_" + l.getIdentifier();
      oldData.put(key, l);
    }
    IDGenerator idgen = IDGenerator.getInstance();
    List<Localization> store = new ArrayList<Localization>();
    for (Localization l : coll) {
      String key = l.getType() + "_" + l.getLanguage() + "_" + l.getIdentifier();
      //pk alter daten muss übernommen werden
      Localization old = oldData.get(key);
      if (old == null) {
        //neuer eintrag
        l.setId(idgen.getUniqueId(Localization.TABLENAME));
        store.add(l);
      } else if (overwriteExistingData) {
        //update
        l.setId(old.getId());
        store.add(l);
      } //else altes datum so belassen wie es ist
    }
    return store;
  }

}
