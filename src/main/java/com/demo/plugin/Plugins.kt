package com.demo.plugin

import com.demo.plugin.plugincommon.Plugin
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.full.primaryConstructor

class PluginClassLoader(private val classPath: String) : URLClassLoader(arrayOf(File(classPath).toURI().toURL())) {
    init {
        println("Classloader init @${hashCode()}")
    }

    protected fun finalize() {
        println("Classloader will be gc @${hashCode()}")
    }
}

class PluginLoader(private val classPath: String) {

    private val watcher = FileWatcher(
        File(classPath),
        onCreated = ::onFileChanged,
        onModified = ::onFileChanged,
        onDeleted = ::onFileChanged
    )

    private var classLoader: PluginClassLoader? = null
    private var plugin: Plugin? = null

    fun load() {
        reload()
        watcher.start()
    }

    private fun onFileChanged(file: File) {
        println("$file changed, reloading...")
        reload()
    }

    @Synchronized
    private fun reload() {
        plugin?.stop()
        this.plugin = null
        this.classLoader?.close()
        this.classLoader = null

        val classLoader = PluginClassLoader(classPath)
        val properties = classLoader.getResourceAsStream(Plugin.CONFIG)?.use {
            Properties().also { properties ->
                properties.load(it)
            }
        } ?: run {
            classLoader.close()
            return println("Cannot find config file for $classPath")
        }

        plugin = properties.getProperty(Plugin.KEY)?.let {
            val pluginImplClass = classLoader.loadClass(it) as? Class<Plugin>
                ?: run {
                    classLoader.close()
                    return println("Plugin Impl from $classPath: $it should be derived from Plugin.")
                }

            pluginImplClass.kotlin.primaryConstructor?.call()
                ?: run {
                    classLoader.close()
                    return println("Illegal! Plugin has no primaryConstructor!")
                }
        }

        plugin?.start()
        this.classLoader = classLoader

        System.gc()
    }

}


fun main() {
    arrayOf(
        "plugin-1/build/libs/plugin-1-1.0-SNAPSHOT.jar",
        "plugin-2/build/libs/plugin-2-1.0-SNAPSHOT.jar"
    ).map {
        PluginLoader(it).also { it.load() }
    }
}