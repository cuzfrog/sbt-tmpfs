### v0.3.2
* Standardize `[SbtTmpfsPlugin]` prefix in logger.
* When no tty present and no askpass program specified,
 mount will fail by logging error instead of throwing RuntimeException.

### v0.3.1
* Suppress `cp` preserving timestamps permission error.
* Abort in CI environment.