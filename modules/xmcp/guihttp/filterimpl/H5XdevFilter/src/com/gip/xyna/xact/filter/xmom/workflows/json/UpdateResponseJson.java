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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public class UpdateResponseJson {

  private String id;
  private XMOMGuiJson content;
  private boolean remove;
  
  
  public UpdateResponseJson(String id, XMOMGuiJson content) {
    this.id = id;
    this.content = content;
  }

  public UpdateResponseJson(GBSubObject gbSubObject) {
    this.id = gbSubObject.getObjectId();
    this.content = gbSubObject.getJsonSerializable();
  }
}
