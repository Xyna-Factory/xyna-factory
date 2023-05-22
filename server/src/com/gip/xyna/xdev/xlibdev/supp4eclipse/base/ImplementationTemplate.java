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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.base;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.exceptions.XDEV_ProjectTemplateZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public abstract class ImplementationTemplate {

  /**
   * im build.xml im TemplateImplName projekt verwendet
   */
  public static final String TEMPLATE_DEPLOY_STATEMENT = "TemplateDeployStatement";
  public static final String TEMPLATE_DEPLOY_TARGET_PATH = "TemplateDeployTargetPath";
  public static final String TEMPLATE_DEPLOY_MAIN_JAR = "TemplateDeployMainJar";
  public static final String TEMPLATE_IMPL_KIND = "TemplateImplKind";

  public final static FilenameFilter FILENAMEFILTER_JARS = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      return name.endsWith(".jar") && !name.equalsIgnoreCase("xynafactory-javadoc.jar");
    }
  };

  protected final Long revision;
  
  public ImplementationTemplate(Long revision) {
    this.revision = revision;
  }

  public Long getRevision() {
    return revision;
  }
  
  public abstract String getProjectName();


  public abstract String getProjectKindFolder();
  
  
  public abstract String getBuildScriptName();


  public abstract String updateBuildXmlDeployTarget(File projectLocationDirectory, String line)
      throws XDEV_InvalidProjectTemplateParametersException;


  public abstract void writeTemplateFiles(File projectLocationDirectory, boolean legacy)
      throws XDEV_ProjectTemplateZipFileCouldNotBeCreatedException, XDEV_InvalidProjectTemplateParametersException,
      Ex_FileAccessException;


  public void writeToClasspathFile(BufferedWriter bw, boolean legacy) throws IOException {

    // add all the libraries that are available in the general lib directory
    final String commonLib;
    if (legacy) {
      commonLib = "CommonLibs/lib/xyna";
    } else {
      commonLib = "CommonLibs/lib";
    }
    
    File[] jars = new File("lib").listFiles(FILENAMEFILTER_JARS);
    for (File j : jars) {
      bw.write("  <classpathentry kind=\"lib\" path=\"/" + commonLib + "/" + j.getName() + "\"");
      if(j.getName().equalsIgnoreCase("xynafactory.jar")) {
        bw.write(">\n");
        bw.write("      <attributes>\n");                                 
        bw.write("           <attribute name=\"javadoc_location\" value=\"jar:platform:/resource/" + commonLib + "/xynafactory-javadoc.jar!\"/>\n");
        bw.write("      </attributes>\n");        
        bw.write("</classpathentry>\n");
      } else {
        bw.write("/>\n");
      }
    }

    // add the mdm jar
    if (legacy) {
      bw.write("  <classpathentry kind=\"lib\" path=\"lib/xyna/" + Support4Eclipse.FILENAME_MDM_JAR + "\"/>\n");
    } else {
      bw.write("  <classpathentry kind=\"lib\" path=\"/" + commonLib + "/" + Support4Eclipse.FILENAME_MDM_JAR + "\"/>\n");
    }
  }


  public String getBaseDirectoryName() {
    return getProjectName();
  }
  
  protected String getWorkspaceName() throws XDEV_InvalidProjectTemplateParametersException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      Workspace workspace = revisionManagement.getWorkspace(revision);
      return workspace.getName();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XDEV_InvalidProjectTemplateParametersException("revision", String.valueOf(revision), e);
    }
  }
  
  protected String getRevisionDir() {
    String revisionDir =  RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    return revisionDir.substring(0,revisionDir.length()-1); //FileSeparator am Ende abschneiden
  }
}
