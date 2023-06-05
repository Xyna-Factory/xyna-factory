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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils.DiscoveryResult;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.update.outdatedclasses_8_2_1_0.XMOMODSConfig;

public class UpdateXMOMODSMappings extends UpdateJustVersion {
  

  public UpdateXMOMODSMappings(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }
  

  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMODSConfig.class);
    Collection<XMOMODSConfig> configs;
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      configs = con.loadCollection(XMOMODSConfig.class);
    } finally {
      con.closeConnection();
    }
    
    GenerationBaseCache parseAdditionalCache = new GenerationBaseCache(); // TODO use global? Should already be filled
    
    List<XMOMODSMapping> tableMappings = new ArrayList<>();
    // apparently xmomodsconfigs can contain table mappings for paths from extens both qualified by base- & sub-fqname 
    Set<String> createdMappingIdentifiers = new HashSet<String>();
    
    // Convert XMOMODSCOnfig to XMOMODSMapping
    for (XMOMODSConfig config : configs) {
      Collection<XMOMODSMapping> conversions = convert(config, parseAdditionalCache);
      Iterator<XMOMODSMapping> conversionIter = conversions.iterator();
      while(conversionIter.hasNext()) {
        XMOMODSMapping conversion = conversionIter.next();
        if (!allowStorage(conversion, createdMappingIdentifiers)) {
          conversionIter.remove();
        }
      }
      tableMappings.addAll(conversions);
    }
    
    tableMappings = handleDuplicateTables(tableMappings);
    
    List<XMOMODSMapping> newTableMappings = new ArrayList<>();
    for (XMOMODSMapping mapping : tableMappings) {
      Collection<XMOMODSMapping> extensions = generateMappingsForExtensions(mapping);
      Iterator<XMOMODSMapping> extensionIter = extensions.iterator();
      while(extensionIter.hasNext()) {
        XMOMODSMapping extension = extensionIter.next();
        if (!allowStorage(extension, createdMappingIdentifiers)) {
          extensionIter.remove();
        }
      }
      newTableMappings.addAll(extensions);
    }
    newTableMappings.addAll(tableMappings);
    
    // delete all entries previously created from startup
    con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteAll(XMOMODSMapping.class);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    //  store all tableMappings
    con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistCollection(newTableMappings);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    // redeploy all storables
    GenerationBase.clearGlobalCache();
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    for (XMOMODSMapping mapping : tableMappings) {
      // Mappings can reverence objects that don't exist, validate vs cache
      XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(mapping.getRevision());
      if (cache != null) {
        XMOMStorableStructureInformation xssi = cache.getStructuralInformation(mapping.getFqxmlname());
        if (xssi != null) {
          GenerationBase gb = DOM.getInstance(mapping.getFqxmlname(), mapping.getRevision());
          if (gb != null) {
            objects.add(gb);
          }
        }
      }
    }
    DeploymentManagement.getInstance().propagateDeployment();
    try {
      GenerationBase.deploy(objects, DeploymentMode.codeChanged, false, WorkflowProtectionMode.FORCE_DEPLOYMENT);
    } catch (Throwable t) {
      logger.warn("Errors during storable class regeneration", t);
    }
  }


  private Set<XMOMODSMapping> convert(XMOMODSConfig config, GenerationBaseCache parseAdditionalCache) throws XynaException {
    Set<XMOMODSMapping> mappings = new HashSet<XMOMODSMapping>();
    Collection<DiscoveryResult>  fqPathsPathsAndTypes = discoverFqPaths(config, parseAdditionalCache);
    for (DiscoveryResult fqPathPathAndType : fqPathsPathsAndTypes) {
      XMOMODSMapping mapping = new XMOMODSMapping();
      mapping.setId(IDGenerator.getInstance().getUniqueId(XMOMODSMapping.ID_REALM));
      mapping.setFqxmlname(fqPathPathAndType.getType());
      mapping.setRevision(config.getRevision());
      mapping.setPath(fqPathPathAndType.getPath());
      mapping.setFqpath(fqPathPathAndType.getFqPath()); 
      mapping.setTablename(config.getOdsName());
      mapping.setColumnname(null);
      mapping.setUserdefined(true); // no way of knowing, but it might be  
      mappings.add(mapping);
    }
    return mappings;
  }

  
  /*
   * Old Algorithm can generate same tablenames for storables that should be separated leading to runtime exceptions
   * for example:
   * Base -> Extension1 . Credentials credentials
   *      -> Extension2 . Credentials credentials
   * both credentials would generate the same tablename      
   */
  private List<XMOMODSMapping> handleDuplicateTables(List<XMOMODSMapping> tableMappings) {
    ConcurrentMap<String, XMOMODSMapping> mappingsByTablename = new ConcurrentHashMap<String, XMOMODSMapping>();
    List<XMOMODSMapping> checked = new ArrayList<>();
    List<XMOMODSMapping> collisions = new ArrayList<XMOMODSMapping>();
    for (XMOMODSMapping mapping : tableMappings) {
      XMOMODSMapping previouslyMapped = mappingsByTablename.putIfAbsent(mapping.getTablename(), mapping);
      if (previouslyMapped != null) {
        if (previouslyMapped.getPath() == null ||
            previouslyMapped.getPath().isBlank()) {
          // ok wenn gleicher fqName
          if (!previouslyMapped.getFqxmlname().equals(mapping.getFqxmlname())) {
            collisions.add(mapping);
          } else {
            checked.add(mapping);
          }
        } else { 
          // different objects on the same table are a probleme as well, but one not caused by the old algorithm
          // same objects in different revisions may have the same tablename
          if (!previouslyMapped.getFqxmlname().equals(mapping.getFqxmlname()) &&
              !previouslyMapped.getFqpath().equals(mapping.getFqpath())) {
            collisions.add(mapping);
          } else {
            checked.add(mapping);
          }
        }
      }
    }
    for (XMOMODSMapping collision : collisions) { 
      String[] pathParts = collision.getPath().split("\\.");
      String simplename = pathParts[pathParts.length - 1].toLowerCase();
      int count = 0;
      if (!mappingsByTablename.containsKey(simplename)) {
        collision.setTablename(simplename);
      } else {
        while (mappingsByTablename.containsKey(simplename + count)) {
          count++;
        }
        collision.setTablename(simplename + count);
      }
      mappingsByTablename.putIfAbsent(collision.getTablename(), collision);
    }
    checked.addAll(mappingsByTablename.values());
    return checked;
  }
  

  private Collection<DiscoveryResult> discoverFqPaths(XMOMODSConfig config, GenerationBaseCache parseAdditionalCache) {
    XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(config.getRevision());
    StorableStructureInformation ssi = cache.getStructuralInformation(config.getFqXmlName());
    if (ssi == null ||
        (ssi.hasSuper() && // return empty, will be handled during generateMappingsForExtensions
         (config.getPath() == null || 
          config.getPath().isBlank()))) {
      // TODO generateMappingsForExtensions will only generate entries for roots
      //      if the extension has configs for new/different paths they are not treated!
      return Collections.emptySet();
    } else {
      try {
        logger.debug("discoverFqPathsForPath for: " + config.getFqXmlName() +"@"+ config.getRevision() + " " + config.getPath());
        return XMOMODSMappingUtils.discoverFqPathsForPath(config.getFqXmlName(), config.getRevision(), config.getPath(), parseAdditionalCache);
      } catch (Throwable t) {
        logger.warn("Error while trying to discover paths for: " + config.getFqXmlName() +"@"+ config.getRevision() + " " + config.getPath());
        return Collections.emptySet();
      }
    }
  }
  
  
  private Collection<XMOMODSMapping> generateMappingsForExtensions(XMOMODSMapping mapping) throws XynaException {
    Collection<XMOMODSMapping> mappings = new ArrayList<>();
    XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(mapping.getRevision());
    StorableStructureInformation ssi = cache.getStructuralInformation(mapping.getFqxmlname());
    if (ssi != null) {
      Set<StorableStructureInformation> resolvesFromPath = followPath(ssi, mapping.getPath());
      for (StorableStructureInformation resolveFromPath : resolvesFromPath) {
        Set<StorableStructureInformation> allSubs = resolveFromPath.getSubEntriesRecursivly();
        for (StorableStructureInformation aSub : allSubs) {
          XMOMODSMapping newMapping = new XMOMODSMapping();
          newMapping.setId(IDGenerator.getInstance().getUniqueId(XMOMODSMapping.ID_REALM));
          newMapping.setFqxmlname(aSub.getFqXmlName());
          newMapping.setRevision(aSub.getDefiningRevision());
          newMapping.setPath(mapping.getPath());
          newMapping.setFqpath(mapping.getFqpath()); 
          newMapping.setTablename(mapping.getTablename());
          newMapping.setColumnname(null);
          newMapping.setUserdefined(mapping.getUserdefined());
          mappings.add(newMapping);
        }
      }
    }
    return mappings;
  }

  
  
  public ODSConnectionType getARelevantConnectionType() {
    if (XMOMPersistenceManagement.defaultPersistenceLayerId.get() >= 0) {
      return ODSConnectionType.DEFAULT;
    }
    if (XMOMPersistenceManagement.defaultHistoryPersistenceLayerId.get() >= 0) {
      return ODSConnectionType.HISTORY;
    }
    if (XMOMPersistenceManagement.defaultAlternativePersistenceLayerId.get() >= 0) {
      return ODSConnectionType.ALTERNATIVE;
    }
    return null;
  }
  
  
  public Set<ODSConnectionType> getAllRelevantConnectionTypes() {
    Set<ODSConnectionType> relevantConTypes = new HashSet<>();
    if (XMOMPersistenceManagement.defaultPersistenceLayerId.get() >= 0) {
      relevantConTypes.add(ODSConnectionType.DEFAULT);
    }
    if (XMOMPersistenceManagement.defaultHistoryPersistenceLayerId.get() >= 0) {
      relevantConTypes.add(ODSConnectionType.HISTORY);
    }
    if (XMOMPersistenceManagement.defaultAlternativePersistenceLayerId.get() >= 0) {
      relevantConTypes.add(ODSConnectionType.ALTERNATIVE);
    }
    return relevantConTypes;
  }
  
  
  
  
  private Set<StorableStructureInformation> followPath(StorableStructureInformation ssi, String path) {
    if (path == null ||
        path.isBlank()) {
      return Collections.singleton(ssi);
    }
    path = path.replaceAll("\\[\\]", "");
    Set<StorableStructureInformation> structures = new HashSet<>();
    String[] pathParts = path.split("\\.");
    structures.addAll(followPathRecursivly(pathParts, 0, ssi.getSuperRootStorableInformation()));
    return structures;
  }
  
  
  private Set<StorableStructureInformation> followPathRecursivly(String[] parts, int index,
                                                                 StorableStructureInformation ssi) {
    Set<StorableStructureInformation>  structures = new HashSet<>();
    StorableColumnInformation sci = ssi.getColumnInfo(parts[index]);
    if (sci == null) {
      return Collections.emptySet();
    }
    if (index +1 < parts.length) {
      structures.addAll(followPathRecursivly(parts, index + 1, ssi));  
    } else {
      if (sci.isStorableVariable()) {
        structures.add(sci.getStorableVariableInformation());
      }
    }
    return structures;
  }
  
  
  private boolean allowStorage(XMOMODSMapping mapping, Set<String> createdMappings) {
    return createdMappings.add(createKey(mapping));
  }

  private String createKey(XMOMODSMapping mapping) {
    StringBuilder sb = new StringBuilder();
    sb.append(mapping.getFqxmlname())
      .append('@')
      .append(mapping.getFqpath())
      .append('@')
      .append(mapping.getRevision());
    return sb.toString();
  }
  
}
