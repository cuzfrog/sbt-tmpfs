# sbt-tmpfs
sbt plugin that automatically "tmpfsifies" directories to speed up development.
   
## Why tmpfs?

Comparison with common specs:

| Tpe  | Read speed | Response time | Power usage |
| ------- | ------ | ------- | ------- | 
| ram  | 30GB/s  | 100 ns | 2 - 3 w | 
| ssd  | 500MB/s  | 0.1 ms | 1 - 3 w | 
| sas-hdd  | fast in raid  | 20+ ms | 15 w | 

RAM is thousands of times faster than SSD. Tmpfs provides an easy way to leverage RAM.
More, RAM is immune to the exertion brought by infinite compilation/clean cycles.
While SSD is tolerant, developers are not.
[Latency Numbers Every Programmer Should Know](https://gist.github.com/jboner/2841832)

## Why sbt-tmpfs?

sbt-tmpfs brings automation to leverage tmpfs to speedup your development.

## How to use:

Mount your `/tmp` with tmpfs, by adding this line to your `/etc/fstab` if you haven't yet:

    tmpfs /tmp tmpfs rw,nosuid,nodev,noatime
    
Reboot you pc.

Add below to `project/plugins.sbt`:

    addSbtPlugin("com.github.cuzfrog" % "sbt-tmpfs" % "0.1.0")
        
Now, enjoy RAM speed!
        
## Detail and Configuration:

#### Directory mode:
There are 2 strategies to use tmpfs:

* Directly mount the point with tmpfs. (Requires super privilege)
* Symlink into existing tmpfs dir. (Harder management.)

_Symlink_ is the default one, since it does not require super. 
Default linking dirs include `crossTarget` and `target/resolution-cache`.
Broken symlink will be overwrite by sbt-tmpfs.
(Symlink of some dir like `streams` may lead to sbt error when symlink is broken after a reboot.)
The base tmpfs dir where symlinks point to, by default, is `/tmp`, 
which is controlled by `tmpfsLinkBaseDirectory`.

When _Mount_ mode is in use, sbt command line may require super password to execute shell command.
Mount size limit key:`tmpfsMountSizeLimit` , shell command can also be changed by `tmpfsMountCommand`.
Default mount point is `target`.
In fact, _Mount_ mode is recommended. It's easier to handle in most cases,
and not likely to cause some unexpectation.

You can set `tmpfsDirectoryMode := TmpfsDirectoryMode.Mount` in your build.sbt.

#### Work flow:
sbt-tmpfs hooks task to `compile`, 
which checks target dirs defined in key `tmpfsLinkDirectories` or `tmpfsMountDirectories`
 and mounts/links tmpfs when necessary.
 
Under _Symlink_ mode, when user does a `clean`, symlinks themselves will be purged.
When new symlinks are created, sbt-tmpfs deletes old dirs in tmpfs that old symlinks referenced.

Task `tmpfsOn`: check and link/mount when needed.
Task `tmpfsSyncMapping`: sync mapped dirs.

#### Map and sync dirs:
Sometimes, we want to speedup some dirs while wanting to preserve them on disk, like `node_modules`,
we can map these dirs.

    tmpfsMappingDirectories := Map(
      sourceDir -> destDir //sourceDir is somewhere on disk.
    )
    
sbt-tmpfs will link/mount `destDir` with tmpfs,
if they are not an active symlink or already of tmpfs,
and automatically does one-way-synchronization: from source to destination.

There is a [Interesting test about choosing which method to do the sync](fileSyncTest/FileSyncTest.md).

## About

Author: Cause Chung (cuzfrog@139.com)

License: [Apache License Version 2.0](LICENSE)