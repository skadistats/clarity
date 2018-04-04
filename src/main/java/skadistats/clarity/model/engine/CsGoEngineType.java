package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.CsGoFieldReader;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.DemoHeader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnPostEmbeddedMessage;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.csgo.EmbeddedPackets;
import skadistats.clarity.wire.csgo.UserMessagePackets;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

import java.io.IOException;

public class CsGoEngineType extends AbstractEngineType {

    @Insert
    private Context ctx;

    public CsGoEngineType(EngineId identifier) {
        super(identifier,
                true,   // CDemoSendTables is container
                11, 10
        );
    }

    @Override
    public boolean handleDeletions() {
        return false;
    }

    @Override
    public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
        return EmbeddedPackets.classForKind(kind);
    }

    @Override
    public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
        return UserMessagePackets.classForKind(kind);
    }

    @Override
    public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
        return UserMessagePackets.isKnownClass(clazz);
    }

    @Override
    public FieldReader getNewFieldReader() {
        return new CsGoFieldReader();
    }

    @Override
    public DemoHeader readHeader(Source source) throws IOException {
//            int32	demoprotocol;					// Should be DEMO_PROTOCOL
        source.skipBytes(4);
//            int32	networkprotocol;				// Should be PROTOCOL_VERSION
        source.skipBytes(4);
//            char	servername[ MAX_OSPATH ];		// Name of server
        source.skipBytes(260);
//            char	clientname[ MAX_OSPATH ];		// Name of client who recorded the game
        source.skipBytes(260);
//            char	mapname[ MAX_OSPATH ];			// Name of map
        source.skipBytes(260);
//            char	gamedirectory[ MAX_OSPATH ];	// Name of game directory (com_gamedir)
        source.skipBytes(260);
//            float	playback_time;					// Time of track
        source.skipBytes(4);
//            int32   playback_ticks;					// # of ticks in track
        source.skipBytes(4);
//            int32   playback_frames;				// # of frames in track
        source.skipBytes(4);
//            int32	signonlength;					// length of sigondata in bytes
        source.skipBytes(4);
        return null;
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(final Source source) throws IOException {
        final int kind = source.readByte() & 0xFF;
        final int tick = source.readFixedInt32();
        source.skipBytes(1); // playerSlot
        if (kind > handlers.length) {
            throw new UnsupportedOperationException("kind " + kind + " not implemented");
        }
        final Handler<T> handler = (Handler<T>) handlers[kind - 1];
        return new PacketInstance<T>() {
            @Override
            public int getKind() {
                return kind;
            }
            @Override
            public int getTick() {
                return tick;
            }
            @Override
            public Class<T> getMessageClass() {
                return handler.clazz;
            }
            @Override
            public T parse() throws IOException {
                return handler.parse(source);
            }
            @Override
            public void skip() throws IOException {
                handler.skip(source);
            }
        };
    }

    @OnPostEmbeddedMessage(S1NetMessages.CSVCMsg_SendTable.class)
    public void onPostSendTable(S1NetMessages.CSVCMsg_SendTable message, BitStream bs) {
        if (message.getIsEnd()) {
            Demo.CDemoClassInfo.Builder b = Demo.CDemoClassInfo.newBuilder();
            int n = bs.readSBitInt(16);
            for (int i = 0; i < n; i++) {
                Demo.CDemoClassInfo.class_t.Builder cb = Demo.CDemoClassInfo.class_t.newBuilder();
                b.addClasses(cb
                        .setClassId(bs.readSBitInt(16))
                        .setNetworkName(bs.readString(255))
                        .setTableName(bs.readString(255))
                        .build()
                );
            }
            ctx.createEvent(OnMessage.class, Demo.CDemoClassInfo.class).raise(b.build());
        }
    }



    private static final int dem_signon       = 1; // it's a startup message, process as fast as possible
    private static final int dem_packet       = 2; // it's a normal network packet that we stored off
    private static final int dem_synctick     = 3; // sync client clock to demo tick
    private static final int dem_consolecmd   = 4; // console command
    private static final int dem_usercmd      = 5; // user input command
    private static final int dem_datatables   = 6; // network data tables
    private static final int dem_stop         = 7; // end of time
    private static final int dem_customdata   = 8; // a blob of binary data understood by a callback function
    private static final int dem_stringtables = 9; // alternative to 6? Similar to dota?



    private byte[] readPacket(Source source) throws IOException {
        int size = source.readFixedInt32();
        return packetReader.readFromSource(source, size, false);
    }

    private void skipPacket(Source source) throws IOException {
        source.skipBytes(source.readFixedInt32());
    }

    public void readCommandInfo(Source source) throws IOException {
        source.skipBytes(152);
    }

    public void skipCommandInfo(Source source) throws IOException {
        source.skipBytes(152);
    }

    private abstract static class Handler<T extends GeneratedMessage> {
        private final Class<T> clazz;
        protected Handler(Class<T> clazz) {
            this.clazz = clazz;
        }
        public abstract T parse(Source source) throws IOException;
        public abstract void skip(Source source) throws IOException;
    }

    private final Handler<?>[] handlers = {
            //(1) dem_signon (will be set below)
            null,
            //(2) dem_packet
            new Handler<Demo.CDemoPacket>(Demo.CDemoPacket.class) {
                @Override
                public Demo.CDemoPacket parse(Source source) throws IOException {
                    readCommandInfo(source);
                    return Demo.CDemoPacket.newBuilder()
                            .setSequenceIn(source.readFixedInt32())
                            .setSequenceOutAck(source.readFixedInt32())
                            .setData(ZeroCopy.wrap(readPacket(source)))
                            .build();
                }

                @Override
                public void skip(Source source) throws IOException {
                    skipCommandInfo(source);
                    skipPacket(source);
                }
            },
            //(3) dem_synctick
            new Handler<Demo.CDemoSyncTick>(Demo.CDemoSyncTick.class) {
                @Override
                public Demo.CDemoSyncTick parse(Source source) throws IOException {
                    return Demo.CDemoSyncTick.newBuilder().build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    // do nothing
                }
            },
            //(4) dem_consolecmd
            new Handler<Demo.CDemoConsoleCmd>(Demo.CDemoConsoleCmd.class) {
                @Override
                public Demo.CDemoConsoleCmd parse(Source source) throws IOException {
                    throw new UnsupportedOperationException("implement me");
                }
                @Override
                public void skip(Source source) throws IOException {
                    throw new UnsupportedOperationException("implement me");
                }
            },
            //(5) dem_usercmd
            new Handler<Demo.CDemoUserCmd>(Demo.CDemoUserCmd.class) {
                @Override
                public Demo.CDemoUserCmd parse(Source source) throws IOException {
                    return Demo.CDemoUserCmd.newBuilder()
                            .setCmdNumber(source.readFixedInt32())
                            .setData(ZeroCopy.wrap(readPacket(source)))
                            .build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    source.skipBytes(4);
                    skipPacket(source);
                }
            },
            //(6) dem_datatables
            new Handler<Demo.CDemoSendTables>(Demo.CDemoSendTables.class) {
                @Override
                public Demo.CDemoSendTables parse(Source source) throws IOException {
                    return Demo.CDemoSendTables.newBuilder()
                            .setData(ZeroCopy.wrap(readPacket(source)))
                            .build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    skipPacket(source);
                }
            },
            //(7) dem_stop
            new Handler<Demo.CDemoStop>(Demo.CDemoStop.class) {
                @Override
                public Demo.CDemoStop parse(Source source) throws IOException {
                    return Demo.CDemoStop.newBuilder().build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    // do nothing
                }
            },
            //(8) dem_customdata
            new Handler<Demo.CDemoCustomData>(Demo.CDemoCustomData.class) {
                @Override
                public Demo.CDemoCustomData parse(Source source) throws IOException {
                    throw new UnsupportedOperationException("implement me");
                }
                @Override
                public void skip(Source source) throws IOException {
                    throw new UnsupportedOperationException("implement me");
                }
            },
            //(9) dem_stringtables
            new Handler<Demo.CDemoStringTables>(Demo.CDemoStringTables.class) {
                @Override
                public Demo.CDemoStringTables parse(Source source) throws IOException {
                    // TODO implement
                    skipPacket(source);
                    return Demo.CDemoStringTables.newBuilder()
                            .build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    skipPacket(source);
                }
            }
    };

    {
        handlers[0] = handlers[1];
    }

}
