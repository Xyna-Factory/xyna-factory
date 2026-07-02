# Design Document: JWT Domain for User Authentication

## Overview

This document describes the current state of JWT authentication in Xyna.
The JWT domain supports two validation modes via `authValidationMode`:

- `JWT`: full signature validation via JJWT + JWKS/OIDC discovery (default)
- `HEADER`: no signature validation in Xyna; claims are read locally from the JWT payload

The mode is selected through domain-specific data (`setdomaintypespecificdata`).

**Note on role selection:** When multiple roles are returned from the JWT claims, the system now supports role
prioritization via the `roleOrder` parameter. Roles are matched against the `roleOrder` list (case-sensitive) after
prefix/suffix normalization. If no `roleOrder` is configured, the first normalized role from the token is selected.

---

## 1. Class Design

### 1.1 `JWTDomainSpecificData`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

**Implements:**
`DomainTypeSpecificData`

**Configuration attributes:**

- `List<String> trustedIssuers`
- `List<String> intendedAudience`
- `Optional<String> roleClaimPath`
- `Optional<String> defaultRole`
- `Optional<String> rolePrefix`
- `Optional<String> roleSuffix`
- `List<String> roleOrder` (optional, case-sensitive role priority list)
- `Optional<String> jwksUri` (optional, otherwise OIDC discovery is used)
- `AuthValidationMode authValidationMode` with values `HEADER|JWT` (default: `JWT`)

**Supported Authentication Modes:**

The `AuthValidationMode` determines how the JWT is validated:

- `JWT` (default): Full signature validation is performed. The parser loads keys from the configured `jwksUri` or via
  OIDC discovery from the issuer's well-known endpoint. This mode requires network access to the IdP/JWKS endpoint.

- `HEADER`: No signature validation is performed in Xyna; the JWT payload is decoded directly. This mode assumes a
  trusted reverse proxy (e.g., Apache with `mod_auth_openidc`) has already validated the token and prevented header
  spoofing. Issuer and audience checks are still performed.

---

### 1.2 `JWTUserAuthentication`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

**Extends:**
`UserAuthentificationMethod`

**Behavior per mode:**

1. `JWT`
    - Parser: `Jwts.parser().keyLocator(...).build().parseSignedClaims(token)`
    - Signature validation enabled
    - Issuer/audience checks enabled
    - Role resolved using `roleOrder` if available, else first normalized role, else `defaultRole`

2. `HEADER`
    - JWT payload is decoded without signature validation using Base64 decoding
    - Issuer/audience checks enabled
    - Role resolved using `roleOrder` if available, else first normalized role, else `defaultRole`

**Role resolution:**

Role selection follows this process:

1. Extract role(s) from the JWT claims using `roleClaimPath` (defaults to `"roles"`)
2. Normalize each role by trimming prefix/suffix (if configured) from matched candidates
3. If `roleOrder` is configured and non-empty:
    - Iterate through `roleOrder` list (in order) and match each role against the normalized roles using case-sensitive
      equality
    - Return the first role from `roleOrder` that exists in the normalized roles
4. Otherwise (no `roleOrder` or no match):
    - Return the first normalized role from the token
5. If no role is found:
    - Use `defaultRole` (if configured)
6. If still no role:
    - Authentication fails with an exception

**Important:** Role matching in `roleOrder` is case-sensitive. Roles from the token are normalized (prefix/suffix
removed) before matching against the `roleOrder` list.

---

### 1.3 `OIDCSigningKeyResolver`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

**Role:**

- implements JJWT key lookup (`Locator<Key>`)
- loads JWKS keys and caches them
- uses `jwksUri` if configured
- if `jwksUri` is not set: OIDC discovery via
  `{issuer}/.well-known/openid-configuration` and `jwks_uri` from the response

**Important operational note:**
In `JWT` mode, the Xyna container must have network access to the IdP/JWKS endpoint.
If that is not possible, use `HEADER`.

---

## 2. Integration

### 2.1 `DomainType.JWT`

`DomainType.generateDomainTypeSpecificData(...)` reads JWT-specific parameters from the CLI map.
Required:

- `trustedIssuers`
- `intendedAudience`

Optional:

- `roleClaimPath`
- `defaultRole`
- `rolePrefix`
- `roleSuffix`
- `jwksUri`
- `authValidationMode` (default: `JWT`)

Invalid values for `authValidationMode` raise `IllegalArgumentException`.

---

### 2.2 Login flow via H5Xdev

In the `/auth/externalUserLogin` flow, the token is read from the configured header (for example `OIDC_access_token`)
and passed to `authorizeSession(...)`.
If the target domain is of type `JWT`, `JWTUserAuthentication.authenticateUserInternally(...)` is used.

---

## 3. Configuration Examples

### 3.1 Strict JWT validation (default)

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=JWT \
  roleClaimPath=roles \
  defaultRole=PortalUser
```

### 3.2 Proxy-validated (`HEADER`)

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=HEADER \
  defaultRole=PortalUser
```

### 3.3 With role prioritization

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=JWT \
  roleClaimPath=roles \
  roleOrder=Admin,SuperUser,PortalUser \
  defaultRole=PortalUser
```

The `roleOrder` list will be matched in order (case-sensitive). In this example:

- If the token has role "Admin", it will be selected
- Else if the token has role "SuperUser", it will be selected
- Else if the token has role "PortalUser", it will be selected
- Else the first role from the token is selected
- If no role is present in the token, `defaultRole` ("PortalUser") is used

---

## 4. Test Cases

Recommended test matrix:

1. `JWT` + valid token + reachable IdP/JWKS -> success
2. `JWT` + IdP unreachable -> `XFMG_UserAuthenticationFailedException`
3. `JWT` + wrong issuer -> failure
4. `JWT` + wrong audience -> failure
5. `JWT` + valid token with multiple roles + `roleOrder` configured -> role selected from `roleOrder`
6. `JWT` + valid token with roles not in `roleOrder` -> first role from token selected
7. `HEADER` + valid claims -> success without network access
8. `HEADER` + missing/invalid claims -> failure
9. `HEADER` + valid token with `roleOrder` -> role selected from `roleOrder`

---

## 5. Security Notes

- `JWT` is the safest default mode.
- `HEADER` assumes that a trusted reverse proxy (for example Apache with `mod_auth_openidc`) performs token validation
  and prevents header spoofing.
- Role prioritization via `roleOrder` is applied after prefix/suffix normalization and is case-sensitive.
