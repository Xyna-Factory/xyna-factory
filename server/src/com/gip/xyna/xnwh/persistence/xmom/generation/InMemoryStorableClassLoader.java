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

package com.gip.xyna.xnwh.persistence.xmom.generation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class InMemoryStorableClassLoader extends ClassLoader {

  private Map<String, ByteBuffer> bytecodes;
  private String storableClassName;
  private XMOMStorableStructureInformation rootEntry;
  private long creationTime;
  private Throwable creationHistory;
  private List<InMemoryStorableClassLoader> presetDelegates;

  public InMemoryStorableClassLoader(ClassLoader parent, String storableClassName) {
    super(parent);
    bytecodes = new HashMap<>();
    this.storableClassName = storableClassName;
    this.creationTime = System.currentTimeMillis();
    this.creationHistory = new RuntimeException("");
  }

  public Class loadClass(String fqdn, ByteBuffer byteCode) throws ClassNotFoundException {
    setBytecode(fqdn, byteCode);
    return loadClass(fqdn);
  }

  public void loadAllClasses() throws ClassNotFoundException {
    for (String fqdn : bytecodes.keySet()) {
      this.loadClass(fqdn, true);
    }
  }

  @Override
  protected Class findClass(String name) throws ClassNotFoundException {
    Class<?> cls = null;
    try {
      if (bytecodes.containsKey(name)) {
        cls = defineClass(name, bytecodes.get(name));
      }
    } catch (ClassFormatError ex) {
      throw new ClassNotFoundException("Class name: " + name, ex);
    }
    return cls;
  }


  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      // erst bei parent schauen, dann bei sich selbst
      try {
        c = super.getParent().loadClass(name);
      } catch (ClassNotFoundException e) {
        // ignorieren
      }

      if (c == null) {
        String simpleName = GenerationBase.getSimpleNameFromFQName(name);
        if (simpleName.contains("$")) {
          simpleName = simpleName.substring(0, simpleName.indexOf('$'));
        }
        if (simpleName.endsWith(StorableCodeBuilder.STORABLE_CLASS_SUFFIX)) {
          if (bytecodes.containsKey(name)) {
            return findClass(name);
          } else { // delegate
            List<InMemoryStorableClassLoader> classloadersForDelegation;
            if (presetDelegates != null) {
              classloadersForDelegation = presetDelegates;
            } else {
              final List<InMemoryStorableClassLoader> classloaders = new ArrayList<>();
              rootEntry.traverse(new StorableStructureVisitor() {
  
                public StorableStructureRecursionFilter getRecursionFilter() {
                  return new StorableStructureRecursionFilter() {
  
                    public boolean accept(StorableColumnInformation columnLink) {
                      return true;
                    }
  
                    public boolean acceptHierarchy(StorableStructureInformation declaredType) {
                      return true;
                    }
                  };
                }
  
                public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
                }
  
                public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
                  if (current.getClassLoaderForStorable() != null) {
                    classloaders.add(current.getClassLoaderForStorable());
                  }
                }
              });
              classloadersForDelegation = classloaders;
            }
            for (InMemoryStorableClassLoader cl : classloadersForDelegation) {
              if (cl.bytecodes.containsKey(name)) {
                return cl.loadClass(name);
              }
            }
            return null;
          }
        }
      }
    }
    return c;
  }

  Class<?> defineClass(String name, ByteBuffer bytecode) {
    return super.defineClass(name, bytecode, null);
  }

  public void setBytecode(String fqdn, ByteBuffer bytecode) {
    this.bytecodes.put(fqdn, bytecode);
  }

  public Map<String, ByteBuffer> getBytecode() {
    return bytecodes;
  }

  public String getStorableClassName() {
    return storableClassName;
  }

  public void setRootXMOMStorable(XMOMStorableStructureInformation rootEntry) {
    this.rootEntry = rootEntry;  
  }
  
  public long getCreationTime() {
    return creationTime;
  }
  
  public Throwable getCreationHistory() {
    return creationHistory;
  }

  public XMOMStorableStructureInformation getRootXMOMStorable() {
    return rootEntry;
  }

  public void setDelegates(List<InMemoryStorableClassLoader> delegates) {
    this.presetDelegates = delegates;
  }
  
  
  @Override
  public String toString() {
    return "InMemoryStorableClassLoader for '" + storableClassName + "'. ByteCodes: " + bytecodes.keySet();
  }

  public Class<?> loadOwnClass() throws ClassNotFoundException {
    return loadClass(storableClassName);
  }
  
}