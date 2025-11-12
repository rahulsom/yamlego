package io.github.rahulsom.yamlego

import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.YamlOutputStreamWriter
import org.snakeyaml.engine.v2.api.lowlevel.Parse
import org.snakeyaml.engine.v2.emitter.Emitter
import org.snakeyaml.engine.v2.events.Event
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * Utility class for converting between YAML strings and SnakeYAML Event objects.
 *
 * This class provides bidirectional conversion capabilities:
 * - Deserializing YAML strings into lists of SnakeYAML events
 * - Serializing lists of SnakeYAML events back into YAML strings
 *
 * Both operations preserve comments and other YAML metadata.
 */
class EventUtils {
    /**
     * Deserializes a YAML string into a list of SnakeYAML Event objects.
     *
     * This method parses the input YAML string and returns the low-level events
     * that represent the structure and content of the YAML document, including comments.
     *
     * @param input The YAML string to parse
     * @return List of Event objects representing the parsed YAML structure
     */
    fun deserialize(input: String): List<Event> {
        val settings =
            LoadSettings
                .builder()
                .setParseComments(true)
                .build()

        val parse = Parse(settings)

        val p = parse.parseString(input)
        return p.toList()
    }

    /**
     * Serializes a list of SnakeYAML Event objects back into a YAML string.
     *
     * This method takes a list of events and emits them back into YAML format,
     * preserving comments and other metadata from the original events.
     *
     * @param input List of Event objects to serialize
     * @return The YAML string representation of the events
     * @throws TODO IOException handling is not yet implemented in the YamlOutputStreamWriter
     */
    fun serialize(input: List<Event>): String {
        val dumperSettings =
            DumpSettings
                .builder()
                .setDumpComments(true)
                .build()

        val baos = ByteArrayOutputStream()

        val emitter =
            Emitter(
                dumperSettings,
                object : YamlOutputStreamWriter(baos, Charset.defaultCharset()) {
                    override fun processIOException(e: IOException?) {
                        TODO("Not yet implemented")
                    }
                },
            )

        input.forEach { emitter.emit(it) }

        return String(baos.toByteArray())
    }
}