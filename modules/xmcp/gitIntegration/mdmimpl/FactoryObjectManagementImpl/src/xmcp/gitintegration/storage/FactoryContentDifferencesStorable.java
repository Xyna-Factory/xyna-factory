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
package xmcp.gitintegration.storage;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = FactoryContentDifferencesStorable.COL_LISTID, tableName = FactoryContentDifferencesStorable.TABLE_NAME)
public class FactoryContentDifferencesStorable extends Storable<FactoryContentDifferencesStorable> {

  private static final long serialVersionUID = -1L;

  public static final String TABLE_NAME = "factorycontentdifferencesstorable";

  public static final String COL_LISTID = "listid";

  @Column(name = COL_LISTID)
  private Long listid;


  public FactoryContentDifferencesStorable() {
    super();
  }


  public FactoryContentDifferencesStorable(Long listid) {
    this.listid = listid;
  }


  @Override
  public Long getPrimaryKey() {
    return listid;
  }


  private static FactoryContentDifferencesStorableReader reader = new FactoryContentDifferencesStorableReader();


  @Override
  public ResultSetReader<? extends FactoryContentDifferencesStorable> getReader() {
    return reader;
  }


  private static class FactoryContentDifferencesStorableReader implements ResultSetReader<FactoryContentDifferencesStorable> {

    @Override
    public FactoryContentDifferencesStorable read(ResultSet rs) throws SQLException {
      FactoryContentDifferencesStorable result = new FactoryContentDifferencesStorable();
      result.listid = rs.getLong(COL_LISTID);
      return result;
    }
  }


  @Override
  public <U extends FactoryContentDifferencesStorable> void setAllFieldsFromData(U data) {
    FactoryContentDifferencesStorable cast = data;
    listid = cast.listid;
  }


  public Long getListid() {
    return listid;
  }


  public void setListid(Long listid) {
    this.listid = listid;
  }


}
