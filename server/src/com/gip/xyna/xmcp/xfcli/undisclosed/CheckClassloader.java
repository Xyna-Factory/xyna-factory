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
package com.gip.xyna.xmcp.xfcli.undisclosed;



import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xmcp.xfcli.generated.Showdeploymentitemdetails;
import com.gip.xyna.xmcp.xfcli.impl.ShowdeploymentitemdetailsImpl;



public class CheckClassloader implements CommandExecution {

  @Override
  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {

    Parameters p = Parameters.parse(allArgs, clw);
    if (p == null) {
      return;
    }

    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    ClassLoaderBase clb = cld.getClassLoaderByType(p.classloaderType, p.fqNameParent, p.revision);
    if (clb == null) {
      //TODO andere classloadertypen probieren und zurückgeben: meintest du classloadertype x?
      clw.writeLineToCommandLine("Classloader for " + p.fqNameParent + " not found.");
      return;
    }
    
    boolean loadedBefore;
    Class<?> clazz = clb.findLoadedClass2(p.fqNameLoad);
    if (clazz == null) {
      if (p.loadclass) {
        try {
          clazz = Class.forName(p.fqNameLoad, true, clb);
        } catch (Throwable e) {
          clw.writeLineToCommandLine("Class " + p.fqNameLoad + " could not be loaded.");
          printStackTrace(clw, e);
          return;
        }
      } else {
        Class<?> clazz2 = clb.getPreviouslyLoadedClass(p.fqNameLoad);
        if (clazz2 == null) {
          clw.writeLineToCommandLine("Class " + p.fqNameLoad + " was not loaded previously with classloader " + clb);
        } else {
          clw.writeLineToCommandLine("Class " + p.fqNameLoad + " was previously loaded, but not cached by classloader " + clb);
        }
        return;
      }
      loadedBefore = false;
    } else {
      loadedBefore = true;
    }
    ClassLoader cl = clazz.getClassLoader();
    if (loadedBefore) {
      clw.writeLineToCommandLine("Class " + p.fqNameLoad + " was already loaded with classloader " + cl);
    } else {
      clw.writeLineToCommandLine("Class " + p.fqNameLoad + " now successfully loaded and initialized with classloader " + cl);
    }
    if (cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb2 = (ClassLoaderBase) cl;
      ClassLoaderType loadedType = clb2.getType();
      ClassLoaderBase clb2FromDispatcher = cld.findClassLoaderByType(p.fqNameLoad, p.revision, loadedType, true);
      if (clb2FromDispatcher == null) {
        //loadedType ist nicht unbedingt der richtige typ.
        for (ClassLoaderType type : ClassLoaderType.values()) {
          switch (type) {
            case MDM :
            case Exception :
            case Filter :
            case Trigger :
            case WF :
            case OutdatedFilter :
              break;
            default :
              continue;
          }
          clb2FromDispatcher = cld.findClassLoaderByType(p.fqNameLoad, p.revision, type, true);
          if (clb2FromDispatcher != null) {
            break;
          }
        }
      }
      if (clb2 == clb2FromDispatcher) {
        clw.writeLineToCommandLine("The found classloader is current!");
        writeClassLoaderDetails(clw, clb2, "Information about the found classloader:", p.showDetails);
      } else if (clb2FromDispatcher == null) {
        clw.writeLineToCommandLine("There is currently no active classloader registered for " + p.fqNameLoad + "!");
        writeClassLoaderDetails(clw, clb2, "Information about the found classloader:", p.showDetails);
        
        //printDeploymentStatus
        clw.writeLineToCommandLine("");
        Showdeploymentitemdetails param = new Showdeploymentitemdetails();
        param.setObjectName(p.fqNameLoad);
        Long revisionOfLoadedObject = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
            .getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(p.fqNameLoad, p.revision);
        if (revisionOfLoadedObject == null) {
          clw.writeLineToCommandLine("Object " + p.fqNameLoad + " is unknown in DeploymentItemStateManagement");
          return;
        }
        RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        if (revMgmt.isWorkspaceRevision(revisionOfLoadedObject)) {
          param.setWorkspaceName(revMgmt.getWorkspace(revisionOfLoadedObject).getName());
        } else if (revMgmt.isApplicationRevision(revisionOfLoadedObject)) {
          param.setApplicationName(revMgmt.getApplication(revisionOfLoadedObject).getName());
          param.setVersionName(revMgmt.getApplication(revisionOfLoadedObject).getVersionName());
        }
        new ShowdeploymentitemdetailsImpl().execute(clw, param);
      } else {
        clw.writeLineToCommandLine("The current classloader is different from the found classloader!");
        writeClassLoaderDetails(clw, clb2, "Information about the found classloader:", true);
        writeClassLoaderDetails(clw, clb2FromDispatcher, "Information about the current classloader:", p.showDetails);
      }
    }
  }


  private void writeClassLoaderDetails(CommandLineWriter clw, ClassLoaderBase clb, String description, boolean showDetails) {
    clw.writeLineToCommandLine(""); //zeilenumbruch
    clw.writeLineToCommandLine(description);
    clw.writeLineToCommandLine(clb.getExtendedDescription(showDetails));
    if (showDetails) {
      clb.debugClassLoadersToReload(clw);
    }
  }


  private void printStackTrace(CommandLineWriter clw, Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    clw.writeLineToCommandLine(sw.toString());
  }


  private static class Parameters {

    String fqNameParent;
    ClassLoaderType classloaderType;
    long revision;
    String fqNameLoad;
    boolean showDetails;
    boolean loadclass = true;

    private Parameters() {
    }


    public static Parameters parse(AllArgs allArgs, CommandLineWriter clw) {
      Parameters p = new Parameters();
      try {
        switch (allArgs.getArgCount()) {
          case 6 :
            p.loadclass = Boolean.valueOf(allArgs.getArg(5));
          case 5 :
            p.showDetails = Boolean.valueOf(allArgs.getArg(4));
          case 4 :
            p.fqNameLoad = allArgs.getArg(3);
            p.revision = Long.valueOf(allArgs.getArg(2));
            p.classloaderType = ClassLoaderType.valueOf(allArgs.getArg(1));
            p.fqNameParent = allArgs.getArg(0);
            break;
          default :
            writeUsage(clw);
            return null;
        }
        return p;
      } catch (Exception e) {
        CentralFactoryLogging.getLogger(CheckClassloader.class).debug("Failed to parse parameters", e);
        writeUsage(clw);
        return null;
      }
    }


    private static void writeUsage(CommandLineWriter clw) {
      clw.writeLineToCommandLine("parameters are <fqName of parent classloader: String>, <classloader type ("
          + Arrays.toString(ClassLoaderType.values())
          + "): String>, <revision: long>, <fqName of class to load: String>, [<showDetails: boolean (false)>, <loadClass: boolean (true)>]\n");
    }
  }
}
