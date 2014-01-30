**WIP: This entire document is very new. Please submit corrections!**

# clarity

Comically fast, almost complete Dota 2 "demo" (aka "replay") parser written in java. 

On a fast CPU (if you consider a i5-3570k fast), clarity parses a complete replay of an hour in less than 6 seconds.

# Installation

You should use the latest 1.7 JVM to run it.
While there is nothing in the code preventing you from using 1.6.  

Maven is used as a build system. clarity has not been uploaded to Maven Central yet, so for now you have to build it yourself.

To build it, invoke maven at the project root:

    mvn package

This will build a JAR in the target folder, which you can run with something like

    java -jar target/clarity-0.1-SNAPSHOT.jar replay.dem

I recommend using a decent IDE though, were you import the project and start playing from there.

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
duration, and often picks/bans (not implemented)

\* **transient**: new dataset (i.e. List or Map) for each tick of the parse

✝ **unprocessed**: data is provided as original protobuf message object

# Parsing Replay Data

See `test/Test.java` for a rough example on how to invoke it.

# License

See LICENSE in the project root. The license for this project is a modified
MIT with an additional clause requiring specifically worded hyperlink
attribution in web properties using smoke.
