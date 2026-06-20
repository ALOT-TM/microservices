package com.fluxusbackend.shared.application.security;

import com.fluxusbackend.authaccess.domain.model.aggregates.UserAccount;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenService(
            @Value("${authorization.jwt.secret:fluxusbackenddevsecretkeyfluxusbackenddevsecret}") String secret,
            @Value("${authorization.jwt.expiration-milliseconds:86400000}") long expirationMillis
    ) {
        this.signingKey = createSigningKey(secret);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UserAccount user) {
        var retail = user.getRetailUser().orElse(null);
        var beneficiary = user.getBeneficiaryUser().orElse(null);
        UserActor actor = retail != null ? UserActor.RETAIL : UserActor.BENEFICIARY;
        CompanyId companyId = retail == null ? null : new CompanyId(retail.getRetailCompanyId());
        Long beneficiaryInstitutionId = beneficiary == null ? null : beneficiary.getBeneficiaryInstitutionId();
        Long roleId = retail == null ? null : retail.getRole().getRoleId();
        String roleName = retail == null ? null : retail.getRole().getName();

        return generateToken(
                user.getUserId().value(),
                user.getEmail().value(),
                actor,
                companyId,
                beneficiaryInstitutionId,
                roleId,
                roleName
        );
    }

    public String generateToken(
            Long userId,
            String email,
            UserActor actor,
            CompanyId companyId,
            Long beneficiaryInstitutionId,
            Long roleId,
            String roleName
    ) {
        var now = new Date();
        var expirationDate = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("actor", actor == null ? null : actor.name())
                .claim("companyId", companyId == null ? null : companyId.value())
                .claim("beneficiaryInstitutionId", beneficiaryInstitutionId)
                .claim("roleId", roleId)
                .claim("roleName", roleName)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
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
            var roleName = claims.get("roleName", String.class);
            var actor = actorText == null ? null : UserActor.valueOf(actorText);
            
            Long companyLong = parseLongClaim(claims, "companyId");
            var companyId = companyLong == null ? null : new CompanyId(companyLong);
            
            Long beneficiaryInstitutionId = parseLongClaim(claims, "beneficiaryInstitutionId");
            Long roleId = parseLongClaim(claims, "roleId");
            
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

    public String getBearerTokenFrom(jakarta.servlet.http.HttpServletRequest request) {
        return getBearerTokenFrom(request.getHeader("Authorization"));
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String stripBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    private Long parseLongClaim(Claims claims, String key) {
        Object claim = claims.get(key);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private SecretKey createSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
