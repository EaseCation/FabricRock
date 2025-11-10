package net.easecation.bedrockloader.sync.client.model

/**
 * 同步取消异常
 * 用于中断同步流程
 */
class CancelledException(message: String = "同步已被取消") : Exception(message)
