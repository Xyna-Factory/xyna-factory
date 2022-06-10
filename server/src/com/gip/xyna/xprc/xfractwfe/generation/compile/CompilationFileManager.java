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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet.TargetKind;

/*
 * unterstützt dynamische output verzeichnisse für die classfiles, indem man im javasourcefromstring target das outputdir angibt.
 * falls es nirgendwo angegeben wird, wird das outputdir vom compilationset verwendet
 */
class CompilationFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  protected final InMemoryCompilationSet imcs;
  protected final Map<String, Set<String>> compiledInnerClasses = new HashMap<String, Set<String>>();
  protected final List<JavaFileObject> allOutput = new ArrayList<>();
  protected final TargetKind targetKind;
  protected final Map<String, JavaFileObject> additionalClassFiles = new HashMap<String, JavaFileObject>();
  protected final CompileErrorCollector errorCollector; 

  public CompilationFileManager(InMemoryCompilationSet imcs, JavaFileManager target, CompileErrorCollector errorCollector, TargetKind targetKind) {
    super(target);
    this.imcs = imcs;
    this.targetKind = targetKind;
    this.errorCollector = errorCollector;
  }


  @Override
  public String inferBinaryName(Location location, JavaFileObject file) {
    if (file instanceof JavaSourceFromString) {
      return ((JavaSourceFromString) file).getFqClassName();
    } else if (file instanceof JavaMemoryObject) {
      return ((JavaMemoryObject) file).getFqClassName();
    } else {
      return super.inferBinaryName(location, file);
    }
  }


  @Override
  public boolean hasLocation(Location location) {
    return super.hasLocation(location);
  }


  @Override
  public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
    //TODO müsste man hier eigtl den source auch zurückgeben, wenn className eine innere klasse davon ist?! bei tests wurde dieser methode allerdings nie aufgerufen!
    JavaFileObject jfo = imcs.compilationTargets.get(className);
    if (jfo != null) {
      if (kind == Kind.SOURCE) {
        return jfo;
      }
      Compilation.logger.warn("asking for target with wrong kind: " + className + ", kind=" + kind + ", location=" + location);
      return null;
    } else {
      return super.getJavaFileForInput(location, className, kind);
    }
  }


  @Override
  public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
    if (kind == Kind.CLASS) {
      // some compilers have been seen to pass file instead of class names
      if (className.contains(Constants.FILE_SEPARATOR)) {
        className = className.replaceAll(Constants.FILE_SEPARATOR, ".");
      }
      int idx = className.indexOf('$');
      String classNameBase = className;
      if (idx > -1) {
        classNameBase = className.substring(0, idx);

        Set<String> innerClasses = compiledInnerClasses.get(classNameBase);
        if (innerClasses == null) {
          innerClasses = new HashSet<String>();
          compiledInnerClasses.put(classNameBase,  innerClasses);
        }
        String simpleClassName = GenerationBase.getSimpleNameFromFQName(classNameBase);
        innerClasses.add(simpleClassName + className.substring(idx) + ".class");
      }
      
      JavaFileObject jfo = imcs.compilationTargets.get(classNameBase);
      if (jfo != null) {
        if (jfo instanceof JavaSourceFromString) {
          JavaSourceFromString jsfs = (JavaSourceFromString) jfo;
          JavaFileObject ret = jsfs.getCustomOutputLocation(className);
          if (ret == null) {
            String outputlocation = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, jsfs.getRevision());
            if (outputlocation != null) {
              if (jsfs.getMode() == DeploymentMode.generateMdmJar) {
                outputlocation += ".tmp";
              }
              ret = createTargetFileObject(outputlocation, className, jsfs.getDomName(), jsfs.getRevision());
            }
          }
          if (idx == -1) {
            imcs.success.add(className);
            
            //entferne classfiles von inneren klassen von früheren compiles             
            if (ret == null) {
              ret = super.getJavaFileForOutput(location, className, kind, sibling);
            }
            
            if (!(ret instanceof JavaMemoryObject)) {
              final String simpleClassName = GenerationBase.getSimpleNameFromFQName(classNameBase) + "$";
              File folder;
              if (ret.toUri().isAbsolute()) {
                folder = new File(ret.toUri()).getParentFile();
              } else {
                folder = new File(ret.toUri().getPath()).getAbsoluteFile().getParentFile();
              }
              final Set<String> innerClasses = compiledInnerClasses.get(classNameBase);
              String[] fileNamesFromPreviousCompilations = folder.list(new FilenameFilter() {
  
                public boolean accept(File dir, String name) {
                  if (name.startsWith(simpleClassName) && name.endsWith(".class") && !(innerClasses != null && innerClasses.contains(name))) {
                    return true;
                  }
                  return false;
                }
              });
              if (fileNamesFromPreviousCompilations != null) {
                for (String fileToBeDeleted : fileNamesFromPreviousCompilations) {
                  new File(folder, fileToBeDeleted).delete();
                }
              }
            }
          }
          if (ret != null) {
            allOutput.add(ret);
            return ret;
          }
        } 
      }/* else {  // TODO would we be correct here...shouldn't it be for input? 
        if (additionalClassFiles.containsKey(className)) {
          return additionalClassFiles.get(className);
        }
      }*/
    }
    JavaFileObject ret = super.getJavaFileForOutput(location, className, kind, sibling);
    if (ret != null) {
      allOutput.add(ret);
    }
    return ret;
  }
  
  
  private JavaFileObject createTargetFileObject(String outputlocation, String fqClassName, String datatypeName, Long revision) {
    switch (targetKind) {
      case FILE:
        return new JavaClass(outputlocation, fqClassName);
      case MEMORY:
        return new JavaMemoryObject(fqClassName, fqClassName, datatypeName, revision);
      default :
        return null;
    }
  }


  @Override
  public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
    Iterable<JavaFileObject> normal = super.list(location, packageName, kinds, recurse); //-> gibt liste von classfiles zurück - inkl innerer klassen
    normal = appendAdditionalFiles(normal, packageName, recurse);
    if (!kinds.contains(Kind.SOURCE)) {
      return normal;
    }

    //idee: falls objekt als class gefunden wird, trotzdem nur als source zurückgeben, wenn es ein compilationtarget ist. 
    List<JavaFileObject> list = new ArrayList<JavaFileObject>(); //JavaFileObject hat keine equals-methode
    String pn = packageName + ".";
    for (JavaFileObject fileSystemObject : normal) {
      String className = deriveSimpleClassName(fileSystemObject);
      if (!imcs.compilationTargets.containsKey(pn + className)) {
        list.add(fileSystemObject);
      }
    }
    for (JavaSourceFromString jfo : imcs.compilationTargetsOrdered) {
      String fqName = jfo.getFqClassName();
      if (recurse) {
        if (fqName.startsWith(pn)) {
          list.add(jfo);
        }
      } else if (GenerationBase.getPackageNameFromFQName(fqName).equals(packageName)) {
        list.add(jfo);
      }
    }
    return list;
  }
  
  
  private List<JavaFileObject> appendAdditionalFiles(Iterable<JavaFileObject> normal, String packageName, boolean recurse) {
    List<JavaFileObject> files = new ArrayList<>();
    for (JavaFileObject javaFileObject : normal) {
      files.add(javaFileObject);
    }
    for (String fqName : additionalClassFiles.keySet()) {
      if (recurse) {
        if (fqName.startsWith(packageName + ".")) {
          files.add(additionalClassFiles.get(fqName));
        }
      } else {
        if (GenerationBase.getPackageNameFromFQName(fqName).equals(packageName)) {
          files.add(additionalClassFiles.get(fqName));
        }
      }
    }
    return files;
  }


  private static String deriveSimpleClassName(JavaFileObject jfo) {
    URI uri = jfo.toUri();
    String scheme = uri.getScheme();
    String className;
    if ("file".equals(scheme) ||
        scheme == null) {
      String schemeSpecific = uri.getSchemeSpecificPart();
      className = schemeSpecific.substring(schemeSpecific.lastIndexOf(Constants.FILE_SEPARATOR) + 1);
    } else if ("jar".equals(scheme)) {
      String schemeSpecific = uri.getSchemeSpecificPart();
      className = schemeSpecific.substring(schemeSpecific.lastIndexOf('!') + 1);
      className = className.substring(className.lastIndexOf(Constants.FILE_SEPARATOR) + 1);
    } else if ("string".equals(scheme)) {
      String schemeSpecific = uri.getSchemeSpecificPart();
      className = schemeSpecific.substring(schemeSpecific.lastIndexOf('!') + 1);
      className = className.substring(className.lastIndexOf(Constants.FILE_SEPARATOR) + 1);
    } else if ("jrt".equals(scheme)) { // JavaRunTime "/modules/java.base/java/util/HashMap$KeySpliterator.class"
      return jfo.getName();
    } else {
      Compilation.logger.debug("Unknown JavaFileObject.scheme encountered: " + scheme + " for file " + jfo.getName());
      return jfo.getName();
    }
    
    int idx = className.indexOf('$');
    if (idx > 0) { //klassenname muss mindestens ein zeichen sein
      className = className.substring(0, idx); //innere klasse abschneiden
    } else {
      className = className.substring(0, className.length() - 6); //.class abschneiden
    }
    return className;
  }

  
  public void addAdditionalClassFile(String fqName, JavaFileObject jfo) {
    additionalClassFiles.put(fqName, jfo);
  }

  public Collection<JavaFileObject> getAllJavaOutput() {
    return Collections.unmodifiableCollection(allOutput);
  }


  public CompileErrorCollector getErrorCollector() {
    return errorCollector;
  }


  
}