package xyz.atsumeru.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JWTManager {
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final int DAYS = 30;

    public static String generateToken(String login) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(login)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(DAYS, ChronoUnit.DAYS)))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractUsername(String token) {
        return getTokenBody(token).getSubject();
    }

    public static Boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private static Claims getTokenBody(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new AuthenticationException("Access denied: " + e.getMessage()) {};
        }
    }

    private static boolean isTokenExpired(String token) {
        return getTokenBody(token).getExpiration().before(new Date());
    }
}