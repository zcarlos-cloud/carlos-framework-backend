# Contributing to Carlos Framework

Thank you for your interest in contributing to Carlos Framework! This document provides guidelines and instructions for
contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Commit Convention](#commit-convention)
- [Pull Request Process](#pull-request-process)
- [Community](#community)

## Code of Conduct

This project adheres to a Code of Conduct. By participating, you are expected to uphold this code. Please
read [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) for details.

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven 3.8 or higher
- Git

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/yourusername/carlos-framework.git
   cd carlos-framework
   ```

3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/original-owner/carlos-framework.git
   ```

### Build the Project

```bash
# Build all modules
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests

# Build with specific profile
mvn clean install -P carlos-public
```

### Run Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run tests for specific module
cd carlos-spring-boot-starter-redis
mvn test
```

## How to Contribute

### Reporting Issues

Before creating an issue, please:

1. Check existing [issues](https://github.com/yourusername/carlos-framework/issues)
2. Use issue templates provided
3. Provide clear, detailed information:
    - Expected behavior vs actual behavior
    - Steps to reproduce
    - Environment details (OS, JDK, Maven versions)
    - Stack traces and logs (if applicable)

### Feature Requests

- Use the "Feature Request" issue template
- Clearly describe the proposed feature and its benefits
- Be open to discussion and feedback
- Consider contributing the implementation

### Documentation Improvements

Documentation is equally important as code. You can:

- Fix typos and grammar errors
- Improve clarity and structure
- Add code examples
- Translate documentation
- Update outdated information

### Code Contributions

#### Finding Issues to Work On

Look for issues labeled:

- `good first issue` - Great for beginners
- `help wanted` - Needs community contribution
- `bug` - Bug fixes needed
- `enhancement` - Feature improvements

## Development Workflow

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description

# Branch naming conventions:
# feature/: New features
# fix/: Bug fixes
# docs/: Documentation updates
# refactor/: Code refactoring
# test/: Test additions/improvements
```

### 2. Make Your Changes

- Follow the coding standards (see below)
- Write or update tests as needed
- Update documentation if required
- Ensure all tests pass

### 3. Test-Driven Development (TDD)

We encourage TDD approach:

1. **Red**: Write a failing test
2. **Green**: Write minimal code to pass the test
3. **Refactor**: Improve code quality

### 4. Commit Your Changes

Follow our [commit convention](#commit-convention):

```bash
git add .
git commit -m "feat: add new Redis cache functionality

- Implement distributed cache abstraction
- Add TTL support
- Include comprehensive unit tests"
```

### 5. Sync with Upstream

```bash
git fetch upstream
git checkout main
git merge upstream/main
git checkout feature/your-feature-name
git rebase main
```

### 6. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## Coding Standards

### Java Code Style

We follow the [Alibaba Java Coding Guidelines](https://github.com/alibaba/p3c):

#### Naming Conventions

- **Classes**: UpperCamelCase, abstract classes start with `Abstract`, exceptions end with `Exception`
- **Methods/Variables**: lowerCamelCase
- **Constants**: ALL_CAPS_WITH_UNDERSCORES
- **Packages**: all lowercase, single form words
- **Enums**: UpperCamelCase ending with `Enum`, values in ALL_CAPS

#### Code Formatting

- Use 4 spaces for indentation (NO tabs)
- Line length: maximum 120 characters
- Method length: maximum 80 lines
- Always use braces for control statements

#### Immutability (CRITICAL)

Always create new objects, never mutate:

```java
// ❌ WRONG - Mutates original object
public User updateUserName(User user, String name) {
    user.setName(name);
    return user;
}

// ✅ CORRECT - Immutable approach
public User updateUserName(User user, String name) {
    return User.builder()
        .id(user.getId())
        .name(name)
        .email(user.getEmail())
        .build();
}
```

### File Organization

- Prefer "many small files" over "few large files"
- Target: 200-400 lines per file, maximum 800 lines
- Extract utility functions from large components
- Organize by feature/domain, not by type

### Exception Handling

Use framework-specific exceptions, never generic ones:

```java
// ✅ CORRECT
throw new ServiceException("User not found");
throw new DaoException("Database connection failed");
throw new OrgModuleException("Department not found");

// ❌ WRONG
throw new RuntimeException("Error occurred");
throw new Exception("Something went wrong");
```

### Input Validation

Always validate user input:

```java
public class UserCreateParam {
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 20, message = "Username length must be 3-20 characters")
    private String username;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;
}
```

### Logging

Use proper logging levels:

```java
// Use slf4j with Lombok @Slf4j
log.error("Operation failed with userId: {}", userId, exception);  // Errors
log.warn("Cache miss for key: {}", cacheKey);                    // Warnings
log.info("User {} logged in successfully", username);            // Important events
log.debug("Processing order: {}", orderId);                      // Debug info
log.trace("Detailed trace information");                         // Very detailed
```

### Security Best Practices

- Never hard-code credentials
- Validate all inputs
- Use parameter binding (not string concatenation) for SQL
- Sanitize user input to prevent XSS
- Enable CSRF protection
- Implement rate limiting
- Don't expose sensitive data in error messages

## Testing Requirements

### Minimum Coverage: 80%

All contributions must include tests with minimum 80% coverage:

#### Test Types Required

1. **Unit Tests** - Test individual functions/components
2. **Integration Tests** - Test API endpoints and database operations
3. **E2E Tests** - Test critical user flows (Playwright)

#### Test Naming

```java
@Test
@DisplayName("Should create user successfully when params valid")
void should_create_user_successfully_when_params_valid() {
    // given
    UserCreateParam param = new UserCreateParam();
    param.setUsername("testuser");
    param.setEmail("test@example.com");

    // when
    User user = userService.createUser(param);

    // then
    assertNotNull(user);
    assertEquals("testuser", user.getUsername());
    verify(userMapper, times(1)).insert(any(User.class));
}
```

#### Testing Guidelines

- **A**utomatic - Fully automated, non-interactive
- **I**ndependent - Tests don't depend on each other
- **R**epeatable - Consistent results regardless of environment

#### Mock Usage

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void should_send_email_when_user_created() {
        // Arrange
        User user = new User("test@example.com", "Test User");
        when(userMapper.save(any(User.class))).thenReturn(user);

        // Act
        userService.createUser(user);

        // Assert
        verify(emailService).sendWelcomeEmail(user.getEmail());
    }
}
```

## Commit Convention

We follow [Conventional Commits](https://conventionalcommits.org/):

### Format

```
type(scope): description

[optional body]

[optional footer(s)]
```

### Types

- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `style` - Code style changes (formatting, etc.)
- `refactor` - Code refactoring
- `perf` - Performance improvements
- `test` - Adding tests
- `chore` - Maintenance tasks
- `ci` - CI/CD changes

### Examples

```bash
# Simple feature
git commit -m "feat: add Redis TTL support"

# Feature with scope
git commit -m "feat(redis): add distributed lock implementation

- Add RedisLock annotation
- Implement lock acquisition and release
- Include unit tests with 90% coverage

Closes #123"

# Bug fix
git commit -m "fix(security): prevent SQL injection in search endpoint

- Use parameterized queries
- Add input validation
- Add security tests

Fixes #456"

# Breaking change
git commit -m "feat(api)!: change user DTO structure

BREAKING CHANGE: UserDTO now uses Long for id instead of String"
```

## Pull Request Process

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run all tests**:
   ```bash
   mvn clean test
   mvn clean verify  # Include integration tests
   ```

3. **Check code quality**:
   ```bash
   mvn checkstyle:check
   mvn spotbugs:check
   ```

4. **Verify documentation**:
    - Update README.md if needed
    - Update CLAUDE.md for developer docs
    - Check for broken links

### PR Template

When creating a Pull Request, use the provided template and include:

#### Summary

Brief description of changes (1-2 sentences).

#### What Changed

- Bullet points of specific changes
- New features added
- Bugs fixed
- Refactoring done

#### Why

Explain the motivation and benefits:

- Problem being solved
- Performance improvements
- Security enhancements
- User experience improvements

#### Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] E2E tests added/updated
- [ ] Code coverage >= 80%
- [ ] Manual testing completed

#### Breaking Changes

Note any breaking changes:

- API changes
- Configuration changes
- Database schema changes

#### Migration Guide

If there are breaking changes, provide migration steps.

#### Screenshots/Examples

Include relevant screenshots, code examples, or API usage samples.

#### Checklist

- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Code is commented where needed
- [ ] Corresponding documentation updated
- [ ] Tests added and passing
- [ ] Commit messages follow convention
- [ ] PR description is clear and complete

### PR Review Process

1. **Automated Checks** (Must Pass):
    - CI/CD pipeline
    - Code coverage threshold (80%)
    - Code style checks

2. **Human Review**:
    - At least 1 approval from maintainers
    - Review for: logic, performance, security, maintainability
    - Address all review comments

3. **Merge**:
    - Maintainer merges after approval
    - Squash commits if needed
    - Delete feature branch after merge

## Community

### Communication Channels

- **Issues**: Bug reports, feature requests
- **Discussions**: Questions, ideas, general discussion
- **Pull Requests**: Code contributions

### Getting Help

1. Check [CLAUDE.md](./CLAUDE.md) - Comprehensive development guide
2. Search existing issues and discussions
3. Ask in GitHub Discussions
4. Create an issue with "question" label

### Becoming a Maintainer

Regular contributors may be invited to become maintainers:

- Consistent, quality contributions
- Understanding of project architecture
- Helpful in reviews and community support
- Alignment with project goals

## Resources

- [CLAUDE.md](./CLAUDE.md) - Complete development guide
- [Architecture Documentation](./README.md#architecture-layers)
- [Module Structure](./README.md#module-structure)
- [API Documentation](https://your-docs-link.com) (if available)

## Recognition

Contributors are recognized in:

- README.md contributors section
- Release notes
- Project website
- Special thanks for significant contributions

## Questions?

If you have questions about contributing, please:

1. Check this document first
2. Look at similar PRs/issues
3. Ask in GitHub Discussions
4. Contact maintainers

Thank you for contributing to Carlos Framework! 🎉
