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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsmiparser.smi.SmiMib;
import org.jsmiparser.smi.SmiModule;

import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
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
public class MIBImport {

  private SmiMib mib;
  private OIDMap<SmiModule> moduleMap;
  private ImportParameter parameter;
  
  private Set<OID> completeModules = new HashSet<OID>();
  private Map<OID,List<OID>> incompleteModules = new HashMap<OID,List<OID>>();

  private Map<String,MIBModuleImport> mibModuleImports;
  private List<DataModel> alreadyImportedDataModels;
  private DataModelResult dataModelResult;
  
  public MIBImport(DataModelResult dataModelResult, MIBReader mibReader, ImportParameter parameter) {
    this.dataModelResult = dataModelResult;
    this.mib = mibReader.getMib();
    this.moduleMap = mibReader.getModuleMap();
    this.parameter = parameter;
  }

  public void selectModules() {
    if( parameter.hasOidRestrictions() ) {
      selectModules( parameter.getOidRestrictions() );
    } else {
      completeModules.addAll(moduleMap.keySet());
    }
    mibModuleImports = new HashMap<String,MIBModuleImport>();
    for( OID oid : completeModules ) {
      MIBModuleImport mmi = new MIBModuleImport( moduleMap.get(oid), parameter, null );
      mibModuleImports.put( mmi.getDataModelName(), mmi );
    }
    for( Map.Entry<OID,List<OID>> entry : incompleteModules.entrySet() ) {
      SmiModule module = moduleMap.get(entry.getKey());
      if( module == null ) {
        dataModelResult.warn("Could not find "+entry.getKey()+" in all known modules");
      } else {
        MIBModuleImport mmi = new MIBModuleImport( module, parameter, entry.getValue() );
        mibModuleImports.put( mmi.getDataModelName(), mmi );
      }
    }
  }
  
  private void selectModules(List<String> oidRestrictions) {
    for( String oidRestriction : oidRestrictions ) {
      String oidStr = MIBTools.getOid(mib,oidRestriction);
      if( oidStr == null ) {
        dataModelResult.warn("Could not resolve oidRestriction "+oidRestriction);
        continue;
      }
      OID oid = new OID(oidStr);
      completeModules.addAll( moduleMap.getChildren(oid, true) );
      SmiModule module = moduleMap.get(oid);
      if( module != null ) {
        completeModules.add(oid);
      } else {
        OID parent = moduleMap.getParent(oid);
        List<OID> branches = incompleteModules.get(parent);
        if( branches == null ) {
          branches = new ArrayList<OID>();
          incompleteModules.put( parent, branches );
        }
        branches.add(oid);
      }
    }
    for( OID complete : completeModules ) {
      incompleteModules.remove(complete);
    }
  }

  public List<String> getSelectedModules() {
    List<String> selected = new ArrayList<String>();
    for( OID oid : moduleMap.keyList() ) {
      if( completeModules.contains(oid) ) {
        selected.add("Complete " +moduleMap.get(oid).getId() );
      }
      if( incompleteModules.containsKey(oid) ) {
        selected.add("Partially " +moduleMap.get(oid).getId() + " starting at " + incompleteModules.get(oid) );
      }
    }
    if( selected.isEmpty() ) {
      selected.add("No module was selected" );
    }
    return selected;
  }
  
  public Pair<Boolean,List<String>> checkImportAllowed(List<DataModel> allExistingDataModels) {
    alreadyImportedDataModels = 
      CollectionUtils.filter(allExistingDataModels, new ExistingFilter(mibModuleImports.keySet() ) );
    if( alreadyImportedDataModels.isEmpty() ) {
      return Pair.of(Boolean.TRUE,null); 
    }
    if( parameter.getOverwrite() ) {
      return Pair.of(Boolean.TRUE,null); 
    }
    //Fehlerfall: bestehende DataModels dürfen nicht überschrieben werden
    List<String> importNotAllowed = new ArrayList<String>();
    for( DataModel dm : alreadyImportedDataModels ) {
      importNotAllowed.add( dm.getType().getFqName() );
    }
    return Pair.of(Boolean.FALSE,importNotAllowed);
  }

  public List<DataModel> getAlreadyImportedDataModels() {
    return alreadyImportedDataModels;
  }
  
  private static class ExistingFilter implements Filter<DataModel> {

    private Set<String> fqNames;

    public ExistingFilter(Set<String> fqNames) {
      this.fqNames = fqNames;
    }

    public boolean accept(DataModel value) {
      String fqName = value.getType().getFqName();
      return fqNames.contains(fqName);
    }
    
  }

  
  public void createModuleDataTypes() {
    for( MIBModuleImport mmi : mibModuleImports.values() ) {
      mmi.createDataTypes();
    }
  }

  public void saveDataModelsAndDataTypes(DataModelResult dmr, DataModelStorage dataModelStorage) throws PersistenceLayerException, Ex_FileWriteException, XFMG_NoSuchRevision {
    for( MIBModuleImport mmi : mibModuleImports.values() ) {
      DataModel dataModel = mmi.getDataModel();
      
      XmomGenerator xmomGenerator = XmomGenerator.inRevision(RevisionManagement.REVISION_DATAMODEL).
          overwrite(parameter.getOverwrite()).build();
      for( Datatype dt : mmi.getDataTypes() ) {
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
      
      dmr.savedDataModel(mmi.getModuleName(), dataModel.getXmomTypeCount() );
    }
  }

  public Collection<MIBModuleImport> getMibModuleImports() {
    return mibModuleImports.values();
  }


  
}
