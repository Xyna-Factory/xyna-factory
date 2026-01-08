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
package com.gip.xyna.xact.trigger.tlvdecoding.radius;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories.ContainerRadiusTlvDecoderFactory;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories.EContainerRadiusTlvDecoderFactory;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories.IpV4AddressRadiusTlvDecoderFactory;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories.OctetStringRadiusTlvDecoderFactory;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories.UnsignedIntegerRadiusTlvDecoderFactory;



/**
 * DOCSIS TLV decoder factory provider.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class RadiusTlvDecoderFactoryProvider {


  private static final RadiusTlvDecoderFactoryProvider INSTANCE = new RadiusTlvDecoderFactoryProvider();

  private final Map<String, RadiusTlvDecoderFactory> decoderFactories;


  private RadiusTlvDecoderFactoryProvider() {
    Map<String, RadiusTlvDecoderFactory> factories = new HashMap<String, RadiusTlvDecoderFactory>();
    for (RadiusTlvDecoderFactory factory : getDecoderFactoriesList()) {
      if (factories.put(factory.getDataTypeName(), factory) != null) {
        throw new IllegalStateException("Decoder factory for data type <" + factory.getDataTypeName() + ">, defined more than once.");
      }
    }
    this.decoderFactories = Collections.unmodifiableMap(factories);
  }


  private static List<RadiusTlvDecoderFactory> getDecoderFactoriesList() {
    List<RadiusTlvDecoderFactory> list = new ArrayList<RadiusTlvDecoderFactory>();
    list.add(new OctetStringRadiusTlvDecoderFactory());
    list.add(new ContainerRadiusTlvDecoderFactory());
    list.add(new UnsignedIntegerRadiusTlvDecoderFactory());
    list.add(new IpV4AddressRadiusTlvDecoderFactory());
    list.add(new EContainerRadiusTlvDecoderFactory());
    return list;
  }


  public static RadiusTlvDecoderFactory get(final String dataTypeName) {
    if (dataTypeName == null) {
      throw new IllegalArgumentException("Data type name may not be null.");
    }
    return INSTANCE.decoderFactories.get(dataTypeName);
  }
}
