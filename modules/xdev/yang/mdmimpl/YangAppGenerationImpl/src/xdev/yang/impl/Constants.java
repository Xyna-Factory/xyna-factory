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
package xdev.yang.impl;

import java.util.Set;

public class Constants {

  public static final String ATT_YANG_TYPE = "type";

  public static final String ATT_MAPPING_YANGPATH = "yang";
  public static final String ATT_MAPPING_VALUE = "config";
  public static final String ATT_MAPPING_KEYWORD = "keyword";
  public static final String ATT_MAPPING_NAMESPACE = "namespace";

  public static final String ATT_SIGNATURE_LOCATION = "location";
  public static final String ATT_SIGNATURE_ENTRY_FQN = "fqn";
  public static final String ATT_SIGNATURE_ENTRY_VARNAME = "variableName";

  public static final String ATT_LIST_CONFIG_TYPE = "type";
  public static final String ATT_LIST_CONFIG_YANG = "yang";
  public static final String ATT_LIST_CONFIG_NS = "namespaces";
  public static final String ATT_LIST_CONFIG_KEYWORDS = "keywords";

  public static final String ATT_LIST_CONFIG_CONSTANT_LENGTH = "length";

  public static final String ATT_LIST_CONFIG_DYNAMIC_VARIABLE = "variable";
  public static final String ATT_LIST_CONFIG_DYNAMIC_PATH = "path";
  
  public static final String ATT_ANYXMLCONFIG_YANG = "yang";
  public static final String ATT_ANYXMLCONFIG_NAMESPACES = "namespaces";
  public static final String ATT_ANYXMLCONFIG_TAG = "tag";
  public static final String ATT_ANYXMLCONFIG_NAMESPACE = "namespace";

  public static final String TAG_YANG = "Yang";
  public static final String TAG_RPC = "Rpc";
  public static final String TAG_RPC_NS = "RpcNamespace";
  public static final String TAG_DEVICE_FQN = "DeviceFqn";
  public static final String TAG_MAPPINGS = "Mappings";
  public static final String TAG_MAPPING = "Mapping";
  public static final String TAG_SIGNATURE = "Signature";
  public static final String TAG_SIGNATURE_ENTRY = "SignatureEntry";
  
  public static final String TAG_ANYXMLCONFIGS = "AnyXmlConfigurations";
  public static final String TAG_ANYXMLCONFIG = "AnyXmlConfiguration";

  public static final String TAG_LISTCONFIGS = "ListConfigurations";
  public static final String TAG_LISTCONFIG = "ListConfiguration";

  public static final String TAG_HELLO = "hello";
  public static final String TAG_CAPABILITIES = "capabilities";
  public static final String TAG_CAPABILITY = "capability";

  public static final String TAG_YANG_LIBRARY = "yang-library";
  public static final String TAG_MODULE_SET = "module-set";
  public static final String TAG_MODULE = "module";
  public static final String TAG_MODULE_NAME = "name";
  public static final String TAG_MODULE_NAMESPACE = "namespace";
  public static final String TAG_MODULE_REVISION = "revision";
  public static final String TAG_MODULE_FEATURES = "features";

  public static final String VAL_OPERATION = "Operation";
  public static final String VAL_MODULECOLLECTION = "ModuleCollection";
  public static final String VAL_DEVICE = "Capabilities";

  public static final String VAL_LOCATION_INPUT = "input";
  public static final String VAL_LOCATION_OUTPUT = "output";

  public static final String VAL_LIST_CONFIG_CONSTANT = "constant";
  public static final String VAL_LIST_CONFIG_DYNAMIC = "dynamic";

  public static final String TYPE_CONTAINER = "container";
  public static final String TYPE_LEAF = "leaf";
  public static final String TYPE_GROUPING = "grouping";
  public static final String TYPE_USES = "uses";
  public static final String TYPE_IDENTITY = "identity";
  public static final String TYPE_RPC = "rpc";
  public static final String TYPE_CHOICE = "choice";
  public static final String TYPE_CASE = "case";
  public static final String TYPE_ANYXML = "anyxml";
  public static final String TYPE_ANYDATA = "anydata";
  public static final String TYPE_LEAFLIST = "leaf-list";
  public static final String TYPE_LIST = "list";
  public static final String TYPE_STATUS = "status";
  public static final String TYPE_CONFIG = "config";
  public static final String TYPE_DESCRIPTION = "description";
  public static final String TYPE_ACTION = "action";
  public static final String TYPE_INPUT = "input";
  public static final String TYPE_OUTPUT = "output";
  public static final String TYPE_NOTIFICATION = "notification";

  public static final String NS_SEPARATOR = "§";
  public static final String LIST_INDEX_SEPARATOR = ") ";


  public static final String NETCONF_NS = "urn:ietf:params:xml:ns:netconf:base:1.0";
  public static final String NETCONF_BASE_CAPABILITY_NO_VERSION = "urn:ietf:params:netconf:base:";
  public static final String NETCONF_CAPABILITY_URL = "urn:ietf:params:netconf:capability:";
  
  // from RFC 8525
  public static final String YANG_LIB_NS = "urn:ietf:params:xml:ns:yang:ietf-yang-library";

  /* from RFC 7950;
     schema node: A node in the schema tree.  One of action, container,
     leaf, leaf-list, list, choice, case, rpc, input, output,
     notification, anydata, and anyxml.
   */
  public static final Set<String> SCHEMA_NODE_TYPE_NAMES = Set.of(TYPE_ACTION, TYPE_CONTAINER, TYPE_LEAF, TYPE_LEAFLIST, TYPE_LIST,
                                                                  TYPE_CHOICE, TYPE_CASE, TYPE_RPC, TYPE_INPUT, TYPE_OUTPUT,
                                                                  TYPE_NOTIFICATION, TYPE_ANYDATA, TYPE_ANYXML);
}
