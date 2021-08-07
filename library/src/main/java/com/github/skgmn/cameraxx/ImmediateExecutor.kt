package com.github.skgmn.cameraxx

import java.util.concurrent.Executor

internal class ImmediateExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}