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

package com.gip.xyna.xprc.remotecallserialization;



import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class XynaXmomSerialization extends Section {

  private static final XynaPropertyBoolean useXMlSerialization = new XynaPropertyBoolean("xprc.remotecallserialization.useXMLSerialization", true);

  private static Logger logger = CentralFactoryLogging.getLogger(XynaXmomSerialization.class);

  private Map<Long, RevisionSerialization> serializations;


  public XynaXmomSerialization() throws XynaException {
    super();
  }


  public void invalidateRevisions(Collection<Long> revisions) {
    if (revisions == null) {
      return;
    }
    for (Long rev : revisions) {
      invalidateRevision(rev);
    }
  }


  public void invalidateRevision(Long revision) {
    synchronized (XynaXmomSerialization.class) {
      RevisionSerialization removed = serializations.remove(revision);
      if (removed != null) {
        removed.close();
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Invalidating revision: " + revision);
    }
  }

  
  public static String getFqnXmlName(Class<? extends GeneralXynaObject> xmomClass) {
    if (GenerationBase.isReservedServerObjectByFqClassName(xmomClass.getCanonicalName())) {
      return GenerationBase.getXmlNameForReservedClass(xmomClass);
    } else {
      return xmomClass.getCanonicalName();
    }
  }
  

  public byte[] serialize(Long revision, GeneralXynaObject obj) {
    if(obj == null) {
      return new byte[0];
    }
    RevisionSerialization revisionSerialization = getOrCreateRevisionSerialization(revision);
    Serializer result = revisionSerialization.getSerializer(obj.getClass());
    if (logger.isDebugEnabled()) {
      logger.debug("serializing " + obj + " - with revision: " + revision);
    }
    return result.serialize(obj);
  }


  public GeneralXynaObject deserialize(Long revision, String fqn, byte[] data) {
    if(data == null || fqn == null || data.length == 0 || fqn.length() == 0) {
      return null;
    }
    RevisionSerialization revisionSerialization = getOrCreateRevisionSerialization(revision);
    Deserializer result = revisionSerialization.getDeserializer(fqn);
    if (logger.isDebugEnabled()) {
      logger.debug("deserialize " + fqn + " with revision: " + revision);
    }
    return result.deserialize(revision, data);
  }


  private RevisionSerialization getOrCreateRevisionSerialization(Long revision) {
    RevisionSerialization revisionSerialization = serializations.get(revision);
    if (revisionSerialization == null || !validSerializationType(revisionSerialization)) {
      synchronized (XynaXmomSerialization.class) {
        revisionSerialization = serializations.get(revision);
        if(revisionSerialization == null || !validSerializationType(revisionSerialization)) {
          RevisionSerialization revSer = useXMlSerialization.get() ? new XMLRevisionSerialization() : new KryoRevisionSerialization(revision);
          serializations.put(revision, revSer);
          revisionSerialization = revSer;      
        }
      }
    }
    return revisionSerialization;
  }


  private boolean validSerializationType(RevisionSerialization rs) {
    boolean useXML = useXMlSerialization.get();
    return (rs instanceof XMLRevisionSerialization && useXML) || (rs instanceof KryoRevisionSerialization && !useXML);
  }

  
  public interface RevisionSerialization {
    public Serializer getSerializer(Class<? extends GeneralXynaObject> clazz);
    public void close();
    public Deserializer getDeserializer(String fqn);
  }

  private static class XMLRevisionSerialization implements RevisionSerialization{

    private Map<String, Serializer> serializers;
    private Map<String, Deserializer> deserializers;


    public XMLRevisionSerialization() {
      serializers = new ConcurrentHashMap<String, XynaXmomSerialization.Serializer>();
      deserializers = new ConcurrentHashMap<String, XynaXmomSerialization.Deserializer>();
    }


    public Serializer getSerializer(Class<? extends GeneralXynaObject> clazz) {
      String fqn = clazz.getCanonicalName();
      Serializer result = serializers.get(fqn);
      if (result == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("creating serializer for " + fqn);
        }
        serializers.put(fqn, XMLRevisionSerialization::serializeXML);
        result = serializers.get(fqn);
      }
      return result;
    }


    public Deserializer getDeserializer(String fqn) {
      Deserializer result = deserializers.get(fqn);
      if (result == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("creating deserializer for " + fqn);
        }
        deserializers.put(fqn, XMLRevisionSerialization::deserializeXML);
        result = deserializers.get(fqn);
      }
      return result;
    }


    //replace serializeXML and deserializeXML with different approach later
    private static byte[] serializeXML(GeneralXynaObject obj) {
      return obj.toXml().getBytes();
    }


    private static GeneralXynaObject deserializeXML(Long revision, byte[] data) {
      try {
        return XynaObject.generalFromXml(new String(data), revision);
      } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException e) {
        throw new RuntimeException(e);
      }
    }


    public void close() {
    }
  }

  public interface Serializer {

    byte[] serialize(GeneralXynaObject obj);
  }

  public interface Deserializer {

    GeneralXynaObject deserialize(Long revision, byte[] data);
  }


  @Override
  public String getDefaultName() {
    return "XynaXmomSerialization";
  }


  @Override
  protected void init() throws XynaException {
    serializations = new ConcurrentHashMap<Long, RevisionSerialization>();
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addDeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION, new DeploymentHandler() {

          //might only need FQN+Revision
          private final Set<Long> objects = new HashSet<Long>();


          @Override
          public void finish(boolean success) throws XPRC_DeploymentHandlerException {
            if (!success) {
              objects.clear();
              return;
            }

            if (logger.isDebugEnabled()) {
              logger.debug("invalidating: " + objects.size() + " revision");
            }
            
            for (Long revision : objects) {
              invalidateRevision(revision);
            }
            objects.clear();
          }


          @Override
          public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
            objects.add(object.getRevision());
          }


          @Override
          public void begin() throws XPRC_DeploymentHandlerException {
            objects.clear();
          }
        });
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
      .addUndeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION, new UndeploymentHandler() {
        
        private final Set<Long> objects = new HashSet<Long>();

        public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
          objects.add(object.getRevision());
        }

        public void finish() throws XPRC_UnDeploymentHandlerException {
          // invalidate changed revisions
          Set<Long> revisionsToInvalidate = new HashSet<Long>();
          for (Long object : objects) {
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(object, revisionsToInvalidate);
          }
          XynaFactory.getInstance().getProcessing().getXmomSerialization().invalidateRevisions(revisionsToInvalidate);
        }
        
        public boolean executeForReservedServerObjects() { return false; }
        public void exec(FilterInstanceStorable object) { }
        public void exec(TriggerInstanceStorable object) { }
        public void exec(FilterStorable object) { }
        public void exec(TriggerStorable object) { }
        public void exec(Capacity object) { }
        public void exec(DestinationKey object) { }

    });

  }


  @Override
  protected void shutdown() throws XynaException {
    serializations = null;
  }
}
