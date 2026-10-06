# Carlos AI Starter

基于 LangChain4j 的 AI 集成 Starter，支持多种大语言模型和向量数据库。

## 功能特性

- **多模型支持**：通过引入对应 langchain4j starter 支持 OpenAI、智谱AI、百度千帆、阿里云 DashScope、Ollama
- **对话管理**：支持多轮对话、流式输出、对话历史记忆
- **嵌入向量**：文档向量化、相似度检索（需引入 langchain4j-redis 扩展向量存储）
- **简单集成**：Spring Boot 自动配置，开箱即用

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-ai</artifactId>
    <version>${carlos.version}</version>
</dependency>

<!-- 选择需要的模型提供商 starter（至少引入一个） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置 AI 参数

```yaml
carlos:
  ai:
    enabled: true
    default-provider: openai          # 默认使用的提供商名称
    memory-enabled: true              # 是否启用对话记忆
    memory-max-messages: 20           # 单会话最大记忆消息数
    memory-max-size: 10000            # 会话缓存最大条目数
    memory-expire-minutes: 120        # 会话无访问过期时间（分钟）

    providers:
      openai:
        api-key: ${OPENAI_API_KEY}
        model: gpt-4o
        temperature: 0.7
        max-tokens: 2000

      zhipu:
        api-key: ${ZHIPU_API_KEY}
        model: glm-4
        temperature: 0.7

      ollama:
        base-url: http://localhost:11434
        model: llama3

    embedding:
      enabled: false                  # 是否启用嵌入向量
      provider: openai                # 嵌入模型提供商
      model: text-embedding-3-small   # 嵌入模型名称
      store-type: redis               # 向量存储类型：redis / in-memory
      redis:
        host: localhost
        port: 6379
        index-name: framework:ai:embeddings
```

### 3. 使用 AI 服务

```java
@Autowired
private AiChatService chatService;

// 简单对话（使用默认提供商）
String response = chatService.chat("你好，请介绍一下 Spring Boot");

// 带上下文的对话
String response = chatService.chat("session-123", "你好");

// 流式输出
chatService.chatStream("你好", content -> {
    System.out.print(content);
});

// 切换模型（需引入对应 starter 并配置 providers）
String response = chatService.chatWithProvider("zhipu", "你好");
```

### 4. 使用嵌入向量服务

```java
@Autowired
private AiEmbeddingService embeddingService;

// 单条文本向量化（需 embedding.enabled=true）
List<Float> vector = embeddingService.embed("Spring Boot 是一个开源的 Java 框架...");

// 批量向量化
List<List<Float>> vectors = embeddingService.embed(List.of("文本1", "文本2"));
```

## 支持的模型提供商

| 提供商 | 配置名称 | 所需依赖 | 说明 |
|--------|----------|----------|------|
| OpenAI | `openai` | `langchain4j-open-ai-spring-boot-starter` | GPT-4, GPT-3.5 等 |
| 智谱AI | `zhipu` | `langchain4j-zhipu-ai-spring-boot-starter` | GLM-4, GLM-3 等 |
| 百度千帆 | `qianfan` | `langchain4j-qianfan-spring-boot-starter` | 文心一言等 |
| 阿里云 | `dashscope` | `langchain4j-dashscope-spring-boot-starter` | 通义千问等 |
| Ollama | `ollama` | `langchain4j-ollama-spring-boot-starter` | 本地模型，如 Llama3 |

## 配置属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `carlos.ai.enabled` | `true` | 是否启用 AI 功能 |
| `carlos.ai.default-provider` | `openai` | 默认模型提供商名称 |
| `carlos.ai.memory-enabled` | `true` | 是否启用对话记忆 |
| `carlos.ai.memory-max-messages` | `20` | 单会话最大记忆消息数 |
| `carlos.ai.memory-max-size` | `10000` | 会话缓存最大条目数 |
| `carlos.ai.memory-expire-minutes` | `120` | 会话无访问过期时间（分钟） |
| `carlos.ai.embedding.enabled` | `false` | 是否启用嵌入向量 |
| `carlos.ai.embedding.provider` | `openai` | 嵌入模型提供商 |
| `carlos.ai.embedding.store-type` | `redis` | 向量存储类型 |

## 版本历史

- **1.0.0** (2026-04-03)
  - 初始版本
  - 支持多模型对话配置结构
  - 支持嵌入向量配置
  - 对话记忆基于 Caffeine 缓存（TTL + 容量限制）
