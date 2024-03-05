 /*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.classloading;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exception.MultipleExceptionHandler;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;



public class ClassLoaderBase extends URLClassLoader {
  
  protected static Logger logger = CentralFactoryLogging.getLogger(ClassLoaderBase.class);

  private ClassLoaderBase[] parents;
  /**
   * liste von zusätzlichen parents, die nicht mit dem konstruktor, sondern zur laufzeit dynamisch belegt werden.
   * beispielsweise von persistencelayerclassloadern, die eine abhängigkeit auf storables erkennen, die
   * in einem service (mdmclassloader) definiert sind. diese abhängigkeit kann irgendwann aufhören, weil z.b. der
   * service undeployed wird. dann sollten auch die referenzen auf die classloader verschwinden wegen
   * oom-gefahr.<br>
   * die weakreferences gewährleisten das.
   */
  private WeakReference<ClassLoaderBase>[] weakReferencedParents;
  private final Object weakReferencedParentsLock = new Object();

  protected ClassLoaderType type;
  private final String id;
  
  private Long revision;
  protected boolean closed = false;
  private ClassLoaderClosedException closedException = null;
  private long creationTimeStamp = System.currentTimeMillis();


  protected ClassLoaderBase(ClassLoaderType type, String id, URL[] urls, ClassLoaderBase[] parents, Long revision) {
    super(urls, checkConstructorArgumentLength(parents)[0]);
    if (logger.isDebugEnabled()) {
      logger.debug(" got urls: ");
      urls = getURLs();
      for (URL u: urls) {
        logger.debug("- " + u);
      }
    }
    this.revision = revision;
    this.parents = parents;
    this.type = type;
    if (id == null) {
      throw new RuntimeException("classloader id may not be null.");
    }
    if (id.trim().isEmpty()) {
      logger.warn("id empty", new RuntimeException());
    }
    this.id = id;
    
    registerPhantomReference();
  }
  
  protected ClassLoaderBase(ClassLoaderType type, String id, URL[] urls, ClassLoader parent, Long revision) {
    super(urls, parent);
    parents =  new ClassLoaderBase[0];
    this.revision = revision;
    this.type = type;
    if (id == null) {
      throw new RuntimeException("classloader id may not be null.");
    }
    if (id.trim().isEmpty()) {
      logger.warn("id empty", new RuntimeException());
    }
    this.id = id;
    
    registerPhantomReference();
  }
  
  public void addWeakReferencedParentClassLoader(ClassLoaderBase cl) {
    synchronized (weakReferencedParentsLock) {
      if (weakReferencedParents == null) {
        weakReferencedParents = new WeakReference[] {new WeakReference<ClassLoaderBase>(cl)};
        return;
      }
      //check doubles and remove gc-ed
      List<WeakReference<ClassLoaderBase>> newWrs = new ArrayList<WeakReference<ClassLoaderBase>>();
      boolean substitutedNewCL = false;
      for (WeakReference<ClassLoaderBase> wr : weakReferencedParents) {
        ClassLoaderBase wrcl = wr.get();
        if (wrcl != null) {
          if (wrcl.getClass() == cl.getClass() && wrcl.id.equals(cl.id)) {
            newWrs.add(new WeakReference<ClassLoaderBase>(cl));
            substitutedNewCL = true;
          } else {
            newWrs.add(wr);
          }
        }
      }
      if (!substitutedNewCL) {
        newWrs.add(new WeakReference<ClassLoaderBase>(cl));
      }
      
      if (newWrs.size() == 0) {
        weakReferencedParents = null;
      } else {
        //create new array      
        weakReferencedParents = newWrs.toArray(new WeakReference[newWrs.size()]);
      }
    }
  }
  
  private void registerPhantomReference() {
    if (Constants.GENERATE_CLASSLOADER_PHANTOM_REFERENCES) {
      StringBuilder classLoaderInformation = new StringBuilder();
      classLoaderInformation.append(this.toString()).append(" [").append(type.toString()).append("] - ").append(id);
      ClassLoaderDispatcher.addPhantomReference(this, type, classLoaderInformation.toString());
    }
  }
  
  protected ClassLoaderBase[] getParents() {
    return parents;
  }


  private static ClassLoaderBase[] checkConstructorArgumentLength(ClassLoaderBase[] arg) {
    if (arg.length == 0)
      throw new IllegalArgumentException("No parent classloader specified while creating " + ClassLoaderBase.class
                      .getSimpleName());
    return arg;
  }
  
  //nur für debugging/analyse von geladenen klassen
  private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>(2, 0.75f, 2);
  
  protected void addPreviouslyLoadedClass(String name, Class<?> clazz) {
    if (loadedClasses.containsKey(name)) {
      logger.info("class loaded again: " + name + " with classloader " + this);
      if (loadedClasses.get(name) != clazz) {
        logger.warn("loaded class different than before: " + loadedClasses.get(name).getClassLoader() + ", now: " + clazz.getClassLoader());
      }
    } else {
      loadedClasses.put(name, clazz);
    }
  }
  
  public Class<?> getPreviouslyLoadedClass(String name) {
    return loadedClasses.get(name);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (logger.isTraceEnabled()) {
      logger.trace("findClass " + name + " by [type=" + getType() + ", id=" + id + ", rev=" + revision + "]");
    }
    try {
      return super.findClass(name);
    } catch (LinkageError e) {
      if (e.getMessage().contains("duplicate class definition")) {
        //passiert durch fehlende synchronisierung von loadclass. die ist wegen deadlock gefahr nicht eingebaut.
        //ein retry sollte immer ausreichen, weil die klasse offenbar ja bereits geladen wurde
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          throw new RuntimeException("Classloading of class " + name
              + " failed. First there was a duplicate class definition exception, but a retry could not find the class.", e);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Caught and handled duplicate class definition successfully: " + e.getMessage());
          if (logger.isTraceEnabled()) {
            logger.trace(null, e);
          }
        }
        return c;
      }
      throw e;
    } catch (IllegalArgumentException e) {
      if (name.contains(e.getMessage())) { //name ist fqclassname - message ist packagename, also teil davon
        //passiert durch fehlende synchronisierung, s.o.
        /*
         * beispiel: 
        at ----- Caused by java.lang.IllegalArgumentException cl.bugz18249. (depth: 1)
        at java.lang.ClassLoader.definePackage(ClassLoader.java:1451)
        at java.net.URLClassLoader.defineClass(URLClassLoader.java:251)
        at java.net.URLClassLoader.access$000(URLClassLoader.java:58)
        at java.net.URLClassLoader$1.run(URLClassLoader.java:197)
        at java.security.AccessController.doPrivileged(Native Method)
        at java.net.URLClassLoader.findClass(URLClassLoader.java:190)
        at com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.findClass(ClassLoaderBase.java:162)
        at com.gip.xyna.xfmg.xfctrl.classloading.WFClassLoader.loadClass(WFClassLoader.java:96)
        at com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher.loadWFClass(ClassLoaderDispatcher.java:307)
        at com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.ProcessManagement.instantiateWF(ProcessManagement.java:269)
         * 
         */
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          //könnte auch ein findClass auf eine andere klasse gewesen sein im gleichen package. dann ist es ok, wenn man die klasse noch nicht als "loaded" findet.
          //also nochmal versuchen
          c = findClass(name); 
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Caught and handled duplicate package exception successfully: " + e.getMessage());
          if (logger.isTraceEnabled()) {
            logger.trace(null, e);
          }
        }
        return c;
      }
      throw e;
    }
  }

  /**
   * Tries to load a class by first asking java, then asking its parents and
   * then looks for it itself.
   * 
   * Always tries to resolve the class by calling {@link #resolveClass(Class)}
   * 
   */
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, true);
  }

  /**
   * Tries to load a class by first asking java, then asking its parents and
   * then looks for it itself.
   * 
   * Can be configured to resolve the class by calling {@link #resolveClass(Class)}
   * 
   */
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if (c== null) {
      //erst bei parent schauen, dann bei sich selbst
      int idx = 0;
      ClassLoaderBase[] pars = getParents();
      while (c == null && idx < pars.length) {
        try {
          c = pars[idx].loadClass(name);
        } catch (ClassNotFoundException e) {
          //ignorieren
        }
        idx++;
      }
      if (c == null) {
        if (weakReferencedParents != null) {
          synchronized (weakReferencedParentsLock) {
            for (WeakReference<ClassLoaderBase> wr : weakReferencedParents) {
              ClassLoaderBase p = wr.get();
              if (p == null) {
                continue;
              }
              try {
                c = p.loadClass(name);
              } catch (ClassNotFoundException e) {
                //ignorieren
              }
              if (c != null) {
                break;
              }
            }
          }
        }
      }
      if (c== null) {
        checkClosed();
        try {
          c = findClass(name); //wirft classnotfoundexception
        }
        catch (ClassNotFoundException e) {
        }
        catch (NoClassDefFoundError e) {
        }
      }
      if (c == null) {
        c = super.loadClass(name, resolve);
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder(name).append(" was loaded by ").append(c.getClassLoader()).toString());
    }

    return c;

  }


  protected void throwClassNotFoundException(String name, Throwable cause) throws ClassNotFoundException {
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRuntimeContext(getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      rc = null;
    }
    StringBuilder msg = new StringBuilder();
    msg.append("Class ").append(name).append(" could not be loaded by classloader ");
    msg.append((!hasBeenDeployed() ? "(undeployed) " : "")).append(getClassLoaderID());
    msg.append(" [").append(rc).append("]. Classloader was created ");
    msg.append(Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(creationTimeStamp)));
    msg.append(".");
    Throwable tmp = cause;
    while (cause != null) {
      msg.append("\nCause: ").append(cause.getClass().getSimpleName()).append(" ").append(cause.getMessage());
      cause = cause.getCause();
    }
    ClassNotFoundException e = new ClassNotFoundException(msg.toString());
    if (tmp != null) {
      e.setStackTrace(tmp.getStackTrace());
    }
    throw e;
  }
  
  public void checkClosed() throws ClassNotFoundException {
    if (closed) {
      //wenn bereits geschlossen, nicht mehr auf die urls zugreifen versuchen
      SimpleDateFormat format = Constants.defaultUTCSimpleDateFormat();
      ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
      ClassLoaderBase clb = cld.findClassLoaderByType(getClassLoaderID(), revision, getParentRevision(), getType(), true);
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      String additionalInfo;
      if (clb == null) {
        additionalInfo = "There is no classloader reachable for this object from ";
        try {
          additionalInfo += rm.getRuntimeContext(revision).toString() + ".";
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          additionalInfo += "inactive revision " + revision + ".";
        }
      } else {
        additionalInfo = "There is a live classloader reachable for this object from ";
        try {
          additionalInfo += rm.getRuntimeContext(revision).toString() + ".";
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          additionalInfo += "inactive revision " + revision + ".";
        }
        Set<Long> revs = new HashSet<Long>();
        for (Long rev : rm.getAllRevisions()) {
          if (cld.getClassLoaderByType(getType(), getClassLoaderID(), rev, getParentRevision()) != null) {
            revs.add(rev);
          }
        }
        if (revs.size() > 0) {
          additionalInfo += " The classloader is available through these runtime contexts: ";
          for (Long rev : revs) {
            try {
              additionalInfo += rm.getRuntimeContext(rev).toString() + ", ";
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            }
          }
          additionalInfo = additionalInfo.substring(0, additionalInfo.length() - 2);
        }
      }
      throw new ClassNotFoundException(getType().name() + " Classloader " + (!hasBeenDeployed() ? "(undeployed)" : "") + id
          + " is already closed since " + format.format(new Date(closedException.closeTimeStamp)) + ". Classloader was created "
          + format.format(new Date(creationTimeStamp)) + ". " + additionalInfo, closedException);
    }
  }
  
  
  public boolean isClosed() {
    return closed;
  }


  public void addJarFile(String fullPath) throws Ex_FileAccessException {
    File f = new File(fullPath);
    URL url;
    try {
      url = f.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new Ex_FileAccessException(f.getPath(), e);
    }
    boolean found = false;
    for (URL u : getURLs()) {
      if (u.equals(url)) { //FIXME das macht dns lookup 
        found = true;
        break;
      }
    }
    if (!found) {
      if (!f.exists()) {
        throw new Ex_FileAccessException(f.getPath());
      }
      addURL(url);

      if (logger.isInfoEnabled()) {
        logger.info(new StringBuilder("Adding url for classloader ").append(this).append(": ").append(url).toString());
      }

    }

  }

  
  private ConcurrentMap<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> loadingClasses = new ConcurrentHashMap<>(3);
  
  static class ClassLoaderIdRevisionRef {
    protected final String classLoaderId;
    protected final Long revision;
    protected final Long parentRevision;
    
    /*
     * Die parentRevision ist in der Regel null. Nur FilterClassLoader mit dem ClassLoaderType == OutdatedFilter übergeben als parentRevision die
     * Revision des Triggers, da sich diese von der eigenen unterscheidet. Dies hängt mit dem dahinterliegenden Konzept für die Filterantwort 
     * RESPONSIBLE_BUT_TOO_NEW zusammen.
     */
    protected ClassLoaderIdRevisionRef(String classLoaderId, Long revision, Long parentRevision) {
      this.classLoaderId = classLoaderId;
      this.revision = revision;
      this.parentRevision = parentRevision;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((classLoaderId == null) ? 0 : classLoaderId.hashCode());
      result = prime * result + ((parentRevision == null) ? 0 : parentRevision.hashCode());
      result = prime * result + ((revision == null) ? 0 : revision.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ClassLoaderIdRevisionRef other = (ClassLoaderIdRevisionRef) obj;
      if (classLoaderId == null) {
        if (other.classLoaderId != null)
          return false;
      } else if (!classLoaderId.equals(other.classLoaderId))
        return false;
      if (parentRevision == null) {
        if (other.parentRevision != null)
          return false;
      } else if (!parentRevision.equals(other.parentRevision))
        return false;
      if (revision == null) {
        if (other.revision != null)
          return false;
      } else if (!revision.equals(other.revision))
        return false;
      return true;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(classLoaderId).append(", ").append(revision);
      if (parentRevision != null) {
        sb.append(", ").append(parentRevision);
      }
      return sb.toString();
    }
  }
  
  public enum ClassLoadingDependencySource {
    TriggerFilterAdditionalDependencies, Classloading, Backup, ClassloaderCreation, GenerationBase
  }

  /**
   * fügt eine classloader abhängigkeit hinzu. 
   * d.h. der classloader der übergebenen klasse
   * muss beim ändern des hiesigen classloaders auch reloaded werden. das ist genau andersrum als eine klassenabhängigkeit.
   * beispiel:
   * class1 benutzt class2 und class1 und class2 sollen unterschiedliche classloader haben (z.b. mdm objekte), 
   * dann delegiert der classloader von class1 das classloading von class2 an den classloader von class2.
   * d.h. falls class2 sich ändert, hat der classloader von class1 als der (evtl) initiierende classloader
   * des classloading-vorgangs von class2 class2 gecached.
   * d.h.: class 1 ändert sich =&gt; class2 classloader ist abhängig im sinne, dass der class2 classloader
   * erneuert werden muss.
   * =&gt; rekursiv müssen auch die von class2 abhängigen classloader erneuert werden.
   * 
   * siehe auch java vm spec:
   * http://java.sun.com/docs/books/jvms/second_edition/html/ConstantPool.doc.html
   */
  public void addDependencyToReloadIfThisClassLoaderIsRecreated(String classLoaderIdToBeReloaded, Long revision, ClassLoaderType type, ClassLoadingDependencySource source) {
    ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(classLoaderIdToBeReloaded, revision, null);
    addDependencyToReloadIfThisClassLoaderIsRecreated(clId, type, source);
  }
  
  private static final XynaPropertyBoolean useReducedClassLoadingDependencies = new XynaPropertyBoolean("xfmg.xfctrl.classloading.dependencies.types.reduce", true);


  public void addDependencyToReloadIfThisClassLoaderIsRecreated(ClassLoaderIdRevisionRef classloaderRef,
                                                                ClassLoaderType type, ClassLoadingDependencySource source) {
    switch (source) {
      case Backup :
      case Classloading :
      case GenerationBase :
        break;
      case ClassloaderCreation :
      case TriggerFilterAdditionalDependencies :
        if (useReducedClassLoadingDependencies.get()) {
          return;
        } else {
          break;
        }
      default :
        throw new RuntimeException();
    }
    if (type == getType() && classloaderRef.classLoaderId.equals(getClassLoaderID())) {
      return;
    }
    if (!loadingClasses.containsKey(type)) {
      loadingClasses.putIfAbsent(type, new ConcurrentHashMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>());
    }
    if (loadingClasses.get(type).putIfAbsent(classloaderRef, source) == null) {
      if (logger.isInfoEnabled()) {
        logger.info(new StringBuilder("Adding dependency: ").append(classloaderRef.classLoaderId)
            .append(" must be reloaded if ").append(this).append(" changes").toString());
      }
    }
  }


  /**
   * rekursives sammeln aller abhängigen classloader
   */
  private void collectDependencies(HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps) {
    for (ClassLoaderType clt : loadingClasses.keySet()) {
      for (ClassLoaderIdRevisionRef c : loadingClasses.get(clt).keySet()) {
        if (!deps.containsKey(clt)) {
          deps.put(clt, new HashSet<ClassLoaderIdRevisionRef>());
        }
        if (!deps.get(clt).contains(c)) {
          deps.get(clt).add(c);
          ClassLoaderBase xlc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                          .getClassLoaderDispatcher().getClassLoaderByType(clt, c.classLoaderId, c.revision, c.parentRevision);
          if (xlc != null) {
            xlc.collectDependencies(deps);
          }
        }
        // else schon erledigt
      }
    }
  }
  

  public interface ClassLoaderSwitcher {
    public void switchClassLoader() throws XynaException;
  }
  
  
  protected final static ClassLoaderSwitcher NOP_CLASSLOADERSWITCHER = new NOPClassLoaderSwitcher();
  
  protected static class NOPClassLoaderSwitcher implements ClassLoaderSwitcher {

    public void switchClassLoader() throws XynaException {
    }
    
  }
  
  
  protected void undeployDependencies() throws XFMG_ClassLoaderRedeploymentException, MultipleExceptions {
    HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps = new HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>>();
    HashSet<ClassLoaderIdRevisionRef> set = new HashSet<ClassLoaderIdRevisionRef>();
    set.add(new ClassLoaderIdRevisionRef(id, revision, getParentRevision()));
    deps.put(getType(), set);
    collectDependencies(deps);
    
    reload_undeploy(deps);
  }
  
  /**
   * tauscht alle classloaders der dependencies-hierarchie gegen neue aus
   */
  protected void recreateDependencies() {
    recreateDependencies(true);
  }


  public void recreateDependencies(boolean includeSelf) {
    HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps = new HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>>();
    HashSet<ClassLoaderIdRevisionRef> set = new HashSet<ClassLoaderIdRevisionRef>();
    if (includeSelf) {
      set.add(new ClassLoaderIdRevisionRef(id, revision, getParentRevision()));
    }
    deps.put(getType(), set);
    collectDependencies(deps);
    
    reload_switchClassLoaders(deps);
  }

  /**
   * @param mode Falls mode==backup, werden Fehler beim onDeployment als XFMG_ClassLoaderRedeploymentException gesammelt geworfen
   *   ansonsten werden sie sofort weiter geworfen (RuntimeExceptions/Errors)
   */
  protected void deployDependencies(DeploymentMode mode) throws XFMG_ClassLoaderRedeploymentException {
    HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps = new HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>>();
    HashSet<ClassLoaderIdRevisionRef> set = new HashSet<ClassLoaderIdRevisionRef>();
    set.add(new ClassLoaderIdRevisionRef(id, revision, getParentRevision()));
    deps.put(getType(), set);
    collectDependencies(deps);
    
    reload_deploy(deps, mode);
  }

  /**
   * Reloads all dependent class loaders including this.
   * <ul> 
   * <li>dependencies der alten classloader übernehmen</li>
   * <li>urls der alten classloader übernehmen</li>
   * <li>sonstige extraaufgaben erledigen, die sich beim classloaderwechsel eines objekts
   * ergeben</li>
   * </ul>
   */
  protected void reloadDependencies(ClassLoaderSwitcher cls) throws XFMG_ClassLoaderRedeploymentException, MultipleExceptions {
    HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps = new HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>>();
    HashSet<ClassLoaderIdRevisionRef> set = new HashSet<ClassLoaderIdRevisionRef>();
    set.add(new ClassLoaderIdRevisionRef(id, revision, getParentRevision()));
    deps.put(getType(), set);
    collectDependencies(deps);
    
    //erst undeployen, dann classloader austauschen, dann deployen.
    //weil: eine mischung dieser operationen kann zu problemen führen, weil im deployment und undeployment objekte instanziiert
    //werden, die ggfs auf andere objekte verweisen, die erst später reloaded werden.
    //z.b. wenn man erst objekt A vollständig redeployed, und danach dann objekt B, welches in A verwendet wird
    //vollständig redeployed, dann sind die verweise in A auf B nicht mehr gültig, da sie noch den alten
    //classloader verwenden.
    reload_undeploy(deps);
    try {
      /* erst hier darf der eigentliche classloader ausgetauscht werden, weil sonst beim undeploy der alte und der neue in der
       * classloaderhierarchie referenziert werden und es dadurch zu verifyerrors kommen kann.
       */
      cls.switchClassLoader(); 
    } catch (XynaException e) {
      throw new XFMG_ClassLoaderRedeploymentException(getType().toString(), id, e);
    }
    //TODO der classloader von this wird hier evtl ein zweites mal ausgetauscht. wäre eigtl nicht notwendig, weil es bereits passiert ist. schadet aber nichts.
    reload_switchClassLoaders(deps);   
    reload_deploy(deps, null);
  }


  //reihenfolge wichtig, weil zb erst workflow neu deployen erzeugt bereits mdm classloader. diese werden nachher nochmal
  //neu erzeugt, was zu classcastexceptions führt
  private static ClassLoaderType[] typesInOrderForReLoad = new ClassLoaderType[] {ClassLoaderType.SharedLib,
                  ClassLoaderType.MDM, ClassLoaderType.Exception, ClassLoaderType.Trigger, ClassLoaderType.WF,
                  ClassLoaderType.Filter, ClassLoaderType.OutdatedFilter};


  private void reload_undeploy(HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps)
      throws XFMG_ClassLoaderRedeploymentException, MultipleExceptions {
    MultipleExceptionHandler<XFMG_ClassLoaderRedeploymentException> h = new MultipleExceptionHandler<>();
    for (ClassLoaderType clt : typesInOrderForReLoad) {
      if (!deps.containsKey(clt)) {
        continue;
      }
      for (ClassLoaderIdRevisionRef c : deps.get(clt)) {
        ClassLoaderBase clb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .getClassLoaderByType(clt, c.classLoaderId, c.revision, c.parentRevision);
        if (clb != null && clb.hasBeenDeployed()) {
          try {
            clb.undeployWhenReload(c.classLoaderId);
          } catch (XFMG_ClassLoaderRedeploymentException e) {
            h.addException(e);
          } catch (RuntimeException e) {
            h.addRuntimeException(e);
          } catch (Error e) {
            h.addError(e);
          }
          clb.setHasBeenUndeployed();
        }
      }
    }
    h.rethrow();
  }


  protected void undeployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {
    //overridden/extended if necessary
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getAutomaticUnDeploymentHandlerManager()
                    .notifyUndeployment(c, revision);
  }

  protected void deployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {
    //overridden/extended if necessary
    
  } 
  
  private void reload_switchClassLoaders(HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps) {
    for (ClassLoaderType clt : typesInOrderForReLoad) {
      if (!deps.containsKey(clt)) {
        continue;
      }
      for (ClassLoaderIdRevisionRef c : deps.get(clt)) {
        try {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                          .reloadClassLoaderByType(clt, c.classLoaderId, c.revision, c.parentRevision);
        } catch (XFMG_ClassLoaderNotFoundException e) {
          /*
           * passiert, wenn objekte nicht deployed wurden konnten.
           * normalerweise sollten diese dann undeployed werden. seit dem deploymentstatus feature wird aber bevorzugt, dass
           * objekte in einem invaliden status deployed sind. dann gibt es aber unter umständen (bei datentypen/exceptions) keinen
           * classloader.
           */
          if (logger.isDebugEnabled()) {
            logger.debug("classloader " + c.classLoaderId + " in revision " + c.revision + " not found", e);
          }
        }
      }
    }
  }
  
  protected void clearDependencies() {
    loadingClasses.clear();
  }


  private void reload_deploy(HashMap<ClassLoaderType, HashSet<ClassLoaderIdRevisionRef>> deps, DeploymentMode mode) throws XFMG_ClassLoaderRedeploymentException {
    for (ClassLoaderType clt : typesInOrderForReLoad) {
      if (!deps.containsKey(clt)) {
        continue;
      }
      MultipleExceptionHandler<XFMG_ClassLoaderRedeploymentException> h = new MultipleExceptionHandler<>();
      for (ClassLoaderIdRevisionRef c : deps.get(clt)) {
        ClassLoaderBase clb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getClassLoaderDispatcher().getClassLoaderByType(clt, c.classLoaderId, c.revision, c.parentRevision);
        if (clb != null && !clb.hasBeenDeployed()) {
          try {
            clb.deployWhenReload(c.classLoaderId);
            clb.setHasBeenDeployed();
          } catch (XFMG_ClassLoaderRedeploymentException | Error | RuntimeException t) {
            if (mode == DeploymentMode.deployBackup) {
              Department.handleThrowable(t);
              if (t instanceof Error) {
                h.addError((Error) t);
              } else if (t instanceof RuntimeException) {
                h.addRuntimeException((RuntimeException) t);
              } else {
                h.addException((XFMG_ClassLoaderRedeploymentException) t);
              }
            } else {
              throw t;
            } 
          }
        }
      }
      try {
        h.rethrow();
      } catch (XFMG_ClassLoaderRedeploymentException e) {
        throw e;
      } catch (Throwable e) {
        throw new XFMG_ClassLoaderRedeploymentException(type.name(), getClassLoaderID(), e);
      }
    }    
  }
  
  protected ConcurrentMap<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> getClassLoadersToReloadIfThisClassLoaderIsRecreated() {
    return loadingClasses;
  }

  public Long getParentRevision() {
    // if this is overridden by something other than FilterClassLoader, the implementation if SerializableClassLoadedObject
    // has to be modified
    return null;
  }
  
  
  protected void removeDependencyToReloadIfThisClassLoaderIsRecreated(String loadedClass, Long revision, ClassLoaderType type) {
    ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(loadedClass, revision, null);
    removeDependencyToReloadIfThisClassLoaderIsRecreated(clId, type);
  }
  
  protected void removeDependencyToReloadIfThisClassLoaderIsRecreated(ClassLoaderIdRevisionRef clId, ClassLoaderType type) {
    ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> map = loadingClasses.get(type);
    if (map == null) {
      return;
    }
    if (map.remove(clId) != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(new StringBuilder("Removing dependency: ").append(clId.classLoaderId)
                          .append(" shouldn't be reloaded any more if ").append(this).append(" changes").toString());
        }
        if (map.size() == 0) {
          loadingClasses.remove(type);
        }
    }
  }
  

  protected void addURLs(URL[] urls) {
    for (URL u: urls) {
      if (!Arrays.asList(getURLs()).contains(u)) {
        addURL(u);
      }
    }
  }

  public final ClassLoaderType getType() {
    return type;
  }

  public String getClassLoaderID() {
    return id;
  }

  private boolean hasBeenDeployed = false; //wurde das "onDeploy" auf dem Classloader bereits aufgerufen? d.h. ist der classloader ordentlich initialisiert.
  private boolean hasBeenUsed = false;
  
  /**
   * gibt das hasBeenDeployed flag zurück (default = false)
   */
  public boolean hasBeenDeployed() {
    return hasBeenDeployed;
  }

  /**
   * setzt das hasBeenDeployed flag auf true
   */
  public void setHasBeenDeployed() {
    hasBeenDeployed = true;
  }


  public void setHasBeenUndeployed() {
    hasBeenDeployed = false;
  }


  public boolean hasBeenUsed() {
    return hasBeenUsed;
  }
  
  protected void setHasBeenUsed() {
    hasBeenUsed = true;
  }

  
  public Long getRevision() {
    return revision;
  }


  public void setRevision(Long revision) {
    this.revision = revision;
  }


  private static ConcurrentMap<Long, ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>>> backuppedDeps =
         new ConcurrentHashMap<Long, ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>>>();



  /**
   * backupped dependencies wieder hinzufügen, falls vorhanden
   * @return restore hat auch tatsächlich was gemacht
   */
  public boolean restoreBackuppedClassloadingDependencies() {
    if (logger.isTraceEnabled()) {
      logger.trace("restoring backupped deps.");
      traceBackuppedClassloadingDeps(revision, id);
    }
    ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> revMap =
        backuppedDeps.get(revision);
    boolean backupExisted = false;
    if (revMap != null) {
      ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> map = revMap.remove(id);
      if (map != null) {
        for (Entry<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> entry : map.entrySet()) {
          for (ClassLoaderIdRevisionRef ref : entry.getValue()) {
            addDependencyToReloadIfThisClassLoaderIsRecreated(ref, entry.getKey(), ClassLoadingDependencySource.Backup);
            backupExisted = true;
          }
        }
      }
    }
    return backupExisted;
  }



  public void backupClassloadingDependencies() {
    if (logger.isTraceEnabled()) {
      logger.trace("backupping classloading deps after deployment error for " + id + ", rev=" + revision);
      traceBackuppedClassloadingDeps(revision, id);
    }
    ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> map =
        backuppedDeps.get(revision);
    if (map == null) {
      backuppedDeps
          .putIfAbsent(revision,
                       new ConcurrentHashMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>>());
    }
    ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> backup = new ConcurrentHashMap<>();
    for (Entry<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> e : loadingClasses.entrySet()) {
      backup.put(e.getKey(), new HashSet<ClassLoaderIdRevisionRef>(e.getValue().keySet()));
    }
    backuppedDeps.get(revision).put(id, backup);
  }


  public static void removeFromBackuppedDependencies(String fqClassName, ClassLoaderType type, Long revision) {
    if (logger.isTraceEnabled()) {
      logger.trace("removing from backupped dependencies: " + fqClassName + ", " + type + ", " + revision);      
    }
    ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> map =
        backuppedDeps.get(revision);
    if (map != null) {
      for (ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> backuppedLoadingClasses : map.values()) {
        Set<ClassLoaderIdRevisionRef> set = backuppedLoadingClasses.get(type);
        if (set != null) {
          Iterator<ClassLoaderIdRevisionRef> it = set.iterator();
          while (it.hasNext()) {
            ClassLoaderIdRevisionRef next = it.next();
            if (next.classLoaderId.equals(fqClassName)) {
              it.remove();
              break;
            }
          }
          //TODO aufräumen, wenn map leer ist
        }
      }
    }
  }
  
  //entferne alle backup-deps von sourceRev->targetRev
  public static void cleanupBackuppedDependencies(long sourceRevision, Set<Long> targetRevisions) {
    if (logger.isTraceEnabled()) {
      logger.trace("removing dependencies between " + sourceRevision + " and " + targetRevisions);      
    }
    ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> subMap = backuppedDeps.get(sourceRevision);
    if (subMap == null) {
      return;
    }
    for (Entry<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> entry : subMap.entrySet()) {
      ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> depsPerType = entry.getValue();
      for (Entry<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> e : depsPerType.entrySet()) {
        Set<ClassLoaderIdRevisionRef> deps = new HashSet<>(e.getValue());
        for (ClassLoaderIdRevisionRef id : deps) {
          if (targetRevisions.contains(id.revision)) {
            e.getValue().remove(id);
          }
        }
        if (e.getValue().isEmpty()) {
          depsPerType.remove(e.getKey());
        }
      }
      if (depsPerType.isEmpty()) {
        subMap.remove(entry.getKey());
      }
    } 
  }
  
  public static void cleanupBackuppedDependencies(Long revision) {
    if (logger.isTraceEnabled()) {
      logger.trace("clearing all backupped deps for rev " + revision);
    }
    backuppedDeps.remove(revision);
  }

  private static void traceBackuppedClassloadingDeps(Long revision, String id) {
    ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> subMap = backuppedDeps.get(revision);
    if (subMap == null) {
      return;
    }
    ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> subMapForName = subMap.get(id);
    if (subMapForName == null) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("backuppedDeps for " + id + " (" + revision + "):\n");
    traceBackuppedClassloadingDeps(sb, subMapForName);
    logger.trace(sb.toString());
  }

  private static void traceBackuppedClassloadingDeps(StringBuilder sb,
                                                     ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> subMapForName) {
    for (Entry<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> entryType : subMapForName.entrySet()) {
      sb.append("     of type=").append(entryType.getKey()).append(":\n");
      for (ClassLoaderIdRevisionRef clRef : entryType.getValue()) {
        sb.append("       - ").append(clRef.classLoaderId).append("\n");
      }
    }
  }

  public static void traceBackuppedDependencies(StringBuilder buf) {
    if (backuppedDeps.size() > 0) {
      buf.append("backuppedDependencies:\n");
    }
    for (Entry<Long, ConcurrentMap<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>>> entryRev : backuppedDeps
        .entrySet()) {
      buf.append(" in rev=").append(entryRev.getKey()).append("\n");
      for (Entry<String, ConcurrentMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>> entryClassLoader : entryRev
          .getValue().entrySet()) {
        buf.append("   for classLoader=").append(entryClassLoader.getKey()).append("\n");
        traceBackuppedClassloadingDeps(buf, entryClassLoader.getValue());
      }
    }
  }

  /**
   * ClassLoader kann fqClassNames registrieren.
   * Damit kann beispielsweise in der Implementation erreicht werden, 
   * dass der ClassLoader eine bestimmte Klasse selbst lädt anstatt seine Parents danach zu fragen.
   *
   */
  public static interface RegisteringClassLoader {

    void register(String fqClassName);
    
  }
  
  /**
   * ClassLoader kann ersetzt werden durch neuen ClassLoader, 
   * falls Parent-ClassLoader erneuert werden müssen
   *
   */
  public static interface ReplaceableClassLoader {

    ClassLoaderBase replace();
    
  }
  

  private static class ClassLoaderClosedException extends Exception {

    private static final long serialVersionUID = 1L;
    Long closeTimeStamp;

    public ClassLoaderClosedException() {
      super("classloader closed by " + Thread.currentThread().getName());
      closeTimeStamp = System.currentTimeMillis();
    }
  }

  public void closeFiles() {
    closed = true;
    closedException = new ClassLoaderClosedException();
    //vgl auch http://management-platform.blogspot.de/2009/01/classloaders-keeping-jar-files-open.html
    //oder http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5041014
    
    try {
      close();
    } catch (IOException t) {
      logger.trace("could not close classloader", t);
    }
  }

  public boolean hasDependencies() {
    return loadingClasses.size() > 0;
  }

  /**
   * schneidet innere klassen etc ab. beispiel: a.b.c.d.E$F$G -&gt; a.b.c.d.E
   * @param name
   * @return
   */
  public static String getBaseClassName(String name) {
    int idx = name.indexOf('$');
    if (idx > -1) {
      return name.substring(0, idx);
    } else {
      return name;
    }
  }

  public String getExtendedDescription(boolean includingClosedException) {
    SimpleDateFormat format = Constants.defaultUTCSimpleDateFormatWithMS();
    String result = System.identityHashCode(this) + " " + type + " " + (!hasBeenDeployed() ? "(undeployed)" : "") + id + " rev=" + revision + " created=" + format.format(new Date(creationTimeStamp));
    if (closed) {
      result += " closed=" + format.format(new Date(closedException.closeTimeStamp));
      if (includingClosedException) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        closedException.printStackTrace(pw);
        result += "\n" + sw.toString();
      }
    }
    return result;
  }
  
  
  public Class<?> findLoadedClass2(String fqClassName) {
    return findLoadedClass(fqClassName);
  }

  public void debugClassLoadersToReload(CommandLineWriter clw) {
    StringBuilder buf = new StringBuilder();
    traceClassLoadingDependencies(buf);
    clw.writeLineToCommandLine(buf);
  }

  private static final Comparator<Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> COMPARATOR_CLASSLOADERID = new Comparator<Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>>() {

    @Override
    public int compare(Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> o1, Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> o2) {
      //erst name, dann revision
      int c = o1.getKey().classLoaderId.compareTo(o2.getKey().classLoaderId);
      if (c == 0) {
        c = Long.compare(o1.getKey().revision, o2.getKey().revision);
        return c;
      }
      return c;
    }
    
  };
  
  
  protected void traceClassLoadingDependencies(StringBuilder buf) {
    buf.append("   + objects to reload if this changes:\n");
    for (ClassLoaderType clt : getClassLoadersToReloadIfThisClassLoaderIsRecreated().keySet()) {
      buf.append("     - ").append(clt).append(":\n");
      List<Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> deps = new ArrayList<>(getClassLoadersToReloadIfThisClassLoaderIsRecreated().get(clt).entrySet());
      Collections.sort(deps, COMPARATOR_CLASSLOADERID);
      for (Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> e : deps) {
        ClassLoaderIdRevisionRef dep = e.getKey();
        buf.append("       * ").append(dep.classLoaderId);
        boolean revprinted = false;
        if (!dep.revision.equals(getRevision())) {
          buf.append(" revision=\"").append(dep.revision).append("\"");
          revprinted = true;
        } 
        if (dep.parentRevision != null) {
          if (!revprinted) {
            buf.append(" revision=\"").append(dep.revision).append("\"");
          }
          buf.append(" parentRevision=\"").append(dep.parentRevision).append("\"");
        }
        buf.append(" [").append(e.getValue()).append("]");
        buf.append("\n");
      }
    }
  }

  protected void copyDependencies(ClassLoaderBase oldCL, boolean ignoreSharedLibDependencies) {
    if (ignoreSharedLibDependencies) {
      Map<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> deps =
          oldCL.getClassLoadersToReloadIfThisClassLoaderIsRecreated();
      for (Entry<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> entry : deps.entrySet()) {
        ClassLoaderType clt = entry.getKey();
        if (clt != ClassLoaderType.SharedLib) {
          for (Entry<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> e : entry.getValue().entrySet()) {
            addDependencyToReloadIfThisClassLoaderIsRecreated(e.getKey(), clt, e.getValue());
          }
        }
      }
    } else {
      loadingClasses = oldCL.loadingClasses;
    }
  }
  

}
