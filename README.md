# sbt-tmpfs
### sbt plugin that automatically "tmpfsifies" directories to speed up development.

## How to use:

Add below to `project/plugins.sbt`, and enjoy RAM speed!

    addSbtPlugin("com.github.cuzfrog" % "sbt-tmpfs" % "0.1.0")
    
## Why?

Putting directories that need to be accessed often has at least 3 benefits:

* Faster. 
* Cheaper - save your disk.
* Greener - consume less energy.