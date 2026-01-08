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
package com.gip.xyna.xact.trigger.tlvencoding.radius;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.ContainerTlvEncoderFactory;
import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.DisallowedTlvEncoderFactory;
import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.EContainerTlvEncoderFactory;
import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.IpV4AddressTlvEncoderFactory;
import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.OctetStringTlvEncoderFactory;
import com.gip.xyna.xact.trigger.tlvencoding.radius.encoderfactories.UnsignedIntegerTlvEncoderFactory;



/**
 * Provides encoder factory for given value data type.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TlvEncoderFactoryProvider {

  private static final Map<String, TlvEncoderFactory> FACTORIES = createFactoriesMap();
  private static final String TLV_ENCODER_FACTORY_SUFIX = "TlvEncoderFactory";


  private TlvEncoderFactoryProvider() {
  }


  private static Map<String, TlvEncoderFactory> createFactoriesMap() {
    Map<String, TlvEncoderFactory> map = new HashMap<String, TlvEncoderFactory>();
    add(map, new ContainerTlvEncoderFactory());
    add(map, new IpV4AddressTlvEncoderFactory());
    add(map, new UnsignedIntegerTlvEncoderFactory());
    add(map, new OctetStringTlvEncoderFactory());
    add(map, new DisallowedTlvEncoderFactory());
    add(map, new EContainerTlvEncoderFactory());
    return Collections.unmodifiableMap(map);
  }


  private static void add(Map<String, TlvEncoderFactory> map, TlvEncoderFactory factory) {
    String name = factory.getClass().getSimpleName();
    if (!name.endsWith(TLV_ENCODER_FACTORY_SUFIX)) {
      throw new IllegalArgumentException("Illegal tlv factory class name: <" + name + ">.");
    }
    name = name.substring(0, name.length() - TLV_ENCODER_FACTORY_SUFIX.length());
    if ("".equals(name)) {
      throw new IllegalArgumentException("Illegal tlv factory class name: <" + TLV_ENCODER_FACTORY_SUFIX + ">.");
    }
    map.put(name, factory);
  }


  public static TlvEncoderFactory get(final String typeName) {
    return FACTORIES.get(typeName);
  }
}
