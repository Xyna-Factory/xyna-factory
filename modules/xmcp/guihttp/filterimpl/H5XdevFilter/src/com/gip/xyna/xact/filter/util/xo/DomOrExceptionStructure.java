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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.ExpiringMap;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.monitor.MonitorAudit;
import com.gip.xyna.xact.filter.monitor.MonitorSession;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.OrderItemWithoutAudit;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.OrderItemWithoutAuditLoader;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.RemoveOperationTagFilter;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.BasicApplicationName;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLInputSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLInputSourceFromStrings;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.BasicAuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.EnhancedAudit;



public class DomOrExceptionStructure extends RuntimeContextDependendAction {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(DomOrExceptionStructure.class);
  
  private final ExpiringMap<Long, EnhancedAudit> auditCache = new ExpiringMap<>(5, TimeUnit.MINUTES, true);
  

  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Test";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }


  @Override
  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/structure") && Method.POST == method;
  }
  
  public static class SubtypesStructureRequest {
    public Long orderId;
    public Boolean isUploadedAudit = false;
    public final List<XMOMObjectIdentifier> objects = new ArrayList<>();
  }
  
  public static class XMOMObjectIdentifier {
    public String fqn;
    public RuntimeContext rtc;
    public RuntimeContext getRTCOr(RuntimeContext rc) {
      if (rtc != null) {
        return rtc;
      }
      return rc;
    }
  }

  /*
   * {
   *   "orderId" : zahl
   *   "objects" : [
   *              {
   *                 "fqn" : myfqn
   *                 "rtc" : {
   *                             "workspace" : workspacename
   *                             "application" : applicationname
   *                             "version" : versionname
   *                         }
   *              }
   *               ]
   * }
   */
  public static class SubtypesStructureRequestParser extends EmptyJsonVisitor<SubtypesStructureRequest> {

    private static final String ORDERID = "orderId";
    private static final String OBJECTS = "objects";
    private static final String FQN = "fqn";
    private static final String RTC = "rtc";
    
    private SubtypesStructureRequest req = new SubtypesStructureRequest();
    
    @Override
    public SubtypesStructureRequest get() {
      return req;
    }

    @Override
    public SubtypesStructureRequest getAndReset() {
      req = new SubtypesStructureRequest();
      return req;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(ORDERID) && value != null) {
        if(value.startsWith(MonitorAudit.IMPORTED)) {
          req.isUploadedAudit = true;
          req.orderId = Long.valueOf(value.replace(MonitorAudit.IMPORTED, ""));
        } else {
          req.orderId = Long.valueOf(value);
        }
      }
    }

    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      if (label.equals(OBJECTS)) {
        for (Object o : values) {
          GenericResult gr = (GenericResult) o;
          XMOMObjectIdentifier i = new XMOMObjectIdentifier();
          String fqn = gr.getAttribute(FQN).getFirst();
          i.fqn = fqn;
          GenericResult rtc = gr.getObject(RTC);
          if (rtc != null) {
            i.rtc = rtc.visit(new RuntimeContextVisitor()); 
          }
          req.objects.add(i);
        }
      }
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      return new GenericVisitor();
    }

    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
    }
    
  }
  
  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonParser jp = new JsonParser();
    SubtypesStructureRequest ssr;
    try {
      ssr = jp.parse(tc.getPayload(), new SubtypesStructureRequestParser());
      if(ssr.isUploadedAudit) {
        // Bei einem Audit-Upload existiert der RTC u. U. lokal nicht.
        return act(null, null, url, tc.getMethodEnum(), tc);
      }
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      // nothing
    }
    return super.act(url, tc);
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (!checkLoginAndRights(tc, jfai, Rights.READ_MDM.toString())) {
      return jfai;
    }
    
    //parsing
    JsonParser jp = new JsonParser();
    SubtypesStructureRequest ssr;
    try {
      ssr = jp.parse(tc.getPayload(), new SubtypesStructureRequestParser());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }
    
    //create Generationbase Objects
    GenerationBaseCache commonCache = new GenerationBaseCache();
    List<Pair<String, DomOrExceptionGenerationBase>> parsedObjects = new ArrayList<>();
    Map<String, RuntimeContext> runtimeContextMap = null;
    GenerationBaseCache commonCacheForBasicDTs = new GenerationBaseCache();
    Long auditOrderId = ssr.orderId;
    if (auditOrderId != null) {
      if(ssr.isUploadedAudit) {
        OrderItemWithoutAudit orderItem  = OrderItemWithoutAuditLoader.loadFromUpload(MonitorSession.getSessionInstance(tc), auditOrderId);
        runtimeContextMap = createRuntimeContextMap(ssr.objects, orderItem.getMeta().getRtc(), orderItem.getImports());
        for (XMOMObjectIdentifier i : ssr.objects) {
          DomOrExceptionGenerationBase gb = null;
          if (BasicAuditImport.isBasicDataType(i.fqn)) {
            Long rev = ((ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getApplicationManagement()).getBasicApplicationRevision(BasicApplicationName.Base);
            gb = (DomOrExceptionGenerationBase) GenerationBase.getOrCreateInstance(i.fqn, commonCacheForBasicDTs, rev);
          } else {
            Map<String, String> xmlsWfAndImports = new HashMap<>();
            for (AuditImport curImport : orderItem.getImports()) {
              xmlsWfAndImports.putIfAbsent(curImport.getFqn(), curImport.getDocument());
            }
            XMLInputSourceFromStrings inputSource = new XMLInputSourceFromStrings(xmlsWfAndImports);
            
            List<AuditImport> imports = orderItem.getImports();
            for (AuditImport auditImport : imports) {
              if(auditImport.getFqn().equals(i.fqn)) {
                gb = getGenerationBaseFromXml(i.fqn, auditImport.getDocument(), inputSource);
                break;
              }
            }
          }
          if(gb != null) {
            gb.parseGeneration(true/*deployed*/, false, false);
            parsedObjects.add(Pair.of(i.fqn, gb));
          }
        }
      } else {
        RepositoryManagement repo = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement();
        
        EnhancedAudit au = getEnhancedAudit(auditOrderId);
        long repositoryRevision = au.getRepositoryRevision();
        
        runtimeContextMap = createRuntimeContextMap(ssr.objects, au.getWorkflowContext(), au.getImports());

        for (XMOMObjectIdentifier i : ssr.objects) {
          DomOrExceptionGenerationBase gb;
          if (BasicAuditImport.isBasicDataType(i.fqn)) { //audit daten enthalten diese objekte nicht, deshalb kann die gui sie auch nicht in der richtigen version anfragen
            Long rev = ((ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getApplicationManagement()).getBasicApplicationRevision(BasicApplicationName.Base);
            gb = (DomOrExceptionGenerationBase) GenerationBase.getOrCreateInstance(i.fqn, commonCacheForBasicDTs, rev);
          } else {
            gb = getGenerationBaseFromRepository(runtimeContextMap, au.getWorkflowContext(), repo, repositoryRevision, i.fqn, commonCache);
          }
          gb.parseGeneration(true/*deployed*/, false, false);
          parsedObjects.add(Pair.of(i.fqn, gb));
        }
      }
    } else {
      Long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rc);
      for (XMOMObjectIdentifier i : ssr.objects) {
        RuntimeContext r = i.getRTCOr(rc);
        Long actualRev = r == rc ? rev : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(r);
        GenerationBase gb = null;
        try {
          gb = DatatypeVariable.ANY_TYPE.equals(i.fqn) ?
                 DOM.getOrCreateInstance(i.fqn, commonCache, actualRev) :
                 GenerationBase.getOrCreateInstance(i.fqn, commonCache, actualRev);
        } catch (Ex_FileAccessException e) {
          gb = GenerationBase.getOrCreateInstance(i.fqn, commonCache, rev); // fallback
        }

        gb.parseGeneration(true/*deployed*/, false, false);
        parsedObjects.add(Pair.of(i.fqn, (DomOrExceptionGenerationBase) gb));
      }
    }

    //rendering
    JsonBuilder jb = new JsonBuilder();
    jb.startObject(); {
      for (Pair<String, DomOrExceptionGenerationBase> p : parsedObjects) {
        jb.addObjectAttribute(p.getFirst()); {
          append(p.getSecond(), jb, rc, runtimeContextMap);
        } jb.endObject();
      }
    } jb.endObject();

    jfai.sendJson(tc, jb.toString());
    return jfai;
  }
  
  
  private EnhancedAudit getEnhancedAudit(Long orderId) {
    EnhancedAudit au = auditCache.get(orderId);
    if (au != null) {
      return au;
    }
    OrderInstanceDetails details;
    try {
      details = XynaFactory.getInstance().getXynaMultiChannelPortal().getOrderInstanceDetails(orderId);
    } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Audit not found", e);
    }
    if (details.getAuditDataAsXML() == null || details.getAuditDataAsXML().length() == 0) {
      throw new RuntimeException("Audit not found");
    }
    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    String shortenedXml = null;
    try {
      shortenedXml = RemoveOperationTagFilter.filter(details.getAuditDataAsXML());
    } catch (ParserConfigurationException | SAXException | TransformerException | TransformerFactoryConfigurationError e) {
      throw new RuntimeException(e);
    }
    
    
    au = xmlHelper.auditFromXml(shortenedXml, false);
    auditCache.put(orderId, au);
    return au;
  }


  private Map<String, RuntimeContext> createRuntimeContextMap(List<XMOMObjectIdentifier> objects, RuntimeContext rc, List<AuditImport> auditImports) {
    Map<String, RuntimeContext> map = new HashMap<>();
    for (XMOMObjectIdentifier i : objects) {
      map.put(i.fqn, i.getRTCOr(rc));
    }
    for (AuditImport auditImport : auditImports) {
      map.put(auditImport.getFqn(),  auditImport.getRuntimeContext());
    }
    return map;
  }
  
  private DomOrExceptionGenerationBase getGenerationBaseFromXml(String fqName, String xml, XMLInputSource inputSource)
      throws XynaException {
    String rootTag;
    try {
      rootTag = XMLUtils.getRootElementName(new ByteArrayInputStream(xml.getBytes(Constants.DEFAULT_ENCODING)));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
    
    
    
//    XMLInputSource inputSource = new XMLInputSource() {
//
//      @Override
//      public Document getOrParseXML(GenerationBase obj, boolean fileFromDeploymentLocation)
//          throws Ex_FileAccessException, XPRC_XmlParsingException {
//        return XMLUtils.parseString(xml, true);
//      }
//    };
    XMOMType type = XMOMType.getXMOMTypeByRootTag(rootTag);
    switch (type) {
      case DATATYPE :
        return DOM.getOrCreateInstance(fqName, new GenerationBaseCache(), -100L /* gibts nicht, wird nicht benötigt */, inputSource);
      case EXCEPTION :
        return ExceptionGeneration.getOrCreateInstance(fqName, new GenerationBaseCache(), -100L /* gibts nicht, wird nicht benötigt */, inputSource);
      default :
        throw new RuntimeException("unsupported type: " + type);
    }
  }


  private DomOrExceptionGenerationBase getGenerationBaseFromRepository(final Map<String, RuntimeContext> runtimeContextMap,
                                                                       final RuntimeContext rtc, final RepositoryManagement repo,
                                                                       final long repRevision, final String fqName,
                                                                       GenerationBaseCache cache)
      throws XynaException {
    String xml = repo.getXMLFromRepository(runtimeContextMap.get(fqName), repRevision, fqName);
    String rootTag;
    try {
      rootTag = XMLUtils.getRootElementName(new ByteArrayInputStream(xml.getBytes(Constants.DEFAULT_ENCODING)));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
    XMLInputSource inputSource = new XMLInputSource() {

      @Override
      public Document getOrParseXML(GenerationBase obj, boolean fileFromDeploymentLocation)
          throws Ex_FileAccessException, XPRC_XmlParsingException {
        RuntimeContext rc = runtimeContextMap.get(obj.getOriginalFqName());
        if (rc == null) {
          rc = rtc;
        }
        String xml = null;
        try {
          xml = repo.getXMLFromRepository(rc, repRevision, obj.getOriginalFqName());
        } catch (XynaException e) {
          logger.warn("Not found in repository:" + obj.getOriginalFqName() + "@" + repRevision + "@"
              + rc.getGUIRepresentation());
        }
        if (xml == null) {
          obj.setDoesntExist();
          return null;
        }
        return XMLUtils.parseString(xml, true);
      }
    };
    XMOMType type = XMOMType.getXMOMTypeByRootTag(rootTag);
    switch (type) {
      case DATATYPE :
        return DOM.getOrCreateInstance(fqName, cache, -100L /* gibts nicht, wird nicht benötigt */, inputSource);
      case EXCEPTION :
        return ExceptionGeneration.getOrCreateInstance(fqName, cache, -100L /* gibts nicht, wird nicht benötigt */, inputSource);
      default :
        throw new RuntimeException("unsupported type: " + type);
    }
  }


  public static void append(DomOrExceptionGenerationBase dom, JsonBuilder jb, RuntimeContext rootRTC, Map<String, RuntimeContext> rtcMap) {
    jb.addObjectAttribute(XynaObjectVisitor.OBJECT_TAG); {
      appendTypeInfo(dom, jb, rootRTC, rtcMap);
      if (dom instanceof DOM) {
        String doc = ((DOM) dom).getDocumentation();
        if (doc != null) {
          jb.addStringAttribute(XynaObjectVisitor.DOCU_TAG, doc);
        }
      }
      jb.addBooleanAttribute(XynaObjectVisitor.IS_ABSTRACT_TAG, dom.isAbstract());
    } jb.endObject();
    List<AVariable> vars = dom.getAllMemberVarsIncludingInherited();
    for (AVariable dv : vars) {
      jb.addObjectAttribute(dv.getVarName()); {
        appendVariableType(jb, dv, rootRTC, rtcMap);
      } jb.endObject();
    }
    //instanzmethoden (ohne die von supertypen)
    /*
     "sow()": {

            "$label": "Sow",
            "$method": {
                "returns": [
                    {
                        "$object": {
                            "fqn": "jh.nonstorables.Harvest",
                            "label": "Harvest"
                        }
                    }
                ],
                "params": [
                    {
                        "$object": {
                            "fqn": "jh.nonstorables.Person",
                            "label": "Sowyer"
                        }
                    },
                    {
                        "$list": {
                            "fqn": "jh.nonstorables.Seed",
                            "label": "Seeds"
                        }
                    },
                    {
                        "$primitive": {
                            "fqn": "String",
                            "label": "Species"
                        }
                    }
                ],
                "$docu": "",
                "label": "Sow"
            }
        }
     */
    if (dom instanceof DOM) {
      DOM datatype = (DOM) dom;
      Map<String, Operation> instanceMethods = new HashMap<>();
      while (datatype != null) {
        for (Operation operation : datatype.getOperations()) {
          if (!operation.isStatic()) {
            if (!instanceMethods.containsKey(operation.getName())) {
              instanceMethods.put(operation.getName(), operation);
            }
          }
        }
        datatype = datatype.getSuperClassGenerationObject();
      }
      for (Operation operation : instanceMethods.values()) {
        jb.addObjectAttribute(operation.getName() + "()");
        {
          jb.addStringAttribute(XynaObjectVisitor.META_LABEL_TAG, operation.getName());
          jb.addObjectAttribute(XynaObjectVisitor.META_METHOD_TAG);
          {
            if (!operation.getOutputVars().isEmpty()) {
              jb.addListAttribute(XynaObjectVisitor.OUTPUTS_LIST_TAG);
              for (AVariable output : operation.getOutputVars()) {
                jb.startObject();
                {
                  appendVariableType(jb, output, rootRTC, rtcMap);
                }
                jb.endObject();
              }
              jb.endList();
            }
            if (!operation.getInputVars().isEmpty()) {
              jb.addListAttribute(XynaObjectVisitor.INPUTS_LIST_TAG);
              for (AVariable input : operation.getInputVars()) {
                jb.startObject();
                {
                  appendVariableType(jb, input, rootRTC, rtcMap);
                }
                jb.endObject();
              }
              jb.endList();
            }
            jb.addStringAttribute(XynaObjectVisitor.DOCU_TAG, operation.getDocumentation());
            jb.addStringAttribute(XynaObjectVisitor.LABEL_TAG, operation.getName());
            if (operation.isAbstract()) {
              jb.addBooleanAttribute(XynaObjectVisitor.IS_ABSTRACT_TAG, operation.isAbstract());
            }
          }
          jb.endObject();
        }
        jb.endObject();
      }
    }
  }


  private static void appendVariableType(JsonBuilder jb, AVariable dv, RuntimeContext rootRTC, Map<String, RuntimeContext> rtcMap) {
      jb.addStringAttribute(XynaObjectVisitor.META_LABEL_TAG, dv.getLabel());
      if (dv.getDocumentation() != null) {
        jb.addStringAttribute(XynaObjectVisitor.DOLLAR_DOCU_TAG, dv.getDocumentation());
      }
      if (dv.isList()) {
        jb.addObjectAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG); {
          if (dv.isJavaBaseType() && dv.getJavaTypeEnum() != PrimitiveType.XYNAEXCEPTIONBASE && dv.getJavaTypeEnum() != PrimitiveType.XYNAEXCEPTION) {
            jb.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, dv.getJavaTypeEnum().getClassOfType());
            jb.addStringAttribute(XynaObjectVisitor.LABEL_TAG, dv.getJavaTypeEnum().getClassOfType());
          } else {
            appendTypeInfo(dv.getDomOrExceptionObject(), jb, rootRTC, rtcMap);
          }
        } jb.endObject();
      } else {
        if (dv.isJavaBaseType()) {
          jb.addObjectAttribute(XynaObjectVisitor.PRIMITIVE_TAG); {
            jb.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, dv.getJavaTypeEnum().getClassOfType());
          } jb.endObject();
        } else {
          jb.addObjectAttribute(XynaObjectVisitor.OBJECT_TAG); {
            appendTypeInfo(dv.getDomOrExceptionObject(), jb, rootRTC, rtcMap);
          } jb.endObject();
        }
      }

  }


  private static void appendTypeInfo(DomOrExceptionGenerationBase dom, JsonBuilder jb, RuntimeContext rootRTC,
                                     Map<String, RuntimeContext> rtcMap) {
    jb.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, dom.getOriginalFqName());
    RuntimeContext objectRTC;
    if (rtcMap != null) {
      objectRTC = rtcMap.get(dom.getOriginalFqName());
    } else {
      objectRTC = dom.getRuntimeContext();
    }
    if (objectRTC != null) {
      Util.writeRuntimeContext(jb, objectRTC);
    }
    jb.addStringAttribute(XynaObjectVisitor.LABEL_TAG, dom.exists() ? dom.getLabel() : dom.getOriginalSimpleName());
  }

}