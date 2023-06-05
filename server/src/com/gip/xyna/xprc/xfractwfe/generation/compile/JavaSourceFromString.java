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
package com.gip.xyna.xprc.xfractwfe.generation.compile;

import java.net.URI;
import java.util.Objects;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;


public class JavaSourceFromString extends SimpleJavaFileObject {

  private final String code;
  private final String fqClassName;
  private String outputLocation;
  private final String domName;
  private final long revision;
  private final DeploymentMode mode;


  public JavaSourceFromString(String fqClassName, String code) {
    this(fqClassName, fqClassName, code, -1L);
  }
  
  public JavaSourceFromString(String fqClassName, String domName, String code, long revision) {
    this(fqClassName, domName, code, revision, DeploymentMode.codeChanged);
  }
  
  public JavaSourceFromString(String fqClassName, String domName, String code, long revision, DeploymentMode mode) {
    super(URI.create("string:///" + fqClassName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
    this.fqClassName = fqClassName;
    this.code = code;
    this.domName = domName;
    this.revision = revision;
    this.mode = mode;
  }


  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return code;
  }
  
  public String getCode() {
    return code;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    JavaSourceFromString other = (JavaSourceFromString) obj;
    return Objects.equals(getFqClassName(), other.getFqClassName()) &&
           Objects.equals(code, other.code);
  }


  @Override
  public int hashCode() {
    return Objects.hash(code, getFqClassName());
  }


  @Override
  public String toString() {
    return super.uri.toString().substring(9); //string:// vorne entfernen
  }
  
  public long getRevision() {
    return revision;
  }


  public String getFqClassName() {
    return fqClassName;
  }


  public DeploymentMode getMode() {
    return mode;
  }

  public String getDomName() {
    return domName;
  }
  
  /**
   * @param basedir der pfad in dem die erzeugten classfiles liegen sollen (also ohne den packagenamen der classfiles anzugeben)
   */
  public void setClassOutputLocation(String basedir) {
    outputLocation = basedir;
  }


  public JavaFileObject getCustomOutputLocation(String className) {
    if (outputLocation == null) {
      return null;
    }
    return new JavaClass(outputLocation, className);
  }


}