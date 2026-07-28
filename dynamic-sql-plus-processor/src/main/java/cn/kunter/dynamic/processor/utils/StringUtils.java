package cn.kunter.dynamic.processor.utils;

/**
 * 字符串工具类，提供各种命名转换等能力，为代码生成服务。
 * @author yangziran
 */
public class StringUtils {

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
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 根据实体类名推断默认的表名。
     * 规则：将驼峰转为下划线，如果以 _eo 或 _entity 结尾，则去掉该后缀。
     * 例如: UserEo -> user, SystemUserEntity -> system_user
     * @param className 实体类的简单类名
     * @return 推断出的表名
     */
    public static String inferTableName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        String temp = className;
        if (temp.endsWith("Eo") && temp.length() > 2) {
            temp = temp.substring(0, temp.length() - 2);
        } else if (temp.endsWith("Entity") && temp.length() > 6) {
            temp = temp.substring(0, temp.length() - 6);
        }
        return camelToSnake(temp);
    }
}
