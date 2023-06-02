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
package xfmg.xfctrl.datamodel.csv.impl.fields;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

public class XmomSimpleField extends AbstractXmomField {

  public XmomSimpleField(String label, String name, XmomField parent, FieldType fieldType, String fqClassName) {
    super(label, name, parent, fieldType, fqClassName);
  }
  
  @Override
  public Object getObject(GeneralXynaObject gxo) {
    return getBaseObject(gxo);
  }


}
