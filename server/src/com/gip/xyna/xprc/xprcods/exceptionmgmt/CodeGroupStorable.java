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
package com.gip.xyna.xprc.xprcods.exceptionmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.CodeGroup;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(tableName="codegroup", primaryKey="codeGroupName")
public class CodeGroupStorable extends Storable<CodeGroupStorable> {

  private static final long serialVersionUID = -1012422435648820157L;

  private static final ResultSetReader<CodeGroupStorable> reader = new ResultSetReader<CodeGroupStorable>() {

    public CodeGroupStorable read(ResultSet rs) throws SQLException{
      CodeGroupStorable cgs = new CodeGroupStorable();
      cgs.codeGroupName = rs.getString("codeGroupName");
      cgs.indexOfCurrentlyUsedPattern = rs.getInt("indexOfCurrentlyUsedPattern");
      return cgs;
    }
    
  };
  
  @Column(name="codeGroupName", size=50)
  private String codeGroupName;
  @Column(name="indexOfCurrentlyUsedPattern")
  private int indexOfCurrentlyUsedPattern;
  
  public CodeGroupStorable() {
    
  }
  
  public CodeGroupStorable(CodeGroup codeGroup) {
    codeGroupName = codeGroup.getCodeGroupName();
    indexOfCurrentlyUsedPattern = codeGroup.getIndexOfCurrentlyUsedPattern();
  }
  
  public CodeGroup getAsCodeGroup() {
    return new CodeGroup(codeGroupName, indexOfCurrentlyUsedPattern);
  }
  
  
  public String getCodeGroupName() {
    return codeGroupName;
  }

  
  public int getIndexOfCurrentlyUsedPattern() {
    return indexOfCurrentlyUsedPattern;
  }

  @Override
  public Object getPrimaryKey() {
    return codeGroupName;
  }
  
  @Override
  public ResultSetReader<? extends CodeGroupStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends CodeGroupStorable> void setAllFieldsFromData(U data) {
    CodeGroupStorable cast = data;
    codeGroupName = cast.codeGroupName;
    indexOfCurrentlyUsedPattern = cast.indexOfCurrentlyUsedPattern;
  }
  
}
