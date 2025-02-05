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
package com.gip.xyna.xprc.xsched;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoColumn;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;


@Persistable(primaryKey = VetoInformationStorable.COL_VETO_NAME, tableName = VetoInformationStorable.TABLE_NAME)
public class VetoInformationStorable extends ClusteredStorable<VetoInformationStorable> {

  public static final String TABLE_NAME = "vetos";
  public static final String COL_VETO_NAME = "vetoName";
  public static final String COL_USING_ORDER_ID = "usingOrderId";
  public static final String COL_USING_ROOT_ORDER_ID = "usingRootOrderId";
  public static final String COL_USING_ORDERTYPE = "usingOrdertype";
  public static final String COL_DOCUMENTATION = "documentation";

  private static final long serialVersionUID = -3562898639780808778L;


  @Column(name = COL_VETO_NAME, size = 100)
  private String vetoName;
  @Column(name = COL_USING_ORDER_ID)
  private Long usingOrderId;
  @Column(name = COL_USING_ROOT_ORDER_ID)
  private Long usingRootOrderId;
  @Column(name = COL_USING_ORDERTYPE)
  private String usingOrdertype;
  @Column(name = COL_DOCUMENTATION, size = 2000)
  private String documentation;
  
  
  public VetoInformationStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  public VetoInformationStorable(String vetoName, int binding) {
    super(binding);
    this.vetoName = vetoName;
  }
  
  public VetoInformationStorable(String vetoName, OrderInformation orderInformation, int binding) {
    this(vetoName, binding);
    this.usingOrderId = orderInformation.getOrderId();
    this.usingRootOrderId = orderInformation.getRootOrderId();
    this.usingOrdertype = orderInformation.getOrderType();
  }
  
  public VetoInformationStorable(String vetoName, OrderInformation orderInformation, String documentation, int binding) {
    this(vetoName, orderInformation, binding);
    this.documentation = documentation;
  }


  public String getVetoName() {
    return this.vetoName;
  }
  
  
  public Long getUsingOrderId() {
    return this.usingOrderId;
  }
  
  public Long getUsingRootOrderId() {
    if (this.usingRootOrderId == null) {
      return getUsingOrderId();
    }
    return this.usingRootOrderId;
  }

  
  public String getUsingOrdertype() {
    return this.usingOrdertype;
  }
  
  public OrderInformation getUsingOrder() {
    return new OrderInformation(getUsingOrderId(), getUsingRootOrderId(), getUsingOrdertype());
  }
  
  public String getDocumentation() {
    return this.documentation;
  }
  
  
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }
  
  
  private static VetoInformationStorableReader reader = new VetoInformationStorableReader();
  
  public static final Transformation<VetoInformation, VetoInformationStorable> fromVetoInformation = 
      new Transformation<VetoInformation, VetoInformationStorable>() {
        public VetoInformationStorable transform(VetoInformation from) {
          return new VetoInformationStorable(from.getName(), from.getOrderInformation(),
                                             from.getDocumentation(), from.getBinding());
        }
  };
  
  public static final Transformation<VetoInformationStorable, VetoInformation> toVetoInformation = 
      new Transformation<VetoInformationStorable, VetoInformation>() {
        public VetoInformation transform(VetoInformationStorable from) {
          return new VetoInformation(from.getVetoName(), 
              from.getUsingOrder(),
              from.getDocumentation(), from.getBinding());
        }
  };
 
  @Override
  public ResultSetReader<VetoInformationStorable>  getReader() {
    return reader;
  }
  

  private static class VetoInformationStorableReader implements ResultSetReader<VetoInformationStorable> {

    public VetoInformationStorable read(ResultSet rs) throws SQLException {
      VetoInformationStorable vis = new VetoInformationStorable();
      vis.vetoName = rs.getString(COL_VETO_NAME);
      vis.usingOrderId = rs.getLong(COL_USING_ORDER_ID);
      vis.usingRootOrderId = rs.getLong(COL_USING_ROOT_ORDER_ID);
      vis.usingOrdertype = rs.getString(COL_USING_ORDERTYPE);
      vis.documentation = rs.getString(COL_DOCUMENTATION);
      vis.setBinding(rs.getInt( COL_BINDING ) );
      return vis;
    }
    
  }
  
  
  public static class DynamicVetoReader implements ResultSetReader<VetoInformationStorable> {

    private List<VetoColumn> selectedCols;

    public DynamicVetoReader(List<VetoColumn> selected) {
      selectedCols = selected;
    }

    public VetoInformationStorable read(ResultSet rs) throws SQLException {
      VetoInformationStorable veto = new VetoInformationStorable();
      veto.vetoName = rs.getString(VetoColumn.VETONAME.getColumnName());
      if (selectedCols.contains(VetoColumn.USINGORDERID)) {
        veto.usingOrderId = rs.getLong(VetoColumn.USINGORDERID.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.USINGROOTORDERID)) {
        veto.usingOrderId = rs.getLong(VetoColumn.USINGROOTORDERID.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.USINGORDERTYPE)) {
        veto.usingOrdertype = rs.getString(VetoColumn.USINGORDERTYPE.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.DOCUMENTATION)) {
        veto.documentation = rs.getString(VetoColumn.DOCUMENTATION.getColumnName());
      }
      return veto;
    }
  }


  @Override
  public Object getPrimaryKey() {
    return vetoName;
  }


  @Override
  public <U extends VetoInformationStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    VetoInformationStorable cast = data;
    vetoName = cast.vetoName;
    usingOrderId = cast.usingOrderId;
    usingRootOrderId = cast.usingRootOrderId;
    usingOrdertype = cast.usingOrdertype;
    documentation = cast.documentation;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof VetoInformationStorable)) {
      return false;
    }
    return this.vetoName.equals(((VetoInformationStorable) obj).vetoName);
  }
  
  @Override
  public int hashCode() {
    return vetoName.hashCode();
  }

}
