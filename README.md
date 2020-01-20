# Clarity 2

Clarity is a parser for Dota 2 and CSGO replay files written in Java.

# Caution - new feature

Today (July 18, 2019) I merged work on a big new feature, which aims at reproducing entity data
more accurately. While making parsing entities quite a bit slower, the data it produces is much
more accurate, and the related events (@OnEntity{Created,Updated,Deleted}) contain less duplicates. 

This also fixes longstanding issues, like entities with the same handle getting instantiated by the parser
multiple times, preventing client code to reliably hold a reference to an entity.

It has already been battle tested with some closed source parsers, but you might still
find problems - so use 2.5-SNAPSHOT with caution, and report bugs!

# Version 2.4.1 released

The fall update of Dota 2 contains a bugfix that are needed for replays from that new patch. If you get 
an exception saying that `highLowMultiplier is zero`, be sure to use the fixed version!

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

* Java 7 or 8
* Maven

# Usage

Fetch the current stable version (2.4) from Maven Central with
```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>2.4</version>
</dependency>
```

Clarity 2.5 is work in progress and only available as a snapshot, so you got to add a pointer to the
repository to your pom.xml (see the [pom.xml of clarity-examples](https://github.com/skadistats/clarity-examples/blob/master/pom.xml), which already does that)

To add the snapshot repository, add the following:
```XML
<repositories>
	<repository>
		<id>sonatype.oss.snapshots</id>
		<name>Sonatype OSS Snapshot Repository</name>
		<url>http://oss.sonatype.org/content/repositories/snapshots</url>
		<releases>
			<enabled>false</enabled>
		</releases>
		<snapshots>
			<enabled>true</enabled>
		</snapshots>
	</repository>
</repositories>
```

and then fetch the dependency with:
```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>2.5-SNAPSHOT</version>
</dependency>
```

# Example Code

For example code, please see the the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See LICENSE in the project root.
