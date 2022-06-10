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
package com.gip.xyna.persistence.xsor.helper;

import com.gip.xyna.xsor.protocol.XSORPayload;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(tableName=ProjectTestStorable.TABLENAME, primaryKey="ip")
public class ProjectTestStorable extends Storable<ProjectTestStorable> implements XSORPayload {

  private static final long serialVersionUID = 871967730799987854L;
  
  public static final String TABLENAME = "leasestable";
  public static final String COL_IP = "ip";
  public static final String COL_PREFIXLENGTH = "prefixlength";
  public static final String COL_MAC = "mac";
  public static final String COL_RESERVATIONTIME = "reservationEnd";
  public static final String COL_IAID = "iaid";
  public static final String COL_CMREMOTEID = "cmremoteid";
  
  public static final String COL_SUPERPOOLID = "superpoolid";
  public static final String COL_BINDING = "binding";
  public static final String COL_EXPIRATIONTIME ="expirationTime";
  
  public static final String COL_CMTSREMOTEID = "cmtsremoteid";
  public static final String COL_CMTSIP = "cmtsip";
  
  @Column(name = COL_IP)
  private String ip;
  
  @Column(name = COL_CMTSREMOTEID)
  private String cmtsremoteid;

  @Column(name = COL_CMTSIP)
  private String cmtsip;

  @Column(name = COL_PREFIXLENGTH)
  private int prefixlength;

  @Column(name = COL_MAC)
  private String mac;

  @Column(name = COL_RESERVATIONTIME)
  private long reservationEnd;

  @Column(name = COL_IAID)
  private String iaid;

  @Column(name = COL_CMREMOTEID)
  private String cmremoteId;

  @Column(name = COL_SUPERPOOLID)
  private long superpoolid;

  @Column(name = COL_BINDING)
  private String binding;

  @Column(name = COL_EXPIRATIONTIME)
  private long expirationtime;

  
  public ProjectTestStorable() { }
  
  public ProjectTestStorable(String ip) { 
    this.ip = ip;
  }
  
  
  public ProjectTestStorable(String binding, long superpoolid, long reservationtime, long expirationtime) { 
    this.binding = binding;
    this.superpoolid = superpoolid;
    this.reservationEnd = reservationtime;
    this.expirationtime = expirationtime;
  }
  
  public ProjectTestStorable(String mac, String iaid, long expirationtime, long superpoolid) { 
    this.mac = mac;
    this.iaid = iaid;
    this.superpoolid = superpoolid;
    this.expirationtime = expirationtime;
  }
  

  public void copyIntoByteArray(byte[] ba, int offset) {
    // irrelevant
  }


  public XSORPayload copyFromByteArray(byte[] ba, int offset) {
    // irrelevant
    return null;
  }


  public byte[] pkToByteArray(Object o) {
    // irrelevant
    return null;
  }


  public Object byteArrayToPk(byte[] ba) {
    // irrelevant
    return null;
  }


  @Override
  public Object getPrimaryKey() {
    return ip;
  }


  @Override
  public ResultSetReader<? extends ProjectTestStorable> getReader() {
    // irrelevant
    return null;
  }


  @Override
  public <U extends ProjectTestStorable> void setAllFieldsFromData(U arg0) {
    // irrelevant
  }


  
  public String getIp() {
    return ip;
  }


  
  public String getCmtsremoteid() {
    return cmtsremoteid;
  }


  
  public String getCmtsip() {
    return cmtsip;
  }


  
  public int getPrefixlength() {
    return prefixlength;
  }


  
  public String getMac() {
    return mac;
  }


  
  public long getReservationEnd() {
    return reservationEnd;
  }


  
  public String getIaid() {
    return iaid;
  }


  
  public String getCmremoteId() {
    return cmremoteId;
  }


  
  public long getSuperpoolid() {
    return superpoolid;
  }


  
  public String getBinding() {
    return binding;
  }


  
  public long getExpirationtime() {
    return expirationtime;
  }


  
  public void setIp(String ip) {
    this.ip = ip;
  }


  
  public void setCmtsremoteid(String cmtsremoteid) {
    this.cmtsremoteid = cmtsremoteid;
  }


  
  public void setCmtsip(String cmtsip) {
    this.cmtsip = cmtsip;
  }


  
  public void setPrefixlength(int prefixlength) {
    this.prefixlength = prefixlength;
  }


  
  public void setMac(String mac) {
    this.mac = mac;
  }


  
  public void setReservationEnd(long reservationEnd) {
    this.reservationEnd = reservationEnd;
  }


  
  public void setIaid(String iaid) {
    this.iaid = iaid;
  }


  
  public void setCmremoteId(String cmremoteId) {
    this.cmremoteId = cmremoteId;
  }


  
  public void setSuperpoolid(long superpoolid) {
    this.superpoolid = superpoolid;
  }


  
  public void setBinding(String binding) {
    this.binding = binding;
  }


  
  public void setExpirationtime(long expirationtime) {
    this.expirationtime = expirationtime;
  }

}
