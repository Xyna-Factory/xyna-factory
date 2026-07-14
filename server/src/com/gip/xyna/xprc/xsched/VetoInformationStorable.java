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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
  public static final String COL_SHARED_ORDER_IDS = "sharedOrderIds";
  public static final String COL_PENDING_EXCLUSIVE_ORDER_ID = "pendingExclusiveOrderId";
  public static final String COL_DOCUMENTATION = "documentation";
  public static final String COL_CREATED = "created";

  private static final long serialVersionUID = -3562898639780808778L;

  private static final int MAX_SHARED_ORDER_IDS_LENGTH = 10000;


  @Column(name = COL_VETO_NAME, size = 100)
  private String vetoName;
  @Column(name = COL_USING_ORDER_ID)
  private Long usingOrderId;
  @Column(name = COL_USING_ROOT_ORDER_ID)
  private Long usingRootOrderId;
  @Column(name = COL_USING_ORDERTYPE)
  private String usingOrdertype;
  @Column(name = COL_SHARED_ORDER_IDS, size = MAX_SHARED_ORDER_IDS_LENGTH)
  private String sharedOrderIds;
  @Column(name = COL_PENDING_EXCLUSIVE_ORDER_ID)
  private Long pendingExclusiveOrderId;
  @Column(name = COL_DOCUMENTATION, size = 2000)
  private String documentation;
  @Column(name = COL_CREATED)
  private Long created;
  
  
  public VetoInformationStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  public VetoInformationStorable(String vetoName, int binding) {
    this(vetoName, null, null, null, null, null, binding);
  }
  
  public VetoInformationStorable(String vetoName, OrderInformation orderInformation, Long created, int binding) {
    this(vetoName, orderInformation, null, null, null, created, binding);
  }

  public VetoInformationStorable(String vetoName, List<Long> sharedOrderIds, Long created, int binding) {
    this(vetoName, null, sharedOrderIds, null, null, created, binding);
  }

  public VetoInformationStorable(String vetoName, Long pendingExclusiveOrderId, Long created, int binding) {
    this(vetoName, null, null, pendingExclusiveOrderId, null, created, binding);
  }

  public VetoInformationStorable(String vetoName, OrderInformation orderInformation, List<Long> sharedOrderIds, Long pendingExclusiveOrderId, String documentation, Long created, int binding) {
    super(binding);
    this.vetoName = vetoName;
    if (orderInformation != null) {
      this.usingOrderId = orderInformation.getOrderId();
      this.usingRootOrderId = orderInformation.getRootOrderId();
      this.usingOrdertype = orderInformation.getOrderType();
    }
    if (!setSharedOrderIds(sharedOrderIds)) {
      throw new IllegalArgumentException("Shared order IDs exceed maximum length");
    }
    this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    this.documentation = documentation;
    this.created = created;
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

  public void setUsingOrder(OrderInformation orderInformation) {
    if (orderInformation != null) {
      this.usingOrderId = orderInformation.getOrderId();
      this.usingRootOrderId = orderInformation.getRootOrderId();
      this.usingOrdertype = orderInformation.getOrderType();
    } else {
      this.usingOrderId = null;
      this.usingRootOrderId = null;
      this.usingOrdertype = null;
    }
  }

  public List<Long> getSharedOrderIds() {
    if (this.sharedOrderIds == null) {
      return Collections.emptyList();
    }
    String[] parts = this.sharedOrderIds.split(",");
    return Arrays.stream(parts).map(Long::valueOf).collect(Collectors.toList());
  }

  public boolean setSharedOrderIds(List<Long> sharedOrderIds) {
    if (sharedOrderIds == null || sharedOrderIds.isEmpty()) {
      this.sharedOrderIds = null;
    } else {
      String stringifiedOrderIds = String.join(",", sharedOrderIds.stream().map(String::valueOf).toArray(String[]::new));
      if (stringifiedOrderIds.length() > MAX_SHARED_ORDER_IDS_LENGTH) {
        return false;
      }
      this.sharedOrderIds = stringifiedOrderIds;
    }
    return true;
  }

  public boolean addSharedOrderIds(List<Long> orderIds) {
    List<Long> sharedOrderIds = getSharedOrderIds();
    sharedOrderIds.addAll(orderIds);
    return setSharedOrderIds(sharedOrderIds);
  }

  public void removeSharedOrderId(Long orderId) {
    List<Long> sharedOrderIds = getSharedOrderIds();
    sharedOrderIds.remove(orderId);
    setSharedOrderIds(sharedOrderIds);
  }

  public Long getPendingExclusiveOrderId() {
    return pendingExclusiveOrderId;
  }

  public void setPendingExclusiveOrderId(Long pendingExclusiveOrderId) {
    this.pendingExclusiveOrderId = pendingExclusiveOrderId;
  }
  
  public String getDocumentation() {
    return this.documentation;
  }
  
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public Long getCreated() {
    return created;
  }

  public void setCreated(Long created) {
    this.created = created;
  }

  public boolean isAllocatedExclusive() {
    return usingOrderId != null && sharedOrderIds == null && pendingExclusiveOrderId == null;
  }

  public boolean isAllocatedShared() {
    return usingOrderId == null && sharedOrderIds != null && pendingExclusiveOrderId == null;
  }

  public boolean isPendingExclusiveAllocation() {
    return usingOrderId == null && pendingExclusiveOrderId != null;
  }


  private static VetoInformationStorableReader reader = new VetoInformationStorableReader();
  
  public static final Transformation<VetoInformation, VetoInformationStorable> fromVetoInformation = 
      new Transformation<VetoInformation, VetoInformationStorable>() {
        public VetoInformationStorable transform(VetoInformation from) {
          return new VetoInformationStorable(from.getName(), from.getOrderInformation(),
                                             from.getSharedOrderIds(), from.getPendingExclusiveOrderId(),
                                             from.getDocumentation(), from.getCreated(), from.getBinding());
        }
  };
  
  public static final Transformation<VetoInformationStorable, VetoInformation> toVetoInformation =
      new Transformation<VetoInformationStorable, VetoInformation>() {
        public VetoInformation transform(VetoInformationStorable from) {
          return new VetoInformation(from.getVetoName(), from.getUsingOrder(), 
                                     from.getSharedOrderIds(), from.getPendingExclusiveOrderId(), 
                                     from.getDocumentation(), from.getCreated(),
                                     from.getBinding());
        }
      };
 
  @Override
  public ResultSetReader<VetoInformationStorable> getReader() {
    return reader;
  }
  

  private static class VetoInformationStorableReader implements ResultSetReader<VetoInformationStorable> {

    public VetoInformationStorable read(ResultSet rs) throws SQLException {
      VetoInformationStorable vis = new VetoInformationStorable();
      vis.vetoName = rs.getString(COL_VETO_NAME);
      vis.usingOrderId = rs.getLong(COL_USING_ORDER_ID);
      vis.usingRootOrderId = rs.getLong(COL_USING_ROOT_ORDER_ID);
      vis.usingOrdertype = rs.getString(COL_USING_ORDERTYPE);
      vis.sharedOrderIds = rs.getString(COL_SHARED_ORDER_IDS);
      vis.pendingExclusiveOrderId = rs.getLong(COL_PENDING_EXCLUSIVE_ORDER_ID);
      vis.documentation = rs.getString(COL_DOCUMENTATION);
      vis.created = rs.getLong(COL_CREATED);
      vis.created = vis.created == 0 ? null : vis.created;
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
      if (selectedCols.contains(VetoColumn.SHAREDORDERIDS)) {
        veto.sharedOrderIds = rs.getString(VetoColumn.SHAREDORDERIDS.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.PENDINGEXCLUSIVEORDERID)) {
        veto.pendingExclusiveOrderId = rs.getLong(VetoColumn.PENDINGEXCLUSIVEORDERID.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.DOCUMENTATION)) {
        veto.documentation = rs.getString(VetoColumn.DOCUMENTATION.getColumnName());
      }
      if (selectedCols.contains(VetoColumn.CREATED)) {
        veto.created = rs.getLong(VetoColumn.CREATED.getColumnName());
        veto.created = veto.created == 0 ? null : veto.created;
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
    sharedOrderIds = cast.sharedOrderIds;
    pendingExclusiveOrderId = cast.pendingExclusiveOrderId;
    documentation = cast.documentation;
    created = cast.created;
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
