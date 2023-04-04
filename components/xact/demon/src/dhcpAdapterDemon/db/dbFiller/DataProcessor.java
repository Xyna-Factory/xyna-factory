/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db.dbFiller;

import com.gip.xyna.utils.db.OutputParam;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.types.State;


public interface DataProcessor<Data> {
  
  public static class SQLData {

    private static final int DML = 0;
    private static final int CALL = 1;
    private String sql;
    private Parameter params;
    private int expectedRows;
    private int numRows;
    private OutputParam<Integer> out;
    private int type;
    
    private SQLData() {/*nur intern*/}
    
    public SQLData(String sql, Parameter params, int expectedRows) {
      this.sql = sql;
      this.params = params;
      this.expectedRows = expectedRows;
    }

    public String getSql() {
      return sql;
    }

    public Parameter getParameter() {
      return params;
    }

    public int getExpectedRows() {
      return expectedRows;
    }

    public static SQLData newCall(String call, Parameter params,
        OutputParam<Integer> out, int expected) {
      SQLData sd = new SQLData();
      sd.sql = call;
      sd.params = params;
      sd.out = out;
      sd.type = CALL;
      sd.expectedRows = expected;
      return sd;
    }

    public static SQLData newCall(String call, Parameter params, int expected) {
      SQLData sd = new SQLData();
      sd.sql = call;
      sd.params = params;
      sd.type = CALL;
      sd.expectedRows = expected;
      return sd;
    }


    public static SQLData newDML(String dml, Parameter params, int expectedRows) {
      SQLData sd = new SQLData();
      sd.sql = dml;
      sd.params = params;
      sd.expectedRows = expectedRows;
      return sd;
    }

    

    public void execute(SQLUtils sqlUtils) {
      if( type == DML ) {
        numRows = sqlUtils.executeDML(sql,params);
      } else {
        sqlUtils.executeCall(sql,params);
        if( out == null ) {
          numRows = 0;
        } else {
          numRows = out.get() != null ? out.get().intValue() : -1;
        }
      }
    }

    public boolean matchesExpectation() {
      return numRows == expectedRows;
    }

    public int getNumRows() {
      return numRows;
    }

    
    
    
  }

  public SQLData processData(Data data);
  
  public void state(Data data, State state );
  
  public void initialize(SQLUtils sqlUtils);
  
  public String getNDC( Data data );
  
}
