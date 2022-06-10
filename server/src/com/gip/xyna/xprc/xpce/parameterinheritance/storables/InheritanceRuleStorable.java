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
package com.gip.xyna.xprc.xpce.parameterinheritance.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;


@Persistable(primaryKey = InheritanceRuleStorable.COL_ID, tableName = InheritanceRuleStorable.TABLENAME)
public class InheritanceRuleStorable extends Storable<InheritanceRuleStorable>{

  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "inheritancerule";
  public static ResultSetReader<InheritanceRuleStorable> reader = new InheritanceRuleStorableReader();

  public static final String COL_ID = "id";
  public static final String COL_ORDERTYPE = "orderType";
  public static final String COL_REVISION = "revision";
  public static final String COL_PARAMETERTYPE = "parameterType";
  public static final String COL_CHILDFILTER = "childFilter";
  public static final String COL_VALUE = "value";
  public static final String COL_PRECEDENCE = "precedence";
  
  @Column(name = COL_ID)
  private long id;
  
  @Column(name = COL_ORDERTYPE)
  private String orderType;
  
  @Column(name = COL_REVISION)
  private Long revision;
  
  @Column(name = COL_PARAMETERTYPE)
  private String parameterType;
  
  @Column(name = COL_CHILDFILTER)
  private String childFilter;
  
  @Column(name = COL_VALUE)
  private String value;
  
  @Column(name = COL_PRECEDENCE)
  private int precedence;
  
  
  public InheritanceRuleStorable() {
  }
  

  public InheritanceRuleStorable(String orderType, Long revision, ParameterType parameterType, String childFilter,
                                 String value, int precedence) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.orderType = orderType;
    this.revision = revision;
    this.parameterType = parameterType.toString();
    this.childFilter = childFilter;
    this.value = value;
    this.precedence = precedence;
  }

  public InheritanceRuleStorable(long id, String orderType, Long revision, ParameterType parameterType, String childFilter,
                                 String value, int precedence) {
    this(orderType, revision, parameterType, childFilter, value, precedence);
    this.id = id;
  }


  @Override
  public ResultSetReader<? extends InheritanceRuleStorable> getReader() {
    return reader;
  }
  
  private static class InheritanceRuleStorableReader implements ResultSetReader<InheritanceRuleStorable> {
    public InheritanceRuleStorable read(ResultSet rs) throws SQLException {
      InheritanceRuleStorable result = new InheritanceRuleStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(InheritanceRuleStorable irs, ResultSet rs) throws SQLException {
    irs.id = rs.getLong(COL_ID);
    irs.orderType = rs.getString(COL_ORDERTYPE);
    irs.revision = rs.getLong(COL_REVISION);
    irs.parameterType = rs.getString(COL_PARAMETERTYPE);
    irs.childFilter = rs.getString(COL_CHILDFILTER);
    irs.value = rs.getString(COL_VALUE);
    irs.precedence = rs.getInt(COL_PRECEDENCE);
  }
  
  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends InheritanceRuleStorable> void setAllFieldsFromData(U data) {
    InheritanceRuleStorable cast = data;
    this.id = cast.id;
    this.orderType = cast.orderType;
    this.revision = cast.revision;
    this.parameterType = cast.parameterType;
    this.childFilter = cast.childFilter;
    this.value = cast.value;
    this.precedence = cast.precedence;
  }

  
  public long getId() {
    return id;
  }

  
  public void setId(long id) {
    this.id = id;
  }

  
  public String getOrderType() {
    return orderType;
  }

  
  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  
  public String getParameterType() {
    return parameterType;
  }

  public ParameterType getParameterTypeAsEnum() {
    return ParameterType.valueOf(parameterType);
  }

  
  public void setParameterType(String parameterType) {
    this.parameterType = parameterType;
  }

  
  public String getChildFilter() {
    return childFilter;
  }

  
  public void setChildFilter(String childFilter) {
    this.childFilter = childFilter;
  }

  
  public String getValue() {
    return value;
  }

  
  public void setValue(String value) {
    this.value = value;
  }

  
  public int getPrecedence() {
    return precedence;
  }

  
  public void setPrecedence(int precedence) {
    this.precedence = precedence;
  }
}
