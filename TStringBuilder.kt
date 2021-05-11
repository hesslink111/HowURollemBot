package io.deltawave.primaryserver.roll

import com.pengrad.telegrambot.model.MessageEntity

class TStringBuilder {
    private val stringBuilder = StringBuilder()
    private val entities = mutableListOf<MessageEntity>()
    val size get() = stringBuilder.length
    val entityCount get() = entities.size

    fun append(s: String) {
        stringBuilder.append(s)
    }

    fun append(s: String, makeEntity: (o: Int, l: Int) -> List<MessageEntity>) {
        makeEntity(stringBuilder.length, s.length).forEach(entities::add)
        stringBuilder.append(s)
    }

    fun append(s: String, entityTypes: List<MessageEntity.Type>) {
        entityTypes
            .map { MessageEntity(it, stringBuilder.length, s.length) }
            .forEach(entities::add)
        stringBuilder.append(s)
    }

    fun shortenToLeq(length: Int, entityCount: Int) {
        val newLength = entities.withIndex()
            .firstOrNull { it.index >= entityCount || it.value.offset() + it.value.length() > length }
            ?.value?.offset() ?: length
        stringBuilder.setLength(newLength)
        entities.removeIf { it.offset() >= newLength }
    }

    fun toTString(): Pair<String, List<MessageEntity>> = Pair(stringBuilder.toString(), entities)
}
