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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.TypeMappingEntry;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XercesUtils;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException.XmlCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeMappingEntryHelper;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


/**
 * TypeMapperCache erzeugt und sammelt alle TypeMapper, die zu einem Target gehören.
 * 
 * Im Konstruktor wird eine Collection aller zum Target gehörenden TypeMappingEntries 
 * übergeben und daraus alle TypeMapper gebaut. 
 * 
 * Folgende Methoden können dann verwendet werden:
 * <ul>
 * <li> {@link #createXynaObjectForRootElement(Element)} ein </li>
 * </ul>
 *
 */
public class TypeMapperCache {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TypeMapper.class);

  private String targetId;
  private XynaObjectClassLoader xynaObjectClassLoader;
  private Map<FQName, TypeMapper> rootElementTypeMappers = new HashMap<FQName, TypeMapper>();
  private Map<String, TypeMapper> typeMappers = new HashMap<String, TypeMapper>();
  private List<String> namespaces = new ArrayList<String>();
  
  public TypeMapperCache(String targetId, Collection<TypeMappingEntry> typeMappingEntries) throws TypeMapperCreationException {
    this(targetId, new FactoryXynaObjectClassLoader(), typeMappingEntries);
  }

  public TypeMapperCache(String targetId, XynaObjectClassLoader xynaObjectClassLoader, Collection<TypeMappingEntry> typeMappingEntries) throws TypeMapperCreationException {
    this.targetId = targetId;
    this.xynaObjectClassLoader = xynaObjectClassLoader;
    buildBaseTypeMapper();
    buildTypeMapper( typeMappingEntries );
  }
  public TypeMapperCache(String targetId, XynaObjectClassLoader xynaObjectClassLoader, List<TypeInfo> typeInfos) throws TypeMapperCreationException {
     this.targetId = targetId;
     this.xynaObjectClassLoader = xynaObjectClassLoader;
     buildBaseTypeMapper();
     buildTypeMapper( typeInfos );
  }

  private void buildBaseTypeMapper() throws TypeMapperCreationException {
    buildBaseTypeMapper( Constants.createBase_TypeInfo() );
    buildBaseTypeMapper( Constants.createAnyType_TypeInfo() );
  }

  private void buildBaseTypeMapper(TypeInfo ti) throws TypeMapperCreationException {
    String fqName;
    try {
      fqName = GenerationBase.transformNameForJava(ti.getXmomType().getFQTypeName());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new TypeMapperCreationException(TypeMapperCreationException.TypeMapperCreationFailure.Initialization, ti.getXmomType().getFQTypeName(), e);
    }
    Class<? extends XynaObject> clazz = xynaObjectClassLoader.loadClass(fqName);
    XynaObjectClassInfo xoci = new XynaObjectClassInfo(clazz);
    TypeMapper typeMapper = new TypeMapper(xoci, this);
    typeMapper.initialize(ti);
    typeMappers.put( fqName, typeMapper);
  }



  public interface XynaObjectClassLoader {

    Class<? extends XynaObject> loadClass(String fqClassName) throws TypeMapperCreationException;
    
  }
  
  public static class FactoryXynaObjectClassLoader implements XynaObjectClassLoader {
    private long revision;
    private ClassLoaderDispatcher classLoaderDispatcher;
    
    public FactoryXynaObjectClassLoader(long revision) {
      this.revision = revision;
      classLoaderDispatcher = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    }
    
    public FactoryXynaObjectClassLoader() {
      ClassLoader cl = getClass().getClassLoader();
      if( cl instanceof ClassLoaderBase ) {
        revision = ((ClassLoaderBase)cl).getRevision();
      } else {
        revision = Long.valueOf(-1);
      }
      classLoaderDispatcher = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    }
    
    public Class<? extends XynaObject> loadClass(String className) throws TypeMapperCreationException {
      if (className == null) {
        return null;
      }
      Class<? extends XynaObject> clazz;
      try {
        clazz = classLoaderDispatcher.loadMDMClass(className, false, null, null, revision);
      } catch (ClassNotFoundException e) {
        throw new TypeMapperCreationException( TypeMapperCreationFailure.XynaObjectClassNotFound, className+" in rev "+revision, e);
      }
      if( clazz == null ) {
        throw new TypeMapperCreationException(TypeMapperCreationFailure.XynaObjectClassNotFound, className+" in rev "+revision);
      }
      return clazz;
    }
    
  }
  private void buildTypeMapper(Collection<TypeMappingEntry> typeMappingEntries) throws TypeMapperCreationException {
    if(typeMappingEntries == null || typeMappingEntries.isEmpty() ) {
      throw new TypeMapperCreationException(TypeMapperCreationFailure.Database, "No data found");
    }
    
    TypeMappingEntryHelper tmeh = new TypeMappingEntryHelper("");
    
    List<TypeInfo> typeInfos = tmeh.importTypeMappingEntries(typeMappingEntries);
    
    if( logger.isDebugEnabled() ) {
      logger.debug( "Found "+typeInfos.size()+" types for target "+targetId);
    }
    
    buildTypeMapper( typeInfos );
  }
  
  private void buildTypeMapper(List<TypeInfo> typeInfos) throws TypeMapperCreationException {
    for( TypeInfo typeInfo : typeInfos ) {
      String fqClassName = typeInfo.getXmomType().getFQTypeName(); //FIXME Bug 23174
      try {
        Class<? extends XynaObject> clazz = xynaObjectClassLoader.loadClass( fqClassName );
        XynaObjectClassInfo xoci = new XynaObjectClassInfo(clazz);
        TypeMapper typeMapper = new TypeMapper(xoci, this);
        typeMapper.initialize(typeInfo);
        typeMappers.put( fqClassName, typeMapper);
      } catch( TypeMapperCreationException e) {
        logger.warn("Exception for typeInfo " +typeInfo.getXmomType() +" " + typeInfo.getXmomType().getFQTypeName() );
        throw e;
      }
    }
    
    //nachdem nun alle TypeMapper angelegt wurden, kann SetterGetterCreator gebaut und verwendet werden
    SetterGetterCreator setterCreator = new SetterGetterCreator(Collections.unmodifiableMap(typeMappers));
    
    Set<String> namespacesSet = new HashSet<String>();
    for ( TypeMapper typeMapper : typeMappers.values() ) {
      //Namespaces und RootElemente suchen
      String ns = typeMapper.getName().getNamespace();
      if( ns != null ) {
        namespacesSet.add( ns );
      }
      for( FQName root : typeMapper.getRootElements() ) {
        rootElementTypeMappers.put(root, typeMapper);
        namespacesSet.add( root.getNamespace() );
      }
      
      //Setter initialisieren
      typeMapper.initializeSetter(setterCreator);
    }
    boolean nullContained = namespacesSet.remove(null);
    
    namespaces.addAll( namespacesSet );
    Collections.sort(namespaces);
    if( nullContained ) {
      namespaces.add(null);
    }
    if( logger.isDebugEnabled() ) {
      logger.debug( "Found "+namespaces.size()+" namespaces for target "+targetId);
    }

  }

  /**
   * Erzeugt ein XynaObject für das übergebene RootElement
   * @param element
   * @return
   * @throws XynaObjectCreationException
   */
  public XynaObject createXynaObjectForRootElement(Element element) throws XynaObjectCreationException {
    TypeMapper typeMapper = getTypeMapperForRootElement(element); 
    XynaObject xo = typeMapper.createXynaObject(element);
    return xo;
  }
  
  private TypeMapper getTypeMapperForRootElement(Element element) throws XynaObjectCreationException {
    //RootElemente müssen eindeutigen FQName haben
    FQName name = new FQName( element.getNamespaceURI(), element.getLocalName());
    TypeMapper typeMapper = rootElementTypeMappers.get(name);
    if( typeMapper != null ) {
      return typeMapper;
    }
    if( element.getNamespaceURI() == null ) {
      //RootElement war unqualifiziert, nochmal in allen Namespaces suchen
      logger.warn( "No namespace given for root element "+element+", trying to guess.." ); 
      for( String ns : namespaces ) {
        FQName nameGuess = new FQName( ns, element.getLocalName());
        typeMapper = rootElementTypeMappers.get(nameGuess);
        if( typeMapper != null ) {
         return typeMapper;
        }
      }
    }
    
    //Keinen TypeMapper gefunden, daher Fehler
    XynaObjectCreationException xoce = null;
    xoce = new XynaObjectCreationException(XynaObjectCreationFailure.UnknownRootElement, name.toString());
    xoce.setType(name.toString());
    throw xoce;
  }

  /**
   * Erzeugt ein XML für das übergebene XynaObject 
   * Falls im XSD mehrere RootElemente für diesen Typ möglich sind, muss der RootElementName angegeben werden.
   * @param xo
   * @return
   * @throws XmlCreationException 
   */
  public String createXmlFor(XynaObject xo, CreateXmlOptions createXmlOptions) throws XmlCreationException {
    if (xo == null) {
      throw new IllegalArgumentException("Input may not be null.");
    }
    //Namespaces definieren
    NamespacePrefixCache namespacePrefixCache = createXmlOptions.getNamespacePrefixCache();
    namespacePrefixCache.addNamespaces( namespaces );
    
    Document doc;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      doc = db.newDocument();
      
      TypeMapper typeMapper = typeMappers.get( xo.getClass().getName() );
      if (typeMapper == null) {
        throw new XmlCreationException(XmlCreationFailure.Creation, "Can not create xml for unsupported type: " + xo.getClass());
      }
      
      XmlContext xmlContext = new XmlContext(doc, createXmlOptions);
      
      FQName rootElement = getRootElement(typeMapper, createXmlOptions.getRootElementName() );
      
      Element root = typeMapper.fillXmlElement(xmlContext, xmlContext.createElement(true, rootElement), xo);
      
      //Namespaces definieren und ans Root-Element anhängen
      xmlContext.appendNamespaces(root);
      
      doc.appendChild( root );
    } catch(ParserConfigurationException e) {
      throw new XmlCreationException(XmlCreationFailure.Configuration, e );
    }
    try {
      return XercesUtils.writeDocumentToString(doc, createXmlOptions.includePIElement());
    } catch( LSException e ) {
      throw new XmlCreationException(XmlCreationFailure.Writing, e );
    }
  }
  
  private FQName getRootElement(TypeMapper typeMapper, String rootElementName) {
    boolean rootElementsExists = typeMapper.getRootElements() != null && typeMapper.getRootElements().size() > 0;
    if( rootElementsExists ) {
      if( rootElementName != null ) {
        //Versuchen, den übergebenen rootElementName zu finden
        for( FQName root : typeMapper.getRootElements() ) {
          if( root.getName().equals(rootElementName) ) {
            return root;
          }
        }
        //kein passendes RootElement gefunden, daher neu bauen
        return new FQName( typeMapper.getName().getNamespace(), rootElementName); 
      } else {
        //erstes RootElement nehmen
        return typeMapper.getRootElements().get(0);
      }
    } else {
      //kein RootElement definiert, daher neu bauen
      return new FQName( typeMapper.getName().getNamespace(), rootElementName == null ? "rootElement" : rootElementName); 
    }
  }

  public Set<String> getFQClassNames() {
    return Collections.unmodifiableSet(typeMappers.keySet());
  }

  public TypeMapper getTypeMapperFor(FQName type) {
    for (TypeMapper tm : typeMappers.values()) {
      if (tm.getName().equals(type)) {
        return tm;
      }
    }
    return null;
  }
  
}
