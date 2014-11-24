# clarity

Comically fast, almost complete Dota 2 "demo" (aka "replay") parser written in java. 

# Replay Data

clarity produces the following data you might be interested in from a replay. Choose from:

* **entities**: in-game things like heroes, players, and creeps
* **modifiers**: auras and effects on in-game entities✝
* **"temp" entities**: fire-and-forget things the game server tells the
client about*
* **user messages**: many different things, including spectator clicks, global
chat messages, overhead events (like last-hit gold, and much more), particle systems, etc.*✝
* **game events**: lower-level messages like Dota TV control (directed camera
commands, for example), combat log messages, etc.*
* **voice data**: the protobuf-formatted binary data blobs that are somehow
strung into voice--only really relevant to commentated pro matches*✝
* **sounds**: sounds that occur in the game*✝
* **overview**: end-of-game summary, including players, game winner, match id,
duration, and often picks/bans

\* **transient**: new dataset (i.e. List or Map) for each tick of the parse

✝ **unprocessed**: data is provided as original protobuf message object

# Processing speed

The measurements here were done under Windows 7 64bit, on a slighty overclocked i5-3570k, 
the replay data was stored on a Samsung SSD 830, JDK 1.7.0_45.

The replay tested was game 5 of the grand final of TI3 (match id #271145478), which weighs 
around 127MB. 

How fast you get to your results will depend on the amount of data you need. If you choose
to only retrieve a subset of all the data, it will get quicker:

| data retrieved       | time taken |
| -------------------- | ---------- |
| full parse           | 5.12s      |
| entities + modifiers | 3.29s      |
| voice data only      | 1.63 s     |
| game events only     | 1.64 s     |

# Memory consumption

There are two modes you can run clarity in. The first one is a streaming type of interface,
with which you will see each data object once, and will not be able to seek on replay data.
This saves memory, since only part of the processed replay data is in memory.

I've been able to run this with a maximum memory setting of 20MB (-Xmx20m),
although giving it a little bit more will not hurt (garbage collection will run less often).

Another possibility is to read the complete replay onto an index, which will cost significantly more 
memory, but will allow you to seek on the replay data easily.

# Usage

Clarity is compiled for a 1.7 JVM. It is available via Maven Central. 
To use the stable version, add the following dependency in your pom.xml, and you should be good to go:

```XML
<dependency>
	<groupId>com.skadistats</groupId>
	<artifactId>clarity</artifactId>
	<version>1.0</version>
</dependency>
```
Warning: Stable version is out of date, and will not be able to parse new replays. Use the newest snapshot instead! Stable will be updated before the century ends, I promise ;-)

There is also the possibility to use the bleeding edge, which I try to keep in sync with the Git-Repo.
However, this latest snapshot is not available in Maven Central, so you got to add a pointer to the 
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
	<version>1.1-SNAPSHOT</version>
</dependency>
```

# Example Code

For example code, please see the the separate project [clarity-examples](https://github.com/skadistats/clarity-examples).

# License

See LICENSE in the project root. The license for this project is a modified
MIT with an additional clause requiring specifically worded hyperlink
attribution in web properties using clarity.
