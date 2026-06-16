package com.fluxusbackend.shared.application.security;

import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final SecretKey signingKey;

    public JwtTokenService(@Value("${authorization.jwt.secret:fluxusbackenddevsecretkeyfluxusbackenddevsecret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(stripBearerPrefix(token));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Optional<AuthInfo> parseToken(String token) {
        try {
            var claims = parseClaims(stripBearerPrefix(token));
            var userId = claims.getSubject() == null ? null : Long.parseLong(claims.getSubject());
            var email = claims.get("email", String.class);
            var actorText = claims.get("actor", String.class);
            var companyClaim = claims.get("companyId");
            var beneficiaryClaim = claims.get("beneficiaryInstitutionId");
            var roleIdClaim = claims.get("roleId");
            var roleName = claims.get("roleName", String.class);
            var actor = actorText == null ? null : UserActor.valueOf(actorText);
            var companyId = companyClaim instanceof Number number ? new CompanyId(number.longValue()) : null;
            Long beneficiaryInstitutionId = beneficiaryClaim instanceof Number number ? number.longValue() : null;
            Long roleId = roleIdClaim instanceof Number number ? number.longValue() : null;
            return Optional.of(new AuthInfo(userId, email, actor, companyId, beneficiaryInstitutionId, roleId, roleName));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public String getBearerTokenFrom(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }

    private String stripBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    public record AuthInfo(
            Long userId,
            String email,
            UserActor actor,
            CompanyId companyId,
            Long beneficiaryInstitutionId,
            Long roleId,
            String roleName
    ) {
    }
}
