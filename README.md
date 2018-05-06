# Clarity 2

Clarity is a parser for Dota 2 and CSGO replay files written in Java.

# CSGO support

Clarity supports reading CSGO replays as of today (May 6, 2018, 2.3-SNAPSHOT only). There should be no breakage with existing code, 
but if you find some, please open a bug.

# Version 2.2 released

Because of a change of string decoding in clarity (2.2 wrongly decoded strings as ISO, while they should be
decoded as UTF-8), I released 2.2 today (March 5, 2018). Changing to 2.3 will require all client code to remove
whatever hack they did to properly convert clarity's output to UTF-8.

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

* Java 7 or newer
* Maven

# Usage

Fetch the current stable version (2.2) from Maven Central with
```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>2.2</version>
</dependency>
```

Clarity 2.3 is work in progress and only available as a snapshot, so you got to add a pointer to the
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
	<version>2.3-SNAPSHOT</version>
</dependency>
```

# Example Code

For example code, please see the the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See LICENSE in the project root.
