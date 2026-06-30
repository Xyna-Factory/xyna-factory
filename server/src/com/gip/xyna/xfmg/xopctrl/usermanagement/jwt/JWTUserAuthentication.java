package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;



import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class JWTUserAuthentication extends UserAuthentificationMethod {

  private static final Logger logger = CentralFactoryLogging.getLogger(JWTUserAuthentication.class);

  private final JWTDomainSpecificData domainSpecificData;
  private final OIDCSigningKeyResolver keyResolver;


  public JWTUserAuthentication(JWTDomainSpecificData domainSpecificData) {
    this.domainSpecificData = domainSpecificData;
    JWTDomainSpecificData.AuthValidationMode mode = domainSpecificData.getAuthValidationMode();

    // OIDC resolver is only needed in strict JWT mode.
    if (mode == JWTDomainSpecificData.AuthValidationMode.JWT) {
      try {
        this.keyResolver = new OIDCSigningKeyResolver(domainSpecificData);
      } catch (NoClassDefFoundError e) {
        logger.error(
            "JWT mode requires JJWT libraries on classpath. " + "Missing class: " + e.getMessage() + ". " + "Please deploy jjwt-api, jjwt-impl, and jjwt-jackson (or switch authValidationMode=HEADER).",
            e);
        throw new IllegalStateException("JWT libraries missing for authValidationMode=JWT", e);
      }
    } else {
      this.keyResolver = null;
    }
  }


  @Override
  public Role authenticateUserInternally(String username, String token)
      throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {

    JWTDomainSpecificData.AuthValidationMode mode = domainSpecificData.getAuthValidationMode();
    logger.debug("authenticateUserInternally was called (authValidationMode=" + mode + ")");

    try {
      Map<String, Object> claimsMap = null;

      if (mode == JWTDomainSpecificData.AuthValidationMode.JWT) {
        Claims jwtClaims = io.jsonwebtoken.Jwts.parser().keyLocator(keyResolver).build().parseSignedClaims(token).getPayload();
        claimsMap = new LinkedHashMap<>(jwtClaims);
        logger.debug("JWT claims (signature verified): " + claimsMap);
      } else if (mode == JWTDomainSpecificData.AuthValidationMode.HEADER) {
        // HEADER mode trusts proxy validation and decodes payload without signature verification.
        claimsMap = decodePayloadWithoutVerification(token);
        logger.debug("JWT payload decoded without signature check (HEADER mode): " + claimsMap);
      }

      String issuer = (String) claimsMap.get("iss");
      logger.debug("JWT issuer: " + issuer);
      if (issuer == null || !domainSpecificData.getTrustedIssuers().contains(issuer)) {
        throw new XFMG_UserAuthenticationFailedException("Untrusted issuer: " + issuer);
      }

      Object audObj = claimsMap.get("aud");
      logger.debug("JWT aud: " + audObj);
      Set<String> tokenAudiences = toStringSet(audObj);
      if (tokenAudiences == null || domainSpecificData.getIntendedAudience().stream().noneMatch(tokenAudiences::contains)) {
        throw new XFMG_UserAuthenticationFailedException("Incorrect audience: " + audObj);
      }

      String role = null;
      if (claimsMap != null) {
        String rolePrefix = domainSpecificData.getRolePrefix().orElse("");
        String roleSuffix = domainSpecificData.getRoleSuffix().orElse("");

        logger.debug("rolePrefix: " + rolePrefix);
        logger.debug("roleSuffix: " + roleSuffix);

        List<String> roleOrder = domainSpecificData.getRoleOrder();
        logger.debug("roleOrder: " + (roleOrder != null && !roleOrder.isEmpty() ? roleOrder : "Not Configured"));

        // set roleClaimPath or use default "roles"
        String roleClaimPath = domainSpecificData.getRoleClaimPath().orElse("roles");
        logger.debug("rolePath: " + roleClaimPath);

        role = extractRoleFromClaims(claimsMap, roleClaimPath, rolePrefix, roleSuffix, roleOrder);
        logger.debug("role after normalization/order: " + role);
      }

      if (role == null) {
        role = domainSpecificData.getDefaultRole().orElse(null);
        logger.debug("defaultRole: " + role);
      }
      if (role == null) {
        throw new XFMG_UserAuthenticationFailedException("No role for user: " + username);
      }

      return com.gip.xyna.XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
          .getRole(role, UserManagement.PREDEFINED_LOCALDOMAIN_NAME);

    } catch (JwtException e) {
      throw new XFMG_UserAuthenticationFailedException("JWT validation failed", e);
    } catch (XFMG_UserAuthenticationFailedException e) {
      throw e;
    } catch (Exception e) {
      throw new XFMG_UserAuthenticationFailedException("JWT authentication failed", e);
    }
  }


  private String extractRoleFromClaims(Map<String, Object> claimsMap, String claimPath, String rolePrefix, String roleSuffix,
                                       List<String> roleOrder) {
    if (claimsMap == null || claimPath == null || claimPath.isEmpty()) {
      return null;
    }

    Object value = resolveClaimPath(claimsMap, claimPath);
    if (value == null) {
      return null;
    }
    logger.debug("roles found in claim: " + value);

    boolean hasPrefix = rolePrefix != null && !rolePrefix.isEmpty();
    boolean hasSuffix = roleSuffix != null && !roleSuffix.isEmpty();
    List<String> normalizedRoles = new ArrayList<String>();

    if (value instanceof Collection) {
      for (Object item : (Collection<?>) value) {
        if (!(item instanceof String)) {
          continue;
        }
        String normalized = normalizeRole(((String) item).trim(), rolePrefix, roleSuffix, hasPrefix, hasSuffix);
        if (normalized != null) {
          normalizedRoles.add(normalized);
        }
      }
    } else if (value instanceof String) {
      String normalized = normalizeRole(((String) value).trim(), rolePrefix, roleSuffix, hasPrefix, hasSuffix);
      if (normalized != null) {
        normalizedRoles.add(normalized);
      }
    }

    if (normalizedRoles.isEmpty()) {
      return null;
    }

    logger.debug("JWT DEBUG: normalizedRoles = " + normalizedRoles);
    logger.debug("JWT DEBUG: roleOrder = " + roleOrder);
    logger.debug("JWT DEBUG: roleOrder is null? " + (roleOrder == null) + ", isEmpty? " + (roleOrder != null && roleOrder.isEmpty()));

    if (roleOrder != null && !roleOrder.isEmpty()) {
      logger.debug("JWT DEBUG: Entering roleOrder matching loop with " + roleOrder.size() + " preferred roles");
      for (String preferredRole : roleOrder) {
        logger.debug("JWT DEBUG: Checking if normalizedRoles.contains('" + preferredRole + "')");
        if (normalizedRoles.contains(preferredRole)) {
          logger.debug("JWT DEBUG: MATCH FOUND! Returning preferred role: " + preferredRole);
          return preferredRole;
        } else {
          logger.debug("JWT DEBUG: No match for '" + preferredRole + "'");
        }
      }
      logger.debug("JWT DEBUG: No role from roleOrder matched in normalizedRoles");
    } else {
      logger.debug("JWT DEBUG: roleOrder is null or empty, using fallback");
    }

    // fallback: use first role found in token.
    logger.debug("JWT DEBUG: Using fallback (first role): " + normalizedRoles.get(0));
    return normalizedRoles.get(0);
  }


  private String normalizeRole(String candidate, String rolePrefix, String roleSuffix, boolean hasPrefix, boolean hasSuffix) {
    if (candidate == null || candidate.isEmpty()) {
      return null;
    }
    if (!matchesPrefixSuffix(candidate, rolePrefix, roleSuffix)) {
      return null;
    }

    String normalized = candidate;
    if (hasPrefix) {
      normalized = normalized.substring(rolePrefix.length());
    }
    if (hasSuffix) {
      normalized = normalized.substring(0, normalized.length() - roleSuffix.length());
    }
    logger.debug("normalized role candidate: " + normalized);
    return normalized;
  }


  /**
   * Returns true if the candidate matches the given prefix and suffix constraints.
   * An empty prefix or suffix is treated as "no constraint".
   */
  private boolean matchesPrefixSuffix(String candidate, String prefix, String suffix) {
    boolean hasPrefix = prefix != null && !prefix.isEmpty();
    boolean hasSuffix = suffix != null && !suffix.isEmpty();

    if (hasPrefix && hasSuffix) {
      return candidate.startsWith(prefix) && candidate.endsWith(suffix);
    }
    if (hasPrefix) {
      return candidate.startsWith(prefix);
    }
    if (hasSuffix) {
      return candidate.endsWith(suffix);
    }
    return true;
  }


  private Object resolveClaimPath(Map<String, Object> claimsMap, String claimPath) {

    logger.debug("claimsMap: " + claimsMap);
    logger.debug("claimPath: " + claimPath);

    if (!claimPath.contains(".")) {
      return claimsMap.get(claimPath);
    }
    Object current = claimsMap;
    for (String part : claimPath.split("\\.")) {
      if (!(current instanceof Map)) {
        return null;
      }
      current = ((Map<?, ?>) current).get(part);
      if (current == null) {
        return null;
      }
    }
    return current;
  }


  private Map<String, Object> decodePayloadWithoutVerification(String token) throws XFMG_UserAuthenticationFailedException {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new XFMG_UserAuthenticationFailedException("Invalid JWT format (expected 3 parts)");
    }
    try {
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
      return parseJsonToMap(payloadJson);
    } catch (IllegalArgumentException e) {
      throw new XFMG_UserAuthenticationFailedException("Failed to decode JWT payload", e);
    }
  }


  private Map<String, Object> parseJsonToMap(String json) {
    Map<String, Object> map = new LinkedHashMap<>();
    json = json.trim();
    if (json.startsWith("{"))
      json = json.substring(1);
    if (json.endsWith("}"))
      json = json.substring(0, json.length() - 1);

    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|([0-9]+)|true|false|null|\\[[^\\]]*\\])").matcher(json);
    while (m.find()) {
      String key = m.group(1);
      String raw = m.group(2);
      if (raw.startsWith("\"")) {
        map.put(key, m.group(3));
      } else if (raw.startsWith("[")) {
        java.util.List<String> list = new java.util.ArrayList<>();
        java.util.regex.Matcher am = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(raw);
        while (am.find())
          list.add(am.group(1));
        map.put(key, list);
      } else {
        map.put(key, raw);
      }
    }
    return map;
  }


  @SuppressWarnings("unchecked")
  private Set<String> toStringSet(Object aud) {
    if (aud == null)
      return null;
    java.util.Set<String> result = new java.util.HashSet<>();
    if (aud instanceof String)
      result.add((String) aud);
    else if (aud instanceof java.util.Collection)
      result.addAll((java.util.Collection<String>) aud);
    else
      result.add(aud.toString());
    return result;
  }
}