package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import jakarta.inject.Inject;

import org.h2.jdbc.JdbcSQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSetStatus;

public class LiquibaseExtensionCleanAtStartTest {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("clean-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Clean at start correctly")
    public void testLiquibaseConfigInjection() throws Exception {
        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat
                    .executeQuery("select * from fake_existing_tbl")) {
                fail("fake_existing_tbl should not exist");
            } catch (JdbcSQLException e) {
                // expected fake_existing_tbl does not exist
            }
        }

        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status, "Status is null");
            assertEquals(1, status.size(), "The set of changes is not null");
            assertTrue(status.get(0).getWillRun());

            List<ChangeSet> unrun = liquibase.listUnrunChangeSets(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(unrun, "Unrun is null");
            assertEquals(1, status.size(), "The set of unrun changes is not null");
        }
    }
}
