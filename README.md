# Clarity

Clarity is an open-source parser for Dota 2, CSGO and CS2 replay files, written in Java.

## Changelog
see the ![Changelog](/CHANGELOG.md) for recent project activity.

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

* Java (17 and above)
* Gradle (for building)

# Usage

Depending on your project build, use one of the following

### Maven
```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>3.0.0</version>
</dependency>
```

### Gradle (Groovy)
```
    implementation group: 'com.skadistats', name: 'clarity', version: '3.0.0'
```

### Gradle (Kotlin)
```
    implementation("com.skadistats:clarity:3.0.0")
```

# Example Code

For example code, please see the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See ![LICENSE](/LICENSE) in the project root.
