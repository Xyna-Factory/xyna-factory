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

package com.gip.xyna.xmcp.xfcli.scriptentry;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FileSystemXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;

public class BuildDatatypeJarFromSource {
  
  
  private final static String APPLICATION_DESCRIPTION_FILE = "application.xml";
  private final static String XMOM_FOLDER = "XMOM";
  

  public static void main(String[] args) throws IOException {
    if (args.length > 3) {
      System.out.println("Expected at least three parameters: \n\t1. ApplicationName/VersionName\n\t"
          + "2. target folder for mdm.jar\n\t" + "3. ++ List of paths to be searched for applications\n\t" + "set '"
          + InMemoryCompilationSet.JAVA_VERSION_ENV_NAME + "' environment variable to specify java version of mdm.jar");
      System.exit(1);
    }


    RuntimeContext toBuild = RuntimeContext.valueOf(args[0]);
    String targetFolder = args[1];
    int startOfFoldersToScan = 2;
    System.out.println("Rtc to build: " + toBuild);
    System.out.println("Target folder: " + targetFolder + " (" + new File(targetFolder).getAbsolutePath() + ")");
    System.out.println("Rtc paths (" + (args.length - startOfFoldersToScan) + "):");
    for (int i = startOfFoldersToScan; i < args.length; i++) {
      System.out.println("  [" + (i - startOfFoldersToScan) + "]: " + args[i]);
    }
    String javaVersion = System.getenv(InMemoryCompilationSet.JAVA_VERSION_ENV_NAME);
    System.out.println("Java compile target version: "
        + (javaVersion != null ? javaVersion : XynaProperty.BUILDMDJAR_JAVA_VERSION.getDefaultValue()));

    Collection<Path> applicationRoots = new ArrayList<>();
    Arrays.stream(args, startOfFoldersToScan, args.length).map(Path::of).forEach((p) -> BuildDatatypeJarFromSource.scanForApplicationFolders(p, applicationRoots));

    //fill dependencies and xmomPaths
    Map<RuntimeContext, Set<RuntimeContext>> dependencies = new HashMap<>();
    Map<RuntimeContext, File> xmomPaths = new HashMap<>();
    for (Path applicationRoot : applicationRoots) {
      Pair<RuntimeContext, Set<RuntimeContext>> newApp = readAppMetaData(applicationRoot);
      dependencies.put(newApp.getFirst(), newApp.getSecond());
      xmomPaths.put(newApp.getFirst(), Path.of(applicationRoot.toAbsolutePath().toString(), XMOM_FOLDER).toFile());
    }
    Path classFolder = Files.createTempDirectory("mdm.jar_build");
    try {
      FileSystemXMLSource source = new FileSystemXMLSource(dependencies, xmomPaths, classFolder.toFile());
      // collect all objects to deploy
      Set<RuntimeContext> relevantRevisions = findRelevantRevisions(toBuild, source, true);

      //validate: No missing runtime contexts
      if (relevantRevisions.contains(null)) {
        System.out.println("One or more Runtime Contexts could not be resolved.");
        if (!xmomPaths.containsKey(toBuild)) {
          System.out.println(toBuild + " was not found. Check VersionName");
          System.out.println("Available RuntimeContexts: ");
          for (RuntimeContext rtc : xmomPaths.keySet()) {
            System.out.println("  " + rtc.toString());
          }
          throw new RuntimeException("One or more Runtime Contexts could not be resolved.");
        }
      }

      Set<GenerationBase> toDeploy = findAllDatatypes(relevantRevisions, source);
      // start mass deployment
      // InMemoryCompilationSet.JAVA_VERSION_ENV_NAME environment variable can be set to choose cross compile java version
      System.out.println("deploying " + toDeploy.size() + " Datatypes and Exceptions.");
      GenerationBase.deploy(new ArrayList<>(toDeploy), DeploymentMode.generateMdmJar, false, null);
      // zip classes to mdm.jar
      FileUtils.zipDirectory(new File(targetFolder, "mdm.jar"), classFolder.toFile());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      FileUtils.deleteDirectoryRecursively(classFolder.toFile());
    }
   
  }


  private static void scanForApplicationFolders(Path path, Collection<Path> applicationRoot) {
    try (Stream<Path> files = Files.walk(path)) {
      Collection<Path> apps = files.filter(BuildDatatypeJarFromSource::isApplicationRootFolder)
                                   .collect(Collectors.toList());
      applicationRoot.addAll(apps);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static boolean isApplicationRootFolder(Path potentialAppRoot) {
    Path applicationFile = Path.of(potentialAppRoot.toAbsolutePath().toString(), APPLICATION_DESCRIPTION_FILE);
    if (applicationFile.toFile().exists()) {
      Path xmomFolder = Path.of(potentialAppRoot.toAbsolutePath().toString(), XMOM_FOLDER);
      if (xmomFolder.toFile().exists()) {
        return true;
      }
    }
    return false;
  }
  
  
  
  private static Pair<RuntimeContext, Set<RuntimeContext>> readAppMetaData(Path applicationRoot) {
    Path applicationFile = Path.of(applicationRoot.toAbsolutePath().toString(), APPLICATION_DESCRIPTION_FILE);
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(new ByteArrayInputStream(FileUtils.readFileAsString(applicationFile.toFile()).getBytes(Constants.DEFAULT_ENCODING)), handler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ApplicationXmlEntry applicationXml = handler.getApplicationXmlEntry();
    Application app = new Application(applicationXml.getApplicationName(), applicationXml.getVersionName());
    // more general mapper? generate workspaces as well
    Set<RuntimeContext> dependencies = applicationXml.getRuntimeContextRequirements().stream().map(x -> new Application(x.getApplication(), x.getVersion())).collect(Collectors.toSet());
    return Pair.of(app, dependencies);
  }
  
  
  private static Set<RuntimeContext> findRelevantRevisions(RuntimeContext toBuild, FileSystemXMLSource source, boolean recursive) {
    Set<RuntimeContext> relevant = new HashSet<>();
    relevant.add(toBuild);
    if (recursive) {
      try {
        relevant.addAll(source.getDependenciesRecursivly(source.getRevision(toBuild)).stream().map(r -> { try { return source.getRuntimeContext(r); } catch (Exception e) { throw new RuntimeException(e);}}).collect(Collectors.toSet()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }
    return relevant;
  }


  private static Set<GenerationBase> findAllDatatypes(Set<RuntimeContext> relevantRevisions, FileSystemXMLSource source) {
    GenerationBaseCache cache = new GenerationBaseCache();
    Set<GenerationBase> datatypes = new HashSet<>();
    for (RuntimeContext relevantRevision : relevantRevisions) {
      datatypes.addAll(instantiateDatatypes(relevantRevision, source, cache));
    }
    return datatypes;
  }


  private static Collection<? extends GenerationBase> instantiateDatatypes(RuntimeContext relevantRevision, FileSystemXMLSource source, GenerationBaseCache cache) {
    try {
      return Files.walk(source.getXMOMPath(relevantRevision).toPath()).filter(BuildDatatypeJarFromSource::isDatatypeOrException).map(p -> BuildDatatypeJarFromSource.instantiateDatytpeOrException(p, relevantRevision, source, cache)).collect(Collectors.toSet());
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("source: " + source);
      System.out.println("relevantRevision: " + relevantRevision);
      throw new RuntimeException(e);
    }
  }
  
  
  private static boolean isDatatypeOrException(Path path) {
    if (path.toFile().isFile()) {
      try (FileInputStream fis = new FileInputStream(path.toFile())) {
        XMOMType type = XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(fis));
        return type == XMOMType.DATATYPE ||
               type == XMOMType.EXCEPTION;
      } catch (Exception e) {
        System.out.println("ERROR for isDatatypeOrException: " + path);
        try {
          System.out.println(FileUtils.readFileAsString(path.toFile()));
        } catch (Ex_FileWriteException e1) {
          e1.printStackTrace();
        }
        throw new RuntimeException(e);
      }
    } else {
      return false;
    }
  }
  
  
  private static GenerationBase instantiateDatytpeOrException(Path path, RuntimeContext currentRevision, FileSystemXMLSource source, GenerationBaseCache cache) {
    try (FileInputStream fis =
        new FileInputStream(path.toFile())) {
      String fqName = deriveFqName(source.getXMOMPath(currentRevision), path);
      XMOMType type = XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(fis));
      switch (type) {
      case DATATYPE:
        return DOM.getOrCreateInstance(fqName, cache, source.getRevision(currentRevision), source);
      case EXCEPTION:
        return ExceptionGeneration.getOrCreateInstance(fqName, cache, source.getRevision(currentRevision), source);
      default:
        throw new RuntimeException("Not of a accepted type: " + type);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static String deriveFqName(File rootPath, Path fullPath) {
    return fullPath.toFile().getAbsolutePath().substring(rootPath.getAbsolutePath().length() + 1, fullPath.toFile().getAbsolutePath().length() - 4).replace(Constants.FILE_SEPARATOR, ".");
  }


}
