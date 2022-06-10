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

package com.gip.juno.ws.enums;

/**
 *  DefaultValue:insert explicit default value in insert/update statement;
 *  IgnoreColumn: Do not mention column in SQL statement (so that default value from SQL table definition can apply);
 *  ConstraintViolation: check before insert or update operation if column would be set null:
 *       throw an exception in that case
 */
public enum IfNoVal {
  EmptyString, Null, DefaultValue, ConstraintViolation, IgnoreColumn
}
