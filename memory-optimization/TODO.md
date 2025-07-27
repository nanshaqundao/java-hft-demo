# Java高性能编程学习Todo

## 🎯 优先级说明
- 🔥 高优先级：容易犯错，影响严重
- ⚡ 中优先级：性能相关，需要理解
- 📚 低优先级：深入理解，锦上添花

---

## 🔥 并发编程陷阱

### 1. volatile的非原子性问题
```java
// ❌ 错误：看似线程安全，实际有竞态条件
private volatile long counter;
public void increment() {
    counter++;  // 非原子操作！读-改-写三步骤
}

// ✅ 正确：使用原子类
private final AtomicLong counter = new AtomicLong();
public void increment() {
    counter.incrementAndGet();
}
```
**学习要点**：volatile vs AtomicXxx的区别

### 2. CAS操作的ABA问题
```java
// 学习：Compare-And-Swap的经典陷阱
// 当前项目用了CAS，但要了解ABA问题
```
**学习要点**：什么是ABA问题，如何避免

---

## ⚡ 内存管理陷阱

### 3. 直接内存的并发安全
```java
// ❌ 错误：多线程访问ByteBuffer
ByteBuffer buffer = ByteBuffer.allocateDirect(size);
// 多个线程同时调用buffer.putLong()会数据竞争

// ✅ 正确：同步或分区访问
public synchronized void writeData(long data) {
    buffer.putLong(data);
}
```
**学习要点**：堆外内存的线程安全性

### 4. 内存映射文件(mmap)的陷阱
```java
// 学习：MappedByteBuffer的并发问题
// 比直接内存更复杂的同步需求
```
**学习要点**：mmap在高并发下的使用注意事项

### 5. 直接内存泄漏问题
```java
// 学习：什么情况下DirectByteBuffer会泄漏
// 当前项目是安全的，但要了解风险场景
```
**学习要点**：直接内存的生命周期管理

---

## 📚 高级性能优化

### 6. 无锁编程的复杂性
```java
// 学习：Lock-free数据结构的设计原理
// 当前项目用了ConcurrentLinkedQueue，了解其实现
```
**学习要点**：无锁队列、无锁栈的实现原理

### 7. NUMA相关优化
```java
// 学习：Non-Uniform Memory Access对性能的影响
// 线程绑定、内存亲和性等概念
```
**学习要点**：NUMA架构下的Java优化

### 8. False Sharing问题
```java
// 学习：缓存行污染问题
// @Contended注解的使用
```
**学习要点**：CPU缓存行对性能的影响

---

## 🔄 学习建议

### 学习顺序
1. **先完成当前项目的理解** ✅ 
2. 深入学习volatile vs AtomicXxx (🔥)
3. 理解CAS和ABA问题 (🔥)
4. 学习直接内存的并发控制 (⚡)
5. 了解无锁编程基础 (📚)
6. 研究NUMA和False Sharing (📚)

### 学习资源提醒
- Java Concurrency in Practice (必读)
- The Art of Multiprocessor Programming
- JVM官方并发指南
- Mechanical Sympathy博客

### 实践建议
- 每个概念都写小Demo验证
- 用JMH测试性能差异
- 尝试故意制造这些问题，然后修复

---

## ✅ 完成记录
- [ ] volatile非原子性
- [ ] ABA问题
- [ ] 直接内存并发
- [ ] mmap陷阱
- [ ] 内存泄漏场景
- [ ] 无锁编程基础
- [ ] NUMA优化
- [ ] False Sharing

---

*备注：这些都是当前项目之外的知识点，现在专注理解项目本身！*