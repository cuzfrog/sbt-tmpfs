# rsync vs cp vs sbt.IO 
## Local file synchronization performance test.

I want to do some one-way sync: cp file from source to destination.
I met this problem, how to choose an at-hand algorithm to sync mapped directories?

There are at least several convenient ways:
* sbt.IO
* rsync -a
* cp -au
* common-io (No test, introduces extra dependency.)

From `sbt.IO.copyDirectory`'s doc, we know it provides fairly smart way to sync.
Modern version `rsync` seems to be an adept.

__This test is actually more valuable in a more general context.__

## Test scheme:
Generate 3 file trees as source to simulate the folder user wants to sync.

| Name  | Total file amount | Max file size | Max dir depth |
| ------------- | ------------- | ------------- | ------------- |
| small  | 100  | 10kb | 3 |
| medium  | 1000  | 100kb | 6 |
| big  | 10000  | 100kb | 12 |
| big2  | 1000  | 1000kb | 12 |

([Sample Tree layout](TestFileTreeSample.md))

Simulate updating/modification in source file tree. And do the synchronization.
Measure how much time elapsed during synchronization.

Because in my scenario, sync happens often while source dir may not change frequently,
before sync, only do some minor modification to source dir
(add 5% extra files, and modify(append) 5% files).

Modification simulation is random, so every time the burden it inflicts to sync may be
different. Test up to 10 rounds to mitigate this effect. 
A fresh source dir for each combination grows after every round.

Test with scala app. Shell commands are executed by `sbt.Process`.
(Which may be not fair, but this is only what my scenario suits.)

Every run, only start one combination, e.g. sbt + ram  or rsync + ssd.

Do it yourself (you need enough RAM):
([Test source code](../src/test/scala/com/github/cuzfrog/RsyncVsSbtCopyTest.scala))

#### Accoutrement: 

* my dev pc: xeon-1230v2 3.3-3.7G, 32GB-DDR3-1600, ubuntu 16.04

* sbt version 0.13.15 / jdk 1.8 / scala 2.12

* rsync version 3.1.1

* cp version 8.25

## Test result:

1. Source in RAM, total time cost:
 
| Tpe  | small | medium | big | big2 |
| ------- | :---: | :---: | :---: | :---: |
| sbt | 20ms | 273ms | 2906ms | 507ms |
| rsync -a | 454ms | 751ms | 3394ms | 2444ms |
| cp -au | 35ms | 209ms | 1712ms | 636ms |

2. Source in RAM, **no modification on source dir**, total time cost:
 
| Tpe  | small | medium | big | big2 |
| ------- | :---: | :---: | :---: | :---: |
| sbt | 20ms | 225ms | 2552ms | 304ms |
| rsync -a | 453ms | 539ms | 1441ms | 625ms |
| cp -au | 18ms | 115ms | 1354ms | 133ms |

3. Source on SSD, total time cost:
 
| Tpe  | small | medium |
| ------- | :---: | :---: | 
| sbt.IO  |  23ms | 280ms | 
| rsync -a  | 455ms | 812ms |
| cp -au  |  22ms | 185ms |

4. Source on SSD, **no modification on source dir**, total time cost:
 
| Tpe  | small | medium |
| ------- | :---: | :---: | 
| sbt.IO  |  21ms | 252ms |
| rsync -a  | 453ms | 615ms |
| cp -au  | 18ms | 106ms | 

I chose `cp -au` to do the sync.
