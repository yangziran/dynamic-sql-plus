package cn.kunter.dynamic.processor.generator;

import cn.kunter.dynamic.processor.exception.DynamicSqlPlusException;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper 接口生成器。
 * 负责生成带有 @Mapper 注解的 MyBatis 接口，并自动继承通用的 CRUD Mapper。
 * @author yangziran
 */
public class MapperClassGenerator {

    private final TypeElement typeElement;
    private final Filer filer;

    public MapperClassGenerator(TypeElement typeElement, Filer filer) {
        this.typeElement = typeElement;
        this.filer = filer;
    }

    /**
     * 执行生成逻辑。
     * @throws IOException 当文件写入失败时抛出
     */
    public void generate() throws IOException {
        String packageName = ((TypeElement) typeElement).getQualifiedName().toString();
        int lastDotIndex = packageName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new DynamicSqlPlusException("实体类必须定义在包中，不允许使用默认包: " + packageName);
        }
        packageName = packageName.substring(0, lastDotIndex);

        String entityClassName = typeElement.getSimpleName().toString();

        String mapperPrefix = cn.kunter.dynamic.processor.utils.StringUtils.getEntityPrefix(entityClassName);
        String mapperClassName = mapperPrefix + "Mapper";

        ClassName mapperAnnotation = ClassName.get("org.apache.ibatis.annotations", "Mapper");
        ClassName entityClass = ClassName.get(packageName, entityClassName);

        // 核心：让生成的 Mapper 继承 MyBatis Dynamic SQL 提供的四大通用接口
        ClassName commonSelect = ClassName.get("org.mybatis.dynamic.sql.util.mybatis3", "CommonSelectMapper");
        ClassName commonUpdate = ClassName.get("org.mybatis.dynamic.sql.util.mybatis3", "CommonUpdateMapper");
        ClassName commonDelete = ClassName.get("org.mybatis.dynamic.sql.util.mybatis3", "CommonDeleteMapper");
        ClassName commonInsert = ClassName.get("org.mybatis.dynamic.sql.util.mybatis3", "CommonInsertMapper");

        // Parse TableId and fields
        Element pkElement = null;
        boolean autoIncrement = false;
        String pkColumnName = null;
        List<Element> allFields = new ArrayList<>();

        for (Element e : cn.kunter.dynamic.processor.utils.ElementUtils.getAllFields(typeElement)) {
            if (!e.getModifiers().contains(Modifier.STATIC)) {
                cn.kunter.dynamic.annotations.TableColumn tableColumn =
                        e.getAnnotation(cn.kunter.dynamic.annotations.TableColumn.class);
                if (tableColumn != null && tableColumn.ignore()) {
                    continue;
                }
                allFields.add(e);
                cn.kunter.dynamic.annotations.TableId tableId =
                        e.getAnnotation(cn.kunter.dynamic.annotations.TableId.class);
                if (tableId != null) {
                    pkElement = e;
                    autoIncrement = tableId.autoIncrement();
                    pkColumnName = tableId.value()
                            .isEmpty() ? cn.kunter.dynamic.processor.utils.StringUtils.camelToSnake(e.getSimpleName()
                            .toString()) : tableId.value();
                }
            }
        }

        TypeSpec.Builder mapperInterfaceBuilder = TypeSpec.interfaceBuilder(mapperClassName)
                .addModifiers(Modifier.PUBLIC).addAnnotation(AnnotationSpec.builder(mapperAnnotation).build())
                .addSuperinterface(commonSelect).addSuperinterface(commonUpdate).addSuperinterface(commonDelete)
                .addSuperinterface(ParameterizedTypeName.get(commonInsert, entityClass))
                .addJavadoc("自动生成的 MyBatis Mapper 接口。\n@see $T\n", entityClass);

        String tableFieldName = cn.kunter.dynamic.processor.utils.StringUtils.lowerFirst(mapperPrefix);
        ClassName supportClass = ClassName.get(packageName, entityClassName + "DynamicSqlSupport");
        ClassName sqlBuilderClass = ClassName.get("org.mybatis.dynamic.sql", "SqlBuilder");
        ClassName renderingStrategies = ClassName.get("org.mybatis.dynamic.sql.render", "RenderingStrategies");

        // Build @Results for selectMany
        ClassName resultsAnnotation = ClassName.get("org.apache.ibatis.annotations", "Results");
        ClassName resultAnnotation = ClassName.get("org.apache.ibatis.annotations", "Result");
        AnnotationSpec.Builder resultsBuilder = AnnotationSpec.builder(resultsAnnotation)
                .addMember("id", "$S", entityClassName + "Result");
        for (Element field : allFields) {
            String property = field.getSimpleName().toString();
            String column = cn.kunter.dynamic.processor.utils.StringUtils.camelToSnake(property);
            cn.kunter.dynamic.annotations.TableColumn tableColumn =
                    field.getAnnotation(cn.kunter.dynamic.annotations.TableColumn.class);
            if (tableColumn != null && !tableColumn.value().isEmpty()) {
                column = tableColumn.value();
            }
            boolean isPk = false;
            cn.kunter.dynamic.annotations.TableId tableId =
                    field.getAnnotation(cn.kunter.dynamic.annotations.TableId.class);
            if (tableId != null) {
                isPk = true;
                if (!tableId.value().isEmpty()) {
                    column = tableId.value();
                }
            }
            AnnotationSpec.Builder resultSpecBuilder = AnnotationSpec.builder(resultAnnotation)
                    .addMember("column", "$S", column).addMember("property", "$S", property);
            if (isPk) {
                resultSpecBuilder.addMember("id", "true");
            }
            resultsBuilder.addMember("value", "$L", resultSpecBuilder.build());
        }

        ClassName selectProvider = ClassName.get("org.apache.ibatis.annotations", "SelectProvider");
        ClassName sqlProviderAdapter = ClassName.get("org.mybatis.dynamic.sql.util", "SqlProviderAdapter");
        ClassName selectStatementProvider = ClassName.get("org.mybatis.dynamic.sql.select.render",
                "SelectStatementProvider");
        ClassName resultMapAnnotation = ClassName.get("org.apache.ibatis.annotations", "ResultMap");

        com.squareup.javapoet.MethodSpec typedSelectMany = com.squareup.javapoet.MethodSpec.methodBuilder("selectMany")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get("java.util", "List"), entityClass))
                .addAnnotation(AnnotationSpec.builder(selectProvider).addMember("type", "$T.class", sqlProviderAdapter)
                        .addMember("method", "$S", "select").build()).addAnnotation(resultsBuilder.build())
                .addParameter(selectStatementProvider, "selectStatement").build();
        mapperInterfaceBuilder.addMethod(typedSelectMany);

        com.squareup.javapoet.MethodSpec typedSelectOne = com.squareup.javapoet.MethodSpec.methodBuilder("selectOne")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityClass))
                .addAnnotation(AnnotationSpec.builder(selectProvider).addMember("type", "$T.class", sqlProviderAdapter)
                        .addMember("method", "$S", "select").build())
                .addAnnotation(AnnotationSpec.builder(resultMapAnnotation)
                        .addMember("value", "$S", entityClassName + "Result").build())
                .addParameter(selectStatementProvider, "selectStatement").build();
        mapperInterfaceBuilder.addMethod(typedSelectOne);

        if (pkElement != null) {
            String pkName = pkElement.getSimpleName().toString();
            com.squareup.javapoet.TypeName pkType = com.squareup.javapoet.TypeName.get(pkElement.asType());
            if (pkType.isPrimitive()) {
                pkType = pkType.box();
            }

            // selectByPrimaryKey
            com.squareup.javapoet.MethodSpec selectByPk = com.squareup.javapoet.MethodSpec.methodBuilder(
                    "selectByPrimaryKey")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityClass))
                    .addParameter(pkType, pkName + "_")
                    .addStatement("return selectOne($T.select($T.$L.allColumns()).from($T.$L).where($T.$L, $T" +
                            ".isEqualTo($L_)).build().render($T.MYBATIS3))", sqlBuilderClass, supportClass,
                            tableFieldName, supportClass, tableFieldName, supportClass, pkName, sqlBuilderClass,
                            pkName, renderingStrategies)
                    .build();
            mapperInterfaceBuilder.addMethod(selectByPk);

            // deleteByPrimaryKey
            com.squareup.javapoet.MethodSpec deleteByPk = com.squareup.javapoet.MethodSpec.methodBuilder(
                    "deleteByPrimaryKey")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).returns(int.class)
                    .addParameter(pkType, pkName + "_")
                    .addStatement("return delete($T.deleteFrom($T.$L).where($T.$L, $T.isEqualTo($L_)).build().render" +
                            "($T.MYBATIS3))", sqlBuilderClass, supportClass, tableFieldName, supportClass, pkName,
                            sqlBuilderClass, pkName, renderingStrategies)
                    .build();
            mapperInterfaceBuilder.addMethod(deleteByPk);

            // updateByPrimaryKey
            if (allFields.size() > 1) {
                com.squareup.javapoet.MethodSpec.Builder updateByPkBuilder =
                        com.squareup.javapoet.MethodSpec.methodBuilder("updateByPrimaryKey")
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).returns(int.class)
                        .addParameter(entityClass, "row");

                updateByPkBuilder.addCode("return update($T.update($T.$L)\n", sqlBuilderClass, supportClass,
                        tableFieldName);
                for (Element field : allFields) {
                    String fieldName = field.getSimpleName().toString();
                    if (fieldName.equals(pkName)) {
                        continue; // Skip updating primary key
                    }
                    String getter;
                    if (field.asType().toString().equals("boolean")) {
                        if (fieldName.startsWith("is") && fieldName.length() > 2 && Character.isUpperCase(fieldName.charAt(2))) {
                            getter = fieldName;
                        } else {
                            getter = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        }
                    } else {
                        getter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    }
                    updateByPkBuilder.addCode("    .set($T.$L).equalTo(row::$L)\n", supportClass, fieldName, getter);
                }
                String pkGetter;
                if (pkElement.asType().toString().equals("boolean")) {
                    if (pkName.startsWith("is") && pkName.length() > 2 && Character.isUpperCase(pkName.charAt(2))) {
                        pkGetter = pkName;
                    } else {
                        pkGetter = "is" + pkName.substring(0, 1).toUpperCase() + pkName.substring(1);
                    }
                } else {
                    pkGetter = "get" + pkName.substring(0, 1).toUpperCase() + pkName.substring(1);
                }
                updateByPkBuilder.addCode("    .where($T.$L, $T.isEqualTo(row.$L()))\n", supportClass, pkName,
                        sqlBuilderClass, pkGetter);
                updateByPkBuilder.addCode("    .build().render($T.MYBATIS3));\n", renderingStrategies);
                mapperInterfaceBuilder.addMethod(updateByPkBuilder.build());
            }

            // autoIncrement
            if (autoIncrement) {
                ClassName insertProvider = ClassName.get("org.apache.ibatis.annotations", "InsertProvider");
                ClassName options = ClassName.get("org.apache.ibatis.annotations", "Options");
                ClassName insertStatementProvider = ClassName.get("org.mybatis.dynamic.sql.insert.render",
                        "InsertStatementProvider");
                ClassName multiRowInsertStatementProvider = ClassName.get("org.mybatis.dynamic.sql.insert.render",
                        "MultiRowInsertStatementProvider");

                com.squareup.javapoet.MethodSpec insert = com.squareup.javapoet.MethodSpec.methodBuilder("insert")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(int.class)
                        .addAnnotation(AnnotationSpec.builder(insertProvider)
                                .addMember("type", "$T.class", sqlProviderAdapter).addMember("method", "$S", "insert")
                                .build())
                        .addAnnotation(AnnotationSpec.builder(options).addMember("useGeneratedKeys", "true")
                                .addMember("keyProperty", "$S", "row." + pkName).build())
                        .addParameter(ParameterizedTypeName.get(insertStatementProvider, entityClass),
                                "insertStatement")
                        .build();
                mapperInterfaceBuilder.addMethod(insert);

                com.squareup.javapoet.MethodSpec insertMultiple = com.squareup.javapoet.MethodSpec.methodBuilder(
                        "insertMultiple")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(int.class)
                        .addAnnotation(AnnotationSpec.builder(insertProvider)
                                .addMember("type", "$T.class", sqlProviderAdapter)
                                .addMember("method", "$S", "insertMultiple").build())
                        .addAnnotation(AnnotationSpec.builder(options).addMember("useGeneratedKeys", "true")
                                .addMember("keyProperty", "$S", "records." + pkName).build())
                        .addParameter(ParameterizedTypeName.get(multiRowInsertStatementProvider, entityClass),
                                "multipleInsertStatement")
                        .build();
                mapperInterfaceBuilder.addMethod(insertMultiple);
            }
        }

        JavaFile javaFile = JavaFile.builder(packageName, mapperInterfaceBuilder.build()).indent("    ").build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            throw new DynamicSqlPlusException("无法写入生成的 Mapper 文件: " + mapperClassName, e);
        }
    }
}
