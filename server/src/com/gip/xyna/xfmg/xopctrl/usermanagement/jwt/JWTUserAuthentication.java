package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
/*import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;*/

import java.util.Optional;

public class JWTUserAuthentication extends UserAuthentificationMethod {

    private final JWTDomainSpecificData domainSpecificData;
    private final Object jwksCache;

    public JWTUserAuthentication(JWTDomainSpecificData domainSpecificData, Object jwksCache) {
        this.domainSpecificData = domainSpecificData;
        this.jwksCache = jwksCache;
    }

    @Override
    public Role authenticateUserInternally(String username, String token) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
        try {
            /*JwtParser parser = Jwts.parserBuilder()
                    .setSigningKeyResolver(new OIDCSigningKeyResolver(jwksCache))
                    .build();

            Jws<Claims> jws = parser.parseClaimsJws(token);
            Claims claims = jws.getBody();

            String issuer = claims.getIssuer();
            if (!domainSpecificData.getTrustedIssuers().contains(issuer)) {
                throw new XFMG_UserAuthenticationFailedException("Untrusted issuer: " + issuer);
            }*/

            String role = null; //claims.get(domainSpecificData.getRoleClaimPath().orElse(""), String.class);
            if (role == null) {
                role = domainSpecificData.getDefaultRole().orElse(null);
            }

            return com.gip.xyna.XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().getRole(role, UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
        } catch (/*Jwt*/Exception e) {
            throw new XFMG_UserAuthenticationFailedException("JWT validation failed", e);
        }
    }
}