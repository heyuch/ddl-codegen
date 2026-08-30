package hyc.codegen.mavenplugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DdlFileRangeTest {

    @Test
    void parseValidRange() {
        DdlFileRange range = DdlFileRange.parse("create-user.sql:66-120");
        assertNotNull(range);
        assertEquals("create-user.sql", range.getPath());
        assertEquals(66, range.getStartLine());
        assertEquals(120, range.getEndLine());
    }

    @Test
    void noRangeMeansWholeFile() {
        assertNull(DdlFileRange.parse("create-user.sql"));
        assertNull(DdlFileRange.parse("dir/create-user.sql"));
    }

    @Test
    void singleLineWithoutDashIsNotARange() {
        // "file:66" 不是 range 格式 → 整文件（找不到文件时错误信息含完整路径）
        assertNull(DdlFileRange.parse("create-user.sql:66"));
    }

    @Test
    void startGreaterThanEndRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DdlFileRange.parse("create-user.sql:120-66"));
        assertEquals("行范围起始大于结束: create-user.sql:120-66", e.getMessage());
    }

    @Test
    void startBelowOneRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DdlFileRange.parse("create-user.sql:0-10"));
        assertEquals("行范围起始必须 ≥1: create-user.sql:0-10", e.getMessage());
    }

}
