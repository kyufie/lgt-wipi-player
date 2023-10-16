# LGT WIPI Player
LGT WIPI Player is a compatibility layer (like [wine](https://winehq.org)) that
allows LGT WIPI applications to run on Android devices.

## Building
Simple `gradle build` should be enough.

## Technical details
The WIPI apps are packaged in the form of an Android application that
shares the same user id with the compatibility layer, therefore they
must be signed with the same key.
The app contains the actual WIPI application to be launched
as well as a simple launcher that sends an intent ```android.intent.action.LAUNCH_WIPI```
to the compatibility layer to launch the WIPI application.

The only thing I can find about this thing is the app itself (no source code, or additional resources).
The app is so old, it was made to support Android 3 or 4.1?, and
it's not compatible with newer versions of Android.

The "Android part" of the "Player" itself is pretty simple.
It's just a wrapper that exposes some Android API to the "real" compatibility layer.
Whereas the "real" compatibility layer is a library made with C programming language.
Since it's a native library, doing anything with it is extremely difficult.

## The problem
Unfortunately, this software is not working with newer Android versions.
In other words, this compatibility layer has compatibility problem.

The problem was not obvious as there were no error messages emitted.
This was due to the poor error handling mechanism, which, in the event of failure,
fails silently instead of reporting it.
I guess the developers think that their creation is so perfect, they don't need
error logs. Or, maybe they're just being lazy like we all do.

The source of the problem is related to a certain type of function.
It goes like this:
* Call the function
* The function return a pointer on success, and a negative integer to indicate failure,
and based on the returned integer, what kind of failure that happened.
The returned pointer is obtained from memory allocation.

One thing that the developers didn't realize is that pointers are integers, and integers,
when treated as signed integers can appear negative.
The error checker expect negative integer as one of the possible output, so it treats
the function return value as signed integer, regardless whether it's a pointer or not.
Since the pointer is obtained through memory allocation, its return value can be
unpredictable. You'll never know when you'll get a "negative" pointer and die.

I assume in the old days of Android almost all dynamically allocated memories are
located in the lower half of the address space, which makes their pointer "positive".
I guess that's why it passed the developer's test.

There are still other problems regarding this compatibility issue, but I won't cover
them all as they're all quite minor.

## The solution
The solution to the memory allocation problem is by overriding system's memory
allocator with a memory allocator that can only emit "positive" pointers.

The current solution is not bulletproof. In fact, it has lots of holes in it.
But it does the job pretty well.

## The source code
The source code is decompiled with [jadx](https://github.com/skylot/jadx).

## Special thanks to
* Rostislav582
