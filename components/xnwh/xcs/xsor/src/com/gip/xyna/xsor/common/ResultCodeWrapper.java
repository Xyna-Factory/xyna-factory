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


public class ResultCodeWrapper {
  
  
  public enum GrabResultCode {
    RESULT_OK(ResultCode.RESULT_OK),
    NOBODY_EXPECTS(ResultCode.NOBODY_EXPECTS),
    OBJECT_NOT_FOUND(ResultCode.OBJECT_NOT_FOUND),
    TIMEOUT_LOCAL_CHANGES(ResultCode.TIMEOUT_LOCAL_CHANGES),
    RESULT_OK_ONLY_LOCAL_CHANGES(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES),
    CLUSTER_TIMEOUT_LOCAL_CHANGES(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES),
    PENDING_ACTIONS_LOCAL_CHANGES(ResultCode.PENDING_ACTIONS_LOCAL_CHANGES),
    RESULT_OK_ONLY_LOCAL_CHANGES_MASTER(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER),
    CONFLICTING_REQUEST_FROM_OTHER_NODE(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS);
    
    private final ResultCode wrappedResultCode;
    
    private GrabResultCode(ResultCode wrappedResultCode) {
      this.wrappedResultCode = wrappedResultCode;
    }
    
    public ResultCode getWrappedResultCode() {
      return wrappedResultCode;
    }
    
    public static GrabResultCode wrapResultCode(ResultCode resultCodeToWrap) {
      for (GrabResultCode wrapper : values()) {
        if (wrapper.getWrappedResultCode() == resultCodeToWrap) {
          return wrapper;
        }
      }
      throw new IllegalArgumentException("ResultCode " + resultCodeToWrap + " can not be wrapped in a GrabResultCode");
    }
  }
  
  
  public enum ReleaseResultCode {
    RESULT_OK(ResultCode.RESULT_OK),
    NOBODY_EXPECTS(ResultCode.NOBODY_EXPECTS),
    OBJECT_NOT_FOUND(ResultCode.OBJECT_NOT_FOUND),
    CLUSTERSTATECHANGED(ResultCode.CLUSTERSTATECHANGED),
    TIMEOUT_LOCAL_CHANGES(ResultCode.TIMEOUT_LOCAL_CHANGES),
    RESULT_OK_ONLY_LOCAL_CHANGES(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES),
    CLUSTER_TIMEOUT_LOCAL_CHANGES(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES),
    CONFLICTING_REQUEST_FROM_OTHER_NODE(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS);
    
    private final ResultCode wrappedResultCode;
    
    private ReleaseResultCode(ResultCode wrappedResultCode) {
      this.wrappedResultCode = wrappedResultCode;
    }
    
    public ResultCode getWrappedResultCode() {
      return wrappedResultCode;
    }
    
    public static ReleaseResultCode wrapResultCode(ResultCode resultCodeToWrap) {
      for (ReleaseResultCode wrapper : values()) {
        if (wrapper.getWrappedResultCode() == resultCodeToWrap) {
          return wrapper;
        }
      }
      throw new IllegalArgumentException("ResultCode " + resultCodeToWrap + " can not be wrapped in a GrabResultCode");
    }
  }
  
  
  public enum WriteResultCode {
    RESULT_OK(ResultCode.RESULT_OK),
    OBJECT_NOT_FOUND(ResultCode.OBJECT_NOT_FOUND),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);
    
    private final ResultCode wrappedResultCode;
    
    private WriteResultCode(ResultCode wrappedResultCode) {
      this.wrappedResultCode = wrappedResultCode;
    }
    
    public ResultCode getWrappedResultCode() {
      return wrappedResultCode;
    }
    
    public static WriteResultCode wrapResultCode(ResultCode resultCodeToWrap) {
      for (WriteResultCode wrapper : values()) {
        if (wrapper.getWrappedResultCode() == resultCodeToWrap) {
          return wrapper;
        }
      }
      throw new IllegalArgumentException("ResultCode " + resultCodeToWrap + " can not be wrapped in a WriteResultCode");
    }
  }
   
  
  public enum DeleteResultCode {
    RESULT_OK(ResultCode.RESULT_OK),
    NOBODY_EXPECTS(ResultCode.NOBODY_EXPECTS),
    OBJECT_NOT_FOUND(ResultCode.OBJECT_NOT_FOUND),
    RESULT_OK_ONLY_LOCAL_CHANGES(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES),
    CLUSTER_TIMEOUT_LOCAL_CHANGES(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES),
    CONFLICTING_REQUEST_FROM_OTHER_NODE(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE),
    REQUESTED_ACTION_NOT_ALLOWED_IN_NOT_STRICT_MODE(ResultCode.REQUESTED_ACTION_NOT_ALLOWED_IN_NOT_STRICT_MODE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);
    
    private final ResultCode wrappedResultCode;
    
    private DeleteResultCode(ResultCode wrappedResultCode) {
      this.wrappedResultCode = wrappedResultCode;
    }
    
    public ResultCode getWrappedResultCode() {
      return wrappedResultCode;
    }
    
    public static DeleteResultCode wrapResultCode(ResultCode resultCodeToWrap) {
      for (DeleteResultCode wrapper : values()) {
        if (wrapper.getWrappedResultCode() == resultCodeToWrap) {
          return wrapper;
        }
      }
      throw new IllegalArgumentException("ResultCode " + resultCodeToWrap + " can not be wrapped in a DeleteResultCode");
    }
  }
  
  
  public enum CreateResultCode {
    RESULT_OK(ResultCode.RESULT_OK),
    TIMEOUT_LOCAL_CHANGES(ResultCode.TIMEOUT_LOCAL_CHANGES),
    NON_UNIQUE_IDENTIFIER(ResultCode.NON_UNIQUE_IDENTIFIER),
    CLUSTER_TIMEOUT_LOCAL_CHANGES(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES),
    CONFLICTING_REQUEST_FROM_OTHER_NODE(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE),
    RESULT_OK_ONLY_LOCAL_CHANGES_MASTER(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER),
    REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);
    
    private final ResultCode wrappedResultCode;
    
    private CreateResultCode(ResultCode wrappedResultCode) {
      this.wrappedResultCode = wrappedResultCode;
    }
    
    public ResultCode getWrappedResultCode() {
      return wrappedResultCode;
    }
    
    public static CreateResultCode wrapResultCode(ResultCode resultCodeToWrap) {
      for (CreateResultCode wrapper : values()) {
        if (wrapper.getWrappedResultCode() == resultCodeToWrap) {
          return wrapper;
        }
      }
      throw new IllegalArgumentException("ResultCode " + resultCodeToWrap + " can not be wrapped in a CreateResultCode");
    }
  }
  
}
