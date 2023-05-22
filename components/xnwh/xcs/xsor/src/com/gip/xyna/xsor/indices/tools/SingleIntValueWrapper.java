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
package com.gip.xyna.xsor.indices.tools;

import com.gip.xyna.xsor.common.IntegrityAssertion;



public class SingleIntValueWrapper implements IntValueWrapper {
  
  int value;

  public SingleIntValueWrapper(int value) {
    this.value = value;
  }
  
  
  public int[] getValues() {
    return new int[] {value};
  }
  

  public IntValueWrapper addValue(int value) {
    if (this.value == value) {
      return new SingleIntValueWrapper(value);
    } else {
      if (this.value < value) {
        return new MultiIntValueWrapper(this.value, value);
      } else {
        return new MultiIntValueWrapper(value, this.value);
      }
    }
  }

  
  public IntValueWrapper removeValue(@IntegrityAssertion int value) {
    assert (this.value == value) : "value to remove not found: " + value + " in " + this.value; 
    return null;
  }
  

  public IntValueWrapper replaceValue(@IntegrityAssertion int oldValue, int newValue) {
    assert (this.value == oldValue) : "value to update not found: " + oldValue + " in " + value + " newValue: " + newValue;
    return new SingleIntValueWrapper(newValue);
  }

 

}
