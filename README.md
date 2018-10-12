# 远程接入协议

[![Download](https://api.bintray.com/packages/mechdancer/maven/remote/images/download.svg?version=0.1.0) ](https://bintray.com/mechdancer/maven/remote/0.1.0/link)

## 设计思路

本协议框架由2部分组成：

* 指令传输和远程调用
* 数据传输

其中指令传输采用3种方式：

* udp 组播：传递内部指令、进程信息和用户广播报文
* udp 单播：传递无可靠性要求的用户数据报
* tcp：传递有可靠性要求的用户报文、实现远程调用

## 开始使用

* Gradle
* Maven
* Bintray

您需要将其添加至  [仓库和依赖](https://docs.gradle.org/current/userguide/declaring_dependencies.html) 中。

### Gradle

```groovy
repositories {
    jcenter()
}
dependencies {
    compile 'org.mechdancer:remote:0.1.0'
}
```

### Maven

```xml
<repositories>
   <repository>
     <id>jcenter</id>
     <name>JCenter</name>
     <url>https://jcenter.bintray.com/</url>
   </repository>
</repositories>

<dependency>
  <groupId>org.mechdancer</groupId>
  <artifactId>remote</artifactId>
  <version>0.1.0</version>
  <type>pom</type>
</dependency>
```

### Bintray

您总可以从 bintray 直接下载 jar： [![Download](https://api.bintray.com/packages/mechdancer/maven/remote/images/download.svg?version=0.1.0) ](https://bintray.com/mechdancer/maven/remote/0.1.0/link)

## 名词解释

* 内部指令：包括服务发现、地址传递等服务于框架内部功能的指令
* 远程调用：调用位于另一进程中的方法，并阻塞，直到获取返回值
* 远程触发：非阻塞地调用位于另一进程中的方法

## 内部指令

`BroadcastServer` 是服务的单位，提供所有通过组播实现的功能，即内部指令解析和响应以及收发数据广播。

内部指令指的是包括服务发现、地址传递等服务于框架内部功能的指令。

协议如下：\[id: Byte]\[name length: Byte]\[name: String]\[info: ByteArray]


| 功能 | Id |
| :--: | :--: |
| 成员询问 | 0 |
| 回复询问 | 1 |
| 数据广播 | 127 |
