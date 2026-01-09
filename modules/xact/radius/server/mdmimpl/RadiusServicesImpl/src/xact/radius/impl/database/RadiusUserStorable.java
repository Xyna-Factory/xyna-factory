/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.radius.impl.database;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.IndexType;



@Persistable(primaryKey = RadiusUserStorable.COL_ID, tableName = RadiusUserStorable.TABLENAME)
public class RadiusUserStorable extends Storable<RadiusUserStorable> {


  private static final long serialVersionUID = -3363326642316655775L;
  public final static String TABLENAME = "radiususer";
  public final static String COL_ID = "id";
  public final static String COL_USERNAME = "username";
  public final static String COL_USERPASSWORD = "userpassword";
  public final static String COL_SHAREDSECRET = "sharedsecret";
  public final static String COL_SERVICETYPE = "servicetype";
  public final static String COL_TIMESTAMP = "timestamp";
  public final static String COL_ROLE = "role";
  public final static String COL_IP = "ip";

  @Column(name = COL_ID, size = 50)
  private long id;

  @Column(name = COL_USERNAME, size = 50, index = IndexType.MULTIPLE)
  private String username;

  @Column(name = COL_USERPASSWORD, size = 50)
  private String userpassword;

  @Column(name = COL_SHAREDSECRET, size = 50)
  private String sharedsecret;

  @Column(name = COL_SERVICETYPE, size = 50)
  private String servicetype;

  @Column(name = COL_TIMESTAMP, size = 50)
  private long timestamp;

  @Column(name = COL_ROLE, size = 50)
  private String role;

  @Column(name = COL_IP, size = 50)
  private String ip;


  public RadiusUserStorable() // fuer Storable benoetigt
  {

  }


  public RadiusUserStorable(final long id, final String username, final String userpassword, final String sharedsecret) {
    this(id, username, userpassword, sharedsecret, "", 0, "", "");
  }


  public RadiusUserStorable(final long id, final String username, final String userpassword, final String sharedsecret,
                            final String servicetype, final long timestamp, final String role, final String ip) {
    if (username == null) {
      throw new IllegalArgumentException("Username may not be null.");
    } else if ("".equals(username)) {
      throw new IllegalArgumentException("Username may not be empty string.");
    }

    if (userpassword == null) {
      throw new IllegalArgumentException("Userpassword may not be null.");
    } else if ("".equals(userpassword)) {
      throw new IllegalArgumentException("Userpassword may not be empty string.");
    }

    if (sharedsecret == null) {
      throw new IllegalArgumentException("Sharedsecret may not be null.");
    } else if ("".equals(sharedsecret)) {
      throw new IllegalArgumentException("Sharedsecret may not be empty string.");
    }


    this.id = id;
    this.username = username;
    this.userpassword = userpassword;
    this.sharedsecret = sharedsecret;
    this.servicetype = servicetype;
    this.timestamp = timestamp;
    this.role = role;
    this.ip = ip;
  }


  public long getId() {
    return this.id;
  }


  public String getUserName() {
    return this.username;
  }


  public String getUserPassword() {
    return this.userpassword;
  }


  public String getSharedSecret() {
    return this.sharedsecret;
  }


  public String getServiceType() {
    return this.servicetype;
  }


  public long getTimestamp() {
    return this.timestamp;
  }


  public String getRole() {
    return this.role;
  }


  public String getIp() {
    return this.ip;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{id<");
    sb.append(this.id);
    sb.append(">,userName:<");
    sb.append(this.username);
    sb.append(">,userPassword:<");
    sb.append(this.userpassword);
    sb.append(">,sharedSecret:<");
    sb.append(this.sharedsecret);
    sb.append(">,serviceType:<");
    sb.append(this.servicetype);
    sb.append(">,timestamp:<");
    sb.append(this.timestamp);
    sb.append(">,role:<");
    sb.append(this.role);
    sb.append(">,ip:<");
    sb.append(this.ip);
    sb.append(">}");
    return sb.toString();
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  private static ResultSetReader<RadiusUserStorable> reader = new ResultSetReader<RadiusUserStorable>() {

    public RadiusUserStorable read(ResultSet rs) throws SQLException {
      RadiusUserStorable e = new RadiusUserStorable();
      e.id = rs.getLong(COL_ID);
      e.username = rs.getString(COL_USERNAME);
      e.userpassword = rs.getString(COL_USERPASSWORD);
      e.sharedsecret = rs.getString(COL_SHAREDSECRET);
      e.servicetype = rs.getString(COL_SERVICETYPE);
      e.timestamp = rs.getLong(COL_TIMESTAMP);
      e.role = rs.getString(COL_ROLE);
      e.ip = rs.getString(COL_IP);

      return e;
    }

  };


  @Override
  public ResultSetReader<? extends RadiusUserStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends RadiusUserStorable> void setAllFieldsFromData(U data) {
    RadiusUserStorable data2 = (RadiusUserStorable) data;
    id = data2.id;
    username = data2.username;
    userpassword = data2.userpassword;
    sharedsecret = data2.sharedsecret;
    servicetype = data2.servicetype;
    timestamp = data2.timestamp;
    role = data2.role;
    ip = data2.ip;

  }
}
