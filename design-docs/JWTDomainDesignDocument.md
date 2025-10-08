# Design Document: JWT Domain for User Authentication

## Overview
This document outlines the design and implementation details for the JWT Domain, which will handle user authentication using JSON Web Tokens (JWT). The implementation will include OIDC discovery, JWKS caching, and dynamic key resolution.

---

## 1. Class Design

### 1.1 `JWTDomainSpecificData`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

**Implements:**
`DomainTypeSpecificData`

**Attributes:**
- `List<String> trustedIssuers`
- `Optional<String> roleClaimPath`
- `Optional<String> defaultRole`

**Methods:**
- Getters/Setters for all attributes.
- `void appendInformation(StringBuilder output)`

**Skeleton Code:**
```java
package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import java.util.List;
import java.util.Optional;

public class JWTDomainSpecificData implements DomainTypeSpecificData {

  private List<String> trustedIssuers;
  private Optional<String> roleClaimPath;
  private Optional<String> defaultRole;

  public JWTDomainSpecificData() {}

  public JWTDomainSpecificData(List<String> trustedIssuers, String tokenHeaderName, Optional<String> roleClaimPath, Optional<String> defaultRole) {
    this.trustedIssuers = trustedIssuers;
    this.roleClaimPath = roleClaimPath;
    this.defaultRole = defaultRole;
  }

  public List<String> getTrustedIssuers() {
    return trustedIssuers;
  }

  public void setTrustedIssuers(List<String> trustedIssuers) {
    this.trustedIssuers = trustedIssuers;
  }

  public String getTokenHeaderName() {
    return tokenHeaderName;
  }

  public void setTokenHeaderName(String tokenHeaderName) {
    this.tokenHeaderName = tokenHeaderName;
  }

  public Optional<String> getRoleClaimPath() {
    return roleClaimPath;
  }

  public void setRoleClaimPath(Optional<String> roleClaimPath) {
    this.roleClaimPath = roleClaimPath;
  }

  public Optional<String> getDefaultRole() {
    return defaultRole;
  }

  public void setDefaultRole(Optional<String> defaultRole) {
    this.defaultRole = defaultRole;
  }

  @Override
  public void appendInformation(StringBuilder output) {
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
          .append("Trusted Issuers: ").append(trustedIssuers).append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
          .append("Role Claim Path: ").append(roleClaimPath.orElse("Not Configured")).append("\n");
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
          .append("Default Role: ").append(defaultRole.orElse("Not Configured")).append("\n");
  }
}
```

---

### 1.2 `JWTUserAuthentication`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

**Extends:**
`UserAuthentificationMethod`

**Attributes:**
- `JWTDomainSpecificData domainSpecificData`
- `JWKSCache jwksCache`

**Methods:**
- `Role authenticateUserInternally(String username, String token)`

**Skeleton Code:**
```java
package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import io.jsonwebtoken.*;
import java.util.Base64;

public class JWTUserAuthentication extends UserAuthentificationMethod {

  private JWTDomainSpecificData domainSpecificData;
  private JWKSCache jwksCache;

  public JWTUserAuthentication(JWTDomainSpecificData domainSpecificData, JWKSCache jwksCache) {
    this.domainSpecificData = domainSpecificData;
    this.jwksCache = jwksCache;
  }

  @Override
  public Role authenticateUserInternally(String username, String token) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    try {
      // Extract and validate issuer
      String[] parts = token.split("\\.");
      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
      String issuer = Jwts.parser().parseClaimsJwt(payloadJson).getBody().getIssuer();

      if (!domainSpecificData.getTrustedIssuers().contains(issuer)) {
        throw new XFMG_UserAuthenticationFailedException("Untrusted issuer: " + issuer);
      }

      // Validate token
      JwtParser parser = Jwts.parserBuilder()
        .setSigningKeyResolver(new OIDCSigningKeyResolver(jwksCache))
        .build();
      Jws<Claims> jws = parser.parseClaimsJws(token);
      Claims claims = jws.getBody();

      // Extract role
      String role = claims.get(domainSpecificData.getRoleClaimPath().orElse(""), String.class);
      if (role == null) {
        role = domainSpecificData.getDefaultRole().orElse(null);
      }

      return new Role(role);
    } catch (JwtException e) {
      throw new XFMG_UserAuthenticationFailedException("JWT validation failed", e);
    }
  }
}
```

---

### 1.3 `JWKSCache`

**Purpose:**
- Cache JWKS (JSON Web Key Sets) for each trusted issuer.
- Refresh keys periodically.

**Skeleton Code:**
```java
package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import java.security.Key;
import java.util.Map;
import java.util.List;

public class JWKSCache {

  private Map<String, List<Key>> issuerToKeys;

  public Key getKey(String issuer, String kid) {
    // Resolve key by issuer and kid
    return null;
  }

  public void refreshKeys(String issuer) {
    // Fetch and update JWKS for the issuer
  }
}
```

---

## 2. Integration

### Adjustments to `Domain`

**Purpose:**
- Extend the `Domain` class to support the new `JWTDomainSpecificData`.
- Ensure that the `Domain` class can serialize and deserialize `JWTDomainSpecificData` for persistence.

**Steps:**
1. Add a new `DomainType` for JWT:
   ```java
   public enum DomainType {
       LOCAL,
       LDAP,
       JWT // Add this new type
   }
   ```

2. Update the `Domain` class to handle `JWTDomainSpecificData`:
   - Modify the `getDomainSpecificData()` method to return an instance of `JWTDomainSpecificData` when the domain type is `JWT`.
   - Example:
     ```java
     public DomainTypeSpecificData getDomainSpecificData() {
         switch (this.type) {
             case LOCAL:
                 return new LocalDomainSpecificData();
             case LDAP:
                 return new LDAPDomainSpecificData();
             case JWT:
                 return new JWTDomainSpecificData(); // Add this case
             default:
                 throw new IllegalArgumentException("Unsupported domain type: " + this.type);
         }
     }
     ```

3. Ensure proper validation of `JWTDomainSpecificData` during domain creation or updates.

### Adjustments to `UserManagement`

**Purpose:**
- Update the `UserManagement` class to recognize and manage the new JWT domain.

**Steps:**
1. Register the new `JWT` domain type in the `UserManagement` class.
2. Update methods that iterate over domains to include logic for `JWT`.
   - Example:
     ```java
     public void authenticateUser(String username, String passwordOrToken) {
         for (Domain domain : domains) {
             if (domain.getType() == DomainType.JWT) {
                 JWTUserAuthentication auth = new JWTUserAuthentication((JWTDomainSpecificData) domain.getDomainSpecificData(), jwksCache);
                 auth.authenticateUser(username, passwordOrToken);
             }
             // Handle other domain types...
         }
     }
     ```
3. Add unit tests to ensure that `UserManagement` correctly handles JWT domains.

---

## 3. Unit Tests

### Class: `JWTUserAuthenticationTest`

**Purpose:**
- Validate the functionality of the `JWTUserAuthentication` class.
- Ensure proper handling of valid and invalid tokens.
- Test integration with `JWKSCache` and `OIDCSigningKeyResolver`.

**Test Cases:**

1. **Valid Token:**
   - **Scenario:** Token with a trusted issuer, valid `kid`, valid signature, and role.
   - **Expected Result:** Authentication succeeds, and the correct role is returned.

2. **Untrusted Issuer:**
   - **Scenario:** Token with an untrusted issuer.
   - **Expected Result:** Authentication fails with `XFMG_UserAuthenticationFailedException`.

3. **Invalid `kid`:**
   - **Scenario:** Token with a `kid` not found in the JWKS.
   - **Expected Result:** Authentication fails with `XFMG_UserAuthenticationFailedException`.

4. **Expired Token:**
   - **Scenario:** Token with an expired `exp` claim.
   - **Expected Result:** Authentication fails with `XFMG_UserAuthenticationFailedException`.

5. **No Role in Token:**
   - **Scenario:** Token without a role claim, but a default role is configured.
   - **Expected Result:** Authentication succeeds, and the default role is returned.

6. **Invalid Signature:**
   - **Scenario:** Token with an invalid signature.
   - **Expected Result:** Authentication fails with `XFMG_UserAuthenticationFailedException`.

7. **OIDC Discovery Failure:**
   - **Scenario:** OIDC discovery or JWKS retrieval fails.
   - **Expected Result:** Authentication fails with `XFMG_UserAuthenticationFailedException`.

8. **JWKS Cache Refresh:**
   - **Scenario:** JWKS cache is refreshed, and a new key is used for validation.
   - **Expected Result:** Authentication succeeds with the new key.

**Mocking:**
- Mock HTTP requests for OIDC discovery and JWKS retrieval.
- Mock `OIDCSigningKeyResolver` to simulate key resolution.
- Mock `JWKSCache` to simulate cache behavior.

**Example Test Code:**
```java
@Test
public void testValidToken() {
    // Arrange
    String token = "<valid JWT token>";
    JWTDomainSpecificData domainData = new JWTDomainSpecificData(
        List.of("https://trusted-issuer.com"), "Authorization", Optional.of("role"), Optional.of("defaultRole")
    );
    JWKSCache mockCache = mock(JWKSCache.class);
    when(mockCache.getKey("https://trusted-issuer.com", "validKid"))
        .thenReturn(mock(Key.class));

    JWTUserAuthentication auth = new JWTUserAuthentication(domainData, mockCache);

    // Act
    Role role = auth.authenticateUserInternally("testUser", token);

    // Assert
    assertEquals("expectedRole", role.getName());
}
```

---

### OIDC Discovery

**Purpose:**
- Dynamically retrieve the JSON Web Key Set (JWKS) and other metadata for a trusted issuer.
- Ensure that the JWT validation process uses up-to-date keys and configuration.

**Steps for OIDC Discovery:**

1. **Construct the Discovery URL:**
   - The discovery URL is derived from the issuer URL by appending `/.well-known/openid-configuration`.
   - Example:
     ```java
     String discoveryUrl = issuer + "/.well-known/openid-configuration";
     ```

2. **Fetch the OIDC Configuration:**
   - Perform an HTTP GET request to the discovery URL.
   - Parse the JSON response to extract the `jwks_uri` (URL for the JWKS).
   - Example:
     ```java
     HttpResponse<String> response = HttpClient.newHttpClient().send(
         HttpRequest.newBuilder(URI.create(discoveryUrl)).GET().build(),
         HttpResponse.BodyHandlers.ofString()
     );
     JSONObject oidcConfig = new JSONObject(response.body());
     String jwksUri = oidcConfig.getString("jwks_uri");
     ```

3. **Fetch the JWKS:**
   - Perform an HTTP GET request to the `jwks_uri`.
   - Parse the JSON response to extract the keys.
   - Example:
     ```java
     HttpResponse<String> jwksResponse = HttpClient.newHttpClient().send(
         HttpRequest.newBuilder(URI.create(jwksUri)).GET().build(),
         HttpResponse.BodyHandlers.ofString()
     );
     JSONObject jwks = new JSONObject(jwksResponse.body());
     JSONArray keys = jwks.getJSONArray("keys");
     ```

4. **Cache the Keys:**
   - Store the keys in the `JWKSCache` for the corresponding issuer.
   - Example:
     ```java
     jwksCache.updateKeys(issuer, keys);
     ```

5. **Handle Errors Gracefully:**
   - If the discovery URL or `jwks_uri` is unreachable, log the error and use cached keys if available.
   - Example:
     ```java
     try {
         // Fetch OIDC configuration and JWKS
     } catch (IOException | InterruptedException e) {
         logger.warn("Failed to fetch JWKS for issuer: " + issuer, e);
         if (!jwksCache.hasKeys(issuer)) {
             throw new RuntimeException("No keys available for issuer: " + issuer);
         }
     }
     ```

**OIDC Discovery Integration in `JWTUserAuthentication`:**
- During the authentication process, if the keys for an issuer are not cached or are expired, trigger OIDC discovery to refresh the keys.
- Example:
  ```java
  if (!jwksCache.hasValidKeys(issuer)) {
      oidcKeyManager.refreshKeys(issuer);
  }
  ```

**Unit Tests for OIDC Discovery:**
1. **Valid Discovery:**
   - Mock the HTTP responses for the discovery URL and `jwks_uri`.
   - Verify that the keys are correctly cached.
2. **Invalid Discovery URL:**
   - Simulate a 404 or timeout for the discovery URL.
   - Verify that cached keys are used if available.
3. **JWKS Fetch Failure:**
   - Simulate a failure to fetch the JWKS.
   - Verify that an exception is thrown if no cached keys are available.
4. **Key Expiry:**
   - Simulate expired keys in the cache.
   - Verify that OIDC discovery is triggered to refresh the keys.

---

### Parsing and Validating the Token

**Purpose:**
- Extract claims from the JWT.
- Validate the token's signature, issuer, and claims.

**Steps for Parsing and Validating the Token:**

1. **Extract the Token from the Header:**
   - Retrieve the token from the HTTP request header specified in `JWTDomainSpecificData`.
   - Remove the `Bearer ` prefix if present.
   - Example:
     ```java
     String token = request.getHeader(domainSpecificData.getTokenHeaderName());
     if (token != null && token.startsWith("Bearer ")) {
         token = token.substring(7);
     }
     ```

2. **Decode the Token Payload (Optional):**
   - Decode the payload to extract the `iss` claim before signature validation.
   - Use Base64 decoding to parse the payload.
   - Example:
     ```java
     String[] parts = token.split("\\.");
     String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
     JSONObject payload = new JSONObject(payloadJson);
     String issuer = payload.getString("iss");
     ```

3. **Validate the Issuer:**
   - Check if the `iss` claim matches one of the `trustedIssuers` in `JWTDomainSpecificData`.
   - Example:
     ```java
     if (!domainSpecificData.getTrustedIssuers().contains(issuer)) {
         throw new XFMG_UserAuthenticationFailedException("Untrusted issuer: " + issuer);
     }
     ```

4. **Parse and Validate the Token with `jjwt`:**
   - Use `jjwt` to validate the token's signature and claims.
   - Configure the `JwtParser` with a `SigningKeyResolver` to dynamically resolve keys from the JWKS cache.
   - Example:
     ```java
     JwtParser parser = Jwts.parserBuilder()
         .setSigningKeyResolver(new OIDCSigningKeyResolver(jwksCache))
         .build();
     Jws<Claims> jws = parser.parseClaimsJws(token);
     Claims claims = jws.getBody();
     ```

5. **Validate Standard Claims:**
   - Check the `exp` (expiration) and `nbf` (not before) claims to ensure the token is valid.
   - Example:
     ```java
     Date now = new Date();
     if (claims.getExpiration().before(now)) {
         throw new XFMG_UserAuthenticationFailedException("Token has expired.");
     }
     if (claims.getNotBefore() != null && claims.getNotBefore().after(now)) {
         throw new XFMG_UserAuthenticationFailedException("Token is not yet valid.");
     }
     ```

6. **Extract the Role Claim:**
   - Use the `roleClaimPath` from `JWTDomainSpecificData` to extract the role from the claims.
   - If the role is not found, use the `defaultRole`.
   - Example:
     ```java
     String role = claims.get(domainSpecificData.getRoleClaimPath().orElse(""), String.class);
     if (role == null) {
         role = domainSpecificData.getDefaultRole().orElse(null);
     }
     ```

7. **Handle Errors Gracefully:**
   - Catch exceptions from `jjwt` and log the error.
   - Example:
     ```java
     try {
         Jws<Claims> jws = parser.parseClaimsJws(token);
     } catch (JwtException e) {
         throw new XFMG_UserAuthenticationFailedException("JWT validation failed", e);
     }
     ```

**Integration in `JWTUserAuthentication`:**
- The `authenticateUserInternally` method will implement the above steps to parse and validate the token.
- Example:
  ```java
  public Role authenticateUserInternally(String username, String token) throws XFMG_UserAuthenticationFailedException {
      // Extract and validate the token
      String issuer = extractIssuerFromToken(token);
      validateIssuer(issuer);
      Claims claims = parseAndValidateToken(token);

      // Extract the role
      String role = extractRoleFromClaims(claims);
      return new Role(role);
  }
  ```

**Unit Tests for Token Parsing and Validation:**
1. **Valid Token:**
   - Token with a valid signature, issuer, and claims → Parsing and validation succeed.
2. **Invalid Signature:**
   - Token with an invalid signature → Validation fails.
3. **Expired Token:**
   - Token with an expired `exp` claim → Validation fails.
4. **Untrusted Issuer:**
   - Token with an untrusted `iss` claim → Validation fails.
5. **Missing Role Claim:**
   - Token without a role claim → Default role is used.
6. **Malformed Token:**
   - Token with an invalid structure → Validation fails.