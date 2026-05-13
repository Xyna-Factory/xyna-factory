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
package com.gip.xyna.xact.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;

public class SFTPFilterConfigurationParameter extends FilterConfigurationParameter {
    public final static String PROCESS_SFTP_ORDERTYPE = "xact.sftp.ProcessSFTPRequest";

    public static final StringParameter<String> FILTER_PREFIX = StringParameter.typeString("filterPrefix")
            .documentation(
                    Documentation
                        .de("Präfix für Zuständigkeit des Filters")
                        .en("Prefix for this filter").build())
            .optional()
            .build();

    public static final StringParameter<String> ORDERTYPE = StringParameter.typeString("ordertype")
            .documentation(
                    Documentation
                        .de("Ordertype für den Filter. Signatur: (xact.sftp.Path, xact.sftp.Username, xact.sftp.SourceIP) -> (xact.sftp.Content)")
                        .en("Ordertype to run. Signature: (xact.sftp.Path, xact.sftp.Username, xact.sftp.SourceIP) -> (xact.sftp.Content)").build())
            .defaultValue(PROCESS_SFTP_ORDERTYPE)
            .build();

    public static final List<StringParameter<?>> ALL_PARAMS = Arrays.<StringParameter<?>>asList(
            FILTER_PREFIX, ORDERTYPE);

    private Optional<String> filterPrefix = Optional.empty();
    private String ordertype = ORDERTYPE.getDefaultValue();

    public Optional<String> getFilterPrefix() {
        return filterPrefix.map(f -> f.endsWith("/") ? f : f + "/");
    }

    public String getOrdertype() {
        return ordertype;
    }

    public FilterConfigurationParameter build(Map<String, Object> paramMap)
            throws XACT_InvalidFilterConfigurationParameterValueException {
        SFTPFilterConfigurationParameter result = new SFTPFilterConfigurationParameter();

        result.filterPrefix = Optional.ofNullable(FILTER_PREFIX.getFromMap(paramMap));
        result.ordertype = ORDERTYPE.getFromMap(paramMap);

        return result;
    }

    public List<StringParameter<?>> getAllStringParameters() {
        return ALL_PARAMS;
    }

}
