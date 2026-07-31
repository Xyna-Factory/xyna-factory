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



@Persistable(primaryKey = InferenceServerManagementRequestHistoryStorable.COL_ID, tableName = InferenceServerManagementRequestHistoryStorable.TABLE_NAME)
public class InferenceServerManagementRequestHistoryStorable extends Storable<InferenceServerManagementRequestHistoryStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "inferenceservermanagementrequesthistory";
  public static final String COL_ID = "id";
  public static final String COL_TIMESTAMP = "entrytimestamp";
  public static final String COL_REQUESTID = "requestid";
  public static final String COL_DESCRIPTION = "description";


  @Column(name = COL_ID)
  private long id;
  @Column(name = COL_TIMESTAMP)
  private long timestamp;

  @Column(name = COL_REQUESTID)
  private long requestId;

  @Column(name = COL_DESCRIPTION, size = 1024)
  private String description;


  private static InferenceServerManagementRequestHistoryReader reader = new InferenceServerManagementRequestHistoryReader();


  @Override
  public ResultSetReader<? extends InferenceServerManagementRequestHistoryStorable> getReader() {
    return reader;
  }


  private static class InferenceServerManagementRequestHistoryReader
      implements
        ResultSetReader<InferenceServerManagementRequestHistoryStorable> {

    @Override
    public InferenceServerManagementRequestHistoryStorable read(ResultSet rs) throws SQLException {
      InferenceServerManagementRequestHistoryStorable result = new InferenceServerManagementRequestHistoryStorable();
      result.id = rs.getLong(COL_ID);
      result.timestamp = rs.getLong(COL_TIMESTAMP);
      result.requestId = rs.getLong(COL_REQUESTID);
      result.description = rs.getString(COL_DESCRIPTION);
      return result;
    }


  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends InferenceServerManagementRequestHistoryStorable> void setAllFieldsFromData(U data) {
    InferenceServerManagementRequestHistoryStorable cast = (InferenceServerManagementRequestHistoryStorable) data;
    id = cast.id;
    timestamp = cast.timestamp;
    requestId = cast.requestId;
    description = cast.description;
  }


  public long getId() {
    return id;
  }


  public void setId(long id) {
    this.id = id;
  }


  public long getTimestamp() {
    return timestamp;
  }


  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public long getRequestId() {
    return requestId;
  }


  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }

}
