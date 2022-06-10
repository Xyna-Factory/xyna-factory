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

package com.gip.xyna.xact.filter.util.xo;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

public class ListTypeAwareVistor extends GenericVisitor{

  // JsonParser is not aware of $list and $primitivelist
  // if a list contains only null entries, it returns it as a list
  // regardless of type.
  // This Visitor turns falsely classified primitive lists into
  // objectLists, if they only contain null values.
  public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
    if(!label.equals("$list"))
      super.list(label, values, type);
    else {
      List<Object> translatedValue = new ArrayList<Object>();
      
      for(int i=0; i<values.size(); i++) {
        translatedValue.add(null);
      }
      
      objectList(label, translatedValue);     
    }
  }
  
  public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
    return new ListTypeAwareVistor();
  }
}
