package io.github.rahulsom.yamlego

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.snakeyaml.engine.v2.comments.CommentType
import org.snakeyaml.engine.v2.events.AliasEvent
import org.snakeyaml.engine.v2.events.CommentEvent
import org.snakeyaml.engine.v2.events.ScalarEvent

class YamlegoParserTest {
    private fun parseYaml(input: String): YamlegoEvent.YamlStream {
        val events = EventUtils().deserialize(input)
        val stream = YamlegoParser().parse(events)
        println(stream)
        println(EventUtils().serialize(stream.toEvents()))
        println(events)
        assertThat(stream.toEvents()).isEqualTo(events)
        assertThat(stream.isValid()).isTrue()
        return stream
    }

    @Test
    fun `doc with key value`() {
        val stream = parseYaml(
            """
            ---
            key: value
            """.trimIndent()
        )

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Document::class.java)
        val doc = stream.children().first() as YamlegoEvent.Document
        assertThat(doc.children()).hasSize(1)
        assertThat(doc.children().first())
            .isInstanceOf(YamlegoEvent.Mappings::class.java)
        val mappings = doc.children().first() as YamlegoEvent.Mappings
        val simpleMap = mappings.asSimpleMap()
        assertThat(simpleMap)
            .hasSize(1)
            .containsKey("key")
        val value = simpleMap["key"]
        assertThat(value)
            .isInstanceOf(YamlegoEvent.Scalar::class.java)
        val scalarEvents = (value as YamlegoEvent.Scalar).toEvents()
        assertThat(scalarEvents.first())
            .isInstanceOf(ScalarEvent::class.java)
        assertThat((scalarEvents.first() as ScalarEvent).value)
            .isEqualTo("value")
    }

    @Test
    fun `implicit doc with key value`() {
        val stream = parseYaml(
            """
            key: value
            """.trimIndent()
        )

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Document::class.java)
        val doc = stream.children().first() as YamlegoEvent.Document
        assertThat(doc.children()).hasSize(1)
        assertThat(doc.children().first())
            .isInstanceOf(YamlegoEvent.Mappings::class.java)
        val mappings = doc.children().first() as YamlegoEvent.Mappings
        val simpleMap = mappings.asSimpleMap()
        assertThat(simpleMap)
            .hasSize(1)
            .containsKey("key")
        val value = simpleMap["key"]
        assertThat(value)
            .isInstanceOf(YamlegoEvent.Scalar::class.java)

    }

    @Test
    fun `implicit doc with list`() {
        val stream = parseYaml(
            """
            list:
            - one
            - two
            """.trimIndent())

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Document::class.java)
        val doc = stream.children().first() as YamlegoEvent.Document
        assertThat(doc.children()).hasSize(1)
        assertThat(doc.children().first())
            .isInstanceOf(YamlegoEvent.Mappings::class.java)
        val mappings = doc.children().first() as YamlegoEvent.Mappings

        val simpleMap = mappings.asSimpleMap()
        assertThat(simpleMap)
            .hasSize(1)
            .containsKey("list")
        val value = simpleMap["list"]
        assertThat(value)
            .isInstanceOf(YamlegoEvent.Sequence::class.java)
        val seq = value as YamlegoEvent.Sequence
        assertThat(seq.children())
            .hasSize(2)
            .allMatch { it is YamlegoEvent.Scalar }

        val firstScalar = seq.children().first() as YamlegoEvent.Scalar
        assertThat(firstScalar.toEvents().first())
            .isInstanceOf(ScalarEvent::class.java)
        assertThat((firstScalar.toEvents().first() as ScalarEvent).value)
            .isEqualTo("one")

        val secondScope = seq.children().get(1) as YamlegoEvent.Scalar
        assertThat(secondScope.toEvents().first())
            .isInstanceOf(ScalarEvent::class.java)
        assertThat((secondScope.toEvents().first() as ScalarEvent).value)
            .isEqualTo("two")

    }

    @Test
    fun `implicit doc with comment`() {
        val stream = parseYaml(
            """
            # some comment here
            """.trimIndent()
        )

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Comment::class.java)
        val comment = stream.children().first() as YamlegoEvent.Comment
        assertThat(comment.toEvents().first())
            .isInstanceOf(CommentEvent::class.java)
        assertThat((comment.toEvents().first() as CommentEvent).value)
            .isEqualTo(" some comment here")
        assertThat((comment.toEvents().first() as CommentEvent).commentType)
            .isEqualTo(CommentType.BLOCK)
    }

    @Test
    fun `map with comment`() {
        val stream = parseYaml(
            """
            key: # some comment here
              value # some other comment here
            """.trimIndent()
        )

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Document::class.java)
        val doc = stream.children().first() as YamlegoEvent.Document
        assertThat(doc.children()).hasSize(1)
        assertThat(doc.children().first())
            .isInstanceOf(YamlegoEvent.Mappings::class.java)
        val mappings = doc.children().first() as YamlegoEvent.Mappings
        val simpleMap = mappings.asSimpleMap()
        assertThat(simpleMap)
            .hasSize(2)
            .containsKey("key")
            .containsKey("value")
    }

    @Test
    fun `implicit doc anchor and reference`() {
        val stream = parseYaml(
            """
            defs:
            - &anc {foo: bar}
            use:
            - a: *anc
            """.trimIndent()
        )

        assertThat(stream.children()).hasSize(1)
        assertThat(stream.children().first())
            .isInstanceOf(YamlegoEvent.Document::class.java)
        val doc = stream.children().first() as YamlegoEvent.Document
        assertThat(doc.children()).hasSize(1)
        assertThat(doc.children().first())
            .isInstanceOf(YamlegoEvent.Mappings::class.java)
        val mappings = doc.children().first() as YamlegoEvent.Mappings
        
        val simpleMap = mappings.asSimpleMap()
        assertThat(simpleMap)
            .hasSize(2)
            .containsKey("defs")
            .containsKey("use")
        
        // Verify defs section
        val defs = simpleMap["defs"]
        assertThat(defs).isInstanceOf(YamlegoEvent.Sequence::class.java)
        val defsSeq = defs as YamlegoEvent.Sequence
        assertThat(defsSeq.children()).hasSize(1)
        val defItem = defsSeq.children().first()
        assertThat(defItem).isInstanceOf(YamlegoEvent.Mappings::class.java)
        val defMap = (defItem as YamlegoEvent.Mappings).asSimpleMap()
        assertThat(defMap)
            .hasSize(1)
            .containsKey("foo")
        val fooValue = defMap["foo"] as YamlegoEvent.Scalar
        assertThat((fooValue.toEvents().first() as ScalarEvent).value)
            .isEqualTo("bar")
        
        // Verify use section
        val use = simpleMap["use"]
        assertThat(use).isInstanceOf(YamlegoEvent.Sequence::class.java)
        val useSeq = use as YamlegoEvent.Sequence
        assertThat(useSeq.children()).hasSize(1)
        val useItem = useSeq.children().first()
        assertThat(useItem).isInstanceOf(YamlegoEvent.Mappings::class.java)
        val useMap = (useItem as YamlegoEvent.Mappings).asSimpleMap()
        assertThat(useMap)
            .hasSize(1)
            .containsKey("a")
        val aValue = useMap["a"]
        assertThat(aValue).isInstanceOf(YamlegoEvent.Alias::class.java)
        val alias = aValue as YamlegoEvent.Alias
        assertThat(alias.toEvents().first())
            .isInstanceOf(AliasEvent::class.java)
        val aliasEvent = alias.toEvents().first() as AliasEvent
        assertThat(aliasEvent.alias.toString()).isEqualTo("anc")
    }
}