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
package com.gip.xyna.xdnc.xnwh;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = VelocityTemplateStorable.COL_ID, tableName = VelocityTemplateStorable.TABLENAME)
public class VelocityTemplateStorable extends
        Storable<VelocityTemplateStorable> {

    private static final long serialVersionUID = 3381551364653611351L;

    public static final String TABLENAME = "velocitytemplate";

    public static final String COL_ID = "id";
    public static final String COL_APPLICATION = "application";
    public static final String COL_SCOPE = "scope";
    public static final String COL_TYPE = "type";
    public static final String COL_PART = "part";
    public static final String COL_CONSTRAINTSET = "constraintset";
    public static final String COL_SCORE = "score";
    public static final String COL_CONTENT = "content";

    @Column(name = COL_ID)
    private long id;

    @Column(name = COL_APPLICATION, size = 256)
    private String application;

    @Column(name = COL_SCOPE, size = 128)
    private String scope;

    @Column(name = COL_TYPE, size = 32)
    private String type;

    @Column(name = COL_PART, size = 32)
    private String part;

    @Column(name = COL_CONSTRAINTSET, size=5000)
    private String constraintSet;

    @Column(name = COL_SCORE)
    private Long score;

    @Column(name = COL_CONTENT, size=35000)
    private String content;

    public VelocityTemplateStorable() {
    }

    public VelocityTemplateStorable(long id) {
        this.id = id;
    }
    
    public long getId() {
      return id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public final static VelocityTemplateStorableReader reader = new VelocityTemplateStorableReader();

    @Override
    public ResultSetReader<VelocityTemplateStorable> getReader() {
        return reader;
    }

    @Override
    public Object getPrimaryKey() {
        return id;
    }

    @Override
    public <U extends VelocityTemplateStorable> void setAllFieldsFromData(U data2) {
        VelocityTemplateStorable data = data2;
        this.id = data.id;
        this.application = data.application;
        this.scope = data.scope;
        this.type = data.type;
        this.part = data.part;
        this.constraintSet = data.constraintSet;
        this.score = data.score;
        this.content = data.content;
    }

    private final static class VelocityTemplateStorableReader implements ResultSetReader<VelocityTemplateStorable> {

        public VelocityTemplateStorable read(ResultSet rs) throws SQLException {
            
            VelocityTemplateStorable veloTemp = new VelocityTemplateStorable();
            veloTemp.id = rs.getLong(COL_ID);
            veloTemp.application = rs.getString(COL_APPLICATION);
            veloTemp.scope = rs.getString(COL_SCOPE);
            veloTemp.type = rs.getString(COL_TYPE);
            veloTemp.part = rs.getString(COL_PART);

            veloTemp.constraintSet = rs.getString(COL_CONSTRAINTSET);
            
            veloTemp.score = rs.getLong(COL_SCORE);
            if (rs.wasNull()) {
              veloTemp.score = null;  
            }
            
            veloTemp.content = rs.getString(COL_CONTENT);
            
            return veloTemp;
        }

    }
    
    
    public final static class SelectiveVelocityTemplateStorableReader implements ResultSetReader<VelocityTemplateStorable> {
      
      private final Set<VelocityTemplateColumn> selection;
      
      public SelectiveVelocityTemplateStorableReader(Set<VelocityTemplateColumn> selection) {
        this.selection = selection;
      }
      
      public VelocityTemplateStorable read(ResultSet rs) throws SQLException {
        VelocityTemplateStorable veloTemp = new VelocityTemplateStorable();
        for (VelocityTemplateColumn column : selection) {
          switch (column) {
            case APPLICATION :
              veloTemp.application = rs.getString(COL_APPLICATION);
              break;
            case CONSTRAINTSET :
              veloTemp.constraintSet = rs.getString(COL_CONSTRAINTSET);
              break;
            case CONTENT :
              veloTemp.content = rs.getString(COL_CONTENT);
              break;
            case ID :
              veloTemp.id = rs.getLong(COL_ID);
              break;
            case PART :
              veloTemp.part = rs.getString(COL_PART);
              break;
            case SCOPE :
              veloTemp.scope = rs.getString(COL_SCOPE);
              break;
            case SCORE :
              veloTemp.score = rs.getLong(COL_SCORE);
              if (rs.wasNull()) {
                veloTemp.score = null;  
              }
              break;
            case TYPE :
              veloTemp.type = rs.getString(COL_TYPE);
              break;
            default :
              break;
          }
        }
        return veloTemp;
      }
    }

}
