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
package xfmg.xfctrl.datamode.json.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xfmg.xfctrl.datamodel.json.impl.InvalidJSONException;
import xfmg.xfctrl.datamodel.json.impl.JSONDatamodelServicesServiceOperationImpl;
import xfmg.xfctrl.datamodel.json.impl.JSONParser;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;

import com.gip.xyna.ObjectStringRepresentation;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import junit.framework.TestCase;


public class JSONTestWithOptions extends TestCase {

  public void testWithOptions() throws IllegalArgumentException, IllegalAccessException {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString =
                  "{\"accountStatus\":\"active\",\"spIrmUserAuthz\":{\"allTenants\":{\"287\":{\"name\":\"287\",\"roles\":{\"portal.tenant.member\":{\"name\":\"portal.tenant.member\"},\"dsc.resource.tenant.services\":{\"name\":\"dsc.resource.tenant.services\"},\"portal.tenant.access_authorizer\":{\"name\":\"portal.tenant.access_authorizer\"}}}}},\"mail\":\"baum@mail.com\",\"spIrmTenantId\":[\"287\"],\"givenName\":\"jo07sKkP\",\"effectiveAssignments\":{},\"mobile\":\"+4911111111111\",\"_rev\":\"3\",\"effectiveRoles\":[],\"sn\":\"FF7w6l4h\",\"_id\":\"f92b5d31-5f29-46ee-8c81-b947f01db13f\",\"gotoUrl\":\"https://test.de/\",\"spSupportCodeWord\":null}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      StringBuilder sb = new StringBuilder();
      JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
      UserXO user = new UserXO();
      Map<String, String> trans = new HashMap<String, String>();
      trans.put("roles", "name");
      trans.put("roles[].roles", "name");
      impl.fillXynaObjectRecursivly(user, job, "", trans, Collections.<String, String>emptyMap());
      ObjectStringRepresentation.createStringRepOfObject(sb, job);
      sb.append("\n=========================================\n\n");
      ObjectStringRepresentation.createStringRepOfObject(sb, user);
      System.out.println(sb);
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 13. Cause: Too may commas.", e.getMessage());
    }

  }
  
  
  public void testWithOptions2() throws IllegalArgumentException, IllegalAccessException {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"{"+
"  \"mail\": \"john.doe@acme.com\","+
"  \"telephoneNumber\": \"+321234567692\","+
"  \"gotoUrl\": \"https://test.de/\","+
"  \"roles\": {"+
"    \"1729\": {"+
"      \"tenant\": \"1729\","+
"      \"roles\": {"+
"        \"dsc.role.tenant.member\": {"+
"          \"name\": \"my.role.tenant.member\""+
"        },"+
"        \"dsc.role.tenant.access_authorizer\": {"+
"          \"name\": \"my.role.tenant.access_authorizer\""+
"        }"+
"      }"+
"    }"+
"  }"+
"}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      StringBuilder sb = new StringBuilder();
      JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
      UserXO user = new UserXO();
      Map<String, String> trans = new HashMap<String, String>();
      trans.put("roles", "name");
      trans.put("roles[].roles", "name");
      Map<String, String> subs = new HashMap<String, String>();
      subs.put("tenant", "roles[].name");
      impl.fillXynaObjectRecursivly(user, job, "", trans, subs);
      ObjectStringRepresentation.createStringRepOfObject(sb, job);
      sb.append("\n=========================================\n\n");
      ObjectStringRepresentation.createStringRepOfObject(sb, user);
      System.out.println(sb);
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 13. Cause: Too may commas.", e.getMessage());
    }

  }
  
  
  public void testWriteWithOptions() throws IllegalArgumentException, IllegalAccessException {
    RoleXO role1 = new RoleXO();
    role1.name = "my.role.tenant.member";
    RoleXO role2 = new RoleXO();
    role2.name = "my.role.tenant.access_authorizer";
    AuthorizationXO auth = new AuthorizationXO();
    auth.name = "1729";
    ArrayList<RoleXO> list = new ArrayList<RoleXO>();
    list.add(role1);
    list.add(role2);
    auth.roles = list;
    UserXO user = new UserXO();
    user.gotoUrl = "https://test.de/";
    user.telephoneNumber = "+321234567692";
    user.mail = "john.doe@acme.com";
    user.roles = Collections.singletonList(auth);
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    trans.put("roles", "name");
    trans.put("roles[].roles", "name");
    JSONObject obj = impl.createFromXynaObjectRecursivly(user, "", trans, Collections.<String, String>emptyMap());
    System.out.println(obj.toJSON(""));
  }
  
  public void testWriteWithOptions2() throws IllegalArgumentException, IllegalAccessException {
    RoleXO role1 = new RoleXO();
    role1.name = "my.role.tenant.member";
    RoleXO role2 = new RoleXO();
    role2.name = "my.role.tenant.access_authorizer";
    AuthorizationXO auth = new AuthorizationXO();
    auth.name = "1729";
    ArrayList<RoleXO> list = new ArrayList<RoleXO>();
    list.add(role1);
    list.add(role2);
    auth.roles = list;
    UserXO user = new UserXO();
    user.gotoUrl = "https://test.de/";
    user.telephoneNumber = "+321234567692";
    user.mail = "john.doe@acme.com";
    user.roles = Collections.singletonList(auth);
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    trans.put("roles", "name");
    trans.put("roles[].roles", "name");
    Map<String, String> subs = new HashMap<String, String>();
    subs.put("roles[].name", "tenant");
    JSONObject obj = impl.createFromXynaObjectRecursivly(user, "", trans, subs);
    System.out.println(obj.toJSON(""));
  }
  
  
  public static class UserXO extends BaseTestXO {

    private String mail;
    private String telephoneNumber;
    private String gotoUrl;
    private List<AuthorizationXO> roles;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("mail");
      set.add("telephoneNumber");
      set.add("gotoUrl");
      set.add("roles");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("mail")) {
        return mail;
      } else if (path.equals("telephoneNumber")) {
        return telephoneNumber;
      } else if (path.equals("gotoUrl")) {
        return gotoUrl;
      } else if (path.equals("roles")) {
        return roles;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("mail")) {
        mail = (String) value;
      } else if (path.equals("telephoneNumber")) {
        telephoneNumber = (String) value;
      } else if (path.equals("gotoUrl")) {
        gotoUrl = (String) value;
      } else if (path.equals("roles")) {
        roles = (List<AuthorizationXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
    
  }
  
public static class AuthorizationXO extends BaseTestXO {
    
    private String name;
    private List<RoleXO> roles;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("name");
      set.add("roles");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("name")) {
        return name ;
      } else if (path.equals("roles")) {
        return roles;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("name")) {
        name = (String) value;
      } else if (path.equals("roles")) {
        roles = (List<RoleXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }

  }
  
  public static class RoleXO extends BaseTestXO {
    
    private String name;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("name");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("name")) {
        return name;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("name")) {
        name = (String) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }

    
  }
  
  
  public static class BaseTestXO extends XynaObject {

    public void collectChanges(long arg0, long arg1, IdentityHashMap<GeneralXynaObject, DataRangeCollection> arg2,
                               Set<Long> arg3) {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    public Object get(String arg0) throws InvalidObjectPathException {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    public void set(String arg0, Object arg1) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    public boolean supportsObjectVersioning() {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    public String toXml(String arg0, boolean arg1, long arg2, XMLReferenceCache arg3) {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    @Override
    public XynaObject clone() {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }

    @Override
    public String toXml(String arg0, boolean arg1) {
      fail("Unreachable mock code");
      throw new IllegalStateException();
    }
    
  }
  
}
