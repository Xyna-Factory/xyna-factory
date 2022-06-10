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
package com.gip.xyna.xnwh.persistence.xmom;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.tools.JavaFileObject;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Graph;
import com.gip.xyna.utils.collections.Graph.HasUniqueStringIdentifier;
import com.gip.xyna.utils.collections.Graph.Node;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exception.MultipleExceptionHandler;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidXMOMStorablePathException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSMappingToNonStorableException;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameChangedButNotDeployedException;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameMustBeUniqueException;
import com.gip.xyna.xnwh.exceptions.XNWH_StorableNotFoundException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils.NameType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceOperationAlgorithms.TypeCollectionVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.ReferenceStorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.generation.InMemoryStorableClassLoader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XMOMObjectDoesNotExist;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.compile.CompilationResult;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet.TargetKind;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaMemoryObject;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class XMOMPersistenceManagement extends FunctionGroup implements XMOMPersistenceOperations {

  static {
    addDependencies(XMOMPersistenceManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaProcessing.class,
                                                                           XynaFractalWorkflowEngine.class,
                                                                           DeploymentHandling.class)})));
  }

  private static final Logger logger = CentralFactoryLogging.getLogger(XMOMPersistenceManagement.class);

  /**
   * xmom oberklasse 
   */
  public static final String STORABLE_BASE_CLASS = "xnwh.persistence.Storable";
  
  public final static String PERSISTENCE_RIGHT_SCOPE_KEY = "xnwh.persistence.Storables";
  public final static String PERSISTENCE_RIGHT_SCOPE_DEFINITION = PERSISTENCE_RIGHT_SCOPE_KEY + ":[read, write, insert, delete, *]:*:*";
  public final static String PERSISTENCE_RIGHT_SCOPE_DOCUMENTATION = "Access rights for xmom storables.";
  public final static String PERSISTENCE_RIGHT_SCOPE_ALL_ACCESS = PERSISTENCE_RIGHT_SCOPE_KEY + ":*:*:*";

  private final static String EMPTYLIST = "emptylist";
  private static XMOMPersistenceOperations operationsImpl;
  
  /**
   * speichert komplexe spalten die xynaobjects sind, nicht per javaserialisierung sondern per xml-serialisierung
   */
  public static abstract class XMOMXMLSerializationStorable<T extends Storable<?>> extends Storable<T> {

    private static final long serialVersionUID = 1L;


    @Override
    public void serializeByColName(String colName, Object val, OutputStream os) throws IOException {
      String xml = null;
      Long revision = VersionManagement.REVISION_WORKINGSET; //wenn unten die revision nicht umgesetzt wird, kann das sein, weil die objekte serverintern gehalten werden? z.b. xynaexception?
      if (val != null) {
        if (val instanceof GeneralXynaObject) {
          GeneralXynaObject gxo = (GeneralXynaObject) val;
          xml = gxo.toXml();
          ClassLoader cl = gxo.getClass().getClassLoader();
          if (cl instanceof ClassLoaderBase) {
            ClassLoaderBase clb = (ClassLoaderBase) cl;
            revision = clb.getRevision();
          }
        } else if (val instanceof List) {
          List l = (List) val;
          if (l.size() > 0) {
            XynaObjectList xol = new XynaObjectList(l, l.get(0).getClass());
            xml = xol.toXml();
            ClassLoader cl = l.get(0).getClass().getClassLoader();
            if (cl instanceof ClassLoaderBase) {
              ClassLoaderBase clb = (ClassLoaderBase) cl;
              revision = clb.getRevision();
            }
          } else {
            xml = EMPTYLIST;
          }
        }
      }
      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(xml);
      oos.writeObject(revision);
      oos.flush();
    }


    @Override
    public Object deserializeByColName(String colName, InputStream is) throws IOException {
      ObjectInputStream ois = getObjectInputStreamForStorable(is);
      String xml;
      try {
        xml = (String) ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      Long revision;
      try {
        revision = (Long) ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (xml == null) {
        return null;
      }
      if (EMPTYLIST.equals(xml)) {
        return new ArrayList<Object>();
      }
      try {
        return XynaObject.generalFromXml(xml, revision);
      } catch (XPRC_XmlParsingException e) {
        throw new RuntimeException(xml, e);
      } catch (XPRC_InvalidXMLForObjectCreationException e) {
        throw new RuntimeException(xml, e);
      } catch (XPRC_MDMObjectCreationException e) {
        logger.warn("xml could not be deserialized: " + xml, e);
        throw (IOException) (new IOException("could not deserialize xml").initCause(e));
      }
    }

  }


  public static enum SerializationType {
    JAVA_SERIALIZATION, XML;
  }


  public static final String DEFAULT_NAME = XMOMPersistenceManagement.class.getSimpleName();
  public static final XynaPropertyInt defaultPersistenceLayerId =
      new XynaPropertyInt("xnwh.persistence.xmom.defaultpersistencelayerid", -1)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "Persistence layer id used as default for additional xmom storables. -1 means that no specific persistence layer is used.");
  public static final XynaPropertyInt defaultHistoryPersistenceLayerId =
      new XynaPropertyInt("xnwh.persistence.xmom.defaulthistorypersistencelayerid", -1)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "History persistence layer id used as default for additional xmom storables. -1 means that no specific persistence layer is used.");
  public static final XynaPropertyInt defaultAlternativePersistenceLayerId =
      new XynaPropertyInt("xnwh.persistence.xmom.defaultalternativepersistencelayerid", -1)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "Alternative persistence layer id used as default for additional xmom storables. -1 means that no specific persistence layer is used.");
  public static final XynaPropertyInt maximumTablenameLength =
      new XynaPropertyInt("xnwh.persistence.xmom.maximumtablenamelength", 64)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "Maximum amount of characters a tablename may not exceed.");
  
  /**
   * beim ausprobieren von stark deadlock-anfälligen stores war 1 ausreichend - 3 ist auf nummer sicher.
   * 
   * bei oracle ist das problem scheinbar nicht aufgetreten, sondern nur bei mysql.
   */
  protected static final XynaPropertyInt DEADLOCK_RETRIES = new XynaPropertyInt("xnwh.persistence.concurrency.retries", 3).
    setDefaultDocumentation(DocumentationLanguage.EN, "The amount of retries on concurrent access.").
    setDefaultDocumentation(DocumentationLanguage.DE, "The Anzahl an Wiederholungen bei konkurrierendem Zugriff.");

  protected static final XynaPropertyDuration DEADLOCK_RETRY_TIMEOUT = 
    new XynaPropertyDuration("xnwh.persistence.concurrency.timeout", new Duration(30, TimeUnit.SECONDS), TimeUnit.SECONDS).
    setDefaultDocumentation(DocumentationLanguage.EN, "Duration before aborting the persistence operation after repeated collisions.").
    setDefaultDocumentation(DocumentationLanguage.DE, "Zeitspanne nach der die Persistenz-Operation abgebrochen wird nachdem Sie mehrfach mit anderen Anfragen kollidierte.");
  

  private IDGenerator idgen;
  private ODS ods;


  public XMOMPersistenceManagement() throws XynaException {
    super();
  }


  /**
   * für die factory initialisierung genutzt
   */
  private XMOMPersistenceManagement(String cause) throws PersistenceLayerException {
    super(cause);
    init_internally();
  }


  public static XMOMPersistenceManagement getXMOMPersistenceManagementPreInit() throws XynaException {
    return new XMOMPersistenceManagement("preInit");
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  private void init_internally() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMODSMapping.class);
    operationsImpl = new XMOMPersistenceOperationAlgorithms();
  }

  public static final String IDGEN_REALM = XMOMPersistenceManagement.class.getSimpleName();

  @Override
  protected void init() throws XynaException {
    //prio: muss vor "deploy_new" durchgeführt werden, weil dort die storables registriert werden
    //      muss nach "undeploy_old" ausgeführt werden, damit beim deregistrieren (z.b. in persistencelayers)
    //        die alte structure verwendet wird (storable könnte sich strukturell geändert haben)
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
    .addDeploymentHandler(DeploymentHandling.PRIORITY_WORKFLOW_DATABASE, new StructureCacheRegistrator());
    
    //prio: muss nach dem aufruf von XynaObject.undeploy aufgerufen werden -> also als letztes schadet nix
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_DEPENDENCY_CREATION, new StructureCacheUnregistrator());

    idgen = IDGenerator.getInstance();
    idgen.setBlockSize(IDGEN_REALM, 10000);
    init_internally();
  }
  
  public long genId() {
    if (idgen == null) {
      return IDGenerator.generateUniqueIdForThisSession();
    } else {
      return idgen.getUniqueId(IDGEN_REALM);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
  }


  public static String createKey(String xmomStorableFqXMLName, String path) {
    return xmomStorableFqXMLName + "#" + path;
  }


  public static class StructureCacheRegistrator implements DeploymentHandler {
    
    private InMemoryCompilationSet set;
    
    private static class StorableNode implements HasUniqueStringIdentifier {

      private final DOM dom;
      private final String id;
      private boolean dependencyCalculationWillBeDone = false;
      
      public StorableNode(DOM dom) {
        this.dom = dom;
        this.id = dom.getRevision() + "-" + dom.getFqClassName();
      }
      
      @Override
      public String getId() {
        return id;
      }

      @Override
      public int hashCode() {
        return id.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null) {
          return false;
        }
        if (getClass() != obj.getClass()) {
          return false;
        }
        StorableNode other = (StorableNode) obj;
        return id.equals(other.id);
      }
      
    }
    
    /*
     * rekursion zu subtypen, basistypen und referenzen passiert beim registrieren => deshalb kann man aus der liste
     * alle subtypen rausschmeissen und alle storables, die von anderen storables in der liste über referenzen
     * erreichbar sind.
     * 
     * dazu erstelle einen gerichteten graph, der alle storables enthält. die kanten sind beziehungen "hat subtype X" und "hat im expansiven baum eine referenz auf X".
     * 
     */
    private Map<StorableNode, Node<StorableNode>> graphData;
    private GenerationBaseCache gbc;
    private Set<DOM> domsThatCacheSubtypes; 

    public void exec(GenerationBase gb, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
      if (gb instanceof DOM) {
        DOM d = (DOM) gb;
        if (domsThatCacheSubtypes.add(d)) {
          d.setCacheSubTypes(true);  
        }
        if (d.isInheritedFromStorable()) {
          getOrAddNode(d, true);
        }
        for (GenerationBase root : d.getRootXMOMStorablesUsingThis(gbc, new HashSet<GenerationBase>())) {
          DOM dom = (DOM) root;
          if (domsThatCacheSubtypes.add(dom)) {
            dom.setCacheSubTypes(true);  
          }
          getOrAddNode(dom, true);
        }
      }
    }
    

    private Node<StorableNode> getOrAddNode(DOM d, boolean includeDependencies) {
      if (d.isStorableEquivalent()) {
        return null;
      }
      StorableNode sn = new StorableNode(d);
      Node<StorableNode> node = graphData.get(sn);
      if (node != null) {
        if (!includeDependencies) {
          return node;
        }
        if (node.getContent().dependencyCalculationWillBeDone) {
          return node;
        }
      } else {
        node = new Node<StorableNode>(sn);
        graphData.put(sn, node);
      }
      if (includeDependencies) {
        node.getContent().dependencyCalculationWillBeDone = true;
        //create connections
        for (GenerationBase subtype : d.getSubTypes(gbc, false)) {
          addDependency(node, (DOM) subtype, true);
        }

        Set<DOM> referencedStorables = new HashSet<>();
        Set<DOM> visited = new HashSet<>();
        collectReferencedStorables(d, d, referencedStorables, "", false, visited);
        for (DOM ref : referencedStorables) {
          addDependency(node, ref, false);
        }
      }
      return node;
    }

    private void addDependency(Node<StorableNode> node, DOM d, boolean includeDependencies) {
      Node<StorableNode> dep = getOrAddNode(d, includeDependencies);
      if (dep != null) {
        node.addDependency(dep);
      }
    }


    private void collectReferencedStorables(DOM rootStorable, DOM d, Set<DOM> referencedStorables, String path, boolean checkInheritedMembers, Set<DOM> visited) {
      if (domsThatCacheSubtypes.add(d)) {
        d.setCacheSubTypes(true);  
      }
      if (!visited.add(d)) {
        return;
      }
      for (AVariable v : checkInheritedMembers ? d.getAllMemberVarsIncludingInherited() : d.getMemberVars()) {
        if (!v.isJavaBaseType() && v.getDomOrExceptionObject() instanceof DOM) {
          DOM child = (DOM) v.getDomOrExceptionObject();
          String localPath = path;
          if (path.length() > 0) {
            localPath += ".";
          }
          localPath += v.getVarName();
          if (XMOMStorableStructureCache.isTransient(rootStorable, v, localPath)) {
            continue;
          }
          if (rootStorable.getPersistenceInformation().getReferences().contains(localPath)) {
            referencedStorables.add(child);
          } else {
            collectReferencedStorables(rootStorable, child, referencedStorables, localPath, true, visited);
          }
        }
      }
      if (d != rootStorable) {
        for (GenerationBase subtype : d.getSubTypes(gbc, false)) {
          collectReferencedStorables(rootStorable, (DOM) subtype, referencedStorables, path, false, visited);
        }
      }
    }


    private void enrichClasspath(DOM dom) {
      HashSet<String> jars = new HashSet<String>();
      try {
        dom.getDependentJarsWithoutRecursion(jars, true, false);
      } catch (XPRC_JarFileForServiceImplNotFoundException e) {
        throw new RuntimeException(e);
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
      
      Set<Long> revisions = new HashSet<Long>();
      for (GenerationBase gb : dom.getDependenciesRecursively().getDependencies(true)) {
        if (gb instanceof DOM) {
          try {
            ((DOM) gb).getDependentJarsWithRecursion(jars, true, true);
          } catch (XPRC_JarFileForServiceImplNotFoundException e) {
            throw new RuntimeException(e);
          } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
            throw new RuntimeException(e);
          }
        }
        revisions.add(gb.getRevision());
      }
      revisions.add(dom.getRevision());

      for (Long rev : revisions) {
        File mdmclasses = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, rev));
        if (!mdmclasses.exists()) {
          mdmclasses.mkdir();
        }
        jars.add(mdmclasses.getPath());
      }

      for (String s : jars) {
        set.addToClassPath(s);
      }
      
      DOM baseType = dom.getSuperClassGenerationObject();
      while (baseType != null &&
             !baseType.isStorableEquivalent()) {
        XMOMStorableStructureInformation entry = XMOMStorableStructureCache.getInstance(baseType.getRevision()).getStructuralInformation(baseType.getOriginalFqName()); // TODO original or class?
        InMemoryStorableClassLoader cl = entry.getClassLoaderForStorable();
        if (cl != null) {
          Map<String, ByteBuffer> classes = cl.getBytecode();
          for (Entry<String, ByteBuffer> clazzEntry : classes.entrySet()) {
            set.addInMemoryClassFile(clazzEntry.getKey(), new JavaMemoryObject(clazzEntry.getKey(), baseType, clazzEntry.getValue()));
          }
        }
        baseType = baseType.getSuperClassGenerationObject();
      }
    }
    

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
      if (!success) {
        return;
      }
      Graph<StorableNode> g = new Graph<>(graphData.values());
      List<Node<StorableNode>> graphRoots = g.getRoots();
      try {
        Map<Long, Set<GenerationBase>> rootsByRevision = new HashMap<>();
        for (Node<StorableNode> root : graphRoots) {
          Set<GenerationBase> partition = rootsByRevision.get(root.getContent().dom.getRevision());
          if (partition == null) {
            partition = new HashSet<>();
            rootsByRevision.put(root.getContent().dom.getRevision(), partition);
          }
          partition.add(root.getContent().dom);
        }

        Set<XMOMStorableStructureInformation> rootInfos = new HashSet<>();
        MultipleExceptionHandler<Throwable> multiex = new MultipleExceptionHandler<>();
        for (Set<GenerationBase> rootSet : rootsByRevision.values()) {
          set = new InMemoryCompilationSet(false, false, false, TargetKind.MEMORY);
          Set<GenerationBase> sortedRootSet = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
          sortedRootSet.addAll(rootSet);
          for (GenerationBase root : sortedRootSet) {
            if (root instanceof DOM) {
              XMOMStorableStructureInformation info = XMOMStorableStructureCache.getInstance(root.getRevision()).register((DOM) root, gbc);
              for (JavaSourceFromString source : info.getStorableSourceRecursivly()) {
                set.addToCompile(source);
              }
              rootInfos.add(info);
              enrichClasspath((DOM) root);
            }
          }

          if (this.set.size() > 0) {
            Map<Long, Map<String, Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>>>> storablesBySourceClassName = collectStorablesByStorableSourceClassName(rootInfos);
            Map<Long, Map<String, InMemoryStorableClassLoader>> cls = new HashMap<>();

            CompilationResult result;
            try {
              result = set.compile();

              for (JavaFileObject jfo : result.getFiles()) {
                JavaMemoryObject jmo = (JavaMemoryObject) jfo;

                String rootObjectName = jmo.getFqClassName();
                if (rootObjectName.contains("$")) {
                  rootObjectName = rootObjectName.substring(0, rootObjectName.indexOf('$'));
                }

                Map<String, InMemoryStorableClassLoader> subMap = cls.get(jmo.getRevision());
                if (subMap == null) {
                  subMap = new HashMap<>();
                  cls.put(jmo.getRevision(), subMap);
                }

                InMemoryStorableClassLoader pacl;
                if (subMap.containsKey(rootObjectName)) {
                  pacl = subMap.get(rootObjectName);
                } else {
                  pacl = new InMemoryStorableClassLoader(InMemoryStorableClassLoader.class.getClassLoader(), rootObjectName);
                  subMap.put(rootObjectName, pacl);
                }

                pacl.setBytecode(jmo.getFqClassName(), jmo.getClassBytes());

                if (storablesBySourceClassName.containsKey(jmo.getRevision()) &&
                    storablesBySourceClassName.get(jmo.getRevision()).containsKey(jmo.getFqClassName())) {
                  Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>> rootAndSubPair = storablesBySourceClassName.get(jmo.getRevision()).get(jmo.getFqClassName());
                  for (StorableStructureInformation ssi : rootAndSubPair.getSecond()) {
                    ssi.setClassLoaderForStorable(pacl);
                  }
                  pacl.setRootXMOMStorable(rootAndSubPair.getFirst());
                }
              }


              for (JavaFileObject jfo : result.getFiles()) {
                JavaMemoryObject jmo = (JavaMemoryObject) jfo;
                if (storablesBySourceClassName.containsKey(jmo.getRevision()) &&
                    storablesBySourceClassName.get(jmo.getRevision()).containsKey(jmo.getFqClassName())) {
                  Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>> rootAndSubPair = storablesBySourceClassName.get(jmo.getRevision()).get(jmo.getFqClassName());
                  InMemoryStorableClassLoader pacl = rootAndSubPair.getSecond().iterator().next().getClassLoaderForStorable();
                  if (pacl != null) { // TODO log?
                    try {
                      @SuppressWarnings("unchecked")
                      Class<? extends Storable<?>> clazz = (Class<? extends Storable<?>>) pacl.loadClass(pacl.getStorableClassName());
                      ODSImpl.getInstance().registerStorable(clazz);
                    } catch (Throwable t) {
                      multiex.addException(t);
                    }
                  }
                }
              }
            } catch (XPRC_CompileError e) {
              multiex.addException(e);
            }
          }
        }
        
        try {
          Collection<XMOMODSMapping> toDelete = new ArrayList<>();
          for (Node<StorableNode> root : graphRoots) {
            DOM dom = root.getContent().dom;
            // discover all paths in the current gb
            Set<String> fqPaths = XMOMODSMappingUtils.discoverPaths(domsThatCacheSubtypes, dom, gbc);
            // retrieve all mappings in the db by fqXmlName (and fqXML of all root-Subtypes?)
            Collection<XMOMODSMapping> mappings = XMOMODSMappingUtils.getAllMappingsForRootType(dom.getOriginalFqName(), dom.getRevision());
            // remove all discovered from list and delete the rest
            Iterator<XMOMODSMapping> mappingIter = mappings.iterator();
            while (mappingIter.hasNext()) {
              XMOMODSMapping mapping = mappingIter.next();
              if (fqPaths.contains(mapping.getFqpath())) {
                mappingIter.remove();
              }
            }
            toDelete.addAll(mappings);
          }
          
          ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
          try {
            con.delete(toDelete);
          } finally {
            con.closeConnection();
          }
        } catch (PersistenceLayerException e) {
          multiex.addException(e);
        }
        
        try {
          multiex.rethrow();
        } catch (MultipleExceptions e) {
          throw new RuntimeException(e);
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      } finally {
        gbc = null;
        graphData = null;
        for (DOM dom : domsThatCacheSubtypes) {
          dom.setCacheSubTypes(false); //eventuell werden die instanzen noch irgendwo weiterverwendet, wo sich die subtypen ändern können
        }
        domsThatCacheSubtypes = null;
      }
    }


    private Map<Long, Map<String, Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>>>> collectStorablesByStorableSourceClassName(Set<XMOMStorableStructureInformation> rootInfos) {
      Map<Long, Map<String, Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>>>> storableMap = new HashMap<>();
      for (XMOMStorableStructureInformation rootInfo : rootInfos) {
        rootInfo.traverse(new StorableStructureVisitor() {
          
          @Override
          public StorableStructureRecursionFilter getRecursionFilter() {
            return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
          }
          
          @Override
          public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
          }
          
          
          @Override
          public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
            if (current.getStorableSource() != null &&
                current.getStorableSource().getFqClassName() != null &&
                !current.getStorableSource().getFqClassName().isBlank()) {
              Map<String, Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>>> byRevision = storableMap.get(current.getDefiningRevision());
              if (byRevision == null) {
                byRevision = new HashMap<>();
                storableMap.put(current.getDefiningRevision(), byRevision);
              }
              Pair<XMOMStorableStructureInformation, Collection<StorableStructureInformation>> rootAndSubPairs = byRevision.get(current.getStorableSource().getFqClassName());
              if (rootAndSubPairs == null) {
                rootAndSubPairs = Pair.of(rootInfo, new ArrayList<>());
                byRevision.put(current.getStorableSource().getFqClassName(), rootAndSubPairs);
              }
              rootAndSubPairs.getSecond().add(current);
            }
          }
        });
      }
      return storableMap;
    }

    
    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
      set = new InMemoryCompilationSet(false, false, false, TargetKind.MEMORY);
      gbc = new GenerationBaseCache();
      graphData = new HashMap<>();
      domsThatCacheSubtypes = Collections.newSetFromMap(new IdentityHashMap<>()); //identityhashset statt normalem hashset, damit nicht irrtümlich objekte aus unterschiedlichen gb-caches vergessen gehen
    }
  }


  //FIXME beim redeployment können sich strukturen geändert haben. dann würde man eigtl alle alten konfigurationen entfernen wollen?!
  //      also sowas ähnliches wie beim undeployment.
  //      vielleicht in generationbase vor dem copy xml eine stelle einbauen, wo auf basis des alten xmls noch kram passiert??

  private static class StructureCacheUnregistrator implements UndeploymentHandler {
    
    Collection<GenerationBase> gbs = new ArrayList<>();
    
    public void exec(GenerationBase gb) throws XPRC_UnDeploymentHandlerException {
      if (gb instanceof DOM) {
        // TODO we can't check inheritance as xml has not been parsed, just try to remove and see if it succeeded
        XMOMStorableStructureInformation info =
            XMOMStorableStructureCache.getInstance(gb.getRevision()).getStructuralInformation(gb.getFqClassName());
        if (info != null) {
          gbs.add(gb);
          try {
            ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
            try {
              Collection<XMOMODSMapping> mappings = XMOMODSMappingUtils.getAllMappingsForRootType(gb.getOriginalFqName(), gb.getRevision());
              con.delete(mappings);
              con.commit();
            } finally {
              con.closeConnection();
            }
            
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("", e);
          }
        }
      }
    }

    public void exec(FilterInstanceStorable object) {
    }


    public void exec(TriggerInstanceStorable object) {
    }


    public void exec(Capacity object) {
    }


    public void exec(DestinationKey object) {
    }


    public void finish() throws XPRC_UnDeploymentHandlerException {
      Set<Long> relevantRevisions = new HashSet<>();
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      
      for (GenerationBase gb : gbs) {
        rcdm.getDependenciesRecursivly(gb.getRevision(), relevantRevisions);
        relevantRevisions.add(gb.getRevision());
      }
      
      Set<XMOMStorableStructureInformation> affectedRoots = new HashSet<>(); 
      for (Long relevantRevision : relevantRevisions) {
        XMOMStorableStructureCache xssc = XMOMStorableStructureCache.getInstance(relevantRevision);
        Collection<XMOMStorableStructureInformation> structures = xssc.getAllStorableStructureInformation();
        for (XMOMStorableStructureInformation structure : structures) {
          TypeCollectionVisitor typeCollector = new TypeCollectionVisitor();
          structure.traverse(typeCollector);
          Map<String, Collection<StorableStructureInformation>> collected = typeCollector.getTypes();
          for (GenerationBase gb : gbs) {
            if (collected.keySet().contains(gb.getFqClassName())) {
              affectedRoots.add(structure);
              break;
            }
          }
        }
      }
      
      Iterator<XMOMStorableStructureInformation> affectedRootIter = affectedRoots.iterator();
      outer: while (affectedRootIter.hasNext()) {
        XMOMStorableStructureInformation current = affectedRootIter.next();
        if (current.hasSuper()) {
          affectedRootIter.remove();
          continue;
        }
        for (GenerationBase gb : gbs) {
          if (current.getFqClassNameForDatatype().equals(gb.getFqClassName())) { // remove types that are currently removed
            affectedRootIter.remove();
            continue outer;
          }
        }
      }
      
      for (XMOMStorableStructureInformation affectedRoot : affectedRoots) {
        cleanSubReferences(affectedRoot);
      }
      
      for (GenerationBase gb : gbs) {
        XMOMStorableStructureCache.getInstance(gb.getRevision()).unregister((DOM) gb);
      }
    }
    
    private void cleanSubReferences(StorableStructureInformation current) {
      Set<StorableStructureIdentifier> subs = current.getSubEntries();
      if (subs != null) {
        Iterator<StorableStructureIdentifier> identifierIter = subs.iterator();
        while (identifierIter.hasNext()) {
          StorableStructureIdentifier identifier = identifierIter.next();
          if (isDying(identifier, gbs)) {
            identifierIter.remove();
          } else {
            cleanSubReferences(identifier.getInfo());
          }
        }
      }

      Collection<StorableColumnInformation> columns = current.getAllComplexColumns();
      for (StorableColumnInformation column : columns) {
        cleanSubReferences(column.getStorableVariableInformation());
      }
    }

    private boolean isDying(StorableStructureIdentifier identifier, Collection<GenerationBase> gbs) {
      if (identifier instanceof ReferenceStorableStructureIdentifier) {
        ReferenceStorableStructureIdentifier refId = (ReferenceStorableStructureIdentifier) identifier;
        StorableStructureInformation info = refId.getInfo();
        for (GenerationBase gb : gbs) {
          if (info.getFqClassNameForDatatype().equals(gb.getFqClassName()) &&
              info.getRevision().equals(gb.getRevision())) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean executeForReservedServerObjects(){
      return false;
    }


    public void exec(FilterStorable object) {
    }


    public void exec(TriggerStorable object) {
    }
  }


  public void removeRevision(long revision) throws PersistenceLayerException {
    XMOMODSMappingUtils.removeAllForRevision(revision);
  }


  
  private void updateTableNamesOfColumns(XMOMODSMapping xmomodsMapping, String oldTableName, ODSConnection con) throws PersistenceLayerException {
    Collection<XMOMODSMapping> sameTableMappings = XMOMODSMappingUtils.getAllColumnsForTable(con, xmomodsMapping, oldTableName);
    for (XMOMODSMapping sameTableMapping : sameTableMappings) {
      sameTableMapping.setTablename(xmomodsMapping.getTablename());
    }
    con.persistCollection(sameTableMappings);
    con.commit();
  }
  
  
  public void setODSName(ODSRegistrationParameter params)
                  throws XNWH_InvalidXMOMStorablePathException, XNWH_StorableNotFoundException, XNWH_ODSNameMustBeUniqueException,
                         PersistenceLayerException, XNWH_ODSNameChangedButNotDeployedException {
    setOdsName(params, params.isTableRegistration() ? NameType.TABLE : NameType.COLUMN);
  }


  private void setOdsName(ODSRegistrationParameter params, NameType type) throws XNWH_InvalidXMOMStorablePathException,
                  XNWH_StorableNotFoundException, XNWH_ODSNameMustBeUniqueException, PersistenceLayerException,
                  XNWH_ODSNameChangedButNotDeployedException {

    XMOMODSMapping xmomodsMapping;
    List<XMOMODSMapping> sameTypeMappings;
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      xmomodsMapping = XMOMODSMappingUtils.getByNameRevisionFqPath(params.getFqxmlname(), params.getRevision(), params.getFqpath());
      if (xmomodsMapping == null) {
        if (params.isBeforeDeployment()) {
          xmomodsMapping = new XMOMODSMapping(genId(), params);
        } else {
          RuntimeContext rc;
          try {
            rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(params.getRevision());
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          }
          throw new XNWH_StorableNotFoundException(params.getFqxmlname(), params.getFqpath(), rc.getGUIRepresentation());
        }
        
        PreparedQuery<XMOMODSMapping> pq = type.getIdentifierQuerySQL(con);
        Parameter queryParams = type.createOdsParams(params);
        sameTypeMappings = con.query(pq, queryParams, -1);
        try {
          if (sameTypeMappings.size() <= 0 || // ist der odsname NICHT in verwendung
              atLeastOnceUsedFromSameObjectInAnyRevision(sameTypeMappings, xmomodsMapping) || // Oder existiert sie schon, aber von dem selben objekt in  einer (anderen) revision?
              usedInSameHierachy(sameTypeMappings, xmomodsMapping, params)) { // Oder ist die andere Verwendung in der gleichen Vererbungshierarchie
            adjustAndStoreMapping(params, type, xmomodsMapping, con);
          } else { // odsname existiert in der DB und es gibt keine revision, in der das xmomobjekt bereits diesen odsname
                   // verwendet => d.h. ein anderes xmomobjekt verwendet den odsname.
            StringBuilder list = new StringBuilder();
            for (XMOMODSMapping sameTypeMapping : sameTypeMappings) {
              list.append("\nrev ").append(sameTypeMapping.getRevision()).append(" - ").append(sameTypeMapping.getFqxmlname());
              if (sameTypeMapping.getPath() != null) {
                list.append(".").append(sameTypeMapping.getPath());
              }
            }
            Throwable cause = null;
            switch (type) {
              case TABLE :
                cause = new RuntimeException("ODSName "
                                + xmomodsMapping.getTablename() + " can not be used for " + xmomodsMapping.getFqxmlname()
                                + ". It is already being used in:" + list);
                break;
              case COLUMN :
                
                cause = new RuntimeException("ODSName "
                                + xmomodsMapping.getColumnname() + " can not be used for " + xmomodsMapping.getFqxmlname()
                                + ". It is already being used for:" + list);
                break;
            }
            throw new XNWH_ODSNameMustBeUniqueException(params.getOdsName(), cause);
          }
        } catch (XPRC_XMOMObjectDoesNotExist e) {
          // logged in getFromCacheOrCreate
          return;
        } catch (XNWH_ODSMappingToNonStorableException e) {
          //log and continue -> new dataType is not a storable
          if (logger.isWarnEnabled()) {
            logger.warn("ODSMapping for '" + params.getFqxmlname() + "' could not be created, because it is not a storable.");
          }
        }
      } else {
        adjustAndStoreMapping(params, type, xmomodsMapping, con);
      }
    } finally {
      con.closeConnection();
    }
    if (!params.isBeforeDeployment()) {
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine()
                        .deployDatatype(xmomodsMapping.getFqxmlname(), WorkflowProtectionMode.BREAK_ON_USAGE, null,
                                        null, xmomodsMapping.getRevision()); // Datentyp neu deployen
      } catch (Exception e) {
        throw new XNWH_ODSNameChangedButNotDeployedException(xmomodsMapping.getFqxmlname(), e);
      }
    }
  }


  private void adjustAndStoreMapping(ODSRegistrationParameter params, NameType type, XMOMODSMapping xmomodsMapping, ODSConnection con)
                  throws PersistenceLayerException {
    switch (type) {
      case TABLE :
        String oldTableName = xmomodsMapping.getTablename();
        xmomodsMapping.setTablename(params.getOdsName());
        storeConfigAndSetPersistenceLayers(xmomodsMapping, con);// wieder speichern
        updateTableNamesOfColumns(xmomodsMapping, oldTableName, con);
        break;
      case COLUMN :
        xmomodsMapping.setColumnname(params.getOdsName());
        con.persistObject(xmomodsMapping);//wieder speichern
        con.commit();
        break;
    }
  }
  
  
  public void storeConfigAndSetPersistenceLayers(XMOMODSMapping config, ODSConnection con) throws PersistenceLayerException {
    con.persistObject(config);
    con.commit();
    if (config.isTableConfig()) {
      int plid = defaultPersistenceLayerId.get();
      if (plid != -1) {
        setPersistenceLayerIfNotSetToDefault(plid, config.getTablename(), ODSConnectionType.DEFAULT);
      }
      plid = defaultHistoryPersistenceLayerId.get();
      if (plid != -1) {
        setPersistenceLayerIfNotSetToDefault(plid, config.getTablename(), ODSConnectionType.HISTORY);
      }
      plid = defaultAlternativePersistenceLayerId.get();
      if (plid != -1) {
        setPersistenceLayerIfNotSetToDefault(plid, config.getTablename(), ODSConnectionType.ALTERNATIVE);
      }      
    }
  }

  //falls bereits eine explizite config besteht, soll diese beibehalten werden
  private void setPersistenceLayerIfNotSetToDefault(int plid, String odsName, ODSConnectionType type) throws PersistenceLayerException {
    long defaultId = ods.getDefaultPersistenceLayerInstance(type).getPersistenceLayerInstanceID();
    long oldId = ods.getPersistenceLayerInstanceId(type, odsName);
    if (oldId == defaultId && oldId != plid) {
      ods.setPersistenceLayerForTable(plid, odsName, null);
    }
  }

  private boolean atLeastOnceUsedFromSameObjectInAnyRevision(Collection<XMOMODSMapping> mappings, XMOMODSMapping newEntry) {
    for (XMOMODSMapping mapping : mappings) {
      if (mapping.describesSameObject(newEntry)) {
        return true;
      }
    }
    return false;
  }
  
  
  private boolean usedInSameHierachy(List<XMOMODSMapping> sameTypeMappings, XMOMODSMapping newEntry,
                                     ODSRegistrationParameter params) throws XPRC_XMOMObjectDoesNotExist, XNWH_ODSMappingToNonStorableException {
    DOM newDT = getFromCacheOrCreate(newEntry.getFqxmlname(), newEntry.getRevision(), params.getCache());
    for (XMOMODSMapping sameTypeMapping : sameTypeMappings) {
      DOM sameTypeDT = getFromCacheOrCreate(sameTypeMapping.getFqxmlname(), sameTypeMapping.getRevision(), params.getCache());
      
      try {
        if (sameTableHierarchy(newDT, sameTypeDT)) {
          return true;
        }        
      } catch(XNWH_ODSMappingToNonStorableException e) {
        if(e.getNewDomIsProblematic()) {
          throw e; //newDT is not a storable
        } else {
          //sameTypeDT is not a storable
          if(logger.isDebugEnabled()) {
            logger.warn("Found XMOMODSMapping for non-storable: '" + sameTypeDT.getOriginalFqName() + "' in revision '" + sameTypeDT.getRevision() + "'.");
          }
          continue; //ignore
        }
      }
      

    }
    return false;
  }
  
  
  private DOM getFromCacheOrCreate(String fqXmlName, Long revision, GenerationBaseCache gbc) throws XPRC_XMOMObjectDoesNotExist {
    DOM newDT = (DOM) gbc.getFromCache(fqXmlName, revision);
    if (newDT == null) {
      try {
        newDT = DOM.generateUncachedInstance(fqXmlName, true, revision);
      } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException e) {
        throw new RuntimeException(e);
      } catch (XPRC_MDMDeploymentException e) {
        if (e instanceof XPRC_XMOMObjectDoesNotExist) {
          logger.debug("Object " + fqXmlName + "@" + revision + " does not exist!");
          throw (XPRC_XMOMObjectDoesNotExist)e;
        }
        throw new RuntimeException(e);
      }
    }
    return newDT;
  }


  private boolean sameTableHierarchy(DOM newDT, DOM sameTypeDT) throws XNWH_ODSMappingToNonStorableException{
    DOM newRoot = findLastNonStorableEquivalentRoot(newDT);
    DOM sameRoot = findLastNonStorableEquivalentRoot(sameTypeDT);
    
    if (newRoot == null || sameRoot == null) {
      String invalidOrgName = newRoot == null ? newDT.getOriginalFqName() : sameTypeDT.getOriginalFqName();
      throw new XNWH_ODSMappingToNonStorableException(invalidOrgName, newRoot == null);
    }
    
    return newRoot.getOriginalFqName().equals(sameRoot.getOriginalFqName()) &&
           newRoot.getRevision().equals(sameRoot.getRevision());
  }
  
  
  private DOM findLastNonStorableEquivalentRoot(DOM dom) {
    DOM currentDom = dom;
    while (currentDom.hasSuperClassGenerationObject()) {
      DOM superDom = currentDom.getSuperClassGenerationObject();
      if (superDom.isStorableEquivalent()) {
        return currentDom;
      } else {
        currentDom = superDom;
      }
    }
    return null;
  }


  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                                          Long revision) throws PersistenceLayerException {
    return operationsImpl.query(correlatedOrder, selectionMask, formula, queryParameter, revision);
  }

  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                                          Long revision, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException {
    return operationsImpl.query(correlatedOrder, selectionMask, formula, queryParameter, revision, extendedParameter);
  }
  
  @Override
  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask,
                                          IFormula formula, QueryParameter queryParameter, Long revision,
                                          ExtendedParameter extendedParameter, XMOMStorableStructureInformation info)
                  throws PersistenceLayerException {
    return operationsImpl.query(correlatedOrder, selectionMask, formula, queryParameter, revision, extendedParameter, info);
  }
  
  public void store(XynaOrderServerExtension correlatedOrder, XynaObject storable, StoreParameter storeParameter)
                  throws PersistenceLayerException {
    operationsImpl.store(correlatedOrder, storable, storeParameter);
  }

  public void store(XynaOrderServerExtension correlatedOrder, XynaObject storable, StoreParameter storeParameter,
                    ExtendedParameter extendedParameter) throws PersistenceLayerException {
    operationsImpl.store(correlatedOrder, storable, storeParameter, extendedParameter);
  }
  
  public void delete(XynaOrderServerExtension correlatedOrder, XynaObject storable, DeleteParameter deleteParameter)
                  throws PersistenceLayerException {
    operationsImpl.delete(correlatedOrder, storable, deleteParameter);
  }

  public void delete(XynaOrderServerExtension correlatedOrder, XynaObject storable, DeleteParameter deleteParameter, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException {
    operationsImpl.delete(correlatedOrder, storable, deleteParameter, extendedParameter);
  }
  
  
  public void update(XynaOrderServerExtension correlatedOrder, XynaObject storable, List<String> updatePaths, UpdateParameter updateParameter, ExtendedParameter extendedParameter) 
                  throws PersistenceLayerException {
    operationsImpl.update(correlatedOrder, storable, updatePaths, updateParameter, extendedParameter);
  }


  public void update(XynaOrderServerExtension correlatedOrder, XynaObject storable, List<String> updatePaths, UpdateParameter updateParameter)
                  throws PersistenceLayerException {
    operationsImpl.update(correlatedOrder, storable, updatePaths, updateParameter);
  }


  public int count(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                   Long revision, ExtendedParameter extendedParameter)
                                   throws PersistenceLayerException {
    return operationsImpl.count(correlatedOrder, selectionMask, formula, queryParameter, revision, extendedParameter);
  }

  
}
