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
package com.gip.xyna.openapi;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseValidator {

    private String name;
    private boolean isNullable;
    private boolean isRequired;
    
    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return getClass().getName();
    }

    public void setNullable() {
        isNullable = true;
    }

    public void setRequired() {
        isRequired = true;
    }

    public boolean getRequired() {
        return isRequired;
    }

    public List<String> checkValid() {
        List<String> errorMessages = new ArrayList<String>();
        
        if (isRequired && isNull()) {
            errorMessages.add(name + ": Property is required but has value null");
        }
        
        return errorMessages;
    }

    abstract boolean isNull();
}
