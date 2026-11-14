package dev.cameraboost.folderchunker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ChunkRulesTest {
    @Test
    public void formatsPartNamesDeterministically() {
        assertEquals("part-000000.bin", ChunkRules.partName(0));
        assertEquals("part-000123.bin", ChunkRules.partName(123));
    }

    @Test
    public void computesExpectedChunkCount() {
        long chunk = 100;
        assertEquals(1, ChunkRules.expectedPartCount(1, chunk));
        assertEquals(1, ChunkRules.expectedPartCount(100, chunk));
        assertEquals(2, ChunkRules.expectedPartCount(101, chunk));
    }

    @Test
    public void ignoresGeneratedFiles() {
        assertTrue(ChunkRules.ignoredFileName("part-000000.bin"));
        assertTrue(ChunkRules.ignoredFileName("manifest.json"));
        assertTrue(ChunkRules.ignoredFileName("parts.sha256"));
        assertTrue(ChunkRules.ignoredFileName("UPLOAD_ORDER.txt"));
        assertFalse(ChunkRules.ignoredFileName("FindX8-camera-port.zip"));
    }
}
