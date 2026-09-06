package io.github.heyuch.codegen.core.naming;

import java.util.Arrays;

import io.github.heyuch.codegen.core.config.ArtifactConfig;
import io.github.heyuch.codegen.core.config.DdlConfig;
import io.github.heyuch.codegen.core.model.Index;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 命名变换链：前缀剥离、分表后缀、camelCase、关键字处理、索引方法名、enum 命名、策略逃生口。
 */
class NamingServiceTest {

    @Test
    void artifactClassNameAppendsSuffix() {
        DdlConfig config = config();
        ArtifactConfig mapper = new ArtifactConfig("mapper");
        config.addArtifact(mapper);
        mapper.setSuffix("Mapper");

        NamingService naming = new NamingService(config);
        assertEquals("UserMapper", naming.artifactClassName("t_user", "mapper"));
        assertEquals("User", naming.artifactClassName("t_user", "entity"));
    }

    @Test
    void cleanIdentifiersFlowThroughJavaNaming() {
        NamingService naming = new NamingService(config());
        // 输入契约 = parse 后干净名（反引号在 DDL parse 层剥除，20260906-11）：MySQL 保留字原名直出，
        // 仅 Java 关键字加后缀——Java 命名不关心 MySQL 保留字
        assertEquals("order", naming.columnFieldName("order"));
        assertEquals("userId", naming.columnFieldName("user_id"));
        assertEquals("Order", naming.enumClassName("t_user", "order"));
        assertEquals("findByOrder", naming.indexMethodName(index("idx_order", "order")));
    }

    @Test
    void columnFieldNameCamelCaseAndKeyword() {
        NamingService naming = new NamingService(config());
        assertEquals("userId", naming.columnFieldName("user_id"));
        assertEquals("name", naming.columnFieldName("name"));
        assertEquals("order", naming.columnFieldName("order"));
        assertEquals("class_", naming.columnFieldName("class"));
        assertEquals("int_", naming.columnFieldName("int"));
    }

    private DdlConfig config() {
        DdlConfig config = new DdlConfig();
        config.addTableStripPrefix("t_");
        config.setTableStripShardSuffix(true);
        return config;
    }

    @Test
    void customStrategyReplacesChain() {
        DdlConfig config = config();
        NamingService naming = new NamingService(config, tableName -> "Sys" + tableName);
        assertEquals("Syst_user", naming.tableClassName("t_user"));
    }

    @Test
    void enumClassNameStyles() {
        DdlConfig config = config();
        NamingService naming = new NamingService(config);
        assertEquals("Gender", naming.enumClassName("t_user", "gender"));

        config.setEnumStyle("tableColumn");
        assertEquals("UserGender", naming.enumClassName("t_user", "gender"));
    }

    private Index index(String name, String... columns) {
        return Index.builder()
                .name(name)
                .unique(true)
                .columns(Arrays.asList(columns))
                .build();
    }

    @Test
    void indexMethodNameJoinedByAnd() {
        NamingService naming = new NamingService(config());
        assertEquals("findByNameAndGender", naming.indexMethodName(index("uk_name_gender", "name", "gender")));
        assertEquals("findByStatusAndType", naming.indexMethodName(index("idx_status_type", "status", "type")));
        assertEquals("findById", naming.indexMethodName(index("PRIMARY", "id")));
        assertEquals("findByUserId", naming.indexMethodName(index("uk_user_id", "user_id")));
    }

    @Test
    void tableClassNameStripPrefixAndShard() {
        NamingService naming = new NamingService(config());
        assertEquals("User", naming.tableClassName("t_user"));
        assertEquals("User", naming.tableClassName("user_0"));
        assertEquals("User", naming.tableClassName("t_user_0"));
        assertEquals("UserProfile", naming.tableClassName("user_profile"));
        assertEquals("User", naming.tableClassName("T_USER"));
    }

}
