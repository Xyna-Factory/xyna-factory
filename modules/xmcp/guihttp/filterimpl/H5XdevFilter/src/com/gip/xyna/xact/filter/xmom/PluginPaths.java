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
package com.gip.xyna.xact.filter.xmom;


public class PluginPaths {

  private static final String documentation = "documentation";
  private static final String members = "members";
  private static final String methods = "methods";
  private static final String implementation = "implementation";
  private static final String location_modeller = "modeller";
  private static final String location_datatype = String.join("/", location_modeller, "datatype");
  private static final String location_servicegroup = String.join("/", location_modeller, "servicegroup");
  private static final String location_exception = String.join("/", location_modeller, "exception");

  public static final String location_datatype_documenation = String.join("/", location_datatype, documentation);
  public static final String location_datatype_methods = String.join("/", location_datatype, methods);
  public static final String location_datatype_members = String.join("/", location_datatype, members);
  public static final String location_datatype_member_documenation = String.join("/", location_datatype_members, documentation);
  public static final String location_datatype_method_documentation = String.join("/", location_datatype_methods, documentation);
  public static final String location_datatype_method_implementation = String.join("/", location_datatype_methods, implementation);

  public static final String location_servicegroup_methods = String.join("/", location_servicegroup, methods);
  public static final String location_servicegroup_method_documentation = String.join("/", location_servicegroup_methods, documentation);
  public static final String location_servicegroup_method_implementation = String.join("/", location_servicegroup_methods, implementation);

  public static final String location_exception_documentation = String.join("/", location_exception, documentation);
  public static final String location_exception_members = String.join("/", location_exception, members);
  public static final String location_exception_member_documentation = String.join("/", location_exception_members, documentation);
  public static final String location_exception_message = String.join("/", location_exception, "message");
}
