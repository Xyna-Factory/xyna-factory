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

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;

public class RuntimeContextVisitor extends EmptyJsonVisitor<RuntimeContext> {
  
  public final static String WORKSPACE_LABEL = "workspace";
  public final static String APPLICATION_LABEL = "application";
  public final static String VERSION_LABEL = "version";

  private String app;
  private String ver;
  private String ws;
   
  public RuntimeContext get() {
    return RevisionManagement.getRuntimeContext(app, ver, ws);
  }

  @Override
  public RuntimeContext getAndReset() {
    RuntimeContext rc = RevisionManagement.getRuntimeContext(app, ver, ws);
    app = null;
    ver = null;
    ws = null;
    return rc;
  }
  
  
  @Override
  public void attribute(String label, String value, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
                  throws UnexpectedJSONContentException {
    if (label.equals(WORKSPACE_LABEL)) {
      this.ws = value;
    } else if (label.equals(APPLICATION_LABEL)) {
      this.app = value;
    } else if (label.equals(VERSION_LABEL)) {
      this.ver = value;
    }
  }
   
 }