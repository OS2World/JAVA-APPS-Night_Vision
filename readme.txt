Night Vision source files
-------------------------

Night Vision may be examined and built with the source files that
have been included in the package that includes this file.

Building NV requires the Java Development Kit and the Ant build tool,
and the knowledge of building Java programs.

Not included in this package (but may be included later in another
package or perhaps this one) are various configuration and other files
used in creating a releasable package, such as:
- files for the ProGuard shrinker/optimizer/obfuscator (to shrink
  code size)
  http://proguard.sourceforge.net/
- files for the Launch4j wrapper to create a Windows executable
  http://launch4j.sourceforge.net/
- files for the Inno Setup installer for Windows
  http://www.innosetup.com/isinfo.php
- files for BitRock InstallBuilder installer for Mac and Linux
  http://installbuilder.bitrock.com/index.html

NV began as a program for the OS/2 operating system.  Around the
year 2000 conversion to Java began.  After conversion was complete
a number of new features have been added, and now (as of March 2019)
NV comprises 69 Java source files.  Some comments on the Java source:
- There were many problems encountered in the Java compilers at
  the time the earliest NV source files were written.  Many of the
  work-arounds are still there, as focus has always been in adding
  new features rather than re-writing existing (and working) code.
- Documentation is hopefully adequate in most cases, but can and
  should be enhanced.  Most of the documentation was written before
  the transition to open source.
- Some incompatibilities between Java 1.3 and 1.4 had to be dealt
  with previously by compiling some files with 1.3 and the rest
  with 1.4.  This allowed NV to run in both environments.  Now
  that NV requires a minimum of Java 1.5 this mixed-compile is
  no longer done, and hopefully all of the code adjustments for
  this have been removed, but it is possible some vestiges remain.
- Some of the smaller source files should be re-examined and
  perhaps removed or consolidated with other files.

