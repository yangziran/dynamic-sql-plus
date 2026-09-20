package cn.kunter.dynamic.annotations;

import java.lang.annotation.*;

/**
 * 标记在实体类的字段上，用于标识该字段为数据库表的主键。
 * @author yangziran
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableId {

    /**
     * 指定确切的数据库列名。如果不指定，默认会将字段的驼峰命名转换为下划线格式。
     */
    String value() default "";

    /**
     * 是否为数据库自增主键。
     * 如果为 true，则在插入数据时会自动回写生成的主键值。
     */
    boolean autoIncrement() default false;
}
