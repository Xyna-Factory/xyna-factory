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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.WrappedMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectDeploymentListenerManagement.XynaObjectDeploymentListener;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.InformationUtils;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.TypeMapperCache.FactoryXynaObjectClassLoader;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.XMLParserWriterCache.XMLParserWriter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.DataTypeXmlHelper;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 * Hält alle TypeMapperCaches für eine Revision. 
 * Ist XynaObjectDeploymentListener, um bei Undeploy eines Datentyps den gesamten betroffenen TypeMapperCache zu entfernen.
 *
 */
public class XMLParserWriterCache extends WrappedMap<Pair<String, String>,XMLParserWriter> implements XynaObjectDeploymentListener {

  private static Logger logger = CentralFactoryLogging.getLogger(XMLParserWriterCache.class);
  
  private long revision;
  private String dataModelType;
  private ConcurrentMap<String,Pair<String, String>> dataModelForFqClassName = new ConcurrentHashMap<String,Pair<String, String>>();
  
  public XMLParserWriterCache(long revision, String dataModelType) {
    super( new ConcurrentHashMap<Pair<String, String>,XMLParserWriter>());
    this.revision = revision;
    this.dataModelType = dataModelType;
    //Registrieren, um bei Undeploy benachrichtigt zu werden
    XynaObject.getXynaObjectDeploymentListenerManagement().registerXynaObjectDeploymentListener(revision, this);
  }

  public XMLParserWriter getOrCreate(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion) throws XynaException, TypeMapperCreationException {
    XMLParserWriter value = wrapped.get(Pair.of(dataModelName, dataModelVersion));
    if( value == null ) {
      synchronized (this) {
        value = wrapped.get(Pair.of(dataModelName, dataModelVersion));
        if( value == null ) {
          value = new XMLParserWriter(dataModelStorage,dataModelName, dataModelType, dataModelVersion, revision);
          XMLParserWriter orig = ((ConcurrentHashMap<Pair<String, String>,XMLParserWriter>)wrapped).putIfAbsent(Pair.of(dataModelName, dataModelVersion), value);
          if( orig == null ) {
            for( String fqcn : value.getFQClassNames() ) {
              dataModelForFqClassName.put(fqcn,Pair.of(dataModelName, dataModelVersion));
            }
          } else {
            value = orig;
          }
        }
      }
    }
    return value;
  }

  public void deploy(String fqClassName, long revision, Class<? extends XynaObject> clazz) {
    //nichts zu tun
  }
  
  public void undeploy(String fqClassName, long revision, Class<? extends XynaObject> clazz) {
    //TODO: nicht ganzen TypeMapper wegwerfen, sondern einzeln neu initialisieren
    Pair<String, String> dataModel = dataModelForFqClassName.get(fqClassName);
    if( dataModel == null ) {
      return; //nichts zu tun
    }
    remove(dataModel);
  }
  
  public XMLParserWriter remove(Pair<String, String> dataModel) {
    XMLParserWriter xpw = wrapped.remove(dataModel);
    if( xpw != null ) {
      for( String fqcn : xpw.getFQClassNames() ) {
        dataModelForFqClassName.remove(fqcn);
      }
    }
    return xpw;
  }

  
  public static class XMLParserWriter {
    private TypeMapperCache tmc;
    
    public XMLParserWriter(DataModelStorage dataModelStorage, String dataModelName, String dataModelType, String dataModelVersion, long revision) throws XynaException, TypeMapperCreationException {
      try {
        List<TypeInfo> typeInfos = getTypeInfos(dataModelStorage, dataModelName, dataModelType, dataModelVersion, revision);
        tmc = new TypeMapperCache(dataModelName, new FactoryXynaObjectClassLoader(revision), typeInfos);
      } catch (TypeMapperCreationException e) {
        throw e;
      } catch (Throwable t) {
        throw new TypeMapperCreationException(TypeMapperCreationFailure.Initialization, dataModelName, t);
      }
    }
    
    public Set<String> getFQClassNames() {
      return tmc.getFQClassNames();
    }

    private List<TypeInfo> getTypeInfos(DataModelStorage dataModelStorage, String dataModelName, String dataModelType, String dataModelVersion, long revision) throws XynaException, TypeMapperCreationException {
      String fqName = dataModelStorage.getFqName(dataModelType,dataModelVersion,dataModelName);
      if (fqName == null) {
        throw new XFMG_NoSuchDataModelException(dataModelName);
      }
      DataModel dm = dataModelStorage.readDataModel(dataModelType, fqName);
      if (dm == null) {
        throw new XFMG_NoSuchDataModelException(dataModelName);
      }
      GenerationBaseCache cache = new GenerationBaseCache();
      List<TypeInfo> typeInfos = new ArrayList<TypeInfo>();
      DataTypeXmlHelper dtxh = new DataTypeXmlHelper();
      for( XmomType xt : dm.getXmomTypes() ) {
        DOM dom = DOM.getOrCreateInstance(xt.getFqName(), cache, revision);
        dom.parseGeneration(true, false, false);
        if (dom.hasError()) {
          Throwable t = dom.getExceptionCause();
          if (t == null) {
            t = new RuntimeException("Unknown problem parsing " + xt.getFqName());
          }
          throw new TypeMapperCreationException(TypeMapperCreationFailure.Initialization, dataModelName, t);
        }
        TypeInfo ti = dtxh.toTypeInfo(dom);
        if( logger.isTraceEnabled() ) {
          logger.trace( xt.getFqName() +" -> "+ InformationUtils.xmomTypeToString(ti) );
        }
        typeInfos.add(ti);
      }
      return typeInfos;
    }

    public String writeDataModel(Map<String, Object> paramMap, XynaObject xynaObject) throws XmlCreationException {
      CreateXmlOptions createXmlOptions = CreateXmlOptions.create(paramMap);
      return tmc.createXmlFor(xynaObject, createXmlOptions);
    }

    public XynaObject parseDataModel(Map<String, Object> paramMap, String data) throws XynaObjectCreationException, XPRC_XmlParsingException {
      Document doc = XMLUtils.parseString(data);
      Element element = doc.getDocumentElement();
      return tmc.createXynaObjectForRootElement(element);
    }
 
  }

}
