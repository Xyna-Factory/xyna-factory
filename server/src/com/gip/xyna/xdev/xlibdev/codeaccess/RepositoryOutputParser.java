/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xdev.xlibdev.codeaccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.FileUpdate;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ModificationType;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification.RepositoryModificationType;
import com.gip.xyna.xfmg.Constants;


public class RepositoryOutputParser {
  
  private final CodeAccess codeAccess;
  
  public RepositoryOutputParser(CodeAccess codeAccess) {
    this.codeAccess = codeAccess;
    prepareRegex();
  }


  private Pattern relevantJavaFilePattern;
  private Pattern relevantJarFilePattern;
  /**
   * sharedlibs, userlibs, globallibs = library-bundles
   */
  private Pattern directLibraryBundleChange;
  private Pattern indirectLibraryBundleChange;
  private Pattern componentTypeAndNamePattern;
  
  
  private void prepareRegex() {
    String svnProjectDir = toCanonicalPath(codeAccess.getProjectDir());
    StringBuilder patternBuilder = new StringBuilder();
    patternBuilder.append(svnProjectDir)
                  .append(Constants.FILE_SEPARATOR)
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+")
                  .append(Constants.FILE_SEPARATOR)
                  .append("){2}src")
                  .append(Constants.FILE_SEPARATOR)
                  .append(".*\\.java$");
    relevantJavaFilePattern = Pattern.compile(patternBuilder.toString());
    patternBuilder = new StringBuilder();
    patternBuilder.append(svnProjectDir)
                  .append(Constants.FILE_SEPARATOR)
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+")
                  .append(Constants.FILE_SEPARATOR)
                  .append("){2}lib")
                  .append(Constants.FILE_SEPARATOR)
                  .append(".*\\.jar$");
    relevantJarFilePattern = Pattern.compile(patternBuilder.toString());
    patternBuilder = new StringBuilder();
    patternBuilder.append(svnProjectDir)
                  .append(Constants.FILE_SEPARATOR)
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+")
                  .append(Constants.FILE_SEPARATOR)
                  .append("){2}")
                  .append("[^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+\\.jar$");
    directLibraryBundleChange = Pattern.compile(patternBuilder.toString());
    patternBuilder = new StringBuilder();
    patternBuilder.append(svnProjectDir)
                  .append(Constants.FILE_SEPARATOR)
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+")
                  .append(Constants.FILE_SEPARATOR)
                  .append("){2}")
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+)($|") //$ = ende, wenn z.b. eine teil-componente gelöscht wird
                  .append(Constants.FILE_SEPARATOR)
                  .append("src")
                  .append(Constants.FILE_SEPARATOR)
                  .append(".+\\.java$)");
    indirectLibraryBundleChange = Pattern.compile(patternBuilder.toString());
    patternBuilder = new StringBuilder();
    patternBuilder.append(svnProjectDir)
                  .append(Constants.FILE_SEPARATOR)
                  .append("(");
    for (int i = 0; i < ComponentType.values().length; i++) {
      patternBuilder.append(ComponentType.values()[i].getProjectSubFolder());
      if (i + 1 < ComponentType.values().length) {
        patternBuilder.append("|");
      }
    }
    patternBuilder.append(")")
                  .append(Constants.FILE_SEPARATOR)
                  .append("([^")
                  .append(Constants.FILE_SEPARATOR)
                  .append("]+)($|")
                  .append(Constants.FILE_SEPARATOR)
                  .append("(.*)$)");
    componentTypeAndNamePattern = Pattern.compile(patternBuilder.toString());
  }
  
  
  private static String toCanonicalPath(String path) {
    File file = new File(path);
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalArgumentException(path, e);
    }
  }

  
  /**
   * aggregiert änderungen an files zu änderungen an xyna components 
   */
  public List<ComponentCodeChange> parseModifiedComponents(List<RepositoryItemModification> modifiedFiles) {
    Map<String, EnumMap<ComponentType, ComponentCodeChange>> changes = new HashMap<String, EnumMap<ComponentType, ComponentCodeChange>>();
    for (RepositoryItemModification repositoryItemModification : modifiedFiles) {
      String canonicalModifiedFileName = toCanonicalPath(repositoryItemModification.getFile());
      Matcher componentMatcher = componentTypeAndNamePattern.matcher(canonicalModifiedFileName);
      if (componentMatcher.find() && componentMatcher.groupCount() >= 2) {
        ComponentType type = null;
        String componentName = componentMatcher.group(2);
        String componentFolderName = componentMatcher.group(1);
        for (ComponentType compType : ComponentType.values()) {
          if (compType.getProjectSubFolder().equals(componentFolderName)) {
            type = compType;
            break;
          }
        }
        if (type == null) {
          continue;
        }
        if (componentMatcher.groupCount() >= 3 && componentMatcher.group(3).length() > 0) {
          EnumMap<ComponentType, ComponentCodeChange> componentNameMap = changes.get(componentName);
          if (componentNameMap == null) {
            componentNameMap = new EnumMap<ComponentType, ComponentCodeChange>(ComponentType.class);
            changes.put(componentName, componentNameMap);
          }
          ComponentCodeChange change = componentNameMap.get(type);
          if (change == null) {
            change = new ComponentCodeChange(componentName, type, ModificationType.Modified);
          }
          switch (type) {
            case SHARED_LIB :
            case GLOBAL_LIB :
            case USER_LIB :
              Matcher directLibraryBundleMatcher = directLibraryBundleChange.matcher(canonicalModifiedFileName);
              if (directLibraryBundleMatcher.matches()) {
                ModificationType mod;
                switch (repositoryItemModification.getModification()) {
                  case Deleted :
                    mod = ModificationType.Deleted;
                    break;
                  default :
                    mod = ModificationType.Modified;
                    break;
                }
                change.addModifiedJars(new FileUpdate(new File(canonicalModifiedFileName), mod));
                componentNameMap.put(type, change);
              } else {
                Matcher indirectLibraryBundleMatcher = indirectLibraryBundleChange.matcher(canonicalModifiedFileName);
                if (indirectLibraryBundleMatcher.matches()) { // group(2) is subComponent name | group 3 is rest and can be checked for removal of sub
                  componentNameMap.put(type, change);
                  String subComponentName = indirectLibraryBundleMatcher.group(2);
                  ComponentCodeChange subChange = null;
                  for (ComponentCodeChange presentSubChange : change.getSubComponentChanges()) {
                    if (presentSubChange.getComponentOriginalName().equals(subComponentName)) {
                      subChange = presentSubChange;
                    }
                  }
                  if (subChange == null) {
                    subChange = new ComponentCodeChange(subComponentName, ComponentType.SHARED_LIB /* or null ?*/, ModificationType.Modified);
                    change.addChangedSubComponent(subChange);
                  }
                  if (indirectLibraryBundleMatcher.groupCount() >= 3 && indirectLibraryBundleMatcher.group(3).length() > 0) {
                    subChange.addModifiedJavaFiles(repositoryItemModification);
                  } else if(repositoryItemModification.getModification() != RepositoryModificationType.Added){
                    subChange.setModificationType(ModificationType.Deleted);
                  }
                }
              }
              break;

            default :
              Matcher relevantJavaMatcher = relevantJavaFilePattern.matcher(canonicalModifiedFileName);
              if (relevantJavaMatcher.matches()) {
                change.addModifiedJavaFiles(repositoryItemModification);
                componentNameMap.put(type, change);
              } else {
                Matcher relevantJarMatcher = relevantJarFilePattern.matcher(canonicalModifiedFileName);
                if (relevantJarMatcher.matches()) {
                  ModificationType mod;
                  switch (repositoryItemModification.getModification()) {
                    case Deleted :
                      mod = ModificationType.Deleted;
                      break;
                    default :
                      mod = ModificationType.Modified;
                      break;
                  }
                  change.addModifiedJars(new FileUpdate(new File(canonicalModifiedFileName), mod));
                  componentNameMap.put(type, change);                  
                }
              }
              break;
          }
        } else if (repositoryItemModification.getModification() == RepositoryModificationType.Deleted) { // thats assuming we don't store files directly in componentType folders
          EnumMap<ComponentType, ComponentCodeChange> map = new EnumMap<ComponentType, ComponentCodeChange>(ComponentType.class);
          map.put(type, new ComponentCodeChange(componentName, type, ModificationType.Deleted));
          changes.put(componentName, map);
        }
      }
    }
    List<ComponentCodeChange> cccs = new ArrayList<ComponentCodeChange>();
    for (EnumMap<ComponentType, ComponentCodeChange> entries : changes.values()) {
      cccs.addAll(entries.values());
    }
    return cccs;
  }
  
}
