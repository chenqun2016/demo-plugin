package com.demo.plugin.plugin1

import com.demo.plugin.plugincommon.Plugin

class PluginImpl1: Plugin{
    override fun start() {
        println("Plugin1: Start")
        newMethod()
    }

    fun newMethod(){
        println("newMethod called!! 2")
    }

    override fun stop() {
        println("Plugin1: Stop")
    }
}
