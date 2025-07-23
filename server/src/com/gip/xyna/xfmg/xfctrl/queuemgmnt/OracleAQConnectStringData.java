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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.Documentation;

public class OracleAQConnectStringData extends OracleAQConnectData {

    private static final StringParameter<String> JDBC_PARAM 
    = StringParameter.typeString("jdbc")
        .label("jdbc")
        .documentation(Documentation
            .en("JDBC URL for AQ database.")
            .de("JDBC URL der AQ Datenbank.")
            .build())
        .mandatory()
        .build();

    private static final StringParameter<String> USER_PARAM 
    = StringParameter.typeString("user")
        .label("user")
        .documentation(Documentation
            .en("Username for database.")
            .de("Benutzername der Datenbank.")
            .build())
        .mandatory()
        .build();

    private static final StringParameter<String> PASSWORD_PARAM 
    = StringParameter.typeString("password")
        .label("password")
        .documentation(Documentation
            .en("Password of the DB user.")
            .de("Passwort des DB Nutzers.")
            .build())
        .mandatory()
        .build();

    public static final List<StringParameter<?>> allParams = StringParameter.asList(JDBC_PARAM, USER_PARAM,
            PASSWORD_PARAM);

    private OracleAQConnectStringData(OracleAQConnectData qcd) {
        this.setJdbcUrl(qcd.getJdbcUrl());
        this.setUserName(qcd.getUserName());
        this.setPassword(qcd.getPassword());
    };

    public static OracleAQConnectStringData fromStringParameters(List<String> parameters) {
        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(parameters).unmatchedKey(Unmatched.Ignore).with(allParams);
            OracleAQConnectData qcd = new OracleAQConnectData();
            qcd.setJdbcUrl(JDBC_PARAM.getFromMap(paramValues));
            qcd.setUserName(USER_PARAM.getFromMap(paramValues));
            qcd.setPassword(PASSWORD_PARAM.getFromMap(paramValues));

            return new OracleAQConnectStringData(qcd);
        } catch (StringParameterParsingException e) {

        }

        return null;
    }

    public static OracleAQConnectStringData fromConnectData(OracleAQConnectData qcd) {
        return new OracleAQConnectStringData(qcd);
    }

    public List<String> toParameters() {
        List<String> params = new ArrayList<String>();
        params.add(JDBC_PARAM.toNamedParameterObject(getJdbcUrl()));
        params.add(USER_PARAM.toNamedParameterObject(getUserName()));
        params.add(PASSWORD_PARAM.toNamedParameterObject(getPassword()));

        return params;
    }

}