# carlos-spring-boot-starter-mongodb

## 模块简介

`carlos-spring-boot-starter-mongodb` 是 Carlos Framework 的 MongoDB 数据库集成模块，基于 Spring Data MongoDB 提供对
MongoDB 数据库的访问支持。该模块提供了与 `carlos-spring-boot-starter-mybatis` 类似的基础设施，包括通用的 Service
基类、字段自动填充、分页支持、查询构建器等，使 MongoDB 开发体验与 MyBatis 保持一致性。

## 主要功能

### 1. Spring Data MongoDB 集成

基于 Spring Boot Starter Data MongoDB 提供完整的 MongoDB 访问支持：

```java
// 实体类定义
@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String email;
    private String phone;

    @Field("create_time")
    private LocalDateTime createTime;

    @Field("update_time")
    private LocalDateTime updateTime;
    
    @Field("create_by")
    private String createBy;
    
    @Field("update_by")
    private String updateBy;
}

// Repository 接口
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // 自定义查询方法
    User findByUsername(String username);

    List<User> findByEmailContaining(String email);

    @Query("{'createTime': {$gte: ?0}}")
    List<User> findAfterCreateTime(LocalDateTime time);
}
```

### 2. 通用 Service 基类

提供与 MyBatis-Plus 风格一致的 Service 基类：

```java
// 基础 Service 接口
public interface UserService extends BaseService<User, String> {
    // 自定义业务方法
    List<User> findByKeyword(String keyword);
}

// 基础 Service 实现
@Service
public class UserServiceImpl extends BaseServiceImpl<UserRepository, User, String> 
        implements UserService {

    @Override
    public List<User> findByKeyword(String keyword) {
        // 使用 Query Builder 构建查询
        Query query = MongoQueryBuilder.create()
            .like("username", keyword)
            .or(MongoQueryBuilder.create().like("email", keyword).build().getCriteria())
            .build();
        return getMongoTemplate().find(query, User.class);
    }
}
```

### 3. 字段自动填充

支持与 MyBatis-Plus 类似的字段自动填充机制：

```java
@Entity
@Data
public class Product {
    @Id
    private String id;
    
    private String name;
    private BigDecimal price;
    
    // 插入时自动填充
    @FieldFill(strategy = FieldFillStrategy.INSERT)
    private LocalDateTime createTime;
    
    // 插入和更新时自动填充
    @FieldFill(strategy = FieldFillStrategy.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @FieldFill(strategy = FieldFillStrategy.INSERT)
    private String createBy;
    
    @FieldFill(strategy = FieldFillStrategy.INSERT_UPDATE)
    private String updateBy;
}
```

### 4. MongoDB 查询构建器

提供便捷的查询条件构建：

```java
@Service
public class ProductService extends BaseServiceImpl<ProductRepository, Product, String> {

    public List<Product> searchProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        // 使用链式调用构建复杂查询
        Query query = MongoQueryBuilder.create()
            .like("name", keyword)
            .between("price", minPrice, maxPrice)
            .eq("status", 1)
            .build();
        
        return getMongoTemplate().find(query, Product.class);
    }

    public List<Product> findByCategoryOrTags(String category, List<String> tags) {
        Criteria categoryCriteria = Criteria.where("category").is(category);
        Criteria tagsCriteria = Criteria.where("tags").in(tags);
        
        Query query = MongoQueryBuilder.create()
            .or(categoryCriteria, tagsCriteria)
            .build();
        
        return getMongoTemplate().find(query, Product.class);
    }
}
```

### 5. MongoDB 工具服务

提供基于 MongoTemplate 的便捷操作：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MongoServiceTool mongoTool;

    public void processOrder(Order order) {
        // 保存订单
        mongoTool.save(order);
        
        // 更新库存
        mongoTool.updateFirst(
            Query.query(Criteria.where("productId").is(order.getProductId())),
            new Update().inc("stock", -order.getQuantity()),
            Product.class
        );
        
        // 分页查询订单
        Query query = new Query(Criteria.where("userId").is(order.getUserId()));
        ParamPage param = new ParamPage();
        param.setCurrent(1);
        param.setSize(10);
        
        Paging<Order> page = mongoTool.findPage(query, param, Order.class);
    }
}
```

### 6. MongoDB 分页支持

提供专门为 MongoDB 设计的分页工具：

```java
@Service
public class ProductService extends BaseServiceImpl<ProductRepository, Product, String> {

    public Paging<ProductDTO> getProductPage(ProductPageParam param) {
        // 构建查询条件
        Query query = new Query();
        if (param.getCategoryId() != null) {
            query.addCriteria(Criteria.where("categoryId").is(param.getCategoryId()));
        }
        
        // 执行分页查询
        Page<Product> page = mongoTemplate.find(
            query.with(pageable(param)), 
            Product.class
        );
        
        // 转换为 Paging 对象
        return pageConvert(page, this::convertToDTO);
    }

    private List<ProductDTO> convertToDTO(List<Product> products) {
        // 转换逻辑
        return products.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mongodb</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. MongoDB 配置

在 `application.yml` 中配置 MongoDB 连接：

```yaml
spring:
  data:
    mongodb:
      # 单机模式 - URI 方式
      uri: mongodb://username:password@localhost:27017/database
      
      # 或者分开配置
      host: localhost
      port: 27017
      database: carlos
      username: admin
      password: password
      
      # 连接池配置
      auto-index-creation: true

carlos:
  mongodb:
    # 是否启用 MongoDB 模块
    enabled: true
    # 是否启用字段自动填充
    field-fill-enabled: true
    # 字段名配置
    create-time-field: createTime
    update-time-field: updateTime
    create-by-field: createBy
    update-by-field: updateBy
    deleted-field: deleted
    version-field: version
```

### 3. 基本使用示例

#### 实体类定义：

```java
@Document(collection = "products")
@Data
public class Product {

    @Id
    private String id;

    @Indexed
    private String productCode;

    private String productName;

    private BigDecimal price;

    private Integer stock;

    @Field("category_id")
    private String categoryId;

    private List<String> tags;

    private Map<String, Object> attributes;

    // 公共字段 - 会被自动填充
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private Boolean deleted;
}
```

#### Repository 定义：

```java
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // 基本查询方法
    List<Product> findByProductNameContaining(String name);

    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    List<Product> findByTagsIn(List<String> tags);

    // 使用 @Query 注解
    @Query("{'price': {$gte: ?0, $lte: ?1}}")
    List<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    // 聚合查询
    @Aggregation(pipeline = {
        "{'$match': {'categoryId': ?0}}",
        "{'$group': {'_id': '$categoryId', 'total': {'$sum': '$stock'}}}"
    })
    List<CategoryStock> getStockByCategory(String categoryId);
}
```

#### Service 实现：

```java
@Service
public class ProductServiceImpl extends BaseServiceImpl<ProductRepository, Product, String> 
        implements ProductService {

    @Override
    public Product saveProduct(Product product) {
        // 业务逻辑
        if (product.getStock() < 0) {
            throw new ServiceException("库存不能为负数");
        }
        // save 方法会自动填充 createTime, updateTime, createBy, updateBy
        return save(product);
    }

    @Override
    public Paging<ProductVO> searchProducts(ProductQueryParam param) {
        // 使用 Query Builder 构建查询
        Query query = MongoQueryBuilder.create()
            .like("productName", param.getKeyword())
            .between("price", param.getMinPrice(), param.getMaxPrice())
            .eq("categoryId", param.getCategoryId())
            .build();

        // 执行分页查询
        Page<Product> page = getMongoTemplate().find(
            query.with(pageable(param)), 
            Product.class
        );
        
        // 转换结果
        return pageConvert(page, this::convertToVO);
    }
}
```

#### Controller 使用：

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Result<Product> createProduct(@RequestBody @Valid CreateProductRequest request) {
        Product product = convertToEntity(request);
        Product saved = productService.saveProduct(product);
        return Result.success(saved);
    }

    @GetMapping("/search")
    public Result<Paging<ProductVO>> searchProducts(ProductQueryParam param) {
        Paging<ProductVO> page = productService.searchProducts(param);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<ProductVO> getProduct(@PathVariable String id) {
        Product product = productService.getByIdOrNull(id);
        if (product == null) {
            throw new ServiceException("产品不存在");
        }
        return Result.success(convertToVO(product));
    }
}
```

### 4. 字段自动填充配置

实现自定义的元对象处理器（可选）：

```java
@Component
@ConditionalOnMissingBean
public class CustomMetaObjectHandler implements MetaObjectHandler {

    @Autowired
    private ApplicationExtend requestExtend;

    @Override
    public void insertFill(MetaObject metaObject) {
        // 设置创建时间
        strictFill(metaObject, "createTime", LocalDateTime::now);
        
        // 设置更新时间
        strictFill(metaObject, "updateTime", LocalDateTime::now);
        
        // 设置创建人
        Serializable userId = getUserId();
        if (userId != null) {
            strictFill(metaObject, "createBy", () -> userId);
            strictFill(metaObject, "updateBy", () -> userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 设置更新时间
        strictFill(metaObject, "updateTime", LocalDateTime::now);
        
        // 设置更新人
        Serializable userId = getUserId();
        if (userId != null) {
            strictFill(metaObject, "updateBy", () -> userId);
        }
    }

    private Serializable getUserId() {
        return requestExtend != null ? requestExtend.getUserId() : null;
    }
}
```

## 详细功能说明

### 1. 多数据源支持

支持配置多个 MongoDB 数据源：

```java
@Configuration
public class MultipleMongoConfig {

    @Primary
    @Bean(name = "primaryMongoTemplate")
    public MongoTemplate primaryMongoTemplate(
            @Qualifier("primaryMongoDatabaseFactory") MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }

    @Bean(name = "secondaryMongoTemplate")
    public MongoTemplate secondaryMongoTemplate(
            @Qualifier("secondaryMongoDatabaseFactory") MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }
}
```

### 2. 事务支持

MongoDB 4.0+ 支持事务，需要在配置中启用：

```yaml
spring:
  data:
    mongodb:
      # 副本集或分片集群才支持事务
      uri: mongodb://localhost:27017,localhost:27018,localhost:27019/database?replicaSet=rs0
```

使用事务：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MongoTemplate mongoTemplate;

    @Transactional
    public void createOrder(Order order) {
        // 保存订单
        mongoTemplate.save(order);

        // 扣减库存
        Query query = new Query(Criteria.where("productId").is(order.getProductId()));
        Update update = new Update().inc("stock", -order.getQuantity());
        mongoTemplate.updateFirst(query, update, Product.class);

        // 记录日志
        OrderLog log = new OrderLog(order.getId(), "CREATED");
        mongoTemplate.save(log);
    }
}
```

### 3. 索引管理

支持自动创建索引和手动管理索引：

```java
@Component
@RequiredArgsConstructor
public class IndexManager implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // 创建复合索引
        mongoTemplate.indexOps(Product.class).ensureIndex(
            new Index().on("categoryId", Sort.Direction.ASC)
                      .on("price", Sort.Direction.ASC)
                      .named("category_price_idx")
        );

        // 创建文本索引
        mongoTemplate.indexOps(Product.class).ensureIndex(
            new TextIndexDefinitionBuilder()
                .onField("productName")
                .onField("description")
                .build()
        );

        // 创建地理空间索引
        mongoTemplate.indexOps(Product.class).ensureIndex(
            new GeospatialIndex("location").named("location_idx")
        );
    }
}
```

### 4. 聚合框架支持

支持 MongoDB 强大的聚合框架：

```java
@Service
@RequiredArgsConstructor
public class ReportService {

    private final MongoTemplate mongoTemplate;

    public SalesReport getSalesReport(LocalDate startDate, LocalDate endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("orderDate").gte(startDate).lte(endDate)),
            Aggregation.group("productId")
                .sum("quantity").as("totalQuantity")
                .sum(ArithmeticOperators.Multiply.valueOf("price").multiplyBy("quantity")).as("totalAmount")
                .avg("price").as("avgPrice"),
            Aggregation.sort(Sort.Direction.DESC, "totalAmount"),
            Aggregation.limit(10)
        );

        AggregationResults<SalesData> results =
            mongoTemplate.aggregate(aggregation, "orders", SalesData.class);

        return new SalesReport(results.getMappedResults());
    }
}
```

### 5. 地理空间查询

支持地理空间数据存储和查询：

```java
@Service
@RequiredArgsConstructor
public class LocationService {

    private final MongoTemplate mongoTemplate;

    public List<Store> findNearbyStores(double longitude, double latitude, double maxDistanceMeters) {
        Point point = new Point(longitude, latitude);
        
        Query query = new Query(
            Criteria.where("location").near(point).maxDistance(maxDistanceMeters)
        );
        
        return mongoTemplate.find(query, Store.class);
    }

    public List<Store> findStoresInArea(Polygon polygon) {
        Query query = Query.query(Criteria.where("location").within(polygon));
        return mongoTemplate.find(query, Store.class);
    }
}
```

## 配置项说明

| 配置项                                           | 类型      | 默认值          | 说明              |
|-----------------------------------------------|---------|--------------|-----------------|
| `spring.data.mongodb.uri`                     | String  | -            | MongoDB 连接 URI  |
| `spring.data.mongodb.host`                    | String  | `localhost`  | 主机地址            |
| `spring.data.mongodb.port`                    | int     | `27017`      | 端口号             |
| `spring.data.mongodb.database`                | String  | -            | 数据库名            |
| `spring.data.mongodb.username`                | String  | -            | 用户名             |
| `spring.data.mongodb.password`                | String  | -            | 密码              |
| `spring.data.mongodb.authentication-database` | String  | `admin`      | 认证数据库           |
| `spring.data.mongodb.auto-index-creation`     | boolean | `true`       | 是否自动创建索引        |
| `carlos.mongodb.enabled`                      | boolean | `true`       | 是否启用 MongoDB 模块 |
| `carlos.mongodb.field-fill-enabled`           | boolean | `true`       | 是否启用字段自动填充      |
| `carlos.mongodb.create-time-field`            | String  | `createTime` | 创建时间字段名         |
| `carlos.mongodb.update-time-field`            | String  | `updateTime` | 更新时间字段名         |
| `carlos.mongodb.create-by-field`              | String  | `createBy`   | 创建人字段名          |
| `carlos.mongodb.update-by-field`              | String  | `updateBy`   | 更新人字段名          |
| `carlos.mongodb.deleted-field`                | String  | `deleted`    | 逻辑删除字段名         |
| `carlos.mongodb.version-field`                | String  | `version`    | 版本号字段名          |

## API 参考

### BaseService 接口

| 方法                                                     | 说明                    |
|--------------------------------------------------------|-----------------------|
| `save(T entity)`                                       | 保存实体（自动填充字段）          |
| `update(T entity)`                                     | 更新实体（自动填充字段）          |
| `saveBatch(List<T> list)`                              | 批量保存                  |
| `getById(ID id)`                                       | 根据 ID 查询（返回 Optional） |
| `getByIdOrNull(ID id)`                                 | 根据 ID 查询（返回对象或 null）  |
| `list()`                                               | 查询所有                  |
| `listByIds(List<ID> ids)`                              | 根据 ID 列表查询            |
| `removeById(ID id)`                                    | 根据 ID 删除              |
| `remove(T entity)`                                     | 删除实体                  |
| `removeBatch(List<T> list)`                            | 批量删除                  |
| `existsById(ID id)`                                    | 判断是否存在                |
| `count()`                                              | 统计数量                  |
| `pageable(ParamPage param)`                            | 生成分页对象                |
| `pageConvert(Page<T> page, PageConvert<V, T> convert)` | 分页转换                  |
| `field(Func1<T, ?> lambda)`                            | 获取字段名                 |

### MongoQueryBuilder 方法

| 方法                           | 说明     |
|------------------------------|--------|
| `eq(field, value)`           | 等于     |
| `ne(field, value)`           | 不等于    |
| `gt(field, value)`           | 大于     |
| `gte(field, value)`          | 大于等于   |
| `lt(field, value)`           | 小于     |
| `lte(field, value)`          | 小于等于   |
| `between(field, start, end)` | 范围查询   |
| `like(field, value)`         | 模糊查询   |
| `likeLeft(field, value)`     | 左模糊    |
| `likeRight(field, value)`    | 右模糊    |
| `in(field, values)`          | 在集合中   |
| `nin(field, values)`         | 不在集合中  |
| `isNull(field)`              | 为空     |
| `isNotNull(field)`           | 不为空    |
| `exists(field, exists)`      | 字段是否存在 |
| `or(criterias...)`           | OR 条件  |
| `and(criterias...)`          | AND 条件 |

## 依赖项

- `carlos-spring-boot-core`：基础工具类、通用字段抽象
- `spring-boot-starter-data-mongodb`：Spring Data MongoDB 核心依赖
- `mongo-plus-boot-starter`：Mongo-Plus 增强库

## 注意事项

### 1. 性能考虑

- MongoDB 适合文档型数据存储，不适合强事务场景
- 合理设计文档结构，避免嵌套过深
- 为常用查询字段创建索引
- 使用投影（Projection）减少返回数据量

### 2. 数据建模建议

- 根据查询模式设计文档结构
- 适当使用嵌入文档减少 JOIN 操作
- 控制文档大小，避免单个文档过大（16MB 限制）
- 考虑数据增长和分片策略

### 3. 使用建议

- 优先使用 Repository 接口的声明式查询
- 复杂查询使用 MongoTemplate 或 MongoQueryBuilder
- 批量操作使用 BulkOperations
- 定期优化索引和查询性能

### 4. 与 MyBatis 对比

| 特性    | MongoDB          | MyBatis     |
|-------|------------------|-------------|
| 数据模型  | 文档型（Schema-less） | 关系型（Schema） |
| 事务支持  | 4.0+ 支持多文档事务     | 完全支持        |
| 查询灵活性 | 丰富的查询和聚合         | SQL 标准查询    |
| 扩展性   | 原生水平扩展           | 依赖分库分表方案    |
| 适用场景  | 非结构化/半结构化数据      | 结构化关系数据     |

## 版本要求

- JDK 17+
- Spring Boot 3.5.9+
- Spring Data MongoDB 4.x+
- MongoDB Server 4.4+（建议 5.0+）

## 相关模块

- **carlos-spring-boot-core**：基础工具类、通用字段抽象
- **carlos-spring-boot-starter-mybatis**：关系型数据库访问，可对比参考
- **carlos-spring-boot-starter-redis**：缓存支持，可与 MongoDB 配合使用
