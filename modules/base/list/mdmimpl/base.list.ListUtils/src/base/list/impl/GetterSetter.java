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

import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

public class GetterSetter {
   private String path;

   public GetterSetter(String path) {
      if (path.startsWith("%")) {
         int idx = path.indexOf(46);
         this.path = path.substring(idx + 1);
      } else {
         this.path = path;
      }

   }

   public Object getFrom(GeneralXynaObject gxo) throws InvalidObjectPathException {
      return gxo.get(this.path);
   }

   public String getPath() {
      return this.path;
   }

   public GeneralXynaObject setTo(GeneralXynaObject gxo, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
      gxo.set(this.path, o);
      return gxo;
   }
}
