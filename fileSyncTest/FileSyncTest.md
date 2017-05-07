# rsync vs sbt.IO 
## Local file synchronization performance test.

I met this problem, how to choose an at-hand algorithm to sync mapped directories?

There are at least two convenient ways:
* sbt.IO - jvm file access.
* rsync - filesystem native access.

From `sbt.IO.copyDirectory`'s doc, we know it provides fairly smart way to sync.
Modern version `rsync` seems to be adept.

I made a test to find out which one I should use.

__This test is actually more valuable in a more general context.__

## Test scheme:
1. Generate 3 file trees as source to simulate the folder user wants to sync.

| Name  | Total file amount | Max file size | Max dir depth |
| ------------- | ------------- | ------------- | ------------- |
| small  | 100  | 10kb | 3 |
| medium  | 1000  | 100kb | 6 |
| big  | 10000  | 100kb | 12 |
| big2  | 1000  | 1000kb | 12 |

([Sample Tree layout](TestFileTreeSample.md))

2. Simulate updating/modification in source file tree. And do the synchronization.
Measure how much time elapsed during synchronization.

You can do it yourself:
([Test source code](../src/test/scala/com/github/cuzfrog/RsyncVsSbtCopyTest.scala))

3. Accoutrement: 

* my dev pc: xeon-1230v2 3.3-3.7G, 32GB-DDR3-1600, ubuntu 16.04

* rsync version 3.1.1

* cp version 8.25

## Test result:

1. Source in RAM, 10 rounds total time cost:
 
| Tpe  | small | medium | big | big2 |
| ------- | ------ | ------- | ------ | ------ |
| sbt.IO  | 29 ms  | 202 ms | 2274 ms | 218 ms |
| rsync -a  | 454 ms  | 598 ms | 1590 ms | 617 ms |
| cp -au (winner) | 27 ms  | 134 ms | 1505 ms | 149 ms |

2. Source in RAM, **no modification on source dir**, 10 rounds total time cost:
 
 
 
|   | small | medium |
| ------- | ------ | ------- |
| sbt.IO  | - ms  | - ms | 
| rsync -a  | - ms  | - ms |
| cp -au  | - ms  | - ms | 