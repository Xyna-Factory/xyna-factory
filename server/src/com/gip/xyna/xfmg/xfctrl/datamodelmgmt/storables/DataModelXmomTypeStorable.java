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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.misc.StringReplacer;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


/**
 *
 */
@Persistable(primaryKey = DataModelXmomTypeStorable.COL_FQNAMEINDEX, tableName = DataModelXmomTypeStorable.TABLENAME)
public class DataModelXmomTypeStorable extends Storable<DataModelXmomTypeStorable> implements Comparable<DataModelXmomTypeStorable> {
  
  
  private static final StringReplacer dotToFileSeparator = StringReplacer.replace('.', Constants.fileSeparator).build();
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "datamodelxmomtype";

  public static final String COL_FQNAMEINDEX = "fqnameIndex";
  public static final String COL_FQNAME = "fqname";
  public static final String COL_PATH = "path";
  public static final String COL_NAME = "name";
  public static final String COL_LABEL = "label";
   
   
  @Column(name = COL_FQNAMEINDEX)
  private String fqNameIndex; //fqName+"_"+index
  
  @Column(name = COL_FQNAME)
  private String fqName; //datamodel-name
  
  @Column(name = COL_PATH)
  private String path; //xmom path
  
  @Column(name = COL_NAME)
  private String name; //xmom name
  
  @Column(name = COL_LABEL)
  private String label;

  private int index;
  
  public DataModelXmomTypeStorable() {
  }
  
  public DataModelXmomTypeStorable(String fqName, int index, XmomType xmomType) {
    this.fqNameIndex = fqName+"_"+index;
    this.fqName = fqName;
    this.index = index;
    this.path = xmomType.getPath();
    this.name = xmomType.getName();
    this.label = xmomType.getLabel();
  }

  @Override
  public ResultSetReader<? extends DataModelXmomTypeStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return fqNameIndex;
  }


  @Override
  public <U extends DataModelXmomTypeStorable> void setAllFieldsFromData(U data) {
    DataModelXmomTypeStorable dmts = data;
    this.fqNameIndex = dmts.fqNameIndex;
    this.fqName = dmts.fqName;
    this.path = dmts.path;
    this.name = dmts.name;
    this.label = dmts.label;
    this.index = Integer.parseInt(fqNameIndex.substring(fqName.length()+1));
  }
  
  @Override
  public String toString() {
    return "DataModelSpecificStorable("+fqName+","+path+"."+name+","+label+")";
  }
  
  public int compareTo(DataModelXmomTypeStorable o) {
    int c = index-o.index;
    if( c == 0 ) {
      c = fqName.compareTo(o.fqName);
    }
    return c;
  }

  
  public static final ResultSetReader<DataModelXmomTypeStorable> reader =
      new ResultSetReader<DataModelXmomTypeStorable>() {

    public DataModelXmomTypeStorable read(ResultSet rs) throws SQLException {
      DataModelXmomTypeStorable dmxts = new DataModelXmomTypeStorable();
      dmxts.fqNameIndex = rs.getString(COL_FQNAMEINDEX);
      dmxts.fqName = rs.getString(COL_FQNAME);
      dmxts.path = rs.getString(COL_PATH);
      dmxts.name = rs.getString(COL_NAME);
      dmxts.label = rs.getString(COL_LABEL);
      dmxts.index = Integer.parseInt(dmxts.fqNameIndex.substring(dmxts.fqName.length()+1));
      return dmxts;
    }

  };
  
  public String getFqNameIndex() {
    return fqNameIndex;
  }
  
  public int getIndex() {
    return index;
  }
  
  public void setIndex(int index) {
    this.index = index;
  }
  
  public String getFqName() {
    return fqName;
  }
  
  public void setFqName(String fqName) {
    this.fqName = fqName;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public String getPath() {
    return path;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public File toFile(String basePath) {
    return new File( basePath + Constants.fileSeparator 
                     + dotToFileSeparator.replace(path)
                     + Constants.fileSeparator + name + ".xml");
  }
  
  public static List<File> toFiles(String basePath, List<? extends DataModelXmomTypeStorable> list) {
    ArrayList<File> files = null;
    if( list != null ) {
      Collections.sort(list);
      files = new ArrayList<File>(list.size());
      for( DataModelXmomTypeStorable dmxts : list ) {
        files.add( dmxts.toFile(basePath) );
      }
    }
    return files;
  }
  
  
  public static List<XmomType> toXmomTypes(List<? extends DataModelXmomTypeStorable> list) {
    ArrayList<XmomType> xmomTypes = null; 
    if( list != null ) {
      xmomTypes = new ArrayList<XmomType>(list.size());
      for( DataModelXmomTypeStorable dmos : list ) {
        xmomTypes.add( dmos.toXmomType() );
      }
    }
    return xmomTypes;
  }

  
  private XmomType toXmomType() {
    return new XmomType(path,name,label);
  }

  public static List<DataModelXmomTypeStorable> toStorables(String fqName, List<? extends XmomType> xmomTypes) {
    if( xmomTypes == null || xmomTypes.isEmpty() ) {
      return Collections.emptyList();
    }
    List<DataModelXmomTypeStorable> objects = new ArrayList<DataModelXmomTypeStorable>();
    for( int i=0; i< xmomTypes.size(); ++i ) {
      XmomType o = xmomTypes.get(i);
      objects.add( new DataModelXmomTypeStorable( fqName, i, o ) );
    }
    return objects;
  }

  public void setFqNameIndex(String fqNameIndex) {
    this.fqNameIndex = fqNameIndex;
  }

}
