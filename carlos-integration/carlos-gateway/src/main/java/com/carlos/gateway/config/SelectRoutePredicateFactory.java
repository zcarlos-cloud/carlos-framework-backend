package com.carlos.gateway.config;

import com.carlos.gateway.config.SelectRoutePredicateFactory.Config;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.http.server.PathContainer;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;
import static org.springframework.http.server.PathContainer.parsePath;


/**
 * <p>
 * 路径选择断言
 * </p>
 *
 * @author Carlos
 * @date 2024/1/23 17:37
 */
@Slf4j
@Setter
public class SelectRoutePredicateFactory extends AbstractRoutePredicateFactory<Config> {


    private static final String MATCH_TRAILING_SLASH = "matchTrailingSlash";

    private PathPatternParser pathPatternParser = new PathPatternParser();

    private final ReentrantLock parserLock = new ReentrantLock();

    public SelectRoutePredicateFactory() {
        super(SelectRoutePredicateFactory.Config.class);
    }

    private static void traceMatch(String prefix, Object desired, Object actual, boolean match) {
        if (log.isTraceEnabled()) {
            String message = String.format("%s \"%s\" %s against value \"%s\"", prefix, desired,
                    match ? "matches" : "does not match", actual);
            log.trace(message);
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("patterns", MATCH_TRAILING_SLASH);
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST_TAIL_FLAG;
    }

    @Override
    public Predicate<ServerWebExchange> apply(SelectRoutePredicateFactory.Config config) {
        final ArrayList<PathPattern> pathPatterns = new ArrayList<>();
        parserLock.lock();
        try {
            config.getPatterns().forEach(pattern -> {
                PathPattern pathPattern = this.pathPatternParser.parse(pattern);
                pathPatterns.add(pathPattern);
            });
        } finally {
            parserLock.unlock();
        }
        return new GatewayPredicate() {
            @Override
            public boolean test(ServerWebExchange exchange) {
                PathContainer path = parsePath(exchange.getRequest().getURI().getRawPath());

                PathPattern match = null;
                for (int i = 0; i < pathPatterns.size(); i++) {
                    PathPattern pathPattern = pathPatterns.get(i);
                    if (pathPattern.matches(path)) {
                        match = pathPattern;
                        break;
                    }
                }

                if (match != null) {
                    traceMatch("Pattern", match.getPatternString(), path, true);
                    PathMatchInfo pathMatchInfo = match.matchAndExtract(path);
                    putUriTemplateVariables(exchange, pathMatchInfo.getUriVariables());
                    exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ATTR, match.getPatternString());
                    String routeId = (String) exchange.getAttributes().get(GATEWAY_PREDICATE_ROUTE_ATTR);
                    if (routeId != null) {
                        // populated in RoutePredicateHandlerMapping
                        exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR, routeId);
                    }
                    return true;
                } else {
                    traceMatch("Pattern", config.getPatterns(), path, false);
                    return false;
                }
            }

            @Override
            public Object getConfig() {
                return config;
            }

            @Override
            public String toString() {
                return String.format("Paths: %s", config.getPatterns());
            }
        };
    }

    @Data
    @Validated
    public static class Config {

        private List<String> patterns = new ArrayList<>();


    }

}
