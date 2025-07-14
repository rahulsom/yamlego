package io.github.rahulsom.yamlego

import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.events.AliasEvent
import org.snakeyaml.engine.v2.events.CollectionEndEvent
import org.snakeyaml.engine.v2.events.CollectionStartEvent
import org.snakeyaml.engine.v2.events.CommentEvent
import org.snakeyaml.engine.v2.events.DocumentEndEvent
import org.snakeyaml.engine.v2.events.DocumentStartEvent
import org.snakeyaml.engine.v2.events.Event
import org.snakeyaml.engine.v2.events.ImplicitTuple
import org.snakeyaml.engine.v2.events.MappingEndEvent
import org.snakeyaml.engine.v2.events.MappingStartEvent
import org.snakeyaml.engine.v2.events.ScalarEvent
import org.snakeyaml.engine.v2.events.SequenceEndEvent
import org.snakeyaml.engine.v2.events.SequenceStartEvent
import org.snakeyaml.engine.v2.events.StreamEndEvent
import org.snakeyaml.engine.v2.events.StreamStartEvent
import java.util.*


/**
 * Abstract base class representing a YAML event that can contain child events and be serialized back to SnakeYAML events.
 * 
 * This sealed class provides a hierarchical structure for parsing and manipulating YAML documents while maintaining
 * the ability to serialize back to the original SnakeYAML event format.
 * 
 * @param stopSign The class of event that signals the end of this event's scope, or null for leaf events
 * @param firstEvent The initial SnakeYAML event that starts this YAML construct
 */
sealed class YamlegoEvent(private val stopSign: Class<out Event>?, firstEvent: Event) {

    protected val events: MutableList<Event> = mutableListOf(firstEvent)
    protected val children: MutableList<YamlegoEvent> = mutableListOf()
    private var accepting = true

    init {
        if (stopSign == null) {
            this.accepting = false
        }
    }

    /**
     * Returns an immutable list of child events contained within this event.
     * 
     * @return List of child YamlegoEvent instances
     */
    fun children(): List<YamlegoEvent> {
        return children
    }

    /**
     * Returns all events that can be serialized by SnakeYAML
     */
    fun toEvents(): List<Event> =
        listOf(events.first()) +
                children.flatMap { it.toEvents() } +
                events.drop(1)

    /**
     * Validates this event and all its children recursively.
     * 
     * @return true if this event and all children are in a valid state
     */
    fun isValid(): Boolean {
        return isThisValid() && children.all { it.isValid() }
    }

    /**
     * Validates only this specific event, not including children.
     * 
     * @return true if this event is in a valid state
     */
    abstract fun isThisValid(): Boolean

    /**
     * Adds an event to the current event.
     * @return true if no more events can be added.
     */
    fun addEvent(event: Event): Boolean {
        val lastChildAcceptingEvents = children.lastOrNull { it.accepting }
        if (lastChildAcceptingEvents != null) {
            if (!children.last().addEvent(event)) {
                return false
            }
        } else {
            addEventToCurrent(event)
        }
        if (event.javaClass == stopSign || stopSign == null) {
            accepting = false
            return false
        } else {
            return true
        }
    }

    private fun addEventToCurrent(event: Event) =
        when (event) {
            is StreamStartEvent -> children.add(YamlStream(event))
            is DocumentStartEvent -> children.add(Document(event))
            is MappingStartEvent -> children.add(Mappings(event))
            is SequenceStartEvent -> children.add(Sequence(event))
            is CollectionStartEvent -> children.add(Collection(event))
            is CommentEvent -> children.add(Comment(event))
            is ScalarEvent -> children.add(Scalar(event))
            is AliasEvent -> children.add(Alias(event))
            else -> events.add(event)
        }

    /**
     * Represents the root YAML stream that contains all documents.
     * 
     * A YAML stream starts with a StreamStartEvent and ends with a StreamEndEvent.
     * It can contain multiple YAML documents.
     * 
     * @param startEvent The StreamStartEvent that begins this YAML stream
     */
    class YamlStream(startEvent: StreamStartEvent) : YamlegoEvent(StreamEndEvent::class.java, startEvent) {
        override fun isThisValid(): Boolean = events.size == 2
        override fun toString() =
            "${events.first()}\n" + children.joinToString("\n") { it.toString() }.prependIndent("  ") + "\n${events.drop(1).lastOrNull()}"
    }

    /**
     * Represents a single YAML document within a stream.
     * 
     * A document starts with a DocumentStartEvent and ends with a DocumentEndEvent.
     * It typically contains a single root construct (mapping, sequence, or scalar).
     * 
     * @param startEvent The DocumentStartEvent that begins this document
     */
    class Document(startEvent: DocumentStartEvent) : YamlegoEvent(DocumentEndEvent::class.java, startEvent) {
        override fun isThisValid(): Boolean = events.size == 2
        override fun toString() =
            "${events.first()}\n" + children.joinToString("\n") { it.toString() }.prependIndent("  ") + "\n${events.drop(1).lastOrNull()}"
    }

    /**
     * Represents a YAML mapping (key-value pairs, similar to a hash map or dictionary).
     * 
     * A mapping starts with a MappingStartEvent and ends with a MappingEndEvent.
     * It contains alternating key and value events as children.
     * 
     * @param startEvent The MappingStartEvent that begins this mapping
     */
    class Mappings(startEvent: MappingStartEvent) : YamlegoEvent(MappingEndEvent::class.java, startEvent) {
        override fun isThisValid(): Boolean = true
        /**
         * Converts this mapping to a Map with Scalar keys and YamlegoEvent values.
         * 
         * If there's an odd number of children (unpaired key), a "PENDING" scalar is added as the value.
         * 
         * @return Map representation of this mapping
         * @throws IllegalStateException if a non-scalar key is encountered
         */
        fun asMap(): Map<Scalar, YamlegoEvent> {
            val map = mutableMapOf<Scalar, YamlegoEvent>()
            val childrenQueue = children.toMutableList()
            if (childrenQueue.size % 2 != 0) {
                childrenQueue.add(Scalar(ScalarEvent(Optional.empty(), Optional.empty(), ImplicitTuple(true, false), "PENDING", ScalarStyle.PLAIN)))
            }
            while(childrenQueue.isNotEmpty()) {
                val key = childrenQueue.removeAt(0)
                val value = childrenQueue.removeAt(0)
                if (key is Scalar) {
                    map[key] = value
                } else {
                    throw IllegalStateException("Mappings must have key-value pairs")
                }
            }
            return map
        }
        /**
         * Converts this mapping to a simple Map with String keys and YamlegoEvent values.
         * 
         * @return Map with string keys extracted from scalar events
         */
        fun asSimpleMap() = asMap().mapKeys { (it.key.events.first() as ScalarEvent).value.toString() }

        override fun toString() =
            "${events.first()}\n" + asSimpleMap().entries.joinToString("\n") { (k, v) ->
                "${k.prependIndent("  ")}:\n" + v.toString().prependIndent("    ")
            } + "\n${events.drop(1).lastOrNull()}"
    }

//    class Mapping(val key: Scalar, val value: BlackAdderEvent): BlackAdderEvent(null, key.events.first()) {
//        override fun isThisValid(): Boolean = true
//        override fun toString() = "$key:\n${value.toString().prependIndent("  ")}"
//    }

    /**
     * Represents a YAML sequence (ordered list of items, similar to an array).
     * 
     * A sequence starts with a SequenceStartEvent and ends with a SequenceEndEvent.
     * It contains sequential items as children.
     * 
     * @param startEvent The SequenceStartEvent that begins this sequence
     */
    class Sequence(startEvent: SequenceStartEvent) : YamlegoEvent(SequenceEndEvent::class.java, startEvent) {
        override fun isThisValid(): Boolean = true
        override fun toString() =
            "${events.first()}\n" + children.joinToString("\n") { it.toString() }.prependIndent("  ") + "\n${events.drop(1).lastOrNull()}"
    }

    /**
     * Represents a generic YAML collection.
     * 
     * A collection starts with a CollectionStartEvent and ends with a CollectionEndEvent.
     * This is a more generic form of sequences and mappings.
     * 
     * @param startEvent The CollectionStartEvent that begins this collection
     */
    class Collection(startEvent: CollectionStartEvent) : YamlegoEvent(CollectionEndEvent::class.java, startEvent) {
        override fun isThisValid(): Boolean = true
        override fun toString() =
            "${events.first()}\n" + children.joinToString("\n") { it.toString() }.prependIndent("  ") + "\n${events.drop(1).lastOrNull()}"
    }

    /**
     * Represents a YAML comment.
     * 
     * Comments are standalone events that don't have ending events, hence the null stopSign.
     * They preserve comment text and type information from the original YAML.
     * 
     * @param startEvent The CommentEvent containing the comment information
     */
    class Comment(startEvent: CommentEvent) : YamlegoEvent(null, startEvent) {
        override fun isThisValid(): Boolean = true
        override fun toString() = "Comment(${events.first()})"
    }

    /**
     * Represents a YAML scalar value (string, number, boolean, etc.).
     * 
     * Scalars are leaf events that don't have ending events, hence the null stopSign.
     * They contain the actual data values in YAML documents.
     * 
     * @param startEvent The ScalarEvent containing the scalar value and metadata
     */
    class Scalar(startEvent: ScalarEvent) : YamlegoEvent(null, startEvent) {
        override fun isThisValid(): Boolean = true
        override fun toString() = "Scalar(${events.first()})"
    }

    /**
     * Represents a YAML alias reference (reference to an anchor).
     * 
     * Aliases are leaf events that don't have ending events, hence the null stopSign.
     * They reference previously defined anchors using the '*' syntax.
     * 
     * @param startEvent The AliasEvent containing the alias reference information
     */
    class Alias(startEvent: AliasEvent) : YamlegoEvent(null, startEvent) {
        override fun isThisValid(): Boolean = true
        override fun toString() = "Alias(${events.first()})"
    }
}