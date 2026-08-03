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
package com.gip.xyna.xact.filter;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

import xint.mcp.MCPPrimitivesProvider;
import xint.mcp.schema.Prompt;
import xint.mcp.schema.Resource;
import xint.mcp.schema.Tool;



public class McpPrimitivesData {

  private static Logger logger = CentralFactoryLogging.getLogger(McpPrimitivesData.class);

  private final List<Tool> tools;
  private final List<Prompt> prompts;
  private final List<Resource> resources;


  public McpPrimitivesData(long revision) {
    Collection<MCPPrimitivesProvider> providers = findProviders(revision);
    List<Tool> tmpTools = new ArrayList<>();
    List<Prompt> tmpPrompts = new ArrayList<>();
    List<Resource> tmpResources = new ArrayList<>();

    for (MCPPrimitivesProvider provider : providers) {
      List<? extends Tool> providedTools = provider.getTools();
      List<? extends Prompt> providedPrompts = provider.getPrompts();
      List<? extends Resource> providedResources = provider.getResources();
      tmpTools.addAll(providedTools == null ? Collections.emptyList() : providedTools);
      tmpPrompts.addAll(providedPrompts == null ? Collections.emptyList() : providedPrompts);
      tmpResources.addAll(providedResources == null ? Collections.emptyList() : providedResources);
    }

    tools = tmpTools;
    prompts = tmpPrompts;
    resources = tmpResources;

    if (logger.isDebugEnabled()) {
      logger.debug("Created/Updated available mcp primitives for revision " + revision);
    }

  }


  private List<MCPPrimitivesProvider> findProviders(long revision) {
    List<MCPPrimitivesProvider> result = new ArrayList<>();
    Set<GenerationBase> subtypes = searchForSubtypes(MCPPrimitivesProvider.class.getCanonicalName(), revision);

    for (GenerationBase subType : subtypes) {
      if (!subType.isAbstract()) {
        GeneralXynaObject obj = XynaObject.instantiate(subType.getOriginalFqName(), true, revision);
        if (!(obj instanceof MCPPrimitivesProvider)) {
          continue;
        }
        result.add((MCPPrimitivesProvider) obj);
      }
    }

    return result;
  }


  private Set<GenerationBase> searchForSubtypes(String instanceDatatype, long revision) {
    try {
      DOM dom = DOM.generateUncachedInstance(instanceDatatype, true, getMcpRevision(revision));
      dom.parseGeneration(true, true);
      return dom.getSubTypes(new GenerationBaseCache());
    } catch (Exception e) {
      logger.warn("Error occured while searching in revision.", e);
      return new HashSet<GenerationBase>();
    }
  }


  private Long getMcpRevision(long revision) throws Exception {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    return cld.getMDMClassLoader(MCPPrimitivesProvider.class.getCanonicalName(), revision).getRevision();
  }


  public List<Tool> getTools() {
    return tools;
  }


  public List<Prompt> getPrompts() {
    return prompts;
  }


  public List<Resource> getResources() {
    return resources;
  }
}
