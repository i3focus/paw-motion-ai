package com.pawmotionai.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

interface MediaStoragePort {
    fun store(key: String, bytes: ByteArray)
    fun load(key: String): Resource?
}

/** Local-disk storage (D-007). Keys are server-generated, so they never contain user input. */
@Component
class LocalDiskMediaStorage(@Value("\${app.media.root:./data/media}") root: String) : MediaStoragePort {

    private val root: Path = Path.of(root).toAbsolutePath().normalize()

    override fun store(key: String, bytes: ByteArray) {
        val target = resolve(key)
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, "upload", ".tmp")
        Files.write(temp, bytes)
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun load(key: String): Resource? = resolve(key).takeIf { Files.isRegularFile(it) }?.let(::FileSystemResource)

    private fun resolve(key: String): Path {
        val path = root.resolve(key).normalize()
        require(path.startsWith(root)) { "Invalid media key" }
        return path
    }
}
