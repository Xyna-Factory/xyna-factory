/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xint.inference.impl.storage;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = InferenceServerConfigurationStorable.COL_ID, tableName = InferenceServerConfigurationStorable.TABLE_NAME)
public class InferenceServerConfigurationStorable extends Storable<InferenceServerConfigurationStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "inferenceserverconfiguration";
  public static final String COL_ID = "id";
  public static final String COL_TYPE = "servertype";
  public static final String COL_SERVER_VERSION = "serverversion";
  public static final String COL_PORT = "port";
  public static final String COL_MODEL = "model";

  public static final String COL_CONEXT_WINDOW_SIZE = "contextwindowsize";
  public static final String COL_ADDITIONAL_PARAMS = "additionalparameters";
  public static final String COL_DESCRIPTION = "description";


  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_TYPE)
  private String type;


  @Column(name = COL_SERVER_VERSION)
  private String serverVersion;

  @Column(name = COL_PORT)
  private int port;

  @Column(name = COL_MODEL)
  private String model;

  @Column(name = COL_CONEXT_WINDOW_SIZE)
  private long contextWindowSize;

  @Column(name = COL_ADDITIONAL_PARAMS, size = 4096)
  private String additionalParameters;

  @Column(name = COL_DESCRIPTION, size = 1024)
  private String description;

  private static InferenceServerConfigurationStorableReader reader = new InferenceServerConfigurationStorableReader();


  @Override
  public ResultSetReader<? extends InferenceServerConfigurationStorable> getReader() {
    return reader;
  }


  private static class InferenceServerConfigurationStorableReader implements ResultSetReader<InferenceServerConfigurationStorable> {

    @Override
    public InferenceServerConfigurationStorable read(ResultSet rs) throws SQLException {
      InferenceServerConfigurationStorable result = new InferenceServerConfigurationStorable();
      result.id = rs.getLong(COL_ID);
      result.type = rs.getString(COL_TYPE);
      result.serverVersion = rs.getString(COL_SERVER_VERSION);
      result.port = rs.getInt(COL_PORT);
      result.model = rs.getString(COL_MODEL);
      result.contextWindowSize = rs.getLong(COL_CONEXT_WINDOW_SIZE);
      result.additionalParameters = rs.getString(COL_ADDITIONAL_PARAMS);
      result.description = rs.getString(COL_DESCRIPTION);
      return result;
    }

  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends InferenceServerConfigurationStorable> void setAllFieldsFromData(U data) {
    InferenceServerConfigurationStorable cast = (InferenceServerConfigurationStorable) data;
    id = cast.id;
    type = cast.type;
    serverVersion = cast.serverVersion;
    port = cast.port;
    model = cast.model;
    contextWindowSize = cast.contextWindowSize;
    additionalParameters = cast.additionalParameters;
    description = cast.description;
  }


  public long getId() {
    return id;
  }


  public void setId(long id) {
    this.id = id;
  }


  public String getType() {
    return type;
  }


  public void setType(String type) {
    this.type = type;
  }


  public String getServerVersion() {
    return serverVersion;
  }


  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }


  public int getPort() {
    return port;
  }


  public void setPort(int port) {
    this.port = port;
  }


  public String getModel() {
    return model;
  }


  public void setModel(String model) {
    this.model = model;
  }


  public long getContextWindowSize() {
    return contextWindowSize;
  }


  public void setContextWindowSize(long contextWindowSize) {
    this.contextWindowSize = contextWindowSize;
  }


  public String getAdditionalParameters() {
    return additionalParameters;
  }


  public void setAdditionalParameters(String additionalParameters) {
    this.additionalParameters = additionalParameters;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }

}
