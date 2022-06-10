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
package com.gip.xyna.xdev.map.mapping;

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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;
import com.gip.xyna.xdev.map.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.XmlCreationException.XmlCreationFailure;
import com.gip.xyna.xdev.map.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xdev.map.typegen.XercesUtils;
import com.gip.xyna.xdev.map.types.FQName;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;


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

  private static final String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String NAMESPACE_XMLNS = "http://www.w3.org/2000/xmlns/";
  
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
    buildTypeMapper( typeMappingEntries );
  }
  public TypeMapperCache(String targetId, XynaObjectClassLoader xynaObjectClassLoader, List<TypeInfo> typeInfos) throws TypeMapperCreationException {
     this.targetId = targetId;
     this.xynaObjectClassLoader = xynaObjectClassLoader;
     buildTypeMapper( typeInfos );
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
        throw new TypeMapperCreationException( TypeMapperCreationFailure.XynaObjectClassNotFound, className, e);
      }
      if( clazz == null ) {
        throw new TypeMapperCreationException(TypeMapperCreationFailure.XynaObjectClassNotFound, className);
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
      String fqClassName = typeInfo.getXmomType().getFQTypeName();
      Class<? extends XynaObject> clazz = xynaObjectClassLoader.loadClass( fqClassName );
      
      XynaObjectClassInfo xoci = new XynaObjectClassInfo(clazz);
      TypeMapper typeMapper = new TypeMapper( xoci);
      typeMapper.initialize(typeInfo);
      typeMappers.put( fqClassName, typeMapper);
    }
    
    //nachdem nun alle TypeMapper angelegt wurden, kann SetterGetterCreator gebaut und verwendet werden
    SetterGetterCreator setterCreator = new SetterGetterCreator(Collections.unmodifiableMap(typeMappers));
    
    Set<String> namespacesSet = new HashSet<String>();
    for ( TypeMapper typeMapper : typeMappers.values() ) {
      //Namespaces und RootElemente suchen
      namespacesSet.add( typeMapper.getName().getNamespace() );
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
  
  /**
   * Liefert die XynaObject-Class für das übergebene RootElement
   * @param element
   * @return
   */
  public Class<? extends XynaObject> getClassForRootElement(Element element) throws XynaObjectCreationException {
    TypeMapper typeMapper = getTypeMapperForRootElement(element); 
    return typeMapper.getXynaObjectClassInfo().getXynaObjectClass();
  }

  public void checkRootElementInstanceOf(Element element, Class<? extends XynaObject> base) throws XynaObjectCreationException {
    TypeMapper typeMapper = getTypeMapperForRootElement(element); 
    Class<? extends XynaObject> rootElementClass = typeMapper.getXynaObjectClassInfo().getXynaObjectClass();
    if( ! base.isAssignableFrom(rootElementClass) ) {
      XynaObjectCreationException xoce = null;
      xoce = new XynaObjectCreationException(XynaObjectCreationFailure.UnexpectedType, rootElementClass.getName() );
      xoce.setType(new FQName( element.getNamespaceURI(), element.getLocalName()).toString());
      throw xoce;
    }
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
        name = new FQName( ns, element.getLocalName());
        typeMapper = rootElementTypeMappers.get(name);
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
   * @param rootElementName
   * @return
   * @throws XmlCreationException 
   */
  public String createXmlFor(XynaObject xo, String rootElementName) throws XmlCreationException {
    return createXmlFor(xo, rootElementName, new CreateXmlOptions() );
  }

  /**
   * Erzeugt ein XML für das übergebene XynaObject 
   * Falls im XSD mehrere RootElemente für diesen Typ möglich sind, muss der RootElementName angegeben werden.
   * @param xo
   * @param rootElementName
   * @return
   * @throws XmlCreationException 
   */
  public String createXmlFor(XynaObject xo, String rootElementName, CreateXmlOptions createXmlOptions) throws XmlCreationException {
    if (xo == null) {
      throw new IllegalArgumentException("Input may not be null.");
    }
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
            
      FQName rootElement = getRootElement(typeMapper, rootElementName );
      Element root = doc.createElementNS(rootElement.getNamespace(), rootElement.getName());
      root = typeMapper.fillXmlElement(doc, root, xo, createXmlOptions);
      doc.appendChild( root );
     
      //Namespaces definieren
      createXmlOptions.addNamespace(NAMESPACE_XSI, "xsi");
      createXmlOptions.addNamespaces( namespaces );
      
      
      //Namespaces definieren und ans Root-Element anhängen
      for( Map.Entry<String,String> entry : createXmlOptions.getNamespacePrefixes().entrySet() ) {
        String name;
        if( entry.getValue() != null ) {
          name = "xmlns:"+entry.getValue();
        } else {
          name = "xmlns";
        }
        root.setAttributeNS(NAMESPACE_XMLNS, name, entry.getKey() ); //"xmlns:" ist hier Pflicht, sonst "org.w3c.dom.DOMException: NAMESPACE_ERR"
      }
      createXmlOptions.addNamespace(NAMESPACE_XMLNS, "xmlns"); //zur Sicherheit, eigentlich bereits direkt darüber gesetzt
     
      //Korrektur aller Namespace-Prefixe
      NodeList nodes = doc.getElementsByTagName("*");
      for( int n=0; n<nodes.getLength(); ++n ) {
        Node node = nodes.item(n);
        String prefix = createXmlOptions.getNamespacePrefix(node.getNamespaceURI());
        if( prefix != null ) {
          node.setPrefix( prefix);
        }
        //Korrektur in Attributen
        NamedNodeMap nnm = node.getAttributes();
        if( nnm != null ) {
          for( int a=0; a<nnm.getLength(); ++a ) {
            String ns = nnm.item(a).getNamespaceURI();
            if( ns != null ) {
              prefix = createXmlOptions.getNamespacePrefix(ns);
              if( prefix != null ) {
                nnm.item(a).setPrefix( prefix);
              }
            }
          }
        }
      }

    } catch(ParserConfigurationException e) {
      throw new XmlCreationException(XmlCreationFailure.Configuration, e );
    }
    try {
      return XercesUtils.writeDocumentToString(doc);
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
  
  public List<String> listTypes() {
    return CollectionUtils.transform(typeMappers.values(), new TypeMapperTypeToString() );
  }

  private static class TypeMapperTypeToString implements Transformation<TypeMapper, String> {

    public String transform(TypeMapper typeMapper) {
      return typeMapper.getXynaObjectClassInfo().getClassName();
    }
    
  }


}
