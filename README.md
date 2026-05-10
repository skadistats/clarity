# Clarity

Clarity is an open-source parser for Dota 2, CSGO, CS2 and Deadlock replay files, written in Java.

## Changelog
see the [Changelog](/CHANGELOG.md) for recent project activity.

# Replay Data

Clarity produces the following data you might be interested in from a replay. Choose from:

* **combat log**: a detailed log of events that happened in the game
* **entities**: in-game things like heroes, players, and creeps
* **modifiers**: auras and effects on in-game entities
* **temporary entities**: fire-and-forget things the game server tells the client about*
* **user messages**: many different things, including spectator clicks, global chat messages, overhead events (like last-hit gold, and much more), particle systems, etc.*
* **game events**: lower-level messages like Dota TV control (directed camera commands, for example), etc.*
* **voice data**: commentary in pro matches*
* **sounds**: sounds that occur in the game*
* **overview**: end-of-game summary, including players, game winner, match id, duration, and often picks/bans
* **unprocessed**: data is provided as original protobuf message object

# Requirements

* Java (21 and above)
* Gradle (for building)

# Usage

Depending on your project build, use one of the following.

> **Note:** the `next` branch tracks Clarity **5.0.0-SNAPSHOT**, the
> in-development line for the upcoming 5.0 release. Snapshot artifacts
> are published to Maven Central's snapshots repository
> (`https://central.sonatype.com/repository/maven-snapshots/`); you'll
> need to add it to your build to consume them. The latest stable
> release line on `master` is the 4.x series — see the tagged commits
> for those coordinates.

### Maven
```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>5.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle (Groovy)
```
    implementation group: 'com.skadistats', name: 'clarity', version: '5.0.0-SNAPSHOT'
```

### Gradle (Kotlin)
```
    implementation("com.skadistats:clarity:5.0.0-SNAPSHOT")
```

# Example Code

For example code, please see the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See [LICENSE](/LICENSE) in the project root.
