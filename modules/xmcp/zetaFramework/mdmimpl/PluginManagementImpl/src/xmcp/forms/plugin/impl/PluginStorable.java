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
package xmcp.forms.plugin.impl;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = PluginStorable.COL_ID, tableName = PluginStorable.TABLE_NAME)
public class PluginStorable extends Storable<PluginStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "pluginmanagement";
  public static final String COL_ID = "id";
  public static final String COL_NavigationEntryLabel = "navigationentrylabel";
  public static final String COL_NavigationEntryName = "navigationentryname";
  public static final String COL_NavigationIconName = "navigationiconname";
  public static final String COL_definitionworkflowfqn = "definitionworkflowfqn";
  public static final String COL_pluginrtc = "pluginrtc";

  @Column(name = COL_ID)
  private String id;

  @Column(name = COL_NavigationEntryLabel)
  private String navigationentrylabel;

  @Column(name = COL_NavigationEntryName)
  private String navigationentryname;

  @Column(name = COL_NavigationIconName)
  private String navigationiconname;

  @Column(name = COL_definitionworkflowfqn)
  private String definitionworkflowfqn;

  @Column(name = COL_pluginrtc)
  private String pluginrtc;


  public PluginStorable() {
    super();
  }


  private static final PluginStorableReader reader = new PluginStorableReader();


  @Override
  public ResultSetReader<? extends PluginStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends PluginStorable> void setAllFieldsFromData(U data) {
    PluginStorable cast = (PluginStorable) data;
    id = cast.id;
    navigationentrylabel = cast.navigationentrylabel;
    navigationentryname = cast.navigationentryname;
    navigationiconname = cast.navigationiconname;
    definitionworkflowfqn = cast.definitionworkflowfqn;
    pluginrtc = cast.pluginrtc;
  }


  private static class PluginStorableReader implements ResultSetReader<PluginStorable> {

    @Override
    public PluginStorable read(ResultSet rs) throws SQLException {
      PluginStorable result = new PluginStorable();
      result.id = rs.getString(COL_ID);
      result.navigationentrylabel = rs.getString(COL_NavigationEntryLabel);
      result.navigationentryname = rs.getString(COL_NavigationEntryName);
      result.navigationiconname = rs.getString(COL_NavigationIconName);
      result.definitionworkflowfqn = rs.getString(COL_definitionworkflowfqn);
      result.pluginrtc = rs.getString(COL_pluginrtc);
      return result;
    }

  }


  public static String createId(PluginStorable plugin) {
    return String.format("%s_%s", plugin.pluginrtc, plugin.navigationentryname);
  }


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getNavigationentrylabel() {
    return navigationentrylabel;
  }


  public void setNavigationentrylabel(String navigationentrylabel) {
    this.navigationentrylabel = navigationentrylabel;
  }


  public String getNavigationentryname() {
    return navigationentryname;
  }


  public void setNavigationentryname(String navigationentryname) {
    this.navigationentryname = navigationentryname;
  }


  public String getNavigationiconname() {
    return navigationiconname;
  }


  public void setNavigationiconname(String navigationiconname) {
    this.navigationiconname = navigationiconname;
  }


  public String getDefinitionworkflowfqn() {
    return definitionworkflowfqn;
  }


  public void setDefinitionworkflowfqn(String definitionworkflowfqn) {
    this.definitionworkflowfqn = definitionworkflowfqn;
  }


  public String getPluginrtc() {
    return pluginrtc;
  }


  public void setPluginrtc(String pluginrtc) {
    this.pluginrtc = pluginrtc;
  }

}
