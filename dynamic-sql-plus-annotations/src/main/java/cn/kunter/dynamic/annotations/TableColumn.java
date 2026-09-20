package cn.kunter.dynamic.annotations;

import java.lang.annotation.*;

/**
 * 标记在实体类的字段上，用于覆盖默认的列名推断或标记该字段为主键等信息。
 * @author yangziran
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableColumn {

    /**
     * 指定对应的数据库列名。如果不指定，默认将字段驼峰命名转为下划线格式。
     */
    String value() default "";

    /**
     * 是否忽略该字段（即不生成到 DynamicSqlSupport 类中）。
     */
    boolean ignore() default false;
}
