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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt;



import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.ExpiringMap;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.exceptions.XFMG_GeneratedInputSourceDataNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_InputSourceNotUniqueException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.classloading.OrderInputSourceTypeClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceSpecificStorable;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceTypeStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public class OrderInputSourceManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = OrderInputSourceManagement.class.getSimpleName();
  private static final String FORM_PATH = "xmcp.factorymanager.orderinputsourcetype";
  private static final String DEPLOYMENT_ITEM_PATH = FORM_PATH;


  private static class OrderInputSourceSeriesInfo {

    //map von einer eindeutigen id f�r den wf-schritt auf die id der referenzierten inputsource
    private final Map<String, Long> childOrderInputSources = new HashMap<String, Long>();

    //alle von der serie direkt aufgerufenen inputsource-Ids
    private final Set<Long> allOrderInputSources = new HashSet<Long>();
    
    private volatile int cachedReferenceCountRecursively;
    private volatile int cacheValidationId_Is;

    public int cntAllOrderInputSources;
    private static volatile int cacheValidationId_Expect;

    public OrderInputSourceSeriesInfo() {
    }


    public void addChild(String idOfInputSourceRefInWF, Long inputSourceId) {
      childOrderInputSources.put(idOfInputSourceRefInWF, inputSourceId);
    }

    public void addInputSource(Long inputSourceId) {
      allOrderInputSources.add(inputSourceId);
    }

  }

  public static class OptionalOISGenerateMetaInformation implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> keyToValues = new HashMap<String, String>();
    private transient Role creationRole;
    public OptionalOISGenerateMetaInformation() {
    }
    public OptionalOISGenerateMetaInformation(List<Pair<String, String>> keyValues) {
      if (keyValues != null) {
        for (Pair<String, String> p: keyValues) {
          keyToValues.put(p.getFirst(), p.getSecond());
        }
      }
    }
    public void setValue(String key, String value) {
      keyToValues.put(key, value);
    }
    /**
     * @return The value, if present. Null otherwise.
     */
    public String getValue(String key) {
      return keyToValues.get(key);
    }
    public void setTransientCreationRole(Role role) {
      this.creationRole = role;
    }
    public Role getTransientCreationRole() {
      return creationRole;
    }
  }

  public static class OrderInputCreationInstanceWithSeriesInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    
    transient OrderInputCreationInstance oici;
    private final long generationContextId;
    //alle von der serie direkt aufgerufenen creationinstances
    final Map<String, OrderInputCreationInstanceAndParameter> childOrderInputCreationInstances =
        new HashMap<String, OrderInputCreationInstanceAndParameter>();


    public OrderInputCreationInstanceWithSeriesInfo(OrderInputCreationInstance oici, long generationContextId) {
      this.oici = oici;
      this.generationContextId = generationContextId;
    }

    void addChild(String idOfInputSourceRefInWF, OrderInputCreationInstanceAndParameter child) {
      childOrderInputCreationInstances.put(idOfInputSourceRefInWF, child);
    }

    public long getGenerationContextId() {
      return generationContextId;
    }


    public XynaOrderCreationParameter getOrCreate(String idOfInputSourceInWF, String inputSourceName, long revision) throws XynaException {
      OrderInputCreationInstanceAndParameter oic = childOrderInputCreationInstances.get(idOfInputSourceInWF);
      if (oic == null) {
        //input muss dann nun generiert werden
        OrderInputSourceManagement orderInputSourceManagement =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        long inputSourceId = orderInputSourceManagement.storage.getInputSourceIdByName(inputSourceName, revision);
        XynaOrderCreationParameter xocp = orderInputSourceManagement.generateOrderInput(inputSourceId);
        orderInputSourceManagement.notifyInputSource(xocp.getOrderInputSourceId());
        oic = new OrderInputCreationInstanceAndParameter(null, xocp, generationContextId);
        childOrderInputCreationInstances.put(idOfInputSourceInWF, oic);
      }
      return oic.xocp;
    }


    public OrderInputCreationInstanceWithSeriesInfo remove(String idOfInputSourceInWF) {
      return childOrderInputCreationInstances.remove(idOfInputSourceInWF);
    }

    public void clear() {
      childOrderInputCreationInstances.clear();
    }
  }

  private static class OrderInputCreationInstanceAndParameter extends OrderInputCreationInstanceWithSeriesInfo {

    private static final long serialVersionUID = 1L;
    
    private final XynaOrderCreationParameter xocp;

    public OrderInputCreationInstanceAndParameter(OrderInputCreationInstance oici, XynaOrderCreationParameter xocp, long generationContextId) {
      super(oici, generationContextId);
      this.xocp = xocp;
    }
  }


  //DestinationValue -> welche inputSourceSeriesObjekte sind referenziert
  private final ConcurrentMap<Pair<DestinationValue, Long>, OrderInputSourceSeriesInfo> orderInputSourceSeriesInfo;
  
  //inputsourcetype-name -> inputsourcetype
  private ConcurrentMap<String, OrderInputSourceType> orderInputSourceTypes;
  private OrderInputSourceStorage storage;
  //storableId -> (ordertype, inputgen)
  private final Map<Long, Pair<String, OrderInputSource>> inputSources = new ConcurrentHashMap<Long, Pair<String, OrderInputSource>>();
  //instanceid -> instance 
  private ExpiringMap<Long, OrderInputCreationInstanceWithSeriesInfo> inputSourceInstances;
  private IDGenerator idgen;


  public OrderInputSourceManagement() throws XynaException {
    super();
    orderInputSourceTypes = new ConcurrentHashMap<String, OrderInputSourceType>();
    orderInputSourceSeriesInfo = new ConcurrentHashMap<Pair<DestinationValue, Long>, OrderInputSourceSeriesInfo>();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    //FIXME
    fExec.addTask(OrderInputSourceManagement.class, "OrderInputSourceManagement.initOrderInputSources").
      after(PersistenceLayerInstances.class, DeploymentItemStateManagementImpl.class).
      after(WorkflowDatabase.FUTURE_EXECUTION_ID).
      execAsync(new Runnable() { public void run() { initOrderInputSourceTypes(); }});

    fExec.addTask(OrderInputSourceManagement.class.getSimpleName() + "_DeploymentHandler", OrderInputSourceManagement.class.getSimpleName() + "_DeploymentHandler").
      after(DeploymentHandling.class).
      execAsync(new Runnable() { public void run() { initDeploymentHandler(); }});
    
  }


  private void initDeploymentHandler() {
        try {
          storage = new OrderInputSourceStorage();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("Could not initialize " + DEFAULT_NAME, e);
        }
        
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
            .addDeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new DeploymentHandler() {

              public void finish(boolean success) throws XPRC_DeploymentHandlerException {
                OrderInputSourceSeriesInfo.cacheValidationId_Expect++;
              }

              public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
                if (object instanceof WF) {
                  WF wf = (WF) object;
                  Map<String, String> inputSourceRefs = wf.getPreparableReferencedOrderInputSources();
                  Set<String> allInputSourceRefs = wf.getAllReferencedOrderInputSources();
                  if (inputSourceRefs.size() > 0 || allInputSourceRefs.size() > 0) {
                    //speichern der referenzierten inputsources zu diesem workflow.
                    DestinationValue key = new FractalWorkflowDestination(object.getFqClassName());
                    OrderInputSourceSeriesInfo value = new OrderInputSourceSeriesInfo();
                    orderInputSourceSeriesInfo.put(Pair.of(key, object.getRevision()), value);
                    value.cntAllOrderInputSources = wf.getCountOfAllReferencedOrderInputSources();

                    for (Entry<String, String> entry : inputSourceRefs.entrySet()) {
                      long inputSourceId;
                      try {
                        inputSourceId = storage.getInputSourceIdByName(entry.getValue(), object.getRevision());
                      } catch (PersistenceLayerException e) {
                        throw new XPRC_DeploymentHandlerException(wf.getOriginalFqName(), OrderInputSourceManagement.class.getSimpleName());
                      }
                      value.addChild(entry.getKey(), inputSourceId);
                    }
                    for (String inputSource : allInputSourceRefs) {
                      long inputSourceId;
                      try {
                        inputSourceId = storage.getInputSourceIdByName(inputSource, object.getRevision());
                      } catch (PersistenceLayerException e) {
                        throw new XPRC_DeploymentHandlerException(wf.getOriginalFqName(), OrderInputSourceManagement.class.getSimpleName());
                      }
                      value.addInputSource(inputSourceId);
                    }
                  }
                }
              }

              @Override
              public void begin() throws XPRC_DeploymentHandlerException {
              }
            });
        
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new UndeploymentHandler() {

          public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
            if (object instanceof WF) {
              DestinationValue key = new FractalWorkflowDestination(object.getFqClassName());
              orderInputSourceSeriesInfo.remove(Pair.of(key, object.getRevision()));
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
          }

          public boolean executeForReservedServerObjects() {
            return false;
          }

          public void exec(FilterStorable object) {
          }

          public void exec(TriggerStorable object) {
          }
          
        });
  }


  private PreparedQuery<OrderInputSourceSpecificStorable> querySpecifics;


  private void initOrderInputSourceTypes() {
    XynaPropertyDuration timeoutOfCreatedInput =
        new XynaPropertyDuration("xfmg.xods.orderinputsourcemgmt.timeoutofcreatedinput",
                                 com.gip.xyna.utils.timing.Duration.valueOf("30", TimeUnit.SECONDS));
    inputSourceInstances =
        new ExpiringMap<Long, OrderInputCreationInstanceWithSeriesInfo>(timeoutOfCreatedInput.getMillis(), TimeUnit.MILLISECONDS);
    try {
      for (OrderInputSourceTypeStorable oigts : storage.getAllOrderInputSourceTypes()) {
        try {
          loadOrderInputSourceType(oigts.getName(), oigts.getFqclassname());
        } catch (XynaException e) {
          logger.warn("OrderInputSourceType " + oigts.getName() + " could not be initialized", e);
        }
      }
      idgen = XynaFactory.getInstance().getIDGenerator();
      ODS ods = ODSImpl.getInstance();
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      querySpecifics =
          con.prepareQuery(new Query<OrderInputSourceSpecificStorable>("select * from " + OrderInputSourceSpecificStorable.TABLENAME
              + " where " + OrderInputSourceSpecificStorable.COL_SOURCEID + " = ?", OrderInputSourceSpecificStorable.reader));
      try {
        Collection<OrderInputSourceStorable> inputGens = con.loadCollection(OrderInputSourceStorable.class);
        for (OrderInputSourceStorable oigs : inputGens) {
          List<OrderInputSourceSpecificStorable> specifics = con.query(querySpecifics, new Parameter(oigs.getId()), -1);
          try {
            createOrderInputSourceFromType(oigs, specifics, false);
          } catch (RuntimeException e) {
            //TODO bei fehlern auf invalid setzen?!
            logger.warn(null, e);
          } catch (XynaException e) {
            //TODO bei fehlern auf invalid setzen?!
            logger.warn(null, e);
          }
        }
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not initialize OrderInputSourceManagement", e);
      throw new RuntimeException(e);
    }
  }


  private void createOrderInputSourceFromType(OrderInputSourceStorable oigs, List<OrderInputSourceSpecificStorable> specifics, boolean modify) throws XynaException {
    OrderInputSourceType oigt = orderInputSourceTypes.get(oigs.getType());
    if (oigt == null) {
      throw new RuntimeException("Could not find " + OrderInputSourceType.class.getSimpleName() + " " + oigs.getType());
    }
    OrderInputSource inputGen;
    try {
      inputGen =
          oigt.createOrderInputSource(oigs.getName(), getDestinationKey(oigs), toMap(oigs.getType(), specifics, oigt),
                                      oigs.getDocumentation());
    } catch (StringParameterParsingException e) {
      throw new RuntimeException("Could not parse " + OrderInputSourceType.class.getSimpleName() + " Parameters.", e);
    }
    Pair<String, OrderInputSource> pair = Pair.of(oigs.getDestinationKey().getOrderType(), inputGen);
    inputSources.put(oigs.getId(), pair);

    try {
      registerInDeploymentItemStateManagement(oigs, inputGen, modify);
    } catch (XynaException e) {
      logger.debug("Exception during handling of deployment item state of order input source "
                       + oigs.getName()
                       + " in runtimeContext "
                       + XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                           .getRuntimeContext(oigs.getRevision()), e);
    }
  }


  private Map<String, Object> toMap(String typeName, List<OrderInputSourceSpecificStorable> specifics, OrderInputSourceType type)
      throws StringParameterParsingException {
    Map<String, Object> ret = new HashMap<String, Object>();
    for (OrderInputSourceSpecificStorable specific : specifics) {
      boolean found = false;
      for (StringParameter<?> p : type.showDescription().getParameters()) {
        if (p.getName().equals(specific.getKey())) {
          ret.put(p.getName(), p.parse(specific.getValue()));
          found = true;
          break; //n�chstes specific
        }
      }
      if (!found) {
        throw new RuntimeException("Invalid input parameter for orderinputsource of type " + typeName + " : " + specific.getKey());
      }
    }
    return ret;
  }


  private DestinationKey getDestinationKey(OrderInputSourceStorable oigs) {
    if (oigs.getWorkspaceName() != null) {
      return new DestinationKey(oigs.getOrderType(), new Workspace(oigs.getWorkspaceName()));
    }
    return new DestinationKey(oigs.getOrderType(), new Application(oigs.getApplicationName(), oigs.getVersionName()));
  }


  @Override
  protected void shutdown() throws XynaException {
  }


  /**
   * Registriert einen neuen {@link OrderInputSourceType}
   * @param name
   * @param fqClassName
   * @return
   * @throws XynaException
   */
  public OrderInputSourceType registerOrderInputSourceType(String name, String fqClassName) throws XynaException {
    OrderInputSourceType oigt = loadOrderInputSourceType(name, fqClassName);

    //OrderInputGeneratorTypeStorable anlegen und persistieren
    storage.persistOrderInputSourceType(name, fqClassName);
    
    //alle existierenden orderinputsource instanzen neu erzeugen mit neuer klasse
    SearchRequestBean srb = new SearchRequestBean(ArchiveIdentifier.orderInputSource, -1);
    srb.addFilterEntry(OrderInputSourceStorable.COL_TYPE, SelectionParser.escape(name));
    SearchResult<OrderInputSourceStorable> result = storage.search(srb);
    for (OrderInputSourceStorable oiss : result.getResult()) {
      List<OrderInputSourceSpecificStorable> specifics = new ArrayList<OrderInputSourceSpecificStorable>();
      Map<String, String> paras = oiss.getParameters();
      for (Entry<String, String> para : paras.entrySet()) {
        //ids nicht ben�tigt
        specifics.add(new OrderInputSourceSpecificStorable(-1, -1, para.getKey(), para.getValue()));
      }
      createOrderInputSourceFromType(oiss, specifics, false);
    }
    return oigt;
  }


  /**
   * Deregistriert einen {@link OrderInputSourceType}
   */
  public void deregisterOrderInputSourceType(String name) throws PersistenceLayerException {
    int cnt = storage.countInputSourcesOfType(name);
    if (cnt > 0) {
      throw new RuntimeException("inputsource type <" + name + "> is still in use.");
    }
    
    orderInputSourceTypes.remove(name);

    //OrderInputGeneratorTypeStorable l�schen
    storage.deleteOrderInputSourceType(name);
  }


  public List<PluginDescription> listOrderInputSourceTypes() {
    List<PluginDescription> pds = new ArrayList<PluginDescription>();
    for (Entry<String, OrderInputSourceType> oigt : orderInputSourceTypes.entrySet()) {
      try {
        //forms m�ssen simplename / label um�ndern auf den typ-namen, den beim registrieren angegeben wurde
        pds.add(transformForms(oigt.getKey(), oigt.getValue().showDescription()));
      } catch (XPRC_XmlParsingException e) {
        logger.info("Invalid form xmls in InputSourceType " + oigt.getKey() + ".", e);
      }
    }
    Collections.sort(pds);
    return pds;
  }


  private static PluginDescription transformForms(String typeName, PluginDescription descr) throws XPRC_XmlParsingException {
    if (descr.getForms() != null) {
      descr = new PluginDescription(descr);
      descr.getForms()[0] = transformForm(typeName, descr.getForms()[0]);
      descr.getDatatypes()[0] = transformDatatype(typeName, descr.getDatatypes()[0]);
    }
    return descr;
  }


  private static String transformDatatype(String typeName, String datatypeXML) throws XPRC_XmlParsingException {
    Document doc = XMLUtils.parseString(datatypeXML);
    String fqDatatypeName = convertTypeNameToFQDatatypeName(typeName);
    doc.getDocumentElement().setAttribute(GenerationBase.ATT.TYPENAME, GenerationBase.getSimpleNameFromFQName(fqDatatypeName));
    doc.getDocumentElement().setAttribute(GenerationBase.ATT.TYPEPATH, FORM_PATH);
    StringWriter sw = new StringWriter();
    XMLUtils.saveDomToWriter(sw, doc);
    return sw.toString();
  }


  private static String transformForm(String typeName, String formXML) throws XPRC_XmlParsingException {
    Document doc = XMLUtils.parseString(formXML);
    String fqDatatypeName = convertTypeNameToFQDatatypeName(typeName);
    String datatypeSimpleName = GenerationBase.getSimpleNameFromFQName(fqDatatypeName);
    String fqFormName = convertTypeNameToFQFormName(typeName);
    doc.getDocumentElement().setAttribute(GenerationBase.ATT.TYPENAME, GenerationBase.getSimpleNameFromFQName(fqFormName)); //label so lassen?
    doc.getDocumentElement().setAttribute(GenerationBase.ATT.TYPEPATH, FORM_PATH);
    Element inputEl = XMLUtils.getChildElementByName(doc.getDocumentElement(), GenerationBase.EL.INPUT);
    if (inputEl == null) {
      throw new RuntimeException("Invalid form xml for type " + typeName + ". Missing Input element.");
    }
    Element dataEl = XMLUtils.getChildElementByName(inputEl, GenerationBase.EL.DATA);
    if (dataEl == null) {
      throw new RuntimeException("Invalid form xml for type " + typeName + ". Missing Data element.");
    }
    dataEl.setAttribute(GenerationBase.ATT.REFERENCENAME, datatypeSimpleName);
    dataEl.setAttribute(GenerationBase.ATT.REFERENCEPATH, FORM_PATH);
    dataEl
        .setAttribute(GenerationBase.ATT.VARIABLENAME, datatypeSimpleName.substring(0, 1).toLowerCase() + datatypeSimpleName.substring(1));
    StringWriter sw = new StringWriter();
    XMLUtils.saveDomToWriter(sw, doc);
    return sw.toString();
  }


  /**
   * L�dt die Klasse "fqClassName" f�r einen {@link OrderInputSourceType} vom Typ "type"
   */
  private OrderInputSourceType loadOrderInputSourceType(String name, String fqClassName) throws XynaException {
    OrderInputSourceTypeClassLoader oigtcl = new OrderInputSourceTypeClassLoader(name);
    Class<?> clazz = null;
    try {
      clazz = oigtcl.loadClass(fqClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    if (!OrderInputSourceType.class.isAssignableFrom(clazz)) {
      throw new RuntimeException(OrderInputSourceType.class.getSimpleName() + " class must extend " + OrderInputSourceType.class.getName());
    }

    OrderInputSourceType oigt = null;
    try {
      oigt = (OrderInputSourceType) clazz.getConstructor().newInstance();
    } catch (Exception e) { //InstantiationException, IllegalAccessException
      throw new RuntimeException(OrderInputSourceType.class.getSimpleName() + " could not be instantiated", e);
    }

    orderInputSourceTypes.put(name, oigt);
    return oigt;
  }


  public SearchResult<?> searchInputSources(SearchRequestBean searchRequest) throws PersistenceLayerException, XNWH_SelectParserException,
      XNWH_InvalidSelectStatementException {
    return storage.search(searchRequest);
  }


  public long createOrderInputSource(OrderInputSourceStorable inputSource) throws XynaException {
    inputSource.cleanupGUIRelics();

    //checken, dass name eindeutig ist
    OrderInputSourceStorable oiss = storage.getInputSourceByName(inputSource.getName(), inputSource.getApplicationName(), inputSource.getVersionName(), inputSource.getWorkspaceName(), false);
    if (oiss != null) {
      throw new XFMG_InputSourceNotUniqueException(inputSource.getName());
    }

    long id = idgen.getUniqueId();
    inputSource.setId(id);

    Map<String, String> paras = inputSource.getParameters();
    List<OrderInputSourceSpecificStorable> specifics = new ArrayList<OrderInputSourceSpecificStorable>();
    for (Entry<String, String> para : paras.entrySet()) {
      specifics.add(new OrderInputSourceSpecificStorable(idgen.getUniqueId(), id, para.getKey(), para.getValue()));
    }

    createOrderInputSourceFromType(inputSource, specifics, false);

    store(inputSource, specifics, id);

    return id;
  }


  private void store(OrderInputSourceStorable inputSource, List<OrderInputSourceSpecificStorable> specifics, long id)
      throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    boolean success = false;
    try {
      con.persistObject(inputSource);
      con.persistCollection(specifics);
      con.commit();
      success = true;
    } finally {
      con.closeConnection();
      if (!success) {
        inputSources.remove(id);
      }
    }
  }


  public void modifyOrderInputSource(OrderInputSourceStorable inputSource) throws XynaException {
    inputSource.cleanupGUIRelics();

    //id typischerweise nicht gesetzt, also erst ermitteln
    if (inputSource.getId() <= 0) {
      storage.findAndFillId(inputSource);
    }

    List<OrderInputSourceSpecificStorable> existingSpecifics;
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      existingSpecifics = storage.getOrderInputSpecifics(con, inputSource.getId());
    } finally {
      con.closeConnection();
    }

    Map<String, String> paras = inputSource.getParameters();
    List<OrderInputSourceSpecificStorable> specifics = new ArrayList<OrderInputSourceSpecificStorable>();
    for (Entry<String, String> para : paras.entrySet()) {
      //checken, ob specific bereits existiert
      boolean found = false;
      for (OrderInputSourceSpecificStorable specific : existingSpecifics) {
        if (specific.getKey().equals(para.getKey())) {
          specifics.add(new OrderInputSourceSpecificStorable(specific.getId(), inputSource.getId(), para.getKey(), para.getValue()));
          existingSpecifics.remove(specific);
          found = true;
          break;
        }
      }
      if (!found) {
        //nicht gefunden, neu erstellen
        specifics.add(new OrderInputSourceSpecificStorable(idgen.getUniqueId(), inputSource.getId(), para.getKey(), para.getValue()));
      }
    }

    createOrderInputSourceFromType(inputSource, specifics, true);

    //TODO eine transaktion statt mehrerer!

    //alle �berbleibenden alten specifics l�schen
    con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.delete(existingSpecifics);
      con.commit();
    } finally {
      con.closeConnection();
    }

    store(inputSource, specifics, inputSource.getId());
  }


  public OrderInputSourceStorable getInputSourceByName(long revision, String inputSourceName) throws PersistenceLayerException {
    return getInputSourceByName(revision, inputSourceName, false);
  }

  public OrderInputSourceStorable getInputSourceByName(long revision, String inputSourceName, boolean withParameters) throws PersistenceLayerException {
    return storage.getInputSourceByName(inputSourceName, revision, withParameters);
  }


  public void deleteOrderInputSource(long inputSourceId) throws XynaException {
    //TODO �berpr�fen, ob noch in verwendung?
    try {
      OrderInputSourceStorable ois = storage.getOrderInputSourceById(inputSourceId);
      unregisterFromDeploymentItemStateManagement(ois.getName(), ois.getRevision());
      
      //OrderInputSource aus den Application-Definitionen entfernen
      ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
      appMgmt.removeObjectFromAllApplications(ois.getName(), ApplicationEntryType.ORDERINPUTSOURCE, ois.getRevision());
    } finally {
      storage.deleteOrderInputSource(inputSourceId);
      inputSources.remove(inputSourceId);
    }
  }


  /**
   * Alle OrderInputSources einer Revision l�schen
   */
  public void deleteOrderInputSourcesForRevision(long revision) throws PersistenceLayerException {
    List<OrderInputSourceStorable> deletedInputSources = storage.deleteOrderInputSourcesForRevision(revision);
    for (OrderInputSourceStorable deleted : deletedInputSources) {
      unregisterFromDeploymentItemStateManagement(deleted.getName(), deleted.getRevision());
      inputSources.remove(deleted.getId());
    }
  }

  /**
   * Alle OrderInputSources einer Revision suchen
   * @return 
   */
  public List<OrderInputSourceStorable> getOrderInputSourcesForRevision(long revision) throws PersistenceLayerException {
    return storage.getOrderInputSourcesForRevision(revision);
  }


  public XynaOrderCreationParameter generateOrderInput(long inputSourceId) throws XynaException {
    return generateOrderInput(inputSourceId, new OptionalOISGenerateMetaInformation());
  }


  public XynaOrderCreationParameter generateOrderInput(long inputSourceId,
                                                       OptionalOISGenerateMetaInformation parameters)
      throws XynaException {
    Pair<String, OrderInputSource> pair = inputSources.get(inputSourceId);
    if (pair == null) {
      throw new RuntimeException("unknown input source: " + inputSourceId);
    }

    OrderInputSource orderInputSource = pair.getSecond();
    OrderInputCreationInstance instance = orderInputSource.createInstance();
    long generationContextId = idgen.getUniqueId();
    XynaOrderCreationParameter xocp = instance.generate(generationContextId, parameters);

    OrderInputCreationInstanceWithSeriesInfo instanceWithSeriesInfo =
        new OrderInputCreationInstanceWithSeriesInfo(instance, generationContextId);
    //checken, ob workflow andere inputsources referenziert
    addChildren(instanceWithSeriesInfo, generationContextId, xocp.getDestinationKey(), new HashSet<DestinationValue>(),
                parameters);

    inputSourceInstances.put(generationContextId, instanceWithSeriesInfo);
    xocp.setOrderInputSourceId(generationContextId);
    return xocp;
  }


  private void addChildren(OrderInputCreationInstanceWithSeriesInfo instanceWithSeriesInfo, long generationContextId,
                           DestinationKey destinationKey, Set<DestinationValue> visited,
                           OptionalOISGenerateMetaInformation parameters) throws XynaException {
    DestinationValue destination =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionDestination(destinationKey);
    if (!visited.add(destination)) {
      //endlosrekursion vermeiden
      return;
    }
    if (destination instanceof FractalWorkflowDestination) {
      Set<Long> allRevisions = destination.resolveAllRevisions(destinationKey);
      for (Long revision : allRevisions) {
        OrderInputSourceSeriesInfo seriesInfo = orderInputSourceSeriesInfo.get(Pair.of(destination, revision));
        if (seriesInfo != null) {
          //workflow referenziert inputsources
          Map<String, Long> childOrderInputSources = seriesInfo.childOrderInputSources;
          for (Entry<String, Long> entry : childOrderInputSources.entrySet()) {
            Pair<String, OrderInputSource> childOrderInputSource = inputSources.get(entry.getValue());
            if (childOrderInputSource == null) {
              throw new RuntimeException("OrderInputSource referenced by OrderType " + destinationKey.getOrderType() + " does not exist.");
            }
            OrderInputCreationInstance instance = childOrderInputSource.getSecond().createInstance();
            XynaOrderCreationParameter xocp = instance.generate(generationContextId, parameters);
            OrderInputCreationInstanceAndParameter child = new OrderInputCreationInstanceAndParameter(instance, xocp, generationContextId);
            instanceWithSeriesInfo.addChild(entry.getKey(), child);
            addChildren(child, generationContextId, xocp.getDestinationKey(), visited, parameters);
          }
        }
        //TODO hier k�nnte man auch noch versuchen, die workflows zu untersuchen, die keine inputsources DIREKT referenzieren
        //weil die k�nnten ja wiederum subwfs enthalten, die inputsources referenzieren.
      }
    }
  }


  public void prepareOrderInputs(XynaOrderServerExtension xo, OptionalOISGenerateMetaInformation parameters)
      throws XynaException {
    DestinationValue destination;
    try {
      destination =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
              .getExecutionDestination(xo.getDestinationKey());
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      //ignorieren. gibt sp�ter eh noch einen fehler...
      return;
    }
    OrderInputSourceSeriesInfo seriesInfo = orderInputSourceSeriesInfo.get(Pair.of(destination, destination.resolveRevision(xo.getDestinationKey())));
    if (seriesInfo != null) {
      //workflow referenziert inputsources

      Long generationContextId = xo.getOrderInputGenerationContextId();
      if (generationContextId == null) {
        generationContextId = idgen.getUniqueId();
      }
      OrderInputCreationInstanceWithSeriesInfo creationInstanceWithSeriesInfo =
          new OrderInputCreationInstanceWithSeriesInfo(null, generationContextId);
      addChildren(creationInstanceWithSeriesInfo, generationContextId, xo.getDestinationKey(),
                  new HashSet<DestinationValue>(), parameters);

      notifyChildren(creationInstanceWithSeriesInfo);

      xo.setOrderInputCreationInstances(creationInstanceWithSeriesInfo);
    }
  }


  private OrderInputCreationInstanceWithSeriesInfo notifyInputSource(long inputGenId) throws XynaException {
    OrderInputCreationInstanceWithSeriesInfo seriesInfo = inputSourceInstances.remove(inputGenId);
    if (seriesInfo == null) {
      throw new XFMG_GeneratedInputSourceDataNotFoundException();
    }
    OrderInputCreationInstance inputGen = seriesInfo.oici;
    inputGen.notifyOnOrderStart();

    notifyChildren(seriesInfo);
    return seriesInfo;
  }

  public void notifyInputSource(long inputGenId, XynaOrderServerExtension xo) throws XynaException {
    OrderInputCreationInstanceWithSeriesInfo seriesInfo = notifyInputSource(inputGenId);

    xo.setOrderInputCreationInstances(seriesInfo);
  }


  private void notifyChildren(OrderInputCreationInstanceWithSeriesInfo seriesInfo) throws XynaException {
    seriesInfo.oici = null;
    Map<String, OrderInputCreationInstanceAndParameter> childOrderInputCreationInstances = seriesInfo.childOrderInputCreationInstances;
    for (OrderInputCreationInstanceAndParameter p : childOrderInputCreationInstances.values()) {
      p.oici.notifyOnOrderStart();
      notifyChildren(p);
    }
  }


  //changemanagement/status behandlung

  public String[] refactor(DependencySourceType refactoredObjectType, String fqClassNameOld, String fqClassNameNew, Long revision)
      throws XynaException {
    /*
     * jede input source �berpr�fen, ob sie betroffen ist. delegation an inputsourcetype f�r interne refactoringarbeit
     */
    if (refactoredObjectType == DependencySourceType.ORDERTYPE) {
      DestinationValue key = new FractalWorkflowDestination(fqClassNameOld);
      Set<Long> allRevisions = key.resolveAllRevisions(revision);
      for (Long aRevision : allRevisions) {
        orderInputSourceSeriesInfo.remove(Pair.of(key, aRevision));
      }
    }
    
    SearchRequestBean srb = new SearchRequestBean(ArchiveIdentifier.orderInputSource, -1);
    Map<String, String> filter = new HashMap<String, String>();
    storage.setFilterForRevision(filter, revision);
    srb.setFilterEntries(filter);
    SearchResult<OrderInputSourceStorable> storables = storage.search(srb);

    List<OrderInputSourceStorable> changedInputSources = new ArrayList<OrderInputSourceStorable>();
    for (OrderInputSourceStorable ois : storables.getResult()) {
      boolean changed = false;

      //einzige generische refactoringmassnahme
      if (refactoredObjectType == DependencySourceType.ORDERTYPE) {
        if (ois.getOrderType().equals(fqClassNameOld)) {
          ois.setOrderType(fqClassNameNew);
          changed = true;
        }
      }

      OrderInputSourceType orderInputSourceType = orderInputSourceTypes.get(ois.getType());

      //parameter transformation
      Map<String, Object> paras = new HashMap<String, Object>();
      for (Entry<String, String> para : ois.getParameters().entrySet()) {
        boolean found = false;
        for (StringParameter<?> p : orderInputSourceType.showDescription().getParameters()) {
          if (p.getName().equals(para.getKey())) {
            try {
              paras.put(p.getName(), p.parse(para.getValue()));
            } catch (StringParameterParsingException e) {
              throw new RuntimeException("Invalid input parameter for orderinputsource of type " + ois.getType() + " : " + para.getKey());
            }
            found = true;
            break; //n�chstes specific
          }
        }
        if (!found) {
          throw new RuntimeException("Invalid input parameter for orderinputsource of type " + ois.getType() + " : " + para.getKey());
        }
      }

      //eigentliche delegation an inputsourcetype
      if (orderInputSourceType.refactorParameters(paras, refactoredObjectType, fqClassNameOld, fqClassNameNew)) {
        //neue parameter map �bernehmen. daf�r muss sie erst wieder transformiert werden
        changed = true;
        List<OrderInputSourceSpecificStorable> specifics = new ArrayList<OrderInputSourceSpecificStorable>();
        for (Entry<String, Object> para : paras.entrySet()) {
          String key = para.getKey();
          boolean found = false;
          for (StringParameter p : orderInputSourceType.showDescription().getParameters()) {
            if (p.getName().equals(para.getKey())) {
              specifics.add(new OrderInputSourceSpecificStorable(-1L, -1L, key, p.asString(para.getValue())));
              found = true;
              break;
            }
          }
          if (!found) {
            logger.warn("Refactoring of parameter map for orderinputsource of type " + ois.getType()
                + " produced unsupported parameter with name " + key);
          }
        }
        ois.setParameters(specifics);
      }

      if (changed) {
        changedInputSources.add(ois);
      }
    }

    //apply changes. TODO performance batchweise persistieren
    for (OrderInputSourceStorable ois : changedInputSources) {
      modifyOrderInputSource(ois);
    }

    String[] ret = new String[changedInputSources.size()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = changedInputSources.get(i).getName();
    }
    return ret;
  }


  private void unregisterFromDeploymentItemStateManagement(String inputSourceName, long revision) {
    DeploymentItemStateManagement deploymentItemStateManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    DeploymentContext ctx = DeploymentContext.empty();
    deploymentItemStateManagement.delete(convertNameToUniqueDeploymentItemStateName(inputSourceName), ctx, revision);
  }

  private void registerInDeploymentItemStateManagement(OrderInputSourceStorable oigs, OrderInputSource inputGen, boolean modify)
      throws XPRC_InvalidPackageNameException, MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException {
    DeploymentItemStateManagement deploymentItemStateManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    final Optional<DeploymentItem> di = DeploymentItemBuilder.buildInputSource(oigs, inputGen);
    long revision = oigs.getRevision();
    //immer so tun, als w�re die inputsource deployed
    deploymentItemStateManagement.save(di.get(), revision);
    DeploymentContext ctx = new DeploymentContext(new GenerationBaseCache()) {

      public Optional<DeploymentMode> getDeploymentMode(XMOMType type, String fqName, long revision) {
        if (fqName.equals(di.get().getName())) {
          return Optional.of(DeploymentMode.codeChanged);
        }
        return super.getDeploymentMode(type, fqName, revision);
      }
    };
    deploymentItemStateManagement.collectUsingObjectsInContext(di.get().getName(), ctx, revision);
    deploymentItemStateManagement.deployFinished(di.get().getName(), DeploymentTransition.SUCCESS, true, Optional.<Throwable> empty(), revision);

    if (modify) {
      //deploy workflows die oben ermittelt wurden, um sie ggf zu reparieren
      Map<Long, Map<XMOMType, Map<String, DeploymentMode>>> additionalObjectsForCodeRegeneration = ctx.getAdditionalObjectsForCodeRegeneration();
      if (additionalObjectsForCodeRegeneration != null) {
        List<GenerationBase> toDeploy = new ArrayList<GenerationBase>();
        GenerationBaseCache cache = new GenerationBaseCache();
        for (Entry<Long, Map<XMOMType, Map<String, DeploymentMode>>> entry : additionalObjectsForCodeRegeneration.entrySet()) {
          Long rev = entry.getKey();
          Map<XMOMType, Map<String, DeploymentMode>> revObjects = entry.getValue();
          for (String name : revObjects.get(XMOMType.WORKFLOW).keySet()) {
            if (logger.isDebugEnabled()) {
              logger.debug("deploying " + name + " because orderinputsource " + oigs.getName() + " changed.");
            }
            toDeploy.add(WF.getOrCreateInstance(name, cache, rev));
          }
        }

        if (toDeploy.size() > 0) {
          for (GenerationBase gb : toDeploy) {
            gb.setDeploymentComment("OrderInputSource dependency of " + oigs.getName());
          }
          GenerationBase.deploy(toDeploy, DeploymentMode.regenerateDeployedAllFeatures, false, WorkflowProtectionMode.FORCE_DEPLOYMENT);
        }
      }
    }
  }


  public static String convertNameToUniqueDeploymentItemStateName(String inputSourceName) {
    return DEPLOYMENT_ITEM_PATH + "." + inputSourceName;
  }


  public static String convertTypeNameToFQFormName(String typeName) {
    //GUI ben�tigt, dass formname immer datatypename + "Form" ist
    return convertTypeNameToFQDatatypeName(typeName) + "Form";
  }


  public static String convertFQFormNameToTypeName(String formName) {
    String simpleFormName = formName.substring(formName.lastIndexOf('.') + 1);
    return simpleFormName.substring(0, simpleFormName.length() - 4);
  }


  public static String convertTypeNameToFQDatatypeName(String typeName) {
    return FORM_PATH + "." + typeName;
  }


  public int getReferenceCount(DestinationValue dv, Long rootRevision) {
    return getReferenceCountRecursively(dv, new ArrayList<String>(), rootRevision);
  }

  public List<String> getReferencedOrderInputSources(DestinationValue dv, Long rootRevision) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Set<String> inputSources = new HashSet<String>();
    Set<Long> allRevisions = dv.resolveAllRevisions(rootRevision);
    for (Long aRevision : allRevisions) {
      OrderInputSourceSeriesInfo info = orderInputSourceSeriesInfo.get(Pair.of(dv, aRevision));
      if (info != null) {
        inputSources.addAll(storage.getOrderInputSourceNames(info.allOrderInputSources));
      }
    }
    return new ArrayList<String>(inputSources);
  }
  
  
  /*
   * - TODO z�hle mehrfache aufrufe von workflows, die auch mehrfach
   * - bei rekursion abbrechen
   * - z�hle jede inputsource-invocation einzeln
   * - bei foreaches nur einmal z�hlen
   */
  private int getReferenceCountRecursively(DestinationValue dv, List<String> visitedWFsStack, Long rootRevision) {
    if (visitedWFsStack.contains(dv.getFQName())) {
      //endlosrekursion vermeiden
      return 0;
    }
    visitedWFsStack.add(dv.getFQName());
    try {
      Set<Long> allRevisions = dv.resolveAllRevisions(rootRevision);
      
      int s = 0;
      for (Long aRevision : allRevisions) {
        OrderInputSourceSeriesInfo info = orderInputSourceSeriesInfo.get(Pair.of(dv, aRevision));   
        if (info != null) {
          //cache aktuell?
          if (OrderInputSourceSeriesInfo.cacheValidationId_Expect == info.cacheValidationId_Is) {
            return info.cachedReferenceCountRecursively;
          }
          s = info.cntAllOrderInputSources;
        }
        //subworkflows �ber deploymentitemstatemanagement herausfinden
        DeploymentItemStateImpl diis =
            (DeploymentItemStateImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement().get(dv.getFQName(), aRevision);
        if (diis != null) {
          Set<String> wfs = diis.getWorkflowsCalledByThis(DeploymentLocation.DEPLOYED);
          for (String wf : wfs) {
            s += getReferenceCountRecursively(new FractalWorkflowDestination(wf), visitedWFsStack, aRevision);
          }
        }

        if (info != null) {
          info.cachedReferenceCountRecursively = s;
          info.cacheValidationId_Is = OrderInputSourceSeriesInfo.cacheValidationId_Expect;
        }
      }
      return s;
    } finally {
      visitedWFsStack.remove(visitedWFsStack.size() - 1);
    }
  }

}
