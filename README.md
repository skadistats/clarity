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
duration, and often picks/bans (not implemented in clarity yet)

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

See `clarity/examples/simple/Main.java` for an example on this. 

I've been able to run this with a maximum memory setting of 20MB (-Xmx20m),
although giving it a little bit more will not hurt (garbage collection will run less often).

Another possibility is to read the complete replay onto an index, which will cost significantly more 
memory, but will allow you to seek on the replay data easily:

See `clarity/examples/seek/Main.java` for an example of this.

# Installation

You should use the latest 1.7 JVM to run it.
While there is nothing in the code preventing you from using 1.6. So, if you want to run it
in 1.6, adjust the compiler settings in pom.xml and you should be good to go.  

Maven is used as a build system. clarity has not been uploaded to Maven Central yet, 
so for now you have to build it yourself.

To build it, invoke maven at the project root:

    mvn package

This will build a JAR in the target folder, which will only contain clarities code, and
none of its dependencies. To be able to run it, you need to run it with the classpath 
setup correctly.

If you just want to try it out quickly, you can run:

	mvn -P full package
	
This will build a JAR containing clarity with all its dependencies.
This can be run with something like:

    java -cp target/clarity-0.1-SNAPSHOT.jar clarity.examples.Simple.Main replay.dem

I recommend using a decent IDE though, were you import the project and start playing from there.
Clarity should build out of the box in Netbeans, and Eclipse should be equally fine 
(if you have Maven support installed).


# Parsing Replay Data

See `clarity/examples/Simple/Main.java` for a rough example on how to invoke it.

# License

See LICENSE in the project root. The license for this project is a modified
MIT with an additional clause requiring specifically worded hyperlink
attribution in web properties using clarity.
