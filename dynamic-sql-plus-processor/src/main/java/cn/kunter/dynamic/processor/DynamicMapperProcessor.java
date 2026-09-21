package cn.kunter.dynamic.processor;

import cn.kunter.dynamic.annotations.DynamicMapper;
import cn.kunter.dynamic.processor.generator.MapperClassGenerator;
import cn.kunter.dynamic.processor.generator.SupportClassGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

/**
 * 核心的编译期注解处理器。
 * 负责扫描被 @DynamicMapper 标记的类，并在编译期动态生成对应的 Support 和 Mapper 类。
 * @author yangziran
 */
public class DynamicMapperProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "MyBatis Dynamic SQL Plus: 正在初始化编译期处理器...");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(DynamicMapper.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(DynamicMapper.class);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "MyBatis Dynamic SQL Plus: @DynamicMapper 注解只能应用于类上。",
                        element);
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            try {
                // 1. 解析实体，生成 DynamicSqlSupport 类
                SupportClassGenerator supportGenerator = new SupportClassGenerator(typeElement, filer);
                supportGenerator.generate();

                // 2. 解析实体，生成 Mapper 接口
                MapperClassGenerator mapperGenerator = new MapperClassGenerator(typeElement, filer);
                mapperGenerator.generate();

                messager.printMessage(Diagnostic.Kind.NOTE,
                        "MyBatis Dynamic SQL Plus: 成功为 [" + typeElement.getSimpleName() + "] 生成动态 SQL 基础类！");
            } catch (cn.kunter.dynamic.processor.exception.DynamicSqlPlusException e) {
                // 捕获预期的业务异常
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "MyBatis Dynamic SQL Plus: 为 [" + typeElement.getSimpleName() + "] 生成文件失败，原因: " + e.getMessage(), typeElement);
            } catch (Exception e) {
                // 捕获未知的系统异常
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "MyBatis Dynamic SQL Plus: 发生未知系统异常，为 [" + typeElement.getSimpleName() + "] 生成文件失败: " + sw.toString(), typeElement);
            }
        }

        return true;
    }
}
