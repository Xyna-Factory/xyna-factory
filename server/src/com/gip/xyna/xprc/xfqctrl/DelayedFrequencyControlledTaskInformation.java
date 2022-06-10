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


package com.gip.xyna.xprc.xfqctrl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = DelayedFrequencyControlledTaskInformation.COL_ID, tableName = DelayedFrequencyControlledTaskInformation.TABLENAME)
public class DelayedFrequencyControlledTaskInformation extends Storable<DelayedFrequencyControlledTaskInformation> {

  private static final long serialVersionUID = 1L;

  public final static String TABLENAME = "delayedFrequencyTaskInfos";
  public final static String COL_ID = "id";
  public final static String COL_DELAY = "delay";
  public final static String COL_CREATIONPARAMS = "creationParams";

  @Column(name = COL_ID, size = 100)
  private String id;
  @Column(name = COL_DELAY, size = 100)
  private String delay;
  @Column(name = COL_CREATIONPARAMS, type = ColumnType.BLOBBED_JAVAOBJECT)
  private FrequencyControlledTaskCreationParameter creationParams = null;


  public DelayedFrequencyControlledTaskInformation() {

  }


  private static void fillByResultSet(DelayedFrequencyControlledTaskInformation df, ResultSet rs) throws SQLException {
    df.setId(rs.getString(COL_ID));//this.id = rs.getString(COL_ID);
    df.setDelay(rs.getString(COL_DELAY));//this.delay = rs.getString(COL_DELAY);
    df.setCreationParams((FrequencyControlledTaskCreationParameter) df.readBlobbedJavaObjectFromResultSet(rs, COL_CREATIONPARAMS));//this.creationParams = (FrequencyControlledTaskCreationParameter) readBlobbedJavaObjectFromResultSet(rs, COL_CREATIONPARAMS);
  }


  private static ResultSetReader<DelayedFrequencyControlledTaskInformation> reader =
      new ResultSetReader<DelayedFrequencyControlledTaskInformation>() {

        public DelayedFrequencyControlledTaskInformation read(ResultSet rs) throws SQLException {
          DelayedFrequencyControlledTaskInformation dfcti = new DelayedFrequencyControlledTaskInformation();
          fillByResultSet(dfcti, rs);//dfcti.fillByResultSet(rs);
          return dfcti;
        }

      };


  public String getId() {
    return this.id;
  }


  public String getDelay() {
    return this.delay;
  }


  public FrequencyControlledTaskCreationParameter getCreationParams() {
    return this.creationParams;
  }


  public void setId(String id) {
    this.id = id;
  }


  public void setDelay(String delay) {
    this.delay = delay;
  }


  public void setCreationParams(FrequencyControlledTaskCreationParameter creationParams) {
    this.creationParams = creationParams;
  }


  @Override
  public ResultSetReader<? extends DelayedFrequencyControlledTaskInformation> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends DelayedFrequencyControlledTaskInformation> void setAllFieldsFromData(U data) {
    DelayedFrequencyControlledTaskInformation cast = data;
    id = cast.id;
    delay = cast.delay;
    creationParams = cast.creationParams;
  }

}
