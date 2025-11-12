package io.github.rahulsom.yamlego

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventUtilsTest {
    @Test
    fun `doc with key value`() {
      /* language=yaml  */
        val input = """
            ---
            key: value
            """.trimIndent()

        val events = EventUtils().deserialize(input)
        assertThat(events.map { it.toString() }).containsExactly(
            "+STR",
            "+DOC ---",
            "+MAP",
            "=VAL :key",
            "=VAL :value",
            "-MAP",
            "-DOC",
            "-STR"
        )
    }

    @Test
    fun `implicit doc with key value`() {
      /* language=yaml  */
        val input = """
            key: value
            """.trimIndent()

        val events = EventUtils().deserialize(input)
        assertThat(events.map { it.toString() }).containsExactly(
            "+STR",
            "+DOC",
            "+MAP",
            "=VAL :key",
            "=VAL :value",
            "-MAP",
            "-DOC",
            "-STR"
        )
    }

    @Test
    fun `implicit doc with list`() {
      /* language=yaml  */
        val input = """
            list:
            - one
            - two
            """.trimIndent()

        val events = EventUtils().deserialize(input)
        assertThat(events.map { it.toString() }).containsExactly(
            "+STR",
            "+DOC",
            "+MAP",
            "=VAL :list",
            "+SEQ",
            "=VAL :one",
            "=VAL :two",
            "-SEQ",
            "-MAP",
            "-DOC",
            "-STR"
        )
    }

    @Test
    fun `implicit doc with comment`() {
      /* language=yaml  */
        val input = """
            # some comment here
            """.trimIndent()

        val events = EventUtils().deserialize(input)
        assertThat(events.map { it.toString() }).containsExactly(
            "+STR",
            "=COM BLOCK  some comment here",
            "-STR"
        )
    }

    @Test
    fun `implicit doc anchor and reference`() {
      /* language=yaml  */
        val input = """
            defs:
            - &anc {foo: bar}
            use:
            - a: *anc
            """.trimIndent()

        val events = EventUtils().deserialize(input)
        assertThat(events.map { it.toString() }).containsExactly(
            "+STR",
            "+DOC",
            "+MAP",
            "=VAL :defs",
            "+SEQ",
            "+MAP {} &anc",
            "=VAL :foo",
            "=VAL :bar",
            "-MAP",
            "-SEQ",
            "=VAL :use",
            "+SEQ",
            "+MAP",
            "=VAL :a",
            "=ALI *anc",
            "-MAP",
            "-SEQ",
            "-MAP",
            "-DOC",
            "-STR"
        )
    }
}
