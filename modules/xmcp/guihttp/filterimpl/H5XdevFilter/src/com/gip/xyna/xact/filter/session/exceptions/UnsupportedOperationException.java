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
package com.gip.xyna.xact.filter.session.exceptions;



public class UnsupportedOperationException extends Exception {

  public static final String OPERATION_DYNAMIC_TYPING = "Dynamic Typing";
  public static final String DYNAMIC_TYPING_ONLY_FOR_SERVICE_CALLS = "Dynamic Typing is only supported for service calls.";
  public static final String DYNAMIC_TYPING_ONLY_FOR_INPUTS_OUTPUTS = "Dynamic Typing is only supported for inputs and outputs of service calls.";
  public static final String DYNAMIC_TYPING_QUERY_ONLY_IMPLICITLY = "Dynamic Typing can only implicitly applied to queries by changing the storable type.";
  public static final String DYNAMIC_TYPING_WRONG_TYPE = "Dynamic Typing can only applied to subtypes of the corresponding supertype.";

  public static final String OPERATION_STORABLE_ROLE = "Change Storable Role";
  public static final String STORABLE_ROLE_NOT_SUPPORTED = "New role must be one of these: ";

  public static final String OPERATION_IMPLEMENTATION_TYPE = "Change Implementation Type";
  public static final String IMPLEMENTATION_TYPE_NOT_SUPPORTED = "New type must be one of these: ";

  public static final String OPERATION_IMPLEMENTATION = "Change Implementation";
  public static final String IMPLEMENTATION_NOT_SUPPORTED = "Implementation Type must be Coded Service";

  public static final String OPERATION_CHANGE_TYPE = "Change Type";
  public static final String CHANGE_TYPE_MUTUALLY_EXCLUSIVE = "FQN and Simple Type are mutually exclusive.";
  
  public static final String NOT_SUPPORTED = "Operation is not supported.";
  
  public static final String PROTOTYPE_CONVERTION_NOT_ALLOWED_TARGET_TYPE_EXISTS = "Convertion to an existing type is not allowed.";
  
  public static final String COPY_OPERATION = "copy";
  public static final String MOVE_IN_YOURSELF_IS_NOT_POSSIBLE = "Moving a step in itself is not supported.";
  public static final String COPY_IN_YOURSELF_IS_NOT_POSSIBLE = "Copying a step in itself is not supported.";
  public static final String COPY_QUERY_TO_CLIPBOARD_NOT_SUPPORTED = "Copying a query step to the clipboard is not supported, yet.";

  public static final String INVALID_TYPE = "The type can't be used for a variable.";
  public static final String INVALID_BASE_TYPE = "The type can't be used as a base type.";

  public static final String COPY_TO_CLIPBOARD = "Copy to Clipboard";
  public static final String CANNOT_COPY_PRIMITIVE_VARIABLE = "A primitive variable can't be copied.";
  public static final String CANNOT_COPY_UNSAVED_THIS_VAR = "A variable referring to an unsaved Data Type can't be copied.";

  public static final String INSERT_OPERATION = "Insert";

  private static final long serialVersionUID = 1L;


  public UnsupportedOperationException(String operationName, String reason) {
    super("The operation " + operationName + " is not supported for this object. Reason: " + reason);
  }

}
