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
package com.gip.xyna.xnwh.utils;

import com.gip.xyna.xnwh.exception.SQLRetryOperationRuntimeException;
import com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException;
import com.gip.xyna.xnwh.exception.SQLRuntimeException;


@SuppressWarnings("unchecked")
public enum SQLErrorHandling {
  
  ERROR {
    @Override
    public SQLRuntimeException getException(Exception cause) {
      return new SQLRuntimeException(cause);
    }
  },
  RETRY_TRANSACTION {
    @Override
    public SQLRetryTransactionRuntimeException getException(Exception cause) {
      return new SQLRetryTransactionRuntimeException(cause);
    }
  },
  RETRY_OPERATION {
    @Override
    public SQLRetryOperationRuntimeException getException(Exception cause) {
      return new SQLRetryOperationRuntimeException(cause);
    }
  };
  
  
  public abstract <E extends SQLRuntimeException> E getException(Exception cause); 

}
