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
package xfmg.oas.mcp.impl;



import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;

import xint.mcp.schema.Prompt;
import xint.mcp.schema.Resource;
import xint.mcp.schema.Tool;
import xfmg.oas.mcp.OasMcpPrimitivesSuperProxy;
import xfmg.oas.mcp.impl.prompts.ImportSpecPrompt;
import xfmg.oas.mcp.impl.resources.ImportHistoryResource;
import xfmg.oas.mcp.impl.tools.ImportOasSpecTool;
import xfmg.oas.mcp.impl.tools.ListOasEndpointsTool;
import xfmg.oas.mcp.OasMcpPrimitivesInstanceOperation;
import xfmg.oas.mcp.OasMcpPrimitives;



public class OasMcpPrimitivesInstanceOperationImpl extends OasMcpPrimitivesSuperProxy implements OasMcpPrimitivesInstanceOperation {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = CentralFactoryLogging.getLogger(OasMcpPrimitivesInstanceOperationImpl.class);


  public OasMcpPrimitivesInstanceOperationImpl(OasMcpPrimitives instanceVar) {
    super(instanceVar);
  }


  public List<? extends Prompt> getPrompts() {
    return List.of(new ImportSpecPrompt());
  }


  public List<? extends Resource> getResources() {
    List<Resource> resources = new ArrayList<>();
    resources.addAll(ImportHistoryResource.listResources());
    return resources;
  }


  public List<? extends Tool> getTools() {
    Long revision = null;
    try {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      revision = clb.getRevision();
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not determine revision.", e);
      }
      return Collections.emptyList();
    }
    return List.of(new ImportOasSpecTool(revision), new ListOasEndpointsTool());
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

}
