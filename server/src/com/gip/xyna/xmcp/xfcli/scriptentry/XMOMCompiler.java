/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Level;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FileSystemXMLSource;



public class XMOMCompiler {

  private final static String APPLICATION_DESCRIPTION_FILE = "application.xml";
  private final static String XMOM_FOLDER = "XMOM";


  private static final String INPUT_SEPARATOR = ":";
  private static final String INPUT_APPLICATION_NAME_AND_VERSION = "app";
  private static final String INPUT_OUPTUT_PATH = "output";
  private static final String INPUT_STORABLE_INTERFACES = "storableInterfaces";
  private static final String INPUT_SOURCEPATHS = "sourcePaths";
  private static final String INPUT_SINGLE_FILE = "singleFile";
  private static final String INPUT_RECURSIVE = "recursive";
  private static final String INPUT_TYPES = "types";
  private static final String INPUT_PRINT_CLASSPATH = "printclasspath";

  private static final List<String> inputKeys =
      Arrays.asList(INPUT_APPLICATION_NAME_AND_VERSION, INPUT_OUPTUT_PATH, INPUT_STORABLE_INTERFACES, INPUT_SOURCEPATHS, INPUT_SINGLE_FILE,
                    INPUT_RECURSIVE, INPUT_TYPES, INPUT_PRINT_CLASSPATH);


  public static void main(String[] args) {
    XMOMCompiler compiler = new XMOMCompiler();
    XMOMCompilationData data = compiler.parseInput(args);
    compiler.validateInputData(data);
    compiler.compile(data);
  }


  private void validateInputData(XMOMCompilationData data) {
    boolean invalid = false;
    if (data.getAllowedTypes() == null || data.getAllowedTypes().size() == 0) {
      System.out.println("No XMOM Types specified. Pass an argument like this: " + INPUT_TYPES + INPUT_SEPARATOR + XMOMType.DATATYPE);
      invalid = true;
    }

    if (data.getApplicationNameAndVersion() == null || data.getApplicationNameAndVersion().isBlank()) {
      System.out.println("No application name/version specified. Pass an argument like this: " + INPUT_APPLICATION_NAME_AND_VERSION
          + INPUT_SEPARATOR + "Crypto/1.0.6");
      invalid = true;
    }

    if (data.getOutputPath() == null || data.getOutputPath().isBlank()) {
      System.out.println("No output path specified. Pass an argument like this: " + INPUT_OUPTUT_PATH + INPUT_SEPARATOR + "./xmomclasses");
      invalid = true;
    }

    if (data.getSourcePaths() == null || data.getSourcePaths().isEmpty()) {
      System.out.println("No source paths specified. Pass an argument like this: " + INPUT_SOURCEPATHS + INPUT_SEPARATOR + "../../modules");
      invalid = true;
    }

    if (invalid) {
      throw new RuntimeException("Invalid Input.");
    }
  }


  private XMOMCompilationData parseInput(String[] args) {
    XMOMCompilationData data = new XMOMCompilationData();

    for (int i = 0; i < args.length; i++) {
      int idx = args[i].indexOf(INPUT_SEPARATOR);
      if (idx == -1) {
        System.out.println("ERROR: invalid argument " + args[i] + ". Does not contain '" + INPUT_SEPARATOR + "'");
        throw new RuntimeException();
      }
      String key = args[i].substring(0, idx);
      String value = args[i].substring(idx + 1);

      switch (key) {
        case INPUT_APPLICATION_NAME_AND_VERSION :
          data.setApplicationNameAndVersion(value);
          break;
        case INPUT_OUPTUT_PATH :
          data.setOutputPath(value);
          break;
        case INPUT_STORABLE_INTERFACES :
          List<String> storableInterfaces = new ArrayList<String>(Arrays.asList(value.split(",")));
          storableInterfaces.removeIf(x -> x.isBlank());
          data.setStorableInterfaces(storableInterfaces);
          break;
        case INPUT_SOURCEPATHS :
          data.setSourcePaths(Arrays.asList(value.split(",")));
          break;
        case INPUT_SINGLE_FILE :
          data.setReturnSingleFile(Boolean.parseBoolean(value));
          break;
        case INPUT_RECURSIVE :
          data.setRecursive(Boolean.parseBoolean(value));
          break;
        case INPUT_TYPES :
          data.setAllowedTypes(Arrays.asList(value.split(",")).stream().map(x -> XMOMType.valueOf(x)).collect(Collectors.toSet()));
          break;
        case INPUT_PRINT_CLASSPATH :
          data.setPrintClasspath(Boolean.parseBoolean(value));
        default :
          throw new RuntimeException("Unknown input: '" + key + "'. Available inputs: " + String.join(", ", inputKeys));
      }
    }

    return data;
  }


  private void configureLoggers() {
    CentralFactoryLogging.getLogger(FileUtils.class).setLevel(Level.WARN);
    CentralFactoryLogging.getLogger(GenerationBase.class).setLevel(Level.WARN);
    CentralFactoryLogging.getLogger(ExceptionGeneration.class).setLevel(Level.WARN);
    CentralFactoryLogging.getLogger(WF.class).setLevel(Level.WARN);
    CentralFactoryLogging.getLogger(XynaProperty.class).setLevel(Level.WARN);
  }


  private void printData(XMOMCompilationData data) {
    List<String> storableIfs = data.getStorableInterfaces();
    Set<String> typesAsStrings = data.getAllowedTypes().stream().map(x -> x.toString()).collect(Collectors.toSet());
    List<String> sourcePaths = data.getSourcePaths().stream().map(this::printWithAbsPath).collect(Collectors.toList());
    int classPathEntries = System.getProperty("java.class.path", "").split(":").length;
    String classPathString = (data.isPrintClasspath() ? System.getProperty("java.class.path", "") : classPathEntries + " entries");
    System.out.println("Building: " + data.getApplicationNameAndVersion());
    System.out.println("Putting result here: " + printWithAbsPath(data.getOutputPath()));
    System.out.println("Storable Interfaces: " + storableIfs.size() + ": " + String.join(", ", storableIfs));
    System.out.println("Source paths: " + sourcePaths.size() + ":\n  " + String.join("\n  ", sourcePaths));
    System.out.println("Included XMOM Types: " + data.getAllowedTypes().size() + ": " + String.join(", ", typesAsStrings));
    System.out.println("Result will " + (data.isReturnSingleFile() ? "" : "NOT ") + "be a single file.");
    System.out.println("Result will " + (data.isRecursive() ? "" : "NOT ") + "include Objects from other Applications.");
    System.out.println("Classpath System property (java.class.path): " + classPathString);
  }


  private String printWithAbsPath(String in) {
    return in + " (" + new File(in).getAbsolutePath() + ")";
  }


  public void compile(XMOMCompilationData data) {
    File outputLocation = new File(data.getOutputPath());
    Path tmpClassFolder = createTmpFolder(outputLocation.getName());
    try {
      configureLoggers();
      printData(data);
      setStorableInterfaces(data.getStorableInterfaces());
      setSingleCompileProperty();

      FileSystemXMLSource source = createXMLSource(data, tmpClassFolder);
      Set<GenerationBase> toDeploy = findXmomObjects(data, source, tmpClassFolder);

      deploy(toDeploy);

      if (!data.isRecursive()) {
        File xmomPath = source.getXMOMPath(RuntimeContext.valueOf(data.getApplicationNameAndVersion()));
        removeClassFilesFromOtherRevisions(tmpClassFolder, xmomPath);
      }

      finalizeResult(data.isReturnSingleFile(), outputLocation.getAbsoluteFile(), tmpClassFolder.toFile());

      System.out.println("Done");
    } finally {
      FileUtils.deleteDirectory(tmpClassFolder.toFile());
    }
  }


  private void finalizeResult(boolean returnSingleFile, File outputLocation, File tmpLocation) {
    if (returnSingleFile) {
      zipResult(outputLocation, tmpLocation);
    } else {
      copyResult(outputLocation, tmpLocation);
    }
  }


  private void setSingleCompileProperty() {
    try {
      XynaProperty.NO_SINGLE_COMPILE.set(true);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private void removeClassFilesFromOtherRevisions(Path classFolder, File xmomPath) {
    ApplicationXmlEntry appXml = readApplicationXml(xmomPath.getParentFile().toPath());
    Set<String> fqNames = new HashSet<String>();
    for (XMOMXmlEntry entry : appXml.getXmomEntries()) {
      fqNames.add(entry.getFqName().replace('.', File.separatorChar));
    }

    int basePathLength = classFolder.toString().length() + 1; //+1 for /
    Collection<File> files;

    try (Stream<Path> paths = Files.walk(classFolder)) {
      files = paths.map(x -> x.toFile()).filter(x -> x.isFile()).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    File classDirFile = classFolder.toFile();
    for (File file : files) {
      String fullName = file.toString().substring(basePathLength);
      String fqName = convertToFqName(fullName);
      if (!fqNames.contains(fqName)) {
        FileUtils.deleteFileWithRetries(file);
        FileUtils.deleteEmptyDirectoryRecursively(file.getParentFile(), classDirFile);
      }
    }

  }


  private String convertToFqName(String s) {
    return s.substring(0, (s.contains("$") ? s.indexOf("$") : s.length() - 6)); //6 => .class
  }


  private void deploy(Set<GenerationBase> toDeploy) {
    try {
      GenerationBase.deploy(new ArrayList<>(toDeploy), DeploymentMode.generateMdmJar, false, null);
    } catch (Exception e) {
      //TODO: improve error message
      throw new RuntimeException(e);
    }
  }


  private void copyResult(File target, File classFolder) {
    try {
      FileUtils.copyRecursivelyWithFolderStructure(classFolder, target);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    } finally {
      FileUtils.deleteDirectoryRecursively(classFolder);
    }
  }


  private Path createTmpFolder(String fileName) {
    try {
      return Files.createTempDirectory(fileName + "_build");
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }
  }


  private void zipResult(File f, File classFolder) {
    try {
      FileUtils.zipDirectory(f, classFolder);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    } finally {
      FileUtils.deleteDirectoryRecursively(classFolder);
    }
  }


  private FileSystemXMLSource createXMLSource(XMOMCompilationData data, Path classFolder) {
    Map<RuntimeContext, Set<RuntimeContext>> dependencies = new HashMap<>();
    Map<RuntimeContext, File> xmomPaths = new HashMap<>();
    Collection<Path> applicationRoots = new ArrayList<>();
    List<String> foldersToScan = data.getSourcePaths();
    foldersToScan.stream().map(Path::of).forEach((p) -> scanForApplicationFolders(p, applicationRoots));

    for (Path applicationRoot : applicationRoots) {
      Pair<RuntimeContext, Set<RuntimeContext>> newApp = readAppMetaData(applicationRoot);
      dependencies.put(newApp.getFirst(), newApp.getSecond());
      xmomPaths.put(newApp.getFirst(), Path.of(applicationRoot.toAbsolutePath().toString(), XMOM_FOLDER).toFile());
    }

    return new FileSystemXMLSource(dependencies, xmomPaths, classFolder.toFile());
  }


  private void scanForApplicationFolders(Path path, Collection<Path> applicationRoot) {
    try (Stream<Path> files = Files.walk(path)) {
      Collection<Path> apps = files.filter(this::isApplicationRootFolder).collect(Collectors.toList());
      applicationRoot.addAll(apps);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private boolean isApplicationRootFolder(Path potentialAppRoot) {
    Path applicationFile = Path.of(potentialAppRoot.toAbsolutePath().toString(), APPLICATION_DESCRIPTION_FILE);
    if (applicationFile.toFile().exists()) {
      Path xmomFolder = Path.of(potentialAppRoot.toAbsolutePath().toString(), XMOM_FOLDER);
      if (xmomFolder.toFile().exists()) {
        return true;
      }
    }
    return false;
  }


  private Set<GenerationBase> findXmomObjects(XMOMCompilationData data, FileSystemXMLSource source, Path classFolder) {
    RuntimeContext toBuild = RuntimeContext.valueOf(data.getApplicationNameAndVersion());
    Set<RuntimeContext> relevantRevisions = findRelevantRevisions(toBuild, source);
    return collectXmomObjects(data, relevantRevisions, source);
  }


  private Set<RuntimeContext> findRelevantRevisions(RuntimeContext toBuild, FileSystemXMLSource source) {
    Set<RuntimeContext> relevant = new HashSet<>();
    relevant.add(toBuild);
    try {
      Long revision = source.getRevision(toBuild);
      Stream<Long> revisions = source.getDependenciesRecursivly(revision).stream();
      relevant.addAll(revisions.map(r -> mapToRuntimeContext(r, source)).collect(Collectors.toSet()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    return relevant;
  }


  private RuntimeContext mapToRuntimeContext(Long revision, FileSystemXMLSource source) {
    try {
      return source.getRuntimeContext(revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private ApplicationXmlEntry readApplicationXml(Path applicationRoot) {
    Path applicationFile = Path.of(applicationRoot.toAbsolutePath().toString(), APPLICATION_DESCRIPTION_FILE);
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      byte[] fileContent = FileUtils.readFileAsString(applicationFile.toFile()).getBytes(Constants.DEFAULT_ENCODING);
      saxParser.parse(new ByteArrayInputStream(fileContent), handler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return handler.getApplicationXmlEntry();
  }


  private Pair<RuntimeContext, Set<RuntimeContext>> readAppMetaData(Path applicationRoot) {
    ApplicationXmlEntry applicationXml = readApplicationXml(applicationRoot);
    Application app = new Application(applicationXml.getApplicationName(), applicationXml.getVersionName());
    Stream<RuntimeContextRequirementXmlEntry> depStream = applicationXml.getRuntimeContextRequirements().stream();
    Set<RuntimeContext> dependencies = depStream.map(this::createApplication).collect(Collectors.toSet());
    dependencies = dependencies == null ? new HashSet<RuntimeContext>() : dependencies;
    return Pair.of(app, dependencies);
  }


  private Application createApplication(RuntimeContextRequirementXmlEntry x) {
    return new Application(x.getApplication(), x.getVersion());
  }


  private Set<GenerationBase> collectXmomObjects(XMOMCompilationData data, Set<RuntimeContext> revisions, FileSystemXMLSource source) {
    GenerationBaseCache cache = new GenerationBaseCache();
    Set<GenerationBase> datatypes = new HashSet<>();
    Set<XMOMType> types = data.getAllowedTypes();
    for (RuntimeContext relevantRevision : revisions) {
      datatypes.addAll(instantiateAll(types, relevantRevision, source, cache));
    }
    return datatypes;
  }


  private Collection<? extends GenerationBase> instantiateAll(Set<XMOMType> types, RuntimeContext revision, FileSystemXMLSource source,
                                                              GenerationBaseCache cache) {
    try {
      File xmomPath = source.getXMOMPath(revision);
      Set<String> keysInApplicationXML = createKeysFromApplicationXml(xmomPath, types);
      Predicate<Path> filterFunction = createCheckXmomFunction(types, keysInApplicationXML, xmomPath);
      Path path = xmomPath.toPath();
      return Files.walk(path).filter(filterFunction).map(p -> instantiate(p, revision, source, cache)).collect(Collectors.toSet());
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("source: " + source);
      System.out.println("relevantRevision: " + revision);
      throw new RuntimeException(e);
    }
  }


  private Set<String> createKeysFromApplicationXml(File pathToRevision, Set<XMOMType> allowedTypes) {
    Set<String> result = new HashSet<String>();
    ApplicationXmlEntry appEntry = readApplicationXml(pathToRevision.toPath().getParent());
    for (XMOMXmlEntry entry : appEntry.getXmomEntries()) {
      XMOMType type = XMOMType.valueOf(entry.getType());
      if (allowedTypes.contains(type)) {
        result.add(createKey(entry.getFqName(), type));
      }
    }

    return result;
  }


  private Predicate<Path> createCheckXmomFunction(Set<XMOMType> allowedTypes, Set<String> keysInApplicationXML, File xmomPath) {
    return x -> checkXmom(x, allowedTypes, keysInApplicationXML, xmomPath);
  }


  private Boolean checkXmom(Path path, Set<XMOMType> allowedTypes, Set<String> keysInApplicationXML, File xmomPath) {
    if (!path.toFile().isFile() || !path.getFileName().toString().endsWith(".xml")) {
      return false;
    }

    try {
      String fqName = FileUtils.deriveFqNameFromPath(xmomPath, path);
      XMOMType type = XMOMType.getXMOMTypeByFile(path.toFile());
      String key = createKey(fqName, type);
      return allowedTypes.contains(type) && keysInApplicationXML.contains(key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  private String createKey(String fqName, XMOMType type) {
    return type.toString() + ":" + fqName;
  }


  private GenerationBase instantiate(Path path, RuntimeContext currentRevision, FileSystemXMLSource source, GenerationBaseCache cache) {
    try {
      String fqName = FileUtils.deriveFqNameFromPath(source.getXMOMPath(currentRevision), path);
      XMOMType type = XMOMType.getXMOMTypeByFile(path.toFile());
      switch (type) {
        case DATATYPE :
          return DOM.getOrCreateInstance(fqName, cache, source.getRevision(currentRevision), source);
        case EXCEPTION :
          return ExceptionGeneration.getOrCreateInstance(fqName, cache, source.getRevision(currentRevision), source);
        case WORKFLOW :
          return WF.getOrCreateInstance(fqName, cache, source.getRevision(currentRevision), source);
        default :
          throw new RuntimeException("Not of an accepted type: " + type);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings("deprecation")
  private void setStorableInterfaces(List<String> storableInterfaces) {
    if (!storableInterfaces.isEmpty()) {
      try {
        DOM.storableInterfaces.get();
        StringSerializableList<String> list = StringSerializableList.autoSeparator(String.class, ",");
        list.addAll(storableInterfaces);
        DOM.storableInterfaces.set(list);
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException(e1);
      }
    }
  }


  public static class XMOMCompilationData {

    private String ApplicationNameAndVersion; //for what application are we building
    private String outputPath; //where to put the result
    private List<String> storableInterfaces; //xprc.xfractwfe.generation.storable.xmom.interfaces
    private List<String> sourcePaths; //where to find things to compile
    private Set<XMOMType> allowedTypes; //what to compile
    private boolean returnSingleFile; //should result be compressed into a jar file?
    private boolean recursive; //should XMOMs from other applications be included?

    private boolean printClasspath;


    public XMOMCompilationData() {
      storableInterfaces = new ArrayList<String>();
      sourcePaths = new ArrayList<String>();
    }


    public String getApplicationNameAndVersion() {
      return ApplicationNameAndVersion;
    }


    public void setApplicationNameAndVersion(String applicationNameAndVersion) {
      ApplicationNameAndVersion = applicationNameAndVersion;
    }


    public String getOutputPath() {
      return outputPath;
    }


    public void setOutputPath(String outputPath) {
      this.outputPath = outputPath;
    }


    public List<String> getStorableInterfaces() {
      return storableInterfaces;
    }


    public void setStorableInterfaces(List<String> storableInterfaces) {
      this.storableInterfaces = storableInterfaces;
    }


    public List<String> getSourcePaths() {
      return sourcePaths;
    }


    public void setSourcePaths(List<String> sourcePaths) {
      this.sourcePaths = sourcePaths;
    }


    public Set<XMOMType> getAllowedTypes() {
      return allowedTypes;
    }


    public void setAllowedTypes(Set<XMOMType> allowedTypes) {
      this.allowedTypes = allowedTypes;
    }


    public boolean isReturnSingleFile() {
      return returnSingleFile;
    }


    public void setReturnSingleFile(boolean returnSingleFile) {
      this.returnSingleFile = returnSingleFile;
    }


    public boolean isRecursive() {
      return recursive;
    }


    public void setRecursive(boolean recursive) {
      this.recursive = recursive;
    }


    public boolean isPrintClasspath() {
      return printClasspath;
    }


    public void setPrintClasspath(boolean printClasspath) {
      this.printClasspath = printClasspath;
    }
  }
}
