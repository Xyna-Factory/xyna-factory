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
package com.gip.xyna.xact.filter.xmom.paths;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.xmom.paths.json.PathItem;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

public class XmomPaths {

  private static final Logger logger = CentralFactoryLogging.getLogger(XmomPaths.class);

  private TreeMap<String,XmomPath> departmentPaths;
  private TreeMap<String, XmomPath> otherPaths;
  private List<PathItem> paths = new ArrayList<PathItem>();
  private Map<String,String> departmentMap = new HashMap<String,String>();
   
  public XmomPaths() throws IOException {
    departmentPaths = new TreeMap<String,XmomPath>();
    otherPaths = new TreeMap<String,XmomPath>();
    fillDepartmentMap(); //FIXME
  }

  public XmomPaths(Long baseRevision) throws IOException {
    this();
    
    Set<Long> revisions = new HashSet<Long>();
    revisions.add(baseRevision);
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    rcdm.getDependenciesRecursivly(baseRevision, revisions);
    for( Long revision : revisions ) {
      try {
        RuntimeContext rc = rm.getRuntimeContext(revision);
        boolean deployed = rc.getType() == RuntimeContextType.Application;
        String path =  RevisionManagement.getPathForRevision(PathType.XMOM, revision, deployed);
        readPaths( Paths.get( path ) );
        
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // FIXME was nun?
        logger.warn("Could not read runtime context for revision "+revision, e );
      }
     
      
    }
  }

  

  
  public void readPaths(Path xmomDir) throws IOException {
    try ( DirectoryStream<Path> ds = Files.newDirectoryStream( xmomDir) ) {
      XmomPath base = null;
      for( Path p : ds ) {
        if( ! Files.isDirectory(p) ) {
          continue;
        }
        String department = hasDepartment(xmomDir, p);
        XmomPath xp = createXmomPath(xmomDir, p, department);
        xp.setAbsolute(true);
        
        if( department != null ) {
          XmomPath old = departmentPaths.get( department );
          if( old == null ) {
            departmentPaths.put( department, xp );
          } else {
            old.merge(xp);
          }
        } else if (xp.getName().equals("base")) {
          base = xp;
        } else {
          XmomPath old = otherPaths.get( xp.getName() );
          if( old == null ) {
            otherPaths.put( xp.getName(), xp );
          } else {
            old.merge(xp);
          }
        }
      }
      if(base != null) {
        XmomPath xprc = departmentPaths.get(departmentMap.get("xprc"));
        if(xprc != null) {
          xprc.addChild(base);
        }
      }
    } catch (NoSuchFileException e) {
      // continue with no errors when no documents have been saved, yet
      logger.warn("No XMOM paths to show since directory " + xmomDir + " does not exist.", e);
    }
  }
  
  private void fillDepartmentMap() {
    departmentMap.put( "xact", "Activation");
    departmentMap.put( "xdev", "Development Factory");
    departmentMap.put( "xdev.tsim", "Test & Simulation");
    departmentMap.put( "xdnc", "Dynamic Network Configuration");
    departmentMap.put( "xfmg", "Factory Management" );
    departmentMap.put( "xmcp", "Multi-Channel Portal");
    departmentMap.put( "xnwh", "Network Warehouse");
    departmentMap.put( "xprc", "Processing");
    departmentMap.put( "xprv", "Provisioning");
    departmentMap.put( "xsas", "Service Assurance");
           
    //FIXME
    //com.gip.xyna.3.0.XMDM.xact
    //com.gip.xyna.3.0.XMDM.xdev
    
  }
  

  
  
  private String hasDepartment(Path root, Path p) {
    String relPath = root.relativize(p).toString();
    relPath = relPath.replace("/", ".");
    return departmentMap.get(relPath);
  }

  private XmomPath createXmomPath(Path root, Path p, String department) throws IOException {
    XmomPath xp = new XmomPath();
    xp.setName(p.getFileName().toString());
    xp.setPath(root.relativize(p).toString());
    xp.setAbsolute(false);
    xp.setDepartment(department);
    
    try( DirectoryStream<Path> ds = Files.newDirectoryStream(p) ) {
      for( Path c : ds ) {
        if( ! Files.isDirectory(c) ) {
          xp.setContainsFiles(true);
          continue;
        }
        String childDepartment = hasDepartment(root, c);
        xp.addChild( createXmomPath(root, c, childDepartment) );
      }
    }
    
    return xp;
  }
  
  
  
  public enum Mode {
    full() {
      //volle Hierarchietiefe
      @Override
      public List<PathItem> transformToPathItem(XmomPath xp, Mode nextMode, PathItem pi) {
        String nextPath = pi.getPath()+"."+xp.getName();
        for( XmomPath child : xp.getChildren() ) {
          pi.addChildren( nextMode.transformToPathItem(child, nextMode, createPathItem(child, this, nextPath, false) ) );
        }
        return Collections.singletonList(pi);
      }
  
    },
    
    compact() {
      //leere Pfade werden in die Pfadnamen tieferliegender gef�llter Pfade integriert
      @Override
      public List<PathItem> transformToPathItem(XmomPath xp, Mode nextMode, PathItem pi) {
        boolean compactify = xp.getDepartment() == null && !xp.containsFiles() && (xp.getChildren() != null && xp.getChildren().size() < 2);
        if( compactify) {
          String nextPath = pi.getPath()+".";
          List<PathItem> pis = new ArrayList<PathItem>();
          for( XmomPath child : xp.getChildren() ) {
            PathItem childPi = createPathItem(child, this, nextPath, pi.isAbsolute());
            childPi.setAbsolute(pi.isAbsolute());
            pis.addAll( transformToPathItem(child, this, childPi ) );
          }
          return pis;
        } else {
          //normal gef�llt Pfade wie "full" behandeln
          return Mode.full.transformToPathItem(xp, this, pi );
        }
      }
    },
    
    flat() {
      //alle Pfade werden als absolute Pfade ohne Kindpfade aufgelistet
      @Override
      public List<PathItem> transformToPathItem(XmomPath xp, Mode nextMode, PathItem pi) {
        String nextPath = pi.getPath()+".";
        List<PathItem> pis = new ArrayList<PathItem>();
        if( xp.containsFiles() || xp.getDepartment() != null ) {
          pis.add( pi );
        }
        for( XmomPath child : xp.getChildren() ) {
          pis.addAll( nextMode.transformToPathItem( child, nextMode, createPathItem(child, nextMode, nextPath, pi.isAbsolute() ) ) );
        }
        return pis;
      }
    },
    
    shallow() {
      //alle Pfade unterhalb der Top-Level-Pfade werden flach als Kindpfade der jeweiligen Top-Level-Pfade dargestellt
      @Override
      public List<PathItem> transformToPathItem(XmomPath xp, Mode nextMode, PathItem pi) {
        if( xp.getDepartment() != null ) {
          //wie "full" behandeln
          return Mode.full.transformToPathItem(xp, this, pi);
        } else {
          //andere Pfade wie "flat" behandeln
          return Mode.flat.transformToPathItem(xp, Mode.flat, pi);
        }
      }
      
    }
    ;

    /**
     * �ber mode.transformToPathItem(..,nextMode,-..) kann ein nextMode �bergeben werden, 
     * damit der Mode "mode" nur einmalig verwendet wird und 
     * f�r die Rekursion dann "nextMode" verwendet wird.
     * @param xp
     * @param nextMode
     * @param pi
     * @return
     */
    public abstract List<PathItem> transformToPathItem(XmomPath xp, Mode nextMode, PathItem pi);
    
   }
  
  private static PathItem createPathItem(XmomPath xp, Mode mode, String basePath, boolean isAbsolute) {
    PathItem pi = new PathItem();
    String path = xp.getName();
    if( mode != Mode.full ) {
      path = basePath+path;
    }
    pi.setPath(path);
    pi.setAbsolute(isAbsolute);
    pi.setLabel(xp.getDepartment());
    return pi;
  }
  
  public void transformToPathItems(Mode mode) {
    for( XmomPath dp : departmentPaths.values() ) {
      paths.addAll(mode.transformToPathItem(dp, mode, createPathItem(dp, mode, "", dp.isAbsolute() ) ) );
    }
    
    PathItem otherPi = new PathItem();
    otherPi.setLabel("Other");
    otherPi.setPath("");
    paths.add(otherPi);
  
    for( XmomPath xp : otherPaths.values() ) {
      otherPi.addChildren( mode.transformToPathItem(xp, mode, createPathItem(xp, mode, "", xp.isAbsolute() ) ) );
    }
  }

  public String toJson() {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();{
      jb.addObjectListAttribute(Tags.PATHS, paths);
    } jb.endObject();
    
    return jb.toString();
  }

}
