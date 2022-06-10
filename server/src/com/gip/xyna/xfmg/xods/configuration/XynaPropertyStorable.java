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

package com.gip.xyna.xfmg.xods.configuration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



@Persistable(primaryKey = "propertykey", tableName = "xynaproperties")
public class XynaPropertyStorable extends ClusteredStorable<XynaPropertyStorable> {

  private static final long serialVersionUID = -16720936507162219L;
  private static XynaPropertyReader reader = new XynaPropertyReader();

  public static final String TABLE_NAME = "xynaproperties";
  public static final String COLUMN_PROPERTY_KEY = "propertykey";
  public static final String COLUMN_PROPERTY_VALUE = "propertyvalue";
  public static final String COLUMN_PROPERTY_DOCUMENTATION = "propertydocumentation";
  public static final String COLUMN_IS_FACTORY_COMPONENT = "factorycomponent";

  @Column(name = COLUMN_PROPERTY_KEY, size = 200)
  private String propertyKey;
  @Column(name = COLUMN_PROPERTY_VALUE, size = 2000)
  private String propertyValue;
  @Column(name = COLUMN_PROPERTY_DOCUMENTATION, size = 5000)
  private DocumentationMap propertyDocumentation;
  @Column(name = COLUMN_IS_FACTORY_COMPONENT)
  private boolean isFactoryComponent;


  public XynaPropertyStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  public XynaPropertyStorable(String propertyKey) {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
    this.propertyKey = propertyKey;
    this.propertyDocumentation = new DocumentationMap();
  }


  public XynaPropertyStorable(int binding, String propertyKey) {
    super(binding);
    this.propertyKey = propertyKey;
    this.propertyDocumentation = new DocumentationMap();
  }


  public String getPropertyKey() {
    return propertyKey;
  }


  public String getPropertyValue() {
    return propertyValue;
  }


  public void setPropertyValue(String value) {
    this.propertyValue = value;
  }


  public void setPropertyDocumentation(Map<DocumentationLanguage, String> documentation) {
    this.propertyDocumentation = new DocumentationMap(documentation);
  }

  /**
   * eine Documentation zu bestehenden hinzufuegen
   * @param documentation
   */
  public void addPropertyDocumentation(DocumentationLanguage lang, String documentation) {
    this.propertyDocumentation.put(lang, documentation);
  }

  public Map<DocumentationLanguage, String> getPropertyDocumentation() {
    return propertyDocumentation;
  }


  public void setIsFactoryComponent(boolean b) {
    this.isFactoryComponent = b;
  }


  public boolean isFactoryComponent() {
    return isFactoryComponent;
  }


  @Override
  public Object getPrimaryKey() {
    return propertyKey;
  }


  @Override
  public ResultSetReader<? extends XynaPropertyStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends XynaPropertyStorable> void setAllFieldsFromData(U data) {
    propertyKey = data.getPropertyKey();
    propertyValue = data.getPropertyValue();
    propertyDocumentation = new DocumentationMap(data.getPropertyDocumentation());
    isFactoryComponent = data.isFactoryComponent();
  }


  private static void fillByResultSet(XynaPropertyStorable oi, ResultSet rs) throws SQLException {
    oi.propertyKey = rs.getString(COLUMN_PROPERTY_KEY);
    oi.propertyValue = rs.getString(COLUMN_PROPERTY_VALUE);
    if (oi.propertyValue == null) {
      // oracle may return null for empty strings
      oi.propertyValue = "";
    }
    oi.propertyDocumentation = DocumentationMap.valueOf(rs.getString(COLUMN_PROPERTY_DOCUMENTATION));
    oi.isFactoryComponent = rs.getBoolean(COLUMN_IS_FACTORY_COMPONENT);
  }


  private static class XynaPropertyReader implements ResultSetReader<XynaPropertyStorable> {

    public XynaPropertyStorable read(ResultSet rs) throws SQLException {
      XynaPropertyStorable oi = new XynaPropertyStorable();
      fillByResultSet(oi, rs);
      return oi;
    }

  }

}
