package hyc.codegen.core.annotation;

import hyc.codegen.core.model.Meta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注解体系：严格语法提取、内置处理器、未知注解容忍、目标位置校验。
 */
class AnnotationProcessorTest {

    private final AnnotationProcessor processor = new AnnotationProcessor(AnnotationRegistry.builtin());

    @Test
    void annotationOnWrongTargetIsIgnored() {
        Meta meta = new Meta();
        // @type 仅允许列；用在表注释时忽略
        processor.process("用户表 @type:UserExt", MetaTarget.TABLE, meta);
        assertFalse(meta.contains("type"));

        // @ignore 不允许表；用在表注释时忽略
        processor.process("用户表 @ignore", MetaTarget.TABLE, meta);
        assertFalse(meta.contains("ignore"));
    }

    @Test
    void asAnnotationOnTableAndColumn() {
        Meta tableMeta = new Meta();
        processor.process("账户表 @as:Account", MetaTarget.TABLE, tableMeta);
        assertEquals("Account", tableMeta.getString("as"));

        Meta columnMeta = new Meta();
        processor.process("性别 @as:UserGender", MetaTarget.COLUMN, columnMeta);
        assertEquals("UserGender", columnMeta.getString("as"));
    }

    @Test
    void ignoreAnnotationOnColumnAndIndex() {
        Meta columnMeta = new Meta();
        processor.process("敏感字段 @ignore", MetaTarget.COLUMN, columnMeta);
        assertTrue(columnMeta.isTrue("ignore"));

        Meta indexMeta = new Meta();
        processor.process("@ignore", MetaTarget.INDEX, indexMeta);
        assertTrue(indexMeta.isTrue("ignore"));
    }

    @Test
    void multipleAnnotationsInOneComment() {
        Meta meta = new Meta();
        processor.process("扩展信息 @type:UserExtInfo @ignore", MetaTarget.COLUMN, meta);

        assertEquals("UserExtInfo", meta.getString("type"));
        assertTrue(meta.isTrue("ignore"));
    }

    @Test
    void noAnnotationInPlainComment() {
        Meta meta = new Meta();
        processor.process("普通注释没有注解", MetaTarget.COLUMN, meta);
        assertEquals(0, meta.asMap().size());
    }

    @Test
    void typeAnnotationOnColumn() {
        Meta meta = new Meta();
        processor.process("扩展信息 @type:UserExtInfo", MetaTarget.COLUMN, meta);

        assertEquals("UserExtInfo", meta.getString("type"));
    }

    @Test
    void unknownAnnotationIsTolerated() {
        Meta meta = new Meta();
        // @boolean 是未注册注解名（无隐式简写），应被忽略且不中断
        processor.process("状态 @boolean", MetaTarget.COLUMN, meta);

        assertNull(meta.getString("type"));
        assertFalse(meta.contains("boolean"));
    }

}
