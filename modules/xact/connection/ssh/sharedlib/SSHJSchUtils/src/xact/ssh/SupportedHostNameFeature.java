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
package xact.ssh;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public enum SupportedHostNameFeature {
  HASHED {
    public boolean accept(HostKeyStorable key) {
      return key.isHashed();
    }
  },
  FUZZY {
    public boolean accept(HostKeyStorable key) {
      return key.isFuzzy();
    }
  },
  LIST {
    public boolean accept(HostKeyStorable key) {
      return key.isHostNameList();
    }
  };
  
  public abstract boolean accept(HostKeyStorable key);
  
  public static Set<SupportedHostNameFeature> all() {
    return new HashSet<SupportedHostNameFeature>(Arrays.asList(SupportedHostNameFeature.values()));
  }
  
  public static Set<SupportedHostNameFeature> inverse(Set<SupportedHostNameFeature> set) {
    Set<SupportedHostNameFeature> all = all();
    all.removeAll(set);
    return all;
  }
  
  public static SupportedHostNameFeature fromString(String string) {
    String value = string.trim();
    for (SupportedHostNameFeature feature : values()) {
      if (feature.toString().equalsIgnoreCase(value)) {
        return feature;
      }
    }
    return null;
  }
  
  public static Set<SupportedHostNameFeature> fromStringList(String stringList) {
    Set<SupportedHostNameFeature> features = new HashSet<SupportedHostNameFeature>();
    if (stringList.contains(",")) {
      String[] strings = stringList.split(",");
      for (String string : strings) {
        addIfNotNull(SupportedHostNameFeature.fromString(string), features);
      }
    } else {
      addIfNotNull(SupportedHostNameFeature.fromString(stringList), features);
    }
    return features;
  }
  
  private static void addIfNotNull(SupportedHostNameFeature feature, Set<SupportedHostNameFeature> features) {
    if (feature != null) {
      features.add(feature);
    }
  }
}
