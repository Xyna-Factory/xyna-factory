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

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.Level;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelType;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Path.PathCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class DataModelTypeImpl implements DataModelType {

  
  private static Logger logger = CentralFactoryLogging.getLogger(DataModelTypeImpl.class);
  
  private String dataModelTypeName;
  private PluginDescription pluginDescription;


  public PluginDescription init(String name) throws XynaException {
    this.dataModelTypeName = name;
    
    pluginDescription = PluginDescription.create(PluginType.dataModelType).
        name(this.dataModelTypeName).
        label("Data Model Type "+this.dataModelTypeName).
        description("Imports MIBs.").
        parameters(PluginDescription.ParameterUsage.Create, ImportParameter.importParameters).
        parameters(PluginDescription.ParameterUsage.Configure, ImportParameter.configurableParameters).
        build();
    
    createBaseTypeIfNotExists();
    
    return pluginDescription;
  }
    
  public PluginDescription configureDefaults(Map<String, Object> paramMap) {
    ImportParameter.initDefaults(paramMap);
    return pluginDescription;
  }

  public PluginDescription showDescription() {
    return pluginDescription;
  }
  
  private void createBaseTypeIfNotExists() throws XFMG_NoSuchRevision, Ex_FileWriteException {
    XmomType type = XmomType.ofFQTypeName(ImportParameter.BASE_TYPE.getDefaultValue());
    Datatype datatype = new Datatype(type);
    XmomGenerator xmomGenerator = XmomGenerator.inRevision(RevisionManagement.REVISION_DATAMODEL).build();
    if( ! xmomGenerator.exists(datatype) ) {
      xmomGenerator.add(datatype);
      xmomGenerator.save();
    }
  }

  public void importDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage,
                              Map<String, Object> importParamMap, List<String> files) { 
    //Importieren der DataModels
    ImportParameter parameter = ImportParameter.parameterForImport(importParamMap, dataModelTypeName);
    MIBImport mibImport = null;
    try {
      mibImport = parseModulesAndSelectDataModels(dataModelResult, parameter, files);
    } catch (MalformedURLException e) {
      dataModelResult.fail("Trying to read MIB-files:", e );
      return;
    }
    
    //Prüfen, ob DatenModelle angelegt werden dürfen
    try {
      List<DataModel> allExistingDataModels = dataModelStorage.listDataModels(dataModelTypeName);
      Pair<Boolean, List<String>> importAllowed = mibImport.checkImportAllowed( allExistingDataModels );
      if( importAllowed.getFirst() ) {
        //DatenModelle dürfen angelegt werden, evtl müssen bestehende gelöscht werden
      } else {
        dataModelResult.failAlreadyExistingNoOverwrite(importAllowed.getSecond());
        return;
      }
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to read existing data models:", e);
      return;
    }
    
    //Anlegen der DataModels und DataTypes
    mibImport.createModuleDataTypes();
    
    //Anlegen hat geklappt, nun evtl. alte DatenModelle entfernen (Overwrite)
    for( DataModel dm : mibImport.getAlreadyImportedDataModels() ) {
      removeDataModel( dataModelResult, dataModelStorage, dm, Collections.<String,Object>emptyMap() ); //TODO Trotz Verwendung erlauben...
    }
    
    //Speichern der Datentypen und DatenModelle
    try {
      mibImport.saveDataModelsAndDataTypes(dataModelResult, dataModelStorage);
    } catch (XFMG_NoSuchRevision e) {
      dataModelResult.fail("Unexpected:", e);
      return;
    } catch (Ex_FileWriteException e) {
      dataModelResult.fail("Trying to save new data type", e);
      return;
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to store new data model", e);
      return;
    }
    
  }
      

  public MIBImport parseModulesAndSelectDataModels(DataModelResult dataModelResult, ImportParameter parameter, List<String> files) throws MalformedURLException {
    
    //Lesen der MIBs
    MIBReader mibReader = new MIBReader();
    mibReader.importFiles(files);
   
    mibReader.parse();
    mibReader.fillModuleMap();
    
    dataModelResult.info( "Reading MIB...");
    addMessage(dataModelResult, Level.Error, mibReader.getErrMessage() );
    addMessage(dataModelResult, Level.Info, mibReader.getOutMessage() );
    
    if( parameter.isNoImport() ) {
      dataModelResult.addMessageGroup( "Found following modules", mibReader.getMIBInformations());
    }
 
    //Suche der zu importierenden Module, Erzeugen der Datentypen
    MIBImport mibImport;
    mibImport = new MIBImport(dataModelResult, mibReader, parameter);
    mibImport.selectModules();
    
    dataModelResult.addMessageGroup( "Selected data models", mibImport.getSelectedModules() );
    return mibImport;
  }

  private void addMessage(DataModelResult dmr, Level level, String message) {
    if( message != null && message.length() != 0 ) {
      if( message.endsWith("\n") ) {
        dmr.addMessage( level, message.substring(0,message.length()-1) );
      } else {
        dmr.addMessage( level, message );
      }
    }
  }
  
  public void modifyDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              DataModel dataModel, Map<String, Object> paramMap) {
    //derzeit nichts zu tun
  }

  public void removeDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              DataModel dataModel, Map<String, Object> paramMap) {
    try {
      XMOMDatabase xdb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      String usedBy = xdb.getDataModelUsedBy(dataModel.getType().getFqName());
      if (usedBy != null && usedBy.length()>0) {
        dataModelResult.fail("Data model used by: " + usedBy);
        return;
      }
    } catch( PersistenceLayerException e ) {
      dataModelResult.fail("Could not get usage information ", e);
    }
    
    String path = dataModel.getType().getPath();
    
    File location = new File(GenerationBase.getFileLocationOfXmlNameForSaving(path, RevisionManagement.REVISION_DATAMODEL));
    
    logger.info(" Deleting directory "+location );
    FileUtils.deleteDirectoryRecursively(location);
    
    try {
      dataModelStorage.removeDataModel(dataModel);
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to delete data model", e);
      return;
    }
  }
  
  public PathCreationVisitor getPathCreationVisitor(DOM dom){
    return new OIDPathCreationVisitor(dom);
  }
  
  public void shutdown() {
    // TODO Auto-generated method stub
    
  }

  public String writeDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion, 
                               Map<String, Object> paramMap, XynaObject data, long revision) {
    throw new UnsupportedOperationException(dataModelName + " cannot write XynaObject to String");
  }

  public XynaObject parseDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                                   Map<String, Object> paramMap, String data, long revision) {
    throw new UnsupportedOperationException(dataModelName + " cannot parse String to XynaObject");
  }

}
