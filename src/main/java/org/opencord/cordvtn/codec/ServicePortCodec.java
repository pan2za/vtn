/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencord.cordvtn.codec;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.opencord.cordvtn.api.net.AddressPair;
import org.opencord.cordvtn.api.net.PortId;
import org.opencord.cordvtn.api.net.ServicePort;
import org.opencord.cordvtn.impl.DefaultServicePort;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service port JSON codec.
 */
public final class ServicePortCodec extends JsonCodec<ServicePort> {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String NETWORK_ID = "network_id";
    private static final String MAC_ADDRESS = "mac_address";
    private static final String IP_ADDRESS = "ip_address";
    private static final String VLAN_ID = "vlan_id";
    private static final String FLOATING_ADDRESS_PAIRS = "floating_address_pairs";

    private static final String ERR_JSON = "Invalid ServicePort received";
    private static final String ERR_ID = "Service port ID cannot be null";

    @Override
    public ObjectNode encode(ServicePort sport, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, sport.id().id());

        if (sport.networkId() != null) {
            result.put(NETWORK_ID, sport.networkId().id());
        }
        if (!Strings.isNullOrEmpty(sport.name())) {
            result.put(NAME, sport.name());
        }
        if (sport.vlanId() != null) {
            result.put(VLAN_ID, sport.vlanId().id());
        }
        if (sport.mac() != null) {
            result.put(MAC_ADDRESS, sport.mac().toString());
        }
        if (sport.ip() != null) {
            result.put(IP_ADDRESS, sport.ip().toString());
        }
        ArrayNode addressPairs = context.mapper().createArrayNode();
        sport.addressPairs().forEach(pair -> {
            ObjectNode pairJson = context.mapper().createObjectNode()
                    .put(IP_ADDRESS, pair.ip().toString())
                    .put(MAC_ADDRESS, pair.mac().toString());
            addressPairs.add(pairJson);
        });
        result.set(FLOATING_ADDRESS_PAIRS, addressPairs);

        return result;
    }

    @Override
    public ServicePort decode(ObjectNode json, CodecContext context) {
        validateJson(json);
        ServicePort.Builder sportBuilder = DefaultServicePort.builder()
                .id(PortId.of(json.get(ID).asText()));

        // TODO allow removing existing value when explicit null received
        if (json.get(NAME) != null && !json.get(NAME).isNull()) {
            sportBuilder.name(json.get(NAME).asText());
        }
        if (json.get(MAC_ADDRESS) != null && !json.get(MAC_ADDRESS).isNull()) {
            sportBuilder.mac(MacAddress.valueOf(json.get(MAC_ADDRESS).asText()));
        }
        if (json.get(IP_ADDRESS) != null && !json.get(IP_ADDRESS).isNull()) {
            sportBuilder.ip(IpAddress.valueOf(json.get(IP_ADDRESS).asText()));
        }
        if (json.get(VLAN_ID) != null && !json.get(VLAN_ID).isNull()) {
            sportBuilder.vlanId(VlanId.vlanId(json.get(VLAN_ID).asText()));
        }
        if (json.get(FLOATING_ADDRESS_PAIRS).isNull()) {
            sportBuilder.addressPairs(ImmutableSet.of());
        } else if (json.get(FLOATING_ADDRESS_PAIRS) != null) {
            Set<AddressPair> addressPairs = Sets.newHashSet();
            json.get(FLOATING_ADDRESS_PAIRS).forEach(pair -> {
                AddressPair addrPair = AddressPair.of(
                        IpAddress.valueOf(pair.get(IP_ADDRESS).asText()),
                        MacAddress.valueOf(pair.get(MAC_ADDRESS).asText()));
                addressPairs.add(addrPair);
            });
            sportBuilder.addressPairs(addressPairs);
        }
        return sportBuilder.build();
    }

    private void validateJson(ObjectNode json) {
        checkArgument(json != null && json.isObject(), ERR_JSON);
        checkArgument(json.get(ID) != null && !json.get(ID).isNull(), ERR_ID);

        // allow explicit null for removing the existing value
        if (json.get(NAME) != null && !json.get(NAME).isNull()) {
            if (Strings.isNullOrEmpty(json.get(NAME).asText())) {
                final String error = "Null or empty ServiceNetwork name received";
                throw new IllegalArgumentException(error);
            }
        }

        if (json.get(MAC_ADDRESS) != null && !json.get(MAC_ADDRESS).isNull()) {
            try {
                MacAddress.valueOf(json.get(MAC_ADDRESS).asText());
            } catch (IllegalArgumentException e) {
                final String error = "Invalid ServicePort MAC address received: ";
                throw new IllegalArgumentException(error + json.get(MAC_ADDRESS).asText());
            }
        }

        if (json.get(IP_ADDRESS) != null && !json.get(IP_ADDRESS).isNull()) {
            try {
                IpAddress.valueOf(json.get(IP_ADDRESS).asText());
            } catch (IllegalArgumentException e) {
                final String error = "Invalid ServicePort IP address received: ";
                throw new IllegalArgumentException(error + json.get(IP_ADDRESS).asText());
            }
        }

        if (json.get(VLAN_ID) != null && !json.get(VLAN_ID).isNull()) {
            try {
                VlanId.vlanId(json.get(VLAN_ID).asText());
            } catch (IllegalArgumentException e) {
                final String error = "Invalid VLAN ID is received: ";
                throw new IllegalArgumentException(error + json.get(VLAN_ID).asText());
            }
        }

        if (json.get(FLOATING_ADDRESS_PAIRS) != null &&
                !json.get(FLOATING_ADDRESS_PAIRS).isNull()) {
            json.get(FLOATING_ADDRESS_PAIRS).forEach(pair -> {
                if (pair.get(IP_ADDRESS) == null ||
                        pair.get(IP_ADDRESS).isNull() ||
                        pair.get(MAC_ADDRESS) == null ||
                        pair.get(MAC_ADDRESS).isNull()) {
                    final String error = "Invalid floating address pair received";
                    throw new IllegalArgumentException(error);
                }
                try {
                    IpAddress.valueOf(pair.get(IP_ADDRESS).asText());
                } catch (IllegalArgumentException e) {
                    final String error = "Invalid floating address pair IP: ";
                    throw new IllegalArgumentException(error + pair.get(IP_ADDRESS).asText());
                }

                try {
                    MacAddress.valueOf(pair.get(MAC_ADDRESS).asText());
                } catch (IllegalArgumentException e) {
                    final String error = "Invalid floating address pair MAC: ";
                    throw new IllegalArgumentException(error + pair.get(MAC_ADDRESS).asText());
                }
            });
        }
    }
}
