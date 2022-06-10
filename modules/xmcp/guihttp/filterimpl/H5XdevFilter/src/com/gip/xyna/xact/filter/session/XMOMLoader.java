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
package com.gip.xyna.xact.filter.session;

import java.util.Optional;

import org.w3c.dom.Document;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.cache.ClassIdentityGenerationBaseCache;
import com.gip.xyna.xact.filter.util.WorkflowUtils;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/**
 * XMOMLoader lädt XMOM-Objekte und Verwaltet die geparsten GenerationBase-Objekte, 
 * so dass diese nicht mehrfach geparst werden müssen.
 *
 */
public class XMOMLoader {

  private ClassIdentityGenerationBaseCache cache;
  private static final String anyTypeFqn = GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME;
  
  public XMOMLoader() {
    cache = new ClassIdentityGenerationBaseCache();
  }
  
  /**
   * Object anhand des XML laden und aktualisieren
   * @param fqName2 
   * @param xml
   * @return
   * @throws XynaException
   */
  public GenerationBaseObject load(FQName fqName, String xml) throws XynaException {
    /*
     * falls das objekt im commoncache bereits existiert, können auch andere xmomobjekte referenzen darauf haben.
     * alle referenzen sollen auch geupdated werden.
     * 
     * => auf das bestehende gb objekt erneut parsexml aufrufen mit dem übergebenen xml
     */
    GenerationBase.XMLInputSource inputSource = new GenerationBase.XMLInputSource() {
      
      GenerationBase.XMLInputSource fromSaved = new GenerationBase.XMLInputSourceFromFileSystem();

      public Document getOrParseXML(GenerationBase obj, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
        if (obj.getOriginalFqName().equals(fqName.getFqName())) {
          return XMLUtils.parseString(xml, true);
        }
        return fromSaved.getOrParseXML(obj, fileFromDeploymentLocation);
      }
      
    };
    GenerationBase gb = GenerationBase.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision(), inputSource);
    //für dieses objekt das gegebene xml verwenden, andere objekte falls notwendig aus saved laden
    gb.setXMLInputSource(inputSource);
    gb.resetState();
    gb.parseGeneration(false, false);
    return createGBO(fqName, gb);
  }
  
  protected GenerationBaseObject createGBO(FQName fqName, GenerationBase gb) {
    GenerationBaseObject gbo = new GenerationBaseObject(fqName, gb, this);
    if (gbo.getType() == XMOMType.WORKFLOW) {
      ScopeStep mainStep = gbo.getWorkflow().getWfAsStep();
      WorkflowUtils.prepareWorkflow(mainStep);
    }
    return gbo;
  }

  
  /**
   * aus commonCache holen oder neu generieren
   * @param fqName
   * @return
   */
  public GenerationBaseObject load(FQName fqName, boolean readOnly) throws XynaException {
    GenerationBase gb;
    
    //if readOnly, try cache first
    if (readOnly && H5XdevFilter.USE_CACHE.get()) {
      gb = cache.getFromCache(fqName.getFqName(), fqName.getRevision());
      if (gb != null) {
        return createGBO(fqName, gb);
      }
    }
    
    gb = loadNewGB(fqName);
    return createGBO(fqName, gb);
  }
  

  public GenerationBaseObject loadNoCacheChange(FQName fqName) throws XynaException {
    GenerationBase gb;

    //try cache first - but do not update it
    if (H5XdevFilter.USE_CACHE.get()) {
      Optional<GenerationBase> ogb = cache.getFromCacheNoInsert(fqName.getFqName(), fqName.getRevision());
      if (ogb.isPresent()) {
        return createGBO(fqName, ogb.get());
      }
    }

    gb = loadNewGB(fqName);
    return createGBO(fqName, gb);
  }
  
  public static GenerationBase loadNewGB(FQName fqName) throws XynaException{
    GenerationBase gb;
    try {
      gb = GenerationBase.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision() );
      gb.parseGeneration(false/*saved*/, false, false);
  
      return gb;
    } catch (Ex_FileAccessException ex) {
      if(anyTypeFqn.equals(fqName.getFqName())) { // TODO PMOD-149
        return null;
      } else {
        throw ex;
      }
    } catch (XynaException ex) {
      throw ex;
    }
  }

  public GenerationBaseObject createNewObject(ObjectIdentifierJson object) throws XPRC_InvalidPackageNameException, XFMG_NoSuchRevision {
    RuntimeContext runtimeContext = object.getRuntimeContext().toRuntimeContext();
    String fqNameString = object.getFQName().getTypePath()+"."+object.getFQName().getTypeName();
    FQName fqName = new FQName(runtimeContext,fqNameString); 
    switch( object.getType() ) {
    case dataType:
      DOM dom = DOM.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision());
      dom.createEmptyDT(object.getLabel());
      GenerationBaseObject gbo = new GenerationBaseObject(fqName, dom, this, true);
      gbo.setViewType(Type.dataType);
      return gbo;
    case serviceGroup:
      dom = DOM.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision());
      dom.createEmptySG(object.getLabel());
      gbo = new GenerationBaseObject(fqName, dom, this, true);
      gbo.setViewType(Type.serviceGroup);
      return gbo;
    case exceptionType:
      ExceptionGeneration exceptionGeneration = ExceptionGeneration.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision());
      exceptionGeneration.createEmpty(object.getLabel());
      return new GenerationBaseObject(fqName, exceptionGeneration, this, true);
    case workflow:
      return createNewWorkflow(object.getLabel(), fqName);
    default:
      break;
    }
    throw new UnsupportedOperationException("Cannot create GenerationBaseObject for type "+object.getType() );
  }
  

  public GenerationBaseObject createNewWorkflow(String label, FQName fqName) throws XPRC_InvalidPackageNameException {
    WF wf = WF.createNewWorkflow(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision());
    wf.setLabel(label);
    return new GenerationBaseObject(fqName, wf, this, true);
  }

  public void prepareModification(GenerationBaseObject gbo) {
  }

}
