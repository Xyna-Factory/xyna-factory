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



@Persistable(primaryKey = DHCPv6EncodingAdm.COL_ID, tableName = DHCPv6EncodingAdm.TABLENAME)
public class DHCPv6EncodingAdm extends Storable<DHCPv6EncodingAdm> implements Comparable<DHCPv6EncodingAdm> {


  private static final long serialVersionUID = -3363326642316655775L;
  public final static String TABLENAME = "optionsv6adm";
  public final static String COL_ID = "id";
  public final static String COL_PARENTID = "parentid";
  public final static String COL_TYPENAME = "typename";
  public final static String COL_TYPEENCODING = "typeencoding";
  public final static String COL_ENTERPRISENR = "enterprisenr";
  public final static String COL_VALUEDATATYPENAME = "valuedatatypename";
  public final static String COL_VALUEDATATYPEARGUMENTSSTRING = "valuedatatypeargumentsstring";
  public final static String COL_READONLY = "readonly";
  public final static String COL_STATUS = "status";
  public final static String COL_GUINAME = "guiname";
  public final static String COL_GUIATTRIBUTE = "guiattribute";
  public final static String COL_GUIFIXEDATTRIBUTE = "guifixedattribute";
  public final static String COL_GUIPARAMETER = "guiparameter";
  public final static String COL_GUIATTRIBUTEID = "guiattributeid";
  public final static String COL_GUIFIXEDATTRIBUTEID = "guifixedattributeid";
  public final static String COL_GUIPARAMETERID = "guiparameterid";
  public final static String COL_GUIATTRIBUTEWERTEBEREICH = "guiattributewertebereich";
  public final static String COL_GUIFIXEDATTRIBUTEVALUE = "guifixedattributevalue";


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

  @Column(name = COL_READONLY, size = 50)
  private boolean readOnly;

  @Column(name = COL_STATUS, size = 50)
  private String statusflag;

  @Column(name = COL_GUINAME, size = 50)
  private String guiname;

  @Column(name = COL_GUIATTRIBUTE, size = 250)
  private String guiattribute;

  @Column(name = COL_GUIFIXEDATTRIBUTE, size = 250)
  private String guifixedattribute;

  @Column(name = COL_GUIPARAMETER, size = 250)
  private String guiparameter;

  @Column(name = COL_GUIATTRIBUTEID, size = 250)
  private int guiattributeid;

  @Column(name = COL_GUIFIXEDATTRIBUTEID, size = 250)
  private int guifixedattributeid;

  @Column(name = COL_GUIPARAMETERID, size = 250)
  private int guiparameterid;

  @Column(name = COL_GUIATTRIBUTEWERTEBEREICH, size = 250)
  private String guiattributewertebereich;

  @Column(name = COL_GUIFIXEDATTRIBUTEVALUE, size = 250)
  private String guifixedattributevalue;


  public DHCPv6EncodingAdm() // fuer Storable benoetigt
  {

  }


  public DHCPv6EncodingAdm(final int id, final Integer parentId, final String typeName, final long typeEncoding,
                           final Integer enterpriseNr, final String valueDataTypeName,
                           final Map<String, String> valueDataTypeArguments, boolean readOnly, final String statusflag,
                           final String guiname, final String guiattribute, final String guifixedattribute,
                           final String guiparameter, final int guiattributeid, final int guifixedattributeid, 
                           final int guiparameterid, final String guiattributewertebereich, final String guifixedattributevalue) {
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
      // throw new IllegalArgumentException("Illegal type encoding: <" + typeEncoding + ">.");
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
    // else if (enterpriseNr != null && enterpriseNr != 4491) {
    // throw new IllegalArgumentException("Enterprise with Nr: " + enterpriseNr + " not supported.");
    // }
    this.id = id;
    this.parentId = parentId;
    this.typeName = typeName;
    this.typeEncoding = typeEncoding;
    this.enterpriseNr = enterpriseNr;
    this.valueDataTypeName = valueDataTypeName;
    this.valueDataTypeArguments = validateAndMakeUnmodifiable(valueDataTypeArguments);
    this.readOnly = readOnly;
    this.statusflag = statusflag;
    this.guiname = guiname;
    this.guiattribute = guiattribute;
    this.guifixedattribute = guifixedattribute;
    this.guiparameter = guiparameter;
    this.guiattributeid = guiattributeid;
    this.guifixedattributeid = guifixedattributeid;
    this.guiparameterid = guiparameterid;
    this.guiattributewertebereich = guiattributewertebereich;
    this.guifixedattributevalue = guifixedattributevalue;
    

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


  public boolean getReadOnly() {
    return this.readOnly;
  }

  public String getStatusFlag() {
    return this.statusflag;
  }
  
  public String getGuiName() {
    return this.guiname;
  }
  
  public String getGuiAttribute()
  {
    return this.guiattribute;
  }
  
  public String getFixedGuiAttribute()
  {
    return this.guifixedattribute;
  }
  
  public String getGuiParameter()
  {
    return this.guiparameter;
  }
  
  public int getGuiAttributeId()
  {
    return this.guiattributeid;
  }
  
  public int getFixedGuiAttributeId()
  {
    return this.guifixedattributeid;
  }
  
  public int getGuiParameterId()
  {
    return this.guiparameterid;
  }
  
  public String getGuiAttributeWertebereich()
  {
    return this.guiattributewertebereich;
  }
  
  public String getFixedGuiAttributeValue()
  {
    return this.guifixedattributevalue;
  }
  
  public void setId(int id)
  {
    this.id=id;
  }
  
  public void setParentId(Integer pid)
  {
    this.parentId=pid;
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
    sb.append(">,readOnly:<");
    sb.append(this.readOnly);
    sb.append(">,statusFlag:<");
    sb.append(this.statusflag);
    sb.append(">,guiName:<");
    sb.append(this.guiname);
    sb.append(">,guiattribute:<");
    sb.append(this.guiattribute);
    sb.append(">,guifixedattribute:<");
    sb.append(this.guifixedattribute);
    sb.append(">,guiparameter:<");
    sb.append(this.guiparameter);
    sb.append(">,guiattributeid:<");
    sb.append(this.guiattributeid);
    sb.append(">,guifixedattributeid:<");
    sb.append(this.guifixedattributeid);
    sb.append(">,guiparameterid:<");
    sb.append(this.guiparameterid);
    sb.append(">,guiattributewertebereich:<");
    sb.append(this.guiattributewertebereich);
    sb.append(">,guifixedattributevalue:<");
    sb.append(this.guifixedattributevalue);

    sb.append(">}");
    return sb.toString();
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  private static ResultSetReader<DHCPv6EncodingAdm> reader = new ResultSetReader<DHCPv6EncodingAdm>() {

    public DHCPv6EncodingAdm read(ResultSet rs) throws SQLException {
      DHCPv6EncodingAdm e = new DHCPv6EncodingAdm();
      e.id = rs.getInt(COL_ID);

      e.parentId = rs.getInt(COL_PARENTID);
      if (rs.wasNull())
        e.parentId = null;

      e.typeName = rs.getString(COL_TYPENAME);

      e.typeEncoding = rs.getLong(COL_TYPEENCODING);
      // if (rs.wasNull())
      // e.typeEncoding = null;

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

      e.readOnly = rs.getBoolean(COL_READONLY);
      
      e.statusflag = rs.getString(COL_STATUS);
      e.guiname = rs.getString(COL_GUINAME);
      e.guiattribute = rs.getString(COL_GUIATTRIBUTE);
      e.guifixedattribute = rs.getString(COL_GUIFIXEDATTRIBUTE);
      e.guiparameter = rs.getString(COL_GUIPARAMETER);
   
      e.guiattributeid = rs.getInt(COL_GUIATTRIBUTEID);
      e.guifixedattributeid = rs.getInt(COL_GUIFIXEDATTRIBUTEID);
      e.guiparameterid = rs.getInt(COL_GUIPARAMETERID);

      e.guiattributewertebereich = rs.getString(COL_GUIATTRIBUTEWERTEBEREICH);
      e.guifixedattributevalue = rs.getString(COL_GUIFIXEDATTRIBUTEVALUE);

      return e;
    }

  };


  @Override
  public ResultSetReader<? extends DHCPv6EncodingAdm> getReader() {
    return reader;
  }


  @Override
  public <U extends DHCPv6EncodingAdm> void setAllFieldsFromData(U dataIn) {
    DHCPv6EncodingAdm data = dataIn;
    id = data.id;
    parentId = data.parentId;
    typeName = data.typeName;
    typeEncoding = data.typeEncoding;
    enterpriseNr = data.enterpriseNr;
    valueDataTypeName = data.valueDataTypeName;
    // valueDataTypeArguments = data.valueDataTypeArguments;
    valueDataTypeArgumentsString = data.valueDataTypeArgumentsString;
    readOnly = data.readOnly;

    statusflag = data.statusflag;
    guiname = data.guiname;
    guiattribute = data.guiattribute;
    guifixedattribute = data.guifixedattribute;
    guiparameter = data.guiparameter;
    
    guiattributeid = data.guiattributeid;
    guifixedattributeid = data.guifixedattributeid;
    guiparameterid = data.guiparameterid;
    
    guiattributewertebereich = data.guiattributewertebereich;
    guifixedattributevalue = data.guifixedattributevalue;

  }


  public int compareTo(DHCPv6EncodingAdm compareObject) {
    if (getId() < compareObject.getId())
      return -1;
    else if (getId() == compareObject.getId())
      return 0;
    else
      return 1;
  }


}
