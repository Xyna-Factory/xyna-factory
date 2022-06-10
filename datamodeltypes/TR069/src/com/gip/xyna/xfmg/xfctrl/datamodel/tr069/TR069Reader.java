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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRDataModelDefinition;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;


/**
 *
 */
public class TR069Reader implements DataModelParseContext {

  private static Logger logger = CentralFactoryLogging.getLogger(TR069Reader.class);
  private DataModelResult dataModelResult;
  private Map<String,TRDataModelDefinition> documents;
  private Map<String,TRModel> models;
  private List<TRDataModelDefinition> sortedDocuments;

  public TR069Reader(DataModelResult dataModelResult) {
    this.dataModelResult = dataModelResult;
    this.documents = new HashMap<String,TRDataModelDefinition>();
    this.models = new HashMap<String,TRModel>();
  }
  

  public void importFiles(List<String> files) throws XPRC_XmlParsingException, Ex_FileAccessException  {
    for( String filename : files ) {
      importRecursively( new File(filename) );
    }
  }

  private void importRecursively(File file) throws XPRC_XmlParsingException, Ex_FileAccessException {
    if( file.isDirectory() ) {
      for( File f : file.listFiles( TRDataModelDefinition.getDataDefinitionAcceptor() ) ) {
        importRecursively(f);
      }
    } else {
      TRDataModelDefinition modelDef = TRDataModelDefinition.instantiateModelDefinition(file);
      documents.put(modelDef.getName(), modelDef);
    }
  }
  
   
  /**
   * 
   */
  public void parse() {
    for( TRDataModelDefinition document : documents.values() ) {
      document.parseImports();
    }
    
    //richtige Import-Reihenfolge beachten
    sortedDocuments = sortDocumentImportOrder(documents.values());
    
    dataModelResult.info("Importing Documents in order "+sortedDocuments);
    if( logger.isDebugEnabled() ) {
      logger.debug( documents );
    }
    for( TRDataModelDefinition document : sortedDocuments ) {
      document.parse(this);
      for( TRModel model : document.getModels() ) {
        models.put( model.getFqName(), model);
      }
    }
  }
  

  private List<TRDataModelDefinition> sortDocumentImportOrder(Collection<TRDataModelDefinition> documents) {
    ArrayList<TRDataModelDefinition> sorted = new ArrayList<TRDataModelDefinition>();
    HashSet<String> alreadySorted = new HashSet<String>();
    for( TRDataModelDefinition document : documents ) {
      sortDocumentImportOrder( sorted, alreadySorted, document );
    }
    return sorted;
  }

  private void sortDocumentImportOrder(List<TRDataModelDefinition> sorted,
                                       Set<String> alreadySorted,
                                       TRDataModelDefinition document) {
    String name = document.getName();
    if( alreadySorted.contains(name) ) {
      return;
    }
    for( String ref : document.getImportReferences() ) {
      TRDataModelDefinition impDoc = documents.get(ref);
      if( impDoc == null ) {
        impDoc = searchSurrogateDocument(ref);
      }
      if( impDoc != null ) {
        sortDocumentImportOrder(sorted, alreadySorted, impDoc);
      } else {
        dataModelResult.warn("Missing document "+ref);
      }
    }
    sorted.add(document);
    alreadySorted.add(name);
  }

  public TRDataModelDefinition searchSurrogateDocument(String ref) {
    List<String> possibleDocs = new ArrayList<String>();
    for( String pos : documents.keySet() ) {
      if( ref.startsWith(pos) || pos.startsWith(ref) ) {
        possibleDocs.add(pos);
      }
    }
    String pos = null;
    int max = 0;
    for( String p : possibleDocs ) {
      int m = 0;
      if( ref.startsWith(p) ) {
        m = p.length();
      } else if( p.startsWith(ref) ) {
        m = ref.length();
      }
      if( m > max ) {
        pos = p;
        max = m;
      }
    }
    if( pos != null ) {
      dataModelResult.warn("Using "+pos+" as surrogate for missing "+ref);
      return documents.get(pos);
    }
    return null;
  }

  
  
  public Map<String, TRModel> getModelMap() {
    return models;
  }

  public List<String> getAllModels() {
    List<String> models = new ArrayList<String>();
    for( TRModel model : this.models.values() ) {
      models.add( model.getFqName()+" from "+model.getDocument().getFileName() );
    }
    Collections.sort(models);
    return models;
  }


  public TRModel getModel(String name) {
    return models.get(name);
  }

  public List<TRDataModelDefinition> getDocuments() {
    return sortedDocuments;
  }

  public void addModel(TRModel model) {
    models.put(model.getFqName(), model);
  }
  
  


  public TRDataModelDefinition getDocument(String ref) {
    return documents.get(ref);
  }


  public void warn(String warning) {
    dataModelResult.warn(warning);
  }


  public void trace(String message) {
    if (logger.isTraceEnabled()) {
      logger.trace(message);
    }
  }
  

  

}
