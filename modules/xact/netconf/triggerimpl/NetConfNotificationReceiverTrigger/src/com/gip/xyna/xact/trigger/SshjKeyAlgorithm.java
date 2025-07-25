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

import com.hierynomus.sshj.key.KeyAlgorithm;

import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.Factory.Named;
import xact.ssh.FactoryUtils;


public class SshjKeyAlgorithm {
  
  private final String name;
  private final Factory.Named<KeyAlgorithm> factory;
  
  
  public SshjKeyAlgorithm(String name) {
    this(name, getFromMap(name));
  }
  
  
  public SshjKeyAlgorithm(Factory.Named<KeyAlgorithm> factory) {
    this(factory.getName(), factory);
  }
  
  
  public SshjKeyAlgorithm(String name, Factory.Named<KeyAlgorithm> factory) {
    this.factory = factory;
    this.name = name;
  }
  
  private static Factory.Named<KeyAlgorithm> getFromMap(String name) {
    if (!FactoryUtils.KeyAlgFactories.containsKey(name)) {
      throw new IllegalArgumentException("Unknown key algorithm name: " + name);
    }
    return FactoryUtils.KeyAlgFactories.get(name).get();
  }
  
  
  public Factory.Named<KeyAlgorithm> getFactory() {
    return factory;
  }
  
  
  public String getName() {
    return name;
  }


  public static String getDescription() {
    return getDescription(values());
  }
  
  
  public static String getDescription(List<SshjKeyAlgorithm> list) {
    StringBuilder ret = new StringBuilder();
    boolean isFirst = true;
    for (SshjKeyAlgorithm val : list) {
      if (isFirst) { isFirst = false; }
      else { ret.append(":"); }
      ret.append(val.getName());
    }
    return ret.toString();
  }
  
  
  public static List<SshjKeyAlgorithm> values() {
    List<SshjKeyAlgorithm> ret = new ArrayList<>();
    for (Entry<String, Supplier<Named<KeyAlgorithm>>> entry :  FactoryUtils.KeyAlgFactories.entrySet()) {
      SshjKeyAlgorithm algo = new SshjKeyAlgorithm(entry.getKey(), entry.getValue().get());
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<SshjKeyAlgorithm> getDefaults() {
    List<SshjKeyAlgorithm> ret = new ArrayList<>();
    List<Named<KeyAlgorithm>> factories = FactoryUtils.createKeyAlgsListDefault();
    for (Named<KeyAlgorithm> item : factories) {
      SshjKeyAlgorithm algo = new SshjKeyAlgorithm(item);
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<SshjKeyAlgorithm> parseColonSeparatedNameList(String input) {
    List<SshjKeyAlgorithm> ret = new ArrayList<>();
    String[] parts = input.split(":");
    for (String part : parts) {
      SshjKeyAlgorithm algo = new SshjKeyAlgorithm(part);
      ret.add(algo);
    }
    return ret;
  }
  
  
  public static List<Factory.Named<KeyAlgorithm>> extractFactories(List<SshjKeyAlgorithm> input) {
    List<Factory.Named<KeyAlgorithm>> ret = new ArrayList<>();
    for (SshjKeyAlgorithm val : input) { ret.add(val.getFactory()); }
    return ret;
  }
  
}
