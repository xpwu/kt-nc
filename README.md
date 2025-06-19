# kt-nc
kotlin 通知中心，并发安全

## 0、代码库的引用
1、使用 jitpack 直接依赖 github 代码   
2、在 root build.grable 加入
```
allprojects {
	repositories {
		google()
		mavenCentral()
		// 在依赖库列表的最后加入jit依赖库
		maven { url 'https://jitpack.io' }
	}
}
```
3、在 module build.grable 加入
```
dependencies {
  // 加入如下依赖，x.x.x填写为具体使用的版本号
  implementation 'com.github.xpwu:kt-nc:x.x.x'
}

```

## 1、event
* 满足 Event 协议的即可成为 Event，一个类即代表一个事件。比如：
  ```kotlin
  class UploadProgressChanged(val ids: Array<String>): Event
  class UserInfoChanged(val ids: Array<String>): Event
  class Built(val ids: Array<Int>): Event
  ```

## 2、nc
* 添加事件
  ```kotlin
  val nc = NC()
  
  val item = nc.addEvent(UserInfoChanged::class) {
			// user code
		}
  ```

* post 事件
  ```kotlin
  nc.post(UserInfoChanged(arrayOf("a", "b")))
  ```

* 删除事件
  ```kotlin
  item.remove()
  ```

#### 注：
addEvent 返回的 ObserverItem 需要调用方保存管理，
NC 不会持有 ObserverItem

## 3、observer
为方便管理多个 ObserverItem，可以使用扩展 Observer 协议来实现，比如需要扩展 SomeView 为CObserver
  ```kotlin
  class SomeView: Observer {
    override val events: ObserverEvents = ObserverEvents()
  }
  ```

则，SomeView 就扩展了如下三个方法
  ```kotlin
  view.addEvent()
  view.removeEvent()
  view.removeAll()
  ```

## 4、内存管理
  ```
   //  ~~~~> hold weakly
   //  ----> hold strongly 
  
   NC ~~~~> Item ----> block
  ```
* 返回的 Item 需要由调用层管理。
* 调用 item.remove()、 observer.removeEvent()或者 observer.removeAll() 都将切断 Item 对 block 的持有。
* 如果 Item 被释放，NC 会自动 remove 此 Item 对应的事件，但是为了防止 block 中引起的循环引用，
  所有的 Item 都建议通过 item.remove()、observer.removeEvent(from)或者 observer.removeAll() 手动删除
