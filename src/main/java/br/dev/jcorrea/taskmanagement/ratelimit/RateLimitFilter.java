package br.dev.jcorrea.taskmanagement.ratelimit;

import br.dev.jcorrea.taskmanagement.config.RateLimitConfig;
import br.dev.jcorrea.taskmanagement.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitConfig config;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitConfig config,
                           @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.config = config;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.clock = Clock.systemUTC();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            RateLimitConfig.Limit limit = resolveLimit(request);
            if (limit == null) {
                filterChain.doFilter(request, response);
                return;
            }
            String key = resolveKey(request, limit);
            Instant now = clock.instant();
            Bucket bucket = buckets.compute(key, (ignored, current) -> nextBucket(current, limit, now));
            cleanup(now);
            if (bucket.requests > limit.capacity()) {
                long retryAfter = Math.max(1, bucket.windowEndsAt.getEpochSecond() - now.getEpochSecond());
                log.info("Rate limit excedido para chave {}", key);
                throw new RateLimitExceededException("Limite de requisições excedido", retryAfter);
            }
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    private RateLimitConfig.Limit resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/signup".equals(path)) {
            return config.signup();
        }
        if ("POST".equals(method) && "/api/login".equals(path)) {
            return config.login();
        }
        if (path.startsWith("/api/")) {
            return config.api();
        }
        return null;
    }

    private String resolveKey(HttpServletRequest request, RateLimitConfig.Limit limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null ? "user:" + authentication.getName() : "ip:" + clientIp(request);
        return request.getMethod() + ":" + request.getRequestURI() + ":" + limit.capacity() + ":" + identity;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && isTrustedProxy(request.getRemoteAddr())) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr);
    }

    private Bucket nextBucket(Bucket current, RateLimitConfig.Limit limit, Instant now) {
        if (current == null || !now.isBefore(current.windowEndsAt)) {
            return new Bucket(1, now.plus(limit.window()));
        }
        return new Bucket(current.requests + 1, current.windowEndsAt);
    }

    private void cleanup(Instant now) {
        Iterator<Map.Entry<String, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Bucket> entry = iterator.next();
            if (now.isAfter(entry.getValue().windowEndsAt.plusSeconds(60))) {
                iterator.remove();
            }
        }
    }

    private record Bucket(int requests, Instant windowEndsAt) {
    }
}
