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
package com.gip.xyna.xprc.xfractwfe.generation.compile;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaCompiler.CompilationTask;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.xfcli.impl.ListsysteminfoImpl;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public class InMemoryCompilationSet implements CompilationSet {
  
  public static enum TargetKind {
    FILE, MEMORY;
  }

  private final Set<String> additionalLibs = new HashSet<String>();
  final Map<String, JavaFileObject> compilationTargets = new TreeMap<String, JavaFileObject>();
  final List<JavaSourceFromString> compilationTargetsOrdered = new ArrayList<JavaSourceFromString>();
  private String classDir;
  private final boolean crossCompile;
  private final boolean compileWithDebug;
  private final boolean proceedOnError;
  private final TargetKind targetKind;
  final List<String> success = new ArrayList<String>();
  final Map<String, XPRC_CompileError> failed = new HashMap<>();
  private final JavaCompiler compiler; 
  private final CompilationFileManager stfm;
  

  public InMemoryCompilationSet(boolean crossCompile, boolean compileWithDebug, boolean proceedOnError) {
    this(crossCompile, compileWithDebug, proceedOnError, TargetKind.FILE);
  }
  
  public InMemoryCompilationSet(boolean crossCompile, boolean compileWithDebug, boolean proceedOnError, TargetKind targetKind) {
    this.crossCompile = crossCompile;
    this.compileWithDebug = compileWithDebug;
    this.proceedOnError = proceedOnError;
    this.targetKind = targetKind;
    compiler = Compilation.getCompiler(proceedOnError);
    CompileErrorCollector dia = new CompileErrorCollector(this);
    StandardJavaFileManager base = 
      compiler.getStandardFileManager(dia, Constants.DEFAULT_LOCALE, Charset.forName(Constants.DEFAULT_ENCODING));
    stfm = new CompilationFileManager(this, base, dia, targetKind);
  }

  /**
   * classdir, in das die classfiles generiert werden.
   * wirkt sich nur auf sources aus, die nach dem setzen des classdirs zum compilationset hinzugefügt werden.
   * 
   * alternativ kann für jede javasource ein eigenes classdir angegeben werden @link{JavaSourceFromString#setClassOutputLocation(String)}
   * @param classDir
   */
  public void setClassDir(String classDir) {
    this.classDir = classDir;
    File f = new File(classDir);
    if (!f.exists()) {
      f.mkdirs();
    }
  }

  public CompilationResult compile() throws XPRC_CompileError {
    if (compilationTargets.size() == 0) {
      return CompilationResult.empty();
    }
    String classPath = getClassPath();

    String targetVersion = null;
    if (crossCompile) {
      String javaVersion = XynaProperty.BUILDMDJAR_JAVA_VERSION.get();
      //FIXME wieso ist die javaversion in der property nicht einfach gleich der targetversion?
      if (javaVersion.equals("Java6")) {
        targetVersion = "1.6";
      } else if (javaVersion.equals("Java7")) {
        targetVersion = "1.7";
      } else if (javaVersion.equals("Java8")) {
        targetVersion = "1.8";
      } else if (javaVersion.equals("Java9")) {
        targetVersion = "1.9";
      } else if (javaVersion.equals("Java11")) {
        targetVersion = "11";
      } else {
        throw new RuntimeException("Xyna Property " + XynaProperty.BUILDMDJAR_JAVA_VERSION.getPropertyName() + " has invalid value: " + javaVersion);
      }
    } else {
      switch (ListsysteminfoImpl.getJavaVersion()) {
        case 7 :
          targetVersion = "1.7";
          break;
        case 8 :
          targetVersion = "1.8";
          break;
        case 9 :
          targetVersion = "1.9";
          break;
        case 11 :
          targetVersion = "11";
          break;
        default :
          throw new RuntimeException("Unsupported java version: " + ListsysteminfoImpl.getJavaVersion());
      }
    }
    
    
    if (compiler == null) {
      throw new RuntimeException("Failed to determine compiler");
    }
    List<String> options = new ArrayList<String>();
    options.add("-encoding");
    options.add(Constants.DEFAULT_ENCODING);
    options.add("-classpath");
    options.add(classPath);
    if (classDir != null) {
      options.add("-d");
      options.add(classDir);
    }
    if (targetVersion != null) {
      options.add("-target");
      options.add(targetVersion);
      options.add("-source");
      options.add(targetVersion);
    }
    options.add("-nowarn");
   // options.add("-verbose");
    if (compileWithDebug) {
      options.add("-g:lines,vars,source");
    }
    
    if (proceedOnError && 
        compiler.isSupportedOption(Compilation.OPTION_NAME_PROCEED_ON_ERROR) >= 0) {
      options.add(Compilation.OPTION_NAME_PROCEED_ON_ERROR);
    }
    /*
     * TODO
     * sidetargetsupport mit:
     * 
     * -implicit:{class,none}
     *  Controls the generation of class files for implicitly loaded source files. To automatically generate class files, use -implicit:class. 
     *  To suppress class file generation, use -implicit:none. If this option is not specified, the default is to automatically generate class files.
     *  In this case, the compiler will issue a warning if any such class files are generated when also doing annotation processing. The warning will 
     *  not be issued if this option is set explicitly. See Searching For Types.
     *  
     *  
     * -Xprefer:{newer,source}
     *  Specify which file to read when both a source file and class file are found for a type. (See Searching For Types). If -Xprefer:newer is used, 
     *  it reads the newer of the source or class file for a type (default). If the -Xprefer:source option is used, it reads source file. Use 
     *  -Xprefer:source when you want to be sure that any annotation processors can access annotations declared with a retention policy of SOURCE. 
     */
    
    try {
      StringWriter sw = null;
      if (Compilation.logger.isDebugEnabled()) {
        sw = new StringWriter();
        StringBuilder sb = new StringBuilder();
        for (JavaFileObject f : compilationTargetsOrdered) {
          sb.append(f.getName()).append(" ");
        }
        String javaFiles = sb.toString().trim();
        Compilation.logger.debug("--------------- compiling " + javaFiles + " options=" + options.toString() + " ...");
      }
      CompilationTask task = compiler.getTask(sw, stfm, stfm.getErrorCollector(), options, null, compilationTargetsOrdered);

      synchronized (GenerationBase.class) { //keine compiles gleichzeitig ausführen (synchronisiert mit Main.compile())
        if (!task.call()) {
          // according to the java-doc we should have encountered an error and will throw once checking the diagnostics collector 
        }
      }
      if (Compilation.logger.isDebugEnabled() && sw != null) {
        Compilation.logger.debug(sw.toString());
      }
    } finally {
      try {
        stfm.close();
      } catch (IOException e) {
        Compilation.logger.warn("Failed to close " + stfm.getClass().getSimpleName(), e);
      }
    }
    if (stfm.getErrorCollector().getCollectedErrors().size() > 0) {
      XPRC_CompileError e = stfm.getErrorCollector().getCollectedErrors().get(0);
      throw e;
    }
    
    return new CompilationResult(stfm.getAllJavaOutput());
  }



  public void compileToJar(File jarFile, boolean includeSource) throws XPRC_CompileError, Ex_FileAccessException {
    File tmpDir = null;
    boolean tmp = classDir == null;
    try {
      if( tmp ) {
        tmpDir = FileUtils.makeTemporaryDirectory();
        String outputDir = tmpDir.getPath();
        setClassDir(outputDir);
        for (JavaSourceFromString src : compilationTargetsOrdered) {
          src.setClassOutputLocation(outputDir);
        }
      } else {
        tmpDir = new File(classDir);
      }
      compileToJarInternal(tmpDir, jarFile, includeSource);
    } catch( IOException e ) {
      throw new RuntimeException(e); 
    } finally {
      if( tmp && tmpDir != null ) {
        FileUtils.deleteDirectoryRecursively(tmpDir);
      }
    }
  }
  
  private void compileToJarInternal(File tmpDir, File jarFile, boolean includeSource) throws XPRC_CompileError, Ex_FileAccessException {
    compile();

    if( includeSource ) {
      for( JavaSourceFromString src : compilationTargetsOrdered ) {
        FileUtils.writeStringToFile(new String(src.getCode()), new File(tmpDir, src.getName() ));
      }
    }

    FileUtils.zipDirectory(jarFile, tmpDir );
  }
  
  /**
   * reihenfolge in der files hier hinzugefügt werden, ist fürs kompilieren relevant
   */
  public void addToCompile(JavaSourceFromString source) {
    if (classDir != null) {
      source.setClassOutputLocation(classDir);
    }
    if (compilationTargets.containsKey(source.getFqClassName())) {
      Compilation.logger.trace("Second addition of: " + source.getFqClassName(),new RuntimeException());
    } else {
      compilationTargets.put(source.getFqClassName(), source);
      compilationTargetsOrdered.add(source);
    }
  }


  public void addToClassPath(String path) {
    additionalLibs.add(path);
  }
  
  
  public void addInMemoryClassFile(String fqName, JavaFileObject jvo) {
    stfm.addAdditionalClassFile(fqName, jvo);
  }


  public List<String> getSuccessfullyCompiled() {
    return success;
  }


  public Map<String, XPRC_CompileError> getUnsuccessfullyCompiled() {
    return failed;
  }
  
  
  public int size() {
    return compilationTargets.size();
  }


  public void clear() {
    stfm.getErrorCollector().getCollectedErrors().clear();
    compilationTargets.clear();
    failed.clear();
    success.clear();
    compilationTargetsOrdered.clear();
    additionalLibs.clear();
    classDir = null;
  }

  public String getClassPath() {
    String additionalLibsAsString = GenerationBase.flattenClassPathSet(additionalLibs);
    return GenerationBase.getJarFiles() + Constants.PATH_SEPARATOR + additionalLibsAsString;
  }

}