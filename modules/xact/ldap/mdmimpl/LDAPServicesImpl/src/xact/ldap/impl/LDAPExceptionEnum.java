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
package xact.ldap.impl;

import xact.ldap.exceptions.AuthenticationFault;
import xact.ldap.exceptions.ConnectionFault;
import xact.ldap.exceptions.ControlNotSupported;
import xact.ldap.exceptions.InsufficientAccessRights;
import xact.ldap.exceptions.InvalidDNSyntax;
import xact.ldap.exceptions.LDAPException;
import xact.ldap.exceptions.NamingViolation;
import xact.ldap.exceptions.NoSuchObject;
import xact.ldap.exceptions.NotAllowedOnNonLeaf;
import xact.ldap.exceptions.ObjectClassViolation;
import xact.ldap.exceptions.ObjectDoesAlreadyExist;
import xact.ldap.exceptions.SizeLimitExceeded;


public enum LDAPExceptionEnum {
  
  OPERATIONS_ERROR(1, null),
  PROTOCOL_ERROR(2, null),
  TIME_LIMIT_EXCEEDED(3, null),
  SIZE_LIMIT_EXCEEDED(4, SizeLimitExceeded.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new SizeLimitExceeded(message, resultCode);
    }
  },
  COMPARE_FALSE(5, null),
  COMPARE_TRUE(6, null),
  AUTH_METHOD_NOT_SUPPORTED(7, AuthenticationFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new AuthenticationFault(message, resultCode);
    }
  },
  STRONG_AUTH_REQUIRED(8, AuthenticationFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new AuthenticationFault(message, resultCode);
    }
  },
  REFERRAL(9, null),
  ADMIN_LIMIT_EXCEEDED(10, null),
  UNAVAILABLE_CRITICAL_EXTENSION(11, ControlNotSupported.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ControlNotSupported(message, resultCode);
    }
  },
  CONFIDENTIALITY_REQUIRED(12, null),
  SASL_BIND_IN_PROGRESS(13, null),
  NO_SUCH_ATTRIBUTE(14, NoSuchObject.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new NoSuchObject(message, resultCode);
    }
  },
  UNDEFINED_ATTRIBUTE_TYPE(15, NoSuchObject.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new NoSuchObject(message, resultCode);
    }
  },
  INAPPROPRIATE_MATCHING(16, null),
  CONSTRAINT_VIOLATION(17, null),
  ATTRIBUTE_OR_VALUE_EXISTS(18, ObjectDoesAlreadyExist.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ObjectDoesAlreadyExist(message, resultCode);
    }
  },
  INVALID_ATTRIBUTE_SYNTAX(19, null),
  NO_SUCH_OBJECT(32, NoSuchObject.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new NoSuchObject(message, resultCode);
    }
  },
  ALIAS_PROBLEM(33, null),
  INVALID_DN_SYNTAX(34, InvalidDNSyntax.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new InvalidDNSyntax(message, resultCode);
    }
  },
  IS_LEAF(35, null),
  ALIAS_DEREFERENCING_PROBLEM(36, null),
  INAPPROPRIATE_AUTHENTICATION(48, AuthenticationFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new AuthenticationFault(message, resultCode);
    }
  },
  INVALID_CREDENTIALS(49, AuthenticationFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new AuthenticationFault(message, resultCode);
    }
  },
  INSUFFICIENT_ACCESS_RIGHTS(50, InsufficientAccessRights.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new InsufficientAccessRights(message, resultCode);
    }
  },
  BUSY(51, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  UNAVAILABLE(52, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  UNWILLING_TO_PERFORM(53, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  LOOP_DETECT(54, null),
  NAMING_VIOLATION(64, NamingViolation.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new NamingViolation(message, resultCode);
    }
  },
  OBJECT_CLASS_VIOLATION(65, ObjectClassViolation.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ObjectClassViolation(message, resultCode);
    }
  },
  NOT_ALLOWED_ON_NONLEAF(66, NotAllowedOnNonLeaf.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new NotAllowedOnNonLeaf(message, resultCode);
    }
  },
  NOT_ALLOWED_ON_RDN(67, null),
  ENTRY_ALREADY_EXISTS(68, ObjectDoesAlreadyExist.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ObjectDoesAlreadyExist(message, resultCode);
    }
  },
  OBJECT_CLASS_MODS_PROHIBITED(69, null),
  AFFECTS_MULTIPLE_DSAS(71, null),
  OTHER(80, null),
  // local errors
  SERVER_DOWN(81, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  LOCAL_ERROR(82, null),
  ENCODING_ERROR(83, null),
  DECODING_ERROR(84, null),
  LDAP_TIMEOUT(85, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  AUTH_UNKNOWN(86, AuthenticationFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new AuthenticationFault(message, resultCode);
    }
  },
  FILTER_ERROR(87, null),
  USER_CANCELLED(88, null),
  NO_MEMORY(90, null),
  CONNECT_ERROR(91, ConnectionFault.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ConnectionFault(message, resultCode);
    }
  },
  LDAP_NOT_SUPPORTED(92, null),
  CONTROL_NOT_FOUND(93, ControlNotSupported.class) {
    @Override
    public Throwable generateThrowable(String message, int resultCode) {
      return new ControlNotSupported(message, resultCode);
    }
  },
  NO_RESULTS_RETURNED(94, null),
  MORE_RESULTS_TO_RETURN(95, null),
  CLIENT_LOOP(96, null),
  REFERRAL_LIMIT_EXCEEDED(97, null),
  INVALID_RESPONSE(100, null),
  AMBIGUOUS_RESPONSE(101, null),
  TLS_NOT_SUPPORTED(112, null);
  
  
  private final int ldapExceptionCode;
  private final Class<? extends LDAPException> appropriateXynaException; // TODO can be removed as XynaExceptions don't have a noArg constructor and therefor can't be instantiated with class.newInstance 
  
  private LDAPExceptionEnum(int ldapExceptionCode, Class<? extends LDAPException> appropriateXynaException) {
    this.ldapExceptionCode = ldapExceptionCode;
    this.appropriateXynaException = appropriateXynaException;
  }
  
  
  public static LDAPExceptionEnum getByExceptionCode(int ldapExceptionCode) {
    for (LDAPExceptionEnum exception : values()) {
      if (exception.ldapExceptionCode == ldapExceptionCode) {
        return exception;
      }
    }
    return OTHER;
  }
  
  
  public Throwable generateThrowable(String message, int resultCode) {
    return new RuntimeException(resultCode + ": " + message);
  }
  
  public int getLdapExceptionCode() {
    return ldapExceptionCode;
  }
  
  public Class<? extends LDAPException> getAppropriateXynaException() {
    return appropriateXynaException;
  }
  
  
  public static Throwable transformLDAPExceptionToAppropriateXynaException(com.novell.ldap.LDAPException exception) {
    int resultCode = exception.getResultCode();
    String message = exception.getMessage();
    Throwable throwable = getByExceptionCode(resultCode).generateThrowable(message, resultCode);
    throwable.initCause(exception);
    return throwable;
  }
  

}
