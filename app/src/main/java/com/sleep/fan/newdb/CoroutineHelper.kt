package com.sleep.fan.newdb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

object CoroutineHelper {
    private val parentJob = SupervisorJob()
    val ioScope = CoroutineScope(Dispatchers.IO + parentJob)

    fun cancel() {
        parentJob.cancel()
    }
}
