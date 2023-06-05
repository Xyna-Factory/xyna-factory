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
package com.gip.xyna.xnwh.exception;

/** 
 * should lead to a direct retry of the last statement
 * extends SQLRetryTransactionRuntimeException to trigger a regular transaction retry in cases of no specific handling
 */
public class SQLRetryOperationRuntimeException extends SQLRetryTransactionRuntimeException {

  private static final long serialVersionUID = 1L;

  public SQLRetryOperationRuntimeException(String msg) {
    super(msg);
  }
  
  public SQLRetryOperationRuntimeException(Throwable e) {
    super(e);
  }
  
  public SQLRetryOperationRuntimeException(String msg, Throwable e) {
    super(msg, e);
  }

}
