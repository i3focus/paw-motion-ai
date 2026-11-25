package com.pawmotionai.notification

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Runs external I/O (push, email) outside the request's database transaction (N15). Work registered inside a
 * transaction is queued only after a successful commit (rollback discards it) and executes on a small bounded pool, so
 * a slow provider neither holds the DB connection nor the request thread. When the queue is full the task is dropped
 * and logged: notifications are best effort and must never back-pressure request handling.
 */
@Component
open class PostCommitDispatcher {
    private val log = LoggerFactory.getLogger(PostCommitDispatcher::class.java)
    private val executor = ThreadPoolExecutor(
        WORKERS, WORKERS, 30, TimeUnit.SECONDS, ArrayBlockingQueue(QUEUE_CAPACITY),
        { runnable -> Thread(runnable, "post-commit-io").apply { isDaemon = true } },
    ).apply { allowCoreThreadTimeOut(true) }

    open fun afterCommit(task: Runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = submit(task)
            })
        } else {
            submit(task)
        }
    }

    private fun submit(task: Runnable) {
        try {
            executor.execute {
                try {
                    task.run()
                } catch (exception: Exception) {
                    log.warn("post-commit task failed: {}", exception.message)
                }
            }
        } catch (exception: RejectedExecutionException) {
            log.warn("post-commit queue full; dropping best-effort task")
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }

    private companion object {
        const val WORKERS = 2
        const val QUEUE_CAPACITY = 1000
    }
}
