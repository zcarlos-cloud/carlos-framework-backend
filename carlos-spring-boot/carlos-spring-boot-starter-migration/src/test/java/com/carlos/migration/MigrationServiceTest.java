package com.carlos.migration;

import com.carlos.migration.service.MigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 迁移服务测试
 *
 * @author carlos
 * @since 3.0.0
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "carlos.migration.enabled=true",
    "carlos.migration.primary.change-log=db/changelog/db.changelog-master.yaml"
})
public class MigrationServiceTest {

    @Autowired
    private MigrationService migrationService;

    @Test
    void contextLoads() {
        assertNotNull(migrationService);
    }

    @Test
    void testGetAllStatus() {
        var status = migrationService.getAllStatus();
        assertNotNull(status);
    }
}
