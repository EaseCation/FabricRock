package net.easecation.bedrockloader.util

import net.minecraft.util.Identifier

fun identifierOf(namespace: String, path: String): Identifier {
    //? if >=1.21 {
    return Identifier.of(namespace, path)
    //?} else {
    /*return Identifier(namespace, path)
    *///?}
}

fun identifierOf(id: String): Identifier {
    //? if >=1.21 {
    return Identifier.of(id)
    //?} else {
    /*return Identifier(id)
    *///?}
}
