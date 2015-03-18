package skadistats.clarity.decoder;

import skadistats.clarity.decoder.prop.PropDecoder;
import skadistats.clarity.model.ReceiveProp;

import java.util.List;

public class BaseInstanceDecoder {

    public static Object[] decode(byte[] data, List<ReceiveProp> receiveProps) {
        BitStream stream = new BitStream(data);
        List<Integer> propList = stream.readEntityPropList();

        Object[] state = new Object[receiveProps.size()];
        for (int i : propList) {
            ReceiveProp p = receiveProps.get(i);
            PropDecoder<?> d = p.getType().getDecoder();
            Object value = d.decode(stream, p);
            state[i] = value;
        }

        return state;
    }

}
