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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.ACSDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.ContainerDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.DUIDDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.DUIDENDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.DUIDLLDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.DUIDLLTDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.EContainerDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.EOctetStringDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.FQDNDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.FlowDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IAAddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IANADHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IAPDDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IAPDOptionDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IATADHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IgnoreValueDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV4AddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV4AddressListDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV6AddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV6AddressListDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.LeaseQueryDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.MacAddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.NonTLVIpV6AddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.NonTLVOctetStringDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.NonTLVUnsignedIntegerDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.OctetStringDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.PaddingDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.ProvServerDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.RelayMessageDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.UnsignedIntegerDHCPv6TlvDecoderFactory;




/**
 * DOCSIS TLV decoder factory provider.
 */
public final class DHCPv6TlvDecoderFactoryProvider {

    public static final String DISALLOWED = "Disallowed";
    public static final String END_OF_DATA_MARKER = "EndOfDataMarker";
    public static final String PADDING = "Padding";
    public static final String CABLE_MODEM_MIC = "CableModemMic";
    public static final String CMTS_MIC = "CmtsMic";

    private static final DHCPv6TlvDecoderFactoryProvider INSTANCE = new DHCPv6TlvDecoderFactoryProvider();

    private final Map<String, DHCPv6TlvDecoderFactory> decoderFactories;

    private DHCPv6TlvDecoderFactoryProvider() {
        Map<String, DHCPv6TlvDecoderFactory> factories = new HashMap<String, DHCPv6TlvDecoderFactory>();
        for (DHCPv6TlvDecoderFactory factory : getDecoderFactoriesList()) {
            if (factories.put(factory.getDataTypeName(), factory) != null) {
                throw new IllegalStateException("Decoder factory for data type <" + factory.getDataTypeName()
                        + ">, defined more than once.");
            }
        }
        this.decoderFactories = Collections.unmodifiableMap(factories);
    }

    private static List<DHCPv6TlvDecoderFactory> getDecoderFactoriesList() {
        List<DHCPv6TlvDecoderFactory> list = new ArrayList<DHCPv6TlvDecoderFactory>();
        list.add(new OctetStringDHCPv6TlvDecoderFactory());
        list.add(new NonTLVOctetStringDHCPv6TlvDecoderFactory());
        list.add(new ProvServerDHCPv6TlvDecoderFactory());
        list.add(new FlowDHCPv6TlvDecoderFactory());
        list.add(new FQDNDHCPv6TlvDecoderFactory());
        list.add(new ACSDHCPv6TlvDecoderFactory());
        list.add(new ContainerDHCPv6TlvDecoderFactory());
        list.add(new RelayMessageDHCPv6TlvDecoderFactory());
        list.add(new LeaseQueryDHCPv6TlvDecoderFactory());
        list.add(new IANADHCPv6TlvDecoderFactory());
        list.add(new IAPDDHCPv6TlvDecoderFactory());
        list.add(new IATADHCPv6TlvDecoderFactory());
        list.add(new IAAddressDHCPv6TlvDecoderFactory());
        list.add(new IAPDOptionDHCPv6TlvDecoderFactory());
        list.add(new DUIDDHCPv6TlvDecoderFactory());
        list.add(new DUIDLLTDHCPv6TlvDecoderFactory());
        list.add(new DUIDENDHCPv6TlvDecoderFactory());
        list.add(new DUIDLLDHCPv6TlvDecoderFactory());
        list.add(new EContainerDHCPv6TlvDecoderFactory());
        list.add(new EOctetStringDHCPv6TlvDecoderFactory());
        list.add(new UnsignedIntegerDHCPv6TlvDecoderFactory());
        list.add(new NonTLVUnsignedIntegerDHCPv6TlvDecoderFactory());
        list.add(new IpV4AddressDHCPv6TlvDecoderFactory());
        list.add(new IpV4AddressListDHCPv6TlvDecoderFactory());
        list.add(new IpV6AddressDHCPv6TlvDecoderFactory());
        list.add(new IpV6AddressListDHCPv6TlvDecoderFactory());
        list.add(new NonTLVIpV6AddressDHCPv6TlvDecoderFactory());
        list.add(new IgnoreValueDHCPv6TlvDecoderFactory(CABLE_MODEM_MIC));
        list.add(new IgnoreValueDHCPv6TlvDecoderFactory(CMTS_MIC));
        list.add(new MacAddressDHCPv6TlvDecoderFactory());
        list.add(new PaddingDHCPv6TlvDecoderFactory(PADDING));
        list.add(new PaddingDHCPv6TlvDecoderFactory(END_OF_DATA_MARKER));
        list.add(new OctetStringDHCPv6TlvDecoderFactory(DISALLOWED));
        return list;
    }

    public static DHCPv6TlvDecoderFactory get(final String dataTypeName) {
        if (dataTypeName == null) {
            throw new IllegalArgumentException("Data type name may not be null.");
        }
        return INSTANCE.decoderFactories.get(dataTypeName);
    }
}
