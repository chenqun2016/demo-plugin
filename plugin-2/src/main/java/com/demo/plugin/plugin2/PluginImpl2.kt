package com.demo.plugin.plugin2

import com.demo.plugin.plugincommon.Plugin

class PluginImpl2: Plugin{
    override fun start() {
        println("Plugin2: Start")
    }

    override fun stop() {
        println("Plugin2: Stop")
    }
}
