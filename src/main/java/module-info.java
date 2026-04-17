module com.skadistats.clarity {

    requires transitive com.skadistats.clarity.protobuf;
    requires org.slf4j;
    requires snappy.java;
    requires java.compiler;
    requires it.unimi.dsi.fastutil.core;

    exports skadistats.clarity;
    exports skadistats.clarity.event;
    exports skadistats.clarity.io;
    exports skadistats.clarity.io.bitstream;
    exports skadistats.clarity.io.decoder;
    exports skadistats.clarity.io.decoder.factory.s1;
    exports skadistats.clarity.io.decoder.factory.s2;
    exports skadistats.clarity.io.s1;
    exports skadistats.clarity.io.s2;
    exports skadistats.clarity.logger;
    exports skadistats.clarity.model;
    exports skadistats.clarity.model.csgo;
    exports skadistats.clarity.model.s1;
    exports skadistats.clarity.model.s2;
    exports skadistats.clarity.model.s2.field;
    exports skadistats.clarity.engine;
    exports skadistats.clarity.engine.s1;
    exports skadistats.clarity.engine.s2;
    exports skadistats.clarity.state;
    exports skadistats.clarity.state.s1;
    exports skadistats.clarity.state.s2;
    exports skadistats.clarity.platform;
    exports skadistats.clarity.platform.buffer;
    // skadistats.clarity.processor is in the processor source set, exported via JAR bundling
    exports skadistats.clarity.processor.entities;
    exports skadistats.clarity.processor.gameevents;
    exports skadistats.clarity.processor.modifiers;
    exports skadistats.clarity.processor.packet;
    exports skadistats.clarity.processor.reader;
    exports skadistats.clarity.processor.resources;
    exports skadistats.clarity.processor.runner;
    exports skadistats.clarity.processor.sendtables;
    exports skadistats.clarity.processor.stringtables;
    exports skadistats.clarity.processor.tempentities;
    exports skadistats.clarity.source;
    exports skadistats.clarity.util;

}
