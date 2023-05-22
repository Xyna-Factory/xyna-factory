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
package com.gip.xyna.xact.filter.xmom.session.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;

public class GboJson implements JsonSerializable {
  
  private final GenerationBaseObject gbo;

  public GboJson(GenerationBaseObject gbo) {
    this.gbo = gbo;
  }

  @Override
  public void toJson(JsonBuilder jb) {
    jb.addStringAttribute("fqName", gbo.getFQName().getFqName() );
    jb.addIntegerAttribute("revision", gbo.getRevision() );
    jb.addBooleanAttribute("saveState", gbo.getSaveState());
    jb.addStringAttribute("deploymentState", gbo.getDeploymentState());
  }

  public static List<GboJson> list(Collection<GenerationBaseObject> gbos) {
    List<GboJson> json = new ArrayList<GboJson>();
    for( GenerationBaseObject gbo : gbos ) {
      json.add( new GboJson(gbo) );
    }
    return json;
  }

}
