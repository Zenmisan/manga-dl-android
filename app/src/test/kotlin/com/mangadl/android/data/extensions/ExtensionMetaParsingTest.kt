package com.mangadl.android.data.extensions

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for extension meta-comment parsing (the // ==Extension== block
 * at the top of each .js file that declares name, lang, version, etc.).
 * Mirrors ExtensionManager.parseMetaComment.
 */
class ExtensionMetaParsingTest {

    private fun parseMetaComment(id: String, script: String): Map<String, String> {
        val meta = mutableMapOf<String, String>("id" to id)
        val metaBlock = Regex("""// ==Extension==\n(.*?)// ==/Extension==""", RegexOption.DOT_MATCHES_ALL)
            .find(script)?.groupValues?.get(1) ?: return meta
        Regex("""// @(\w+)\s+(.+)""").findAll(metaBlock).forEach { m ->
            meta[m.groupValues[1]] = m.groupValues[2].trim()
        }
        return meta
    }

    private val sampleScript = """
// ==Extension==
// @name        Manga Katana
// @lang        en
// @version     1.2.3
// @nsfw        false
// @icon        https://mangakatana.com/favicon.ico
// ==/Extension==

var extension = { search: async function() {} };
""".trimIndent()

    @Test
    fun `id is always set from parameter`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("mangakatana", meta["id"])
    }

    @Test
    fun `name is parsed correctly`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("Manga Katana", meta["name"])
    }

    @Test
    fun `lang is parsed correctly`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("en", meta["lang"])
    }

    @Test
    fun `version is parsed correctly`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("1.2.3", meta["version"])
    }

    @Test
    fun `nsfw flag is parsed correctly`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("false", meta["nsfw"])
    }

    @Test
    fun `icon url is parsed correctly`() {
        val meta = parseMetaComment("mangakatana", sampleScript)
        assertEquals("https://mangakatana.com/favicon.ico", meta["icon"])
    }

    @Test
    fun `script with no meta comment returns only id`() {
        val script = "var extension = {};"
        val meta = parseMetaComment("noop", script)
        assertEquals(1, meta.size)
        assertEquals("noop", meta["id"])
    }

    @Test
    fun `ExtensionMeta name falls back to id when name not in meta`() {
        val script = "// ==Extension==\n// @lang en\n// ==/Extension==\nvar extension = {};"
        val meta = parseMetaComment("fallback-src", script)
        val extMeta = ExtensionMeta("fallback-src", "fallback-src.js", script, meta)
        assertEquals("fallback-src", extMeta.name)
    }

    @Test
    fun `ExtensionMeta nsfw is false when meta says false`() {
        val meta = parseMetaComment("x", sampleScript)
        val extMeta = ExtensionMeta("x", "x.js", sampleScript, meta)
        assertFalse(extMeta.nsfw)
    }

    @Test
    fun `ExtensionMeta nsfw is true when meta says true`() {
        val script = "// ==Extension==\n// @nsfw true\n// ==/Extension==\nvar extension = {};"
        val meta = parseMetaComment("nsfw-src", script)
        val extMeta = ExtensionMeta("nsfw-src", "nsfw-src.js", script, meta)
        assertTrue(extMeta.nsfw)
    }

    @Test
    fun `ExtensionMeta version falls back to 1-0-0 when missing`() {
        val script = "// ==Extension==\n// @name Test\n// ==/Extension==\nvar extension = {};"
        val meta = parseMetaComment("x", script)
        val extMeta = ExtensionMeta("x", "x.js", script, meta)
        assertEquals("1.0.0", extMeta.version)
    }
}
