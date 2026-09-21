# MyBatis Dynamic SQL Plus 架构设计文档

## 1. 背景与痛点分析

MyBatis Dynamic SQL 是官方推荐的现代化类型安全（Type-Safe）SQL 构建工具，彻底摒弃了繁琐易错的
XML。然而在实际使用中，其侵入性强、样板代码（Boilerplate）过多的问题让不少开发者望而却步。

### 1.1 痛点 1：繁琐的 Support 类

在使用原生的 MyBatis Dynamic SQL 时，对于每一个数据库表，开发者都必须手写一个对应的 `XxxDynamicSqlSupport` 类，在里面逐一声明每个字段的
`SqlColumn`，不仅冗长且难以维护。

### 1.2 痛点 2：冗余的 Mapper 接口

开发者依然需要手写 Mapper 接口，并使用 `@SelectProvider` 等大量注解将自定义的 Provider 与接口方法进行绑定，使得 Mapper
层异常臃肿。

## 2. 核心解决思路：编译期黑科技 (Annotation Processor)

参考 Lombok 与 MapStruct 的设计哲学，我们将打造一个名为 **MyBatis Dynamic SQL Plus** 的增强框架，核心口号是：
**“一个注解，消除所有动态 SQL 样板代码。”**

利用 Java 原生的 JSR 269 (Pluggable Annotation Processing API)，在代码 **编译期**（而非运行期反射）自动拦截指定注解，动态生成出原生的
Support 类和 Mapper 接口。

## 3. 核心设计与使用方式

### 3.1 极简的使用体验

开发者只需要在实体类（Entity/Eo）上打上 `@DynamicMapper` 核心注解：

```java
import cn.kunter.dynamic.annotations.DynamicMapper;
import lombok.Data;

@Data
@DynamicMapper // 核心魔法注解
public class UserEo {
    private Long id;
    private String username;
    private Integer status;
}
```

### 3.2 编译期的魔法生成

当执行 Maven 编译或 IDE Build 时，我们的 `Annotation Processor` 会自动扫描到 `@DynamicMapper`，并基于 JavaPoet 技术在
`target/generated-sources/annotations` 下 **自动生成**两份代码：

1. **`UserDynamicSqlSupport.java`**（自动解析 UserEo 内的字段生成对应的 SqlColumn）
2. **`UserMapper.java`**（自动继承 MyBatis 的 `SelectDSLCompleter` 等接口，并自带基础通用 CRUD Provider 绑定）

### 3.3 业务层的极致丝滑调用

因为 Mapper 和 Support 类已经在编译期生成就绪，在 `Service` 层，开发者可以直接注入 `UserMapper` 并享受原生 Lambda
带来的强类型安全体验，完全不需要知道那些繁琐类的存在：

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<UserDto> listActiveUsers() {
        // 直接使用生成的 UserDynamicSqlSupport 静态常量
        return userMapper.select(c -> c.where(UserDynamicSqlSupport.status, isEqualTo(1))
                                       .orderBy(UserDynamicSqlSupport.id.descending()));
    }
}
```

## 4. 技术栈选型与实现步骤

1. **核心注解模块 (`dynamic-sql-plus-annotations`)**
    - 定义 `@DynamicMapper` 以及辅助注解（如 `@TableColumn` 用于处理特殊字段映射）。
2. **编译期处理器模块 (`dynamic-sql-plus-processor`)**
    - 继承 `javax.annotation.processing.AbstractProcessor`。
    - 引入 `JavaPoet` (com.squareup:javapoet) 用于优雅地组装和输出 Java 源码字节流。
    - 解析 AST 语法树中的成员变量，利用 `Filer` 接口将生成的 Mapper 和 Support 写入到编译器的标准生成目录。
3. **整合打包与 Spring Boot Starter**
    - 向外输出轻量级依赖。使用者仅需引入依赖并配置好 `maven-compiler-plugin` 即可享受这套黑魔法。

## 5. 预期价值

- **零运行期损耗**：所有脏活累活全在编译期解决。
- **100% 原生兼容**：生成的代码依然是正统的 MyBatis Dynamic SQL 代码，不侵入 MyBatis 底层逻辑。
- **极大提升研发效能**：至少缩减单表开发中 50% 以上的数据访问层样板代码。
