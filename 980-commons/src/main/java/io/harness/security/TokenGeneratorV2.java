package io.harness.security;

import static io.harness.network.Localhost.getLocalHostName;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenGeneratorV2 {
  private final String accountId;
  private static final String HOST_NAME = getLocalHostName();
  private static final long EXP_DURATION = Duration.ofMinutes(30).toMillis();
  Algorithm algorithm;

  @Inject
  public TokenGeneratorV2(String accountId, String accountSecret) {
    this.accountId = accountId;
    try {
      algorithm = Algorithm.HMAC256(accountSecret);
    } catch (Exception ex) {
      log.error("Error while initializing token generator", ex);
    }
  }

  public String getHS256JwtToken() {
    final long currentTime = System.currentTimeMillis();
    JWTCreator.Builder jwtBuilder =
        JWT.create()
            .withIssuer(HOST_NAME)
            .withSubject(accountId)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + EXP_DURATION))
            .withNotBefore(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));

    return jwtBuilder.sign(algorithm);
  }
}
