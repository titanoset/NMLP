package com.nmlp.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationRunnerStripTest {

    @Test
    void stripPreservesStringWithDoubleDash() {
        String sql = "SELECT '--not-a-comment' FROM t;\n-- real comment; fake stmt\nINSERT INTO t VALUES (1);";
        String stripped = MigrationRunner.stripLineCommentsOutsideQuotes(sql);
        assertTrue(stripped.contains("'--not-a-comment'"));
        assertFalse(stripped.contains("real comment"));
        assertTrue(stripped.contains("INSERT INTO t VALUES (1)"));
    }
}
