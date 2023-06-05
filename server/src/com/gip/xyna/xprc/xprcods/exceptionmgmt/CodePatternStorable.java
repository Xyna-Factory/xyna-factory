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
package com.gip.xyna.xprc.xprcods.exceptionmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.Pattern;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(tableName="codepattern", primaryKey="id")
public class CodePatternStorable extends Storable<CodePatternStorable> {

  private static final long serialVersionUID = 3692313251035808817L;

  private static final ResultSetReader<CodePatternStorable> reader = new ResultSetReader<CodePatternStorable>() {

    public CodePatternStorable read(ResultSet rs) throws SQLException {
      CodePatternStorable cps = new CodePatternStorable();
      cps.id = rs.getString("id");
      cps.codeGroupName = rs.getString("codeGroupName");
      cps.prefix = rs.getString("prefix");
      cps.suffix = rs.getString("suffix");
      cps.padding = rs.getInt("padding");
      cps.startIndex = rs.getInt("startIndex");
      cps.endIndex = rs.getInt("endIndex");
      cps.currentIndex = rs.getInt("currentIndex");
      cps.patternIndex = rs.getInt("patternIndex");
      return cps;
    }
    
  };
  
  @Column(name="id", size=100)
  private String id;
  
  @Column(name="codeGroupName", size=20)
  private String codeGroupName;
  
  @Column(name="prefix", size=30)
  private String prefix;
  
  @Column(name="suffix", size=30)
  private String suffix;
  
  @Column(name="padding")
  private int padding;
  
  @Column(name="startIndex")
  private int startIndex;
  
  @Column(name="endIndex")
  private int endIndex;
  
  @Column(name="currentIndex")
  private int currentIndex;
  
  @Column(name="patternIndex")
  private int patternIndex;
  
  public CodePatternStorable() {
    
  }
  
  public CodePatternStorable(String id, int index, String codeGroupName, Pattern pattern) {
    this.id = id;
    this.codeGroupName = codeGroupName;
    prefix = pattern.getPrefix();
    suffix = pattern.getSuffix();
    padding = pattern.getPadding();
    startIndex = pattern.getStartIndex();
    endIndex = pattern.getEndIndex();
    currentIndex = pattern.getCurrentIndex();
    patternIndex = index;
  }
  
  public Pattern getAsPattern() {    
    return new Pattern(prefix, suffix, padding, startIndex, endIndex, currentIndex);
  }
  
  public String getId() {
    return id;
  }
  
  public int getPatternIndex() {
    return patternIndex;
  }

  
  public String getCodeGroupName() {
    return codeGroupName;
  }

  
  public String getPrefix() {
    return prefix;
  }

  
  public String getSuffix() {
    return suffix;
  }

  
  public int getPadding() {
    return padding;
  }

  
  public int getStartIndex() {
    return startIndex;
  }

  
  public int getEndIndex() {
    return endIndex;
  }

  
  public int getCurrentIndex() {
    return currentIndex;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public ResultSetReader<? extends CodePatternStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends CodePatternStorable> void setAllFieldsFromData(U data) {
    CodePatternStorable cast = data;
    id = cast.id;
    codeGroupName = cast.codeGroupName;
    prefix = cast.prefix;
    suffix = cast.suffix;
    padding = cast.padding;
    startIndex = cast.startIndex;
    endIndex = cast.endIndex;
    currentIndex = cast.currentIndex;
    patternIndex = cast.patternIndex;
  }
  
}
