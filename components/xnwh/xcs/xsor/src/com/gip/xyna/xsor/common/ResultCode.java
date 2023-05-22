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

package com.gip.xyna.xsor.common;


public enum ResultCode {
  RESULT_OK, 
  NON_UNIQUE_IDENTIFIER, 
  OBJECT_NOT_FOUND, 
  REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE, 
  REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE, 
  REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS,
  TIMEOUT_LOCAL_CHANGES,
  NOBODY_EXPECTS, CLUSTERSTATECHANGED, 
  CONFLICTING_REQUEST_FROM_OTHER_NODE, 
  PENDING_ACTIONS_LOCAL_CHANGES, 
  RESULT_OK_ONLY_LOCAL_CHANGES, 
  RESULT_OK_ONLY_LOCAL_CHANGES_MASTER, 
  REQUESTED_ACTION_NOT_ALLOWED_IN_NOT_STRICT_MODE,
  CLUSTER_TIMEOUT_LOCAL_CHANGES

}
