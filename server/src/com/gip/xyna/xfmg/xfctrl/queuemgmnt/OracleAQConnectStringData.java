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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.gip.xyna.utils.misc.EnvironmentVariable.StringEnvironmentVariable;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;



public class OracleAQConnectStringData extends OracleAQConnectData {

    private StringEnvironmentVariable jdbcEnv;
    private StringEnvironmentVariable userEnv;
    private StringEnvironmentVariable passwordEnv;
    private String uuid;


    public String getUuid() {
        return uuid;
    }


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public StringEnvironmentVariable getJdbcEnv() {
        return jdbcEnv;
    }


    public void setJdbcEnv(StringEnvironmentVariable jdbcEnv) {
        this.jdbcEnv = jdbcEnv;
    }


    public StringEnvironmentVariable getUserEnv() {
        return userEnv;
    }


    public void setUserEnv(StringEnvironmentVariable userEnv) {
        this.userEnv = userEnv;
    }


    public StringEnvironmentVariable getPasswordEnv() {
        return passwordEnv;
    }


    public void setPasswordEnv(StringEnvironmentVariable passwordEnv) {
        this.passwordEnv = passwordEnv;
    }


    @Override
    public String getJdbcUrl() {
        if (jdbcEnv != null) {
            return jdbcEnv.getValue().orElse(getConfiguredJdbcUrl());
        }

        return getConfiguredJdbcUrl();
    }


    @Override
    public String getUserName() {
        if (userEnv != null) {
            return userEnv.getValue().orElse(getConfiguredUserName());
        }

        return getConfiguredUserName();
    }


    @Override
    public String getPassword() {
        if (passwordEnv != null) {
            return passwordEnv.getValue().orElse(getConfiguredPassword());
        }

        return getConfiguredPassword();
    }


    private static final StringParameter<String> JDBC_PARAM = StringParameter.typeString("jdbc").label("jdbc")
            .documentation(Documentation.en("JDBC URL for AQ database.").de("JDBC URL der AQ Datenbank.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> JDBC_ENV_PARAM =
            StringParameter
                    .typeEnvironmentVariable(StringEnvironmentVariable.class, "jdbcEnv").label("jdbc env var").documentation(Documentation
                            .en("Env var for JDBC URL of AQ database.").de("Umgebungsvariable für JDBC URL der AQ Datenbank.").build())
                    .build();

    private static final StringParameter<String> USER_PARAM = StringParameter.typeString("user").label("user")
            .documentation(Documentation.en("Username for database.").de("Benutzername der Datenbank.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> USER_ENV_PARAM =
            StringParameter
                    .typeEnvironmentVariable(StringEnvironmentVariable.class, "userEnv").label("user env var").documentation(Documentation
                            .en("Env var for username for database.").de("Umgebungsvariable für Benutzername der Datenbank.").build())
                    .build();

    private static final StringParameter<String> PASSWORD_PARAM = StringParameter.typeString("password").label("password")
            .documentation(Documentation.en("Password of the DB user.").de("Passwort des DB Nutzers.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> PASSWORD_ENV_PARAM = StringParameter
            .typeEnvironmentVariable(StringEnvironmentVariable.class, "passwordEnv").label("password env var").documentation(Documentation
                    .en("Env var for password of the DB user.").de("Umgebungsvariable für Passwort des DB Nutzers.").build())
            .build();

    private static final StringParameter<String> UUID_PARAM = StringParameter.typeString("uuid").label("uuid")
            .documentation(Documentation.en("Unique identifier for the secret used to encrypt the password.")
                    .de("Eindeutige Kennung für das Secret zur Verschlüsselung des Passworts.").build())
            .build();

    public static final List<StringParameter<?>> allParams = Collections.unmodifiableList(StringParameter
            .asList(UUID_PARAM, JDBC_PARAM, USER_PARAM, PASSWORD_PARAM, JDBC_ENV_PARAM, USER_ENV_PARAM, PASSWORD_ENV_PARAM));


    private OracleAQConnectStringData(OracleAQConnectData qcd) {
        Objects.requireNonNull(qcd, "OracleAQConnectData must not be null");

        if (qcd instanceof OracleAQConnectStringData) {
            OracleAQConnectStringData qcsd = (OracleAQConnectStringData) qcd;
            this.setJdbcUrl(qcsd.getConfiguredJdbcUrl());
            this.setUserName(qcsd.getConfiguredUserName());
            this.setPassword(qcsd.getConfiguredPassword());
            this.setUuid(qcsd.getUuid());

            this.setJdbcEnv(qcsd.getJdbcEnv());
            this.setUserEnv(qcsd.getUserEnv());
            this.setPasswordEnv(qcsd.getPasswordEnv());
        } else {
            this.setJdbcUrl(qcd.getJdbcUrl());
            this.setUserName(qcd.getUserName());
            this.setPassword(qcd.getPassword());
        }
    }


    private String getConfiguredJdbcUrl() {
        return super.getJdbcUrl();
    }


    private String getConfiguredUserName() {
        return super.getUserName();
    }


    private String getConfiguredPassword() {
        return super.getPassword();
    }


    @Override
    public String toString() {
        return "OracleAQConnectStringData {jdbcEnv:" + jdbcEnv + ", userEnv:" + userEnv + ", passwordEnv:" + passwordEnv + ", "
                + super.toString() + "}";
    }


    public static OracleAQConnectStringData fromStringParameters(List<String> parameters) {
        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(parameters).unmatchedKey(Unmatched.Ignore).with(allParams);
            String uuid = UUID_PARAM.getFromMap(paramValues);
            if (uuid == null || uuid.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing mandatory UUID parameter for OracleAQ connection data.");
            }

            OracleAQConnectStringData qcd = new OracleAQConnectStringData(new OracleAQConnectData());
            qcd.setJdbcUrl(JDBC_PARAM.getFromMap(paramValues));
            qcd.setUserName(USER_PARAM.getFromMap(paramValues));
            qcd.setPassword(QueueConnectStringData.decryptPassword(uuid, PASSWORD_PARAM.getFromMap(paramValues)));
            qcd.setUuid(uuid);

            qcd.setJdbcEnv(JDBC_ENV_PARAM.getFromMap(paramValues));
            qcd.setUserEnv(USER_ENV_PARAM.getFromMap(paramValues));
            qcd.setPasswordEnv(PASSWORD_ENV_PARAM.getFromMap(paramValues));

            validateMandatoryValueOrEnv(qcd);

            return qcd;
        } catch (StringParameterParsingException e) {
            throw new IllegalArgumentException("Unable to parse OracleAQ connect data parameters", e);
        }
    }


    public static QueueConnectData fromRegisterQueueParameters(String[] connectParams) {
        if (connectParams == null || connectParams.length == 0) {
            throw new IllegalArgumentException("Error: Connect parameter missing.");
        }

        if (isNamedParameterSyntax(connectParams)) {
            List<String> namedParams = new ArrayList<String>(Arrays.asList(connectParams));
            Map<String, Object> paramValues;
            try {
                paramValues = StringParameter.parse(namedParams).unmatchedKey(Unmatched.Ignore)
                        .with(StringParameter.asList(UUID_PARAM, PASSWORD_PARAM));
            } catch (StringParameterParsingException e) {
                throw new IllegalArgumentException("Unable to parse OracleAQ connect data parameters", e);
            }

            String uuid = UUID_PARAM.getFromMap(paramValues);
            String password = PASSWORD_PARAM.getFromMap(paramValues);

            if (!hasText(uuid)) {
                uuid = UUID.randomUUID().toString();
            }
            String encryptedPassword = hasText(password) ? QueueConnectStringData.encryptPassword(uuid, password) : null;

            List<String> normalizedParams = new ArrayList<String>();
            normalizedParams.add(UUID_PARAM.toNamedParameterObject(uuid));

            for (String parameter : namedParams) {
                if (parameter != null && parameter.startsWith(UUID_PARAM.getName() + "=")) {
                    continue;
                }

                if (parameter != null && parameter.startsWith(PASSWORD_PARAM.getName() + "=")) {
                    if (encryptedPassword != null) {
                        normalizedParams.add(PASSWORD_PARAM.toNamedParameterObject(encryptedPassword));
                    } else {
                        normalizedParams.add(parameter);
                    }
                    continue;
                }

                normalizedParams.add(parameter);
            }

            return fromStringParameters(normalizedParams);
        }

        if (connectParams.length != 3) {
            throw new IllegalArgumentException("Error: Connect parameter missing.");
        }

        OracleAQConnectData connectData = new OracleAQConnectData();
        connectData.setUserName(QueueManagement.checkParameter("userName", connectParams[0]));
        connectData.setPassword(QueueManagement.checkParameter("password", connectParams[1]));
        connectData.setJdbcUrl(QueueManagement.checkParameter("jdbcUrl", connectParams[2]));
        return connectData;
    }


    private static boolean isNamedParameterSyntax(String[] connectParams) {
        int namedParameters = 0;
        for (String param : connectParams) {
            if (isNamedParameter(param, allParams)) {
                namedParameters++;
            }
        }

        if (namedParameters > 0 && namedParameters < connectParams.length) {
            throw new IllegalArgumentException("Error: Mixed named and unnamed connect parameters are not supported.");
        }

        return namedParameters == connectParams.length;
    }


    private static boolean isNamedParameter(String param, List<StringParameter<?>> validParameters) {
        String parameterName = extractParameterName(param);
        if (parameterName == null) {
            return false;
        }

        for (StringParameter<?> validParameter : validParameters) {
            if (validParameter.getName().equals(parameterName)) {
                return true;
            }
        }

        return false;
    }


    private static String extractParameterName(String param) {
        if (param == null) {
            return null;
        }

        int separatorIndex = param.indexOf('=');
        if (separatorIndex <= 0) {
            return null;
        }

        return param.substring(0, separatorIndex);
    }


    private static void validateMandatoryValueOrEnv(OracleAQConnectStringData qcd) {
        requireTextValueOrEnvVar("jdbc", qcd.getJdbcUrl(), qcd.getJdbcEnv());
        requireTextValueOrEnvVar("user", qcd.getUserName(), qcd.getUserEnv());
        requireTextValueOrEnvVar("password", qcd.getPassword(), qcd.getPasswordEnv());
    }


    private static void requireTextValueOrEnvVar(String parameterName, String value, StringEnvironmentVariable envVar) {
        if (!hasText(value) && envVar == null) {
            throw new IllegalArgumentException("Missing mandatory parameter '" + parameterName + "': provide either '" + parameterName
                    + "' or '" + parameterName + "Env'.");
        }
    }


    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }


    public static OracleAQConnectStringData fromConnectData(OracleAQConnectData qcd) {
        Objects.requireNonNull(qcd, "OracleAQConnectData must not be null");
        return new OracleAQConnectStringData(qcd);
    }


    public List<String> toParameters() {
        if (getUuid() == null || getUuid().trim().isEmpty()) {
            setUuid(UUID.randomUUID().toString());
        }

        List<String> params = new ArrayList<String>();
        params.add(UUID_PARAM.toNamedParameterObject(getUuid()));
        if (getConfiguredJdbcUrl() != null) {
            params.add(JDBC_PARAM.toNamedParameterObject(getConfiguredJdbcUrl()));
        }
        if (getConfiguredUserName() != null) {
            params.add(USER_PARAM.toNamedParameterObject(getConfiguredUserName()));
        }
        if (getConfiguredPassword() != null) {
            params.add(PASSWORD_PARAM.toNamedParameterObject(QueueConnectStringData.encryptPassword(getUuid(), getConfiguredPassword())));
        }

        if (getJdbcEnv() != null) {
            params.add(JDBC_ENV_PARAM.toNamedParameterObject(getJdbcEnv()));
        }
        if (getUserEnv() != null) {
            params.add(USER_ENV_PARAM.toNamedParameterObject(getUserEnv()));
        }
        if (getPasswordEnv() != null) {
            params.add(PASSWORD_ENV_PARAM.toNamedParameterObject(getPasswordEnv()));
        }

        return params;
    }

}