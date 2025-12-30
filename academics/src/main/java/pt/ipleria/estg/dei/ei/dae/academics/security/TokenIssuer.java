package pt.ipleria.estg.dei.ei.dae.academics.security;

import io.jsonwebtoken.Jwts;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
public class TokenIssuer {

    protected static final byte[] SECRET_KEY =
            "veRysup3rstr0nginv1ncible5ecretkeY@academics.dae.ipleiria".getBytes();

    protected static final String ALGORITHM = "HmacSHA384";
    public static final long EXPIRY_MINS = 60L;

    public static String issue(String username, String role) {

        LocalDateTime expiryPeriod = LocalDateTime.now().plusMinutes(EXPIRY_MINS);
        Date expirationDateTime = Date.from(
                expiryPeriod.atZone(ZoneId.systemDefault()).toInstant()
        );

        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)   // 👈 ESSENCIAL
                .issuedAt(new Date())
                .expiration(expirationDateTime)
                .signWith(key)
                .compact();
    }
}
