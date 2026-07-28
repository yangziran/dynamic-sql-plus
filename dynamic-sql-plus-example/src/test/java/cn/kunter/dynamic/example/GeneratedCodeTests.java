package cn.kunter.dynamic.example;

import org.junit.jupiter.api.Test;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.util.mybatis3.CommonDeleteMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonInsertMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonSelectMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonUpdateMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生成代码结果的单元测试。
 * 通过反射验证生成的 DynamicSqlSupport 和 Mapper 接口结构是否符合预期。
 * @author yangziran
 */
class GeneratedCodeTests {

    @Test
    void testSupportClassGeneration() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        // 使用反射加载自动生成的类，确保在编译期正确生成
        Class<?> supportClass = Class.forName("cn.kunter.dynamic.example.entity.UserEoDynamicSqlSupport");
        
        // 1. 验证包含了表对象
        Field userField = supportClass.getDeclaredField("user");
        assertTrue(Modifier.isStatic(userField.getModifiers()));
        assertTrue(Modifier.isFinal(userField.getModifiers()));
        
        Object userTable = userField.get(null);
        assertTrue(userTable instanceof SqlTable);
        assertEquals("user", ((SqlTable) userTable).tableNameAtRuntime());

        // 2. 验证生成了基础的列
        Field idField = supportClass.getDeclaredField("id");
        assertTrue(idField.getType().isAssignableFrom(SqlColumn.class));

        // 3. 验证 @TableColumn 生效
        Field statusField = supportClass.getDeclaredField("status");
        SqlColumn<?> statusColumn = (SqlColumn<?>) statusField.get(null);
        assertEquals("user_status", statusColumn.name());

        // 4. 验证 @TableColumn(ignore = true) 生效
        boolean hasTemporaryToken = false;
        try {
            supportClass.getDeclaredField("temporaryToken");
            hasTemporaryToken = true;
        } catch (NoSuchFieldException e) {
            // 预期找不到该字段
        }
        assertTrue(!hasTemporaryToken, "应该忽略 temporaryToken 字段");
    }

    @Test
    void testMapperClassGeneration() throws ClassNotFoundException {
        // 使用反射加载自动生成的 Mapper 接口
        Class<?> mapperClass = Class.forName("cn.kunter.dynamic.example.entity.UserMapper");

        // 1. 验证是一个接口
        assertTrue(mapperClass.isInterface());

        // 2. 验证是否继承了所需的四大通用 Mapper
        Class<?>[] interfaces = mapperClass.getInterfaces();
        boolean hasSelect = false;
        boolean hasUpdate = false;
        boolean hasDelete = false;
        boolean hasInsert = false;

        for (Class<?> i : interfaces) {
            if (i.equals(CommonSelectMapper.class)) hasSelect = true;
            if (i.equals(CommonUpdateMapper.class)) hasUpdate = true;
            if (i.equals(CommonDeleteMapper.class)) hasDelete = true;
            if (i.equals(CommonInsertMapper.class)) hasInsert = true;
        }

        assertTrue(hasSelect, "需要继承 CommonSelectMapper");
        assertTrue(hasUpdate, "需要继承 CommonUpdateMapper");
        assertTrue(hasDelete, "需要继承 CommonDeleteMapper");
        assertTrue(hasInsert, "需要继承 CommonInsertMapper");

        // 3. 验证是否生成了 ByPrimaryKey 相关的方法
        boolean hasSelectByPk = false;
        boolean hasUpdateByPk = false;
        boolean hasDeleteByPk = false;
        boolean hasInsertOverride = false;

        for (java.lang.reflect.Method m : mapperClass.getDeclaredMethods()) {
            if (m.getName().equals("selectByPrimaryKey")) hasSelectByPk = true;
            if (m.getName().equals("updateByPrimaryKey")) hasUpdateByPk = true;
            if (m.getName().equals("deleteByPrimaryKey")) hasDeleteByPk = true;
            if (m.getName().equals("insert")) {
                org.apache.ibatis.annotations.Options options = m.getAnnotation(org.apache.ibatis.annotations.Options.class);
                if (options != null && options.useGeneratedKeys() && "row.id".equals(options.keyProperty())) {
                    hasInsertOverride = true;
                }
            }
        }

        assertTrue(hasSelectByPk, "需要生成 selectByPrimaryKey");
        assertTrue(hasUpdateByPk, "需要生成 updateByPrimaryKey");
        assertTrue(hasDeleteByPk, "需要生成 deleteByPrimaryKey");
        assertTrue(hasInsertOverride, "需要重写 insert 并带有 @Options 注解");
    }
}
