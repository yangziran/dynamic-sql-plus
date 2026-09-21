package cn.kunter.dynamic.processor.generator;

import cn.kunter.dynamic.annotations.DynamicMapper;
import cn.kunter.dynamic.annotations.TableColumn;
import cn.kunter.dynamic.processor.exception.DynamicSqlPlusException;
import cn.kunter.dynamic.processor.utils.StringUtils;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态 SQL Support 类生成器。
 * 负责解析实体类中的字段，生成 MyBatis Dynamic SQL 所需的 SqlTable 和 SqlColumn 元数据。
 * @author yangziran
 */
public class SupportClassGenerator {

    private final TypeElement typeElement;
    private final Filer filer;

    public SupportClassGenerator(TypeElement typeElement, Filer filer) {
        this.typeElement = typeElement;
        this.filer = filer;
    }

    /**
     * 执行生成逻辑。
     * @throws DynamicSqlPlusException 如果解析失败或写入文件失败时抛出
     */
    public void generate() {
        String packageName = ((TypeElement) typeElement).getQualifiedName().toString();
        int lastDotIndex = packageName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new DynamicSqlPlusException("实体类必须定义在包中，不允许使用默认包: " + packageName);
        }
        packageName = packageName.substring(0, lastDotIndex);

        String className = typeElement.getSimpleName().toString();
        String supportClassName = className + "DynamicSqlSupport";

        DynamicMapper dynamicMapper = typeElement.getAnnotation(DynamicMapper.class);
        String tableName = dynamicMapper.tableName();
        if (tableName.isEmpty()) {
            tableName = StringUtils.inferTableName(className);
        }

        ClassName sqlTableClass = ClassName.get("org.mybatis.dynamic.sql", "SqlTable");
        ClassName sqlColumnClass = ClassName.get("org.mybatis.dynamic.sql", "SqlColumn");

        // 构建内部的表元数据类 (例如 UserEo 继承 SqlTable)
        String tableClassName = className + "Table";
        String tableFieldName = StringUtils.lowerFirst(StringUtils.getEntityPrefix(className));

        TypeSpec.Builder innerTableBuilder = TypeSpec.classBuilder(tableClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).superclass(sqlTableClass)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addStatement("super($S)", tableName).build());

        TypeSpec.Builder supportClassBuilder = TypeSpec.classBuilder(supportClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("自动生成的 MyBatis Dynamic SQL 支持类。\n@see $T\n", ClassName.get(packageName, className))
                .addField(FieldSpec.builder(ClassName.bestGuess(tableClassName), tableFieldName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $L()", tableClassName).build());

        List<Element> fields = new ArrayList<>();
        for (Element e : cn.kunter.dynamic.processor.utils.ElementUtils.getAllFields(typeElement)) {
            if (!e.getModifiers().contains(Modifier.STATIC)) {
                TableColumn tableColumn = e.getAnnotation(TableColumn.class);
                if (tableColumn != null && tableColumn.ignore()) {
                    continue; // 忽略被 @TableColumn(ignore = true) 标记的字段
                }
                fields.add(e);
            }
        }

        // 解析并生成每一列
        for (Element field : fields) {
            String fieldName = field.getSimpleName().toString();
            String columnName = StringUtils.camelToSnake(fieldName);

            TableColumn tableColumn = field.getAnnotation(TableColumn.class);
            if (tableColumn != null && !tableColumn.value().isEmpty()) {
                columnName = tableColumn.value();
            }
            cn.kunter.dynamic.annotations.TableId tableId =
                    field.getAnnotation(cn.kunter.dynamic.annotations.TableId.class);
            if (tableId != null && !tableId.value().isEmpty()) {
                columnName = tableId.value();
            }

            TypeMirror fieldType = field.asType();
            TypeName typeName;
            if (fieldType.getKind().isPrimitive()) {
                typeName = TypeName.get(fieldType).box();
            } else if (fieldType.getKind() == TypeKind.ARRAY && fieldType.toString().equals("byte[]")) {
                typeName = TypeName.get(byte[].class);
            } else {
                typeName = TypeName.get(fieldType);
            }

            JDBCType jdbcType = inferJdbcType(fieldType);

            ParameterizedTypeName sqlColumnType = ParameterizedTypeName.get(sqlColumnClass, typeName);

            supportClassBuilder.addField(FieldSpec.builder(sqlColumnType, fieldName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L.column($S, $T.$L)", tableFieldName, columnName, JDBCType.class, jdbcType.name())
                    .build());
        }

        supportClassBuilder.addType(innerTableBuilder.build());

        JavaFile javaFile = JavaFile.builder(packageName, supportClassBuilder.build()).indent("    ").build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            throw new DynamicSqlPlusException("无法写入生成的 Support 文件: " + supportClassName, e);
        }
    }

    /**
     * 根据 Java 类型推断相应的 JDBCType
     */
    private JDBCType inferJdbcType(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.INT) {
            return JDBCType.INTEGER;
        } else if (typeMirror.getKind() == TypeKind.LONG) {
            return JDBCType.BIGINT;
        } else if (typeMirror.getKind() == TypeKind.DOUBLE) {
            return JDBCType.DOUBLE;
        } else if (typeMirror.getKind() == TypeKind.FLOAT) {
            return JDBCType.REAL;
        } else if (typeMirror.getKind() == TypeKind.BOOLEAN) {
            return JDBCType.BIT;
        } else if (typeMirror.getKind() == TypeKind.SHORT) {
            return JDBCType.SMALLINT;
        } else if (typeMirror.getKind() == TypeKind.BYTE) {
            return JDBCType.TINYINT;
        } else if (typeMirror.getKind() == TypeKind.ARRAY && typeMirror.toString().equals("byte[]")) {
            return JDBCType.VARBINARY;
        } else if (typeMirror.getKind() == TypeKind.DECLARED) {
            String typeName = typeMirror.toString();
            if (typeName.equals("java.lang.String")) {
                return JDBCType.VARCHAR;
            } else if (typeName.equals("java.lang.Integer")) {
                return JDBCType.INTEGER;
            } else if (typeName.equals("java.lang.Long") || typeName.equals("java.math.BigInteger")) {
                return JDBCType.BIGINT;
            } else if (typeName.equals("java.math.BigDecimal")) {
                return JDBCType.DECIMAL;
            } else if (typeName.equals("java.util.Date") || typeName.equals("java.time.LocalDateTime")) {
                return JDBCType.TIMESTAMP;
            } else if (typeName.equals("java.time.LocalDate")) {
                return JDBCType.DATE;
            } else if (typeName.equals("java.time.LocalTime")) {
                return JDBCType.TIME;
            } else if (typeName.equals("java.lang.Boolean")) {
                return JDBCType.BIT;
            } else if (typeName.equals("java.lang.Double")) {
                return JDBCType.DOUBLE;
            } else if (typeName.equals("java.lang.Float")) {
                return JDBCType.REAL;
            } else if (typeName.equals("java.lang.Short")) {
                return JDBCType.SMALLINT;
            } else if (typeName.equals("java.lang.Byte")) {
                return JDBCType.TINYINT;
            }
        }
        return JDBCType.VARCHAR; // 默认回退到 VARCHAR
    }
}
