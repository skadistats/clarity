package clarity.decoder;

import java.util.List;

import clarity.decoder.prop.PropDecoder;
import clarity.model.ReceiveProp;

public class BaseInstanceDecoder {

    public static Object[] decode(byte[] data, List<ReceiveProp> receiveProps) {
        EntityBitStream stream = new EntityBitStream(data);
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
