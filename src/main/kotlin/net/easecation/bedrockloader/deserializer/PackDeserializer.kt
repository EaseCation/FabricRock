package net.easecation.bedrockloader.deserializer

import java.util.zip.ZipFile

interface PackDeserializer<T> {

    fun deserialize(file: ZipFile): T

}