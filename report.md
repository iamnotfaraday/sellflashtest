加入乐观锁(seckill_goods启用version字段), 发现性能大打折扣, 如图

![image-20260629182904553](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260629182904553.png)

平均响应时间达到800+ms, 吞吐量只有约400/sec

检查order表, 发现userid不是很集中, 相对来说比较分散, 感觉就是在1000中均匀选100人, 但不是某个子区间连续100人

![image-20260629184753286](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260629184753286.png)



不加乐观锁版本:

![image-20260630211632000](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260630211632000.png)

average: 78, 吞吐率976, 并且用户分布数十分连续, 为4 - 103子区间

![image-20260630212132297](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260630212132297.png)





2026年7月2日压测最新发现乐观锁版本出现少卖, 图片数据如下

![image-20260702213629888](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260702213629888.png)



![image-20260702213757609](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260702213757609.png)

少卖29份, 值得一提的是, 每一个失败的请求都是返回的库存不足, 且呈现十分均匀的10组成功一个请求现象

奇了怪了, 错误率95%居然还卖完了

![image-20260702215202226](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260702215202226.png)



![image-20260702215230372](C:\Users\524\AppData\Roaming\Typora\typora-user-images\image-20260702215230372.png)

奇怪, 错误率应该是90%啊

上面少卖, 我发现一个规律, 只要我关掉jemter, 然后数据库库存和订单重置, 然后重新开测的第一次少卖概率100%?(测试了6次), 然后只要第一次少卖后, 不关闭jmeter, 然后数据库和订单重置, 重测就会发现无论多少次少卖都不可能(试了5次), 而且错误率都是90%