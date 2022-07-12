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
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FileSystemXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class BuildDatatypeJarFromSource {
  
  
  private final static String APPLICATION_DESCRIPTION_FILE = "application.xml";
  private final static String XMOM_FOLDER = "XMOM";
  

  public static void main(String[] args) throws IOException {
    RuntimeContext toBuild = RuntimeContext.valueOf(args[0]);
    // TODO non-recursive build is not possible, is there a use-case for it?
    int startOfFoldersToScan = 1;
    Collection<Path> applicationRoots = new ArrayList<>();
    Arrays.stream(args, startOfFoldersToScan, args.length).map(Path::of).forEach((p) -> BuildDatatypeJarFromSource.scanForApplicationFolders(p, applicationRoots));
    Map<RuntimeContext, Set<RuntimeContext>> dependencies = new HashMap<>();
    Map<RuntimeContext, File> xmomPaths = new HashMap<>();
    for (Path applicationRoot : applicationRoots) {
      Pair<RuntimeContext, Set<RuntimeContext>> newApp = readAppMetaData(applicationRoot);
      if (newApp.getFirst().equals(toBuild)) {
        dependencies.put(newApp.getFirst(), newApp.getSecond());
      }
      xmomPaths.put(newApp.getFirst(), Path.of(applicationRoot.toAbsolutePath().toString(), XMOM_FOLDER).toFile());
    }
    Path classFolder = Files.createTempDirectory("mdm.jar_build");
    try {
      FileSystemXMLSource source = new FileSystemXMLSource(dependencies, xmomPaths, classFolder.toFile());
      // collect all objects to deploy
      Set<RuntimeContext> relevantRevisions = findRelevantRevisions(toBuild, source, true);
      Set<GenerationBase> toDeploy = findAllDatatypes(relevantRevisions, source);
      // start mass deployment
      // TODO generateMdmJar supports crossCompile, does this default too low because XynaProperties are not available?
      //      should the crossCompile javaVersion be an input
      GenerationBase.deploy(new ArrayList<>(toDeploy), DeploymentMode.generateMdmJar, false, null);
      // zip classes to mdm.jar
      // TODO this creates a local file. Should there be a targetDir input?
      FileUtils.zipDirectory(new File("mdm.jar"), classFolder.toFile());
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
    //Document appDoc = XMLUtils.parse(applicationFile.toFile());
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static boolean isDatatypeOrException(Path path) {
    System.out.println("isDatatypeOrException: " + path);
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
          // TODO Auto-generated catch block
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
