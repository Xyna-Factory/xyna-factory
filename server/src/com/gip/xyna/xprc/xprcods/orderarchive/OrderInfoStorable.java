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

package com.gip.xyna.xprc.xprcods.orderarchive;




import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



@Persistable(primaryKey = OrderInfoStorable.COL_ID, tableName = OrderInfoStorable.TABLE_NAME)
public class OrderInfoStorable extends OrderInstance {

  private static final long serialVersionUID = -1110466715851182759L;

  public static final String TABLE_NAME = "orderinfo";

  public OrderInfoStorable() {
    super();
  }
  
  public OrderInfoStorable(long id) {
    super(id);
  }

  private static class OrderInfoReader implements ResultSetReader<OrderInfoStorable> {


    private OrderInfoReader() {
    }

    public OrderInfoStorable read(ResultSet rs) throws SQLException {
      OrderInfoStorable oi = new OrderInfoStorable();
      fillByResultSet(oi, rs);
      return oi;
    }

  }


  private static OrderInfoReader reader = new OrderInfoReader();

  @Override
  public ResultSetReader<? extends OrderInstance> getReader() {
    return reader;
  }

}
