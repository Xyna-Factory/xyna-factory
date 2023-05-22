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
package xmcp.factorymanager.impl.converter.payload;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;

public class RuntimeContextJson implements JsonSerializable {

  private static final String LABEL_WORKSPACE = "workspace";
  private static final String LABEL_APPLICATION = "application";
  private static final String LABEL_VERSION = "version";
  
  private String workspace;
  private String application;
  private String version;
  
  private RuntimeContextJson() {
  }
  
  public RuntimeContextJson(RuntimeContext runtimeContext) {
    if( runtimeContext instanceof Workspace ) {
      this.workspace = runtimeContext.getName();
    } else if( runtimeContext instanceof Application ) {
      this.application = runtimeContext.getName();
      this.version = ((Application)runtimeContext).getVersionName();
    }
  }

  @Override
  public void toJson(JsonBuilder jb) {
    jb.addOptionalStringAttribute(LABEL_WORKSPACE, workspace);
    jb.addOptionalStringAttribute(LABEL_APPLICATION, application);
    jb.addOptionalStringAttribute(LABEL_VERSION, version);
  }
  
  public RuntimeContext toRuntimeContext() {
    if( workspace != null ) {
      return new Workspace(workspace);
    } else {
      return new Application(application,version);
    }
  }
  

  public static JsonVisitor<RuntimeContextJson> getJsonVisitor() {
    return new RuntimeContextJsonVisitor();
  }
  
  public static class RuntimeContextJsonVisitor extends EmptyJsonVisitor<RuntimeContextJson> {
    private RuntimeContextJson rcj = new RuntimeContextJson();
    
    @Override
    public RuntimeContextJson get() {
      return rcj;
    }

    @Override
    public RuntimeContextJson getAndReset() {
      RuntimeContextJson ret = rcj;
      rcj = new RuntimeContextJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(LABEL_WORKSPACE) ) {
        rcj.workspace = value;
        return;
      }
      if( label.equals(LABEL_APPLICATION) ) {
        rcj.application = value;
        return;
      }
      if( label.equals(LABEL_VERSION) ) {
        rcj.version = value;
        return;
      }
      throw new UnexpectedJSONContentException(label);
    }
    
  }
  
}
