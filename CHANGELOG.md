# Clarity 2 Changelog

## September 14, 2024: Version 3.1.0 released

* add support for Deadlock
* update protobufs

## July 10, 2024: Version 3.0.6 released

* CS2 stopped sending deletions as well
* implemented a fix that should be backwards compatible
  (in replays that still have deletions, they are read)

## May 25, 2024: Version 3.0.5 released

Compatibility with patch 7.36

## April 19, 2024: Version 3.0.4 released

Workaround #311: Deletions are not encoded correctly. Maybe Valve removed them?

## April 06, 2024: Version 3.0.3 released

Fix clarity-examples #60: cannot determine last tick before engine type is known

## Febuary 15, 2024: Version 3.0.2 released

Some additional fixes to support PVS bits.

## Febuary 09, 2024: Version 3.0.1 released

Fix CS2: Arms Race Update

* add support for polymorphic pointers

## November 18, 2023: Version 3.0.0 released

Major release with a lot of new features:

* switched build system to Gradle
* raised minimum required JDK version to 17
* support for parsing CSGO 2 replays
* Protobuf structure improved

## April 21, 2023: Version 2.7.9 released

Compatibility with Dota 7.33 - The New Frontiers Update

## March 08, 2023: Version 2.7.8 released

Fix game events not correctly parsing.

## March 07, 2023: Version 2.7.7 released

Yesterday, Valve released "The Dead Reckoning" update.
This release adds a new "GameTime_t" data type to be able to parse new replays.
The protobufs have also been updated to 4.29.

## January 14, 2023: Version 2.7.6 released

Finally, protobufs have been updated to version 4.28.

You can use the new protobufs with older versions (they will use them automatically), 
however there have been some additions to the combatlog, and if you want to be able 
to access them, you need 2.7.6.

Also contains a performance update for `Util.arrayIdxToString()`, as well as dependency 
updates to bring everything to current versions.

Attention: Some dependencies could not be updated to their newest revisions, because they
rely on a minimum of Java 11. So it's quite possible that I will up the minimum
requirement to Java 11 in the near future as well. If you're still on 8, start working on it!

## October 7, 2022: Version 2.7.5 released

Improve on the incomplete fix from yesterday.

## October 6, 2022: Version 2.7.4 released

Fixes an issue with today's update, which introduced field path length 7.

## August 01, 2022: Version 2.7.3 released

Fixes an issue with console recorded replays on a bad connection, where more than one entity update needs to be deferred.

## July 27, 2022: Version 2.7.2 released

Fixes an issue with CSGO replays where entities with the same handle but different dtClass are created.

## August 4, 2021: Version 2.7.1 released

Fixes an issue where bytecode generated was not executable on an older JVM.

## July 27, 2021: Version 2.7.0 released

Version 2.7.0 brings support for running on JDK > 8.
Tested with 8, 11 and 16.

## June 24, 2021: Version 2.6.2 released

Contains a bugfix for replays with the new Nemestice update, as well as a fix for an NPE when using the LiveSource.
Sorry for the double jump in patch level.

## January 26, 2021: New releases and versioning model update!

Starting today, I will switch to a semantic versioning theme (MAJOR.MINOR.PATCH)

* MAJOR will probably not change in a long time
* MINOR will be increased when there are changes that I believe to be disruptive (and want you to test them first)
* PATCH will be for bugfixes, like we had with the latest 7.28 update.

Today, I made two releases:
* Version 2.5: this is simply the last snapshot as a release
* Version 2.6.0: the first version using the new scheme

There will be no bugfix releases for 2.5, so please migrate your code in a timely manner.

## Changes in 2.6.0

* lots of restructuring / code cleanup regarding the field update parsing code (should not be noticeable)
* package rename `skadistats.clarity.decoder` -> `skadistats.clarity.io` (global search/replace should suffice)
* package rename `skadistats.clarity.io.unpacker` -> `skadistats.clarity.io.decoder` (global search/replace should suffice)
* new event `OnEntityPropertyCountChanged`, which is raised when the amount of properties in an entity changed
* with ControllableRunner (seeking), improved `OnEntityUpdated` to only contain FieldPaths that have been changed
* small performance increase for BitStream
* added proper handling of a special case with Dota 2 console recorded replays, which would throw an exception before
