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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RolesResolver;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class JWTDomainSpecificData implements DomainTypeSpecificData, RolesResolver {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_ASSOCIATED_ORDERTYPE = "xact.http.jwt.auth.AuthenticateWithJWT";
  private static final String DEFAULT_ROLES_RESOLVER_ORDERTYPE = "xact.http.jwt.auth.ResolveAvailableRolesWithJWT";



  public enum AuthValidationMode {
    HEADER, JWT
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
  private String rolesResolverOrdertype;
  private long revision;
  private transient RuntimeContext runtimeContext;


  public JWTDomainSpecificData() {
  }


  public JWTDomainSpecificData(List<String> trustedIssuers, List<String> intendedAudience, Optional<String> roleClaimPath,
                               Optional<String> defaultRole, Optional<String> rolePrefix, Optional<String> roleSuffix,
                               List<String> roleOrder, Optional<String> jwksUri, String associatedOrdertype,
                               Optional<String> rolesResolverOrdertype, long revision) {
    this.trustedIssuers = trustedIssuers;
    this.intendedAudience = intendedAudience;
    this.roleClaimPath = roleClaimPath.orElse(null);
    this.defaultRole = defaultRole.orElse(null);
    this.rolePrefix = rolePrefix.orElse(null);
    this.roleSuffix = roleSuffix.orElse(null);
    this.roleOrder = roleOrder;
    this.jwksUri = jwksUri.orElse(null);
    this.associatedOrdertype = associatedOrdertype;
    this.rolesResolverOrdertype = rolesResolverOrdertype.orElse(null);
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
    String configured = associatedOrdertype != null ? associatedOrdertype.trim() : null;
    if (configured == null || configured.isEmpty()) {
      return DEFAULT_ASSOCIATED_ORDERTYPE;
    }
    return configured;
  }


  public void setAssociatedOrdertype(String associatedOrdertype) {
    String configured = associatedOrdertype != null ? associatedOrdertype.trim() : null;
    this.associatedOrdertype = (configured == null || configured.isEmpty()) ? null : configured;
  }


  public Optional<String> getRolesResolverOrdertype() {
    String configured = rolesResolverOrdertype != null ? rolesResolverOrdertype.trim() : null;
    if (configured == null || configured.isEmpty()) {
      return Optional.of(DEFAULT_ROLES_RESOLVER_ORDERTYPE);
    }
    return Optional.of(configured);
  }


  public void setRolesResolverOrdertype(Optional<String> rolesResolverOrdertype) {
    String configured = rolesResolverOrdertype.map(String::trim).orElse(null);
    this.rolesResolverOrdertype = (configured == null || configured.isEmpty()) ? null : configured;
  }


  public long getRevision() {
    return revision;
  }


  public void setRevision(long revision) {
    this.revision = revision;
  }


  public RuntimeContext getRuntimeContext() {
    if (runtimeContext == null) {
      RevisionManagement revisionManagement =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
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


  public RuntimeContext getRolesResolverRuntimeContext() {
    return getRuntimeContext();
  }


  public String getRolesResolverOrderContextKey() {
    return "xfmg.xopctrl.jwt.token";
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


  public Optional<String> getJwksUri() {
    return Optional.ofNullable(jwksUri);
  }


  public void setJwksUri(Optional<String> jwksUri) {
    this.jwksUri = jwksUri.orElse(null);
  }


  @Override
  public void appendInformation(StringBuilder output) {
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Trusted Issuers: ").append(trustedIssuers).append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Intended Audience: ").append(intendedAudience).append("\n");
    AuthValidationMode mode = getAuthValidationMode();
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Auth Validation Mode: ").append(mode)
        .append(mode == AuthValidationMode.JWT ? " (default)" : "").append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Role Claim Path: ")
        .append(getRoleClaimPath().orElse("Not Configured")).append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Default Role: ").append(getDefaultRole().orElse("Not Configured"))
        .append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Role Prefix: ").append(getRolePrefix().orElse("Not Configured"))
        .append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Role Suffix: ").append(getRoleSuffix().orElse("Not Configured"))
        .append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Role Order: ")
        .append(roleOrder != null && !roleOrder.isEmpty() ? roleOrder : "Not Configured").append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("JWKS URI: ")
        .append(getJwksUri().orElse("Not Configured (auto-discover)")).append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Associated Order Type (for authentication): ")
        .append(getAssociatedOrdertype()).append(associatedOrdertype != null && !associatedOrdertype.trim().isEmpty() ? "" : " (default)")
        .append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Roles Resolver Order Type: ")
        .append(getRolesResolverOrdertype().orElse(DEFAULT_ROLES_RESOLVER_ORDERTYPE))
        .append(rolesResolverOrdertype != null && !rolesResolverOrdertype.trim().isEmpty() ? "" : " (default)").append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA).append("Runtime Context (for both order types): @rev_").append(revision)
        .append("\n");
  }


  @Override
  public List<String> resolveAvailableRoles(String domainName, String credential) {
    JWTRolesResolver resolver = new JWTRolesResolver(
        getRolesResolverOrdertype().orElse(null),
        getRolesResolverRuntimeContext(),
        getRolesResolverOrderContextKey()
    );
    return resolver.resolveAvailableRoles(domainName, credential);
  }
}
