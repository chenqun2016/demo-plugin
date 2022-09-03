package com.demo.plugin

import java.io.File
import java.nio.file.*
import java.nio.file.WatchEvent.Kind
import java.nio.file.attribute.BasicFileAttributes
import kotlin.concurrent.thread

typealias FileEventListener = (file: File) -> Unit
private val EMPTY: FileEventListener = {}

class FileWatcher(private val watchFile: File,
                  private val recursively: Boolean = true,
                  private val onCreated: FileEventListener = EMPTY,
                  private val onModified: FileEventListener = EMPTY,
                  private val onDeleted: FileEventListener = EMPTY) {

    private val folderPath by lazy {
        Paths.get(watchFile.canonicalPath).let {
            if (Files.isRegularFile(it)) it.parent else it
        } ?: throw IllegalArgumentException("Illegal path: $watchFile")
    }

    @Volatile
    private var isWatching = false

    private fun File.isWatched() = watchFile.isDirectory || this.name == watchFile.name

    private fun Path.register(watchService: WatchService, recursively: Boolean, vararg events: Kind<Path>) {
        when (recursively && watchFile.isDirectory) {
            true -> // register all subfolders
                Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        dir.register(watchService, *events)
                        return FileVisitResult.CONTINUE
                    }
                })
            false -> register(watchService, *events)
        }
    }

    @Synchronized
    fun start() {
        if(!isWatching){
            isWatching = true
        }

        thread {
            // We obtain the file system of the Path
            val fileSystem = folderPath.fileSystem

            // We create the new WatchService using the try-with-resources block(in kotlin we use `use` block)
            fileSystem.newWatchService().use { service ->
                // We watch for modification events
                folderPath.register(service, recursively,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )

                // Start the infinite polling loop
                while (isWatching) {
                    // Wait for the next event
                    val watchKey = service.take()

                    watchKey.pollEvents().forEach { watchEvent ->
                        // Get the type of the event
                        Paths.get(folderPath.toString(), (watchEvent.context() as Path).toString()).toFile()
                            .takeIf {
                                it.isWatched()
                            }
                            ?.let(
                                when (watchEvent.kind()) {
                                    StandardWatchEventKinds.ENTRY_CREATE -> {
                                        println("onCreated")
                                        onCreated
                                    }
                                    StandardWatchEventKinds.ENTRY_DELETE -> {
                                        println("onDeleted")
                                        onDeleted
                                    }
                                    else ->{
                                        println("onModified")
                                        onModified // modified.
                                    }
                                }
                            )
                    }

                    if (!watchKey.reset()) {
                        // Exit if no longer valid
                        break
                    }
                }
            }
        }
    }

    @Synchronized
    fun stop(){
        isWatching = false
    }
}