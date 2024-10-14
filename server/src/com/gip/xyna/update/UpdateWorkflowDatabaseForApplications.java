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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationStorable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Connection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeployedDatatypesAndExceptionsStorable;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeployedProcessOriginalFQsStorable;


public class UpdateWorkflowDatabaseForApplications extends UpdateJustVersion{


  UpdateWorkflowDatabaseForApplications(Version oldVersion, Version newVersion, boolean mustRegenerate) {
    super(oldVersion, newVersion, mustRegenerate);
  }
    
  @Override
  protected void update() throws XynaException {
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();  //VersionManagement wird gebraucht
      
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(ApplicationStorable.class);
      ods.registerStorable(DeployedProcessOriginalFQsStorable.class);
      ods.registerStorable(DeployedDatatypesAndExceptionsStorable.class);
      
      List<DeployedProcessOriginalFQsStorable> persListWorkflows = new ArrayList<DeployedProcessOriginalFQsStorable>();
      List<DeployedDatatypesAndExceptionsStorable> persListDatatypesExceptions = new ArrayList<DeployedDatatypesAndExceptionsStorable>();

      Connection con = ods.openConnection(ODSConnectionType.HISTORY);
      try{
        //alle Applications suchen
        Collection<ApplicationStorable> applications = con.loadCollection(ApplicationStorable.class);
        for (ApplicationStorable application : applications) {
          Long revision = null;
          try {
            revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement().getRevision(application.getName(), application.getVersion());
          } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            //Revision nicht gefunden (WorkingSet oder kaputte Application) -> mit anderen weitermachen
            continue;
          }
          
          //alle XMLs aus dem Revision-Ordner holen und parsen, um den fqXmlName und type zu bestimmen
          Map<String, XMOMType> toDeploy = new HashMap<String, XMOMType>();
          List<File> files = FileUtils.getMDMFiles(application.getName(), application.getVersion());
          for (File f : files) {
            Document doc;
            try {
              doc = XMLUtils.parse(f);
            } catch (InvalidXMLException e) {
              logger.warn("invalid xml: " + f.getAbsolutePath(), e);
              continue;
            }

            String fqXmlName = GenerationBase.getFqXMLName(doc);
            if (GenerationBase.isReservedServerObjectByFqOriginalName(fqXmlName)) {
              continue;
            }

            XMOMType type = XMOMType.getXMOMTypeByRootTag(doc.getDocumentElement().getTagName());
            toDeploy.put(fqXmlName, type);
          }
          
          //Storables anlegen
          for(String fqName : toDeploy.keySet()) {
            switch (toDeploy.get(fqName)) {
              case WORKFLOW:
                persListWorkflows.add(new DeployedProcessOriginalFQsStorable(fqName, revision));
                break;
              case DATATYPE:
                persListDatatypesExceptions.add(DeployedDatatypesAndExceptionsStorable.datatype(fqName, revision));
                break;
              case EXCEPTION:
                persListDatatypesExceptions.add(DeployedDatatypesAndExceptionsStorable.exception(fqName, revision));
                break;
            }
          }
        }

        //persistieren
        con.persistCollection(persListWorkflows);
        con.persistCollection(persListDatatypesExceptions);
        con.commit();
      } finally {
        con.closeConnection();
        ods.unregisterStorable(ApplicationStorable.class);
        ods.unregisterStorable(DeployedProcessOriginalFQsStorable.class);
        ods.unregisterStorable(DeployedDatatypesAndExceptionsStorable.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }
}
