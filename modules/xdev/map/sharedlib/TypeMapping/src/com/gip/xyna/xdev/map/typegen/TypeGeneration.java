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
package com.gip.xyna.xdev.map.typegen;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xdev.map.typegen.exceptions.WSDLParsingException;
import com.gip.xyna.xdev.map.typegen.exceptions.XSDParsingException;
import com.gip.xyna.xdev.map.types.DataTypeXmlHelper;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class TypeGeneration {
  private static Logger logger = CentralFactoryLogging.getLogger(TypeGeneration.class);
  
  private XSModel schema;
  private TypeInfoGenerator typeInfoGenerator;
  private XmomGenerator xmomGenerator;
  private List<TypeMappingEntry> typeMappings;
  private XSDErrorHandler errorHandler;
  private XmomDataCreator xmomDataCreator;
  private GenerationParameter generationParameter;

  
  public TypeGeneration(TypeGenerationOptions typeGenerationOptions) {
    this( (GenerationParameter) typeGenerationOptions);
  }
  
  public TypeGeneration(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
    errorHandler = new XSDErrorHandler();
    xmomDataCreator = new XmomDataCreator(generationParameter);
    xmomGenerator = XmomGenerator.with(-1L); //FIXME wegen Abwärtskompatibilität nötig, generationParameter.isOverwrite() nutzen
  }

  public TypeGeneration(String basePathForGeneration, 
                        boolean useNamespaceForXmomPath, boolean changeLabelForAttribute,
                        XmomType baseXmomTypeRoot, XmomType baseXmomType) {
    this( new TypeGenerationOptions(basePathForGeneration,useNamespaceForXmomPath,changeLabelForAttribute,baseXmomTypeRoot,baseXmomType) );
  }
    
  public TypeGeneration(String basePathForGeneration, 
                        boolean useNamespaceForXmomPath, boolean changeLabelForAttribute ) {
    
    this( new TypeGenerationOptions(basePathForGeneration,useNamespaceForXmomPath,changeLabelForAttribute,null,null) );
  }
  

  /**
   * liest XSDs ein, erzeugt DOM-Daten
   *
   * @param xsds
   * @throws FileNotFoundException
   * @throws XSDParsingException
   * @throws WSDLParsingException 
   * @throws TypeMapperCreationException 
   */
  public void parseXSDs(List<String> xsdsWsdlsDirs) throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    
    
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
    //XMOM-Namen festlegen 
    typeInfoGenerator.createXMOMData(xmomDataCreator);
  }

 
  /**
   * erzeugt und deployt die tatsaechlichen Xyna-XMOM-Datentypen
   *
   */
  public void saveAndDeployDataTypes() {
    //XMOM-Objekte vorbereiten
    DataTypeXmlHelper dtg = new DataTypeXmlHelper(generationParameter);
    for( TypeInfo typeInfo : typeInfoGenerator.getAllTypeInfos() ) {
      xmomGenerator.add( dtg.toDatatype(typeInfo) );
    }
    try {
      xmomGenerator.save();
      xmomGenerator.deploy();
    } catch (XynaException e) {
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
  
  public List<String> getDataTypes() {
    return new ArrayList<String>( xmomGenerator.getAllFqNames() );
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


}
