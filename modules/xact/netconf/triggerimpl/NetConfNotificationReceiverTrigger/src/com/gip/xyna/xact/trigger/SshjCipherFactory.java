/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


package com.gip.xyna.xact.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.Factory.Named;
import net.schmizz.sshj.transport.cipher.Cipher;
import xact.ssh.FactoryUtils;


public class SshjCipherFactory {

  private final String name;
  private final Named<Cipher> factory;
  
  
  public SshjCipherFactory(String name) {
    this(name, getFromMap(name));
  }
  
  public SshjCipherFactory(Named<Cipher> factory) {
    this(factory.getName(), factory);
  }

  public SshjCipherFactory(String name, Named<Cipher> factory) {
    this.factory = factory;
    this.name = name;
  }
  
  private static Named<Cipher> getFromMap(String name) {
    if (!FactoryUtils.CipherFactories.containsKey(name)) {
      throw new IllegalArgumentException("Unknown cipher name: " + name);
    }
    return FactoryUtils.CipherFactories.get(name).get();
  }
  
  
  public Named<Cipher> getFactory() {
    return factory;
  }
  
  
  public String getName() {
    return name;
  }


  public static String getDescription() {
    return getDescription(values());
  }
  
  
  public static String getDescription(List<SshjCipherFactory> list) {
    StringBuilder ret = new StringBuilder();
    boolean isFirst = true;
    for (SshjCipherFactory val : list) {
      if (isFirst) { isFirst = false; }
      else { ret.append(":"); }
      ret.append(val.getName());
    }
    return ret.toString();
  }
  
  
  public static List<SshjCipherFactory> values() {
    List<SshjCipherFactory> ret = new ArrayList<>();
    for (Entry<String, Supplier<Named<Cipher>>> entry :  FactoryUtils.CipherFactories.entrySet()) {
      SshjCipherFactory algo = new SshjCipherFactory(entry.getKey(), entry.getValue().get());
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<SshjCipherFactory> parseColonSeparatedNameList(String input) {
    List<SshjCipherFactory> ret = new ArrayList<>();
    String[] parts = input.split(":");
    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) { continue; }
      SshjCipherFactory algo = new SshjCipherFactory(part);
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<Factory.Named<Cipher>> extractFactories(List<SshjCipherFactory> input) {
    List<Factory.Named<Cipher>> ret = new ArrayList<>();
    for (SshjCipherFactory val : input) { ret.add(val.getFactory()); }
    return ret;
  }
  
}
