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
package com.gip.xyna.update;



import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater.ApplicationUpdate;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDomDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDomOrExceptionDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMExceptionDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMWorkflowDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



/*
 * durch die modularisierungsumstellung werden xmls von reserved objects nun ganz normal behandelt und deshalb z.b. auch in applications erwartet.
 * 
 * in bestehenden applications fehlen die xmls aber, weil sie bisher immer über das default-workingset aufgelöst worden sind.
 * deshalb werden sie nun an die fehlenden stellen kopiert.
 */
public class UpdateApplicationsReservedServerObjects extends UpdateJustVersion implements ApplicationUpdate {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateApplicationsReservedServerObjects.class);


  public UpdateApplicationsReservedServerObjects(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion, false);
    Updater.getInstance().addApplicationUpdate(this);
  }


  @Override
  protected void update() throws XynaException {
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      // factory ist noch nicht initialisiert
      UpdateGeneratedClasses.mockFactory();
      ODSImpl ods = ODSImpl.getInstance();

      XMOMDatabase.getXMOMDatabasePreInit(ods, "update"); //registriert storables etc

      //abhängigkeiten von reservierten objekten auf andere bestimmen
      for (long r : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getAllWorkspaceRevisions()) {
        analyseReservedObjects(r, ODSConnectionType.HISTORY);
      }

      /*
       * für alle revisions:
       *   suche verwendungen von reserved server objects.
       *   falls gefunden, füge reserved server objects zu revision hinzu
       */
      for (long r : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getAllApplicationRevisions()) {
        updateRevision(r);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


  private void analyseReservedObjects(long revision, ODSConnectionType type) throws XynaException {
    ODSImpl ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(type);
    try {
      Collection<XMOMDomDatabaseEntry> coll = con.loadCollection(XMOMDomDatabaseEntry.class);
      Map<String, XMOMDomOrExceptionDatabaseEntry> mapOfReservedObjects = new HashMap<String, XMOMDomOrExceptionDatabaseEntry>();
      for (XMOMDomDatabaseEntry e : coll) {
        if (e.getRevision() == revision) {
          if (isReserved(e.getFqname())) {
            mapOfReservedObjects.put(e.getFqname(), e);
          }
        }
      }
      Collection<XMOMExceptionDatabaseEntry> collEx = con.loadCollection(XMOMExceptionDatabaseEntry.class);
      for (XMOMExceptionDatabaseEntry e : collEx) {
        if (e.getRevision() == revision) {
          if (isReserved(e.getFqname())) {
            mapOfReservedObjects.put(e.getFqname(), e);
          }
        }
      }

      for (XMOMDomOrExceptionDatabaseEntry e : mapOfReservedObjects.values()) {
        findDepsToOtherReservedObjects(e);
      }
    } finally {
      con.closeConnection();
    }
  }


  private Map<String, Set<String>> reservedDeps = new HashMap<String, Set<String>>();


  private void findDepsToOtherReservedObjects(XMOMDomOrExceptionDatabaseEntry reserved) {
    Set<String> deps = reservedDeps.get(reserved.getFqname());
    if (deps == null) {
      deps = new HashSet<String>();  
      reservedDeps.put(reserved.getFqname(), deps);
    }
    deps.addAll(getUsedReservedObjects(reserved.getExtends()));
    deps.addAll(getUsedReservedObjects(reserved.getPossesses()));
  }


  private void updateRevision(long revision) throws XynaException {
    ODSImpl ods = ODSImpl.getInstance();

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<XMOMDomDatabaseEntry> coll = con.loadCollection(XMOMDomDatabaseEntry.class);
      Map<String, XMOMDomOrExceptionDatabaseEntry> mapOfAllObjects = new HashMap<String, XMOMDomOrExceptionDatabaseEntry>();
      for (XMOMDomDatabaseEntry e : coll) {
        if (e.getRevision() == revision) {
          mapOfAllObjects.put(e.getFqname(), e);
        }
      }
      Collection<XMOMExceptionDatabaseEntry> collEx = con.loadCollection(XMOMExceptionDatabaseEntry.class);
      for (XMOMExceptionDatabaseEntry e : collEx) {
        if (e.getRevision() == revision) {
          mapOfAllObjects.put(e.getFqname(), e);
        }
      }

      Set<String> reservedObjects = new HashSet<String>();
      //die exceptions werden immer benötigt, weil sie von der infrastruktur geworfen werden können und dadurch in audits auch ohne explizite nutzung auftauchen können
      reservedObjects.add(GenerationBase.CORE_EXCEPTION);
      reservedObjects.add(GenerationBase.CORE_XYNAEXCEPTION);
      for (XMOMDomOrExceptionDatabaseEntry e : mapOfAllObjects.values()) {
        addUsedReservedObjects(e, reservedObjects);
      }
      Collection<XMOMWorkflowDatabaseEntry> collWf = con.loadCollection(XMOMWorkflowDatabaseEntry.class);
      for (XMOMWorkflowDatabaseEntry e : collWf) {
        if (e.getRevision() == revision) {
          addUsedReservedObjects(e, reservedObjects);
        }
      }

      //nun noch reservierte objekte hinzufügen, die nicht in der xmomdb stehen und nicht direkt referenziert werden, aber indirekt referenzeirt werden.      
      addIndirectlyReferencedReservedObjects(reservedObjects);
      for (String usedReserved : reservedObjects) {
        copyReservedToRevision(usedReserved, revision);
      }

    } finally {
      con.closeConnection();
    }
  }


  private void addUsedReservedObjects(XMOMWorkflowDatabaseEntry e, Set<String> reservedObjects) {
    reservedObjects.addAll(getUsedReservedObjects(e.getExceptions()));
    reservedObjects.addAll(getUsedReservedObjects(e.getImplUses()));
    reservedObjects.addAll(getUsedReservedObjects(e.getNeeds()));
    reservedObjects.addAll(getUsedReservedObjects(e.getProduces()));
    reservedObjects.addAll(getUsedReservedObjects(e.getUsesInstancesOf()));
  }


  private void addIndirectlyReferencedReservedObjects(Set<String> reservedObjects) {
    boolean foundAdditionalDeps = true;
    while (foundAdditionalDeps) {
      foundAdditionalDeps = false;
      Set<String> localCopy = new HashSet<String>(reservedObjects);
      for (String s : localCopy) {
        Set<String> deps = reservedDeps.get(s);
        if (deps != null) {
          for (String d : deps) {
            foundAdditionalDeps |= reservedObjects.add(d);
          }
        }
      }
    }
  }


  private void addUsedReservedObjects(XMOMDomOrExceptionDatabaseEntry e, Set<String> reservedObjects) {
    reservedObjects.addAll(getUsedReservedObjects(e.getExtends()));
    reservedObjects.addAll(getUsedReservedObjects(e.getPossesses()));
  }


  private static final Pattern p = Pattern.compile(",");


  private Set<String> getUsedReservedObjects(String commaseparatedList) {
    if (commaseparatedList == null) {
      return Collections.emptySet();
    }
    if (commaseparatedList.length() == 0) {
      return Collections.emptySet();
    }
    String[] parts = p.split(commaseparatedList);
    Set<String> s = new HashSet<String>();
    for (String name : parts) {
      if (isReserved(name)) {
        s.add(name);
      }
    }
    return s;
  }


  public Version update(Version versionOfApplication, Long revision, ApplicationXmlEntry applicationXml) throws XynaException {
    if (getVersionAfterUpdate().isStrictlyGreaterThan(versionOfApplication)) {
      if (logger.isInfoEnabled()) {
        logger.info("Updating Application during import");
      }
      Set<String> reserved = new HashSet<String>();
      for (XMOMXmlEntry e : applicationXml.getXmomEntries()) {
        if (isReserved(e.getFqName())) {
          reserved.add(e.getFqName());
          continue;
        }
        /*
         * in allen xmls nach referenzen auf reservierte objekte checken
         * formen:
         *    ReferenceName="IPv4" ReferencePath="base"
         *  oder 
         *    BaseTypeName="XynaExceptionBase" BaseTypePath="core.exception"
         *  oder 
         *    ReferenceName="QSInlineCodedServices.QSInlineCodedServices" ReferencePath="QS.infrastructure"
         */
        File f = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(e.getFqName(), revision) + ".xml");
        if (f.exists()) {
          Document d = XMLUtils.parse(f);          
          searchReferencedReservedObjects(d.getDocumentElement(), reserved);
        } 
      }
      synchronized (this) {
        initReservedDeps();
      }
      addIndirectlyReferencedReservedObjects(reserved);
      for (String fqName : reserved) {
        if (copyReservedToRevision(fqName, revision)) {
          String rootTag = GenerationBase.retrieveRootTag(fqName, revision);
          applicationXml.getXmomEntries().add(new XMOMXmlEntry(true, fqName, XMOMType.getXMOMTypeByRootTag(rootTag).name()));
        }
      }
      return getVersionAfterUpdate();
    }
    return versionOfApplication;
  }

  private Set<Long> revisionsLastCheck = new HashSet<Long>();

  private void initReservedDeps() {
    Set<Long> revisionsWithReservedObjects = new HashSet<Long>();
    revisionsWithReservedObjects.add(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    for (long r : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .getAllApplicationRevisions()) {
      revisionsWithReservedObjects.add(r);
    }
    for (long l : revisionsWithReservedObjects) {
      if (revisionsLastCheck.contains(l)) {
        //nicht mehrfach checken
        continue;
      }
      //dass man hier überhaupt ein zweites mal checkt, ist eigtl nur deswegen, weil z.b. die baseapplication beim ersten mal nicht da gewesen war oder sowas.
      logger.info("checking for reserved object dependencies in revision " + l);
      try {
        analyseReservedObjects(l, ODSConnectionType.HISTORY);
      } catch (XynaException e) {
        logger.info("Exception analysing reserved objects", e);
      }
    }
    revisionsLastCheck.clear();
    revisionsLastCheck.addAll(revisionsWithReservedObjects);
  }


  private void searchReferencedReservedObjects(Element el, Set<String> reserved) {
    checkAttributes(el, "ReferencePath", "ReferenceName", reserved);
    checkAttributes(el, "BaseTypePath", "BaseTypeName", reserved);
    checkAttribute(el, "Premise", reserved);
    for (Element c : XMLUtils.getChildElements(el)) {
      searchReferencedReservedObjects(c, reserved);
    }
  }


  private void checkAttribute(Element el, String att, Set<String> reserved) {
    String val = el.getAttribute(att);
    if (val.length() == 0) {
      return;
    }
    if (isReserved(val)) {
      reserved.add(val);
    }
  }


  private void checkAttributes(Element el, String attPath, String attName, Set<String> reserved) {
    String path = el.getAttribute(attPath);
    if (path.length() == 0) {
      return;
    }
    String name = el.getAttribute(attName);
    if (name.length() == 0) {
      return;
    }
    int idx = name.indexOf('.');
    if (idx > -1) {
      name = name.substring(0, idx - 1);
    }
    String fqName = path + "." + name;
    if (isReserved(fqName)) {
      reserved.add(fqName);
    }
  }


  private boolean copyReservedToRevision(String fqName, Long revision) throws Ex_FileAccessException {
    File f = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(fqName, revision) + ".xml");
    if (!f.exists()) {
      File existingFile = searchReservedObjectLocation(fqName);
      if (existingFile != null) {
        if (logger.isInfoEnabled()) {
          logger.info("Copying " + fqName + " to revision " + revision);
        }
        FileUtils.copyFile(existingFile, f, true);
        return true;
      } else {
        throw new RuntimeException("Did not find XML for server internal object " + fqName + " used in revision " + revision + ".");
      }
    }
    return false;
  }


  private File searchReservedObjectLocation(String fqName) {
    /*
     * erstmal in default workspace schauen - wenn es hier um eine updateinstallation geht, ist es hier vielleicht noch drin
     * danach checken wir die üblichen verdächtigen applications, und zuletzt einfach in allen revisions schauen.
     */
    File f =
        new File(GenerationBase.getFileLocationForDeploymentStaticHelper(fqName, RevisionManagement.REVISION_DEFAULT_WORKSPACE) + ".xml");
    if (f.exists()) {
      return f;
    }
    for (long r : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .getAllApplicationRevisions()) {
      f = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(fqName, r) + ".xml");
      if (f.exists()) {
        return f;
      }
    }
    for (long r : XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .getAllWorkspaceRevisions()) {
      f = new File(GenerationBase.getFileLocationForSavingStaticHelper(fqName, r) + ".xml");
      if (f.exists()) {
        return f;
      }
    }
    return null;
  }


  private boolean isReserved(String fqName) {
    return GenerationBase.isReservedServerObjectByFqOriginalName(fqName);
  }

}
