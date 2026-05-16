# Flaky Test Report

**Name:** Boos, Jan

## Flaky Test 1

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#save bets to the shared file`

**Root cause:**

Within the test a new `FileBettingService` object is instantiated using a `SHARED_BET_FILE` which
is in a temporary directory of the JVM. The comment above the variable also tells us, that the
filename is only unique per JVM launch. One run of all tests happens in one JVM launch so if the
passed file already exists and has beed modified, there will be more than zero bets stored. If the
betting file already exists and still stores some old bets, the number of placed bets is greater
than three which leads to a false assertion.

**Fix:**

I added a `BeforeTest` function called `delete shared test file()` which delete the
`SHARED_BET_FILE` if it exists. Now each test operates on a fresh file and is not influenced by any
other test.

## Flaky Test 2

**Test name:** `de.seuhd.worldcup.WorldCupTest#load json from network`

**Root cause:**

The method `loadJsonFromNetwork()` is called which shuffles a list of three URLs. One of those
URLs points to a (local) IP address which should be dead for most environments. Now there is a
for-loop which returns the first URL that works or throws an error if none of them works. In the
case of the dead URL being the first one, there will be a timeout after 3000ms (defined in the
default URL fetcher). However, the test itself sets a timeout of only 300ms. Even if the next URL
would work the whole tests fails now because trying to connect to the dead one already taks more
than 300ms.

**Fix:**

I removed the shuffling from within `loadJsonFromNetwork()` to ensure a fixed order. This keeps the
test stable. One could also argue, that local endpoints like the IP address should not be used
within a test, but I kept it for now.

## Flaky Test 3

**Test name:** `de.seuhd.worldcup.WorldCupTest#standings are stable when multiple teams tie on all criteria`

**Root cause:**

The test expects the exact order of "Alpha", "Beta", "Gamma", however the `calculate()` function
only sorts by points, then by goal difference and lastly by goals scored (goalsFor). If all these
values are the same for the teams, the order is defined by the order of the hashmap. The official
documentation (https://developer.android.com/reference/java/util/IdentityHashMap) states that there
is not specific order guaranteed which is why some test runs fail and some succeed.

**Fix:**

I added another sorting criteria at the very end of the `calculate()` function. Now it is lastly
sorted by the team name, which then matches the expeced output of "Alpha", "Beta", "Gamma".

## Flaky Test 4

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#fresh service has no bets`

**Root cause:**

Similar to flaky test 1:

Within the test a new `FileBettingService` object is instantiated using a `SHARED_BET_FILE` which
is in a temporary directory of the JVM. The comment above the variable also tells us, that the
filename is only unique per JVM launch. One run of all tests happens in one JVM launch so if the
passed file already exists and has beed modified, there will be more than zero bets stored. There
are more tests like `save bets to the shared file()` for example which stores bets in that exact
file. Now the class annotation `@TestMethodOrder(MethodOrderer.Random::class)` tells us that  the order
is actually pseudo-random which means that sometimes the `save bets to the shared file()` test is
called before the `fresh service has no bets()` test and thus the file already contains bets.

**Fix:**

I added a `BeforeTest` function called `delete shared test file()` which delete the
`SHARED_BET_FILE` if it exists. Now each test operates on a fresh file and is not influenced by any
other test.

## Flaky Test 5

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#test file betting with threads`

**Root cause:**

The issue is a very typical race condition because of multiple threads. There are two threads
opened and started which call the function `placeBet()` each 50 times. Each call tries to read the
temporary betting file, stores it in a hashmap, adds the new bet and writes the hashmap back to the
betting file. Now it is possible that one thread is faster than the other which leads to less
stored bets in the betting file since the slower threads overwrites the whole file with less bets
in total in its current hashmap. If that happens at least once, there aren't 100 bets at the
execution end of both threads and the assertion fails.

**Fix:**

I added a simple lock to the `FileBettingService` so that only one thread at a time can access the
file effectively. If there is already a thread calling `placeBet()` other threads have to wait
until that lock has been removed. It's fair to say that I used OpenCode and Codex to generate that
code modification since I didn't know how to implement mutexes in Kotlin.
