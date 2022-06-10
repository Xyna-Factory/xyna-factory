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
package com.gip.xyna.xdev.map.mapping.exceptions;




public class TypeMapperCreationException extends Exception {

  private static final long serialVersionUID = 1L;
  
  private String fieldName;
  private String name;
  private TypeMapperCreationFailure failure;
  
  public static enum TypeMapperCreationFailure {
    UnexpectedType, 
    StringConstructorMissing, 
    FieldAccess,
    NoXynaObjectClass,
    XynaObjectClassNotFound,
    Database,
    Incomplete, MethodAccess
  }
  
  public TypeMapperCreationException(TypeMapperCreationFailure failure, String value) {
    super(failure +": "+value);
    this.failure = failure;
    this.name = value;
  }
  
  public TypeMapperCreationException(TypeMapperCreationFailure failure, String value, Throwable cause) {
    super(failure +": "+value, cause);
    this.failure = failure;
    this.name = value;
  }
  
  public TypeMapperCreationException(TypeMapperCreationFailure failure, Throwable cause) {
    super(failure +": "+cause.getMessage(), cause);
    this.failure = failure;
  }


  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getFieldName() {
    return fieldName;
  }
  
  public String getName() {
    return name;
  }
  
  public TypeMapperCreationFailure getFailure() {
    return failure;
  }
}
