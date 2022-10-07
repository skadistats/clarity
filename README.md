# Clarity 2

Clarity is a parser for Dota 2 and CSGO replay files, written in Java.

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

# Replay Data

clarity produces the following data you might be interested in from a replay. Choose from:

* **combat log**: a detailed log of events that happened in the game
* **entities**: in-game things like heroes, players, and creeps
* **modifiers**: auras and effects on in-game entities
* **temporary entities**: fire-and-forget things the game server tells the client about*
* **user messages**: many different things, including spectator clicks, global chat messages, overhead events (like last-hit gold, and much more), particle systems, etc.*
* **game events**: lower-level messages like Dota TV control (directed camera commands, for example), etc.*
* **voice data**: commentary in pro matches*
* **sounds**: sounds that occur in the game*
* **overview**: end-of-game summary, including players, game winner, match id, duration, and often picks/bans

\* **unprocessed**: data is provided as original protobuf message object

# Requirements

* Java (minimum version 8)
* Maven

# Usage

Fetch the current stable version (2.7.5) from Maven Central with

```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>2.7.5</version>
</dependency>
```

# Example Code

For example code, please see the the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See LICENSE in the project root.
