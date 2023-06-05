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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = RemoteDestinationTypeInstanceStorable.COL_NAME, tableName = RemoteDestinationTypeInstanceStorable.TABLENAME)
public class RemoteDestinationTypeInstanceStorable extends Storable<RemoteDestinationTypeInstanceStorable>{
  
    private static final long serialVersionUID = 1L;
    public static final String TABLENAME = "remotedestinationtypeinstance";

    public static final String COL_NAME = "name";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_TYPENAME = "typename";
    public static final String COL_EXECTIMEOUT = "exectimeout";
    public static final String COL_PARAMETER = "parameter";
     
    @Column(name = COL_NAME)
    private String name;
    
    @Column(name = COL_DESCRIPTION)
    private String description;
    
    @Column(name = COL_TYPENAME)
    private String typename;
    
    @Column(name = COL_EXECTIMEOUT)
    private String exectimeout;
    
    private transient Duration executionTimeout;
    
    @Column(name = COL_PARAMETER)
    private CSVStringList parameter;
    
    
    private transient Map<String, Object> parameterMap;
    
    private static RemoteDestinationTypeInstanceResultSetReader reader = new RemoteDestinationTypeInstanceResultSetReader();
    
    public RemoteDestinationTypeInstanceStorable() {
    }
    
    
    public RemoteDestinationTypeInstanceStorable(String name, String description, String typeName, Duration executionTimeout, RemoteDestinationType type, Map<String, String> parameter) {
      this.name = name;
      this.description = description;
      this.typename = typeName;
      this.executionTimeout = executionTimeout;
      if (executionTimeout != null) {
        this.exectimeout = executionTimeout.serializeToString();
      }
      this.parameterMap = readParameter(type, parameter);
      this.parameter = new CSVStringList(serializeParameter(type));
    }
    
    @Override
    public ResultSetReader<? extends RemoteDestinationTypeInstanceStorable> getReader() {
      return reader;
    }


    @Override
    public Object getPrimaryKey() {
      return name;
    }
    
    
    public String getName() {
      return name;
    }

    
    public void setName(String name) {
      this.name = name;
    }

    
    public String getDescription() {
      return description;
    }

    
    public void setDescription(String description) {
      this.description = description;
    }

    
    public String getTypename() {
      return typename;
    }

    
    public void setTypename(String typename) {
      this.typename = typename;
    }
    
    
    public String getExectimeout() {
      return exectimeout;
    }

    
    public void setExectimeout(String exectimeout) {
      this.exectimeout = exectimeout;
      if (exectimeout != null &&
          !exectimeout.isEmpty()) {
        this.executionTimeout = Duration.valueOf(exectimeout);        
      }
    }
    
    
    public Duration getExecutionTimeout() {
      return executionTimeout;
    }

    
    public List<String> getParameter() {
      return parameter;
    }

    
    public void setParameter(List<String> parameter) {
      this.parameter = new CSVStringList(parameter);
    }


    public Map<String, Object> getParameterMap() throws StringParameterParsingException {
      if (parameterMap == null) {
        parameterMap = readParameter();
      }
      return parameterMap;
    }

    
    public void setParameterMap(Map<String, Object> parameterMap) {
      this.parameterMap = parameterMap;
    }


    @Override
    public <U extends RemoteDestinationTypeInstanceStorable> void setAllFieldsFromData(U data) {
      RemoteDestinationTypeInstanceStorable cast = data;
      this.name = cast.name;
      this.description = cast.description;
      this.typename = cast.typename;
      this.parameter = cast.parameter;
      this.executionTimeout = cast.executionTimeout;
      this.exectimeout = cast.exectimeout;
      this.parameterMap = cast.parameterMap;
    }
    
    private List<String> serializeParameter(RemoteDestinationType type) {
      PluginDescription description = type.getInitialisationParameterDescription();
      List<String> parameter = StringParameter.toList(description.getParameters(ParameterUsage.Create), parameterMap, false);
      return parameter;
    }
    
    private Map<String, Object> readParameter() throws StringParameterParsingException {
      return readParameter(StringParameter.listToMap(this.parameter));
    }

    private Map<String, Object> readParameter(Map<String, String> parameter) throws StringParameterParsingException {
      RemoteDestinationManagement rdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
      RemoteDestinationType type = rdm.getRemoteDestinationType(typename);
      return readParameter(type, parameter);
    }
    
    private static Map<String, Object> readParameter(RemoteDestinationType type, Map<String, String> parameter) {
      PluginDescription paramDesc  = type.getInitialisationParameterDescription();
      if (paramDesc == null) { // TODO remove this
        return Collections.<String, Object>emptyMap();
      }
      List<StringParameter<?>> params = paramDesc.getParameters(ParameterUsage.Create);
      try {
        return StringParameter.parse(parameter).with(params);
      } catch (StringParameterParsingException e) {
        throw new IllegalArgumentException(e); 
      }
    }

    private static class RemoteDestinationTypeInstanceResultSetReader implements ResultSetReader<RemoteDestinationTypeInstanceStorable> {
      public RemoteDestinationTypeInstanceStorable read(ResultSet rs) throws SQLException {
        RemoteDestinationTypeInstanceStorable result = new RemoteDestinationTypeInstanceStorable();
        fillByResultset(result, rs);
        return result;
      }
    }
    
    
    private static void fillByResultset(RemoteDestinationTypeInstanceStorable rdti, ResultSet rs) throws SQLException {
      rdti.name = rs.getString(COL_NAME);
      rdti.description = rs.getString(COL_DESCRIPTION);
      rdti.typename = rs.getString(COL_TYPENAME);
      rdti.exectimeout = rs.getString(COL_EXECTIMEOUT);
      if (rdti.exectimeout != null &&
          !rdti.exectimeout.isEmpty()) {
        rdti.executionTimeout = Duration.valueOf(rdti.exectimeout);
      }
      rdti.parameter = CSVStringList.valueOf(rs.getString(COL_PARAMETER));
    }



}
