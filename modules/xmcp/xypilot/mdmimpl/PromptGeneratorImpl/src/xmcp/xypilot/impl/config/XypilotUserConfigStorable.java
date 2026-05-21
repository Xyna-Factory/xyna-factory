/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.config;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = XypilotUserConfigStorable.COL_USER, tableName = XypilotUserConfigStorable.TABLE_NAME)
public class XypilotUserConfigStorable extends Storable<XypilotUserConfigStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "xypilotuserconfig";
  public static final String COL_USER = "username";
  public static final String COL_XYPILOTURI = "xypiloturi";
  public static final String COL_MODEL = "model";
  public static final String COL_MAXSUGGESTIONS = "maxsuggestions";
  public static final String COL_METRICS = "metrics";


  @Column(name = COL_USER)
  private String user;
  @Column(name = COL_XYPILOTURI)
  private String xypiloturi;
  @Column(name = COL_MODEL)
  private String model;
  @Column(name = COL_MAXSUGGESTIONS)
  private int maxsuggestions;
  @Column(name = COL_METRICS, type=ColumnType.BLOBBED_JAVAOBJECT)
  private byte[] metrics;

  public static XypilotUseConfigStorableReader reader = new XypilotUseConfigStorableReader();


  public XypilotUserConfigStorable() {
  }

  public XypilotUserConfigStorable(String user) {
    super();
    this.user = user;
  }


  public XypilotUserConfigStorable(String user, String xypiloturi, String model, int maxsuggestions, String metrics) {
    super();
    this.user = user;
    this.xypiloturi = xypiloturi;
    this.model = model;
    this.maxsuggestions = maxsuggestions;
    this.metrics = metrics == null ? null : metrics.getBytes();
  }


  private static class XypilotUseConfigStorableReader implements ResultSetReader<XypilotUserConfigStorable> {

    @Override
    public XypilotUserConfigStorable read(ResultSet rs) throws SQLException {
      XypilotUserConfigStorable result = new XypilotUserConfigStorable();
      result.user = rs.getString(COL_USER);
      result.xypiloturi = rs.getString(COL_XYPILOTURI);
      result.model = rs.getString(COL_MODEL);
      result.maxsuggestions = rs.getInt(COL_MAXSUGGESTIONS);
      result.metrics = (byte[]) result.readBlobbedJavaObjectFromResultSet(rs, COL_METRICS, result.user);
      return result;
    }

  }


  @Override
  public ResultSetReader<? extends XypilotUserConfigStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return user;
  }


  @Override
  public <U extends XypilotUserConfigStorable> void setAllFieldsFromData(U data) {
    XypilotUserConfigStorable cast = data;
    user = cast.user;
    xypiloturi = cast.xypiloturi;
    model = cast.model;
    maxsuggestions = cast.maxsuggestions;
    metrics = cast.metrics;
  }


  public String getUser() {
    return user;
  }


  public void setUser(String user) {
    this.user = user;
  }


  public String getXypiloturi() {
    return xypiloturi;
  }


  public void setXypiloturi(String xypiloturi) {
    this.xypiloturi = xypiloturi;
  }


  public String getModel() {
    return model;
  }


  public void setModel(String model) {
    this.model = model;
  }


  public int getMaxsuggestions() {
    return maxsuggestions;
  }


  public void setMaxsuggestions(int maxsuggestions) {
    this.maxsuggestions = maxsuggestions;
  }

  
  public String getMetrics() {
    return metrics == null ? null : new String(metrics);
  }

  
  public void setMetrics(String metrics) {
    this.metrics = metrics == null ? null : metrics.getBytes();
  }

}
