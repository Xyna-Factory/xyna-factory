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

package com.gip.xyna.xact.filter.session.workflowissues;


public class WorkflowIssueMessageCode {

  public static final String AMBIGUE_VARIABLE = "AMBIGUE_VARIABLE";
  public static final String UNASSIGNED_VARIABLE = "UNASSIGNED_VARIABLE";
  public static final String UNASSIGNED_VARIABLE_BRANCH = "UNASSIGNED_VARIABLE_BRANCH";
  public static final String ABSTRACT_CONSTANT = "ABSTRACT_CONSTANT";
  public static final String PROTOTYPE_VARIABLE = "PROTOTYPE_VARIABLE";
  public static final String PROTOTYPE_STEP = "PROTOTYPE_STEP";
  public static final String RETRY_AT_INVALID_POSITION = "RETRY_AT_INVALID_POSITION";
  public static final String OBJECTS_AFTER_BLOCKER = "OBJECTS_AFTER_BLOCKER";
  public static final String INVALID_FORMULA = "INVALID_FORMULA";
  public static final String INVALID_ORDER_INPUT_SOURCE = "INVALID_ORDER_INPUT_SOURCE";
}
