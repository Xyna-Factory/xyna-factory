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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.List;



/**
 * mehrere codebuffer gleichzeitig mit ähnlichem code befüllen
 */
public class CodeBufferDelegation {
  
  private final static NOPCodeBuffer EMPTY_CODE_BUFFER = new NOPCodeBuffer();

  private List<CodeBuffer> buffers = new ArrayList<CodeBuffer>();
  
  public CodeBufferDelegation() {
  }
  
  public synchronized int addCodeBuffer(CodeBuffer buffer) {
    int bufferId = buffers.size();
    buffers.add(buffer);
    return bufferId;
  }
  
  public synchronized CodeBuffer getCodeBuffer(int bufferId) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      return buffers.get(bufferId);
    } else {
      return EMPTY_CODE_BUFFER;
    }
  }
  
  // to all
  
  public CodeBufferDelegation addLine(String ... s) {
    for (CodeBuffer buffer : buffers) {
      buffer.addLine(s);
    }
    return this;
  }
  
  
  public CodeBufferDelegation addLine(String s) {
    for (CodeBuffer buffer : buffers) {
      buffer.addLine(s);
    }
    return this;
  }

  public CodeBufferDelegation addLB() {
    for (CodeBuffer buffer : buffers) {
      buffer.addLB();
    }
    return this;
  }

  public CodeBufferDelegation add(String ... s) {
    for (CodeBuffer buffer : buffers) {
      buffer.add(s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(String s) {
    for (CodeBuffer buffer : buffers) {
      buffer.add(s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(CodeBuffer cb) {
    for (CodeBuffer buffer : buffers) {
      buffer.add(cb);
    }
    return this;
  }
  
  public CodeBufferDelegation addString(String s) {
    for (CodeBuffer buffer : buffers) {
      buffer.addString(s);
    }
    return this;
  }


  public CodeBufferDelegation addListElement(String s) {
    for (CodeBuffer buffer : buffers) {
      buffer.addListElement(s);
    }
    return this;
  }



  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (CodeBuffer buffer : buffers) {
      sb.append(buffer.toString());
    }
    return sb.toString();
  }

  public String toString(boolean withHeader) {
    StringBuilder sb = new StringBuilder();
    for (CodeBuffer buffer : buffers) {
      sb.append(buffer.toString(withHeader));
    }
    return sb.toString();
  }


  public CodeBufferDelegation setGIPSourceHeader(String departmentName) {
    for (CodeBuffer buffer : buffers) {
      buffer.setGIPSourceHeader(departmentName);
    }
    return this;
  }
  
  
  // to one
  
  public CodeBufferDelegation addLine(int bufferId, String ... s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).addLine(s);
    }
    return this;
  }
  
  
  public CodeBufferDelegation addLine(int bufferId, String s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).addLine(s);
    }
    return this;
  }

  public CodeBufferDelegation addLB(int bufferId) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).addLB();
    }
    return this;
  }

  public CodeBufferDelegation add(int bufferId, String ... s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).add(s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(int bufferId, String s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).add(s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(int bufferId, CodeBuffer cb) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).add(cb);
    }
    return this;
  }
  
  public CodeBufferDelegation addString(int bufferId, String s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).addString(s);
    }
    return this;
  }


  public CodeBufferDelegation addListElement(int bufferId, String s) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).addListElement(s);
    }
    return this;
  }

  public String toString(int bufferId) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      return buffers.get(bufferId).toString();
    } else {
      return null;
    }
  }

  public String toString(int bufferId, boolean withHeader) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      return buffers.get(bufferId).toString(withHeader);
    } else {
      return null;
    }
  }


  public CodeBufferDelegation setGIPSourceHeader(int bufferId, String departmentName) {
    if (bufferId >= 0 && bufferId < buffers.size()) {
      buffers.get(bufferId).setGIPSourceHeader(departmentName);
    }
    return this;
  }
  
  
  // to multiple
  
  public CodeBufferDelegation addLine(int[] bufferIds, String ... s) {
    for (int bufferId : bufferIds) {
      this.addLine(bufferId, s);
    }
    return this;
  }
  
  
  public CodeBufferDelegation addLine(int[] bufferIds, String s) {
    for (int bufferId : bufferIds) {
      this.addLine(bufferId, s);
    }
    return this;
  }

  public CodeBufferDelegation addLB(int[] bufferIds) {
    for (int bufferId : bufferIds) {
      this.addLB(bufferId);
    }
    return this;
  }

  public CodeBufferDelegation add(int[] bufferIds, String ... s) {
    for (int bufferId : bufferIds) {
      this.add(bufferId, s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(int[] bufferIds, String s) {
    for (int bufferId : bufferIds) {
      this.addLine(bufferId, s);
    }
    return this;
  }
  
  public CodeBufferDelegation add(int[] bufferIds, CodeBuffer cb) {
    for (int bufferId : bufferIds) {
      this.add(bufferId, cb);
    }
    return this;
  }
  
  public CodeBufferDelegation addString(int[] bufferIds, String s) {
    for (int bufferId : bufferIds) {
      this.addString(bufferId, s);
    }
    return this;
  }


  public CodeBufferDelegation addListElement(int[] bufferIds, String s) {
    for (int bufferId : bufferIds) {
      this.addLine(bufferId, s);
    }
    return this;
  }

  public String toString(int[] bufferIds) {
    StringBuilder sb = new StringBuilder();
    for (int bufferId : bufferIds) {
      sb.append(this.toString(bufferId));
    }
    return sb.toString();
  }

  public String toString(int[] bufferIds, boolean withHeader) {
    StringBuilder sb = new StringBuilder();
    for (int bufferId : bufferIds) {
      sb.append(this.toString(bufferId, withHeader));
    }
    return sb.toString();
  }


  public CodeBufferDelegation setGIPSourceHeader(int[] bufferIds, String departmentName) {
    for (int bufferId : bufferIds) {
      this.setGIPSourceHeader(bufferId, departmentName);
    }
    return this;
  }
  
  
  private final static class NOPCodeBuffer extends CodeBuffer {

    public NOPCodeBuffer() {
      super("NOP");
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer add(com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer arg0) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer add(String s) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer add(String... arg0) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addLB() {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addLB(int arg0) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addLine(String s) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addLine(String... arg0) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addListElement(String s) {
      return this;
    }
    @Override
    public com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer addString(String s) {
      return this;
    }
    @Override
    public void setGIPSourceHeader(String departmentName) {
    }
    
    
    
  }

}
