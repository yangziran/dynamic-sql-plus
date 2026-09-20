package cn.kunter.dynamic.processor.utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class ElementUtils {

    /**
     * 获取类中声明的所有字段，包括从父类（递归）继承的字段。
     */
    public static List<Element> getAllFields(TypeElement typeElement) {
        List<Element> fields = new ArrayList<>();
        TypeElement currentElement = typeElement;

        while (currentElement != null && !currentElement.getQualifiedName().toString().equals("java.lang.Object")) {
            for (Element e : currentElement.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD) {
                    // Check if field with same name already exists (shadowing)
                    String name = e.getSimpleName().toString();
                    boolean exists = fields.stream().anyMatch(f -> f.getSimpleName().toString().equals(name));
                    if (!exists) {
                        fields.add(e);
                    }
                }
            }

            TypeMirror superclass = currentElement.getSuperclass();
            if (superclass.getKind() == TypeKind.DECLARED) {
                currentElement = (TypeElement) ((DeclaredType) superclass).asElement();
            } else {
                currentElement = null;
            }
        }

        return fields;
    }
}
