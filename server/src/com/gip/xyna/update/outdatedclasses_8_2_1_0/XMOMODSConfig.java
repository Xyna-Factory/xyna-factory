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
package com.gip.xyna.update.outdatedclasses_8_2_1_0;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = XMOMODSConfig.COL_ID, tableName = XMOMODSConfig.TABLENAME)
public class XMOMODSConfig extends Storable<XMOMODSConfig> {

  private static final long serialVersionUID = 1L;
  private static final String COL_FQXMLNAME = "fqxmlname";
  private static final String COL_REVISION = "revision";
  private static final String COL_PATH = "path";
  private static final String COL_ODSNAME = "odsname";

  public static final String COL_ID = "id";
  public static final String TABLENAME = "xmomodsconfig";
  

  @Column(name = COL_ID) 
  private long id;
  
  @Column(name = COL_FQXMLNAME)
  private String xmomStorableFqXMLName;
  
  @Column(name = COL_REVISION)
  private long revision;
  
  @Column(name = COL_PATH) 
  private String pathToStorable; //"", falls storable selbst gemeint, ansonsten format "<membervarName>.<membervarName>..."
  
  @Column(name = COL_ODSNAME)
  private String odsName;

  private static final ResultSetReader<XMOMODSConfig> reader =
      new ResultSetReader<XMOMODSConfig>() {

        public XMOMODSConfig read(ResultSet rs) throws SQLException {
          XMOMODSConfig st = new XMOMODSConfig();
          st.id = rs.getLong(COL_ID);
          st.xmomStorableFqXMLName = rs.getString(COL_FQXMLNAME);
          st.revision = rs.getLong(COL_REVISION);
          st.pathToStorable = rs.getString(COL_PATH);
          if (st.pathToStorable == null) {
            st.pathToStorable = "";
          }
          st.odsName = rs.getString(COL_ODSNAME);
          return st;
        }
      };


  public XMOMODSConfig() {
  }
  
  public XMOMODSConfig(long id) {
    this.id = id;
  }


  @Override
  public ResultSetReader<? extends XMOMODSConfig> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends XMOMODSConfig> void setAllFieldsFromData(U data) {
    XMOMODSConfig cast = data;
    id = cast.id;
    xmomStorableFqXMLName = cast.xmomStorableFqXMLName;
    revision = cast.revision;
    pathToStorable = cast.pathToStorable;
    odsName = cast.odsName;
  }

  
  public String getFqXmlName() {
    return xmomStorableFqXMLName;
  }

  public long getId() {
    return id;
  }


  public void setId(long id) {
    this.id = id;
  }


  public String getXmomStorableFqXMLName() {
    return xmomStorableFqXMLName;
  }


  public void setXmomStorableFqXMLName(String xmomStorableFqXMLName) {
    this.xmomStorableFqXMLName = xmomStorableFqXMLName;
  }


  public long getRevision() {
    return revision;
  }


  public void setRevision(long revision) {
    this.revision = revision;
  }


  public String getPath() {
    return pathToStorable;
  }


  public void setPathToStorable(String pathToStorable) {
    this.pathToStorable = pathToStorable;
  }


  public String getOdsName() {
    return odsName;
  }


  public void setOdsName(String odsName) {
    this.odsName = odsName;
  }


}