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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateVersionForApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.BasicApplicationName;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.StartApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.ImportParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.TypeMappingCache;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.TypeMappingEntry;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.WorkspaceHelper;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.WSDLParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.XSDParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.XMLDocumentUtilsGenerator;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.DataTypeXmlHelper;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeMappingEntryHelper;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;


/**
 *
 */
public class TypeGeneration {
  private static Logger logger = CentralFactoryLogging.getLogger(TypeGeneration.class);
  
  public final static String XSD_DATAMODEL_BASE_APP_NAME = "XSD Datamodel Base";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION = "1.0";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_WITH_VALIDATE = "1.1";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_DEPENDENCY_CONTEXT_ADJUSTED = "1.1.1";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_MIXED_DISTRIBUTIONS = "1.1.2";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_MIXED_DISTRIBUTIONS_BUGFIX = "1.1.3";
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_NEWEST = "1.1.3"; //Bugfix 23382 falsche Version 0 
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_NEWEST = "1.1.4";  //Bugfix 23393 XSD:AnyType wird nun besser umgesetzt
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_REPAIR_REV = "1.1.5";  //Bugfix 23691: Revision Fallback auf Rootrevision
  //public final static String XSD_DATAMODEL_BASE_APP_VERSION_FOUNDDM_NOT_NULL = "1.1.6";  //Bugfix 23293: ValidateXML hat Datamodel nicht ermittelt
  public final static String XSD_DATAMODEL_BASE_APP_VERSION_CDATA = "1.1.7";
  
  private XSModel schema;
  private TypeInfoGenerator typeInfoGenerator;
  private XmomGenerator xmomGenerator;
  private List<TypeMappingEntry> typeMappings;
  private XSDErrorHandler errorHandler;
  private XmomDataCreator xmomDataCreator;
  private GenerationParameter generationParameter;
  private Set<String> readXSDs;

    
  public TypeGeneration(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
    errorHandler = new XSDErrorHandler();
    xmomDataCreator = new XmomDataCreator(generationParameter);
    try {
      xmomGenerator = XmomGenerator.
          inRevision(RevisionManagement.REVISION_DATAMODEL).
          overwrite(generationParameter.isOverwrite()).
          build();
    } catch (XFMG_NoSuchRevision e) {
      throw new RuntimeException(e); //sollte nie auftreten können
    }
  }

  /**
   * liest XSDs ein, erzeugt DOM-Daten
   *
   * @param xsds
   * @return 
   * @throws FileNotFoundException
   * @throws XSDParsingException
   * @throws WSDLParsingException 
   * @throws TypeMapperCreationException 
   */
  public Set<String> parseXSDs(List<String> xsdsWsdlsDirs) throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    XsdWsdlSorter xsdWsdlSorter = new XsdWsdlSorter();
    xsdWsdlSorter.sortAll( xsdsWsdlsDirs );
    
    if( ! xsdWsdlSorter.getWsdls().isEmpty() ) {
      WsdlToXsd wsdlToXsd = new WsdlToXsd();
      for( File wsdl : xsdWsdlSorter.getWsdls() ) {
        boolean containsXsd = wsdlToXsd.extract(wsdl);
        if( containsXsd ) {
          File wsdlXsd = wsdlToXsd.saveXsdAs( wsdl.getAbsolutePath()+".xsd" );
          xsdWsdlSorter.addXsd( wsdlXsd );
        }
      }
    }
    
    if( logger.isDebugEnabled() ) {
      logger.debug("Parsing XSDs "+xsdWsdlSorter.getXsds() );
    }
    XSLoader loader = XercesUtils.getXSLoader(errorHandler);
    
    StringListImpl uriList = new StringListImpl(new Vector<String>(xsdWsdlSorter.getXsds()));
    schema = loader.loadURIList(uriList);
    
    if( errorHandler.hasErrors() ) {
      throw new XSDParsingException(errorHandler.getErrors() );
    }
    if (schema == null) {
      throw new XSDParsingException("XSModel is null.");
    }

    readXSDs = xsdWsdlSorter.getXsds();
    return readXSDs;
  }

  private static class XsdWsdlSorter implements FileFilter {
    Set<String> xsds = new HashSet<String>();
    List<File> wsdls = new ArrayList<File>();
    
    public Set<String> getXsds() {
      return xsds;
    }


    public List<File> getWsdls() {
      return wsdls;
    }

    /**
     * Akzeptiert nur Verzeichnisse, XSDs und WSDLs werden gleich einsortiert
     */
    public boolean accept(File pathname) {
      if( pathname.isDirectory() ) {
        return true;
      }
      if( pathname.getName().toLowerCase().endsWith(".xsd") ) {
        addXsd(pathname);
        return false;
      }
      if( pathname.getName().toLowerCase().endsWith(".wsdl") ) {
        wsdls.add(pathname);
        return false;
      }
      return false;
    }

    public void sortAll(List<String> xsdsWsdlsDirs) {
      for (String xwd : xsdsWsdlsDirs) {
        File f = new File(xwd);
        if( accept(f) ) {
          sortDirectory(f);
        }
      }
    }
    
    public void addXsd(File xsd) {
      xsds.add(xsd.toURI().toString());
    }

    private void sortDirectory(File dir) {
      for( File d : dir.listFiles(this) ) {
        sortDirectory(d);
      }
    }
  }

  /**
   * iteriert durch XSD-DOM-Baum und erzeugt Informationen ueber XSD-Struktur,
   * die im Generator-Objekt gespeichert werden. Erzeugt die XMOMs und TypeMapping-Einträge.
   */
  public void generateTypes() {
    typeInfoGenerator = new TypeInfoGenerator(generationParameter);
    XSNamedMap namedMap = schema.getComponents(XSConstants.ELEMENT_DECLARATION);
    for (int j = 0; j < namedMap.getLength(); j++) {
      XSObject el = namedMap.item(j);
      typeInfoGenerator.addRootLevelElement((XSElementDeclaration)el);
    }
    
    if( generationParameter.isGenerationOptions_includeHiddenTypes() || typeInfoGenerator.getAnyTypeCount() > 0 ) {
      namedMap = schema.getComponents(XSConstants.TYPE_DEFINITION);
      for (int j = 0; j < namedMap.getLength(); j++) {
        XSObject el = namedMap.item(j);
        typeInfoGenerator.addType(el);
      }
    }
    
    //XMOM-Namen festlegen 
    typeInfoGenerator.createXMOMData(xmomDataCreator);
  }

  /**
   * erzeugt die tatsaechlichen Xyna-XMOM-Datentypen, speichert aber noch nicht
   */
  public void generateDataTypes(DataModel dm) {
    //XMOM-Objekte vorbereiten
    DataTypeXmlHelper dtg = new DataTypeXmlHelper(generationParameter);
    for( TypeInfo typeInfo : typeInfoGenerator.getAllTypeInfos() ) {
      xmomGenerator.add( dtg.toDatatype(typeInfo, dm) );
    }
  }
  
  public List<Datatype> listAlreadyExistingDatatypes() {
    return xmomGenerator.listAlreadyExistingDatatypes();
  }

  /**
   * speichert die tatsächlichen XMOM-Datentypen und die DatenModell-Daten
   */
  public boolean saveDataTypes_persistDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, DataModel dataModel, ImportParameter parameter) {
    if (parameter.getDistributeToWorkspaces()) {
      try {
        xmomGenerator.save();
      } catch (Ex_FileWriteException e) {
        dataModelResult.fail("Failed to save XMOM-Types", e);
        return false;
      }
    } else {
      try {
        ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                        .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        List<RuntimeContextRequirementXmlEntry> rcrxes = Collections.singletonList(lazyCreateXsdBaseApp(applicationManagement));
        StringBuilder comment = new StringBuilder("XSD Datamodel");
        for (String xsd : readXSDs) {
          comment.append("\n- ").append(new File(xsd).getName());
        }
        File applicationZip = xmomGenerator.buildApplication(dataModel.getType().getName(), dataModel.getVersion(), comment.toString(), rcrxes);
        importApplication(applicationManagement, applicationZip);
        applicationManagement.startApplication(parameter.getDataModelName(), dataModel.getVersion(), new StartApplicationParameters());
      } catch (Exception e) {
        dataModelResult.fail("Failed to save XMOM-Types", e);
        logger.debug("Failed to save XMOM-Types", e);
        return false;
      }
    }
    
    List<XmomType> xts = new ArrayList<XmomType>();
    for (Datatype datatype : xmomGenerator.getDatatypes() ) {
      xts.add( new XmomType(datatype.getType()) );
    }
    dataModel.setXmomTypes(xts);
    dataModel.setXmomTypeCount(xts.size());
    
    try {
      dataModelStorage.addDataModel(dataModel);
    } catch (PersistenceLayerException e) {
      dataModelResult.fail("Failed to persist DataModel", e);
      return false;
    }
    dataModelResult.info("Saved data model "+dataModel.getBaseType().getLabel()+" with "+ dataModel.getXmomTypeCount()+" data types." );
   
    return true;
  }


  private void importApplication(ApplicationManagementImpl appMgmt, File applicationFile) throws XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    TemporarySessionAuthentication tsa = 
      TemporarySessionAuthentication.tempAuthWithUniqueUser("ImportDataModel", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
    try {
      tsa.initiate();
    } catch (Exception e) {
      // ntbd
    }
    try {
      appMgmt.importApplication(applicationFile.getAbsolutePath(), true, true, 
                                              false, false, false, false, false, true, false, tsa.getUsername(), null);
    } finally {
      tsa.destroy();
    }
  }


  private List<RuntimeContextRequirementXmlEntry> createBasicAppRequirements(ApplicationManagementImpl appMgmt) {
    Application basicApp = appMgmt.getBasicApplication(BasicApplicationName.Processing);
    List<RuntimeContextRequirementXmlEntry> basicAppRequirements = new ArrayList<RuntimeContextRequirementXmlEntry>();
    basicAppRequirements.add(new RuntimeContextRequirementXmlEntry(basicApp));
    return basicAppRequirements;
  }

  private RuntimeContextRequirementXmlEntry lazyCreateXsdBaseApp(ApplicationManagementImpl appMgmt) throws PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, Ex_FileAccessException, XFMG_NoSuchRevision, IOException, ParserConfigurationException {
    Collection<ApplicationStorable> applications = appMgmt.listApplicationStorables();
    for (ApplicationStorable application : applications) {
      if (application.getName().equals(XSD_DATAMODEL_BASE_APP_NAME) &&
          application.getVersion().equals(XSD_DATAMODEL_BASE_APP_VERSION_CDATA) &&
          application.getStateAsEnum() != ApplicationState.AUDIT_MODE) {
        return new RuntimeContextRequirementXmlEntry(XSD_DATAMODEL_BASE_APP_NAME, XSD_DATAMODEL_BASE_APP_VERSION_CDATA, null);
      }
    }
    createXsdBaseApp(appMgmt);
    return new RuntimeContextRequirementXmlEntry(XSD_DATAMODEL_BASE_APP_NAME, XSD_DATAMODEL_BASE_APP_VERSION_CDATA, null);
  }

  public void createXsdBaseApp(ApplicationManagementImpl appMgmt) throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException, Ex_FileAccessException, IOException, ParserConfigurationException {
    XmomGenerator baseGenerator;
    try {
      baseGenerator = XmomGenerator.inRevision(RevisionManagement.REVISION_DATAMODEL).build();
    } catch (XFMG_NoSuchRevision e) {
      // should never happen for REVISION_DATAMODEL
      throw new RuntimeException(e);
    }
    baseGenerator.add(WorkspaceHelper.createBaseType());
    XMLDocumentUtilsGenerator utilsGen = new XMLDocumentUtilsGenerator(generationParameter);
    utilsGen.createConstants();
    utilsGen.createAndAddUtilTypes(baseGenerator);
    StringBuilder comment = new StringBuilder("XSD Datamodel Utils");
    File xsdBaseApp = baseGenerator.buildApplication(XSD_DATAMODEL_BASE_APP_NAME, XSD_DATAMODEL_BASE_APP_VERSION_CDATA, comment.toString(), createBasicAppRequirements(appMgmt));
    try {
      importApplication(appMgmt, xsdBaseApp);
    } catch (Exception e) {
      logger.debug("createXsdBaseApp", e);
      throw new RuntimeException(e);
    }
  }

  public List<TypeMappingEntry> createTypeMappingEntries(String idForTypeMapping) {
    TypeMappingEntryHelper tmeh = new TypeMappingEntryHelper(idForTypeMapping);
    typeMappings = new ArrayList<TypeMappingEntry>();
    for( TypeInfo typeInfo : typeInfoGenerator.getAllTypeInfos() ) {
      typeMappings.addAll( tmeh.toTypeMappingEntries(typeInfo) );
    }
    return typeMappings;
  }
  
  public List<TypeMappingEntry> getTypeMappingEntries() {
    return typeMappings;
  }

  /**
   * persistiert die zuvor erzeugten TypeMapping-Infos
   */
  public void storeTypeMappings(TypeMappingCache typeMappingCache, String idForTypeMapping) {
    createTypeMappingEntries(idForTypeMapping);

    TypeMappingCache tmc = typeMappingCache;
    try {
      if( tmc == null ) {
        tmc = new TypeMappingCache();
      }
      tmc.store(typeMappings);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
   
  }
  
  public List<String> getDataTypeFqNames() {
    return new ArrayList<String>( xmomGenerator.getAllFqNames() );
  }
  
  public Collection<Datatype> getDataTypes() {
    return xmomGenerator.getDatatypes();
  }


  public static class XSDErrorHandler implements DOMErrorHandler {
    
    List<String> errors = new ArrayList<String>();
        
    public boolean handleError(DOMError error) {
      //hier schon auswerten, da DOMError recyclet wird (nur eine Instanz für alle Fehler)
      StringBuilder sb = new StringBuilder();
      
      
      switch( error.getSeverity() ) {
        case DOMError.SEVERITY_ERROR:
          sb.append("[Error] ");
          break;
        case DOMError.SEVERITY_WARNING:
          sb.append("[Warning] ");
          break;
        case DOMError.SEVERITY_FATAL_ERROR:
          sb.append("[Fatal] ");
          break;
        default:
          sb.append("[Other] ");
          break;
      }
      
      String filename = error.getLocation().getUri();
      if( filename != null ) {
        int idx = filename.lastIndexOf(File.separatorChar);
        if( idx > 0 ) {
          filename = filename.substring(idx+1);
        }
        sb.append(filename);
        sb.append(":").append(error.getLocation().getLineNumber());
        sb.append(":").append(error.getLocation().getColumnNumber());
        sb.append(":");
      }
      sb.append(" ").append(error.getMessage());
     
      errors.add( sb.toString() );
      return false;
    }

    public List<String> getErrors() {
      return errors;
    }

    public String getErrorString() {
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for( String error : errors ) {
        sb.append(sep).append(error);
        sep = "\n";
      }
      return sb.toString();
    }

    public boolean hasErrors() {
      return ! errors.isEmpty();
    }
  }
  

  public List<TypeInfo> getTypeInfos() {
    return typeInfoGenerator.getAllTypeInfos();
  }

  public List<String> listNamespaceXmomPath() {
    List<String> list = new ArrayList<String>();
    for( Map.Entry<String,String> e : xmomDataCreator.getNamespaceXmomPath().entrySet() ) {
      if( e.getKey() == null ) {
        list.add( "no namespace "+e.getValue());
      } else {
        list.add( e.getKey()+" "+e.getValue());
      }
    }
    return list;
  }

  public List<String> listXmomPaths() {
    return xmomDataCreator.listXmomPaths();
  }

  public Set<String> getReadXSDs() {
    return readXSDs;
  }

  public int getAnyTypeUsage() {
    return typeInfoGenerator.getAnyTypeCount();
  }

}
