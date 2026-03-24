package os.filesystem;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VirtualFileSystem — path resolution, CRUD, navigation,
 * edge-cases (root removal, non-existent paths, non-recursive rmdir).
 */
class VirtualFileSystemTest {

    private VirtualFileSystem fs;

    @BeforeEach
    void setUp() { fs = new VirtualFileSystem(); }

    // ── pwd ──────────────────────────────────────────────────────────────────

    @Test
    void pwd_startsAtRoot() { assertEquals("/", fs.pwd()); }

    // ── mkdir + cd ───────────────────────────────────────────────────────────

    @Test
    void mkdir_createsDirectory() {
        assertTrue(fs.mkdir("/docs"));
        assertTrue(fs.resolve("/docs").isPresent());
        assertTrue(fs.resolve("/docs").get().isDirectory());
    }

    @Test
    void mkdir_duplicateFails() {
        fs.mkdir("/a");
        assertFalse(fs.mkdir("/a"));
    }

    @Test
    void cd_changesWorkingDirectory() {
        fs.mkdir("/home/testuser"); // /home already exists from constructor
        assertTrue(fs.cd("/home"));
        assertEquals("/home", fs.pwd());
    }

    @Test
    void cd_nonExistentFails() {
        assertFalse(fs.cd("/nonexistent"));
    }

    @Test
    void cd_dotDot_movesUp() {
        fs.cd("/home");
        fs.cd("..");
        assertEquals("/", fs.pwd());
    }

    @Test
    void cd_dotDot_atRootStaysAtRoot() {
        fs.cd("..");
        assertEquals("/", fs.pwd());
    }

    @Test
    void mkdirp_createsNestedPaths() {
        assertTrue(fs.mkdirp("/a/b/c/d"));
        assertTrue(fs.resolve("/a/b/c/d").isPresent());
    }

    // ── touch + write + cat ──────────────────────────────────────────────────

    @Test
    void touch_createsEmptyFile() {
        assertTrue(fs.touch("/etc/config.txt"));
        Optional<FileNode> node = fs.resolve("/etc/config.txt");
        assertTrue(node.isPresent());
        assertTrue(node.get().isFile());
        assertEquals(0, node.get().getSize());
    }

    @Test
    void touch_existingFile_isIdempotent() {
        fs.touch("/tmp/f.txt");
        assertTrue(fs.touch("/tmp/f.txt")); // should succeed without error
    }

    @Test
    void write_storesContent() {
        fs.touch("/tmp/data.txt");
        assertTrue(fs.write("/tmp/data.txt", "hello world"));
        Optional<String> content = fs.cat("/tmp/data.txt");
        assertTrue(content.isPresent());
        assertEquals("hello world", content.get());
    }

    @Test
    void write_createsFileIfMissing() {
        assertTrue(fs.write("/tmp/new.txt", "auto-created"));
        assertEquals("auto-created", fs.cat("/tmp/new.txt").orElse(""));
    }

    @Test
    void append_addsToExistingContent() {
        fs.write("/tmp/log.txt", "line1");
        assertTrue(fs.append("/tmp/log.txt", "\nline2"));
        assertEquals("line1\nline2", fs.cat("/tmp/log.txt").get());
    }

    @Test
    void cat_nonExistentFile_returnsEmpty() {
        assertTrue(fs.cat("/tmp/ghost.txt").isEmpty());
    }

    @Test
    void cat_directory_returnsEmpty() {
        assertTrue(fs.cat("/home").isEmpty());
    }

    // ── rm ───────────────────────────────────────────────────────────────────

    @Test
    void rm_file_succeeds() {
        fs.touch("/tmp/del.txt");
        assertTrue(fs.rm("/tmp/del.txt", false));
        assertTrue(fs.resolve("/tmp/del.txt").isEmpty());
    }

    @Test
    void rm_emptyDirectory_nonRecursive_succeeds() {
        fs.mkdir("/tmp/emptydir");
        assertTrue(fs.rm("/tmp/emptydir", false));
    }

    @Test
    void rm_nonEmptyDirectory_nonRecursive_fails() {
        fs.mkdir("/tmp/nonempty");
        fs.touch("/tmp/nonempty/child.txt");
        assertFalse(fs.rm("/tmp/nonempty", false));
        // With recursive flag it should succeed
        assertTrue(fs.rm("/tmp/nonempty", true));
    }

    @Test
    void rm_root_fails() {
        assertFalse(fs.rm("/", false));
        assertFalse(fs.rm("/", true));
    }

    @Test
    void rm_nonExistent_fails() {
        assertFalse(fs.rm("/tmp/ghost", false));
    }

    // ── mv ───────────────────────────────────────────────────────────────────

    @Test
    void mv_renamesFile() {
        fs.write("/tmp/old.txt", "data");
        assertTrue(fs.mv("/tmp/old.txt", "/tmp/new.txt"));
        assertTrue(fs.resolve("/tmp/new.txt").isPresent());
        assertTrue(fs.resolve("/tmp/old.txt").isEmpty());
        assertEquals("data", fs.cat("/tmp/new.txt").get());
    }

    @Test
    void mv_movesToDirectory() {
        fs.write("/tmp/file.txt", "content");
        fs.mkdir("/home/dest");
        assertTrue(fs.mv("/tmp/file.txt", "/home/dest"));
        assertTrue(fs.resolve("/home/dest/file.txt").isPresent());
    }

    @Test
    void mv_nonExistentSource_fails() {
        assertFalse(fs.mv("/tmp/ghost.txt", "/tmp/new.txt"));
    }

    // ── ls ───────────────────────────────────────────────────────────────────

    @Test
    void ls_returnsChildrenOfDirectory() {
        fs.touch("/tmp/a.txt");
        fs.touch("/tmp/b.txt");
        fs.mkdir("/tmp/subdir");
        Optional<List<FileNode>> listing = fs.ls("/tmp");
        assertTrue(listing.isPresent());
        assertEquals(3, listing.get().size());
    }

    @Test
    void ls_nonExistentPath_returnsEmpty() {
        assertTrue(fs.ls("/ghost").isEmpty());
    }

    // ── find ─────────────────────────────────────────────────────────────────

    @Test
    void find_locatesFileByPartialName() {
        fs.write("/home/report2024.txt", "x");
        fs.write("/tmp/report2023.txt", "y");
        List<String> results = fs.find("report");
        assertEquals(2, results.size());
    }

    @Test
    void find_noMatch_returnsEmpty() {
        assertTrue(fs.find("nonexistent_xyz").isEmpty());
    }

    // ── Path resolution edge cases ────────────────────────────────────────────

    @Test
    void resolve_absolutePath_fromAnyWorkingDir() {
        fs.cd("/home");
        assertTrue(fs.resolve("/etc").isPresent());
    }

    @Test
    void resolve_relativePath_fromCwd() {
        fs.cd("/home");
        fs.mkdir("/home/projects");
        assertTrue(fs.resolve("projects").isPresent());
    }

    @Test
    void resolve_dotPath_returnsCwd() {
        fs.cd("/tmp");
        assertEquals(fs.getCwd(), fs.resolve(".").get());
    }
}
