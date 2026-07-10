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

package xact.http.jwt.auth.impl;



import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.jwt.JWTDomainSpecificData;

import io.jsonwebtoken.Claims;



/**
 * Central JWT validation and role extraction logic for the JSONWebToken application.
 */
public final class JWTAuthenticationLogic {

  private static final Logger logger = CentralFactoryLogging.getLogger(JWTAuthenticationLogic.class);


  private JWTAuthenticationLogic() {
  }


  public static List<String> resolveAvailableRoles(JWTDomainSpecificData domainSpecificData, String token)
      throws XFMG_UserAuthenticationFailedException {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "resolveAvailableRoles: token present=" + (token != null && !token.isEmpty()) + ", mode=" + domainSpecificData.getAuthValidationMode() + ", roleClaimPath=" + domainSpecificData.getRoleClaimPath()
              .orElse("roles"));
    }

    Map<String, Object> claimsMap = resolveAndValidateClaims(domainSpecificData, token);
    if (claimsMap == null) {
      logger.info("resolveAvailableRoles: claims map is null, returning empty list");
      return Collections.emptyList();
    }

    String rolePrefix = domainSpecificData.getRolePrefix().orElse("");
    String roleSuffix = domainSpecificData.getRoleSuffix().orElse("");
    List<String> roleOrder = domainSpecificData.getRoleOrder();
    String roleClaimPath = domainSpecificData.getRoleClaimPath().orElse("roles");

    if (logger.isDebugEnabled()) {
      logger.debug(
          "resolveAvailableRoles: using roleClaimPath=" + roleClaimPath + ", rolePrefix='" + rolePrefix + "', roleSuffix='" + roleSuffix + "', roleOrder=" + roleOrder);
    }

    List<String> roles = extractRolesFromClaims(claimsMap, roleClaimPath, rolePrefix, roleSuffix, roleOrder);

    if (logger.isDebugEnabled()) {
      logger.debug("resolveAvailableRoles: extracted roles=" + roles);
    }

    if (roles.isEmpty()) {
      String defaultRole = domainSpecificData.getDefaultRole().orElse(null);
      if (defaultRole != null && !defaultRole.trim().isEmpty()) {
        if (logger.isInfoEnabled()) {
          logger.info("resolveAvailableRoles: no roles extracted, using defaultRole=" + defaultRole.trim());
        }
        return Collections.singletonList(defaultRole.trim());
      }
      logger.warn("resolveAvailableRoles: no roles extracted and no defaultRole configured");
    }
    return roles;
  }


  private static Map<String, Object> resolveAndValidateClaims(JWTDomainSpecificData domainSpecificData, String token)
      throws XFMG_UserAuthenticationFailedException {
    // AuthValidationMode:
    //  JWT:    use OIDCSigningKeyResolver to get the signing key and validate the token
    //  HEADER: trust proxy and skip signature verification
    JWTDomainSpecificData.AuthValidationMode mode = domainSpecificData.getAuthValidationMode();
    Map<String, Object> claimsMap;
    if (mode == JWTDomainSpecificData.AuthValidationMode.JWT) {
      OIDCSigningKeyResolver keyResolver = new OIDCSigningKeyResolver(domainSpecificData);
      Claims jwtClaims = io.jsonwebtoken.Jwts.parser().keyLocator(keyResolver).build().parseSignedClaims(token).getPayload();
      claimsMap = new LinkedHashMap<>(jwtClaims);
      logger.debug("JWT claims (signature verified): " + claimsMap);
    } else {
      claimsMap = decodePayloadWithoutVerification(token);
      logger.debug("JWT payload decoded without signature check (HEADER mode): " + claimsMap);
    }

    String issuer = (String) claimsMap.get("iss");
    if (issuer == null || !domainSpecificData.getTrustedIssuers().contains(issuer)) {
      logger.warn("resolveAndValidateClaims: Untrusted issuer: " + issuer);
      throw new XFMG_UserAuthenticationFailedException("Untrusted issuer: " + issuer);
    }

    Object audObj = claimsMap.get("aud");
    Set<String> tokenAudiences = toStringSet(audObj);
    if (tokenAudiences == null || domainSpecificData.getIntendedAudience().stream().noneMatch(tokenAudiences::contains)) {
      logger.warn("resolveAndValidateClaims: Incorrect audience: " + audObj);
      throw new XFMG_UserAuthenticationFailedException("Incorrect audience: " + audObj);
    }

    return claimsMap;
  }


  /**
   * AuthValidationMode: HEADER: trust proxy and skip signature verification
   */

  private static Map<String, Object> decodePayloadWithoutVerification(String token) throws XFMG_UserAuthenticationFailedException {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      logger.warn("decodePayloadWithoutVerification: Invalid JWT format, expected 3 parts but got " + parts.length);
      throw new XFMG_UserAuthenticationFailedException("Invalid JWT format (expected 3 parts)");
    }
    try {
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
      return parseJsonToMap(payloadJson);
    } catch (IllegalArgumentException e) {
      logger.warn("decodePayloadWithoutVerification: Failed to decode JWT payload", e);
      throw new XFMG_UserAuthenticationFailedException("Failed to decode JWT payload", e);
    }
  }


  private static Map<String, Object> parseJsonToMap(String json) {
    Map<String, Object> map = new LinkedHashMap<>();
    json = json.trim();
    if (json.startsWith("{")) {
      json = json.substring(1);
    }
    if (json.endsWith("}")) {
      json = json.substring(0, json.length() - 1);
    }

    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|([0-9]+)|true|false|null|\\[[^\\]]*\\])").matcher(json);
    while (m.find()) {
      String key = m.group(1);
      String raw = m.group(2);
      if (raw.startsWith("\"")) {
        map.put(key, m.group(3));
      } else if (raw.startsWith("[")) {
        List<String> list = new ArrayList<>();
        java.util.regex.Matcher am = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(raw);
        while (am.find()) {
          list.add(am.group(1));
        }
        map.put(key, list);
      } else {
        map.put(key, raw);
      }
    }
    return map;
  }


  /**
   * extract roles from claims map with configured settings
   */


  private static List<String> extractRolesFromClaims(Map<String, Object> claimsMap, String claimPath, String rolePrefix, String roleSuffix,
                                                     List<String> roleOrder) {
    if (claimsMap == null || claimPath == null || claimPath.isEmpty()) {
      return Collections.emptyList();
    }

    Object value = resolveClaimPath(claimsMap, claimPath);
    if (value == null) {
      return Collections.emptyList();
    }

    boolean hasPrefix = rolePrefix != null && !rolePrefix.isEmpty();
    boolean hasSuffix = roleSuffix != null && !roleSuffix.isEmpty();
    LinkedHashSet<String> normalizedRoles = new LinkedHashSet<>();

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
      return Collections.emptyList();
    }

    List<String> orderedRoles = new ArrayList<>();
    if (roleOrder != null && !roleOrder.isEmpty()) {
      for (String preferredRole : roleOrder) {
        if (normalizedRoles.contains(preferredRole)) {
          orderedRoles.add(preferredRole);
        }
      }
    }

    for (String role : normalizedRoles) {
      if (!orderedRoles.contains(role)) {
        orderedRoles.add(role);
      }
    }
    return orderedRoles;
  }


  private static String normalizeRole(String candidate, String rolePrefix, String roleSuffix, boolean hasPrefix, boolean hasSuffix) {
    // normalizeRole: check for prefix suffix and remove it
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
    return normalized;
  }


  private static boolean matchesPrefixSuffix(String candidate, String prefix, String suffix) {
    // get matching roles
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


  private static Object resolveClaimPath(Map<String, Object> claimsMap, String claimPath) {
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


  @SuppressWarnings("unchecked")
  private static Set<String> toStringSet(Object aud) {
    if (aud == null) {
      return null;
    }
    Set<String> result = new java.util.HashSet<>();
    if (aud instanceof String) {
      result.add((String) aud);
    } else if (aud instanceof Collection) {
      result.addAll((Collection<String>) aud);
    } else {
      result.add(aud.toString());
    }
    return result;
  }
}
