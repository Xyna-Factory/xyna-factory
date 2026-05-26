/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.sftp.impl;


import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xfmg.Constants;

import xact.sftp.Content;
import xact.sftp.ContentInstanceOperation;
import xact.sftp.ContentSuperProxy;


public class ContentInstanceOperationImpl extends ContentSuperProxy implements ContentInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  private static enum ContentType {
    STRING("String"), BYTES("Bytes");
    
    private final String typeIdentifier;
   
    private ContentType(String typeIdentifier) {
      this.typeIdentifier = typeIdentifier;
    }
    
    public static ContentType byIdentifier(String typeIdentifier) {
      for (ContentType value : values()) {
        if (value.typeIdentifier.equalsIgnoreCase(typeIdentifier)) {
          return value;
        }
      }
      return STRING;
    }
  }
  
  private byte[] raw;

  public ContentInstanceOperationImpl(Content instanceVar) {
    super(instanceVar);
  }

  public List<Integer> getRawContent() {
    byte[] rawContent;
    switch (byIdentifier()) {
      case BYTES :
        rawContent = raw;
        break;
      case STRING :
        rawContent = toByte(instanceVar.getContent() == null ? "" : instanceVar.getContent());
        break;
      default :
        throw new IllegalArgumentException("Illegal type identifier: " + String.valueOf(instanceVar.getType()));
    }
    @SuppressWarnings("unchecked")
    var container = (List<Integer>) (List<?>) Arrays.asList(rawContent);
    return container;
  }

  
  public void setRawContent(List<Integer> list) {
    Object untyped = ((List<?>)list).get(0);
    if (untyped instanceof byte[]) {
      raw = (byte[]) untyped;
    } else if (untyped != null) {
      raw = toByte(untyped.toString());
    } // else leave it unset
  }
  
  
  public void calculateSize() {
    switch (byIdentifier()) {
      case BYTES :
        instanceVar.setSize(raw.length);
        break;
      case STRING :
        instanceVar.setSize(toByte(instanceVar.getContent() == null ? "" : instanceVar.getContent()).length);
        break;
      default :
        throw new IllegalArgumentException("Illegal type identifier: " + String.valueOf(instanceVar.getType()));
    }
  }
  
  
  private static byte[] toByte(String value) {
    try {
      return value.getBytes(Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported default enconding!", e);
    }
  }
  
  private ContentType byIdentifier() {
    return ContentType.byIdentifier(instanceVar.getType());
  }
  
  

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


}
