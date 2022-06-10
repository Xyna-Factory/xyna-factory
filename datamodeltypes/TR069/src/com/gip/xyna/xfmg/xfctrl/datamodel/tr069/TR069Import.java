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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;


/**
 *
 */
public class TR069Import {


  private TR069Reader tr069Reader;
  private ImportParameter parameter;
  private Map<String, TRModel> modelMap;
  private Map<String,TR069ModelImport> modelImports;
  private List<DataModel> alreadyImportedDataModels;
   
  public TR069Import(TR069Reader tr069Reader, ImportParameter parameter) {
    this.tr069Reader = tr069Reader;
    this.modelMap = tr069Reader.getModelMap();
    this.parameter = parameter;
  }

  /**
   * 
   */
  public void selectModels() {
    modelImports = new HashMap<String,TR069ModelImport>();
    
    if( parameter.hasModelRestrictions() ) {
      for( String modelRestriction : parameter.getModelRestrictions() ) {
        //TODO weitere Einschränkung
        TRModel model = modelMap.get(modelRestriction);
        if( model == null ) {
          //TODO Fehler
        } else {
          modelImports.put( modelRestriction, new TR069ModelImport(model, parameter ) );
        }
      }
    } else {
      //alle
      for( Map.Entry<String, TRModel> entry : modelMap.entrySet() ) {
        modelImports.put( entry.getKey(), new TR069ModelImport(entry.getValue(), parameter ) );
      }
    }
    
    //Sonderbehandlung für Services:
    // TODO: Services should be generated with a Services Basetype and a list of themself
    /*if( modelImports.containsKey("VoiceService:1.0") ) {
      prepareServiceImport("VoiceService:1.0");
    }
    if( modelImports.containsKey("STBService:1.0") ) {
      prepareServiceImport("STBService:1.0");
    }*/
  }


  /**
   * @param string
   */
  /*private void prepareServiceImport(String service) {
    TR069ModelImport tmi = modelImports.remove(service);
    TRModel modelService = tmi.getModel();
    
    TRModel modelIGD = createServiceModel(modelService, "InternetGatewayDevice");
    modelImports.put( modelIGD.getFqName(), 
                      new TR069ModelImport(modelIGD, parameter, 
                                           tmi.getLabel()+" for InternetGatewayDevice" ) );
    
    TRModel modelD = createServiceModel(modelService, "Device");
    modelImports.put( modelD.getFqName(), 
                      new TR069ModelImport(modelD, parameter, 
                                           tmi.getLabel()+" for Device" ) );
    
  }*/

  /**
   * @param modelService
   * @return
   */
  /*private TRModel createServiceModel(TRModel modelService, String baseName) {
    TRModel model = new TRModel(modelService.getDocument(), modelService);
                                
    model.migrateFqName( baseName+"_"+modelService.getFqName() );
    
    TRObject igd = new TRObject(model, baseName+".");
    if( baseName.equals("Device") ) {
      igd.setDescription( "The top-level object for a Device." );
    } else {
      igd.setDescription( "The top-level object for an Internet Gateway Device." );
    }
    
    TRObject services = new TRObject(model, baseName+".Services.");
    services.setDescription( "This object contains general services information." );
    igd.addChild(services);
    
    model.addObject(igd);
    model.addObject(services);
    for( TRObject root : modelService.getRootObjects() ) {
      model.adoptRoot( root.getFqName(), services);
    }
    //model.treeify(dataModelResult); TODO
    
    tr069Reader.addModel(model);
    return model;
  }*/

  public List<String> getSelectedModels() {
    //TODO schöner mit Ausgabe ob partiell
    return new ArrayList<String>(modelImports.keySet());
  }

  /**
   * @param allExistingDataModels
   * @return
   */
  public Pair<Boolean, List<String>> checkImportAllowed(List<DataModel> allExistingDataModels) throws PersistenceLayerException {
    alreadyImportedDataModels = new ArrayList<DataModel>();
    return Pair.of(Boolean.TRUE, null); //FIXME
  }

  /**
   * 
   */
  public void createModelDataTypes() {
    for( TR069ModelImport tmi : modelImports.values() ) {
      tmi.createDataTypes();
    }
  }

  /**
   * @return
   */
  public List<DataModel> getAlreadyImportedDataModels() {
    return alreadyImportedDataModels;
  }


  public void saveDataModelsAndDataTypes(DataModelResult dataModelResult, DataModelStorage dataModelStorage) throws PersistenceLayerException, XFMG_NoSuchRevision, Ex_FileWriteException {
    for( TR069ModelImport tmi : modelImports.values() ) {
      DataModel dataModel = tmi.getDataModel();
      
      XmomGenerator xmomGenerator = XmomGenerator.inRevision(RevisionManagement.REVISION_DATAMODEL).
          overwrite(parameter.getOverwrite()).build();
      for( Datatype dt : tmi.getDataTypes() ) {
        xmomGenerator.add(dt);
      }
      xmomGenerator.save();
      
      List<XmomType> xts = new ArrayList<XmomType>();
      for (Datatype datatype : xmomGenerator.getDatatypes() ) {
        xts.add( new XmomType(datatype.getType()) );
      }
      dataModel.setXmomTypes(xts);
      dataModel.setXmomTypeCount(xts.size());
      
      dataModelStorage.addDataModel(dataModel);

      dataModelResult.savedDataModel(tmi.getModelName(), dataModel.getXmomTypeCount() );
    }
  }

  /**
   * @return
   */
  public Collection<TR069ModelImport> getTR069ModelImports() {
    return modelImports.values();
  }

  
  public TR069Reader getTr069Reader() {
    return tr069Reader;
  }
}
