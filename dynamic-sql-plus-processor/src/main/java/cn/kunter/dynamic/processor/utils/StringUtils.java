package cn.kunter.dynamic.processor.utils;

/**
 * 字符串工具类，提供各种命名转换等能力，为代码生成服务。
 * @author yangziran
 */
public class StringUtils {

    private static final java.util.regex.Pattern CAMEL_PATTERN_1 = java.util.regex.Pattern.compile("([a-z])([A-Z]+)");
    private static final java.util.regex.Pattern CAMEL_PATTERN_2 = java.util.regex.Pattern.compile("([A-Z])" +
            "([A-Z][a-z])");

    /**
     * 将驼峰命名（CamelCase）字符串转换为下划线命名（snake_case）。
     * 例如: userId -> user_id, createTime -> create_time
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        String step1 = CAMEL_PATTERN_1.matcher(camelCase).replaceAll("$1_$2");
        return CAMEL_PATTERN_2.matcher(step1).replaceAll("$1_$2").toLowerCase();
    }

    public static String lowerFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static String getEntityPrefix(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        String temp = className;
        String upper = temp.toUpperCase();
        if ((upper.endsWith("EO") || upper.endsWith("DO") || upper.endsWith("PO")) && temp.length() > 2) {
            temp = temp.substring(0, temp.length() - 2);
        } else if (upper.endsWith("ENTITY") && temp.length() > 6) {
            temp = temp.substring(0, temp.length() - 6);
        }
        return temp;
    }

    /**
     * 根据实体类名推断默认的表名。
     * 规则：将驼峰转为下划线，如果以 _eo 或 _entity 结尾，则去掉该后缀。
     * 例如: UserEo -> user, SystemUserEntity -> system_user
     * @param className 实体类的简单类名
     * @return 推断出的表名
     */
    public static String inferTableName(String className) {
        return camelToSnake(getEntityPrefix(className));
    }
}
