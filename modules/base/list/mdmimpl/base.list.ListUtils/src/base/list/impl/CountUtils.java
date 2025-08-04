/* 
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package base.list.impl;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import java.util.List;

public class CountUtils {
   public static int count(List<GeneralXynaObject> list, CollectionUtils.Filter<GeneralXynaObject> filter) {
      int count = 0;

      for(GeneralXynaObject gxo : list) {
         if (filter.accept(gxo)) {
            ++count;
         }
      }

      return count;
   }
}
