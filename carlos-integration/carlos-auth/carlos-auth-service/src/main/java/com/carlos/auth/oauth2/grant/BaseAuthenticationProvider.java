// package com.carlos.auth.oauth2.grant;
//
// import com.carlos.auth.exception.UserNotFoundException;
// import com.carlos.auth.exception.VerificationCodeException;
// import com.carlos.auth.oauth2.OAuth2ErrorCodesExpand;
// import com.carlos.core.exception.ComponentException;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.security.authentication.*;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.AuthenticationException;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.security.oauth2.core.*;
// import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
// import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
// import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
// import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
// import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
// import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
// import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
// import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
// import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
// import org.springframework.util.Assert;
// import org.springframework.util.CollectionUtils;
//
// import java.security.Principal;
// import java.time.Instant;
// import java.util.LinkedHashSet;
// import java.util.Map;
// import java.util.Objects;
// import java.util.Set;
//
// /**
//  * <p>
//  *
//  * </p>
//  *
//  * @author carlos
//  * @date 2022/11/4 17:42
//  */
// @Slf4j
// public abstract class BaseAuthenticationProvider<T extends BaseAuthenticationToken> implements AuthenticationProvider {
//
//     private static final String ERROR_URI = "";
//
//
//     private OAuth2AuthorizationService authorizationService;
//
//     private OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
//
//     private AuthenticationManager authenticationManager;
//
//
//     public BaseAuthenticationProvider(AuthenticationManager authenticationManager,
//                                       OAuth2AuthorizationService authorizationService,
//                                       OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
//         Assert.notNull(authorizationService, "authorizationService cannot be null");
//         Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
//         this.authenticationManager = authenticationManager;
//         this.authorizationService = authorizationService;
//         this.tokenGenerator = tokenGenerator;
//
//     }
//
//
//     public abstract UsernamePasswordAuthenticationToken buildToken(Map<String, String> reqParameters);
//
//     /**
//      * 当前provider是否支持此令牌类型
//      */
//     @Override
//     public abstract boolean supports(Class<?> authentication);
//
//     /**
//      * 当前的请求客户端是否支持此模式
//      */
//     public abstract void checkClient(RegisteredClient registeredClient);
//
//     @Override
//     public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//         T authenticationToken = (T) authentication;
//
//         Map<String, String> reqParameters = authenticationToken.getParameters();
//         UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = buildToken(reqParameters);
//
//         return doAuthenticate(authenticationToken, usernamePasswordAuthenticationToken);
//     }
//
//     /**
//      * 执行认证流程
//      *
//      * <p>支持子类传入自定义的用户认证信息（如第三方登录）。</p>
//      *
//      * @param authenticationToken 原始认证 Token
//      * @param userAuthenticationToken 用户认证 Token
//      * @return Authentication 认证结果
//      * @throws AuthenticationException 认证异常
//      */
//     protected Authentication doAuthenticate(T authenticationToken,
//                                             UsernamePasswordAuthenticationToken userAuthenticationToken)
//             throws AuthenticationException {
//
//         OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(authenticationToken);
//
//         RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
//         checkClient(registeredClient);
//
//         Set<String> authorizedScopes;
//         // Default to configured scopes
//         if (!CollectionUtils.isEmpty(authenticationToken.getScopes())) {
//             for (String requestedScope : authenticationToken.getScopes()) {
//                 if (!registeredClient.getScopes().contains(requestedScope)) {
//                     throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
//                 }
//             }
//             authorizedScopes = new LinkedHashSet<>(authenticationToken.getScopes());
//         } else {
//             throw new ComponentException("scope is null");
//         }
//
//         try {
//             UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = userAuthenticationToken;
//
//             log.debug("got usernamePasswordAuthenticationToken={}", usernamePasswordAuthenticationToken);
//
//             Authentication usernamePasswordAuthentication;
//             if (usernamePasswordAuthenticationToken != null) {
//                 usernamePasswordAuthentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
//             } else {
//                 // 如果传入 null，假设已经通过其他方式认证（如第三方登录）
//                 throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
//             }
//
//             // @formatter:off
//             DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
//                     .registeredClient(registeredClient)
//                     .principal(usernamePasswordAuthentication)
//                     // .providerContext(ProviderContextHolder.getProviderContext())
//                     .authorizedScopes(authorizedScopes)
//                     // 不设置 authorizationGrantType，因为这是扩展授权流程
//                     .authorizationGrant(authenticationToken);
//             // @formatter:on
//
//             OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
//                 .withRegisteredClient(registeredClient).principalName(usernamePasswordAuthentication.getName());
//             // 不设置 authorizationGrantType
//
//             // ----- Access token -----
//             OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
//             OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
//             if (generatedAccessToken == null) {
//                 OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
//                         "The token generator failed to generate the access token.", ERROR_URI);
//                 throw
//                         new OAuth2AuthenticationException(error);
//             }
//             OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
//                     generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
//                     generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
//             if (generatedAccessToken instanceof ClaimAccessor claimAccessor) {
//                 authorizationBuilder.id(accessToken.getTokenValue())
//                         .token(accessToken,
//                                 (metadata) -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
//                                     claimAccessor.getClaims()))
//                         // .attribute(OAuth2Authorization.AUTHORIZED_SCOPE_ATTRIBUTE_NAME, authorizedScopes)
//                         .attribute(Principal.class.getName(), usernamePasswordAuthentication);
//             } else {
//                 authorizationBuilder.id(accessToken.getTokenValue()).accessToken(accessToken);
//             }
//
//             // ----- Refresh token -----
//             OAuth2RefreshToken refreshToken = null;
//             if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN) &&
//                     // Do not issue refresh token to public client
//                     !clientPrincipal.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.NONE)) {
//
//                 if (this.refreshTokenGenerator != null) {
//                     Instant issuedAt = Instant.now();
//                     Instant expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getRefreshTokenTimeToLive());
//                     refreshToken = new OAuth2RefreshToken(this.refreshTokenGenerator.get(), issuedAt, expiresAt);
//                 } else {
//                     tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
//                     OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
//                     if (!(generatedRefreshToken instanceof OAuth2RefreshToken rt)) {
//                         OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
//                                 "The token generator failed to generate the refresh token.", ERROR_URI);
//                         throw new OAuth2AuthenticationException(error);
//                     }
//                     refreshToken = rt;
//                 }
//                 authorizationBuilder.refreshToken(refreshToken);
//             }
//
//             OAuth2Authorization authorization = authorizationBuilder.build();
//
//             this.authorizationService.save(authorization);
//
//             log.debug("returning OAuth2AccessTokenAuthenticationToken");
//
//             return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken,
//                     refreshToken, Objects.requireNonNull(authorization.getAccessToken().getClaims()));
//
//         } catch (Exception ex) {
//             log.error("problem in authenticate", ex);
//             throw oAuth2AuthenticationException(authenticationToken, (AuthenticationException) ex);
//         }
//
//     }
//
//     /**
//      * 登录异常转换为oauth2异常
//      *
//      * @param authentication          身份验证
//      * @param authenticationException 身份验证异常
//      * @return {@link OAuth2AuthenticationException}
//      */
//     private OAuth2AuthenticationException oAuth2AuthenticationException(Authentication authentication,
//                                                                         AuthenticationException authenticationException) {
//         return switch (authenticationException) {
//             case UsernameNotFoundException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.USERNAME_NOT_FOUND.toOAuth2Error());
//             case BadCredentialsException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.BAD_CREDENTIALS.toOAuth2Error());
//             case LockedException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.USER_LOCKED.toOAuth2Error());
//             case DisabledException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.USER_DISABLE.toOAuth2Error());
//             case AccountExpiredException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.USER_EXPIRED.toOAuth2Error());
//             case CredentialsExpiredException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.CREDENTIALS_EXPIRED.toOAuth2Error());
//             case VerificationCodeException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.VERIFICATION_CODE_ERROR.toOAuth2Error());
//             case UserNotFoundException ex ->
//                 new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.USER_NOT_FOUND.toOAuth2Error());
//             default -> new OAuth2AuthenticationException(OAuth2ErrorCodesExpand.UN_KNOW_LOGIN_ERROR.toOAuth2Error());
//         };
//     }
//
//     private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
//             Authentication authentication) {
//
//         OAuth2ClientAuthenticationToken clientPrincipal = null;
//
//         if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken token) {
//             clientPrincipal = token;
//         }
//
//         if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
//             return clientPrincipal;
//         }
//
//         throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
//     }
//
// }
