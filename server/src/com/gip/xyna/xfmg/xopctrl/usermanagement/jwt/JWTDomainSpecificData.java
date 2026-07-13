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
package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import java.util.List;
import java.util.Optional;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

public class JWTDomainSpecificData implements DomainTypeSpecificData {

    private static final long serialVersionUID = 7177523157271609582L;

    public enum AuthValidationMode {
        HEADER,
        JWT
    }

    private List<String> trustedIssuers;
    private List<String> intendedAudience;
    private String roleClaimPath;
    private String defaultRole;
    private String rolePrefix;
    private String roleSuffix;
    private List<String> roleOrder;
    private String jwksUri;
    private String authValidationMode = AuthValidationMode.JWT.name();
    private String associatedOrdertype;
    private long revision;
    private transient RuntimeContext runtimeContext;

    public JWTDomainSpecificData() {}

    public JWTDomainSpecificData(List<String> trustedIssuers, List<String> intendedAudience,
                                 Optional<String> roleClaimPath, Optional<String> defaultRole,
                                 Optional<String> rolePrefix, Optional<String> roleSuffix,
                                 List<String> roleOrder, Optional<String> jwksUri,
                                 String associatedOrdertype, long revision) {
        this.trustedIssuers = trustedIssuers;
        this.intendedAudience = intendedAudience;
        this.roleClaimPath = roleClaimPath.orElse(null);
        this.defaultRole = defaultRole.orElse(null);
        this.rolePrefix = rolePrefix.orElse(null);
        this.roleSuffix = roleSuffix.orElse(null);
        this.roleOrder = roleOrder;
        this.jwksUri = jwksUri.orElse(null);
        this.associatedOrdertype = associatedOrdertype;
        this.revision = revision;
    }

    public AuthValidationMode getAuthValidationMode() {
        try {
            return AuthValidationMode.valueOf(Optional.ofNullable(authValidationMode).orElse(AuthValidationMode.JWT.name()));
        } catch (IllegalArgumentException e) {
            return AuthValidationMode.JWT;
        }
    }

    public void setAuthValidationMode(AuthValidationMode mode) {
        this.authValidationMode = (mode != null ? mode : AuthValidationMode.JWT).name();
    }

    public String getAssociatedOrdertype() {
        return associatedOrdertype;
    }

    public void setAssociatedOrdertype(String associatedOrdertype) {
        this.associatedOrdertype = associatedOrdertype;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public RuntimeContext getRuntimeContext() {
        if (runtimeContext == null) {
            RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
            try {
                runtimeContext = revisionManagement.getRuntimeContext(revision);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                CentralFactoryLogging.getLogger(UserManagement.class).warn("Failed to restore RuntimeContext for revision " + revision, e);
                try {
                    return revisionManagement.getRuntimeContext(-1L);
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                    throw new RuntimeException("Failed to resolve default revision.");
                }
            }
        }
        return runtimeContext;
    }

    public List<String> getTrustedIssuers() {
        return trustedIssuers;
    }

    public void setTrustedIssuers(List<String> trustedIssuers) {
        this.trustedIssuers = trustedIssuers;
    }

    public List<String> getIntendedAudience() {
        return intendedAudience;
    }

    public void setIntendedAudience(List<String> intendedAudience) {
        this.intendedAudience = intendedAudience;
    }

    public Optional<String> getRoleClaimPath() {
        return Optional.ofNullable(roleClaimPath);
    }

    public void setRoleClaimPath(Optional<String> roleClaimPath) {
        this.roleClaimPath = roleClaimPath.orElse(null);
    }

    public Optional<String> getDefaultRole() {
        return Optional.ofNullable(defaultRole);
    }

    public void setDefaultRole(Optional<String> defaultRole) {
        this.defaultRole = defaultRole.orElse(null);
    }

    public Optional<String> getRolePrefix() {
        return Optional.ofNullable(rolePrefix);
    }

    public void setRolePrefix(Optional<String> rolePrefix) {
        this.rolePrefix = rolePrefix.orElse(null);
    }

    public Optional<String> getRoleSuffix() {
        return Optional.ofNullable(roleSuffix);
    }

    public void setRoleSuffix(Optional<String> roleSuffix) {
        this.roleSuffix = roleSuffix.orElse(null);
    }

    public List<String> getRoleOrder() {
        return roleOrder;
    }

    public void setRoleOrder(List<String> roleOrder) {
        this.roleOrder = roleOrder;
    }

    public Optional<String> getJwksUri() { return Optional.ofNullable(jwksUri); }

    public void setJwksUri(Optional<String> jwksUri) { this.jwksUri = jwksUri.orElse(null); }

    @Override
    public void appendInformation(StringBuilder output) {
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Trusted Issuers: ").append(trustedIssuers).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Intended Audience: ").append(intendedAudience).append("\n");
        AuthValidationMode mode = getAuthValidationMode();
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Auth Validation Mode: ").append(mode)
              .append(mode == AuthValidationMode.JWT ? " (default)" : "").append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Role Claim Path: ").append(getRoleClaimPath().orElse("Not Configured")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("Default Role: ").append(getDefaultRole().orElse("Not Configured")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("Role Prefix: ").append(getRolePrefix().orElse("Not Configured")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("Role Suffix: ").append(getRoleSuffix().orElse("Not Configured")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("Role Order: ").append(roleOrder != null && !roleOrder.isEmpty() ? roleOrder : "Not Configured").append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("JWKS URI: ").append(getJwksUri().orElse("Not Configured (auto-discover)")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append("Associated Order Type: ").append(associatedOrdertype).append(" @rev_").append(revision).append("\n");
    }

}