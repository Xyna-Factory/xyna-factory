package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;
/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.ACSTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.ContainerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DUIDENTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DUIDLLTTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DUIDLLTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DUIDTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DisallowedTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DoNothingTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.EContainerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.EOctetStringTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.FQDNTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.FlowTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IAAddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IANATlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IAPDOptionTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IAPDTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IATATlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IpV4AddressListTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IpV4AddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IpV6AddressListTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IpV6AddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.LeaseQueryTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.MacAddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.NonTLVIpV6AddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.NonTLVOctetStringTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.NonTLVUnsignedIntegerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.OctetStringTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.PaddingTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.ProvServerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.RelayMessageTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.UnsignedIntegerTlvEncoderFactory;

/**
 * Provides encoder factory for given value data type.
 */
public final class TlvEncoderFactoryProvider {

    private static final Map<String, TlvEncoderFactory> FACTORIES = createFactoriesMap();
    private static final String TLV_ENCODER_FACTORY_SUFIX = "TlvEncoderFactory";

    private TlvEncoderFactoryProvider() {
    }

    private static Map<String, TlvEncoderFactory> createFactoriesMap() {
        Map<String, TlvEncoderFactory> map = new HashMap<String, TlvEncoderFactory>();
        add(map, new ContainerTlvEncoderFactory());
        add(map, new EContainerTlvEncoderFactory());
        add(map, new EOctetStringTlvEncoderFactory());
        add(map, new IpV4AddressTlvEncoderFactory());
        add(map, new IpV4AddressListTlvEncoderFactory());
        add(map, new IpV6AddressTlvEncoderFactory());
        add(map, new IpV6AddressListTlvEncoderFactory());
        add(map, new NonTLVIpV6AddressTlvEncoderFactory());
        add(map, new UnsignedIntegerTlvEncoderFactory());
        add(map, new NonTLVUnsignedIntegerTlvEncoderFactory());
        add(map, new MacAddressTlvEncoderFactory());
        add(map, new OctetStringTlvEncoderFactory());
        add(map, new NonTLVOctetStringTlvEncoderFactory());
        add(map, new ProvServerTlvEncoderFactory());
        add(map, new FlowTlvEncoderFactory());
        add(map, new ACSTlvEncoderFactory());
        add(map, new FQDNTlvEncoderFactory());
        add(map, new RelayMessageTlvEncoderFactory());
        add(map, new IANATlvEncoderFactory());
        add(map, new IATATlvEncoderFactory());
        add(map, new IAAddressTlvEncoderFactory());
        add(map, new IAPDTlvEncoderFactory());
        add(map, new IAPDOptionTlvEncoderFactory());
        add(map, new DUIDTlvEncoderFactory());
        add(map, new DUIDLLTTlvEncoderFactory());
        add(map, new DUIDLLTlvEncoderFactory());
        add(map, new DUIDENTlvEncoderFactory());
        add(map, new LeaseQueryTlvEncoderFactory());
        add(map, new DisallowedTlvEncoderFactory());
        map.put(DHCPv6ConfigurationEncoder.CABLE_MODEM_MIC_DATA_TYPE_NAME, new DoNothingTlvEncoderFactory());
        map.put(DHCPv6ConfigurationEncoder.CMTS_MIC_DATA_TYPE_NAME, new DoNothingTlvEncoderFactory());
        map.put(DHCPv6ConfigurationEncoder.PADDING_DATA_TYPE_NAME, new PaddingTlvEncoderFactory());
        map.put(DHCPv6ConfigurationEncoder.END_OF_DATA_MARKER_TYPE_NAME, new DisallowedTlvEncoderFactory());
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
