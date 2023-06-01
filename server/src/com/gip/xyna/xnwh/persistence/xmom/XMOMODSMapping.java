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
package com.gip.xyna.xnwh.persistence.xmom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = XMOMODSMapping.COL_ID, tableName = XMOMODSMapping.TABLENAME)
public class XMOMODSMapping extends Storable<XMOMODSMapping> {

  private static final long serialVersionUID = 1L;
  
  public static final String TABLENAME = "xmomodsmapping";
  public static final String ID_REALM = TABLENAME;
  
  public static final String COL_REVISION = "revision";
  public static final String COL_ID = "id";
  public static final String COL_FQXMLNAME = "fqxmlname";
  public static final String COL_PATH = "path";
  public static final String COL_FQPATH = "fqpath";
  public static final String COL_TABLENAME = "tablename";
  public static final String COL_COLUMNNAME = "columnname";
  private static final String COL_USERDEFINED = "userdefined";
  
  

  @Column(name = COL_ID) 
  private long id;
  
  @Column(name = COL_FQXMLNAME, index=IndexType.MULTIPLE, size=512)
  private String fqxmlname;
  
  @Column(name = COL_REVISION)
  private long revision;
  
  @Column(name = COL_PATH, index=IndexType.MULTIPLE, size=1024) 
  private String path; //"", falls storable selbst gemeint, ansonsten format "<membervarName>.<membervarName>..."
  
  @Column(name = COL_FQPATH, index=IndexType.MULTIPLE, size=2048) 
  private String fqpath; //"", falls storable selbst gemeint, ansonsten format "<membervarName>{memberFqTypeName}.<membervarName>{memberFqTypeName}..."
  
  @Column(name = COL_TABLENAME)
  private String tablename;
  
  @Column(name = COL_COLUMNNAME)
  private String columnname;  // "", falls es sich um eine tablename-Mapping handelt
  
  @Column(name = COL_USERDEFINED)
  private boolean userdefined;
  
  

  public static final ResultSetReader<XMOMODSMapping> reader =
      new ResultSetReader<XMOMODSMapping>() {

        public XMOMODSMapping read(ResultSet rs) throws SQLException {
          XMOMODSMapping st = new XMOMODSMapping();
          st.id = rs.getLong(COL_ID);
          st.fqxmlname = rs.getString(COL_FQXMLNAME);
          st.revision = rs.getLong(COL_REVISION);
          st.path = rs.getString(COL_PATH);
          if (st.path == null) {
            st.path = "";
          }
          st.fqpath = rs.getString(COL_FQPATH);
          if (st.fqpath == null) {
            st.fqpath = "";
          }
          st.tablename = rs.getString(COL_TABLENAME);
          st.columnname = rs.getString(COL_COLUMNNAME);
          st.userdefined = rs.getBoolean(COL_USERDEFINED);
          return st;
        }
      };


  public XMOMODSMapping() {
  }
  
  public XMOMODSMapping(long id) {
    this.id = id;
  }


  public XMOMODSMapping(long id, ODSRegistrationParameter params) {
    this(id);
    fqxmlname = params.getFqxmlname();
    revision = params.getRevision();
    path = convertFqPathToPath(params.getFqpath());
    fqpath = params.getFqpath();
    tablename = params.getTableName();
    if (!params.isTableRegistration()) {
      columnname = params.getOdsName();      
    }
    userdefined = true;
  }

  
  public ResultSetReader<? extends XMOMODSMapping> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends XMOMODSMapping> void setAllFieldsFromData(U data) {
    XMOMODSMapping cast = data;
    id = cast.id;
    fqxmlname = cast.fqxmlname;
    revision = cast.revision;
    path = cast.path;
    fqpath = cast.fqpath;
    tablename = cast.tablename;
    columnname = cast.columnname;
    userdefined = cast.userdefined;
  }

  
  public long getId() {
    return id;
  }


  public void setId(long id) {
    this.id = id;
  }


  public String getFqxmlname() {
    return fqxmlname;
  }


  public void setFqxmlname(String xmomStorableFqXMLName) {
    this.fqxmlname = xmomStorableFqXMLName;
  }


  public long getRevision() {
    return revision;
  }


  public void setRevision(long revision) {
    this.revision = revision;
  }


  public String getPath() {
    return path;
  }


  public void setPath(String path) {
    this.path = path;
  }
  
  
  public String getFqpath() {
    return fqpath;
  }


  public void setFqpath(String fqpath) {
    this.fqpath = fqpath;
  }


  public String getTablename() {
    return tablename;
  }


  public void setTablename(String tablename) {
    this.tablename = tablename;
  }
  
  
  public String getColumnname() {
    return columnname;
  }


  public void setColumnname(String columnname) {
    this.columnname = columnname;
  }
  
  
  public boolean getUserdefined() {
    return userdefined;
  }


  public void setUserdefined(boolean userdefined) {
    this.userdefined = userdefined;
  }

  public boolean isTableConfig() {
    return columnname == null || columnname.isBlank();
  }

  public boolean describesSameObject(XMOMODSMapping other) {
    return Objects.equals(fqxmlname, other.getFqxmlname()) &&
           Objects.equals(fqpath, other.getFqpath());
  }
  
  private String convertFqPathToPath(String fqpath) {
    Pattern classInfoPattern = Pattern.compile("\\{[a-zA-Z0-9\\._]+\\}");
    Matcher classInfoMatcher = classInfoPattern.matcher(fqpath);
    StringBuilder pathBuilder = new StringBuilder();
    while (classInfoMatcher.find()) {
      classInfoMatcher.appendReplacement(pathBuilder, "");
    }
    classInfoMatcher.appendTail(pathBuilder);
    return pathBuilder.toString();
  }

}