package cn.kunter.dynamic.processor.exception;

/**
 * MyBatis Dynamic SQL Plus 统一业务与运行时异常。
 * 用于在编译期的代码生成过程中，如果遇到不可恢复的解析或生成错误，中断处理流程并向外抛出。
 * @author yangziran
 */
public class DynamicSqlPlusException extends RuntimeException {

    public DynamicSqlPlusException(String message) {
        super(message);
    }

    public DynamicSqlPlusException(String message, Throwable cause) {
        super(message, cause);
    }
}
