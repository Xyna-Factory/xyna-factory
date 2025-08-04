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
import net.schmizz.sshj.transport.mac.MAC;
import xact.ssh.FactoryUtils;


public class SshjMacFactory {
  
  private final String name;
  private final Named<MAC> factory;
  
  
  public SshjMacFactory(String name) {
    this(name, getFromMap(name));
  }
  
  public SshjMacFactory(Named<MAC> factory) {
    this(factory.getName(), factory);
  }

  public SshjMacFactory(String name, Named<MAC> factory) {
    this.factory = factory;
    this.name = name;
  }
  
  private static Named<MAC> getFromMap(String name) {
    if (!FactoryUtils.macFactories.containsKey(name)) {
      throw new IllegalArgumentException("Unknown Message Authentication Code (MAC) name: " + name);
    }
    return FactoryUtils.macFactories.get(name).get();
  }
  
  
  public Named<MAC> getFactory() {
    return factory;
  }
  
  
  public String getName() {
    return name;
  }


  public static String getDescription() {
    return getDescription(values());
  }
  
  
  public static String getDescription(List<SshjMacFactory> list) {
    StringBuilder ret = new StringBuilder();
    boolean isFirst = true;
    for (SshjMacFactory val : list) {
      if (isFirst) { isFirst = false; }
      else { ret.append(":"); }
      ret.append(val.getName());
    }
    return ret.toString();
  }
  
  
  public static List<SshjMacFactory> values() {
    List<SshjMacFactory> ret = new ArrayList<>();
    for (Entry<String, Supplier<Named<MAC>>> entry :  FactoryUtils.macFactories.entrySet()) {
      SshjMacFactory algo = new SshjMacFactory(entry.getKey(), entry.getValue().get());
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<SshjMacFactory> getDefaults() {
    List<SshjMacFactory> ret = new ArrayList<>();
    List<Named<MAC>> factories = FactoryUtils.createMacListDefault();
    for (Named<MAC> item : factories) {
      SshjMacFactory algo = new SshjMacFactory(item);
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<SshjMacFactory> parseColonSeparatedNameList(String input) {
    List<SshjMacFactory> ret = new ArrayList<>();
    String[] parts = input.split(":");
    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) { continue; }
      SshjMacFactory algo = new SshjMacFactory(part);
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<Factory.Named<MAC>> extractFactories(List<SshjMacFactory> input) {
    List<Factory.Named<MAC>> ret = new ArrayList<>();
    for (SshjMacFactory val : input) { ret.add(val.getFactory()); }
    return ret;
  }
  
}
