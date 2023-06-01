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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_ParseStringToDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_WriteDataModelToStringException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.events.AppMgmtEventHandler;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.ImportParameter.Information;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.GeneralParseXMLException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.TypeGeneration;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.WSDLParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.XSDParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.CreateXmlOptions;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.validation.ValidationUtils;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.Level;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelType;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Path.PathCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;


/**
 *
 */
public class DataModelTypeImpl implements DataModelType {

  private static Logger logger = CentralFactoryLogging.getLogger(DataModelTypeImpl.class);
  private String dataModelTypeName; 
  private PluginDescription pluginDescription;
  private WorkspaceXMLSupport workspaceXMLSupport;
  private XSDApplicationEventHandler eventHandler;
  private static final XynaPropertyBoolean storeTypeMappingsProperty =
      new XynaPropertyBoolean("xfmg.xfctrl.datamodel.xsd.store_in_typemapping", false)
          .setDefaultDocumentation(DocumentationLanguage.EN, "Imported types can be optionally stored in typemapping table for compatibility with XynaTypeGeneration Service.");


  public PluginDescription init(final String name) throws XynaException {
    this.dataModelTypeName = name;
    
    pluginDescription = PluginDescription.create(PluginType.dataModelType).
        name(this.dataModelTypeName).
        label("Data Model Type "+this.dataModelTypeName).
        description("Imports XSDs.").
        parameters(PluginDescription.ParameterUsage.Create, ImportParameter.importParameters).
        parameters(PluginDescription.ParameterUsage.Configure, ImportParameter.configurableParameters).
        parameters(PluginDescription.ParameterUsage.Modify, ImportParameter.modifyParameters).
        parameters(PluginDescription.ParameterUsage.Delete, ImportParameter.removeParameters).
        parameters(PluginDescription.ParameterUsage.Write, CreateXmlOptions.parameters).
        build();
    
    WorkspaceHelper.createBaseTypeIfNotExists(RevisionManagement.REVISION_DATAMODEL);
    workspaceXMLSupport = new WorkspaceXMLSupport(dataModelTypeName);

    storeTypeMappingsProperty.registerDependency(UserType.Plugin, "DataModelType: " + name);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("DataModelType XSD", "DataModelType XSD.checkForUpdate").
      after(DataModelManagement.class, ApplicationManagementImpl.class, RuntimeContextDependencyManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable(){ public void run(){ 
        checkForUpdate(name);
        registerEventHandling();
      }});
    
    return pluginDescription;
  }
  
  public PluginDescription configureDefaults(Map<String, Object> paramMap) {
    ImportParameter.initDefaults(paramMap);
    return pluginDescription;
  }
  
  public PluginDescription showDescription() {
    return pluginDescription;
  }
  
  
  public void importDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              Map<String, Object> importParamMap, List<String> xsds)  {
    if (xsds == null || xsds.size() == 0) {
      throw new IllegalArgumentException("Expected at least one file to import.");
    }
    //Importieren des DataModels
    ImportParameter parameter = ImportParameter.parameterForImport(dataModelTypeName, importParamMap);
    DataModel dataModel = DataModelUtils.getDataModel(parameter, dataModelTypeName);
    
    TypeGeneration tg = parseXSDsAndGenerateTypeInfo(dataModelResult, parameter, xsds);
    if( tg == null ) {
      //Fehler bereits in DataModelResult eingtragen, hier nun Austieg
      return;
    }
    
    if( parameter.isNoImport() ) {
      //Kein Import, daher jetzt fertig
      return;
    }
    if (logger.isInfoEnabled()) {
      logger.info("XSD Datamodel Import: Name=" + parameter.getDataModelName() + ", basePath=" + parameter.getBasePath() + ", prefix="
          + parameter.getModelNamePrefix() + ", pathCustomization=" + parameter.getPathCustomization() + ", distributeToWorkspaces="
          + parameter.getDistributeToWorkspaces() + ", overwrite=" + parameter.isOverwrite());
    }
    
    if (!distributesToWorkspaces(dataModel)) {
      try {
        dataModel = adjustVersionOrAbortIfNecessary(dataModelResult, dataModel, dataModelStorage, parameter);
      } catch (PersistenceLayerException e) {
        dataModelResult.fail(e);
      }
    }
    
    if (!dataModelResult.isSucceeded()) {
      return;
    }
    
    //DatenTypen anlegen
    tg.generateDataTypes(dataModel);
    
    DataModel oldDataModel = null;
    try {
      String fqName = dataModelStorage.getFqName(dataModelTypeName, dataModel.getVersion(), parameter.getDataModelName());
      if( fqName != null ) {
        oldDataModel = dataModelStorage.readDataModel(dataModelTypeName, fqName );
      }
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to read existing data models:", e);
      return;
    }
  
    if (parameter.getDistributeToWorkspaces()) {
      //Prüfen, ob DatenModell angelegt werden darf
      if( oldDataModel != null ) {
        if( ! parameter.isOverwriteModel() ) {
          dataModelResult.fail("\nDataModel \""+parameter.getDataModelName()+"\" already exists and overwrite-flag is not set" );
          return;
        } else {
          //altes Datenmodel entfernen, aber erst, nachdem Konflikt mit anderen Datentypen geprüft wurden 
        }
      }

      //Prüfen, ob einzelne Datentypen (durch andere Data Model) bereits existieren
      List<Datatype> alreadyExisting = tg.listAlreadyExistingDatatypes();
      if( ! alreadyExisting.isEmpty() ) {
        List<String> alreadyExistingTypes = new ArrayList<String>(); 
        for( Datatype dt : alreadyExisting ) {
          alreadyExistingTypes.add(dt.getFQTypeName());
        }
        if( oldDataModel != null ) {
          //Die meisten bereits existierenden Datentypen dürften zum alten Model gehören.
          //Diese dürfen jedoch überschrieben werden und zählen daher nicht
          Set<String> oldDataTypes = DataModelUtils.listDatatypes(oldDataModel);
          alreadyExistingTypes.removeAll(oldDataTypes);
        }
        if( ! alreadyExistingTypes.isEmpty() ) {
          if( ! parameter.isOverwriteTypes() ) {
            dataModelResult.failAlreadyExistingNoOverwrite(alreadyExistingTypes);
            return;
          } else {
            dataModelResult.infoAlreadyExisting(alreadyExistingTypes);
          }
        }
      }
    }
    
    if( oldDataModel != null ) {
      //altes Datemmodell entfernen
      Map<String, Object> params = new HashMap<String, Object>();
      params.put( ImportParameter.REMOVE_USED.getName(), true);
      params.put( ImportParameter.REMOVE_COMPLETE.getName(), true);
      removeDataModel(dataModelResult, dataModelStorage, oldDataModel, params);
      if( !dataModelResult.isSucceeded() ) {
        return; //Abbruch
      }
    }
    
    if (parameter.getXSDStorage() != null && 
        parameter.getXSDStorage().size() > 0) {
      List<DataModelSpecific> newDms = new ArrayList<DataModelSpecific>(dataModel.getDataModelSpecifics());
      try {
        newDms = DataModelUtils.listXSDStorage(parameter.getXSDStorage());
        dataModelStorage.replaceDataModelSpecifics(dataModel, Collections.<DataModelSpecific>emptyList(), newDms); // emptyList - as it should have been empty previously
        newDms = DataModelUtils.listXSDFilenames(parameter.getXSDFilenames());
        dataModelStorage.replaceDataModelSpecifics(dataModel, Collections.<DataModelSpecific>emptyList(), newDms); // emptyList - as it should have been empty previously
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException();
      }
    }
    
    if( logger.isDebugEnabled() ) {
      logger.debug( "Save and deploy datatypes for target "+parameter.getDataModelName());
    }
    
    boolean success = tg.saveDataTypes_persistDataModel(dataModelResult, dataModelStorage, dataModel, parameter);
    if( success ) {
      //TODO noch nötig?
      if (storeTypeMappingsProperty.get()) {
        tg.storeTypeMappings(null, parameter.getDataModelName());
      }

      dataModelResult.addMessageGroup( "Imported "+tg.getDataTypeFqNames().size()+" datatypes",
                                       tg.getDataTypeFqNames());
    }

    if (parameter.getDistributeToWorkspaces()) {
      //in weitere Workspaces kopieren
      List<String> datatypes = tg.getDataTypeFqNames();
      WorkspaceHelper wh = new WorkspaceHelper(dataModelResult, dataModelStorage, dataModel, datatypes, parameter, workspaceXMLSupport );
      wh.addWorkspaces(parameter.getWorkspaces());
    }
  }
  
  
  private DataModel adjustVersionOrAbortIfNecessary(DataModelResult result, DataModel dataModel, DataModelStorage dataModelStorage, ImportParameter parameter) throws PersistenceLayerException {
    DataModel existingDataModel = dataModel;
    boolean retry = false;
    do {
      DataModel newModule = dataModelStorage.readDataModel(existingDataModel.getType().getFqName());
      if (newModule == null) {
        retry = false;
      } else if (parameter.isOverwriteModel()) {
        retry = false;
      } else {
        try {
          int version = Integer.parseInt(newModule.getVersion());
          version++;
          existingDataModel.setVersion(Integer.toString(version));
          existingDataModel.setType(DataModelUtils.getXMOMType(dataModel.getType().getName(), parameter.getModelNamePrefix(), Integer.toString(version)));
          retry = true;
        } catch (NumberFormatException e) {
          result.fail("DateModel already exists and automatic version increment is not possible");
          return existingDataModel;
        }
      }
    } while (retry);
    return existingDataModel;
  }
  
  /**
   * @param dataModelResult
   * @param parameter
   * @param xsds
   * @return
   */
  private TypeGeneration parseXSDsAndGenerateTypeInfo(DataModelResult dataModelResult, ImportParameter parameter,
                                                      List<String> xsds) {
    
    if( logger.isDebugEnabled() ) {
      logger.debug( "Parsing xsds and generating types for target "+parameter.getDataModelName());
    }
    TypeGeneration tg = new TypeGeneration(parameter);
    
    try {
      Set<String> readXsds = tg.parseXSDs(xsds);
      
      try {
        List<String> xsdFilenames = prepareFilenames(readXsds);
        List<String> xsdStorage = generateXSDStorage(xsdFilenames);
        parameter.setXSDStorage(xsdStorage);
        parameter.setXSDFilenames(xsdFilenames);
      } catch (Exception e) {
        dataModelResult.fail( "Failed to generate xsd storage "+ e.getMessage(), e );
      }
      
      
      if( parameter.getInformations().contains(Information.ListXSDs ) ) {
        List<String> xsdsSort = new ArrayList<String>(readXsds);
        Collections.sort(xsdsSort);
        dataModelResult.addMessageGroup("XSDs", xsdsSort);
      }
    } catch( XSDParsingException e ) {
      dataModelResult.fail( "Parsing XSDs failed" );
      dataModelResult.addMessageGroup(Level.Error, "Parsing Failures", e.getErrors() );
      return null;
    } catch (FileNotFoundException e) {
      dataModelResult.fail( "XSD not found: "+ e.getMessage(), e );
      return null;
    } catch( WSDLParsingException e ) {
      dataModelResult.fail( "WSDL "+e.getName()+" failed on "+ e.getFailure(), e);
      return null;
    }
    tg.generateTypes();
    
    if( tg.getAnyTypeUsage() != 0 ) {
      dataModelResult.info( "AnyType is used "+tg.getAnyTypeUsage()+" times." );
    }
    
    if( parameter.getInformations().contains(Information.ListNamespaces) ) {
      dataModelResult.addMessageGroup("Namespaces", tg.listNamespaceXmomPath() );
      dataModelResult.addMessageGroup("XMOM paths", tg.listXmomPaths() );
    }
    
    if( parameter.getInformations().contains(Information.ListDatatypes) ) {
      dataModelResult.addMessageGroup("Data Types", InformationUtils.xmomTypesToString(tg) );
    }
    
    if( parameter.getInformations().contains(Information.ListDatatypeTree) ) {
      dataModelResult.addMessageGroup("Data Type Tree", InformationUtils.xmomTypesToTree(tg) );
    }
    
    return tg;
  }


  private List<String> prepareFilenames(Collection<String> readXsds) {
    List<String> preparedNames = new ArrayList<String>();
    for (String readXsd : readXsds) {
      if (readXsd.startsWith("file:")) {
        preparedNames.add(readXsd.substring("file:".length()));
      }
    }
    return preparedNames;
  }

  private List<String> generateXSDStorage(List<String> readXsds) throws IOException, Ex_FileWriteException {
    List<String> encodedXSDs = new ArrayList<String>();
    for (String readXsd : readXsds) {
      String fileContent = FileUtils.readFileAsString(new File(readXsd));
      String base64Content = Base64.encode(fileContent.getBytes("UTF8"));
      encodedXSDs.add(base64Content);
    }
    return encodedXSDs;
  }

  public void modifyDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage,
                              DataModel dataModel, Map<String, Object> paramMap) {
    //Derzeit können nur Workspaces geändert werden
    ImportParameter parameter = ImportParameter.parameterForModify(dataModelTypeName, paramMap);
    
    if (distributesToWorkspaces(dataModel)) {
      List<String> datatypes = getDatatypes(dataModel);

      WorkspaceHelper wh = new WorkspaceHelper(dataModelResult, dataModelStorage, dataModel, datatypes, parameter, workspaceXMLSupport);
      wh.modifyWorkspaces(parameter.getWorkspaces(), parameter.getWorkspaceMode());
    } else {
      dataModelResult.fail("DataModel '" + dataModel.getType().getName() + "' does not distribute to workspaces and is therefore unmodifiable");
    }
  }



  public void removeDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              DataModel dataModel, Map<String, Object> paramMap ) {
    ImportParameter parameter = ImportParameter.parameterForRemove(dataModelTypeName, paramMap);

    if (distributesToWorkspaces(dataModel)) {
      //Derzeitige Workspaces
      Set<String> workspaces = DataModelUtils.getWorkspaces(dataModel);
      
      if( ! workspaces.isEmpty() ) {
        //evtl. hat der Benutzer nichts zum Löschen gewählt, dies melden
        if( ! parameter.isRemoveComplete() ) {
          dataModelResult.fail( "DataModel is used in workspaces");
          dataModelResult.addMessageGroup("Workspaces", new ArrayList<String>(workspaces));
          return;
        }
      }
      
      List<String> datatypes = getDatatypes(dataModel);
      WorkspaceHelper wh = new WorkspaceHelper(dataModelResult, dataModelStorage, dataModel, datatypes, parameter, null);
      boolean success = wh.removeWorkspaces(workspaces);
      if( ! success ) {
        return;
      }
      
      //DatenModell existiert in keinem Workspace mehr, daher DatenModell komplett löschen
      try {
        wh.removeFromWorkspace(RevisionManagement.REVISION_DATAMODEL);
      } catch (XynaException e) {
        dataModelResult.fail("Could not delete XMOMObjects from revision \""+RevisionManagement.REVISION_DATAMODEL+"\"", e );
      }
    } else {
      if (paramMap.containsKey(DataModelManagement.OVERWRITE_PARAMETER_NAME) &&
          (boolean)paramMap.get(DataModelManagement.OVERWRITE_PARAMETER_NAME)) {
        return;
      }
      if (!parameter.isRemoveComplete()) {
        Application app = new Application(dataModel.getType().getName(), dataModel.getVersion());
        Set<RuntimeDependencyContext> users = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRuntimeContexts(app);
        if (users.size() > 0) {
          dataModelResult.fail("DateModel application ist still referenced");
          List<String> readableUsers = CollectionUtils.transform(users, new Transformation<RuntimeDependencyContext, String>() {
            public String transform(RuntimeDependencyContext from) {
              return from.getGUIRepresentation();
            }
          });
          dataModelResult.addMessageGroup("ParentRuntimeContexts", readableUsers);
          return;
        }
      }
      TemporarySessionAuthentication tsa = 
         TemporarySessionAuthentication.tempAuthWithUniqueUser("RemoveDataModel", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
      try {
        tsa.initiate();
      } catch (Exception e) {
        // ntbd
      }
      try {
        RemoveApplicationParameters rap = new RemoveApplicationParameters();
        rap.setKeepForAudits(false);
        rap.setStopIfRunning(true);
        rap.setUser(tsa.getUsername());
        rap.setForce(true);
        rap.setExtraForce(true);
        rap.setRemoveIfUsed(true);
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().removeApplicationVersion(dataModel.getType().getName(), dataModel.getVersion(), rap, null);
      } catch (XFMG_CouldNotRemoveApplication e) {
        try {
          //checken, ob application überhaupt noch existiert
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(dataModel.getType().getName(), dataModel.getVersion(), null);
          logger.debug("Could not remove application", e);
          dataModelResult.fail(e);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
          //ok, bereits entfernt
        }
      } finally {
        try {
          tsa.destroy();
        } catch (PersistenceLayerException e) {
          // ntbd
        } catch (XFMG_PredefinedXynaObjectException e) {
          // ntbd
        }
      }
    }
    
    try {
      dataModelStorage.removeDataModel(dataModel);
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to delete data model", e);
      return;
    }
    dataModelResult.info("Data Model is removed completely");
  }

  
  private boolean distributesToWorkspaces(DataModel dm) {
    for (DataModelSpecific dms : dm.getDataModelSpecifics()) {
      if (dms.getKey().endsWith(ImportParameter.DISTRIBUTE_TO_WORKSPACES.getName())) {
        try {
          return ImportParameter.DISTRIBUTE_TO_WORKSPACES.parse(dms.getValue());
        } catch (StringParameterParsingException e) {
          return false;
        }
      }
    }
    return false;
  }


  private List<String> getDatatypes(DataModel dataModel) {
    List<String> datatypes = new ArrayList<String>();
    for( com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType xt : dataModel.getXmomTypes() ) {
      String datatype = xt.getPath()+"."+xt.getName();
      datatypes.add( datatype );
    }
    return datatypes;
  }

  public void shutdown() {
    final ApplicationManagementImpl appMgmt = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    eventHandler = new XSDApplicationEventHandler();
    appMgmt.unregisterEventHandler(eventHandler.getName(), eventHandler.getVersionName());
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelType#getPathCreationVisitor(com.gip.xyna.xprc.xfractwfe.generation.DOM)
   */
  public PathCreationVisitor getPathCreationVisitor(DOM dom) {
    // TODO Auto-generated method stub
    return null;
  }

  public String writeDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                               Map<String, Object> paramMap, XynaObject data, long revision) throws XFMG_WriteDataModelToStringException {
    return workspaceXMLSupport.writeDataModel(dataModelStorage,dataModelName,dataModelVersion,paramMap,data,revision);
  }

  public XynaObject parseDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                                   Map<String, Object> paramMap, String data, long revision) throws XFMG_ParseStringToDataModelException {
    return workspaceXMLSupport.parseDataModel(dataModelStorage,dataModelName,dataModelVersion,paramMap,data,revision);
  }
  
  
  public void validateAgainstDataModel(String toValidate, DataModel correspondingDataModel) throws XFMG_ParseStringToDataModelException {
    List<String> xsdFilenames = DataModelUtils.extractXSDFileNames(correspondingDataModel);
    List<String> xsdStorage = DataModelUtils.extractXSDStorage(correspondingDataModel);
    
    Map<String, String> filenameContentMap = new HashMap<String, String>();
    for (int i=0; i < xsdFilenames.size(); i++) {
      filenameContentMap.put(xsdFilenames.get(i), xsdStorage.get(i));
    }
    
    try {
      ValidationUtils.validate(toValidate, filenameContentMap);
    } catch (Exception e) {
      throw new GeneralParseXMLException(e.getMessage(), e);
    }
  }
  
  

  
  private void checkForUpdate(final String name) {
    final ApplicationManagementImpl appMgmt = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    boolean newestVersionPresent = false;
    final Set<Application> migrationSources = new HashSet<Application>();
    for (ApplicationInformation appInfo : appMgmt.listApplications(false, false)) {
      if (appInfo.getName().equals(TypeGeneration.XSD_DATAMODEL_BASE_APP_NAME)) {
        if (appInfo.getVersion().equals(TypeGeneration.XSD_DATAMODEL_BASE_APP_VERSION_FOUNDDM_NOT_NULL)) {
          newestVersionPresent = true;
        } else {
          migrationSources.add(new Application(TypeGeneration.XSD_DATAMODEL_BASE_APP_NAME, appInfo.getVersion()));
        }
      }
    }
    if (!newestVersionPresent) {
          TypeGeneration typeGen = new  TypeGeneration(ImportParameter.parameterForImport(name, Collections.<String, Object>emptyMap()));
          try {
            typeGen.createXsdBaseApp(appMgmt);
            if (migrationSources.size() > 0) {
              Application newestVersion = new Application(TypeGeneration.XSD_DATAMODEL_BASE_APP_NAME, TypeGeneration.XSD_DATAMODEL_BASE_APP_VERSION_FOUNDDM_NOT_NULL);
              for (Application migrationSource : migrationSources) {
                try {
                  MigrateRuntimeContext.migrateRuntimeContext(migrationSource, newestVersion, Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()), true);
                } catch (PersistenceLayerException e) {
                  logger.warn("Error during migration to new XSD-BaseApp", e);
                } catch (XFMG_CouldNotModifyRuntimeContextDependenciesException e) {
                  logger.warn("Error during migration to new XSD-BaseApp", e);
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                  logger.warn("Error during migration to new XSD-BaseApp", e);
                }
              }
            }
          } catch (Exception e) {
            logger.warn("Failed to check für XSD-Datamodel update.", e);
          }
        }
  }
  
  
  private void registerEventHandling() {
    final ApplicationManagementImpl appMgmt = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    eventHandler = new XSDApplicationEventHandler();
    appMgmt.registerEventHandler(eventHandler);
    
  }
  
  
  private class XSDApplicationEventHandler extends AppMgmtEventHandler {
    
    private final static String DATAMODEL_ARCHIVE_NAME = "datamodelarchive";

    public String getName() {
      return "XSD Datamodel";
    }
    
    protected String getVersionName() {
      return "1.0";
    }

    public void shutdown(com.gip.xyna.utils.misc.EventHandler.ShutdownReason reason) {
    }
    
    public void handleFileException(IOException e) {
      logger.warn("Failed to interact with datamodelarchive application entry.", e);
    }

    public List<AdditionalData> getAdditionalExports(String application, String version) {
      List<AdditionalData> data = new ArrayList<AdditionalData>();
      if (isDatamodelApp(application, version)) {
        DataModel dm = getCorrespondingDataModel(application, version);
        DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
        File file;
        try {
          DataModelResult dmr = new DataModelResult();
          file = File.createTempFile(application + "." + version, DATAMODEL_ARCHIVE_NAME);
          dmm.exportDataModels(dmr, file.getCanonicalPath(), Collections.singletonList(dm.getType().getFqName()));
          if (dmr.isSucceeded()) {
            data.add(new AdditionalData(file.getName(), new FileInputStream(file)));
          } else {
            logger.warn("Failed to generate datamodelarchive." + com.gip.xyna.xfmg.Constants.LINE_SEPARATOR + dmr.singleMessagesToString(com.gip.xyna.xfmg.Constants.LINE_SEPARATOR));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        
      }
      return data;
    }

    public void receiveAdditionalFiles(String application, String version, List<AdditionalFile> files) {
      for (AdditionalFile file : files) {
        if (file.getName().endsWith(DATAMODEL_ARCHIVE_NAME)) {
          DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
          DataModelResult dmr = new DataModelResult();
          try {
            dmm.importDataModelArchive(dmr, file.getFile().getCanonicalPath(), true);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    
    private boolean isDatamodelApp(String application, String version) {
      return getCorrespondingDataModel(application, version) != null;
    }
    
    private DataModel getCorrespondingDataModel(String application, String version) {
      DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
      List<DataModel> models;
      try {
        models = dmm.listDataModels(dataModelTypeName);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      for (DataModel dataModel : models) {
        if (DataModelUtils.isApplication(dataModel) &&
            application.equals(dataModel.getType().getName()) &&
            version.equals(dataModel.getVersion())) {
          return dataModel;
        }
      }
      return null;
    }
    
  }
  
}