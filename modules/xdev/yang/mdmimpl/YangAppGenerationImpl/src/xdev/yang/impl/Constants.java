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


public class Constants {

  public static final String ATT_YANG_TYPE = "type";
  public static final String ATT_MAPPING_YANGPATH = "yang";
  public static final String ATT_MAPPING_VALUE = "config";
  public static final String ATT_MAPPING_NAMESPACE = "namespace";
  
  public static final String TAG_YANG = "Yang";
  public static final String TAG_RPC = "Rpc";
  public static final String TAG_RPC_NS = "RpcNamespace";
  public static final String TAG_DEVICE_FQN = "DeviceFqn";
  public static final String TAG_MAPPINGS = "Mappings";
  public static final String TAG_MAPPING = "Mapping";
  
  public static final String TAG_HELLO = "hello";
  public static final String TAG_CAPABILITIES = "capabilities";
  public static final String TAG_CAPABILITY = "capability";
  
  public static final String VAL_USECASE = "Usecase";
  public static final String VAL_MODULECOLLECTION = "ModuleCollection";
  public static final String VAL_DEVICE = "Capabilities";
  
  public static final String TYPE_CONTAINER = "container";
  public static final String TYPE_LEAF = "leaf";
  public static final String TYPE_GROUPING = "grouping";
  
  public static final String NS_SEPARATOR = "�";
  
  public static final String NETCONF_NS = "urn:ietf:params:xml:ns:netconf:base:1.0";
}
