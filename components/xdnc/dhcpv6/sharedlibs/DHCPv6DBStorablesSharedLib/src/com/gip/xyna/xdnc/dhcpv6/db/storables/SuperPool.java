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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = SuperPool.COL_SUPERPOOLID, tableName = SuperPool.TABLENAME)
public class SuperPool extends Storable<SuperPool> implements Comparable<SuperPool>, Cloneable{  
  
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "superpool";
  public static final String COL_SUPERPOOLID = "superpoolID";
  public static final String COL_CHECKSUM = "checksum";
  public static final String COL_RANGES = "ranges";
  public static final String COL_STARTM = "startm";
  public static final String COL_ENDS = "ends";
  public static final String COL_LEASECOUNT = "leasecount";
  public static final String COL_CMTSIP = "cmtsip";
  public static final String COL_POOLTYPE = "pooltype";
  public static final String COL_CLUSTERNODE = "clusternode";
  public static final String COL_CFGTIMESTAMP = "cfgtimestamp";
  public static final String COL_PREFIXLENGTH = "prefixlength";
  public static final String COL_STATUS = "status";
  public static final String COL_SUBNETS = "subnets";


  
  @Column(name = COL_CHECKSUM, size=4096)
  private String checksum;

  @Column(name = COL_RANGES, size=4096)
  private String ranges;

  @Column(name = COL_STARTM, size=128)
  private String startm;

  @Column(name = COL_ENDS, size=128)
  private String ends;

  @Column(name = COL_SUPERPOOLID)
  private long superpoolID;

  @Column(name = COL_LEASECOUNT, size=128)
  private String leasecount;

  @Column(name = COL_CMTSIP, size=128)
  private String cmtsip;
  
  @Column(name = COL_POOLTYPE, size=128)
  private String pooltype;

  @Column(name = COL_CLUSTERNODE, size=128)
  private String clusternode;

  @Column(name = COL_CFGTIMESTAMP)
  private long cfgtimestamp;

  @Column(name = COL_PREFIXLENGTH)
  private int prefixlength;

  @Column(name = COL_STATUS, size=128)
  private String status;
  
  @Column(name = COL_SUBNETS, size=20000)
  private String subnets;


  
  public String getStartm() {
    return startm;
  }


  
  public void setStartm(String startm) {
    this.startm = startm;
  }


  
  public String getEnds() {
    return ends;
  }


  
  public void setEnds(String ends) {
    this.ends = ends;
  }


  
  public long getSuperpoolID() {
    return superpoolID;
  }


  
  public void setSuperpoolID(long superpoolID) {
    this.superpoolID = superpoolID;
  }


  
  
  public int getPrefixlength() {
    return prefixlength;
  }



  
  public void setPrefixlength(int prefixlength) {
    this.prefixlength = prefixlength;
  }



  
  public String getStatus() {
    return status;
  }



  
  public void setStatus(String status) {
    this.status = status;
  }



  public String getLeasecount() {
    return leasecount;
  }


  
  public void setLeasecount(String leasecount) {
    this.leasecount = leasecount;
  }


  
  public String getCmtsip() {
    return cmtsip;
  }


  
  public void setCmtsip(String cmtsip) {
    this.cmtsip = cmtsip;
  }


  
  public String getPooltype() {
    return pooltype;
  }


  
  public void setPooltype(String pooltype) {
    this.pooltype = pooltype;
  }


  
  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }
  
  public String getChecksum() {
    return checksum;
  }

  public void setRanges(String ranges) {
    this.ranges = ranges;
  }
  
  public String getRanges() {
    return ranges;
  }
  
  public String getClusternode() {
    return clusternode;
  }

  public void setClusternode(String clusternode) {
    this.clusternode = clusternode;
  }

  public long getCfgtimestamp() {
    return cfgtimestamp;
  }

  public void setCfgtimestamp(long cfgtimestamp) {
    this.cfgtimestamp = cfgtimestamp;
  }

  
  public String getSubnets() {
    return subnets;
  }



  public void setSubnets(String subnets) {
    this.subnets = subnets;
  }

  
  public SuperPool(){
    
  }
  
  
  @Override
  public Object getPrimaryKey() {
    return superpoolID;
  }
  
  private static class SuperpoolReader implements ResultSetReader<SuperPool> {

    public SuperPool read(ResultSet rs) throws SQLException {
      SuperPool h = new SuperPool();
      SuperPool.fillByResultSet(h, rs);
      return h;
    }
  }
  
  public static void fillByResultSet(SuperPool h, ResultSet rs) throws SQLException {
    h.startm = rs.getString(COL_STARTM);
    h.ends = rs.getString(COL_ENDS);
    h.superpoolID = rs.getLong(COL_SUPERPOOLID);
    h.leasecount = rs.getString(COL_LEASECOUNT);
    h.cmtsip = rs.getString(COL_CMTSIP);
    h.checksum = rs.getString(COL_CHECKSUM);
    h.pooltype = rs.getString(COL_POOLTYPE);
    h.ranges = rs.getString(COL_RANGES);
    h.clusternode = rs.getString(COL_CLUSTERNODE);
    h.cfgtimestamp = rs.getLong(COL_CFGTIMESTAMP);
    h.prefixlength = rs.getInt(COL_PREFIXLENGTH);
    h.status = rs.getString(COL_STATUS);
    h.subnets = rs.getString(COL_SUBNETS);

  }
  
  private static final SuperpoolReader reader = new SuperpoolReader();

  @Override
  public ResultSetReader<? extends SuperPool> getReader() {
    return reader;
  }

  @Override
  public <U extends SuperPool> void setAllFieldsFromData(U data2) {
    SuperPool data=data2;
    startm = data.startm;
    ends = data.ends;
    superpoolID = data.superpoolID;
    leasecount = data.leasecount;
    cmtsip = data.cmtsip;
    checksum = data.checksum;
    pooltype = data.pooltype;
    ranges = data.ranges;
    clusternode = data.clusternode;
    cfgtimestamp = data.cfgtimestamp;
    prefixlength = data.prefixlength;
    status = data.status;
    subnets = data.subnets;

  }

  public int compareTo(SuperPool other) {
    int ret = this.ranges.compareTo(other.ranges);
    if(ret==0)
    {
      
      ret = new Long(this.getCfgtimestamp()).compareTo(other.getCfgtimestamp());
    }
    
    return ret;
  }
  
  public boolean equalsKey(SuperPool other){
    return(cmtsip.equals(other.getCmtsip())&& pooltype.equals(other.getPooltype())&&prefixlength==(other.getPrefixlength()));
  }

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    return super.clone();
  }
  
}
