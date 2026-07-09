# Design Document: JWT Domain for User Authentication

## Overview
This document describes the current JWT authentication architecture in Xyna.

The JWT domain supports two validation modes via `authValidationMode`:

- `JWT`: full signature validation via JJWT + JWKS/OIDC discovery (default)
- `HEADER`: no signature validation in Xyna; claims are read from the JWT payload

The mode is selected through domain-specific data (`setdomaintypespecificdata`).

- JWT domain setup for the order-backed flow also requires an `ordertype` and optionally a runtime context (`application` + `version` or `workspace`) to resolve the revision.
- The JSONWebToken application exposes `authenticate` for this flow.
- Role selection is done internally during authentication: normalize -> apply `roleOrder` -> first role -> `defaultRole` fallback.

---

## 1. Class Design

### 1.1 `JWTDomainSpecificData`

Package:
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

Implements:
`DomainTypeSpecificData`

Configuration attributes:

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

- `JWT` (default): Full signature validation is performed. The parser loads keys from the configured `jwksUri` or via OIDC discovery from the issuer's well-known endpoint. This mode requires network access to the IdP/JWKS endpoint.

- `HEADER`: No signature validation is performed in Xyna; the JWT payload is decoded directly. This mode assumes a trusted reverse proxy (e.g., Apache with `mod_auth_openidc`) has already validated the token and prevented header spoofing. Issuer and audience checks are still performed.

---

### 1.2 `JWTUserAuthentication`

**Package:**
`com.gip.xyna.xfmg.xopctrl.usermanagement.jwt`

Extends:
`OrderBackedUserAuthentication`

Role in architecture:

- Creates an auth order for JWT domains.
- Stores JWT token in order context under `xfmg.xopctrl.jwt.token`.
- Delegates JWT claim validation and role extraction to the JSONWebToken application.
- Translates successful `AuthenticationResult` into a local Xyna `Role`.

Note:

- `JWTUserAuthentication` is order-backed and no longer performs a separate role-list API step for UI selection.

---

### 1.3 `JSONWebTokenAuthenticationServiceOperationImpl`

Package:
`xact.http.jwt.auth.impl`

Role:

- Entry point for JWT authentication inside the JSONWebToken application.
- Reads token from order context (`xfmg.xopctrl.jwt.token`).
- Resolves domain and validates domain type (`JWT`).
- Calls `JWTAuthenticationLogic.resolveAvailableRoles(...)` internally.
- Chooses role with this strategy:
  1. first role from ordered/normalized role list
  2. otherwise `defaultRole`
  3. otherwise authentication failure

Public service surface:

- `authenticate(...)` only
- No public `resolveAvailableRoles(...)` operation is used in this architecture.

---

### 1.4 `JWTAuthenticationLogic`

Package:
`xact.http.jwt.auth.impl`

Role:

- Validates claims according to configured validation mode.
- Enforces issuer and audience checks.
- Extracts roles from configured claim path.
- Normalizes roles (prefix/suffix trimming).
- Applies `roleOrder` (case-sensitive) and appends remaining roles in token order.

Role selection pipeline:

1. Extract role claim via `roleClaimPath` (default `roles`)
2. Normalize roles by prefix/suffix rules
3. Apply `roleOrder` (case-sensitive)
4. Return ordered roles list to authenticate flow

---

### 1.5 `OIDCSigningKeyResolver`

Package:
`xact.http.jwt.auth.impl`

Role:

- JJWT key locator for signature validation in `JWT` mode
- Loads and caches JWKS keys
- Uses configured `jwksUri`, or OIDC discovery via `{issuer}/.well-known/openid-configuration`

Operational note:

- In `JWT` mode, Xyna must reach IdP/JWKS endpoints.
- If that is not possible, use `HEADER` mode.

---

## 2. Integration

### 2.1 `DomainType.JWT`

`DomainType.generateDomainTypeSpecificData(...)` reads JWT-specific parameters from CLI map.

Required:

- `ordertype`
- `trustedIssuers`
- `intendedAudience`

Optional:

- `application` (together with `version`) or `workspace` for runtime context / revision lookup
- `version` (together with `application`)
- `roleClaimPath`
- `defaultRole`
- `rolePrefix`
- `roleSuffix`
- `roleOrder`
- `jwksUri`
- `authValidationMode` (default: `JWT`)

Invalid values for `authValidationMode` raise `IllegalArgumentException`.

---

### 2.2 Login flow via H5Xdev

1. `/auth/externalUserLoginInformation`
   - Reads external identity from configured header/certificate.
   - Returns only:
     - `username`
     - `userdisplayname`
     - `externaldomains`

2. `/auth/externalUserLogin`
   - Reads JWT token from configured header (for example `OIDC_access_token`).
   - Calls `authorizeSession(...)`.
   - For JWT domains, `JWTUserAuthentication.authenticateUserInternally(...)` is used.
   - Role is selected internally during auth processing (no user-side role choice).

---

## 3. Configuration Examples

### 3.1 Strict JWT validation (default)

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  ordertype=xact.http.jwt.auth.AuthenticateWithJWT \
  application=JSONWebToken \
  version=1.0.4 \
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
  ordertype=xact.http.jwt.auth.AuthenticateWithJWT \
  application=JSONWebToken \
  version=1.0.4 \
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
  ordertype=xact.http.jwt.auth.AuthenticateWithJWT \
  application=JSONWebToken \
  version=1.0.4 \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=JWT \
  roleClaimPath=roles \
  roleOrder=Admin,SuperUser,PortalUser \
  defaultRole=PortalUser
```

`roleOrder` is matched in configured order (case-sensitive) after normalization.

---

## 4. Test Cases

Recommended test matrix:

1. `JWT` + valid token + reachable IdP/JWKS -> success
2. `JWT` + IdP unreachable -> authentication failure
3. `JWT` + wrong issuer -> failure
4. `JWT` + wrong audience -> failure
5. `JWT` + valid token with multiple roles + `roleOrder` configured -> first matching role from `roleOrder`
6. `JWT` + valid token with roles not in `roleOrder` -> first normalized role from token
7. `JWT` + no extractable role + configured `defaultRole` -> default role selected
8. `HEADER` + valid claims -> success without network access
9. `HEADER` + missing/invalid claims -> failure
10. H5Xdev info endpoint returns no role list fields

---

## 5. Security Notes

- `JWT` is the safest default mode.
- `HEADER` assumes a trusted reverse proxy (for example Apache with `mod_auth_openidc`) validates tokens and prevents header spoofing.
- Role prioritization via `roleOrder` is applied after prefix/suffix normalization and is case-sensitive.
