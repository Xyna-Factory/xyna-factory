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

package xdnc.dhcpv6.tlvdatabase;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = DHCPv6Encoding.COL_ID, tableName = DHCPv6Encoding.TABLENAME)
public class DHCPv6Encoding extends Storable<DHCPv6Encoding> implements Comparable<DHCPv6Encoding> {


  private static final long serialVersionUID = -3363326642316655775L;
  public final static String TABLENAME = "optionsv6";
  public final static String COL_ID = "id";
  public final static String COL_PARENTID = "parentid";
  public final static String COL_TYPENAME = "typename";
  public final static String COL_TYPEENCODING = "typeencoding";
  public final static String COL_ENTERPRISENR = "enterprisenr";
  public final static String COL_VALUEDATATYPENAME = "valuedatatypename";
  public final static String COL_VALUEDATATYPEARGUMENTSSTRING = "valuedatatypeargumentsstring";
  public final static String COL_READONLY = "readonly";

  @Column(name = COL_ID, size = 50)
  private int id;

  @Column(name = COL_PARENTID, size = 50)
  private Integer parentId;

  @Column(name = COL_TYPENAME, size = 50)
  private String typeName;

  @Column(name = COL_TYPEENCODING, size = 50)
  private long typeEncoding;

  @Column(name = COL_ENTERPRISENR, size = 50)
  private Integer enterpriseNr;

  @Column(name = COL_VALUEDATATYPENAME, size = 50)
  private String valueDataTypeName;

  private Map<String, String> valueDataTypeArguments;

  @Column(name = COL_VALUEDATATYPEARGUMENTSSTRING, size = 50)
  private String valueDataTypeArgumentsString;


  public DHCPv6Encoding() // fuer Storable benoetigt
  {

  }


  public DHCPv6Encoding(final int id, final Integer parentId, final String typeName, final long typeEncoding,
                        final Integer enterpriseNr, final String valueDataTypeName,
                        final Map<String, String> valueDataTypeArguments) {
    if (parentId != null && parentId.equals(id)) {
      throw new IllegalArgumentException("Parent id may not be same as id.");
    }
    else if (typeName == null) {
      throw new IllegalArgumentException("Type name may not be null.");
    }
    else if ("".equals(typeName)) {
      throw new IllegalArgumentException("Type name may not be empty string.");
    }
    else if (typeEncoding < 0 || typeEncoding > 65536) {
      //throw new IllegalArgumentException("Illegal type encoding: <" + typeEncoding + ">.");
    }
    else if (valueDataTypeName == null) {
      throw new IllegalArgumentException("Value data type name may not be null.");
    }
    else if (!valueDataTypeName.matches("[A-Z][A-Za-z0-9]*")) {
      throw new IllegalArgumentException("IllegalDataTypeName: <" + valueDataTypeName + ">.");
    }
    else if (valueDataTypeArguments == null) {
      throw new IllegalArgumentException("Value data type arguments may not be null.");
    }
    else if (enterpriseNr != null && enterpriseNr != 4491) {
      throw new IllegalArgumentException("Enterprise with Nr: " + enterpriseNr + " not supported.");
    }
    this.id = id;
    this.parentId = parentId;
    this.typeName = typeName;
    this.typeEncoding = typeEncoding;
    this.enterpriseNr = enterpriseNr;
    this.valueDataTypeName = valueDataTypeName;
    this.valueDataTypeArguments = validateAndMakeUnmodifiable(valueDataTypeArguments);

    String tmp = this.valueDataTypeArguments.toString();
    if (!tmp.equals("{}") && !tmp.contains("\"")) {
      tmp = tmp.replace("{", "{\"");
      tmp = tmp.replace("}", "\"}");
      tmp = tmp.replace("=", "\"=\"");
    }


    this.valueDataTypeArgumentsString = tmp;
  }


  private static Map<String, String> validateAndMakeUnmodifiable(final Map<String, String> valueDataTypeArguments) {
    Map<String, String> response = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : valueDataTypeArguments.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key == null) {
        throw new IllegalArgumentException("Found null value key in value data type arguments.");
      }
      else if (value == null) {
        throw new IllegalArgumentException("Found key with value null in value data type arguments.");
      }
      response.put(key, value);
    }
    return Collections.unmodifiableMap(response);
  }


  public int getId() {
    return this.id;
  }


  public Integer getParentId() {
    return this.parentId;
  }


  public String getTypeName() {
    return this.typeName;
  }


  public long getTypeEncoding() {
    return this.typeEncoding;
  }


  public Integer getEnterpriseNr() {
    return this.enterpriseNr;
  }


  public String getValueDataTypeName() {
    return this.valueDataTypeName;
  }


  public Map<String, String> getValueDataTypeArguments() {
    return this.valueDataTypeArguments;
  }


  public String getValueDataTypeArgumentsString() {
    return this.valueDataTypeArgumentsString;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{id<");
    sb.append(this.id);
    sb.append(">,parentId:<");
    sb.append(this.parentId);
    sb.append(">,typeName:<");
    sb.append(this.typeName);
    sb.append(">,typeEncoding:<");
    sb.append(this.typeEncoding);
    sb.append(">,enterpriseNr:<");
    sb.append(this.enterpriseNr);
    sb.append(">,valueDataTypeName:<");
    sb.append(this.valueDataTypeName);
    sb.append(">,valueDataTypeArguments:<");
    sb.append(this.valueDataTypeArguments);
    sb.append(">}");
    return sb.toString();
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  private static ResultSetReader<DHCPv6Encoding> reader = new ResultSetReader<DHCPv6Encoding>() {

    public DHCPv6Encoding read(ResultSet rs) throws SQLException {
      DHCPv6Encoding e = new DHCPv6Encoding();
      e.id = rs.getInt(COL_ID);

      e.parentId = rs.getInt(COL_PARENTID);
      if (rs.wasNull())
        e.parentId = null;

      e.typeName = rs.getString(COL_TYPENAME);

      e.typeEncoding = rs.getLong(COL_TYPEENCODING);
//      if (rs.wasNull())
//        e.typeEncoding = null;

      e.enterpriseNr = rs.getInt(COL_ENTERPRISENR);
      if (rs.wasNull())
        e.enterpriseNr = null;

      e.valueDataTypeName = rs.getString(COL_VALUEDATATYPENAME);

      /*
       * e.valueDataTypeArguments = (Map<String, String>) e .readBlobbedJavaObjectFromResultSet(rs,
       * COL_VALUEDATATYPEARGUMENTS);
       */


      e.valueDataTypeArgumentsString = rs.getString(COL_VALUEDATATYPEARGUMENTSSTRING);

      String tmp = e.valueDataTypeArgumentsString;
      tmp = tmp.replace("{", "");
      tmp = tmp.replace("}", "");
      tmp = tmp.replace(" ", "");


      e.valueDataTypeArguments = StringToMapUtil.toMap(tmp);

      return e;
    }

  };


  @Override
  public ResultSetReader<? extends DHCPv6Encoding> getReader() {
    return reader;
  }


  @Override
  public <U extends DHCPv6Encoding> void setAllFieldsFromData(U dataIn) {
    DHCPv6Encoding data = dataIn;
    id = data.id;
    parentId = data.parentId;
    typeName = data.typeName;
    typeEncoding = data.typeEncoding;
    enterpriseNr = data.enterpriseNr;
    valueDataTypeName = data.valueDataTypeName;
    // valueDataTypeArguments = data.valueDataTypeArguments;
    valueDataTypeArgumentsString = data.valueDataTypeArgumentsString;


  }


  public int compareTo(DHCPv6Encoding compareObject) {
    if (getId() < compareObject.getId())
      return -1;
    else if (getId() == compareObject.getId())
      return 0;
    else
      return 1;
  }


}
