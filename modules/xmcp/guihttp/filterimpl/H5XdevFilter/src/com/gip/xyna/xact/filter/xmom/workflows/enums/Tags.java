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
package com.gip.xyna.xact.filter.xmom.workflows.enums;

public class Tags {
  
  public static final String TYPE = "type";
  
  public static final String AREAS = "areas";
  public static final String AREA_TYPE = "areaType"; // TODO: remove when changes to JSON-structure are complete
  
  public static final String XML = "xml"; //for copy between factories
  
  public static final String QUERY = "query";
  public static final String QUERY_OUTPUT = "output";
  public static final String QUERY_FILTER_CRITERIA = "filterCriteria";
  public static final String QUERY_SORTINGS = "sortings";
  public static final String QUERY_SELECTION_MASKS = "selectionMasks";
  public static final String QUERY_SELECTION_MASK = "selectionMask";
  public static final String QUERY_CONST_QUERY_PARAMETER = "queryParameter";
  
  public static final String QUERY_FILTER_CRITERION = "queryFilterCriterion";
  public static final String QUERY_SORT_CRITERION = "querySortCriterion";
  
  public static final String QUERY_CONST_SELECTION_MASK = "selectionMask";
  
  public static final String SERVICE_INPUT = "input";
  public static final String SERVICE_OUTPUT = "output";
  public static final String SERVICE_THROWS = "throws";
  public static final String SERVICE_LABEL = "label";
  public static final String SERVICE_DOCUMENTATION = "documentation";
  public static final String SERVICE_CONTENT = "content";
  
  public static final String THROW_INPUT = "input";
  public static final String RETRY_INPUT = "input";
  public static final String RETRY_DOCUMENTATION = "documentation";
  
  public static final String READONLY = "readonly";
  
  public static final String LIMIT_RESULTS = "limitResults";
  public static final String QUERY_HISTORY = "queryHistory";
  public static final String QUERY_ASCENDING = "ascending";
  
  public static final String META = "$meta";
  public static final String FQN = "fqn";
  public static final String OPERATION = "operation";
  public static final String DETACHED = "detached";
  public static final String FREE_CAPACITIES = "freeCapacities";
  public static final String OVERRIDE_COMPENSATION = "overrideCompensation";
  public static final String ORDER_INPUT_SOURCES = "orderInputSources";
  public static final String REMOTE_DESTINATION = "remoteDestination";
  public static final String SERVICE = "service";
  public static final String RETRY = "retry";
  public static final String TEMPLATE = "template";
  
  public static final String REVISION = "revision";
  public static final String SAVE_STATE = "saveState";
  public static final String DEPLOYMENT_STATE = "deploymentState";
  
  public static final String CONTENT = "content";
  public static final String RTC = "rtc";
  public static final String LABEL = "label";
  
  public static final String TEXT = "text";
  
  public static final String VARIABLE = "variable";
  public static final String VARIABLES = "variables"; // TODO: delete?
  
  public static final String IS_LIST = "isList";
  public static final String LINK_STATE_IN = "linkStateIn";
  public static final String LINK_STATE_OUT = "linkStateOut";
  public static final String CAST_TO_FQN = "castToFqn";
  public static final String CONSTANT = "constant";
  
  public static final String IS_PROTOTYPE = "isAbstract";
  
  public static final String FORMULA = "formula";
  public static final String FORMULAS = "formulas";
  
  public static final String CONDITIONAL_CHOICE = "conditionalChoice";
  public static final String CONDITIONAL_BRANCHING = "conditionalBranching";
  public static final String TYPE_CHOICE = "typeChoice";
  public static final String BRANCH = "branch";
  public static final String CHOICE_INPUT = "input";
  public static final String CHOICE_CASES = "cases";
  public static final String CHOICE_CONDITION = "condition";
  public static final String CASE = "case";
  public static final String CASES = "cases";
  public static final String CASE_INPUT = "input";
  public static final String CASE_OUTPUT = "output";
  public static final String CASE_FORMULA_AREA = "condition";
  public static final String EXPRESSION = "expression";
  public static final String EXPRESSION_PARAMETER_0 = "%0%";
  public static final String BOOLEAN = "boolean";
  
  public static final String MAPPING = "mapping";
  public static final String MAPPING_INPUT = "input";
  public static final String MAPPING_OUTPUT = "output";
  
  public static final String FOREACH_INPUT = "input";
  public static final String FOREACH_OUTPUT = "output";
  
  // signature
  public static final String SIGNATURE_INPUTS = "inputs";
  public static final String SIGNATURE_OUTPUTS = "outputs";
  public static final String THROW = "throw";
  public static final String EXCEPTION = "exception";
  public static final String UNHANDLED_EXCEPTIONS = "unhandledExceptions";
  public static final String ERROR_HANDLING = "errorHandling";
  
  // Dataflow
  public static final String SOURCE_ID = "sourceId";
  public static final String TARGET_ID = "targetId";
  public static final String BRANCH_ID = "branchId";
  public static final String CONNECTION_TYPE = "type";
  
  // Data Types
  
  public static final String DATA_TYPE_DOCUMENTATION_AREA = "documentation";
  
  public static final String DATA_TYPE_TYPE_INFO_AREA = "typeInfo";
  public static final String DATA_TYPE_STORABLE_PROPERTIES_AREA = "storableProperties";  
  public static final String DATA_TYPE_GLOBAL_STORABLE_PROPERTIES_AREA = "globalStorableProperties";  
  public static final String DATA_TYPE_EXCEPTION_MESSAGES_AREA = "exceptionMessages";
  public static final String DATA_TYPE_INHERITED_VARS_AREA = "inheritedVars";  
  public static final String DATA_TYPE_MEMBER_VARS_AREA = "memberVars";
  public static final String DATA_TYPE_INHERITED_METHODS_AREA = "inheritedMethods";
  public static final String DATA_TYPE_OVERRIDDEN_METHODS_AREA = "overriddenMethods";
  public static final String DATA_TYPE_MEMBER_METHODS_AREA = "memberMethods";
  
  public static final String DATA_TYPE_TYPE_CUSTOM_FIELD0 = "customField0";
  public static final String DATA_TYPE_TYPE_CUSTOM_FIELD1 = "customField1";
  public static final String DATA_TYPE_TYPE_CUSTOM_FIELD2 = "customField2";
  public static final String DATA_TYPE_TYPE_CUSTOM_FIELD3 = "customField3";

  public static final String DATA_TYPE_MEMBER_VAR = "memberVar";
  public static final String DATA_TYPE_STORABLE_ROLE = "storableRole";
  public static final String DATA_TYPE_FQN= "fqn";
  public static final String DATA_TYPE_PRIMITIVE_TYPE = "primitiveType";
  public static final String DATA_TYPE_IS_ABSTRACT = "isAbstract";
  public static final String DATA_TYPE_BASE_TYPE = "baseType";
  public static final String EXCEPTION_TYPE_MESSAGE_LANGUAGE = "messageLanguage";
  public static final String EXCEPTION_TYPE_MESSAGE_TEXT = "messageText";
  
  // member methods
  public static final String DATA_TYPE_MEMBER_METHOD = "memberMethod";
  public static final String DATA_TYPE_IMPLEMENTATION_TYPE = "implementationType";
  public static final String DATA_TYPE_IMPLEMENTATION = "implementation";
  public static final String DATA_TYPE_REFERENCE = "reference";
  public static final String DATA_TYPE_IS_ABORTABLE = "isAbortable";
  public static final String DATA_TYPE_DOCUMENTATION = "documentation";
  public static final String DATA_TYPE_INPUT = "input";
  public static final String DATA_TYPE_OUTPUT = "output";
  public static final String DATA_TYPE_THROWS = "throws";


  // ServiceGroups
  public static final String SERVICE_GROUP_TYPE_LABEL_AREA_ID = "typeInfoArea";
  public static final String SERVICE_GROUP_TYPE_LABEL_AREA_NAME = "typeInfo";
  public static final String SERVICE_GROUP_JAVA_LIBRARIES_AREA_ID = "libs";
  public static final String SERVICE_GROUP_JAVA_SHARED_LIBRARIES_AREA_ID = "sharedLibs";
  public static final String SERVICE_GROUP_MEMBER_METHODS_AREA_ID = "methodsArea";
  public static final String SERVICE_GROUP_MEMBER_SERVICE = "memberService";
  public static final String SERVICE_GROUP_LIB = "lib";
  public static final String SERVICE_GROUP_FILE_ID = "fileId";
  public static final String SERVICE_GROUP_LIB_IS_USED = "isUsed";
  
  // orderinputsource
  public static final String STEP_FUNCTION_ORDER_INPUT_SOURCE_NAME = "name";
  
  // convert
  public static final String CONVERT_PATH = "path";
  public static final String CONVERT_LABEL = "label";
  public static final String CONVERT_TARGET_TYPE = "targetType";
}
