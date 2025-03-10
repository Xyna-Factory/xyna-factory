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
// !DO NOT EDIT THIS FILE!
// This source file is generated by Oracle tools
// Contents may be subject to change
// For reporting problems, use the following
// Version = Oracle WebServices (10.1.3.1.1, build 070111.22769)

package com.gip.xyna._1_5.xsd.common._1;


public class SuspendOrder implements java.io.Serializable {
    protected java.lang.String externalOrderNumber;
    protected java.lang.String cause;
    protected boolean releaseCapsImmediately;
    
    public SuspendOrder() {
    }
    
    public java.lang.String getExternalOrderNumber() {
        return externalOrderNumber;
    }
    
    public void setExternalOrderNumber(java.lang.String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }
    
    public java.lang.String getCause() {
        return cause;
    }
    
    public void setCause(java.lang.String cause) {
        this.cause = cause;
    }
    
    public boolean isReleaseCapsImmediately() {
        return releaseCapsImmediately;
    }
    
    public void setReleaseCapsImmediately(boolean releaseCapsImmediately) {
        this.releaseCapsImmediately = releaseCapsImmediately;
    }
}
