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

import com.gip.xyna.xnwh.persistence.ResultSetReader;



public class OrderCount {

  private int count;


  public OrderCount(int count) {
    this.count = count;
  }


  public int getCount() {
    return count;
  }


  private static ResultSetReader<OrderCount> countReader = new ResultSetReader<OrderCount>() {

    public OrderCount read(ResultSet rs) throws SQLException {
      int count = rs.getInt(1);
      return new OrderCount(count);
    }

  };


  public static ResultSetReader<? extends OrderCount> getCountReader() {
    return countReader;
  }
}
