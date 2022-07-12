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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.tools.SimpleJavaFileObject;

class JavaClass extends SimpleJavaFileObject {

  protected JavaClass(String basedir, String fqClassName) {
    super(new File(basedir + (!basedir.endsWith("/") ? "/" : "") + fqClassName.replace('.', '/') + ".class").toURI(), Kind.CLASS);
  }


  @Override
  public OutputStream openOutputStream() throws IOException {
    File f = new File(toUri());
    if (!f.exists()) {
      f.getParentFile().mkdirs();
      f.createNewFile();
    }
    return new FileOutputStream(f);
  }


  @Override
  public Writer openWriter() throws IOException {
    return new OutputStreamWriter(openOutputStream());
  }

}