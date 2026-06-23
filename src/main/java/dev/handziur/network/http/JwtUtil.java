package dev.handziur.network.http;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;

public class JwtUtil {

    private static final String SECRET = "secret secret secret secret";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);

    public static String generateToken(String username) {
        return JWT.create()
                .withIssuer("store-server")
                .withClaim("user", username)
                .sign(ALGORITHM);
    }

    public static boolean verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM)
                    .withIssuer("store-server")
                    .build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }
}
