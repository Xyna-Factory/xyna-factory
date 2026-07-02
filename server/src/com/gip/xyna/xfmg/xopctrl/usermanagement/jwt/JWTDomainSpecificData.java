package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;

import java.util.List;
import java.util.Optional;

public class JWTDomainSpecificData implements DomainTypeSpecificData {

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
    private String authValidationMode;

    public JWTDomainSpecificData() {}

    public JWTDomainSpecificData(List<String> trustedIssuers, List<String> intendedAudience,
                                 Optional<String> roleClaimPath, Optional<String> defaultRole,
                                 Optional<String> rolePrefix, Optional<String> roleSuffix,
                                 List<String> roleOrder, Optional<String> jwksUri,
                                 AuthValidationMode authValidationMode) {
        this.trustedIssuers = trustedIssuers;
        this.intendedAudience = intendedAudience;
        this.roleClaimPath = roleClaimPath.orElse(null);
        this.defaultRole = defaultRole.orElse(null);
        this.rolePrefix = rolePrefix.orElse(null);
        this.roleSuffix = roleSuffix.orElse(null);
        this.roleOrder = roleOrder;
        this.jwksUri = jwksUri.orElse(null);
        this.authValidationMode = (authValidationMode != null ? authValidationMode : AuthValidationMode.JWT).name();
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
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
                .append("Auth Validation Mode: ").append(getAuthValidationMode()).append("\n");
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
    }

}