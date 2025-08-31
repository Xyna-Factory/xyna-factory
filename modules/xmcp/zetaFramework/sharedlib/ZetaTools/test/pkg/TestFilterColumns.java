/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package pkg;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import com.gip.xyna.xnwh.persistence.Parameter;

import xmcp.zeta.storage.generic.filter.FilterColumnConfig;
import xmcp.zeta.storage.generic.filter.FilterColumnInput;
import xmcp.zeta.storage.generic.filter.TableFilter;


public class TestFilterColumns {

  private FilterColumnInput buildFilterColumnInput(String path, String value) {
    FilterColumnInput ret = new FilterColumnInput();
    ret.setFilter(value);
    ret.setPath(path);
    return ret;
  }
  
  
  private FilterColumnConfig buildFilterColumnConfig(String colname, String path) {
    FilterColumnConfig conf = FilterColumnConfig.builder().sqlColumnName(colname).xmomPath(path).build();
    return conf;
  }
  
  
  @Test
  public void test1() {
    try {
      String colname1 = "fileName";
      String colname2 = "importStatus";
      String colname3 = "uniqueIdentifier";
      
      FilterColumnConfig conf1 = buildFilterColumnConfig(colname1, "fileName");
      FilterColumnConfig conf2 = buildFilterColumnConfig(colname2, "importStatus");
      FilterColumnConfig conf3 = buildFilterColumnConfig(colname3, "uniqueIdentifier");
      
      FilterColumnInput fci1 = buildFilterColumnInput("fileName", "=16 | =*22");
      FilterColumnInput fci2 = buildFilterColumnInput("importStatus", "'Success'");
      FilterColumnInput fci3 = buildFilterColumnInput("uniqueIdentifier", ">2800300");
      
      Function<String, String> escape = (x -> "`" + x + "`");
      
      TableFilter tf = TableFilter.builder(List.of(conf1, conf2, conf3)).
                                   build(List.of(fci1, fci2, fci3), escape);
      Parameter param = tf.buildParameter();
      log(tf.getWhereClause());
      logParameter(param);
      
      assertEquals(" WHERE (`fileName` LIKE ?) OR (`fileName` LIKE ?) AND `importStatus` = ? AND `uniqueIdentifier` > ?",
                   tf.getWhereClause());
      assertEquals(4, param.size());
      assertEquals("%16%", param.get(0));
      assertEquals("%22", param.get(1));
      assertEquals("Success", param.get(2));
      assertEquals("2800300", param.get(3));
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private void logParameter(Parameter param) {
    for (int i = 0; i < param.size(); i++) {
      Object obj = param.get(i);
      log("SQL Parameter: " + obj);
    }
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestFilterColumns().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
