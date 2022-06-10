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

package com.gip.xyna.xdev.xfractmod.xmdm;



import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoaderXMLBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public interface GeneralXynaObject extends Serializable {
  
  public String toXml();


  public String toXml(String varName);


  public String toXml(String varName, boolean onlyContent);

  
  public static class XMLReferenceCache {
    
    private static final Logger logger = CentralFactoryLogging.getLogger(XMLReferenceCache.class);
    
    private static RuntimeContextDependencyManagement rcdm;
    private static DependencyRegister dr;

    public final IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers;
    private final ConcurrentMap<ObjectVersionBase, Long> ids;
    private long currentId;
    
    private final Set<DependencyNode> auditImports;
    
    private final long ownerRevision;
    
    public XMLReferenceCache(long ownerRevision) {
      this.ownerRevision = ownerRevision;
      changeSetsOfMembers = new IdentityHashMap<GeneralXynaObject, DataRangeCollection>();
      ids = new ConcurrentHashMap<ObjectVersionBase, Long>();
      auditImports = Collections.newSetFromMap(new ConcurrentHashMap<DependencyNode, Boolean>());
    }
    
    private XMLReferenceCache(long ownerRevision, String x) {
      this.ownerRevision = ownerRevision;
      changeSetsOfMembers = null;
      ids = null;
      auditImports = null;
      currentId = Long.MAX_VALUE / 2;
    }
    
    /*
     * es werden immer negative ids zurückgegeben, die höher als LONG.MAX_VALUE/2 sind.
     * d.h. ea funktioniert so, als wäre jedes übergebene object anders/neu
     */
    public static XMLReferenceCache getCacheObjectWithoutCaching(long ownerRevision) {
      return new XMLReferenceCache(ownerRevision, "");
    }

    /**
     * checkt, ob version bereits eine id zugeordnet hat. falls ja, wird die version zurückgegeben. falls nein, wird die negative neue versionsnummer zurückgegeben
     */
    //vorteil: hinzufügen und gleichzeitiger check auf gleichheit (teures equal+hashcode) muss so nur einmal durchgeführt werden
    public long putIfAbsent(ObjectVersionBase version) {
      long id = ++currentId;
      if (ids == null) {
        return -id;
      }
      Long previousValue = ids.putIfAbsent(version, id);
      if (previousValue != null) {
        if (logger.isTraceEnabled()) {
          logger.trace(version.xo + " gets previous id=" + previousValue);
        }
        currentId --;
        return previousValue;
      } else {
        //das Objekt soll als Import ins Audit aufgenommen werden
        addAuditImport(version.xo);
        
        if (logger.isTraceEnabled()) {
          logger.trace(version.xo + " gets new id=" + id);
        }
        return -id;
      }
    }
    

    private void addAuditImport(GeneralXynaObject xo) {
      Class<? extends GeneralXynaObject> xoClass = xo.getClass();

      DependencySourceType type = null;

      if (xo instanceof XynaObject) {
        type = DependencySourceType.DATATYPE;
      } else if (xo instanceof Throwable) {
        type = DependencySourceType.XYNAEXCEPTION;
      }
      if (dr == null) {
        dr = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
      }
      DependencyNode node = null;
      if (xoClass.getClassLoader() instanceof MDMClassLoaderXMLBase) {
        MDMClassLoaderXMLBase classLoader = ((MDMClassLoaderXMLBase) xoClass.getClassLoader());
        String fullXmlName = classLoader.getOriginalXmlPath() + "." + classLoader.getOriginalXmlName();
        node = dr.getDependencyNode(fullXmlName, type, classLoader.getRevision());
      } else if (GenerationBase.isReservedServerObjectByFqClassName(xoClass.getName())) {
        if (rcdm == null) {
          rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        }
        String xmlName = GenerationBase.getXmlNameForReservedClass(xoClass);
        node = dr.getDependencyNode(xmlName, type, rcdm.getRevisionDefiningXMOMObjectOrParent(xmlName, ownerRevision));
      } else if (xo instanceof GeneralXynaObjectList) {
        GeneralXynaObjectList<?> xol = (GeneralXynaObjectList<?>) xo;
        //wenn die liste mindestens ein element hat, braucht man hier nichts tun, weil dann das element den typ der liste explizit oder implizit referenziert
        if (xol.size() == 0) {
          String xmlName = xol.getContainedFQTypeName();
          if (rcdm == null) {
            rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
          }
          long rev;
          if (xol.getContainedClass() != null && xol.getContainedClass().getClassLoader() instanceof ClassLoaderBase) {
            rev = ((ClassLoaderBase) xol.getContainedClass().getClassLoader()).getRevision();
          } else {
            rev = rcdm.getRevisionDefiningXMOMObjectOrParent(xmlName, ownerRevision);
          }
          type = DependencySourceType.DATATYPE;
          node = dr.getDependencyNode(xmlName, type, rev);
          if (node == null) {
            type = DependencySourceType.XYNAEXCEPTION;
            node = dr.getDependencyNode(xmlName, type, rev);
          }
        }
      }

      if (node != null) {
        auditImports.add(node);
      }
      //keine rekursion notwendig, weil für die kindobjekte diese methode ebenso aufgerufen wird bei toXml()
    }


    public Set<DependencyNode> getAuditImports() {
      return Collections.unmodifiableSet(auditImports);
    }
    
    
    public long getOwnerRevision() {
      return ownerRevision;
    }
  }
  
  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache);
  

  public GeneralXynaObject clone();

  public GeneralXynaObject clone(boolean deep);
  

  public boolean supportsObjectVersioning();

  public ObjectVersionBase createObjectVersion(long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers);
  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                                Set<Long> datapoints);
  

  /**
   * Liefert die Membervariable die durch den Pfad beschrieben wird.
   * @param path name einer Membervariable oder pfad (separiert durch ".")
   * @throws InvalidObjectPathException falls path nicht auf die Membervariablen des Datentyps passt
   */
  public Object get(String path) throws InvalidObjectPathException;


  /**
   * MemberVariable per Variablennamen setzen
   */
  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND;


  
}
