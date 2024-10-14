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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables;



import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource.OrderInputSourceColumn;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



@Persistable(primaryKey = OrderInputSourceStorable.COL_ID, tableName = OrderInputSourceStorable.TABLENAME)
public class OrderInputSourceStorable extends Storable<OrderInputSourceStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "orderinputsource";
  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_TYPE = "type";
  public static final String COL_ORDERTYPE = "ordertype";
  public static final String COL_APPLICATIONNAME = "applicationname";
  public static final String COL_VERSIONNAME = "versionname";
  public static final String COL_WORKSPACENAME = "workspacename";
  public static final String COL_DOCUMENTATION = "documentation";

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_NAME, index=IndexType.MULTIPLE)
  private String name;

  @Column(name = COL_TYPE)
  private String type;

  @Column(name = COL_ORDERTYPE)
  private String orderType;

  @Column(name = COL_APPLICATIONNAME)
  private String applicationName;

  @Column(name = COL_VERSIONNAME)
  private String versionName;

  @Column(name = COL_WORKSPACENAME)
  private String workspaceName;

  @Column(name = COL_DOCUMENTATION)
  private String documentation;
  
  private String state;

  private Map<String, String> parameters;

  private int referencedInputSourceCount = 0;

  public OrderInputSourceStorable() {
  }


  public OrderInputSourceStorable(long id, String name, String type, String ordertype, String applicationName, String versionName,
                                  String workspaceName, String documentation) {
    this(name, type, ordertype, applicationName, versionName, workspaceName, documentation, null);
    this.id = id;
  }


  public OrderInputSourceStorable(String name, String type, String ordertype, String applicationName, String versionName,
                                  String workspaceName, String documentation, Map<String, String> parameters) {
    this.name = name;
    this.type = type;
    this.orderType = ordertype;
    this.applicationName = applicationName;
    this.versionName = versionName;
    this.workspaceName = workspaceName;
    this.documentation = documentation;
    this.parameters = parameters;
  }


  public OrderInputSourceStorable(long id) {
    this.id = id;
  }


  public static final ResultSetReader<OrderInputSourceStorable> reader = new ResultSetReader<OrderInputSourceStorable>() {

    public OrderInputSourceStorable read(ResultSet rs) throws SQLException {
      OrderInputSourceStorable ret = new OrderInputSourceStorable();
      ret.applicationName = rs.getString(COL_APPLICATIONNAME);
      ret.documentation = rs.getString(COL_DOCUMENTATION);
      ret.id = rs.getLong(COL_ID);
      ret.name = rs.getString(COL_NAME);
      ret.orderType = rs.getString(COL_ORDERTYPE);
      ret.type = rs.getString(COL_TYPE);
      ret.versionName = rs.getString(COL_VERSIONNAME);
      ret.workspaceName = rs.getString(COL_WORKSPACENAME);
      return ret;
    }
  };


  @Override
  public ResultSetReader<? extends OrderInputSourceStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends OrderInputSourceStorable> void setAllFieldsFromData(U data) {
    OrderInputSourceStorable cast = data;
    applicationName = cast.applicationName;
    documentation = cast.documentation;
    id = cast.id;
    name = cast.name;
    orderType = cast.orderType;
    type = cast.type;
    versionName = cast.versionName;
    workspaceName = cast.workspaceName;
  }


  public long getId() {
    return id;
  }


  public String getName() {
    return name;
  }
  
  
  public void setName(String name) {
    this.name = name;
  }


  public String getType() {
    return type;
  }


  public String getOrderType() {
    return orderType;
  }


  public String getApplicationName() {
    return applicationName;
  }


  public String getVersionName() {
    return versionName;
  }


  public String getWorkspaceName() {
    return workspaceName;
  }


  public String getDocumentation() {
    return documentation;
  }


  public Map<String, String> getParameters() {
    return parameters;
  }


  public void setId(long id) {
    this.id = id;
  }


  public void setParameters(List<OrderInputSourceSpecificStorable> specifics) {
    parameters = new HashMap<String, String>();
    for (OrderInputSourceSpecificStorable sp : specifics) {
      parameters.put(sp.getKey(), sp.getValue());
    }
  }


  public static class DynamicOrderInputGeneratorReader implements ResultSetReader<OrderInputSourceStorable> {

    private Set<OrderInputSourceColumn> selectedCols;


    public DynamicOrderInputGeneratorReader(Set<OrderInputSourceColumn> selected) {
      selectedCols = selected;
    }


    public OrderInputSourceStorable read(ResultSet rs) throws SQLException {
      OrderInputSourceStorable ret = new OrderInputSourceStorable();

      if (selectedCols.contains(OrderInputSourceColumn.ID)) {
        ret.id = rs.getLong(COL_ID);
      }
      if (selectedCols.contains(OrderInputSourceColumn.NAME)) {
        ret.name = rs.getString(COL_NAME);
      }
      if (selectedCols.contains(OrderInputSourceColumn.ORDERTYPE)) {
        ret.orderType = rs.getString(COL_ORDERTYPE);
      }
      if (selectedCols.contains(OrderInputSourceColumn.TYPE)) {
        ret.type = rs.getString(COL_TYPE);
      }
      if (selectedCols.contains(OrderInputSourceColumn.APPLICATIONNAME)) {
        ret.applicationName = rs.getString(COL_APPLICATIONNAME);
      }
      if (selectedCols.contains(OrderInputSourceColumn.VERSIONNAME)) {
        ret.versionName = rs.getString(COL_VERSIONNAME);
      }
      if (selectedCols.contains(OrderInputSourceColumn.WORKSPACENAME)) {
        ret.workspaceName = rs.getString(COL_WORKSPACENAME);
      }
      if (selectedCols.contains(OrderInputSourceColumn.DOCUMENTATION)) {
        ret.documentation = rs.getString(COL_DOCUMENTATION);
      }

      return ret;
    }

  }


  //die gui erzeugt werte mit "%0%. ..." in der parameter-map
  public void cleanupGUIRelics() {
    if (parameters != null) {
      Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();
      List<Pair<String, String>> transformed = new ArrayList<Pair<String, String>>();
      while (iterator.hasNext()) {
        Entry<String, String> next = iterator.next();
        if (next.getKey().startsWith("%0%.")) {
          transformed.add(Pair.of(next.getKey().substring(4), next.getValue()));
          iterator.remove();
        }
      }
      for (Pair<String, String> p : transformed) {
        parameters.put(p.getFirst(), p.getSecond());
      }
    }
  }
  
  public String getState() {
    return state;
  }


  public void setState(String state) {
    this.state = state;
  }


  public DestinationKey getDestinationKey() {
    RuntimeContext rc;
    if (workspaceName != null) {
      rc = new Workspace(workspaceName);
    } else {
      rc = new Application(applicationName, versionName);
    }
    return new DestinationKey(orderType, rc);
  }
  
  private transient long revision = Integer.MIN_VALUE;


  public long getRevision() {
    if (revision == Integer.MIN_VALUE) {
      try {
        revision =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                .getRevision(applicationName, versionName, workspaceName);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }
    return revision;
  }


  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }
  
  public int getReferencedInputSourceCount() {
    return referencedInputSourceCount;
  }
  
  public void setReferencedInputSourceCount(int cnt) {
    referencedInputSourceCount = cnt;
  }

  public void setRuntimeContext(RuntimeContext rc) {
    workspaceName = (rc instanceof Workspace) ? rc.getName() : null;
    applicationName = (rc instanceof Application) ? rc.getName() : null;
    versionName = (rc instanceof Application) ? ((Application)rc).getVersionName() : null;
  }
  
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    revision = Integer.MIN_VALUE;
  }
}
