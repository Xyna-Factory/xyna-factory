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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.collections.CollectionUtils.Join;
import com.gip.xyna.utils.collections.CollectionUtils.JoinType;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel.DataModelColumn;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelSpecificStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelTypeStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelXmomTypeStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.Path;


/**
 */
public class DataModelStorage {

  private static Logger logger = CentralFactoryLogging.getLogger(DataModelStorage.class);
  
  private ODSImpl ods;
  private PreparedQueryCache queryCache;
  private DataModelInfoCache dataModelInfoCache;

  private static final String QUERY_GET_DATA_MODEL_SPECIFIC_FOR_FQNAME = 
      "select * from "+DataModelSpecificStorable.TABLENAME
      +" where "+DataModelSpecificStorable.COL_FQNAME+"=?";
  private static final String QUERY_GET_DATA_MODEL_XMOM_TYPE_FOR_FQNAME = 
      "select * from "+DataModelXmomTypeStorable.TABLENAME
      +" where "+DataModelXmomTypeStorable.COL_FQNAME+"=?";
  private static final String QUERY_COUNT_DATA_MODEL_FOR_TYPE = 
      "select count(*) from "+DataModelStorable.TABLENAME
      +" where "+DataModelStorable.COL_DATAMODELTYPE+"=?";
  
  private static ResultSetReader<Integer> countReader = new ResultSetReader<Integer>() {
    public Integer read(ResultSet rs) throws SQLException {
      int count = rs.getInt(1);
      return count;
    }
  };  
  
  public DataModelStorage() throws PersistenceLayerException {
    
    ods = ODSImpl.getInstance();
    
    ods.registerStorable(DataModelTypeStorable.class);
    ods.registerStorable(DataModelStorable.class);
    ods.registerStorable(DataModelSpecificStorable.class);
    ods.registerStorable(DataModelXmomTypeStorable.class);
    
    queryCache = new PreparedQueryCache();
    
    dataModelInfoCache = new DataModelInfoCache(ods);
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }

  public Collection<DataModelTypeStorable> getAllDataModelTypes() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(DataModelTypeStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  public Collection<DataModelStorable> getAllDataModelStorables() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(DataModelStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  
  public void deleteDataModelType(String name) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelTypeStorable dmts = new DataModelTypeStorable(name);
      con.deleteOneRow(dmts);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  public void persistDataModelType(String name, String fqClassName, List<String> parameter) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelTypeStorable dmts = new DataModelTypeStorable(name,fqClassName,parameter);
      logger.info( "Persist "+dmts);
      con.persistObject(dmts);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
    
  public int countDataModels(String dataModelType) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    PreparedQuery<Integer> query =  queryCache.getQueryFromCache(QUERY_COUNT_DATA_MODEL_FOR_TYPE, con, countReader);
    Integer count = con.queryOneRow(query, new Parameter(dataModelType) );
    return count;
  }

  public List<DataModel> listDataModels(String dataModelType) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<DataModelStorable> dataModels = CollectionUtils.filter(con.loadCollection(DataModelStorable.class), new DataModelFilter(dataModelType) );
      
      return CollectionUtils.join(dataModels, 
                                  con.loadCollection(DataModelSpecificStorable.class),
                                  new DataModelJoin(),
                                  JoinType.LeftOuter );  
    } finally {
      finallyClose(con);
    }
  }
  

  private static class DataModelFilter implements Filter<DataModelStorable> {

    private String dataModelType;

    public DataModelFilter(String dataModelType) {
      this.dataModelType = dataModelType;
    }

    public boolean accept(DataModelStorable value) {
      if( dataModelType != null ) {
        return value.getDataModelType().equals(dataModelType);
      } else {
        return true;
      }
    }
    
  }

  
  private static class DataModelJoin implements Join<DataModelStorable, DataModelSpecificStorable, String, DataModel> {
    
    public String leftKey(DataModelStorable left) {
      return left.getFqName();
    }
    
    public String rightKey(DataModelSpecificStorable right) {
      return right.getFqName();
    }
    
    public DataModel join(String key, List<DataModelStorable> lefts, List<DataModelSpecificStorable> rights) {
      DataModelStorable dms = lefts.get(0); //andere Anzahlen kann es nicht geben, da key=PK ist
      return dms.toDataModel(DataModelSpecificStorable.toDataModelSpecifics(rights));
    }
  }
  
 

  public void addDataModel(DataModel dataModel) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelStorable dms = new DataModelStorable(dataModel);
      
      con.persistObject(dms);
      
      List<? extends DataModelSpecificStorable> dmss =
        DataModelSpecificStorable.toStorables(dms.getFqName(), dataModel.getDataModelSpecifics() );
      con.persistCollection(dmss);
      
      List<? extends DataModelXmomTypeStorable> dmxts =
        DataModelXmomTypeStorable.toStorables(dms.getFqName(), dataModel.getXmomTypes() );
      con.persistCollection(dmxts);
      
      dataModelInfoCache.add(dms);
      
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  public void removeDataModel(DataModel dataModel) throws PersistenceLayerException {
    //DataModel aus Path-Cache entfernen
    Path.removeDataModelFromCache(dataModel.getType().getFqName());
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      
      DataModelStorable dms = new DataModelStorable(dataModel); 
      dataModelInfoCache.remove(dms);
      
      con.deleteOneRow(dms);
      
      List<? extends DataModelSpecificStorable> dmss = getAllDataModelSpecificForFqName(con, dms.getFqName());
      con.delete(dmss);
     
      List<? extends DataModelXmomTypeStorable> dmxts = getAllDataModelXmomTypeForFqName(con, dms.getFqName());
      con.delete(dmxts);
      
      con.commit();
      
      //aus XMOMDatabase austragen
      XMOMDatabase xdb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      xdb.unregisterDataModel(dataModel.getType().getFqName());

    } finally {
      finallyClose(con);
    }
  }

  public DataModel readDataModel(String dataModelType, String dataModelName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelStorable dms = new DataModelStorable(dataModelName);
      con.queryOneRow(dms);
      if( ! dms.getDataModelType().equals(dataModelType) ) {
        return null;
      }
      List<? extends DataModelSpecificStorable> dmss = getAllDataModelSpecificForFqName(con, dms.getFqName());
      List<? extends DataModelXmomTypeStorable> dmxts = getAllDataModelXmomTypeForFqName(con, dms.getFqName());
      
      return dms.toDataModel(DataModelSpecificStorable.toDataModelSpecifics(dmss),
                             DataModelXmomTypeStorable.toXmomTypes(dmxts) );
    } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
      return null;
    } finally {
      finallyClose(con);
    }
  }
  
  
  public DataModel readDataModel(String fqName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelStorable dms = new DataModelStorable(fqName);
      con.queryOneRow(dms);
      List<? extends DataModelSpecificStorable> dmss = getAllDataModelSpecificForFqName(con, dms.getFqName());
      List<? extends DataModelXmomTypeStorable> dmos = getAllDataModelXmomTypeForFqName(con, dms.getFqName());
      return dms.toDataModel(DataModelSpecificStorable.toDataModelSpecifics(dmss),
                             DataModelXmomTypeStorable.toXmomTypes(dmos));
    } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
      return null;
    } finally {
      finallyClose(con);
    }
  }

  public String getDataModelType(String dataModelName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelStorable dms = new DataModelStorable(dataModelName);
      con.queryOneRow(dms);
      return dms.getDataModelType();
    } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
      return null;
    } finally {
      finallyClose(con);
    }
  }

  private List<DataModelSpecificStorable> getAllDataModelSpecificForFqName(ODSConnection con, String fqName) throws PersistenceLayerException {
    PreparedQuery<DataModelSpecificStorable> query = 
        queryCache.getQueryFromCache(QUERY_GET_DATA_MODEL_SPECIFIC_FOR_FQNAME, con, DataModelSpecificStorable.reader);
    return con.query(query, new Parameter(fqName), -1);
  }
  
  private List<DataModelXmomTypeStorable> getAllDataModelXmomTypeForFqName(ODSConnection con, String fqName) throws PersistenceLayerException {
    PreparedQuery<DataModelXmomTypeStorable> query = 
        queryCache.getQueryFromCache(QUERY_GET_DATA_MODEL_XMOM_TYPE_FOR_FQNAME, con, DataModelXmomTypeStorable.reader);
    return con.query(query, new Parameter(fqName), -1);
  }

  public List<File> readFiles(String basePath, String fqName) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DataModelStorable dms = new DataModelStorable(fqName);
      con.queryOneRow(dms); //nur überprüfen, ob Datenmodell existiert
      return DataModelXmomTypeStorable.toFiles(basePath, getAllDataModelXmomTypeForFqName(con, fqName) );
    } finally {
      finallyClose(con);
    }
  }

  /**
   * @param searchRequest
   * @return
   * @throws PersistenceLayerException 
   * @throws XNWH_SelectParserException 
   * @throws XNWH_InvalidSelectStatementException 
   */
  public SearchResult<DataModelStorable> search(SearchRequestBean searchRequest) throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException {
    SearchResult<DataModelStorable> result = new SearchResult<DataModelStorable>();   
    Selection selection = SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<DataModelStorable> query = queryCache.getQueryFromCache(selection.getSelectString(), con, selection.getReader(DataModelStorable.class) );
      List<DataModelStorable> dataModels = con.query(query, selection.getParameter(), searchRequest.getMaxRows() );
      result.setResult(dataModels);
      if (dataModels.size() >= searchRequest.getMaxRows() ) {
        PreparedQuery<Integer> queryCount = queryCache.getQueryFromCache(selection.getSelectCountString(), con, countReader);
        result.setCount( con.queryOneRow(queryCount, selection.getParameter()) );
      } else {
        result.setCount( dataModels.size() );
      }
      if( selection.containsColumn(DataModelColumn.PARAMETER) ) {
        for( DataModelStorable dms : dataModels ) {
          dms.setParameters(getAllDataModelSpecificForFqName(con, dms.getFqName() ) );
        }
      }
    } finally {
      finallyClose(con);
    }
    return result;
  }

  /**
   * Löscht oldDms, trägt newDms neu ein
   * @param dataModel
   * @param oldDms
   * @param newDms
   * @throws PersistenceLayerException
   */
  public void replaceDataModelSpecifics(DataModel dataModel, List<DataModelSpecific> oldDms, List<DataModelSpecific> newDms) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      String fqName = dataModel.getType().getFqName();
      con.delete( DataModelSpecificStorable.toStorables(fqName, oldDms) );
      con.persistCollection( DataModelSpecificStorable.toStorables(fqName, newDms) );
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  public List<String> deleteDataModelSpecifics(String keyPrefix, String value) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      
      DataModelSpecificStorableFilter filter = new DataModelSpecificStorableFilter(keyPrefix,value);
      Collection<DataModelSpecificStorable> allDmss = con.loadCollection(DataModelSpecificStorable.class);
      List<DataModelSpecificStorable> dmsss = CollectionUtils.filter(allDmss, filter);
      con.delete(dmsss);
      con.commit();
      return CollectionUtils.transform(dmsss, new GetFqNameFromDataModelSpecificStorable() );
    } finally {
      finallyClose(con);
    }
  }

  private static class DataModelSpecificStorableFilter implements Filter<DataModelSpecificStorable> {

    private String keyPrefix;
    private String value;

    public DataModelSpecificStorableFilter(String keyPrefix, String value) {
      this.keyPrefix = keyPrefix;
      this.value = value;
    }

    public boolean accept(DataModelSpecificStorable dmss) {
      if (dmss.getValue() == null || !dmss.getValue().equals(value)) {
        return false;
      }
      if (dmss.getKey() == null || !dmss.getKey().startsWith(keyPrefix)) {
        return false;
      }
      return true;
    }
    
  }

  private static class GetFqNameFromDataModelSpecificStorable implements Transformation<DataModelSpecificStorable, String> {
    public String transform(DataModelSpecificStorable from) {
      return from.getFqName();
    }
  }

  
  public boolean isDataModelDeployable(String fqName) {
    return dataModelInfoCache.isDeployable(fqName); 
  }

  public String getFqName(String dataModelType, String version, String label) {
    return dataModelInfoCache.getFqName(dataModelType, version, label);
  }

  private static class DataModelInfoCache {
    
    private ConcurrentHashMap<String,DataModelInfo> allDataModelInfo;
    private ConcurrentHashMap<String,String> typeLabelToFqName;
    
    private static class DataModelInfo {
      
      boolean deployable;
      String dataModelType;
      
      public DataModelInfo(DataModelStorable dms) {
        this.deployable = dms.getDeployable() == null ? false : dms.getDeployable();
        this.dataModelType = dms.getDataModelType();
      }
      
      @Override
      public String toString() {
        return "DataModelInfo("+dataModelType+","+deployable+")";
      }
     
    }

    public DataModelInfoCache(ODSImpl ods)  throws PersistenceLayerException {
      allDataModelInfo = new ConcurrentHashMap<String,DataModelInfo>();
      typeLabelToFqName = new ConcurrentHashMap<String,String>();
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        for( DataModelStorable dms : con.loadCollection(DataModelStorable.class) ) {
          add(dms);
        }
      } finally {
        finallyClose(con);
      }
    }
    

    public void add(DataModelStorable dms) {
      typeLabelToFqName.put( createKey(dms.getDataModelType(), dms.getVersion(), dms.getLabel()), dms.getFqName() );
      allDataModelInfo.put( dms.getFqName(), new DataModelInfo(dms) );
    }

    public void remove(DataModelStorable dms) {
      typeLabelToFqName.remove( createKey(dms.getDataModelType(), dms.getVersion(), dms.getLabel()) );
      allDataModelInfo.remove( dms.getFqName() );
    }

    
    private String createKey(String type, String version, String label) {
      return type+":"+version+":"+label;
    }

    public String getFqName(String dataModelType, String version, String label) {
      return typeLabelToFqName.get(createKey(dataModelType,version, label));
    }

    public boolean isDeployable(String fqName) {
      DataModelInfo dmi = allDataModelInfo.get(fqName);
      if( dmi == null ) {
        return false;
      }
      return dmi.deployable;
    }

    
  }

}
