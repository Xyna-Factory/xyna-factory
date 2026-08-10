/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.serialization;



import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;

import xint.mcp.schema.Icon;
import xint.mcp.schema.Resource;



public class ResourceSerializer implements JsonSerializable {

  private final Resource resource;


  public ResourceSerializer(Resource resource) {
    this.resource = resource;
  }


  @Override
  public void toJson(JsonBuilder jb) {
    jb.addStringAttribute("uri", resource.getUri());
    jb.addStringAttribute("name", resource.getName());
    jb.addStringAttribute("title", resource.getTitle());
    jb.addStringAttribute("description", resource.getDescription());
    jb.addStringAttribute("mimeType", resource.getMimeType());
    if (resource.getIcons() != null) {
      jb.addListAttribute("icons");
      for (Icon icon : resource.getIcons()) {
        jb.addObjectListElement(new IconSerializer(icon));
      }
      jb.endList();
    }
  }

}
