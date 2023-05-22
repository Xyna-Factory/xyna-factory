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
package xact.ssh;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

/*
 * Format Erkl�rungen:
 * http://man.openbsd.org/sshd.8
 */
@Persistable(tableName = HostKeyStorable.TABLE_NAME, primaryKey = HostKeyStorable.COL_ID)
public class HostKeyStorable extends Storable<HostKeyStorable> {
  
  private static final long serialVersionUID = 9077661986329920786L;
  
  public final static String TABLE_NAME = "hostkeyrepository";
  public final static String COL_ID = "id";
  public final static String COL_NAME = "name";
  public final static String COL_PUBLICKEY = "publickey";
  public final static String COL_TYPE = "type";
  public final static String COL_HASHED = "hashed";
  public final static String COL_FUZZY = "fuzzy";
  public final static String COL_COMMENT = "comment";
  
  @Column(name = COL_ID)
  private long id;
  
  // FIXME reintroduce indizes when testing on live machine
  
  //@Column(name = COL_NAME, index = IndexType.MULTIPLE)
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_PUBLICKEY, size = Integer.MAX_VALUE)
  private String publickey;
  
  @Column(name = COL_TYPE)
  private String type;
  
  //@Column(name = COL_HASHED, index = IndexType.MULTIPLE)
  @Column(name = COL_HASHED)
  private String hashed;
  
  //@Column(name = COL_FUZZY, index = IndexType.MULTIPLE)
  @Column(name = COL_FUZZY)
  private String fuzzy;
  
  @Column(name = COL_COMMENT)
  private String comment;
  
  
  private transient Pattern[] fuzzyPatterns;
  
  
  public HostKeyStorable() {
  }
  
  
  // TODO ensure not to waste id's check call hierarchy
  public HostKeyStorable(String name, String type, String publickey, boolean hashed, String comment) { // overload with byte[] key ?
    this();
    this.name = name;
    this.type = type;
    this.publickey = publickey;
    this.fuzzy = Boolean.toString(isFuzzyIdentifier(name));
    this.hashed = Boolean.toString(hashed);
    if (comment == null) {
      comment = "";
    }
    this.comment = comment;
    try {
      this.id = IDGenerator.getInstance().getUniqueId();
    } catch (XynaException e) {
      throw new RuntimeException("Could not generate unique id",e);
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

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }

  
  public String getPublickey() {
    return publickey;
  }
  
  
  public void setPublickey(String publickey) {
    this.publickey = publickey;
  }

  
  public String getType() {
    return type;
  }

  
  public void setType(String type) {
    this.type = type;
  }
  
  
  public String getHashed() {
    return hashed;
  }
  
  
  public void setHashed(String hashed) {
    this.hashed = hashed;
  }
  
  
  public boolean isHashed() {
    return Boolean.parseBoolean(hashed);
  }
  
  
  public String getFuzzy() {
    return fuzzy;
  }
  
  
  public void setFuzzy(String fuzzy) {
    this.fuzzy = fuzzy;
  }
  
  
  public boolean isFuzzy() {
    return Boolean.parseBoolean(fuzzy);
  }
  
  
  public String getComment() {
    return comment;
  }
  
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  
  public Pattern[] getFuzzyPatterns() {
    if (isFuzzy()) {
      if (fuzzyPatterns == null) {
        String[] names = name.split(",");
        Pattern[] patterns = new Pattern[names.length];
        for (int i = 0; i < names.length; i++) {
          String regexpedName = names[i].replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
          if (regexpedName.startsWith("!")) {
            regexpedName = "(?" + regexpedName + ")";
          } 
          regexpedName = "^" + regexpedName + "$";
          patterns[i] = Pattern.compile(regexpedName);
        }
        fuzzyPatterns = patterns;
      }
      return fuzzyPatterns;
    } else {
      return null;
    }
  }
  
  
  /*HostKey getHostKeyRepresentation() {
    if (jschRepresentation == null) {
      jschRepresentation = JSchUtil.instantiateHashedHostKey(name, getTypeAsEnum(), publickey, null, false); // don't ever hash, either it is hashed already or it shouldn't
    }
    return jschRepresentation;
  }*/
  
  
  public static final ResultSetReader<HostKeyStorable> reader = new HostKeyStorableResultSetReader();
  
  @Override
  public ResultSetReader<? extends HostKeyStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends HostKeyStorable> void setAllFieldsFromData(U data) {
    HostKeyStorable cast = data;
    this.id = cast.id;
    this.name = cast.name;
    this.publickey = cast.publickey;
    this.type = cast.type;
    this.hashed = cast.hashed;
    this.fuzzy = cast.fuzzy;
    this.comment = cast.comment;
  }
  
  
  private static class HostKeyStorableResultSetReader implements ResultSetReader<HostKeyStorable> {

    public HostKeyStorable read(ResultSet rs) throws SQLException {
      HostKeyStorable result = new HostKeyStorable();
      result.id = rs.getLong(COL_ID);
      result.name = rs.getString(COL_NAME);
      result.publickey = rs.getString(COL_PUBLICKEY);
      result.type = rs.getString(COL_TYPE);
      result.hashed = rs.getString(COL_HASHED);
      result.fuzzy = rs.getString(COL_FUZZY);
      result.comment = rs.getString(COL_COMMENT);
      return result;
    }

  }
  
  
  private static final Pattern fuzzyPattern = Pattern.compile("[*?!]");
  
  public static boolean isFuzzyIdentifier(String name) {
    Matcher fuzzyMatcher = fuzzyPattern.matcher(name);
    return fuzzyMatcher.find();
  }
  
  /*
     Alternately, hostnames may be stored in a hashed form which hides host
     names and addresses should the file's contents be disclosed.  Hashed
     hostnames start with a `|' character.  Only one hashed hostname may
     appear on a single line and none of the above negation or wildcard
     operators may be applied.
   */
  public static boolean canBeHashed(String hostname) {
    return !(isFuzzyIdentifier(hostname) || hostname.contains(","));
  }
  
  
  public boolean isHostNameList() {
    return !isHashed() && name.contains(",");
  }
  

  public void removeFromHostNameList(String singleHostname) {
    if (isHashed() || !isHostNameList()) {
      return;
    }
    String newHostName = StringUtils.removeFromSeperatedList(name, singleHostname, ",", false);
    if (newHostName.equals(name) && (singleHostname.startsWith("[") && singleHostname.contains("]:"))) {
      newHostName = StringUtils.removeFromSeperatedList(name, singleHostname.substring(1, singleHostname.indexOf("]:")), ",", false);
    }
    this.name = newHostName;
  }
  

}
