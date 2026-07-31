# Design Document: JWT Domain for User Authentication

## Overview
This document describes the current JWT authentication architecture in Xyna.

The JWT domain supports two validation modes via `authValidationMode`:

- `JWT`: full signature validation via JJWT + JWKS/OIDC discovery (default)
- `HEADER`: no signature validation in Xyna; claims are read from the JWT payload

The mode is selected through domain-specific data (`setdomaintypespecificdata`).

- JWT domain setup for the order-backed flow also requires an `ordertype` and optionally a runtime context (`application` + `version` or `workspace`) to resolve the revision.
- The JSONWebToken application exposes `authenticate` for this flow.
- It is possible for the JWT domain to choose a role in the login form via dropdown. For new gui versions, the role selection is part of the login request and validated server-side.
- If no role was selected, role selection is done internally during authentication: normalize -> apply `roleOrder` -> first role -> `defaultRole` fallback.

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
- `Optional<String> rolesResolverOrdertype` (optional, default: `xact.http.jwt.auth.ResolveAvailableRolesWithJWT`)
- `AuthValidationMode authValidationMode` with values `HEADER|JWT` (default: `JWT`)

Both `associatedOrdertype` and `rolesResolverOrdertype` use the same `RuntimeContext`/revision source
(from `application+version`).

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
- If a `selectedRole` was passed by the caller, stores it in order context under `xfmg.xopctrl.jwt.selectedRole`.
- Delegates JWT claim validation and role extraction to the JSONWebToken application.
- Translates successful `AuthenticationResult` into a local Xyna `Role`.

**selectedRole encoding:**

Because the authentication infrastructure only provides a single `password` field, an optional `selectedRole` is encoded into the password string using a `\0` separator:

```
password = "selectedRole\0jwtToken"   (if role was selected)
password = "jwtToken"                 (if no role was selected - backward compatible)
```

`\0` is safe here because JWT tokens are base64url-encoded and never contain this character.
`generateAuthOrder` splits on `\0` and sets both context keys independently.

Order context keys:

| Key | Value |
|---|---|
| `xfmg.xopctrl.jwt.token` | the actual JWT token |
| `xfmg.xopctrl.jwt.selectedRole` | the role chosen by the user (optional) |

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
  1. If `xfmg.xopctrl.jwt.selectedRole` is set **and** the role is present in the JWT claims -> use selected role *(verified, cannot be spoofed)*
  2. If `selectedRole` is set but **not** present in JWT claims -> authentication failure (reject)
  3. If no `selectedRole` -> first role from ordered/normalized role list
  4. otherwise `defaultRole`
  5. otherwise authentication failure

Public service surface:

- `authenticate(...)` - used in the order-backed auth flow
- `resolveAvailableRoles(...)` - called by `ResolveAvailableRolesWithJWT` workflow to populate the role dropdown in the GUI

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

- `trustedIssuers`
- `intendedAudience`
- `application` + `version` (runtime context for revision lookup)

Optional:

- `ordertype` (default: `xact.http.jwt.auth.AuthenticateWithJWT`)
- `roleClaimPath`
- `defaultRole`
- `rolePrefix`
- `roleSuffix`
- `roleOrder`
- `jwksUri`
- `rolesResolverOrdertype` (default: `xact.http.jwt.auth.ResolveAvailableRolesWithJWT`)
- `authValidationMode` (default: `JWT`)

Invalid values for `authValidationMode` raise `IllegalArgumentException`.

---

### 2.2 Login flow via H5Xdev

1. `/auth/externalUserLoginInformation`
   - Reads external identity from configured header/certificate.
   - Returns:
     - `username`
     - `userdisplayname`
     - `externaldomains` (legacy, for backward compatibility)
     - `domains` - list of `{ name, roles[] }` with available roles per domain, resolved via `rolesResolverOrdertype` (or default `ResolveAvailableRolesWithJWT`); role names are extracted from `xfmg.xopctrl.Role` objects returned by the workflow
   - Domain order is determined by the H5XdevFilter's `preferredDomain` parameter: if set, that domain appears first in the list.

2. `/auth/externalUserLogin`
   - Reads JWT token from configured header (for example `OIDC_access_token`).
   - Accepts optional `selectedRole` field in the request body.
   - If `selectedRole` is present, encodes it into the password as `selectedRole\0jwtToken`.
   - Calls `authorizeSession(...)`.
   - For JWT domains, `JWTUserAuthentication.authenticateUserInternally(...)` is used.
   - Role selection strategy (in `authenticate`):
     - **With `selectedRole`**: verified against JWT claims - used if valid, rejected if not present
     - **Without `selectedRole`**: highest-priority extracted role, then `defaultRole` (original behavior, fully backward compatible)

**Frontend flow (role dropdown):**

```
GET /auth/externalUserLoginInformation
  -> Response: { domains: [{ name, roles[] }] }
  -> GUI renders role dropdown (only if roles.length > 0)

POST /auth/externalUserLogin
  -> Body: { domain, force, path, selectedRole? }
  -> selectedRole is verified server-side against JWT claims
```

---

## 3. Configuration Examples

### 3.1 Strict JWT validation (default)

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  application=JSONWebToken \
  version=1.0.5 \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=JWT \
  roleClaimPath=roles \
  defaultRole=PortalUser
```

`ordertype` is omitted here and `xact.http.jwt.auth.AuthenticateWithJWT` is used by default.

### 3.2 Proxy-validated (`HEADER`)

```bash
./xynafactory.sh setdomaintypespecificdata \
  -domainName JWT_DOMAIN \
  -domainTypeSpecificData \
  application=JSONWebToken \
  version=1.0.5 \
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
  application=JSONWebToken \
  version=1.0.5 \
  trustedIssuers=https://idp.example.com/realms/master \
  intendedAudience=account \
  authValidationMode=JWT \
  roleClaimPath=roles \
  roleOrder=Admin,SuperUser,PortalUser \
  defaultRole=PortalUser
```

`roleOrder` is matched in configured order (case-sensitive) after normalization.

### 3.4 With H5XdevFilter preferred domain configuration

```bash
./xynafactory.sh deployfilter \
  -filterName H5XdevFilter \
  -filterInstanceName H5XdevFilterinstance \
  -triggerInstanceName HttpTrigger \
  -applicationName GuiHttp \
  -versionName 1.5.5 \
  -configurationParameter \
    externalAuthType=JSON_WEB_TOKEN \
    externalAuthHeader=OIDC_access_token \
    preferredDomain=JWT_DOMAIN
```

`preferredDomain` is a filter-level parameter (not domain-specific). It causes the named domain 
to appear first in the login form's domain dropdown, regardless of registration order. 
If not set or empty, the natural domain order is preserved.

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


---

## 5. Security Notes

- `JWT` is the safest default mode.
- `HEADER` assumes a trusted reverse proxy (for example Apache with `mod_auth_openidc`) validates tokens and prevents header spoofing.
- Role prioritization via `roleOrder` is applied after prefix/suffix normalization and is case-sensitive.
- **`selectedRole` cannot be spoofed**: even if a client sends an manipulated `selectedRole`, it is always verified against the roles extracted from the (validated) JWT claims. A role not present in the token's claims causes an immediate authentication failure.
- The `\0` encoding for `selectedRole` in the password field is an internal transport detail with no security implications, as the password is never persisted or exposed outside the authentication chain.
