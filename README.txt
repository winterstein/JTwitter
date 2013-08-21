
# Welcome To JTwitter

JTwitter is a robust, easy to use library for working with Twitter.
See http://www.winterwell.com/software/jtwitter.php for info.

Developed and maintained by Daniel Winterstein of SoDash (http://sodash.com).
Released under the LGPL license.

Open source notes: You must let your users know that you are using the JTwitter 
library, which they can get the source code for. A credit on your home page 
with a link-back to our page, e.g. "built using JTwitter", is a good way 
way to do this. Your own code can be licensed commercially however you like
and you do not have to release the source code.


## liblocal: A dummy Android jar

The Android classes require android.jar to compile.

If you have a real adt install, then you should reference it's copy of android.jar

Otherwise, a small dummy Android jar is provided in the liblocal folder.
If you copy android-dummy.jar to android.jar, then AndroidTwitterLogin will compile.

This jar has some class stubs -- enough to keep Eclipse happy.
It does not contain any real working code! It should not be shipped as part of a production system.
