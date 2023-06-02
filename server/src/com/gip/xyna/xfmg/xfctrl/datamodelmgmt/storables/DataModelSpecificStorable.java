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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


/**
 *
 */
@Persistable(primaryKey = DataModelSpecificStorable.COL_FQNAMEINDEX, tableName = DataModelSpecificStorable.TABLENAME)
public class DataModelSpecificStorable extends Storable<DataModelSpecificStorable> implements Comparable<DataModelSpecificStorable> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "datamodelspecific";

  public static final String COL_FQNAMEINDEX = "fqnameIndex";
  public static final String COL_FQNAME = "fqname";
  public static final String COL_KEY = "key";
  public static final String COL_VALUE = "value";
  public static final String COL_LABEL = "label";
   
   
  @Column(name = COL_FQNAMEINDEX)
  private String fqNameIndex; //fqName+"_"+key
  
  @Column(name = COL_FQNAME)
  private String fqName;
    
  @Column(name = COL_KEY)
  private String key;

  @Column(name = COL_VALUE)
  private String value;
  
  @Column(name = COL_LABEL)
  private String label;
  
  public DataModelSpecificStorable() {
  }
  
  public DataModelSpecificStorable(String fqName, String key, String value, String label) {
    this.fqNameIndex = fqName+"_"+key;
    this.fqName = fqName;
    this.key = key;
    this.value = value;
    this.label = label;
  }

  @Override
  public ResultSetReader<? extends DataModelSpecificStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return fqNameIndex;
  }


  @Override
  public <U extends DataModelSpecificStorable> void setAllFieldsFromData(U data) {
    DataModelSpecificStorable dmts = data;
    this.fqNameIndex = dmts.fqNameIndex;
    this.fqName = dmts.fqName;
    this.key = dmts.key;
    this.value = dmts.value;
    this.label = dmts.label;
  }
  
  @Override
  public String toString() {
    return "DataModelSpecificStorable("+fqName+","+label+"->"+value+")";
  }
  
  public int compareTo(DataModelSpecificStorable o) {
    int c = 0;
    if( key != null ) {
      c = key.compareTo(o.key);
    }
    if( c == 0 ) {
      c = fqName.compareTo(o.fqName);
    }
    return c;
  }

  
  public static final ResultSetReader<DataModelSpecificStorable> reader =
      new ResultSetReader<DataModelSpecificStorable>() {

    public DataModelSpecificStorable read(ResultSet rs) throws SQLException {
      DataModelSpecificStorable dmts = new DataModelSpecificStorable();
      dmts.fqNameIndex = rs.getString(COL_FQNAMEINDEX);
      dmts.fqName = rs.getString(COL_FQNAME);
      dmts.key = rs.getString(COL_KEY);
      dmts.value = rs.getString(COL_VALUE);
      dmts.label = rs.getString(COL_LABEL);
      return dmts;
    }

  };

  public String getFqNameIndex() {
    return fqNameIndex;
  }
  
  public void setFqNameIndex(String fqNameIndex) {
    this.fqNameIndex = fqNameIndex;
  }
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String key) {
    this.key = key;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
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

  public DataModelSpecific toDataModelSpecific() {
    return new DataModelSpecific(key,value,label);
  }
  
  public static List<DataModelSpecific> toDataModelSpecifics(List<? extends DataModelSpecificStorable> list) {
    ArrayList<DataModelSpecific> dataModelSpecifics = null; 
    if( list != null ) {
      Collections.sort(list);
      dataModelSpecifics = new ArrayList<DataModelSpecific>(list.size());
      for( DataModelSpecificStorable dmss : list ) {
        dataModelSpecifics.add( dmss.toDataModelSpecific() );
      }
    }
    return dataModelSpecifics;
  }

  public static List<DataModelSpecificStorable> toStorables(String fqName, List<? extends DataModelSpecific> dataModelSpecifics) {
    if( dataModelSpecifics == null || dataModelSpecifics.isEmpty() ) {
      return Collections.emptyList();
    }
    List<DataModelSpecificStorable> specifics = new ArrayList<DataModelSpecificStorable>();
    for( DataModelSpecific dms : dataModelSpecifics ) {
      specifics.add( new DataModelSpecificStorable( fqName, dms.getKey(), dms.getValue(), dms.getLabel() ) );
    }
    return specifics;
  }

}
