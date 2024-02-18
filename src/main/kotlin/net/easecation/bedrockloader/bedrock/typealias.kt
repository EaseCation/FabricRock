package net.easecation.bedrockloader.bedrock

import net.minecraft.util.Identifier

typealias BedrockTexturePath = String   // 不包含命名空间的路径(如textures/block/stone)
typealias JavaTexturePath = Identifier  // 包含命名空间，不包含textures的路径(如minecraft:block/stone)