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

package com.gip.xyna.xdev.xfractmod.xmdm;



import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.concurrent.ExecutionWrapper;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_CLASS_INSTANTIATION_PROBLEM;
import com.gip.xyna.xdev.exceptions.XDEV_INVALID_XML_MISSING_TAG;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.exceptions.XDEV_UserDefinedDeploymentException;
import com.gip.xyna.xdev.exceptions.XDEV_UserDefinedUnDeploymentException;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public abstract class XynaObject implements GeneralXynaObject {

  private static final long serialVersionUID = -6068274822967865871L;

  private static final transient Logger logger = CentralFactoryLogging.getLogger(XynaObject.class);
  public static final transient GeneralXynaObject[] EMPTY_XYNA_OBJECT_ARRAY = new GeneralXynaObject[0];

  private static ReentrantLock cacheLock = new ReentrantLock();
  
  //TODO als Management beim Factory-Start anlegen
  private static XynaObjectDeploymentListenerManagement deploymentListenerManagement = new XynaObjectDeploymentListenerManagement();
  
  
  public static XynaObjectDeploymentListenerManagement getXynaObjectDeploymentListenerManagement() {
    return deploymentListenerManagement;
  }
  
  /**
   * cache fürs fromXML, damit da nicht jedes mal die xmls geparst werden müssen. wird bei jeden deployment invalidiert.
   */
  private static final GenerationBaseCache generationCache = new GenerationBaseCache();

  // TODO These deployment tasks are just kept here for downward compatibility, remove this map once we stop supporting versions < 3.1.0.0
  private static HashMap<Class<? extends XynaObject>, List<DeploymentTask>> deploymentTasks =
      new HashMap<Class<? extends XynaObject>, List<DeploymentTask>>();


  public XynaObject() {
    if (logger.isTraceEnabled()) {
      logger.trace(new StringBuilder().append("created object ").append(this).append(" with classloader ")
          .append(getClass().getClassLoader()));
    }
  }


  public void onDeployment() throws XynaException {
    // defaultmässig passiert hier nichts. abgeleitete objekte können hier:
    // - trigger registrieren
    if (logger.isTraceEnabled()) {
      logger.trace("deploying " + this);
    }
  }


  public void onUndeployment() throws XynaException {
    if (logger.isTraceEnabled()) {
      logger.trace("undeploying " + this);
    }
  }


  /**
   * deep copy implementation
   */
  public abstract XynaObject clone();
  
  public XynaObject clone(boolean deep) {
    if (deep) {
      return clone();
    } else {
      throw new UnsupportedOperationException("shallow clone not supported for xynaObject");
    }
  }

  //ACHTUNG: wenn neue (auch abstrakte) instanzmethoden hinzugefügt werden, auch in JavaServiceImplementation im generierten code beachten! am besten mal abstrakten instanzservice testen

  public static XynaObject fromXml(String xmlString) throws XPRC_XmlParsingException,
      XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    return fromXml(xmlString, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  //abwärtskompatible version from generalFromXml, die nur xynaobjekte zurückgibt
  public static XynaObject fromXml(String xmlString, Long revision) throws XPRC_XmlParsingException,
      XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    GeneralXynaObject result = generalFromXml(xmlString, revision);
    if (!(result instanceof XynaObject)) {
      throw new ClassCastException("xml does not represent a XynaObject.");
    }
    return (XynaObject) result;
  }
  
  public static XynaObject fromXml(String xmlString, ConnectionFilter<?> cf) throws XPRC_XmlParsingException,
      XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    Long revision = ((ClassLoaderBase) cf.getClass().getClassLoader()).getRevision();
    if (revision == null) {
      revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    return fromXml(xmlString, revision);
  }


  /**
   * erstellt ein xynaobject aus einem xml, welches das format &lt;Data&gt;...&lt;/Data&gt; hat, oder ein beliebiges rootelement
   * hat, welches aber mehrere solcher Data-Elemente enthält. im letzeren fall wird ein container-objekt um die
   * einzelnen xynaobjects erstellt.
   * Wenn das umschliessende Container-Objekt fehlt, funktioniert es auch.
   */
  public static GeneralXynaObject generalFromXml(String xmlString, Long revision) throws XPRC_XmlParsingException,
      XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    Document doc;
    try {
      doc = XMLUtils.parseString(xmlString);
    } catch (XPRC_XmlParsingException e) {
      xmlString = "<Container>" + xmlString + "</Container>";
      try {
        doc = XMLUtils.parseString(xmlString);
      } catch (XPRC_XmlParsingException e2) {
        throw e;
      }
    }
    Element root = doc.getDocumentElement();
    if (!(root.getTagName().equals(GenerationBase.EL.DATA) || root.getTagName().equals(GenerationBase.EL.EXCEPTION))) {
      List<Element> varEls = XMLUtils.getChildElements(root);
      if (varEls.size() > 1) {
        Container c = new Container();
        for (Element e : varEls) {
          if (!(e.getTagName().equals(GenerationBase.EL.DATA) || e.getTagName().equals(GenerationBase.EL.EXCEPTION))) {
            throw new XPRC_InvalidXMLForObjectCreationException(new XDEV_INVALID_XML_MISSING_TAG(GenerationBase.EL.DATA));
          }
          c.add(createObject(e, revision));
        }
        return c;
      } else if (varEls.size() == 1) {
        return createObject(varEls.get(0), revision);
      } else {
        return new Container();
      }
    } else {
      return createObject(root, revision);
    }
  }


  /**
   * falls e eine liste beschreibt, wird eine xynaobjectlist zurückgegeben, 
   * falls ReferenceName und ReferencePath fehlen, wird null zurückgegeben, 
   * ansonsten eine instanz eines normalen mdm objekts
   */
  private static GeneralXynaObject createObject(Element e, Long revision) throws XPRC_MDMObjectCreationException,
      XPRC_InvalidXMLForObjectCreationException {
    AVariable v;
    DomOrExceptionGenerationBase dom;
    String originalClassName = e.getAttribute(GenerationBase.ATT.REFERENCENAME);
    String originalPath = e.getAttribute(GenerationBase.ATT.REFERENCEPATH);
    if( isEmpty(originalPath) && isEmpty(originalClassName) ) {
      return null; //ReferenceName und ReferencePath fehlen
    }
    
    String originalFqName = originalPath + "." + originalClassName;
    
    Element meta = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
    if (meta != null) {
      //bei auditdaten und toXml verwendet:
      Element applicationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.APPLICATION);
      Element applicationVersionElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.APPLICATION_VERSION);
      Element workspaceElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.WORKSPACE);
      if (applicationElement != null && applicationVersionElement != null) {
        try {
          revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(new Application(XMLUtils.getTextContent(applicationElement),
                                           XMLUtils.getTextContent(applicationVersionElement)));
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        }
      } else if (workspaceElement != null) {
        try {
          revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(new Workspace(XMLUtils.getTextContent(workspaceElement)));
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        }
      }
    }
    
    try {
      cacheLock.lock(); //TODO locking pro revision
      try {
        dom = (DomOrExceptionGenerationBase) generationCache.getFromCacheInCorrectRevision(originalFqName, revision);
        if (dom == null) {
          boolean isDom;
          String tagName = e.getTagName();
          isDom = tagName.equals(GenerationBase.EL.DATA);
          if (isDom) {
            dom = DOM.getOrCreateInstance(originalFqName, generationCache, revision);
            //revision separat übergeben, weil die ist evtl anders als dom.getRevision und kann kind-members auflösen
            v = new DatatypeVariable(dom, revision);
            v.parseXML(e);
          } else {
            dom = ExceptionGeneration.getOrCreateInstance(originalFqName, generationCache, revision);
            v = new ExceptionVariable(dom, revision);
            v.parseXML(e);
          }
          if (v.getFQClassName().equals(XynaObject.class.getName())) {
            throw new XPRC_InvalidXMLForObjectCreationException(new RuntimeException("Invalid XML: Possibly the root XML tag is missing a "
                                                                                         + GenerationBase.ATT.REFERENCENAME
                                                                                         + "or "
                                                                                         + GenerationBase.ATT.REFERENCEPATH
                                                                                         + " attribute."));
          }
        } else if (dom instanceof DOM) {
          v = new DatatypeVariable(dom, revision);
          v.parseXML(e);
        } else {
          v = new ExceptionVariable(dom, revision);
          v.parseXML(e);
        }
      } finally {
        cacheLock.unlock();
      }
    } catch (XynaException f) {
      throw new XPRC_InvalidXMLForObjectCreationException(f);
    }
    
    return dom.createObject(v, false); //cleanup passiert durch "clearGenerationCache"
  }

  
  private static boolean isEmpty(String string) {
    return string == null || string.length() == 0;
  }


  // in the event of a deployment the next from XML call needs to use a separate cache
  public static void clearGenerationCache(long revision) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    cacheLock.lock();    
    try {
      for (long r : generationCache.revisions()) {
        try {
          rm.getRuntimeContext(r);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //nicht vorhanden -> aus cache entfernen
          generationCache.removeRevision(r);
        }
      }
      generationCache.removeRevision(revision);
    } finally {
      cacheLock.unlock();
    }
  }
  

  @SuppressWarnings("unchecked")
  public static GeneralXynaObject instantiate(String className, boolean isDataType, Long revision) {
    Class<? extends GeneralXynaObject> c;
    if (GenerationBase.isReservedServerObjectByFqClassName(className)) {
      try {
        c = (Class<? extends GeneralXynaObject>) XynaObject.class.getClassLoader().loadClass(className);
      } catch (ClassNotFoundException | IllegalArgumentException | SecurityException e) {
        throw new RuntimeException("could not instantiate " + className, e);
      }
    } else {
      try {
        if (isDataType) {
          c =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                  .loadMDMClass(className, false, null, null, revision);
        } else {
          c =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                  .loadExceptionClass(className, false, null, null, revision);
        }
        if (c == null) {
          throw new RuntimeException("No classloader found for class " + className + " in revision " + revision
              + ". It is not deployed successfully.");
        }
        

      } catch (ClassNotFoundException | IllegalArgumentException | SecurityException e) {
        throw new RuntimeException("could not instantiate " + className, e);
      }
    }
    
    try {
      if (isDataType) {
        return c.getConstructor().newInstance();
      } else {
        //exceptions haben keinen public leeren konstruktor, aber private!
        Constructor<? extends GeneralXynaObject> constr = c.getDeclaredConstructor();
        constr.setAccessible(true);
        return constr.newInstance();
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
        | SecurityException e) {
      throw new RuntimeException("could not instantiate " + className, e);
    }
    
  }


  /**
   * erzeugt ein xml von einer instanz
   */
  public abstract String toXml(String varName, boolean onlyContent);


  public String toXml(String varName) {
    return toXml(varName, false);
  }


  public String toXml() {
    return toXml(null);
  }
  
  //TODO unversionedSet unterstützen. z.b. für xmompersistence
  public static void set(GeneralXynaObject target, String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND, InvalidObjectPathException {
    if (path.contains(".")) {
      int lastIdx = path.lastIndexOf(".");
      String pathToGet = path.substring(0, lastIdx);
      String varNameToSet = path.substring(lastIdx + 1, path.length());
      Object o = target.get(pathToGet);
      if (o == null) {
        throw new NullPointerException("Path " + pathToGet + " must not be null in targetobject.");
      }
      if (o instanceof GeneralXynaObject) {
        GeneralXynaObject xo = (GeneralXynaObject) o;
        xo.set(varNameToSet, value);
      } else {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
      }
    } else {
      target.set(path, value);
    }
  }


  public static interface DeploymentTask {

    /**
     * Called when deploying the service implementing it.<p>
     * 
     * This is also executed again on each classloader-reload, that is each
     * time a dependent object is redeployed, for example a type of an input parameter.
     * @throws XynaException
     */
    public void onDeployment() throws XynaException;


    /**
     * Called when undeploying the service implementing it.<p>
     * 
     * This is also executed again on each classloader-reload, that is each
     * time a dependent object is redeployed, for example a type of an input parameter.
     * @throws XynaException
     */
    public void onUndeployment() throws XynaException;

  }
  
  public static enum BehaviorAfterOnUnDeploymentTimeout {
    
    // Nach Timeout wird ein Interrupt-Signal an den on(Un)Deployment-Thread gesendet. Wenn Thread nicht reagiert, wird Deployment abgebrochen - Undeployment wird fortgesetzt.
    /**
     * Deployment will be aborted, while undeployment will log the exception and NOT abort.
     */
    EXCEPTION,
    
    // Nach Timeout wird ein Interrupt-Signal an den on(Un)Deployment-Thread gesendet. Wenn Thread nicht reagiert, wird (Un)Deployment fortgesetzt.
    /**
     * (Un)Deployment will be continued in another thread asynchronously.
     */
    IGNORE,
    
    // Nach Timeout wird ein Interrupt-Signal an den on(Un)Deployment-Thread gesendet. Wenn Thread nicht reagiert, wird er gekillt.
    /**
     * (Un)Deployment will be continued after calling {@link Thread#stop()} on the thread.<p>
     * Attention: {@link Thread#stop()} is inherently unsafe, use with care.
     */
    KILLTHREAD
  }

  /**
   * The (un)deployment process executes {@link DeploymentTask#onDeployment()} and {@link DeploymentTask#onUndeployment()}
   * in its own thread. After a timeout {@link Thread#interrupt()} will be called on the thread.
   * It the Thread still doesn't return, the Behavior returned by {@link #getBehaviorAfterOnUnDeploymentTimeout()} 
   * is used to decide the continuation.
   */
  public static interface ExtendedDeploymentTask extends DeploymentTask {

    /**
     * @return the timeout of the (un)deployment handler in milliseconds or null, if the default timeout defined by {@link XynaProperty}
     * xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout is to be used.
     */
    public Long getOnUnDeploymentTimeout();


    /**
     * @return how the (un)deployment process reacts to timeouts after calling 
     * {@link DeploymentTask#onDeployment()} or {@link DeploymentTask#onUndeployment()}
     */
    public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout();

  }


  public static void addDeploymentTask(Class<? extends XynaObject> cl, DeploymentTask task) {
    List<DeploymentTask> list = deploymentTasks.get(cl);
    if (list == null) {
      list = new ArrayList<DeploymentTask>();
      deploymentTasks.put(cl, list);
    }
    list.add(task);
  }

  
  public static void undeploy(Class<? extends XynaObject> clazz)
      throws XDEV_CLASS_INSTANTIATION_PROBLEM, XDEV_UserDefinedUnDeploymentException {
    try {
      if (!Modifier.isAbstract(clazz.getModifiers())) {
        if (logger.isTraceEnabled()) {
          logger.trace("creating instance of " + clazz.getName());
        }
        XynaObject xo;
        try {
          xo = clazz.getConstructor().newInstance();
        } catch (Exception e) {
          throw new XDEV_CLASS_INSTANTIATION_PROBLEM(clazz.getName(), e);
        }
        try {
          xo.onUndeployment();
        } catch (UnDeploymentTimeoutException e) {
          logger.warn("Timeout reached for undeploying datatype " + clazz.getName(), e);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.error("Error while undeploying datatype " + clazz.getName() + ". Ignoring it and continue undeployment.", t);
        }
      }
      // deploymenttask ausführen und entfernen.
      //abwärtskompatibilität. früher konnte man deploymenttasks bei xynaobject registrieren
      List<DeploymentTask> tasks = deploymentTasks.get(clazz);
      if (tasks != null) {
        deploymentTasks.remove(clazz);
        try {
          for (DeploymentTask t : tasks) {
            t.onUndeployment();
          }
        } catch (XynaException e) {
          throw new XDEV_UserDefinedUnDeploymentException(clazz.getName(), e);
        }
      }

    } finally {
      //DeploymentListener benachrichtigen
      deploymentListenerManagement.notifyUndeploy(clazz);
    }
  }


  public final void callOndeploymentHandlerFromServiceImpl(DeploymentTask task) throws XynaException {
    callThreadForDeploymentHandler(task, DeployCommand.DEPLOY);
  }
  
  
  public final void callOnUndeploymentHandlerFromServiceImpl(DeploymentTask task) throws XynaException {
    callThreadForDeploymentHandler(task, DeployCommand.UNDEPLOY);
  }
  
  private void callThreadForDeploymentHandler(final DeploymentTask task, final DeployCommand command) throws XynaException {
    
    BehaviorAfterOnUnDeploymentTimeout behaviorAfterOnUndeploymentTimeout = BehaviorAfterOnUnDeploymentTimeout.IGNORE;
    Long onUnDeploymentTimeout = null;
    if (task instanceof ExtendedDeploymentTask) {
      if (logger.isTraceEnabled()) {
        logger.trace("Found ExtendedDeploymentTask.");
      }
      ExtendedDeploymentTask extendedDeploymentTask = (ExtendedDeploymentTask) task;
      if (extendedDeploymentTask.getBehaviorAfterOnUnDeploymentTimeout() != null) {
        behaviorAfterOnUndeploymentTimeout = extendedDeploymentTask.getBehaviorAfterOnUnDeploymentTimeout();
      }
      if (extendedDeploymentTask.getOnUnDeploymentTimeout() != null) {
        onUnDeploymentTimeout = extendedDeploymentTask.getOnUnDeploymentTimeout();
      }
    }
    if (onUnDeploymentTimeout == null) {
      onUnDeploymentTimeout = XynaProperty.DEPLOYMENTHANDLER_TIMEOUT.getMillis();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Timeout for " + command + " handler is <" + onUnDeploymentTimeout + "> milliseconds. Behavior after timeout: "
          + behaviorAfterOnUndeploymentTimeout);
    }

    ExecutionWrapper<XynaException> unDeploymentRunnable =
        new ExecutionWrapper<XynaException>(command.name() + " of " + task.getClass().getName() ) {

          public void execute() throws XynaException {
            if (command == DeployCommand.UNDEPLOY) {
              task.onUndeployment();
            } else {
              task.onDeployment();
            }
          }

        };
    try {
      XynaExecutor.getInstance().executeRunnableWithUnDeploymentThreadpool(unDeploymentRunnable);
    } catch (RejectedExecutionException e) {
      String errorMsg = task + " can not be executed on " + command.name.toLowerCase() + ", because the threadpool is exhausted.";
      if (command == DeployCommand.DEPLOY) {
        throw new RuntimeException(errorMsg, e);
      } else {
        //kein fehler werfen, weil beim undeploment dann inkonsistenzen entstehen können
        logger.error(errorMsg, e);
        return;
      }
    }
    if (!unDeploymentRunnable.await(onUnDeploymentTimeout)) {
      // Timeout abgelaufen ... Interrupt senden
      if (logger.isDebugEnabled()) {
        logger.debug("Timeout of " + command + " reached for object " + task.getClass().getName() + ". Try to interrupt.");
      }
      Thread thread = unDeploymentRunnable.getThread();
      if(thread != null) {
        thread.interrupt();
        try {
          // 1 Sekunde warten, um dem Thread die Chance zu geben, sich zu beenden
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
    }
    if (unDeploymentRunnable.getThread() == null && unDeploymentRunnable.getXynaException() != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(command + " thread for object " + task.getClass().getName()
            + " throws exception.");
      }
      if (command == DeployCommand.DEPLOY) {
        throw new XDEV_UserDefinedDeploymentException(task.getClass().getName(),
                                                        unDeploymentRunnable.getXynaException());
      } else {
        throw new XDEV_UserDefinedUnDeploymentException(task.getClass().getName(),
                                                        unDeploymentRunnable.getXynaException());
      }
    }
    if (unDeploymentRunnable.getThread() != null) {
      // Thread scheinbar nicht fertig ... Interrupt hat auch nichts gebracht
      switch (behaviorAfterOnUndeploymentTimeout) {
        case EXCEPTION :          
          if (logger.isDebugEnabled()) {
            debug(unDeploymentRunnable, command + " thread for object " + task.getClass().getName()
                + " is hanging. Throwing an exception.");
          }
          throw new UnDeploymentTimeoutException();
        case KILLTHREAD :
          Thread thread = unDeploymentRunnable.getThread();
          if (thread != null) {
            // hoffen wir, dass wir das richtige Runnable töten und nicht ein anderen Undeployment-Handler
            // abschießen, weil unseres gerade fertig geworden ist?
            if (logger.isDebugEnabled()) {
              debug(unDeploymentRunnable, "Kill " + command + " thread for object " + task.getClass().getName()
                  + ". Continue the " + command + ".");
            }
            thread.stop();
          }
          break;
        case IGNORE :
          // ignorieren wir - weiter geht's ...
          if (logger.isDebugEnabled()) {
            debug(unDeploymentRunnable, command + " thread for object " + task.getClass().getName()
                + " is hanging. Continuing undeployment.");
          }
          break;
      }
    }
  }
  

  private void debug(ExecutionWrapper unDeploymentRunnable, String text) {
    Thread t = unDeploymentRunnable.getThread();
    if (t != null) {
      Exception e = new Exception("Hanging thread " + t.getName() + " is running after timeout.");
      e.setStackTrace(t.getStackTrace());
      logger.debug(text, e);
    } else {
      logger.debug(text + " Thread finished");
    }
  }


  private class UnDeploymentTimeoutException extends RuntimeException {    
  }
  
  protected enum DeployCommand {
    DEPLOY("deployment"), UNDEPLOY("undeployment");
    
    private String name;
    
    private DeployCommand(String name) {
      this.name = name;
    }
    
    public String toString() {
      return name;
    }
  }
  
  public static void deploy(Class<? extends XynaObject> cl) throws XDEV_UserDefinedDeploymentException,
      XDEV_CLASS_INSTANTIATION_PROBLEM {
    //new Instance, damit statische initialisierung des datentyps stattfindet und deploymenttask definiert werden kann
    if (!Modifier.isAbstract(cl.getModifiers())) {
      if (logger.isTraceEnabled()) {
        logger.trace("creating instance of " + cl.getName());
      }
      XynaObject xo;
      try {
        xo = cl.getConstructor().newInstance();
      } catch (Exception e) {
        throw new XDEV_CLASS_INSTANTIATION_PROBLEM(cl.getName(), e);
      } catch (Error e) {
        throw new XDEV_CLASS_INSTANTIATION_PROBLEM(cl.getName(), e);
      }
      try {
        xo.onDeployment();
      } catch(UnDeploymentTimeoutException e) {
        throw new XDEV_UserDefinedDeploymentException(cl.getName(), e);
      } catch (XynaException e) {
        if (e instanceof XDEV_UserDefinedDeploymentException) {
          throw (XDEV_UserDefinedDeploymentException) e;
        } else {
          throw new XDEV_UserDefinedDeploymentException(cl.getName(), e);
        }
      }
    }
    //abwärtskompatibilität. früher konnte man deploymenttasks bei xynaobject registrieren
    List<DeploymentTask> tasks = deploymentTasks.get(cl);
    if (tasks != null) {
      try {
        for (DeploymentTask t : tasks) {
          t.onDeployment();
        }
      } catch(XynaException e) {
        if (e instanceof XDEV_UserDefinedDeploymentException) {
          throw (XDEV_UserDefinedDeploymentException) e;
        } else {
          throw new XDEV_UserDefinedDeploymentException(cl.getName(), e);
        }
      }
    }
    
    //DeploymentListener benachrichtigen
    deploymentListenerManagement.notifyDeploy(cl);
  }

  public static class XMLHelper {
    public static void beginExceptionType(StringBuilder xml, String varName, String referenceName, String referencePath, long objectId, long refId) {
      beginExceptionType(xml, varName, referenceName, referencePath, objectId, refId, 0, null);
    }
    
    public static void beginExceptionType(StringBuilder xml, String varName, String referenceName, String referencePath, long objectId, long refId, long revision, XMLReferenceCache cache) {
      xml.append("<" + GenerationBase.EL.EXCEPTION + " ");
      if (varName != null) {
        xml.append(GenerationBase.ATT.VARIABLENAME + "=\"").append(varName).append("\" ");
      }
      if (objectIdIsSet(objectId)) {
        xml.append(GenerationBase.ATT.OBJECT_ID + "=\"").append(objectId).append("\" ");
      } else if (refId > 0) {
        xml.append(GenerationBase.ATT.OBJECT_REFERENCE_ID + "=\"").append(refId).append("\" ");
      }
      xml.append(GenerationBase.ATT.REFERENCEPATH + "=\"").append(referencePath);
      xml.append("\" " + GenerationBase.ATT.REFERENCENAME + "=\"").append(referenceName);
      xml.append("\" " + GenerationBase.ATT.ERROR_TYPE + "=\"" + OrderInstanceStatus.XYNA_ERROR.getName() + "\" >\n");
      
      //RuntimeContext falls nicht in Dependencies enthalten
      if (cache != null && refId < 0 && revision != 0) {
        RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (cache.getOwnerRevision() != revision && !rcdMgmt.isDependency(cache.getOwnerRevision(),revision)) {
          appendRuntimeContext(xml, revision);
        }
      }
    }
    
    private static boolean objectIdIsSet(long objectId) {
      return objectId > 0 && objectId < Long.MAX_VALUE / 2; //vgl GeneralXynaObject.XMLReferenceCache.getCacheObjectWithoutCaching
    }

    public static void beginType(StringBuilder xml, String varName, String referenceName, String referencePath) {
      beginType(xml, varName, referenceName, referencePath, -1, -1, 0, null);
    }

    public static void beginType(StringBuilder xml, String varName, String referenceName, String referencePath, long objectId, long refId) {
      beginType(xml, varName, referenceName, referencePath, objectId, refId, 0, null);
    }

    public static void beginType(StringBuilder xml, String varName, String referenceName, String referencePath, long objectId, long refId,
                                 long revision, XMLReferenceCache cache) {
      xml.append("<Data ");
      if (varName != null) {
        xml.append("VariableName=\"").append(varName).append("\" ");
      }
      if (objectIdIsSet(objectId)) {
        xml.append(GenerationBase.ATT.OBJECT_ID + "=\"").append(objectId).append("\" ");
      } else if (refId > 0) {
        xml.append(GenerationBase.ATT.OBJECT_REFERENCE_ID + "=\"").append(refId).append("\" ");
      }
      xml.append("ReferenceName=\"").append(referenceName);
      xml.append("\" ReferencePath=\"").append(referencePath).append("\" >\n");
      
      
      //RuntimeContext falls nicht in Dependencies enthalten
      if (cache != null && refId < 0 && revision != 0) {
        RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (cache.getOwnerRevision() != revision && !rcdMgmt.isDependency(cache.getOwnerRevision(),revision)) {
          appendRuntimeContext(xml, revision);
        }
      }
    }


    public static void endType(StringBuilder xml) {
      xml.append("</Data>\n");
    }


    public static void endExceptionType(StringBuilder xml) {
      xml.append("</" + GenerationBase.EL.EXCEPTION + ">\n");
    }


    public static void appendExceptionList(StringBuilder xml, String varName, String referenceName, String referencePath,
                                           List<? extends Throwable> values, long version, long revision, XMLReferenceCache cache) {
      xml.append("<" + GenerationBase.EL.EXCEPTION + " " + GenerationBase.ATT.VARIABLENAME + "=\"");
      xml.append(varName);
      xml.append("\" " + GenerationBase.ATT.REFERENCENAME + "=\"");
      xml.append(referenceName);
      xml.append("\" " + GenerationBase.ATT.REFERENCEPATH + "=\"");
      xml.append(referencePath);
      xml.append("\" " + GenerationBase.ATT.ISLIST + "=\"true\">\n");

      if (cache != null && revision != 0) {
        RuntimeContextDependencyManagement rcdMgmt =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (cache.getOwnerRevision() != revision && !rcdMgmt.isDependency(cache.getOwnerRevision(), revision)) {
          appendRuntimeContext(xml, revision);
        }
      }
      if (values != null) {
        for (Throwable e : values) {
          if (e == null) {
            xml.append("<" + GenerationBase.EL.VALUE + "/>\n");
          } else {
            xml.append("<" + GenerationBase.EL.VALUE + ">\n");
            appendException(xml, null, e, version, cache);
            xml.append("</" + GenerationBase.EL.VALUE + ">\n");
          }
        }
      }
      xml.append("</" + GenerationBase.EL.EXCEPTION + ">\n");
    }
    
    public static <T extends XynaObject> void appendDataList(StringBuilder xml, String variableName, String referenceName, String referencePath, List<? extends T> values) {
      appendDataList(xml, variableName, referenceName, referencePath, values, -1, 0, null);
    }

    public static <T extends XynaObject> void appendDataList(StringBuilder xml, String variableName, String referenceName, String referencePath, List<? extends T> values, long version, GeneralXynaObject.XMLReferenceCache cache) {
      appendDataList(xml, variableName, referenceName, referencePath, values, version, 0, cache);
    }    
    //GeneralXynaObject zulässig für AnyType-Listen
    public static <T extends GeneralXynaObject> void appendDataList(StringBuilder xml, String variableName, String referenceName, String referencePath, List<? extends T> values, long version, long revision, GeneralXynaObject.XMLReferenceCache cache) {
      xml.append("<Data VariableName=\"").append(variableName);
      xml.append("\" ReferenceName=\"").append(referenceName);
      xml.append("\" ReferencePath=\"").append(referencePath);
      if( values == null ) {
        xml.append("\" ").append(GenerationBase.ATT.ISNULL).append("=\"").append(GenerationBase.ATT.TRUE);
      }
      xml.append("\" ").append(GenerationBase.ATT.ISLIST).append("=\"").append(GenerationBase.ATT.TRUE).append("\" >\n");
      if (cache != null && revision != 0) {
        RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (cache.getOwnerRevision() != revision && !rcdMgmt.isDependency(cache.getOwnerRevision(), revision)) {
          appendRuntimeContext(xml, revision);
        }
      }
      if( values != null ) {
        for( T val : values ) {
          if( val == null ) {
            xml.append("<Value/>");
          } else {
            xml.append("<Value>");
            xml.append(val.toXml(null, false, version, cache));
            xml.append("</Value>");
          }
        }
      }
      xml.append("</Data>\n");
    }
    
    //valueclass ist immer class von typ, den wir als "simpletype" bezeichnen. int/string/etc
    public static <T> void appendDataList(StringBuilder xml, String variableName, List<? extends T> values, Class<T> valueClass) {
      xml.append("<Data VariableName=\"").append(variableName);
      if( values == null ) {
        xml.append("\" ").append(GenerationBase.ATT.ISNULL).append("=\"").append(GenerationBase.ATT.TRUE);
      }
      xml.append("\" ").append(GenerationBase.ATT.ISLIST).append("=\"").append(GenerationBase.ATT.TRUE).append("\" >\n<Meta><Type>").append(valueClass.getSimpleName()).append("</Type></Meta>");
      if( values != null ) {
        for( T val : values ) {
          if (val == null) {
            xml.append("<Value/>");
          } else {
            xml.append("<Value>");
            xml.append(XMLUtils.replaceControlAndInvalidChars(XMLUtils.escapeXMLValue(String.valueOf(val)), true));
            xml.append("</Value>\n");
          }
        }
      }
      xml.append("</Data>\n");
    }
    
    public static void appendException(StringBuilder xml, String varName, Throwable e, long version, XMLReferenceCache cache) {
      if (e != null) {
        if (e instanceof GeneralXynaObject) {
          xml.append(((GeneralXynaObject) e).toXml(varName, false, version, cache));
        } else if (e instanceof XynaException) {
          //spezialbehandlung für spezielle exceptions
          beginExceptionType(xml, varName, "XynaException", "core.exception", -1, -1, Integer.MAX_VALUE, cache);
          xml.append("<" + GenerationBase.EL.ERRORMESSAGE + ">");
          try {
            xml.append(e.getMessage());
          } catch (Throwable t) {
            logger.debug("Exception creating Exception Message.", t);
          }
          xml.append("</" + GenerationBase.EL.ERRORMESSAGE + ">\n</" + GenerationBase.EL.EXCEPTION + ">\n");
        } else {
          beginExceptionType(xml, varName, "Exception", "core.exception", -1, -1, Integer.MAX_VALUE, cache);
          xml.append("<" + GenerationBase.EL.ERRORMESSAGE + ">");
          try {
            xml.append(e.getMessage());
          } catch (Throwable t) {
            logger.debug("Exception creating Exception Message.", t);
          }
          xml.append("</" + GenerationBase.EL.ERRORMESSAGE + ">\n</" + GenerationBase.EL.EXCEPTION + ">\n");
        }
      }
    }
      
    public static void appendData(StringBuilder xml, String variableName, XynaObject value) {
      appendData(xml, variableName, value, -1, null);
    }

    public static void appendData(StringBuilder xml, String variableName, XynaObject value, long version, XMLReferenceCache cache) {
      if (value != null) {
        xml.append(value.toXml(variableName, false, version, cache)); //TODO schöner wäre es, den StringBuilder zu übergeben
      }
    }

    public static void appendData(StringBuilder xml, String variableName, Object value) {
      appendData(xml, variableName, value, -1, null);
    }
    
    public static void appendData(StringBuilder xml, String variableName, Object value, long version, XMLReferenceCache cache) {
      xml.append("<Data VariableName=\"").append(variableName).append("\">\n");
      if (value != null) {
        xml.append("<Value>");
        xml.append(XMLUtils.replaceControlAndInvalidChars(XMLUtils.escapeXMLValue(String.valueOf(value)), true));
        xml.append("</Value>\n");
      }
      xml.append("</Data>\n");
    }
    
    public static void appendData(StringBuilder xml, String variableName, int value) {
      appendData(xml, variableName, value, -1, null);
    }
    
    public static void appendData(StringBuilder xml, String variableName, int value, long version, XMLReferenceCache cache) {
      xml.append("<Data VariableName=\"").append(variableName).append("\">\n");
      xml.append("<Value>").append(value).append("</Value>\n");
      xml.append("</Data>\n");
    }
    
    public static void appendRuntimeContext(StringBuilder xml, Long revision) {
      if (!XynaFactory.isFactoryServer()) {
        return;
      }
      if (revision == Integer.MAX_VALUE) { //vergleiche RevisionManagement.getRevisionByClass
        //serverinternes objekt. TODO revision herausfinden, in der das objekt definiert ist!
        return;
      }
      
      try {
        RuntimeContext runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
        xml.append("<Meta>\n");
        if (runtimeContext instanceof Application) {
          xml.append("<Application>").append(runtimeContext.getName()).append("</Application>\n");
          xml.append("<Version>").append(((Application) runtimeContext).getVersionName()).append("</Version>\n");
        } else if (runtimeContext instanceof Workspace) {
          xml.append("<Workspace>").append(runtimeContext.getName()).append("</Workspace>\n");
        }
        xml.append("</Meta>\n");
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error("runtimeContext not found (revision " + revision + ")", e);
      }
    }

  }

  
  //Achtung: wird vom generierten Code aus aufgerufen
  @SuppressWarnings("unchecked")
  public static <T extends GeneralXynaObject> T clone(T xynaObject, boolean deep) {
    if (xynaObject == null) {
      return null;
    }
    
    if (deep) {
      return (T) xynaObject.clone(deep);
    } else {
      return xynaObject;
    }
  }
  
  //Achtung: wird vom generierten Code aus aufgerufen
  @SuppressWarnings("unchecked")
  public static <T> List<T> cloneList(List<T> list, Class<T> type, boolean isJavaBaseType, boolean deep) {
    if (list == null) {
      return null;
    }
    if (deep) {
      if (isJavaBaseType) {
        return new ArrayList<T>(list);
      } else {
        List<T> result = new ArrayList<T>();
        for (T el : list) {
          if (el == null) {
            result.add(null);
          } else if (el instanceof GeneralXynaObject) {
            result.add((T) (((GeneralXynaObject) el).clone(true)));
          } else {
            result.add(el); //kann das passieren? offenbar wissen wir dann nicht, wie man das objekt sinnvoll cloned
          }
        }
        return result;
      }
    } else {
      return new ArrayList<T>(list); //bei shallow clones werden nur die elemente shallow gecloned - nicht die liste an sich
    }
  }


  //TODO kann weg?
  public ObjectVersionBase createObjectVersion(long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
    //default-implementierung (schlechte performance) für nicht codegenerierte objekte
    return new ObjectVersionBase(this, version, changeSetsOfMembers) {

      @Override
      public int calcHashOfMembers(Stack<GeneralXynaObject> stack) {
        return -1;
      }

      @Override
      protected boolean memberEquals(ObjectVersionBase other) {
        return xo == other.xo;
      }
      
    };
  }

  public Set<String> getVariableNames() {
    return new HashSet<>();
  }
}
