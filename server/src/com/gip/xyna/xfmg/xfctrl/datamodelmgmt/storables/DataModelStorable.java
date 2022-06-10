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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel.DataModelColumn;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


/**
 *
 */
@Persistable(primaryKey = DataModelStorable.COL_FQNAME, tableName = DataModelStorable.TABLENAME)
public class DataModelStorable extends Storable<DataModelStorable> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "datamodel";

  public static final String COL_FQNAME = "fqname";
  public static final String COL_LABEL = "label";
  public static final String COL_BASETYPEFQNAME = "basetypefqname";
  public static final String COL_BASETYPELABEL = "basetypelabel";
  public static final String COL_DATAMODELTYPE = "datamodeltype";
  public static final String COL_DATAMODELPREFIX = "datamodelprefix";
  public static final String COL_VERSION = "version";
  public static final String COL_DOCUMENTATION = "documentation";
  public static final String COL_XMOMTYPECOUNT = "xmomTypeCount";
  public static final String COL_DEPLOYABLE = "deployable";
   
  @Column(name = COL_FQNAME)
  private String fqName; //datamodel instance name
  
  @Column(name = COL_LABEL)
  private String label;

  @Column(name = COL_BASETYPELABEL)
  private String baseTypeLabel;

  @Column(name = COL_BASETYPEFQNAME)
  private String baseTypeFqName;

  @Column(name = COL_DATAMODELTYPE)
  private String dataModelType;

  @Column(name = COL_DATAMODELPREFIX)
  private String dataModelPrefix;
 
  @Column(name = COL_VERSION)
  private String version;
  
  @Column(name = COL_DOCUMENTATION)
  private String documentation;

  @Column(name = COL_XMOMTYPECOUNT)
  private Integer xmomTypeCount;

  @Column(name = COL_DEPLOYABLE)
  private Boolean deployable;

  
  private List<DataModelSpecificStorable> parameters;
  

  public DataModelStorable() {
  }
  
  public DataModelStorable(String fqName) {
    this.fqName = fqName;
  }
  
  public DataModelStorable(DataModel dataModel) {
    this.fqName = dataModel.getType().getFqName();
    this.label = dataModel.getType().getLabel();
    this.baseTypeFqName = dataModel.getBaseType().getFqName();
    this.baseTypeLabel = dataModel.getBaseType().getLabel();
    this.dataModelType = dataModel.getDataModelType();
    this.dataModelPrefix = dataModel.getBaseType().getPath();
    this.version = dataModel.getVersion();
    this.documentation = dataModel.getDocumentation();
    this.xmomTypeCount = dataModel.getXmomTypeCount();
    this.deployable = dataModel.getDeployable();
    
  }

  @Override
  public ResultSetReader<? extends DataModelStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return fqName;
  }


  @Override
  public <U extends DataModelStorable> void setAllFieldsFromData(U data) {
    DataModelStorable dms = data;
    this.fqName = dms.fqName;
    this.label = dms.label;
    this.baseTypeFqName = dms.baseTypeFqName;
    this.baseTypeLabel = dms.baseTypeLabel;
    this.dataModelType = dms.dataModelType;
    this.dataModelPrefix = dms.dataModelPrefix;
    this.version = dms.version;
    this.documentation = dms.documentation;
    this.xmomTypeCount = dms.xmomTypeCount;
    this.deployable = dms.deployable;
  }
  
  @Override
  public String toString() {
    return "DataModelStorable("+fqName+")";
  }
  
  
  private static final ResultSetReader<DataModelStorable> reader =
      new ResultSetReader<DataModelStorable>() {

    public DataModelStorable read(ResultSet rs) throws SQLException {
      DataModelStorable dms = new DataModelStorable();
      
      dms.fqName = rs.getString(COL_FQNAME);
      dms.label = rs.getString(COL_LABEL);
      dms.baseTypeFqName = rs.getString(COL_BASETYPEFQNAME);
      dms.baseTypeLabel = rs.getString(COL_BASETYPELABEL);
      dms.dataModelType = rs.getString(COL_DATAMODELTYPE);
      dms.dataModelPrefix = rs.getString(COL_DATAMODELPREFIX);
      dms.version = rs.getString(COL_VERSION);
      dms.documentation = rs.getString(COL_DOCUMENTATION);
      dms.xmomTypeCount = rs.getInt(COL_XMOMTYPECOUNT);
      if( rs.wasNull() ) {
        dms.xmomTypeCount = null;
      }
      dms.deployable = rs.getBoolean(COL_DEPLOYABLE);
      return dms;
    }

  };

  public DataModel toDataModel( List<DataModelSpecific> dataModelSpecifics, List<XmomType> xmomTypes) {
    DataModel dm = toDataModel(dataModelSpecifics);
    dm.setXmomTypes(xmomTypes);
    return dm;
  }
  
  public DataModel toDataModel( List<DataModelSpecific> dataModelSpecifics) {
    return new DataModel.Builder().
        type( new XmomType().buildXmomType().label(label).fqTypeName(fqName).instance() ).
        baseType( new XmomType().buildXmomType().label(baseTypeLabel).fqTypeName(baseTypeFqName).instance() ).
        version(version).
        documentation(documentation).
        dataModelType(dataModelType).
        dataModelSpecifics(dataModelSpecifics).
        xmomTypeCount(xmomTypeCount).
        deployable(deployable).
        instance();
  }

  public String getFqName() {
    return fqName;
  }

  public void setFqName(String fqName) {
    this.fqName = fqName;
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
    
  public String getBaseTypeFqName() {
    return baseTypeFqName;
  }
  
  public void setBaseTypeFqName(String baseTypeFqName) {
    this.baseTypeFqName = baseTypeFqName;
  }
  
  public String getBaseTypeLabel() {
    return baseTypeLabel;
  }
  
  public void setBaseTypeLabel(String baseTypeLabel) {
    this.baseTypeLabel = baseTypeLabel;
  }
  
  public String getDataModelType() {
    return dataModelType;
  }
  
  public void setDataModelType(String dataModelType) {
    this.dataModelType = dataModelType;
  }
  
  public String getDataModelPrefix() {
    return dataModelPrefix;
  }
  
  public void setDataModelPrefix(String dataModelPrefix) {
    this.dataModelPrefix = dataModelPrefix;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  public String getDocumentation() {
    return documentation;
  }
  
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }
  
  public void setParameters(List<DataModelSpecificStorable> parameters) {
    this.parameters = parameters;
  }

  public List<DataModelSpecificStorable> getParameters() {
    return parameters;
  }
  
  public Boolean getDeployable() {
    return deployable;
  }
  
  public Integer getXmomTypeCount() {
    return xmomTypeCount;
  }
  
  public static class DynamicDataModelReader implements ResultSetReader<DataModelStorable> {

    private Set<DataModelColumn> selectedCols;

    public DynamicDataModelReader(Set<DataModelColumn> selected) {
      selectedCols = selected;
    }

    public DataModelStorable read(ResultSet rs) throws SQLException {
      DataModelStorable dms = new DataModelStorable();
      
      if(selectedCols.contains(DataModelColumn.FQNAME)) {
        dms.fqName = rs.getString(COL_FQNAME);
      }
      if(selectedCols.contains(DataModelColumn.LABEL)) {
        dms.label = rs.getString(COL_LABEL);
      }
      if(selectedCols.contains(DataModelColumn.BASETYPEFQNAME)) {
        dms.baseTypeFqName = rs.getString(COL_BASETYPEFQNAME);
      }
      if(selectedCols.contains(DataModelColumn.BASETYPELABEL)) {
        dms.baseTypeLabel = rs.getString(COL_BASETYPELABEL);
      }
      if(selectedCols.contains(DataModelColumn.DATAMODELTYPE)) {
        dms.dataModelType = rs.getString(COL_DATAMODELTYPE);
      }
      if(selectedCols.contains(DataModelColumn.DATAMODELPREFIX)) {
        dms.dataModelPrefix = rs.getString(COL_DATAMODELPREFIX);
      }
      if(selectedCols.contains(DataModelColumn.VERSION)) {
        dms.version = rs.getString(COL_VERSION);
      }
      if(selectedCols.contains(DataModelColumn.DOCUMENTATION)) {
        dms.documentation = rs.getString(COL_DOCUMENTATION);
      }    
      if(selectedCols.contains(DataModelColumn.XMOMTYPECOUNT)) {
        dms.xmomTypeCount = rs.getInt(COL_XMOMTYPECOUNT);
        if( rs.wasNull() ) {
          dms.xmomTypeCount = null;
        }
      }    
      if(selectedCols.contains(DataModelColumn.DEPLOYABLE)) {
        dms.deployable = rs.getBoolean(COL_DEPLOYABLE);
      }    
      
      return dms;
    }

  }

}
