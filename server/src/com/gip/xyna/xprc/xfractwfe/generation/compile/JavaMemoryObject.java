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

package com.gip.xyna.xprc.xfractwfe.generation.compile;


import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;


public class JavaMemoryObject implements JavaFileObject {

  private ByteArrayOutputStream bos;
  private URI uri;
  private String className;
  private String baseDataTypeFqName;
  private Long revision;
  private ByteBuffer bytes;

  public JavaMemoryObject(String fqClassName, DOM baseType, ByteBuffer bytes) {
    this(fqClassName, fqClassName, baseType.getFqClassName(), baseType.getRevision());
    this.bytes = bytes;
  }

  public JavaMemoryObject(String fileName, String fqClassName, String baseDataTypeFqName, Long revision) {
    this.uri = URI.create("string:///" + fileName.replace('.', '/')+ Kind.CLASS.extension);
    bos = new ByteArrayOutputStream();
    this.className = fqClassName;
    this.revision = revision;
    this.baseDataTypeFqName = baseDataTypeFqName;
  }


  public ByteBuffer getClassBytes() {
    if (bytes != null) {
      return bytes;
    } else {
      return ByteBuffer.wrap(bos.toByteArray());  
    }
  }

  @Override
  public final URI toUri() {
    return uri;
  }

  @Override
  public final String getName() {
    return uri.getPath();
  }

  @Override
  public final InputStream openInputStream() throws IOException {
    if (bytes != null) {
      return new ByteArrayInputStream(bytes.array());
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public final OutputStream openOutputStream() throws IOException {
    return bos;
  }

  @Override
  public final Reader openReader(boolean ignoreEncodingErrors) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Writer openWriter() throws IOException {
    return new OutputStreamWriter(openOutputStream());
  }

  @Override
  public final long getLastModified() {
    return 0L;
  }

  @Override
  public final boolean delete() {
    return false;
  }

  @Override
  public final Kind getKind() {
    return Kind.CLASS;
  }

  @Override
  public final boolean isNameCompatible(String simpleName, Kind fileKind) {
    String baseName = simpleName + Kind.CLASS.extension;
    return fileKind.equals(getKind()) && 
           (baseName.equals(toUri().getPath()) ||
            toUri().getPath().endsWith("/" + baseName));
  }

  @Override
  public final NestingKind getNestingKind() {
    return null;
  }

  @Override
  public final Modifier getAccessLevel() {
    return null;
  }

  public final String getFqClassName() {
    return className;
  }

  public final Long getRevision() {
    return revision;
  }

  @Override
  public final String toString() {
    return getClass().getName() + "[" + toUri() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JavaMemoryObject that = (JavaMemoryObject) o;
    return Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri);
  }

  public String getBaseDataTypeFqName() {
    return baseDataTypeFqName;
  }
}