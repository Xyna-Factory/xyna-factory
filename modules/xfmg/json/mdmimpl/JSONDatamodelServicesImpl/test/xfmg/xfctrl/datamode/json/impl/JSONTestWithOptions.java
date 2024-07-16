/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.util.Arrays;
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
import xfmg.xfctrl.datamodel.json.impl.JSONDatamodelServicesServiceOperationImpl.JsonOptions;
import xfmg.xfctrl.datamodel.json.impl.JSONDatamodelServicesServiceOperationImpl.OASScope;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObjectWriter;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;
import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import xfmg.xfctrl.datamodel.json.parameter.XynaObjectDecider;

import com.gip.xyna.ObjectStringRepresentation;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;

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
      impl.fillXynaObjectRecursivly(user, job, "", new JsonOptions(trans, Collections.emptyMap(), Collections.emptySet(), false, false), null);
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
      impl.fillXynaObjectRecursivly(user, job, "", new JsonOptions(trans, subs, Collections.emptySet(), false, false), null);
      ObjectStringRepresentation.createStringRepOfObject(sb, job);
      sb.append("\n=========================================\n\n");
      ObjectStringRepresentation.createStringRepOfObject(sb, user);
      System.out.println(sb);
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 13. Cause: Too may commas.", e.getMessage());
    }

  }
  
  
  private XynaObjectDecider createDecider() {
    XynaObjectDecider decider = new XynaObjectDecider() {

      private static final long serialVersionUID = 1L;

      @Override
      public XynaObjectDecider clone() {
        return null;
      }

      @Override
      public XynaObjectDecider clone(boolean arg0) {
        return null;
      }

      @Override
      public GeneralXynaObject decide(String baseType, JSONObject data) {
        JSONKeyValue t = data.getMembers().stream().filter(x -> x.getKey().equals("@type")).findFirst().get();
        String dyntype = t.getValue().getStringOrNumberValue();
        if(dyntype.equals("RoleXO")) {
          return new RoleXO();
        }
        return null;
      }
      
    };
    return decider;
  }
  
  public void testWithOptions3() throws IllegalArgumentException, IllegalAccessException {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"{"+
"  \"member\": {"+
"     \"name\": \"test\","+
"     \"@type\": \"RoleXO\""+
"  }"+
"}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    XynaObjectDecider decider = createDecider();
    try {
      jp.fillObject(tokens, 0, job);
      StringBuilder sb = new StringBuilder();
      JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
      ContainerXO container = new ContainerXO();
      impl.fillXynaObjectRecursivly(container, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), false, false), decider);
      ObjectStringRepresentation.createStringRepOfObject(sb, job);
      sb.append("\n=========================================\n\n");
      ObjectStringRepresentation.createStringRepOfObject(sb, container);
      System.out.println(sb);
      assertTrue(container.member instanceof RoleXO);
      container = new ContainerXO();
      impl.fillXynaObjectRecursivly(container, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), false, false), null);
      assertTrue(container.member instanceof BaseTestXO);
    } catch (InvalidJSONException e) {
      fail();
    }
  }

  
  public void testWithOptions4() throws IllegalArgumentException, IllegalAccessException {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"{"+
"  \"member\": ["+ 
"    {"+
"      \"member\": {"+
"         \"name\": \"test\""+
"      },"+
"      \"@type\": \"RoleXO\""+
"    }"+
"  ]"+
"}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    XynaObjectDecider decider = createDecider();
    try {
      jp.fillObject(tokens, 0, job);
      StringBuilder sb = new StringBuilder();
      JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
      ListContainerXO container = new ListContainerXO();
      impl.fillXynaObjectRecursivly(container, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), false, false), decider);
      ObjectStringRepresentation.createStringRepOfObject(sb, job);
      sb.append("\n=========================================\n\n");
      ObjectStringRepresentation.createStringRepOfObject(sb, container);
      System.out.println(sb);
      assertTrue(container.member.get(0) instanceof RoleXO);
      container = new ListContainerXO();
      impl.fillXynaObjectRecursivly(container, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), false, false), null);
      assertTrue(container.member.get(0) instanceof BaseTestXO);
    } catch (InvalidJSONException e) {
      fail();
    }
  }
  
  
  public void testWithOptions5_listwrapperwrapper() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"["+
"  ["+ 
"    {"+
"       \"name\": \"test\""+
"    }"+
"  ]"+
"]";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    List<JSONValue> job = new ArrayList<JSONValue>();
    XynaObjectDecider decider = null;
    jp.fillArray(tokens, 0, job);
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    ListWrapperWrapper xo = new ListWrapperWrapper();
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperWrapper.class.getCanonicalName());
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    impl.fillXynaObjectListWrapper(xo, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, false, false), decider);
    assertTrue(xo.lists.size() == 1);
    assertTrue(xo.lists.get(0).roles.size() == 1);
    assertTrue(xo.lists.get(0).roles.get(0).name.equals("test"));
  }
  
  public void testWithOptions6_listwrapper() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"["+
"  ["+ 
"    {"+
"       \"name\": \"test\""+
"    }"+
"  ]"+
"]";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    List<JSONValue> job = new ArrayList<JSONValue>();
    XynaObjectDecider decider = null;
    jp.fillArray(tokens, 0, job);
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    List<ListWrapperXO> xo;
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    xo = impl.createList(ListWrapperXO.class, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, false, false), decider);
    assertTrue(xo.size() == 1);
    assertTrue(xo.get(0).roles.size() == 1);
    assertTrue(xo.get(0).roles.get(0).name.equals("test"));
  }
  
  public void testWithOptions7_innerListWrapper() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"{"+        
"  \"list2d\": ["+
"    ["+ 
"      {"+
"         \"name\": \"test\""+
"      }"+
"    ]"+
"  ]"+
"}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    XynaObjectDecider decider = null;
    List2dContainerXO xo = new List2dContainerXO();
    jp.fillObject(tokens, 0, job);
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    impl.fillXynaObjectRecursivly(xo, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, false, false), decider);

    assertTrue(xo.list2d.size() == 1);
    assertTrue(xo.list2d.get(0).roles.size() == 1);
    assertTrue(xo.list2d.get(0).roles.get(0).name.equals("test"));
  }
  

  public void testWithOptions8_innerListWrapperWrapper() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = 
"{"+        
"  \"list2d\": ["+
"    ["+ 
"      {"+
"         \"name\": \"test\""+
"      }"+
"    ]"+
"  ]"+
"}";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    XynaObjectDecider decider = null;
    List2dWrapper2XO xo = new List2dWrapper2XO();
    jp.fillObject(tokens, 0, job);
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperWrapper.class.getCanonicalName());
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    impl.fillXynaObjectRecursivly(xo, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, false, false), decider);

    assertTrue(xo.list2d.lists.size() == 1);
    assertTrue(xo.list2d.lists.get(0).roles.size() == 1);
    assertTrue(xo.list2d.lists.get(0).roles.get(0).name.equals("test"));
  }
  
  public void testWithOptions9_primitiveList() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString =       
"["+
"  ["+ 
"       \"test\""+
"  ]"+
"]";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    List<JSONValue> job = new ArrayList<JSONValue>();
    jp.fillArray(tokens, 0, job);
    XynaObjectDecider decider = null;
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(PrimitiveListWrapperXO.class.getCanonicalName());
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    List<PrimitiveListWrapperXO> list = null;
    list = impl.createList(PrimitiveListWrapperXO.class, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, false, false), decider);
    
    assertTrue(list.size() == 1);
    assertTrue(list.get(0).values.size() == 1);
    assertTrue(list.get(0).values.get(0).equals("test"));
  }

  public void testWithOptions10_primitiveList_label() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString =       
"["+
"  ["+ 
"       \"test\""+
"  ]"+
"]";
    
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    List<JSONValue> job = new ArrayList<JSONValue>();
    jp.fillArray(tokens, 0, job);
    XynaObjectDecider decider = null;
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(PrimitiveListWrapperXO.class.getCanonicalName());
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    List<PrimitiveListWrapperXO> list = null;
    list = impl.createList(PrimitiveListWrapperXO.class, job, "", new JsonOptions(Collections.emptyMap(), Collections.emptyMap(), wrappers, true, false), decider);
    
    assertTrue(list.size() == 1);
    assertTrue(list.get(0).values.size() == 1);
    assertTrue(list.get(0).values.get(0).equals("test"));
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
    JsonOptions options = new JsonOptions(trans, Collections.emptyMap(), Collections.emptySet(), false, false);
    JSONObject obj = impl.createFromXynaObjectRecursivly(user, "", options, OASScope.none);
    System.out.println(JSONObjectWriter.toJSON("", obj));
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
    JsonOptions options = new JsonOptions(trans, subs, Collections.emptySet(), false, false);
    JSONObject obj = impl.createFromXynaObjectRecursivly(user, "", options, OASScope.none);
    System.out.println(JSONObjectWriter.toJSON("", obj));
  }
  
  public void testWriteWithOptions3UseLabel() {
    RoleXO role1 = new RoleXO();
    role1.name = "my.role.tenant.member";
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    Map<String, String> subs = new HashMap<String, String>();
    JsonOptions options = new JsonOptions(trans, subs, Collections.emptySet(), true, false);
    JSONObject obj = impl.createFromXynaObjectRecursivly(role1, "", options, OASScope.none);
    //can't use getMember(), because implementation is not set in mdm.jars created outside of a running factory
    JSONValue readName = obj.getMembers().stream().filter(x -> "SomeName".equals(x.getKey())).map(x -> x.getValue()).findFirst().orElse(null);
    assertTrue(readName != null);
    assertTrue(role1.name.equals(readName.getStringOrNumberValue()));
  }
  
  public void testWriteWithOptions4UseLabelAndSubstitude() {
    RoleXO role1 = new RoleXO();
    role1.name = "my.role.tenant.member";
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    Map<String, String> subs = new HashMap<String, String>();
    subs.put("name", "someOtherName");
    JsonOptions options = new JsonOptions(trans, subs, Collections.emptySet(), true, false);
    JSONObject obj = impl.createFromXynaObjectRecursivly(role1, "", options, OASScope.none);
    //can't use getMember(), because implementation is not set in mdm.jars created outside of a running factory
    JSONValue readName = obj.getMembers().stream().filter(x -> "someOtherName".equals(x.getKey())).map(x -> x.getValue()).findFirst().orElse(null);
    assertTrue(readName != null);
    assertTrue(role1.name.equals(readName.getStringOrNumberValue()));
  }
  
  public void testWriteWithOptions5ListWrapper() {
    ListWrapperXO wrapper = new ListWrapperXO();
    wrapper.roles = Arrays.asList(new RoleXO[] { new RoleXO("test") });
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    Map<String, String> subs = new HashMap<String, String>();
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    JsonOptions options = new JsonOptions(trans, subs, wrappers, false, false);
    JSONValue value = impl.createValFromXynaObjectListRecurisvely(Arrays.asList(wrapper), "", options, OASScope.none);
    assertTrue(value.getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().size() == 1);
    assertTrue(value.getArrayValue().get(0).getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().get(0).getArrayValue().get(0).getType().equals(JSONVALTYPES.OBJECT));
    JSONValue val = getValue(value.getArrayValue().get(0).getArrayValue().get(0).getObjectValue(), "name");
    assertTrue(val.getStringOrNumberValue().equals("test"));    
  }
  
  public void testWriteWithOptions6ListWrapperWrapper() {
    ListWrapperWrapper wrapper = new ListWrapperWrapper();
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    wrapper.lists = Arrays.asList(new ListWrapperXO());
    wrapper.lists.get(0).roles = Arrays.asList(new RoleXO("test"));
    Map<String, String> trans = new HashMap<String, String>();
    Map<String, String> subs = new HashMap<String, String>();
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(ListWrapperWrapper.class.getCanonicalName());
    wrappers.add(ListWrapperXO.class.getCanonicalName());
    JsonOptions options = new JsonOptions(trans, subs, wrappers, false, false);
    JSONValue value = impl.createValFromXynaObjectRecursively(wrapper, "", options, OASScope.none);
    assertTrue(value.getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().size() == 1);
    assertTrue(value.getArrayValue().get(0).getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().get(0).getArrayValue().get(0).getType().equals(JSONVALTYPES.OBJECT));
    JSONValue val = getValue(value.getArrayValue().get(0).getArrayValue().get(0).getObjectValue(), "name");
    assertTrue(val.getStringOrNumberValue().equals("test"));  
  }
  
  public void testWriteWIthOptions7PrimitiveListWrapper() {
    PrimitiveListWrapperXO primListWrapper = new PrimitiveListWrapperXO();
    primListWrapper.values = Arrays.asList("test");
    JSONDatamodelServicesServiceOperationImpl impl = new JSONDatamodelServicesServiceOperationImpl();
    Map<String, String> trans = new HashMap<String, String>();
    Map<String, String> subs = new HashMap<String, String>();
    Set<String> wrappers = new HashSet<String>();
    wrappers.add(PrimitiveListWrapperXO.class.getCanonicalName());
    JsonOptions options = new JsonOptions(trans, subs, wrappers, false, false);
    JSONValue value = impl.createValFromXynaObjectListRecurisvely(Arrays.asList(primListWrapper), "", options, OASScope.none);
    assertTrue(value.getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().size() == 1);
    assertTrue(value.getArrayValue().get(0).getType().equals(JSONVALTYPES.ARRAY));
    assertTrue(value.getArrayValue().get(0).getArrayValue().size() == 1);
    assertTrue(value.getArrayValue().get(0).getArrayValue().get(0).getStringOrNumberValue().equals("test"));
  }
  
  private JSONValue getValue(JSONObject obj, String name) {
    return obj.getMembers().stream().filter(x -> name.equals(x.getKey())).map(x -> x.getValue()).findFirst().orElse(null);
  }
  
  public static class ListWrapperWrapper extends BaseTestXO {
    private static final long serialVersionUID = 1L;
    private List<ListWrapperXO> lists;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("lists");
      return set;
    }
    
    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("lists")) {
        return lists;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }
    
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("lists")) {
        lists = (List<ListWrapperXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class ListWrapperXO extends BaseTestXO {
    private static final long serialVersionUID = 1L;
    private List<RoleXO> roles;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("roles");
      return set;
    }
    
    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("roles")) {
        return roles;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }
    
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("roles")) {
        roles = (List<RoleXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class PrimitiveListWrapperXO extends BaseTestXO {
    private static final long serialVersionUID = 1L;
    
    @LabelAnnotation(label="Values")
    private List<String> values;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("values");
      return set;
    }
    
    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("values")) {
        return values;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }
    
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("values")) {
        values = (List<String>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class UserXO extends BaseTestXO {

    private static final long serialVersionUID = 1L;
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

    @SuppressWarnings("unchecked")
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
    
    private static final long serialVersionUID = 1L;
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

    @SuppressWarnings("unchecked")
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
    
    private static final long serialVersionUID = 1L;
    
    @LabelAnnotation(label="SomeName")
    private String name;
    
    public RoleXO() { }
    public RoleXO(String name) {
      this.name = name;
    }
    
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
  
  public static class ContainerXO extends BaseTestXO {
    
    private static final long serialVersionUID = 1L;
    private BaseTestXO member;
    private String type;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("member");
      set.add("@type");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("member")) {
        return member;
      } else if(path.equals("@type")) {
        return type;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("member")) {
        member = (BaseTestXO) value;
      } 
      else if(path.equals("@type")) {
        type = (String) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }

    
  }
  
  

  public static class ListContainerXO extends BaseTestXO {
    
    private static final long serialVersionUID = 1L;
    private List<BaseTestXO> member;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("member");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("member")) {
        return member;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("member")) {
        member = (List<BaseTestXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class List2dContainerXO extends BaseTestXO {
    private static final long serialVersionUID = 1L;
    private List<ListWrapperXO> list2d;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("list2d");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("list2d")) {
        return list2d;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("list2d")) {
        list2d = (List<ListWrapperXO>) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class List2dWrapper2XO extends BaseTestXO {
    private static final long serialVersionUID = 1L;
    private ListWrapperWrapper list2d;
    
    public Set<String> getVariableNames() {
      Set<String> set = new HashSet<String>();
      set.add("list2d");
      return set;
    }

    public Object get(String path) throws InvalidObjectPathException {
      if (path.equals("list2d")) {
        return list2d;
      } else {
        throw new InvalidObjectPathException(path);
      }
    }

    public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      if (path.equals("list2d")) {
        list2d = (ListWrapperWrapper) value;
      } else {
        throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
      }
    }
  }
  
  public static class BaseTestXO extends XynaObject {

    private static final long serialVersionUID = 1L;

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
