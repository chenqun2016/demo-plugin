#动态加载 插件 demo

运行 plugin-1  plugin-2  的 assemble
运行 Plugins.kt 文件

开发步骤：
插件：
1：需要 implementation project(":plugin-common")
2：实现Plugin类
3：配置 plugin.config文件，   plugin.impl=Plugin实现类全名
主工程：
4：FileWatcher：文件监听类，通过fileSystem.newWatchService实现对文件改动的监听。
5：自定义类加载器 ：PluginClassLoader
6：通过类加载器 获取 plugin.config 的 内容并加载该类
7：通过反射调用类的主构造器来创建实例，调用方法。