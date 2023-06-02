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

package com.gip.xyna.templateprovider.persistence.storables;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.*;



@Persistable(tableName = VelocityTemplate.TABLENAME, primaryKey = VelocityTemplate.COL_ID) // was COL_PK
public class VelocityTemplate extends Storable<VelocityTemplate> {
  
  final private static Logger logger = CentralFactoryLogging.getLogger(VelocityTemplate.class);


  public static final String TABLENAME = "velocitytemplate";
  public static final String COL_ID = "id";
  public static final String COL_APPLICATION = "application";
  public static final String COL_SCOPE = "scope";
  public static final String COL_PART = "part";
  public static final String COL_CONSTRAINTSET = "constraintset";
  public static final String COL_SCORE = "score";
  public static final String COL_CONTENT = "content";
  
  private static final long serialVersionUID = 1L;



  private static class ReaderVelocityTemplate implements ResultSetReader<VelocityTemplate> {

    public VelocityTemplate read(ResultSet rs) throws SQLException {
      VelocityTemplate ps = new VelocityTemplate();
      VelocityTemplate.fillByResultSet(ps, rs);
      return ps;
    }

  }

  public int getId() {
    return id;
  }

  
  public void setId(int id) {
    this.id = id;
  }

  
  public String getApplication() {
    return application;
  }

  
  public void setApplication(String application) {
    this.application = application;
  }

  
  public String getScope() {
    return scope;
  }

  
  public void setScope(String scope) {
    this.scope = scope;
  }

  
  public String getPart() {
    return part;
  }

  
  public void setPart(String part) {
    this.part = part;
  }

  
  public String getConstraintSet() {
    return constraintSet;
  }

  
  public void setConstraintSet(String constraintSet) {
    this.constraintSet = constraintSet;
  }

  
  public int getScore() {
    return score;
  }

  
  public void setScore(int score) {
    this.score = score;
  }

  
  public String getContent() {
    return content;
  }

  
  public void setContent(String content) {
    this.content = content;
  }


  @Column(name = COL_ID)
  private int id;
  
   @Column(name = COL_APPLICATION, size=256)
  private String application;
  
  @Column(name = COL_SCOPE, size=128)
  private String scope;
  
  @Column(name = COL_PART, size=256)
  private String part;
  
  @Column(name = COL_CONSTRAINTSET, size=4000)
  private String constraintSet;
  
  @Column(name = COL_SCORE)
  private int score;
  
  @Column(name = COL_CONTENT, size=4000)
  private String content;
  

  
  public VelocityTemplate() {
  }
  
   public static void fillByResultSet(VelocityTemplate vt, ResultSet rs) throws SQLException {
     vt.id = rs.getInt(COL_ID);
     vt.application = rs.getString(COL_APPLICATION);
     vt.scope = rs.getString(COL_SCOPE);
     vt.part = rs.getString(COL_PART);
     vt.constraintSet = rs.getString(COL_CONSTRAINTSET);
     vt.score = rs.getInt(COL_SCORE);
     vt.content = rs.getString(COL_CONTENT);    
   }


  @Override
  public Object getPrimaryKey() {
    return new Integer(id);

  }


  @Override
  public ResultSetReader<? extends VelocityTemplate> getReader() {
    return new ReaderVelocityTemplate();
  }


  @Override
  public <U extends VelocityTemplate> void setAllFieldsFromData(U arg0) {
     this.id=arg0.getId();
     this.application=arg0.getApplication();
     this.scope=arg0.getScope();
     this.part=arg0.getPart();
     this.constraintSet=arg0.getConstraintSet();
     this.score=arg0.getScore();
     this.content=arg0.getContent();
  }
   
}
