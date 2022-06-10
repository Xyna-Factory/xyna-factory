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

package com.gip.juno.ws.exceptions;

public class DPPWebserviceAuthenticationException extends DPPWebserviceException {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final String _domain = "A";

  public DPPWebserviceAuthenticationException(String message, String errorNumber) {
    super(new MessageBuilder().setDomain(_domain).setErrorNumber(errorNumber).setDescription(message));
  }
  
  public DPPWebserviceAuthenticationException(String message, String errorNumber, Throwable cause) {
    super(new MessageBuilder().setDomain(_domain).setErrorNumber(errorNumber).setDescription(message).setCause(cause));
  }
  
  public DPPWebserviceAuthenticationException(String message) {
    super(new MessageBuilder().setDomain(_domain).setDescription(message));
  }
  
  public DPPWebserviceAuthenticationException(String message, Throwable cause) {    
    super(new MessageBuilder().setDomain(_domain).setDescription(message).setCause(cause));
  }
  
  public DPPWebserviceAuthenticationException(Throwable cause) {
    super(new MessageBuilder().setDomain(_domain).setCause(cause));
  }
   
  public DPPWebserviceAuthenticationException(MessageBuilder builder, Throwable cause) {    
    super(builder.setDomain(_domain).setCause(cause));
  }
  
  public DPPWebserviceAuthenticationException(MessageBuilder builder) {    
    super(builder.setDomain(_domain));
  }
}

