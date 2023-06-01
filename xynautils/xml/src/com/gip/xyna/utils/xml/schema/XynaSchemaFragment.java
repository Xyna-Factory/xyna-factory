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
package com.gip.xyna.utils.xml.schema;

/**
 * Ein Teilfragment eines Schemas, welches aus einem Schema ausgeschnitten und
 * in ein anderes eingef�gt werden kann. Ein Fragment an sich kann gar nichts.
 * Wenn es in ein Schema eingef�gt wird, erh�lt es dessen Namespace.
 */
public class XynaSchemaFragment {

   private XynaSchemaNode node;

   protected XynaSchemaFragment(XynaSchemaNode node) {
      this.node = node;
   }

   protected XynaSchemaNode getNode() {
      return node;
   }

}
