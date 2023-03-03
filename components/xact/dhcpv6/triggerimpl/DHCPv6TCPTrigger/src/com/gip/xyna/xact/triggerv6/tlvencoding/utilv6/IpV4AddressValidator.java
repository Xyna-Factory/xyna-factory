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
package com.gip.xyna.xact.triggerv6.tlvencoding.utilv6;

import java.util.regex.Pattern;

/**
 * Validates IPv4 addresses.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV4AddressValidator {

  private static final Pattern ipAddressValidator = Pattern.compile("([1-9][0-9]?)?[0-9](\\.(([1-9][0-9]?)?[0-9])){3}");

    private IpV4AddressValidator() {
    }

    public static boolean isValid(final String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("Ip address may not be null.");
        } else if (ipAddressValidator.matcher(ipAddress).matches()) {
            String[] parts = ipAddress.split("\\.");
            for (String part : parts) {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
