/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.StringParameter.Unparseable;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_DataModelTypeStillUsedException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidDataModelParameterException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelTypeException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.BasicApplicationName;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.DataModelTypeClassLoader;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.DataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ParseDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.WriteDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelTypeStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Path.PathCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class DataModelManagement extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "DataModelTypeManagement";
  private static final String XML_DATAMODEL_FILENAME = "datamodels.xml";
  public static final String OVERWRITE_PARAMETER_NAME = DEFAULT_NAME + "_overwrite";
  
  private static Logger logger = CentralFactoryLogging.getLogger(DataModelManagement.class);
  
  private ConcurrentHashMap<String,DataModelType> dataModelTypes;
  private DataModelStorage dataModelStorage;
  private boolean dataModelIsAlwaysDeployable = false;
  
  public DataModelManagement() throws XynaException {
    super();
    dataModelTypes = new ConcurrentHashMap<String,DataModelType>();
  }
  
  private DataModelManagement(String cause) throws XynaException {
    super(cause);
    dataModelIsAlwaysDeployable = true; //Damit kann GenerationBase immer isDataModelDeployable(...) aufrufen,
    //dies sollte nur bei bereits deployten Datentypen auftreten, daher ist dataModelIsAlwaysDeployable = true richtig
  }

  public static DataModelManagement getDataModelManagementPreInit() throws XynaException {
    return new DataModelManagement("preInit");
  }
  
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DataModelStorage.class,"DataModelTypeManagement.initDataModelStorage").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initDataModelStorage(); }});
    fExec.addTask(DataModelManagement.class,"DataModelTypeManagement.initDataModelTypes").
      after(DataModelStorage.class, WorkspaceManagement.class, XMOMDatabase.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() { initDataModelTypes(); }});
  }
  
  private void initDataModelStorage() {
    try {
      dataModelStorage = new DataModelStorage();
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize DataModelTypeManagement", e);
      throw new RuntimeException(e);
    }
  }
 
  private void initDataModelTypes() {
    try {
      for( DataModelTypeStorable dmts : dataModelStorage.getAllDataModelTypes() ) {
        try {
          DataModelType dmt = loadDataModelType( dmts.getName(), dmts.getFqclassname() );
          PluginDescription pd = dmt.init( dmts.getName() );
          
          List<StringParameter<?>> configurableParameters = pd.getParameters(ParameterUsage.Configure);
          Map<String, Object> paramMap = null;
          
          try {
            paramMap = StringParameter.parse(dmts.getParameter()).with(configurableParameters);
            dmt.configureDefaults(paramMap);
            if( logger.isDebugEnabled() ) {
              logger.debug( "Data model type "+dmts.getName() +" successfully loaded" );
            }
          } catch (StringParameterParsingException e) {
            paramMap = StringParameter.parse(dmts.getParameter()).unmatchedKey(Unmatched.Ignore).
                unparseableValue(Unparseable.Ignore).with(configurableParameters);
            dmt.configureDefaults(paramMap);
            logger.warn( "Data model type "+dmts.getName() +" loaded with incomplete configuration, first warning:", e );
          }
          dataModelTypes.put(dmts.getName(), dmt);
        } catch (Throwable t) { //XFMG_JarFolderNotFoundException, RuntimeException, NoClassDefFoundError, StringParameterParsingException
          Department.handleThrowable(t);
          String msg = "Data Model Type "+dmts.getName()+" could not be initialized";
          logger.warn(msg, t);
          XynaExtendedStatusManagement.addFurtherInformationAtStartup("Data Model Type", msg);
        }
      }
      
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize DataModelTypeManagement", e);
      throw new RuntimeException(e);
    }
  }
  

  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  protected void shutdown() throws XynaException {
    for( DataModelType dmt : dataModelTypes.values() ) {
      dmt.shutdown();
    }
  }
  
  
  public DataModelType registerDataModelType(String name, String fqClassName) throws XynaException {
    DataModelType dmt = loadDataModelType(name, fqClassName);
    
    dmt.init(name);
    
    //DataModelTypeStorable anlegen und persistieren
    dataModelStorage.persistDataModelType(name, fqClassName, Collections.<String>emptyList() );
    dataModelTypes.put(name, dmt);
    return dmt;
  }
  
  public void configureDataModelType(String name, List<String> params) throws XynaException, StringParameterParsingException {
    DataModelType dmt = dataModelTypes.get(name);
    List<StringParameter<?>> configurableParameters = dmt.showDescription().getParameters(ParameterUsage.Configure);
    
    //Parameter parsen und als Defaults setzen
    Map<String, Object> paramMap = StringParameter.parse(params).with(configurableParameters);
    dmt.configureDefaults(paramMap);
    
    //TODO besser in Liste oder String verwandeln
    List<String> paramList = StringParameter.toList(configurableParameters, null, true); 
    
    //DataModelTypeStorable anlegen und persistieren
    dataModelStorage.persistDataModelType(name, dmt.getClass().getName(), paramList);
  }

  
  /**
   * L�dt die Klasse "fqClassName" f�r einen DataModelType vom Typ "type"
   */
  private DataModelType loadDataModelType(String type, String fqClassName) throws XynaException {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    cld.removeDataModelTypeClassLoader(type);
    DataModelTypeClassLoader dmtcl = cld.getDataModelTypeClassLoaderLazyCreate(type);
    boolean success = false;
    try {

      Class<?> clazz = null;
      try {
        clazz = dmtcl.loadClass(fqClassName);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (!DataModelType.class.isAssignableFrom(clazz)) {
        throw new RuntimeException("DataModelType class must extend " + DataModelType.class.getName());
      }

      DataModelType dmt = null;
      try {
        dmt = (DataModelType) clazz.getConstructor().newInstance();
      } catch (Exception e) { //InstantiationException, IllegalAccessException
        throw new RuntimeException("DataModelType could not be instantiated", e);
      }
      success = true;
      return dmt;
    } finally {
      if (!success) {
        cld.removeDataModelTypeClassLoader(type);
      }
    }
  }


  /**
   * @param name
   */
  public void deregisterDataModelType(String name) throws PersistenceLayerException, XFMG_DataModelTypeStillUsedException {
    int dataModels = dataModelStorage.countDataModels(name);
    if( dataModels != 0 ) {
      throw new XFMG_DataModelTypeStillUsedException(name, dataModels);
    }
    
    DataModelType dmt = dataModelTypes.remove(name);
    if( dmt == null ) {
      //TODO Fehler?
      return;
    }
    
    dataModelStorage.deleteDataModelType(name);

    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    cld.removeDataModelTypeClassLoader(name);
  }

  public List<PluginDescription> listDataModelTypeDescriptions() {
    List<PluginDescription> pds = new ArrayList<PluginDescription>();
    for( DataModelType dmt : dataModelTypes.values() ) {
      pds.add( dmt.showDescription() );
    }
    Collections.sort(pds);
    return pds;
  }

  public PluginDescription getDataModelTypeDescription(String type) throws XFMG_NoSuchDataModelTypeException {
    DataModelType dmt = dataModelTypes.get(type);
    if( dmt == null ) {
      throw new XFMG_NoSuchDataModelTypeException(type);
    }
    return dmt.showDescription();
  }


  /**
   * @param parameters
   * @return
   */
  public boolean importDataModel(DataModelResult dataModelResult, ImportDataModelParameters parameters) throws XFMG_NoSuchDataModelTypeException {
    DataModelType dmt = getDataModelType(parameters);
    Map<String, Object> paramMap;
    try {
      paramMap = parseParamMap(dmt, ParameterUsage.Create, parameters);
    } catch( StringParameterParsingException e ) {
      dataModelResult.fail("Invalid parameter:", e);
      return false;
    }
    
    //Files zu den �bergebenen FileIds zug�nglich machen
    File tmpDir;
    try {
      tmpDir = copyFiles( parameters.getFileIds(), parameters.getDataModelType() );
    } catch( Exception e ) {
      dataModelResult.fail("Could not access file:", e);
      return false;
    }      
    
    try {
      //Import durchf�hren
      dmt.importDataModel(dataModelResult, dataModelStorage, paramMap, combineFiles(parameters.getFiles(),tmpDir) );
      return dataModelResult.isSucceeded();
    } finally {
      if(tmpDir != null ) {
        FileUtils.deleteDirectory(tmpDir);
      }
    }
  }

  private File copyFiles(List<String> fileIds, String dataModelType) throws Ex_FileAccessException {
    if( fileIds == null || fileIds.isEmpty() ) {
      return null; //nichts zu tun
    }
    File tmpDir = new File("/tmp/datamodelmgmt/"+dataModelType+"_"+System.currentTimeMillis());
    boolean succeeded = false;
    try {
      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      for (String fileId : fileIds ) {
        fm.copyToDir(tmpDir, fileId, true);
      }
      succeeded = true;
      return tmpDir;
    } finally {
      if( ! succeeded ) {
        FileUtils.deleteDirectory(tmpDir);
      }
    }
  }
  
  private List<String> combineFiles(List<String> files, File tmpDir) {
    if( tmpDir == null ) {
      return files;
    } else {
      if( files == null ) {
        return Arrays.asList(tmpDir.getAbsolutePath());
      } else {
        List<String> combined = new ArrayList<String>(files);
        combined.add(tmpDir.getAbsolutePath());
        return combined;
      }
    }
  }

  
  
  public boolean modifyDataModel(DataModelResult dataModelResult, ModifyDataModelParameters parameters) throws XFMG_NoSuchDataModelTypeException, XFMG_NoSuchDataModelException, PersistenceLayerException {
    DataModelType dmt = getDataModelType(parameters);
    DataModel dataModel = getDataModel(parameters);
    
    Map<String, Object> paramMap;
    try {
      paramMap = parseParamMap(dmt, ParameterUsage.Modify, parameters);
    } catch( StringParameterParsingException e ) {
      dataModelResult.fail("Invalid parameter:", e);
      return false;
    }
    
    dmt.modifyDataModel(dataModelResult, dataModelStorage, dataModel, paramMap);
    return dataModelResult.isSucceeded();
  }


  private Map<String, Object> parseParamMap(DataModelType dmt, ParameterUsage usage,
                                            DataModelParameters parameters) throws StringParameterParsingException {
    List<StringParameter<?>> sps = dmt.showDescription().getParameters(usage);
    if( sps == null || sps.isEmpty() ) {
      return Collections.emptyMap();
    }
    return StringParameter.
        parse(parameters.getParameters()).
        unmatchedKey(parameters.getUnmatchedKeys()).
        with(sps);
  }

  public boolean removeDataModel(DataModelResult dataModelResult, RemoveDataModelParameters parameters) throws XFMG_NoSuchDataModelTypeException, XFMG_NoSuchDataModelException, PersistenceLayerException {
    DataModelType dmt = getDataModelType(parameters);
    DataModel dataModel = getDataModel(parameters);
    
    Map<String, Object> paramMap;
    try {
      paramMap = parseParamMap(dmt, ParameterUsage.Delete, parameters);
    } catch( StringParameterParsingException e ) {
      dataModelResult.fail("Invalid parameter:", e);
      return false;
    }
    
    dmt.removeDataModel(dataModelResult, dataModelStorage, dataModel, paramMap);
    return dataModelResult.isSucceeded();
  }
  
  
  public boolean hasDataModel(DataModelParameters parameters) {
    String fqName = dataModelStorage.getFqName(parameters.getDataModelType(), parameters.getDataModelVersion(), parameters.getDataModelName());
    return fqName != null;
  }
 

  private DataModelType getDataModelType(DataModelParameters parameters) throws XFMG_NoSuchDataModelTypeException {
    DataModelType dmt = dataModelTypes.get(parameters.getDataModelType());
    if( dmt == null ) {
      throw new XFMG_NoSuchDataModelTypeException(parameters.getDataModelType());
    }
    return dmt;
  }
  
  private DataModel getDataModel(DataModelParameters parameters) throws PersistenceLayerException, XFMG_NoSuchDataModelException {
    String fqName = dataModelStorage.getFqName(parameters.getDataModelType(), parameters.getDataModelVersion(), parameters.getDataModelName());
    if (fqName == null) {
      if (parameters.getDataModelFqName() != null) {
        fqName = parameters.getDataModelFqName();
      } else {
        throw new XFMG_NoSuchDataModelException(parameters.getDataModelName());
      }
    }
    
    DataModel dataModel = dataModelStorage.readDataModel(parameters.getDataModelType(),fqName);
    if( dataModel == null ) {
      throw new XFMG_NoSuchDataModelException(parameters.getDataModelName());
    }
    return dataModel;
  }

  
  
  public List<DataModel> listDataModels() throws PersistenceLayerException {
    return dataModelStorage.listDataModels(null);
  }

  public List<DataModel> listDataModels(String dataModelType) throws PersistenceLayerException {
    return dataModelStorage.listDataModels(dataModelType);
  }

  public SearchResult<DataModelStorable> search(SearchRequestBean searchRequest) throws XNWH_SelectParserException, PersistenceLayerException, XNWH_InvalidSelectStatementException  {
    return dataModelStorage.search(searchRequest);
  }
  
  
  
  public List<File> getMDMFiles(String fqName) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    //TODO f�r MIB und TR069, nicht f�r XSD...
    String savedMdmDir = 
        RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DATAMODEL, false);
    
    List<File> files = dataModelStorage.readFiles(savedMdmDir, fqName);
    if( files == null || files.isEmpty() ) {
      //TODO F�r bestehende MIB-Datenmodelle auf alte Weise lesen
      DataModel dms = dataModelStorage.readDataModel(fqName);
      if( dms.getBaseType().getFqName().equals("xdnc.model.mib.MIBBaseModel") ) {
        files = new ArrayList<File>();
        XmomType type = XmomType.ofFQTypeName(fqName);
        String dir = savedMdmDir
            +Constants.fileSeparator
            +type.getPath().replaceAll("\\.", Constants.fileSeparator);
        files = FileUtils.getMDMFiles(new File(dir), files);
        files.add( new File(savedMdmDir+"/xdnc/model/mib/MIBBaseModel.xml") ); //FIXME
      }
    }
    return files;
  }

  public PathCreationVisitor getPathCreationVisitor(DOM dom) throws PersistenceLayerException {
    String dataModelName = dom.getOriginalFqName();
    String dataModelType = dataModelStorage.getDataModelType(dataModelName);
    DataModelType dmt = dataModelTypes.get(dataModelType);
    
    return dmt.getPathCreationVisitor(dom);
  }
  
  
  public void exportDataModels(DataModelResult dataModelResult, String fileName, List<String> fqNames) {
    List<DataModel> dataModelList = new ArrayList<DataModel>();
    XynaObjectList<DataModel> dataModels;
    try {
      Collection<DataModelStorable> dataModelStorables = dataModelStorage.getAllDataModelStorables();
      
      for (DataModelStorable dms : dataModelStorables) {
        if (fqNames == null || fqNames.contains(dms.getFqName())) {
          dataModelList.add(dataModelStorage.readDataModel(dms.getFqName()));
        }
      }
      
      dataModels = new XynaObjectList<DataModel>(dataModelList, DataModel.class);
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to find existing data models:", e);
      return;
    }
    
    Map<String, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType> datatypesForExport = 
      new HashMap<String, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType>();
    for (DataModel dataModel : dataModels) {
      com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType type = dataModel.getBaseType();
      datatypesForExport.put(type.getFqName(), type);
      for (com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType generatedType : dataModel.getXmomTypes()) {
        datatypesForExport.put(generatedType.getFqName(), generatedType);
      }
    }
    
    ZipOutputStream zos = null;
    try {
      zos = new ZipOutputStream(new FileOutputStream(new File(fileName)));
      byte[] data = new byte[2048];
      int length;
      
      //XMOM xmls exportieren
      for (com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType type : datatypesForExport.values()) {
        String dataModelPath = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DATAMODEL, false);
        File dataModelFolder = new File(dataModelPath);
        File file = new File(dataModelPath + Constants.fileSeparator + type.getFqName().replaceAll("\\.", Constants.fileSeparator) + ".xml");
        if (file.exists()) {
          String path = FileUtils.getRelativePath(dataModelFolder.getAbsolutePath(), file.getAbsolutePath());
          FileInputStream fis;
          try {
            fis = new FileInputStream(file);
          } catch (FileNotFoundException e) {
            throw new Ex_FileAccessException(file.getAbsolutePath(), e);
          }
          try {
            try {
              ZipEntry ze = new ZipEntry(path);
              zos.putNextEntry(ze);
              while ((length = fis.read(data)) != -1) {
                zos.write(data, 0, length);
              }
              zos.closeEntry();
            } finally {
              fis.close();
            }
          } catch (IOException e) {
            throw new Ex_FileWriteException(path, e);
          }
        }
      }

      //Metadaten zu den Datenmodellen
      ZipEntry ze = new ZipEntry(XML_DATAMODEL_FILENAME);
      try {
        zos.putNextEntry(ze);
        zos.write(dataModels.toXml().getBytes(Constants.DEFAULT_ENCODING));
      } finally {
        zos.closeEntry();
      }
      
      zos.flush();
      
      if (dataModelList.size() == 0) {
        dataModelResult.info("No data model exported.");
      } else {
        dataModelResult.info("Exported data models: ");
        for (DataModel dm : dataModelList) {
          dataModelResult.info(dm.getType().getFqName());
        }
      }
    } catch (IOException e) {
      dataModelResult.fail("Trying to write " + XML_DATAMODEL_FILENAME + ":", e);
    } catch (Ex_FileAccessException e) {
      dataModelResult.fail("Trying to write " + XML_DATAMODEL_FILENAME + ":", e);
    } finally {
      if (zos != null) {
        try {
          zos.close();
        } catch (IOException e) {
          dataModelResult.fail("Trying to write " + XML_DATAMODEL_FILENAME + ":", e);
        }
      }
    }
  }
  

  public void importDataModelArchive(DataModelResult dataModelResult, String fileName, boolean overwrite) {
    //Datenmodelle aus datamodels.xml auslesen
    XynaObjectList<DataModel> xol = readDataModels(dataModelResult, fileName);
    
    if (xol == null) {
      return;
    }

    //bereits vorhandene Datenmodelle suchen
    List<String> allExistingDataModels;
    try {
      allExistingDataModels = CollectionUtils.transform(listDataModels(), new GetFqName());
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Trying to find existing data models:", e);
      return;
    }

    //Datenmodelle anlegen
    for(DataModel dataModel : xol) {
      String fqName = dataModel.getType().getFqName();
      DataModelType dmt = dataModelTypes.get(dataModel.getDataModelType());
      if( dmt == null ) {
        dataModelResult.fail("'" + fqName + "' could not be imported, because data model type '" + dataModel.getDataModelType() + "' is missing.");
        continue;
      }
      
      if (allExistingDataModels.contains(fqName)) {
        if (overwrite) {
          Map<String, Object> params = new HashMap<>();
          params.put(OVERWRITE_PARAMETER_NAME, true);
          dmt.removeDataModel(dataModelResult, dataModelStorage, dataModel, params);
        } else {
          dataModelResult.fail("'" + fqName + "' already exists and overwrite-flag is not set.");
          continue;
        }
      }
      
      try {
        dataModelStorage.addDataModel(dataModel);
      } catch (PersistenceLayerException e) {
        dataModelResult.fail("Trying to persist data models:", e);
        return;
      }
    }
    
    //zugeh�rige XMOM xmls speichern
    saveZipEntries(dataModelResult, fileName);
  }
  
  /**
   * Liest das datamodels.xml aus dem zipFile ein.
   * @param dataModelResult
   * @param zipFileName
   * @return
   */
  @SuppressWarnings("unchecked")
  private XynaObjectList<DataModel> readDataModels(DataModelResult dataModelResult, String zipFileName) {
    ApplicationManagementImpl appMgmt = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    Application app = appMgmt.getBasicApplication(BasicApplicationName.Base);
    Long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(app);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(new FileInputStream(new File(zipFileName)));
      
      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        if (zipEntry.getName().equals(XML_DATAMODEL_FILENAME)) {
          logger.debug("Reading file " + XML_DATAMODEL_FILENAME);
          
          //direkt das xml aus dem zipinputstream lesen macht probleme, weil der stream geschlossen wird, deshalb erstmal das xml in eine bytearray transferieren
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          int length = 0;
          byte[] data = new byte[2048];
          while ((length = zis.read(data)) != -1) {
            bos.write(data, 0, length);
          }
          return (XynaObjectList<DataModel>) XynaObject.generalFromXml(bos.toString(Constants.DEFAULT_ENCODING), revision);
        }
      }
      
      //datamodels.xml nicht gefunden
      dataModelResult.fail("Could not find " + XML_DATAMODEL_FILENAME + " in file " + zipFileName + ".");
    } catch (FileNotFoundException e) {
      dataModelResult.fail("Trying to read " + XML_DATAMODEL_FILENAME + ":", e);
    } catch (IOException e) {
      dataModelResult.fail("Trying to read " + XML_DATAMODEL_FILENAME + ":", e);
    } catch (XPRC_XmlParsingException e) {
      dataModelResult.fail("Trying to parse " + XML_DATAMODEL_FILENAME + ":", e);
    } catch (XPRC_InvalidXMLForObjectCreationException e) {
      dataModelResult.fail("Trying to create data models from " + XML_DATAMODEL_FILENAME + ":", e);
    } catch (XPRC_MDMObjectCreationException e) {
      dataModelResult.fail("Trying to create data models from " + XML_DATAMODEL_FILENAME + ":", e);
    } finally {
      if (zis != null) {
        try {
          zis.close();
        } catch (IOException e) {
          dataModelResult.fail("Trying to read " + XML_DATAMODEL_FILENAME + ":", e);
        }
      }
    }
    
    return null;
  }
  
  
  private static class GetFqName implements Transformation<DataModel, String> {
    public String transform(DataModel from) {
      if( from == null ) {
        return null;
      }
      return from.getType().getFqName();
    }
  }
  
  /**
   * Speichert die Dateien aus dem zip-File im rev_datamodel-Verzeichnis (f�r importierte Datenmodelle)
   * @param dataModelResult
   * @param zipFileName
   * @param importedDataModelPaths Pfade der importierten Datenmodelle
   */
  private void saveZipEntries(DataModelResult dataModelResult, String zipFileName) {
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(new FileInputStream(new File(zipFileName)));
      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        if (!zipEntry.isDirectory() && 
            !zipEntry.getName().equals(XML_DATAMODEL_FILENAME)) {
          String savedDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DATAMODEL, false);
          File f = new File(savedDir + Constants.fileSeparator + zipEntry.getName());
          FileUtils.saveToFile(zis, f);
        }
      }
    } catch (FileNotFoundException e) {
      dataModelResult.fail("Trying to read " + zipFileName + ":", e);
    } catch (Ex_FileAccessException e) {
      dataModelResult.fail("Trying to read " + zipFileName + ":", e);
    } catch (IOException e) {
      dataModelResult.fail("Trying to read " + zipFileName + ":", e);
    } finally {
      try {
        if (zis != null) {
          zis.close();
        }
      } catch (IOException e) {
        dataModelResult.fail("Trying to read " + zipFileName + ":", e);
      }
    }
  }

  public boolean isDataModelDeployable(String modelName) {
    if( dataModelIsAlwaysDeployable  ) {
      return true;
    }
    return dataModelStorage.isDataModelDeployable(modelName);
  }

  public String writeDataModel(WriteDataModelParameters parameters, XynaObject data, long revision) throws XynaException {
    DataModelType dmt = getDataModelType(parameters);
    Map<String, Object> paramMap;
    try {
      paramMap = parseParamMap(dmt, ParameterUsage.Write, parameters);
    } catch( StringParameterParsingException e ) {
      throw new XFMG_InvalidDataModelParameterException(e.getMessage(), e);
    }
    return dmt.writeDataModel(dataModelStorage, parameters.getDataModelName(), parameters.getDataModelVersion(), paramMap, data, revision);
  }

  public XynaObject parseDataModel(ParseDataModelParameters parameters, String data, long revision) throws XynaException {
    DataModelType dmt = getDataModelType(parameters);
    Map<String, Object> paramMap;
    try {
      paramMap = parseParamMap(dmt, ParameterUsage.Read, parameters);
    } catch( StringParameterParsingException e ) {
      throw new XFMG_InvalidDataModelParameterException(e.getMessage(), e);
    }
    return dmt.parseDataModel(dataModelStorage, parameters.getDataModelName(), parameters.getDataModelVersion(), paramMap, data, revision);
  }

  public void removeWorkspace(Workspace workspace) throws PersistenceLayerException {
    //Workspace wird entfernt, daher alle Datenmodell-Eintr�ge zu diesem Workspace entfernen
    //TODO Suche nach key-Prefix "%0%.workspaces" ist nur richtig f�r XSD. F�r MIB und TR069 ist das derzeit nicht n�tig.
    List<String> fqNames = dataModelStorage.deleteDataModelSpecifics("%0%.workspaces", workspace.getName() );
    logger.info( "Removed workspace \""+workspace.getName()+"\" from datamodels "+fqNames);
  }
  
  
}
