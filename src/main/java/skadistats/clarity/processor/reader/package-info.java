/**
 * Raw message and tick events.
 *
 * <p>{@link skadistats.clarity.processor.reader.OnMessage} delivers decoded protobuf messages, optionally filtered
 * by message class; {@link skadistats.clarity.processor.reader.OnMessageContainer} delivers the message class and
 * the still serialized bytes; {@link skadistats.clarity.processor.reader.OnPostEmbeddedMessage} fires for messages
 * unpacked from a parent message. {@link skadistats.clarity.processor.reader.OnFullPacket} fires for full packets.
 * Top-level messages nobody listens to are skipped without parsing. These events require a
 * {@link skadistats.clarity.processor.runner.FileRunner}.
 *
 * <p>{@link skadistats.clarity.processor.reader.OnTickStart} and {@link skadistats.clarity.processor.reader.OnTickEnd}
 * mark tick boundaries; their {@code synthetic} flag is {@code true} for ticks without replay data.
 * {@link skadistats.clarity.processor.reader.OnReset} reports the {@link skadistats.clarity.processor.reader.ResetPhase}s
 * of a reset, which happens when a runner seeks. {@code InputSourceProcessor} and
 * {@link skadistats.clarity.processor.reader.PacketInstance} are internals.
 *
 * <pre>{@code
 * @OnMessage(CommonNetMessages.CSVCMsg_PacketEntities.class)
 * public void onPacketEntities(Context ctx, CommonNetMessages.CSVCMsg_PacketEntities msg) { ... }
 * }</pre>
 */
package skadistats.clarity.processor.reader;
