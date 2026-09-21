# MyBatis Dynamic SQL Plus

MyBatis Dynamic SQL Plus 是一个基于 Java 原生编译期注解处理器（Annotation Processor，JSR 269）构建的轻量级增强框架。它的核心使命是：
**“一个注解，消除所有动态 SQL 样板代码。”**

## 背景与痛点

MyBatis
官方推荐的现代化数据访问工具是 [MyBatis Dynamic SQL](https://mybatis.org/mybatis-dynamic-sql/docs/introduction.html)
，它提供了极其优雅的、类型安全的（Type-Safe）动态 SQL 编写体验，彻底摒弃了繁琐且易错的 XML。

然而，在实际开发中，开发者必须为每一个数据库表手动编写对应的 `XxxDynamicSqlSupport` 类（逐一声明表名和列元数据），并手动编写带有大量
`@SelectProvider` 注解的 `Mapper` 接口。这种高度重复的样板代码使得开发体验变得冗长且难以维护。

## 我们的解决方案：编译期黑科技

参考 Lombok 的设计哲学，本框架将样板代码的生成前置到了 **代码编译期**。你只需要在实体类上添加一个简单的 `@DynamicMapper`
注解，我们的编译器插件就会在后台默默地为你生成所需的全部底层元数据类和通用的 CRUD Mapper 接口。

- 🚀 **零运行期损耗**：全部通过编译期 AST 语法树解析和源码生成实现，没有运行期反射开销。
- 🛡️ **完全原生兼容**：生成的代码 100% 遵循原生的 MyBatis Dynamic SQL 规范。
- ⚡ **极大提升效能**：告别繁琐的复制粘贴，专注核心业务逻辑。
- 🌍 **双版本并行**：本项目采用双分支策略， **2.x（main 分支）** 基于 Java 17 与 MyBatis Dynamic SQL 2.0+ 打造现代化体验；
  **1.x 分支** 继续支持 Java 8 与 MyBatis Dynamic SQL 1.5.x，为传统企业提供长期兜底。

> ⚠️ **【重要】版本与环境要求**
>
> 当前 `main` 分支为 **2.x 版本**，最低要求 **Java 17**，底层依赖 MyBatis Dynamic SQL 2.0+。
>
> * 如果你的项目仍运行在 **Java 8** 环境，请切换到 [`1.x` 分支](../../tree/1.x) 使用对应版本。
> * **2.x 版本** 全面适配了 MyBatis Dynamic SQL 2.0+ 的现代化 API（如 `isEqualTo` 替代已废弃的 `equalTo`），并享受最新特性的红利。

---

## 快速开始

### 1. 引入依赖 (通过 JitPack)

由于本项目托管在 GitHub 上，你可以通过 [JitPack](https://jitpack.io) 零配置引入。

首先，在你的 `pom.xml` 中添加 JitPack 仓库：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

然后引入 Starter 依赖：

```xml
<dependency>
    <groupId>cn.kunter</groupId>
    <artifactId>dynamic-sql-plus-spring-boot-starter</artifactId>
    <version>v2.0.0</version> <!-- 替换为 GitHub 上的最新 Release Tag -->
</dependency>
```

> **注意**：为了让编译器能够自动下载并发现 Annotation Processor，**必须**在使用本框架的业务模块的 `maven-compiler-plugin` 插件中配置 processor 路径。请将以下构建配置加入到您的业务 `pom.xml` 中：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>cn.kunter</groupId>
                        <artifactId>dynamic-sql-plus-processor</artifactId>
                        <version>v2.0.0</version> <!-- 保持与 starter 版本一致 -->
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. 配置 IDE（以 IntelliJ IDEA 为例）

由于本框架基于 Java 原生的编译期注解处理器生成代码，无需安装任何第三方插件。
你只需要在 IDEA 中依次打开：
`Settings (Preferences)` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`
勾选 **`Enable annotation processing`** 即可。
IDEA 会在后台自动为你生成代码，并将 `target/generated-sources/annotations` 标记为 Generated Sources Root，彻底告别红色波浪线！

### 3. 在实体类上添加注解

直接在你的普通 Java Bean（Entity/Eo）上添加 `@DynamicMapper` 即可：

```java
import cn.kunter.dynamic.annotations.DynamicMapper;
import cn.kunter.dynamic.annotations.TableColumn;
import cn.kunter.dynamic.annotations.TableId;

@DynamicMapper
public class UserEo {
    
    // 标识主键，开启自动递增后生成的 insert 方法会自动配置 useGeneratedKeys
    @TableId(autoIncrement = true)
    private Long id;
    private String username;
    
    // 如果字段名和数据库列名不一致，可以通过 @TableColumn 自定义映射
    @TableColumn(value = "user_status")
    private Integer status;
    
    // 如果该字段并非数据库字段，直接忽略即可
    @TableColumn(ignore = true)
    private String temporaryToken;
    
    // 省略 getter/setter
}
```

### 4. 享受极简的开发体验

执行 `mvn compile` 或在 IDE 中进行 Build 后，去看看 `target/generated-sources/annotations` 目录吧！你会发现两个被自动生成的类：

- `UserEoDynamicSqlSupport.java`：所有字段已经帮你完美映射为了 `SqlColumn`。
- `UserMapper.java`：自带 MyBatis 通用增删改查能力的接口。

在 Service 层，你现在可以直接注入 `UserMapper`，不仅可以使用全套动态 SQL 语法，还可以直接享受 **开箱即用、完全类型安全的单表
CRUD 方法**：

```java
@Service
public class UserServiceImpl {

    @Autowired
    private UserMapper userMapper;

    public void queryUsers() {
        // 1. 根据主键查询（返回 Optional，安全防空）
        Optional<UserEo> user = userMapper.selectByPrimaryKey(1L);
        
        // 2. 根据主键更新（自动过滤掉主键本身的更新）
        UserEo updateRecord = new UserEo();
        updateRecord.setId(1L);
        updateRecord.setStatus(0);
        userMapper.updateByPrimaryKey(updateRecord);
        
        // 3. 原生强类型动态 SQL 构建
        // userMapper.select(c -> c.where(UserEoDynamicSqlSupport.status, isEqualTo(1)));
    }
}
```

---

## 高级配置

### `@DynamicMapper`

- `tableName`：默认情况下，框架会将实体类名称由驼峰转为下划线，并自动去掉结尾的 `Eo` 或 `Entity` 作为表名（例如 `UserEo`
  会被推断为 `user`）。如果你有特定的表名前缀，可以通过 `@DynamicMapper(tableName = "sys_user")` 手动指定。

### `@TableId`

- `autoIncrement`：布尔值。如果为 `true`，会在底层生成的 `insert` 和 `insertMultiple` 方法上自动添加
  `@Options(useGeneratedKeys = true, keyProperty = "row.xxx")`，实现主键自动回填。

### `@TableColumn`

- `value`：指定确切的数据库列名。默认会将字段的驼峰命名转换为下划线（例如 `createTime` -> `create_time`）。
- `ignore`：如果为 `true`，则该字段会被处理器完全忽略，不会生成对应的 `SqlColumn`。

---

## 协议

本项目基于开源精神构建。欢迎提交 Issue 和 Pull Request，共同建设更优雅的 Java 基础设施生态。
