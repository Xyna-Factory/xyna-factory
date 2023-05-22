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

package com.gip.xyna.XMOM.base.net.internal;

import java.io.Serializable;

import com.gip.xyna.XMOM.base.net.exception.PortValidationException;


public class PortData implements Serializable {

  private static final long serialVersionUID = 1L;
  private final int portNumber;

  public PortData(int portNumber) throws PortValidationException {
    if (portNumber < 0) {
      throw new PortValidationException("" + portNumber);
    }
    if (portNumber > 65536) {
      throw new PortValidationException("" + portNumber);
    }
    this.portNumber = portNumber;
  }

  
  public int getPortNumber() {
    return portNumber;
  }
}
