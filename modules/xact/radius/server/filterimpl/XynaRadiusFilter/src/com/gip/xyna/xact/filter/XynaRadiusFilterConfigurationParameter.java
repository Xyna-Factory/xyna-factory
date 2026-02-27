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



import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;



public class XynaRadiusFilterConfigurationParameter extends FilterConfigurationParameter {

    private static final long serialVersionUID = 1L;

    private String accessRequestWf;
    private static final String DEFAULTACCESSREQUESTWF = "xact.radius.RADIUSAccessRequest";
    public static final StringParameter<String> ACCESSREQUESTWF =
            StringParameter.typeString("accessRequestWorkflow").defaultValue(DEFAULTACCESSREQUESTWF)
                    .documentation(Documentation.de("FQN des Access-Request-Workflows, default: " + DEFAULTACCESSREQUESTWF)
                            .en("FQN of the Access-Request workflow, default: " + DEFAULTACCESSREQUESTWF).build())
                    .optional().build();
    protected static final List<StringParameter<?>> ALL_PARAMETERS = StringParameter.asList(ACCESSREQUESTWF);


    @Override
    public List<StringParameter<?>> getAllStringParameters() {
        return ALL_PARAMETERS;
    }


    @Override
    public XynaRadiusFilterConfigurationParameter build(Map<String, Object> map)
            throws XACT_InvalidFilterConfigurationParameterValueException {
        XynaRadiusFilterConfigurationParameter param = new XynaRadiusFilterConfigurationParameter();
        param.accessRequestWf = ACCESSREQUESTWF.getFromMap(map);
        return param;
    }


    public String getAccessRequestWf() {
        return accessRequestWf;
    }
}
