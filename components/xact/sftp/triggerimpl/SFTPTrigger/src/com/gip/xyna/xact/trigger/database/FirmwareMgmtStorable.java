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
package com.gip.xyna.xact.trigger.database;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = FirmwareMgmtStorable.TABLE_NAME, primaryKey = FirmwareMgmtStorable.COL_ID)
public class FirmwareMgmtStorable extends Storable<FirmwareMgmtStorable> {

  private static final long serialVersionUID = 9077661986329920786L;

  public final static String TABLE_NAME = "firmwaremgmt";
  public final static String COL_ID = "id";
  public final static String COL_NAME = "name";
  public final static String COL_FIRMWAREVERSION = "firmwareversion";
  public final static String COL_ISDEFAULT = "isdefault";
  public final static String COL_LASTDEFAULT = "lastdefault";
  public final static String COL_DEVICETYPEID = "devicetypeid";
  public final static String COL_URL = "url";
  public final static String COL_UPLOADDATE = "uploaddate";
  public final static String COL_LASTCHANGEDATE = "lastchangedate";
  public final static String COL_XYNACONTENTID = "xynacontentid";

  @Column(name = COL_ID)
  private long id;


  // @Column(name = COL_NAME, index = IndexType.MULTIPLE)
  @Column(name = COL_NAME)
  private String name;

  @Column(name = COL_FIRMWAREVERSION)
  private String firmwareversion;

  @Column(name = COL_ISDEFAULT)
  private boolean isdefault;

  @Column(name = COL_LASTDEFAULT)
  private String lastdefault;

  @Column(name = COL_DEVICETYPEID)
  private long devicetypeid;

  @Column(name = COL_URL)
  private String url;

  @Column(name = COL_UPLOADDATE)
  private long uploaddate;

  @Column(name = COL_LASTCHANGEDATE)
  private long lastchangedate;

  @Column(name = COL_XYNACONTENTID)
  private long xynacontentid;


  public FirmwareMgmtStorable() {
  }


  public FirmwareMgmtStorable(String name, String firmwareversion, boolean isdefault, String lastdefault, long devicetypeid, String url, long uploaddate, long lastchangedate, long xynacontentid) {
    this.setName(name);
    this.setFirmwareversion(firmwareversion);
    this.setIsdefault(isdefault);
    this.setLastdefault(lastdefault);
    this.setDevicetypeid(devicetypeid);
    this.setUrl(url);
    this.setUploaddate(uploaddate);
    this.setLastchangedate(lastchangedate);
    this.setXynacontentid(xynacontentid);
    try {
      this.id = IDGenerator.getInstance().getUniqueId();
    }
    catch (XynaException e) {
      throw new RuntimeException("Could not generate unique id", e);
    }
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public long getId() {
    return id;
  }


  public void setIdentifier(long id) {
    this.id = id;
  }



  public static final ResultSetReader<FirmwareMgmtStorable> reader = new FileContentStorableResultSetReader();


  @Override
  public ResultSetReader<? extends FirmwareMgmtStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends FirmwareMgmtStorable> void setAllFieldsFromData(U dataIn) {
    FirmwareMgmtStorable data = dataIn;
    this.id = data.id;
    this.name = data.name;
    this.firmwareversion = data.firmwareversion;
    this.isdefault = data.isdefault;
    this.lastdefault = data.lastdefault;
    this.devicetypeid = data.devicetypeid;
    this.url = data.url;
    this.uploaddate = data.uploaddate;
    this.lastchangedate = data.lastchangedate;
    this.xynacontentid = data.xynacontentid;
  }




  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }




  public String getFirmwareversion() {
    return firmwareversion;
  }


  public void setFirmwareversion(String firmwareversion) {
    this.firmwareversion = firmwareversion;
  }




  public boolean isDefault() {
    return isdefault;
  }


  public void setIsdefault(boolean isdefault) {
    this.isdefault = isdefault;
  }




  public String getLastdefault() {
    return lastdefault;
  }


  public void setLastdefault(String lastdefault) {
    this.lastdefault = lastdefault;
  }




  public long getDevicetypeid() {
    return devicetypeid;
  }


  public void setDevicetypeid(long devicetypeid) {
    this.devicetypeid = devicetypeid;
  }




  public String getUrl() {
    return url;
  }


  public void setUrl(String url) {
    this.url = url;
  }




  public long getUploaddate() {
    return uploaddate;
  }


  public void setUploaddate(long uploaddate) {
    this.uploaddate = uploaddate;
  }




  public long getLastchangedate() {
    return lastchangedate;
  }


  public void setLastchangedate(long lastchangedate) {
    this.lastchangedate = lastchangedate;
  }




  public long getXynacontentid() {
    return xynacontentid;
  }


  public void setXynacontentid(long xynacontentid) {
    this.xynacontentid = xynacontentid;
  }




  private static class FileContentStorableResultSetReader implements ResultSetReader<FirmwareMgmtStorable> {

    public FirmwareMgmtStorable read(ResultSet rs) throws SQLException {
      FirmwareMgmtStorable result = new FirmwareMgmtStorable();
      result.id = rs.getLong(COL_ID);
      result.name = rs.getString(COL_NAME);
      result.firmwareversion = rs.getString(COL_FIRMWAREVERSION);
      result.isdefault = rs.getBoolean(COL_ISDEFAULT);
      result.lastdefault = rs.getString(COL_LASTDEFAULT);
      result.devicetypeid = rs.getLong(COL_DEVICETYPEID);
      result.url = rs.getString(COL_URL);
      result.uploaddate = rs.getLong(COL_UPLOADDATE);
      result.lastchangedate = rs.getLong(COL_LASTCHANGEDATE);
      result.xynacontentid = rs.getLong(COL_XYNACONTENTID);
      return result;
    }
  }
}
