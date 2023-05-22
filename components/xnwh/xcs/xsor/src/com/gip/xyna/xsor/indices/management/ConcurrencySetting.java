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
package com.gip.xyna.xsor.indices.management;

import com.gip.xyna.xsor.indices.tools.AtomicitySupport;
import com.gip.xyna.xsor.indices.tools.AtomicitySupportOptimisticInteger;
import com.gip.xyna.xsor.indices.tools.AtomictySupportLock;


public enum ConcurrencySetting {
  
  OPTIMISTIC {

    @Override
    public AtomicitySupport getAtomicitySupportForSetting() {
      return new AtomicitySupportOptimisticInteger();
    }
  },  
  PESSIMISTIC {

    @Override
    public AtomicitySupport getAtomicitySupportForSetting() {
      return new AtomictySupportLock();
    }
  }; 
  
  public abstract AtomicitySupport getAtomicitySupportForSetting();
  
}
