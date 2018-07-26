
# 时间轮
## java调度方法：
### Timer，ScheduledExecutorService 
时间复杂度 O(log(n))
因为它们使用的 是  最小堆的对排序，每当有新任务的时候都需要堆堆进行插入，
堆排序插入的时间复杂度为 O(log(n))
### Timer 的问题：

1. 如果任务执行时间过长，TimerTask会出现延迟执行的情况。比如，第一任务在1000ms执行了4000ms，第二个任务定时在2000ms开始执行。这里由于第一个任务要执行4000，所以第二个任务实际在5000ms开始执行。这是由于Timer是单线程，且顺序执行提交的任务
2. 如果执行任务抛出异常，Timer是不会执行会后面的任务的

### ScheduledExecutorService 解决了这些问题
实现方式|加入任务|取消任务|运行任务
---|:--:|---:|---:
基于排序链表|O(n)|	O(1)|	O(1)
基于最小堆|	O(lgn)|	O(1)|	O(1)

## 有没有更高效的算法呢
# 有的 那就是 时间轮 调度算法
## 算法demo
参见code
### 算法作者
根据George Varghese 和 Tony Lauck 1996 年的论文 [Hashed and Hierarchical Timing Wheels: data structures to efficiently implement a timer facility](https://github.com/wolaiye1010/zdc-java-script/blob/master/twheel.pdf)

## 算法原理：
![1](http://img.my.csdn.net/uploads/201209/29/1348926970_9123.png)

### 问题:轮子过大

### 解决办法：多层时间轮
<img src="http://pic1.58cdn.com.cn/dwater/fang/big/n_v27a8a06eebb464455a2d9d276610d29b4.jpg" width="400" />

### 应用
linux 定时器，游戏buffer

### 算法对比
实现方式|加入任务|取消任务|运行任务
---|:--:|---:|---:
基于排序链表|O(n)|	O(1)|	O(1)
基于最小堆|	O(lgn)|	O(1)|	O(1)
基于时间轮|	O(1)|	O(1)|	O(1)

java代码实现及使用：
#### 使用
```javascript 1.8
TimeWheelService instance = TimeWheelService.instance;
        System.out.println("      start:"+new Date());
        instance.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task1 start:"+new Date());
//                throw new RuntimeException("aaa");
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },1000);

        instance.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task2 start:"+new Date());
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },2000);
```
[github java 实现](https://github.com/wolaiye1010/zdc-java-script](https://github.com/wolaiye1010/zdc-java-script)

[实现](https://github.com/wolaiye1010/zdc-java-script/blob/master/src/main/java/com/zdc/java/script/TimeWheelService.java)

[使用](https://github.com/wolaiye1010/zdc-java-script/blob/master/src/test/java/com/zdc/java/script/TimeWheelTest.java)

[论文（英文）](https://github.com/wolaiye1010/zdc-java-script/blob/master/twheel.pdf)

[参考博文（英文）](http://www.embeddedlinux.org.cn/RTConforEmbSys/5107final/LiB0071.html)

[惊艳的时间轮定时器](https://www.cnblogs.com/zhongwencool/p/timing_wheel.html)

[Linux 下定时器的实现方式分析](https://www.ibm.com/developerworks/cn/linux/l-cn-timers/))

