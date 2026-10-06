# Carlos Framework 前端登录全流程对接指南

> **适用前端**: carlos-ui (Vue 3 + Soybean Admin Antd)
> **适用后端**: carlos-auth 统一登录体系 (feat/unified-login-architecture)
> **日期**: 2026-07-02

---

## 一、两种登录模式说明

### 模式 A：SPA 直连模式（当前 carlos-ui 使用）

前端直接调用 carlos-auth 的 JSON API 获取 Token，适合单一内部系统。

```
前端 → POST /auth/user/login → 拿到 accessToken → 存 localStorage → 后续请求带 Bearer Token
```

### 模式 B：标准 OIDC/SSO 模式（多系统 SSO）

前端跳转到 carlos-auth 的统一登录页（`/login`），登录成功后通过 `/oauth2/authorize` 拿到 code，换取 Token。适合多个独立系统共享登录态。

```
前端 → 302 /oauth2/authorize → 302 /login（统一登录页）→ 用户登录
→ 302 /oauth2/authorize → 302 redirect_uri?code=xxx → 前端用 code 换 Token
```

**当前阶段建议**: 保持模式 A，仅需适配新的 API 响应结构（MFA Token、风控挑战等）。后续全面 SSO 时切换到模式 B。

---

## 二、登录全流程时序图（模式 A - SPA 直连）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        完整登录流程（含预校验 + MFA）                          │
└─────────────────────────────────────────────────────────────────────────────┘

  carlos-ui (前端)                              carlos-auth (后端)
       │                                              │
       │──── Step 1: 登录预校验 ───────────────────────►
       │     POST /auth/user/pre-check                │
       │     {username, deviceFingerprint, loginType}  │
       │                                              │
       │◄─── 预校验响应 ──────────────────────────────│
       │     {riskLevel, needCaptcha, captchaType,     │
       │      mfaRequired, blocked}                    │
       │                                              │
       │     [如果 blocked=true → 显示拒绝提示，流程终止]
       │     [如果 needCaptcha=true → 显示验证码组件]
       │                                              │
       │──── Step 2: 提交登录 ─────────────────────────►
       │     POST /auth/user/login                     │
       │     {username, password, clientId,             │
       │      encrypted, captchaToken}                  │
       │                                              │
       │◄─── 登录响应 ───────────────────────────────│
       │                                              │
       ├── 情况 A: 直接成功（无需 MFA）                    │
       │   {accessToken, refreshToken, expiresIn,       │
       │    mfaRequired: false, userInfo}               │
       │   → 存 Token → 获取用户信息 → 跳转首页           │
       │                                              │
       ├── 情况 B: 需要 MFA Step-Up                      │
       │   {mfaRequired: true, mfaToken: "xxx"}         │
       │   → 显示 MFA 验证弹窗                           │
       │                                              │
       │──── Step 3: MFA 验证 ─────────────────────────►
       │     POST /auth/mfa/step-up-verify             │
       │     {mfaToken, code, trustDevice,              │
       │      deviceFingerprint, deviceName}             │
       │                                              │
       │◄─── MFA 验证响应 ──────────────────────────│
       │     {accessToken, refreshToken, expiresIn}     │
       │   → 存 Token → 获取用户信息 → 跳转首页           │
       │                                              │

  ─────────── Token 续期（后台自动） ───────────────────

       │──── POST /auth/user/refreshToken ────────────►
       │     {refreshToken: "xxx"}                     │
       │                                              │
       │◄─── {accessToken, refreshToken, expiresIn} ──│
       │   → 替换旧 Token（Refresh Rotation）            │
```

---

## 三、前端需要改造的文件

### 3.1 类型定义 `src/typings/api.d.ts`

```typescript
namespace Auth {
  /** 登录响应（兼容 MFA 两步流程） */
  interface LoginToken {
    /** 访问令牌（mfaRequired=true 时为空） */
    token: string;
    /** 刷新令牌 */
    refreshToken: string;
    /** 过期时间（秒） */
    expiresIn?: number;
    /** 令牌类型 */
    tokenType?: string;
    /** 是否需要 MFA 验证 */
    mfaRequired?: boolean;
    /** 是否建议启用 MFA */
    mfaRecommended?: boolean;
    /** MFA 临时令牌（mfaRequired=true 时返回） */
    mfaToken?: string;
  }

  /** 登录预校验响应 */
  interface PreCheckResponse {
    riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    needCaptcha: boolean;
    captchaType: 'slider' | 'behavior' | null;
    mfaRequired: boolean;
    blocked: boolean;
    blockReason: string | null;
  }

  interface UserInfo {
    userId: string;
    userName: string;
    roles: string[];
    buttons: string[];
  }
}
```

### 3.2 API Service 层 `src/service/api/auth.ts`

```typescript
import { request } from '../request';

/**
 * 登录预校验（风控决策）
 * 前端在提交登录前调用，决定是否需要验证码、MFA 等
 */
export function fetchPreCheck(username: string, deviceFingerprint: string) {
  return request<Api.Auth.PreCheckResponse>({
    url: '/auth/user/pre-check',
    method: 'post',
    data: {
      username,
      deviceFingerprint,
      loginType: 'password'
    }
  });
}

/**
 * 账号密码登录
 * 响应可能是 Token（直接成功）或 mfaToken（需要 MFA）
 */
export function fetchLogin(username: string, password: string) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/user/login',
    method: 'post',
    data: {
      username,
      password,
      clientId: 'carlos-client',
      encrypted: false
    }
  });
}

/**
 * MFA Step-Up 验证
 * 密码验证通过后，携带 mfaToken + TOTP code 完成二次验证
 */
export function fetchMfaStepUpVerify(params: {
  mfaToken: string;
  code: string;
  trustDevice?: boolean;
  deviceFingerprint?: string;
  deviceName?: string;
}) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/mfa/step-up-verify',
    method: 'post',
    data: params
  });
}

/**
 * 短信验证码登录
 */
export function fetchSmsLogin(phone: string, code: string) {
  return request<Api.Auth.LoginToken>({
    url: '/oauth2/token',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: `grant_type=sms_code&phone=${phone}&sms_code=${code}&client_id=carlos-client&client_secret=carlos-secret`
  });
}

/**
 * 发送短信验证码
 */
export function fetchSendSmsCode(phone: string) {
  return request({ url: '/auth/captcha/sms/send', method: 'post', data: { phone } });
}

/**
 * 获取扫码登录二维码
 */
export function fetchQrCodeGenerate() {
  return request<{ qrcodeId: string; qrcodeUrl: string; expiresAt: string; ttl: number }>({
    url: '/auth/qrcode/generate',
    method: 'post',
    params: { clientId: 'carlos-client' }
  });
}

/** 获取用户信息 */
export function fetchGetUserInfo() {
  return request<Api.Auth.UserInfo>({ url: '/auth/user/me' });
}

/** 刷新 Token */
export function fetchRefreshToken(refreshToken: string) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/user/refreshToken',
    method: 'post',
    params: { refreshToken }
  });
}

/** 登出 */
export function fetchLogout() {
  return request({ url: '/auth/user/logout', method: 'post' });
}
```

### 3.3 Auth Store 改造 `src/store/modules/auth/index.ts`

关键改动：`login` 方法需要处理 MFA 两步流程。

```typescript
// 新增状态
const mfaToken = ref('');
const mfaRequired = ref(false);

/**
 * 登录（含 MFA 两步流程处理）
 */
async function login(userName: string, password: string, redirect = true) {
  startLoading();

  // Step 1: 预校验（可选，用于前端提前展示验证码组件）
  // const { data: preCheck } = await fetchPreCheck(userName, getDeviceFingerprint());
  // if (preCheck?.blocked) { showError(preCheck.blockReason); endLoading(); return; }

  // Step 2: 提交登录
  const { data: loginToken, error } = await fetchLogin(userName, password);

  if (!error && loginToken) {
    // 情况 B: 需要 MFA
    if (loginToken.mfaRequired && loginToken.mfaToken) {
      mfaToken.value = loginToken.mfaToken;
      mfaRequired.value = true;
      endLoading();
      return; // 前端显示 MFA 弹窗，等用户输入
    }

    // 情况 A: 直接成功
    const pass = await loginByToken(loginToken);
    if (pass) {
      await redirectFromLogin(redirect);
      window.$notification?.success({
        message: $t('page.login.common.loginSuccess'),
        description: $t('page.login.common.welcomeBack', { userName: userInfo.userName })
      });
    }
  } else {
    resetStore();
  }

  endLoading();
}

/**
 * MFA 验证（Step 3）
 */
async function verifyMfa(code: string, trustDevice = false) {
  startLoading();

  const { data: loginToken, error } = await fetchMfaStepUpVerify({
    mfaToken: mfaToken.value,
    code,
    trustDevice,
    deviceFingerprint: getDeviceFingerprint(),
    deviceName: navigator.userAgent.substring(0, 50)
  });

  if (!error && loginToken) {
    mfaRequired.value = false;
    mfaToken.value = '';

    const pass = await loginByToken(loginToken);
    if (pass) {
      await redirectFromLogin(true);
      window.$notification?.success({
        message: '登录成功',
        description: 'MFA 验证通过'
      });
    }
  } else {
    window.$message?.error('MFA 验证码错误，请重试');
  }

  endLoading();
}

// 记得 return 新增的状态和方法
return {
  token, userInfo, isStaticSuper, isLogin, loginLoading,
  mfaRequired, // 新增
  resetStore, login, verifyMfa, initUserInfo // verifyMfa 新增
};
```

### 3.4 LoginToken 响应字段映射

后端 `LoginResponse` 的 JSON 字段与前端 `Api.Auth.LoginToken` 的映射：

| 后端字段 | 前端字段 | 说明 |
|----------|----------|------|
| `accessToken` | `token` | 需要在 request 层做映射（或改后端字段名） |
| `refreshToken` | `refreshToken` | 一致 |
| `expiresIn` | `expiresIn` | 一致 |
| `mfaRequired` | `mfaRequired` | 新增 |
| `mfaToken` | `mfaToken` | 新增 |
| `tokenType` | `tokenType` | Bearer |

**适配方案**：在 request 拦截器中做一层映射：

```typescript
// src/service/request/index.ts 或响应拦截器中
function transformLoginResponse(data: any): Api.Auth.LoginToken {
  return {
    token: data.accessToken || data.token,
    refreshToken: data.refreshToken,
    expiresIn: data.expiresIn,
    mfaRequired: data.mfaRequired,
    mfaToken: data.mfaToken,
    mfaRecommended: data.mfaRecommended
  };
}
```

---

## 四、前端登录页 UI 改造建议

当前 `carlos-ui` 已有的登录模块：
- `pwd-login.vue` — 账号密码
- `code-login.vue` — 验证码登录
- `bind-wechat.vue` — 微信绑定
- `register.vue` — 注册
- `reset-pwd.vue` — 找回密码

### 需要新增的模块

| 模块 | 功能 | 对接接口 |
|------|------|----------|
| `mfa-verify.vue` | MFA TOTP 验证弹窗 | `POST /auth/mfa/step-up-verify` |
| `qrcode-login.vue` | 扫码登录 Tab | `POST /auth/qrcode/generate` + SSE `/auth/qrcode/status/{id}` |
| `passkey-login.vue` | Passkey 按钮 | `POST /auth/passkey/authenticate/begin` + WebAuthn API |
| `captcha-widget.vue` | 滑块验证码组件 | 根据 preCheck 决定显示 |

### MFA 验证弹窗 `mfa-verify.vue`（示意）

```vue
<template>
  <AModal v-model:open="authStore.mfaRequired" title="MFA 二次验证" :closable="false" :footer="null">
    <p style="margin-bottom: 16px; color: #666">请输入验证器中的 6 位验证码</p>
    <AInput v-model:value="code" placeholder="6位验证码" :maxlength="6" size="large" @press-enter="handleVerify" />
    <ACheckbox v-model:checked="trustDevice" style="margin: 12px 0">7天内此设备免 MFA</ACheckbox>
    <AButton type="primary" block size="large" :loading="authStore.loginLoading" @click="handleVerify">
      验证
    </AButton>
  </AModal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '@/store/modules/auth';

const authStore = useAuthStore();
const code = ref('');
const trustDevice = ref(false);

function handleVerify() {
  authStore.verifyMfa(code.value, trustDevice.value);
}
</script>
```

---

## 五、Token 续期策略（Refresh Rotation）

后端已配置 `reuse-refresh-tokens: false`，每次刷新都签发新的 Refresh Token。前端需要：

```typescript
// request 拦截器中的 Token 续期逻辑
async function handleTokenRefresh() {
  const refreshToken = localStg.get('refreshToken');
  if (!refreshToken) return null;

  const { data } = await fetchRefreshToken(refreshToken);
  if (data) {
    // 重要：新的 refreshToken 替换旧的（Rotation）
    localStg.set('token', data.token);
    localStg.set('refreshToken', data.refreshToken);
    return data.token;
  }
  return null;
}
```

Access Token 15 分钟过期，前端拦截 401 响应时自动用 Refresh Token 续期。

---

## 六、扫码登录前端实现

```typescript
// qrcode-login.vue 核心逻辑
const { data } = await fetchQrCodeGenerate();
qrcodeUrl.value = data.qrcodeUrl;

// SSE 订阅状态
const eventSource = new EventSource(`/auth/qrcode/status/${data.qrcodeId}`);
eventSource.addEventListener('status', (event) => {
  const { state, accessToken, refreshToken } = JSON.parse(event.data);

  switch (state) {
    case 'SCANNED':
      statusText.value = '已扫码，请在 APP 确认';
      break;
    case 'CONFIRMED':
      localStg.set('token', accessToken);
      localStg.set('refreshToken', refreshToken);
      router.push('/');
      break;
    case 'EXPIRED':
      statusText.value = '二维码已过期';
      break;
  }
});
```

---

## 七、第三方 SSO 登录前端实现

```typescript
// 钉钉/微信登录
async function socialLogin(provider: 'dingtalk' | 'wechat') {
  const { data: authorizeUrl } = await request({
    url: '/auth/oauth2/authorize',
    params: {
      provider,
      redirectUri: `${location.origin}/social-callback`
    }
  });
  // 跳转到第三方授权页
  window.location.href = authorizeUrl;
}

// social-callback 页面处理回调
// URL: /social-callback?provider=dingtalk&isNewUser=false&state=xxx
// 如果 isNewUser=true → 引导绑定已有账号
// 如果 isNewUser=false → 调用 /oauth2/token grant_type=social 换 Token
```

---

## 八、最终的接口清单（前端需对接）

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/auth/user/pre-check` | POST | 登录预校验 | 无需 |
| `/auth/user/login` | POST | 密码登录 | 无需 |
| `/auth/mfa/step-up-verify` | POST | MFA 二次验证 | 无需（mfaToken 即凭证） |
| `/auth/user/refreshToken` | POST | Token 续期 | 无需（refreshToken 即凭证） |
| `/auth/user/me` | GET | 获取用户信息 | Bearer Token |
| `/auth/user/logout` | POST | 登出 | Bearer Token |
| `/auth/captcha/sms/send` | POST | 发送短信验证码 | 无需 |
| `/oauth2/token` | POST | 短信/社交登录 Token | client_id + client_secret |
| `/auth/qrcode/generate` | POST | 生成扫码二维码 | 无需 |
| `/auth/qrcode/status/{id}` | GET (SSE) | 订阅扫码状态 | 无需 |
| `/auth/passkey/authenticate/begin` | POST | Passkey 开始 | 无需 |
| `/auth/passkey/authenticate/finish` | POST | Passkey 完成 | 无需 |
| `/auth/oauth2/authorize` | GET | 获取第三方授权 URL | 无需 |
| `/auth/mfa/setup` | POST | MFA 设置（生成密钥） | Bearer Token |
| `/auth/mfa/verify` | POST | MFA 启用验证 | Bearer Token |
| `/auth/mfa/status` | GET | 查询 MFA 状态 | Bearer Token |
| `/auth/mfa/devices` | GET | 受信任设备列表 | Bearer Token |
| `/auth/mfa/devices/{id}` | DELETE | 移除受信任设备 | Bearer Token |

---

## 九、后端 LoginResponse 完整字段

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "mfaRequired": false,
  "mfaRecommended": false,
  "mfaToken": null,
  "userInfo": {
    "id": 10001,
    "username": "admin",
    "email": "admin@carlos.com",
    "phone": "13800000000"
  }
}
```

MFA 需要时（密码验证通过但还需二因素）：

```json
{
  "accessToken": null,
  "refreshToken": null,
  "tokenType": null,
  "expiresIn": null,
  "mfaRequired": true,
  "mfaRecommended": false,
  "mfaToken": "abc123def456789..."
}
```

---

## 十、迁移步骤总结

| 步骤 | 改动 | 工作量 |
|------|------|--------|
| 1 | `api.d.ts` 添加 `mfaRequired`/`mfaToken`/`PreCheckResponse` 类型 | 小 |
| 2 | `auth.ts` 新增 `fetchPreCheck`/`fetchMfaStepUpVerify`/`fetchQrCodeGenerate`/`fetchSmsLogin` | 小 |
| 3 | Auth Store `login` 方法增加 MFA 分支处理 + `verifyMfa` 新方法 | 中 |
| 4 | 新增 `mfa-verify.vue` MFA 弹窗组件 | 小 |
| 5 | 新增 `qrcode-login.vue` 扫码 Tab（SSE 订阅） | 中 |
| 6 | request 拦截器适配 `accessToken` → `token` 字段映射 | 小 |
| 7 | refreshToken 续期改造（401 拦截 → 自动刷新 → 重试） | 中 |
| 8 | （可选）新增 Passkey 登录 Tab | 后续 |
| 9 | （可选）第三方 SSO 回调页 `/social-callback` | 后续 |

总估时：前端改造约 **2-3 天**。核心是 Step 3（Store MFA 分支）和 Step 7（Token 续期），其余都是新增代码不影响现有功能。
