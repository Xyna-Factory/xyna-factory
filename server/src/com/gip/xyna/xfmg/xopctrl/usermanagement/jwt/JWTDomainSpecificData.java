package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class JWTDomainSpecificData implements DomainTypeSpecificData {

    private List<String> trustedIssuers;
    private List<String> intendedAudience;
    private String roleClaimPath;
    private String defaultRole;

    public JWTDomainSpecificData() {}

    public JWTDomainSpecificData(List<String> trustedIssuers, List<String> intendedAudience, Optional<String> roleClaimPath, Optional<String> defaultRole) {
        this.trustedIssuers = trustedIssuers;
        this.intendedAudience = intendedAudience;
        this.roleClaimPath = roleClaimPath.orElse(null);
        this.defaultRole = defaultRole.orElse(null);
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

    @Override
    public void appendInformation(StringBuilder output) {
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Trusted Issuers: ").append(trustedIssuers).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Intended Audience: ").append(intendedAudience).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Role Claim Path: ").append(getRoleClaimPath().orElse("Not Configured")).append("\n");
        output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
              .append("Default Role: ").append(getDefaultRole().orElse("Not Configured")).append("\n");
    }

}