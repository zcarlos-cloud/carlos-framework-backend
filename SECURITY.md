# Security Policy

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible to receive such patches depends on the CVSS
v3.0 Rating:

| Version | Supported          |
|---------|--------------------|
| 3.0.x   | :white_check_mark: |
| 2.x     | :x:                |
| 1.x     | :x:                |

## Reporting a Vulnerability

We take the security of Carlos Framework seriously. If you discover a security vulnerability, please follow the process
below.

### Process

1. **DO NOT** create a public GitHub issue
2. Email the security team at [security@example.com](mailto:security@example.com)
3. Include:
    - Description of the vulnerability
    - Steps to reproduce the issue
    - Possible impact
    - Suggested fix (if any)
    - Your contact information (optional)

### What Happens Next

- We will acknowledge receipt within 48 hours
- We will investigate and validate the issue within 5 business days
- We will work on a fix and coordinate disclosure
- We will credit you (if desired) in the security advisory

### Safe Harbor

We support safe harbor for security researchers:

- We will not pursue legal action for good-faith security research
- We ask that you make efforts to avoid privacy violations and service disruption
- We ask that you only access your own accounts in the course of testing

## Security Best Practices

### For Users

#### Configuration Security

- **Never** commit sensitive data (passwords, API keys, tokens) to version control
- Use environment variables for secrets
- Rotate credentials regularly
- Use strong passwords and encryption

#### Dependency Management

- Keep dependencies updated
- Use `mvn dependency-check` to scan for vulnerabilities
- Review dependencies for known CVEs
- Only use trusted repositories

#### Deployment Security

- Use HTTPS in production
- Enable CSRF protection
- Implement proper CORS policies
- Use security headers (HSTS, X-Frame-Options, etc.)
- Enable request rate limiting
- Log security events

### For Developers

#### Secure Coding Checklist

Use this checklist before submitting PRs:

- [ ] No hard-coded secrets or credentials
- [ ] All user inputs are validated and sanitized
- [ ] SQL queries use parameterized statements (never string concatenation)
- [ ] Output is properly escaped to prevent XSS
- [ ] Authentication is implemented correctly
- [ ] Authorization checks are in place
- [ ] Passwords are properly hashed (use BCrypt or Argon2)
- [ ] Session management is secure
- [ ] Sensitive data is encrypted at rest and in transit
- [ ] Error messages don't leak sensitive information
- [ ] File uploads are restricted by type and size
- [ ] Rate limiting is implemented
- [ ] Security-related operations are logged

#### Input Validation

```java
// ✅ CORRECT - Use validation annotations
public class UserCreateParam {
    @NotBlank(message = "Username required")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "Invalid username format")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;
}

// ❌ WRONG - No validation
public class UserCreateParam {
    private String username;  // Could be empty or invalid
    private String email;
}
```

#### SQL Injection Prevention

```java
// ✅ CORRECT - Use MyBatis-Plus parameterized queries
@Select("SELECT * FROM users WHERE username = #{username}")
User findByUsername(@Param("username") String username);

// ❌ WRONG - String concatenation
@Select("SELECT * FROM users WHERE username = '" + username + "'")
User findByUsername(String username);
```

#### XSS Prevention

```java
// ✅ CORRECT - Proper escaping
public String sanitize(String input) {
    return HtmlUtils.htmlEscape(input);
}

// ❌ WRONG - Direct output
public String display(String userInput) {
    return userInput;  // XSS vulnerability!
}
```

### Security Headers

Recommended security headers for Spring Boot applications:

```java
@Bean
public FilterRegistrationBean<Filter> securityHeadersFilter() {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setFilter((request, response, chain) -> {
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("X-XSS-Protection", "1; mode=block");
        res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        res.setHeader("Content-Security-Policy", "default-src 'self'");
        chain.doFilter(request, response);
    });
    registration.addUrlPatterns("/*");
    return registration;
}
```

## Known Security Considerations

### Framework-Specific

#### OAuth2 Authorization Server

- Default implementation uses in-memory token storage (not production-ready)
- Password grant type is intentionally disabled (Spring Authorization Server 1.x)
- Production should use Redis JDBC token store
- Implement proper user details service

#### License Management

- License generation module should NOT be included in production builds
- License verification relies on hardware fingerprinting
- Time-based licenses can be circumvented by system clock manipulation

#### Code Generation Tools

- GUI tools generate code based on database schema
- Review generated code for security before deployment
- Generated code may need additional security hardening

### Dependencies

Monitor these dependencies for security updates:

```bash
# Check for dependency vulnerabilities
mvn dependency-check:check

# Check for updates
mvn versions:display-dependency-updates
```

## Additional Resources

### Security Tools

- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) - Dependency vulnerability scanning
- [OWASP ZAP](https://www.zaproxy.org/) - Web application security scanner
- [Snyk](https://snyk.io/) - Open source security platform
- [SonarQube](https://www.sonarqube.org/) - Code quality and security analysis

### Learning Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/) - Web application security risks
- [Spring Security Documentation](https://spring.io/projects/spring-security) - Security best practices
- [Java Security Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/Java_Security_Cheat_Sheet.html) - OWASP
  Java guidelines

### Security Updates

Subscribe to security notifications:

- GitHub Security Advisories for this repository
- Spring Security announcements
- Dependency vulnerability databases

## Security Team

| Role          | Name   | Contact                                                 |
|---------------|--------|---------------------------------------------------------|
| Security Lead | [Name] | [security@example.com](mailto:security@example.com)     |
| Maintainer    | [Name] | [maintainer@example.com](mailto:maintainer@example.com) |

## Policy Updates

This security policy may be updated periodically. The latest version is always available in our repository
at [SECURITY.md](./SECURITY.md).

---

**Last Updated:** 2026-03-04

**Version:** 1.0
