package io.github.rahulsom.yamlego

import org.snakeyaml.engine.v2.events.Event
import org.snakeyaml.engine.v2.events.StreamStartEvent

/**
 * Parser that converts a list of SnakeYAML events into a hierarchical YamlegoEvent structure.
 * 
 * This parser takes low-level SnakeYAML events and builds a tree structure that maintains
 * the original event information while providing a more convenient API for manipulation.
 */
class YamlegoParser {
    /**
     * Parses a list of SnakeYAML events into a YamlegoEvent.YamlStream.
     * 
     * The input must start with a StreamStartEvent as the first event, which forms
     * the root of the YAML document structure.
     * 
     * @param input List of SnakeYAML Event objects to parse
     * @return YamlStream containing the parsed YAML structure
     * @throws IllegalArgumentException if the first event is not a StreamStartEvent
     */
    fun parse(input: List<Event>): YamlegoEvent.YamlStream {
        if (input.first() is StreamStartEvent) {
            val yaml = YamlegoEvent.YamlStream(input.first() as StreamStartEvent)
            input.drop(1).forEach(yaml::addEvent)
            return yaml
        } else {
            throw IllegalArgumentException("First event must be a StreamStartEvent")
        }
    }
}
