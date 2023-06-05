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
package xfmg.xfctrl.filemgmt.impl;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import xact.templates.Document;
import xact.templates.PlainText;
import xfmg.xfctrl.filemgmt.ManagedFileSuperProxy;
import xfmg.xfctrl.filemgmt.ManagedFileInstanceOperation;
import xfmg.xfctrl.filemgmt.Charset;
import xfmg.xfctrl.filemgmt.ManagedFile;


public class ManagedFileInstanceOperationImpl extends ManagedFileSuperProxy implements ManagedFileInstanceOperation {

  private static final long serialVersionUID = 1L;

  public ManagedFileInstanceOperationImpl(ManagedFile instanceVar) {
    super(instanceVar);
  }
  
  private java.io.File asFile() {
    return new java.io.File(this.getInstanceVar().getPath());
  }

  public void create(XynaOrderServerExtension correlatedOrder) {
    java.io.File file = asFile();
    ManagedFileServiceOperationImpl.checkRight(file, Action.insert, correlatedOrder.getCreationRole());
    try {
      if (!file.exists()) {
        if (file.getParentFile() != null) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
      } else {
        // TODO would like to return false in those cases, but then the method won't be callable direktly
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(XynaOrderServerExtension correlatedOrder) {
    java.io.File file = asFile();
    ManagedFileServiceOperationImpl.checkRight(file, Action.delete, correlatedOrder.getCreationRole());
    if (file.exists()) {
      if (!file.delete()) {
        // TODO throw or return false
      }
    }
  }

  public Boolean exists(XynaOrderServerExtension correlatedOrder) {
    java.io.File file = asFile();
    ManagedFileServiceOperationImpl.checkRight(file, Action.read, correlatedOrder.getCreationRole());
    return file.exists();
  }

  public Document readIntoDocument(XynaOrderServerExtension correlatedOrder) {
    java.io.File file = asFile();
    ManagedFileServiceOperationImpl.checkRight(file, Action.read, correlatedOrder.getCreationRole());
    try {
      String content = FileUtils.readFileAsString(file);
      return new Document(new PlainText(), content);
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Document readIntoDocument_1(XynaOrderServerExtension correlatedOrder, Charset charSet) {
    java.io.File file = asFile();
    preventNullCharset(charSet);
    ManagedFileServiceOperationImpl.checkRight(file, Action.read, correlatedOrder.getCreationRole());
    try {
      String content = FileUtils.readFileAsString(file, false, charSet.getCharsetName());
      return new Document(new PlainText(), content);
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  public void writeFromDocument(XynaOrderServerExtension correlatedOrder, Document document) {
    java.io.File file = asFile();
    ManagedFileServiceOperationImpl.checkRight(file, Action.write, correlatedOrder.getCreationRole());
    try {
      FileUtils.writeStringToFile(document.getText(), file);
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void writeFromDocument_1(XynaOrderServerExtension correlatedOrder, Document document, Charset charset) {
    java.io.File file = asFile();
    preventNullCharset(charset);
    ManagedFileServiceOperationImpl.checkRight(file, Action.write, correlatedOrder.getCreationRole());
    try {
      FileUtils.writeStringToFile(document.getText(), file, charset.getCharsetName());
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  private void preventNullCharset(Charset charset) {
    if (charset == null || charset.getCharsetName() == null || charset.getCharsetName().length() == 0) {
      charset = new Charset();
      charset.setCharsetName(Constants.DEFAULT_ENCODING);
    }
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

  @Override
  public void appendDocument(XynaOrderServerExtension correlatedOrder, Document document, Charset charset) {
    java.io.File file = asFile();
    preventNullCharset(charset);
    ManagedFileServiceOperationImpl.checkRight(file, Action.write, correlatedOrder.getCreationRole());
    try {
      FileUtils.appendStringToFile(document.getText(), file, charset.getCharsetName());
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }
}
