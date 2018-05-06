package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.CsGoFieldReader;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnPostEmbeddedMessage;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.source.ResetRelevantKind;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.csgo.EmbeddedPackets;
import skadistats.clarity.wire.csgo.UserMessagePackets;
import skadistats.clarity.wire.csgo.proto.CsGoClarityMessages;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

import java.io.IOException;

public class CsGoEngineType extends AbstractEngineType {

    @Insert
    private Context ctx;
    private CsGoClarityMessages.CsGoDemoHeader header;
    private float millisPerTick;

    public CsGoEngineType(EngineId identifier) {
        super(identifier,
                true,   // CDemoSendTables is container
                11, 10
        );
    }

    @Override
    public float getMillisPerTick() {
        return millisPerTick;
    }

    @Override
    public boolean isFullPacketSeekAllowed() {
        return false;
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

    private String readHeaderString(Source source) throws IOException {
        return Util.readFixedZeroTerminated(source, 260);
    }

    @Override
    public void readHeader(Source source) throws IOException {
        header = CsGoClarityMessages.CsGoDemoHeader.newBuilder()
                .setDemoprotocol(source.readFixedInt32())
                .setNetworkprotocol(source.readFixedInt32())
                .setServername(readHeaderString(source))
                .setClientname(readHeaderString(source))
                .setMapname(readHeaderString(source))
                .setGamedirectory(readHeaderString(source))
                .setPlaybackTime(Float.intBitsToFloat(source.readFixedInt32()))
                .setPlaybackTicks(source.readFixedInt32())
                .setPlaybackFrames(source.readFixedInt32())
                .setSignonlength(source.readFixedInt32())
                .build();
        millisPerTick = header.getPlaybackTime() * 1000.0f / header.getPlaybackTicks();
    }

    @Override
    public void skipHeader(Source source) throws IOException {
        source.skipBytes(1064);
    }

    @Override
    public void emitHeader() {
        ctx.createEvent(OnMessage.class, CsGoClarityMessages.CsGoDemoHeader.class).raise(header);
    }

    @Override
    public int determineLastTick(Source source) throws IOException {
        return header.getPlaybackTicks();
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
            public ResetRelevantKind getResetRelevantKind() {
                return handler.resetRelevantKind;
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
                b.addClassesBuilder()
                        .setClassId(bs.readSBitInt(16))
                        .setNetworkName(bs.readString(255))
                        .setTableName(bs.readString(255));
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
        private final ResetRelevantKind resetRelevantKind;
        protected Handler(Class<T> clazz, ResetRelevantKind resetRelevantKind) {
            this.clazz = clazz;
            this.resetRelevantKind = resetRelevantKind;
        }
        public abstract T parse(Source source) throws IOException;
        public abstract void skip(Source source) throws IOException;
    }

    private final Handler<?>[] handlers = {
            //(1) dem_signon (will be set below)
            null,
            //(2) dem_packet
            new Handler<Demo.CDemoPacket>(Demo.CDemoPacket.class, null) {
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
                    source.skipBytes(8);
                    skipPacket(source);
                }
            },
            //(3) dem_synctick
            new Handler<Demo.CDemoSyncTick>(Demo.CDemoSyncTick.class, ResetRelevantKind.SYNC) {
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
            new Handler<Demo.CDemoConsoleCmd>(Demo.CDemoConsoleCmd.class, null) {
                @Override
                public Demo.CDemoConsoleCmd parse(Source source) throws IOException {
                    return Demo.CDemoConsoleCmd.newBuilder()
                            .setCmdstringBytes(ZeroCopy.wrap(readPacket(source)))
                            .build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    skipPacket(source);
                }
            },
            //(5) dem_usercmd
            new Handler<Demo.CDemoUserCmd>(Demo.CDemoUserCmd.class, null) {
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
            new Handler<Demo.CDemoSendTables>(Demo.CDemoSendTables.class, null) {
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
            new Handler<Demo.CDemoStop>(Demo.CDemoStop.class, null) {
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
            new Handler<Demo.CDemoCustomData>(Demo.CDemoCustomData.class, null) {
                @Override
                public Demo.CDemoCustomData parse(Source source) throws IOException {
                    return Demo.CDemoCustomData.newBuilder()
                            .setData(ZeroCopy.wrap(readPacket(source)))
                            .build();
                }
                @Override
                public void skip(Source source) throws IOException {
                    skipPacket(source);
                }
            },
            //(9) dem_stringtables
            new Handler<Demo.CDemoStringTables>(Demo.CDemoStringTables.class, ResetRelevantKind.STRINGTABLE) {
                @Override
                public Demo.CDemoStringTables parse(Source source) throws IOException {
                    BitStream bs = BitStream.createBitStream(ZeroCopy.wrap(readPacket(source)));
                    Demo.CDemoStringTables.Builder b = Demo.CDemoStringTables.newBuilder();
                    int nTables = bs.readUBitInt(8);
                    for (int t = 0; t < nTables; t++) {
                        Demo.CDemoStringTables.table_t.Builder tb = b.addTablesBuilder();
                        tb.setTableName(bs.readString(4095));
                        int nStrings = bs.readUBitInt(16);
                        for (int s = 0; s < nStrings; s++) {
                            Demo.CDemoStringTables.items_t.Builder ib = tb.addItemsBuilder();
                            ib.setStr(bs.readString(255));
                            if (bs.readBitFlag()) {
                                byte[] data = new byte[bs.readUBitInt(16)];
                                bs.readBitsIntoByteArray(data, data.length * 8);
                                ib.setData(ZeroCopy.wrap(data));
                            }
                        }
                        if (bs.readBitFlag()) {
                            Demo.CDemoStringTables.items_t.Builder ib = tb.addItemsClientsideBuilder();
                            ib.setStr(bs.readString(4095));
                            if (bs.readBitFlag()) {
                                byte[] data = new byte[bs.readUBitInt(16)];
                                bs.readBitsIntoByteArray(data, data.length * 8);
                                ib.setData(ZeroCopy.wrap(data));
                            }

                        }
                    }
                    return b.build();
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
