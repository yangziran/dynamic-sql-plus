package cn.kunter.dynamic.processor.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 字符串工具类的单元测试。
 * @author yangziran
 */
class StringUtilsTests {

    @Test
    void testCamelToSnake() {
        assertEquals("user_name", StringUtils.camelToSnake("userName"));
        assertEquals("status", StringUtils.camelToSnake("status"));
        assertEquals("create_time_str", StringUtils.camelToSnake("createTimeStr"));
        assertEquals("id", StringUtils.camelToSnake("id"));
        assertEquals("", StringUtils.camelToSnake(""));
        assertNull(StringUtils.camelToSnake(null));
    }

    @Test
    void testInferTableName() {
        assertEquals("user", StringUtils.inferTableName("UserEo"));
        assertEquals("system_user", StringUtils.inferTableName("SystemUserEntity"));
        assertEquals("product", StringUtils.inferTableName("Product"));
        assertEquals("sys_role", StringUtils.inferTableName("SysRole"));
        assertEquals("", StringUtils.inferTableName(""));
        assertNull(StringUtils.inferTableName(null));
    }
}
