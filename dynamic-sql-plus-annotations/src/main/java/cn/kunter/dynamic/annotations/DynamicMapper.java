package cn.kunter.dynamic.annotations;

import java.lang.annotation.*;

/**
 * 标记在实体类（Entity/Eo）上，用于在编译期自动生成 MyBatis Dynamic SQL 的 Support 类和 Mapper 接口。
 * @author yangziran
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamicMapper {

    /**
     * 指定表名。如果不指定，则默认将实体类名转为下划线格式（如 UserEo -> user）。
     */
    String tableName() default "";
}
