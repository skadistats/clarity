package test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import clarity.match.Match;
import clarity.model.ReceiveProp;
import clarity.model.SendTable;
import clarity.model.SendTableFlattener;
import clarity.parser.Peek;
import clarity.parser.ReplayFile;

import com.dota2.proto.Demo.CDemoSyncTick;

public class Test {

	public static void main(String[] args) throws IOException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		try {
			long tStart = System.currentTimeMillis();
			
			Iterator<Peek> iter =
				new ReplayFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem")
					.iterator();
	
			Match match = new Match();
			while (iter.hasNext()) {
				Peek p = iter.next();
				p.getPacketHandler().apply(p.getMessage(), match);
				if (p.getMessage() instanceof CDemoSyncTick) {
					for (Map.Entry<String, SendTable> e : match.getSendTableByDT().entrySet()) {
						String dt = e.getKey();
						SendTable sendTable = e.getValue();
						//System.out.println();
						//System.out.println("------------------------------------- processing send table " + sendTable.getMessage().getNetTableName());
						if (!sendTable.getMessage().getNeedsDecoder()) {
							continue;
						}
						Integer cls = match.getClassByDT().get(dt);
						List<ReceiveProp> rps = new SendTableFlattener(match.getSendTableByDT(), sendTable).flatten();
						match.getReceivePropsByClass().put(cls, rps);
					}
					
//					for (Map.Entry<Integer, List<ReceiveProp>> e : match.getReceivePropsByClass().entrySet()) {
//						System.out.println("\n\nreceive props for class " + getKeyByValue(match.getClassByDT(), e.getKey()) + " (" + e.getKey() + ")");
//						for (ReceiveProp rp : e.getValue()) {
//							System.out.println(rp);
//						}
//					}
					
				}
			}
			
			
			long tEnd = System.currentTimeMillis();
			System.out.println((double)(tEnd - tStart) / 1000.0 + "s");
			
		} catch (Throwable e) {
			System.out.flush();
			throw e;
		}
		 
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Map.Entry<T, E> entry : map.entrySet()) {
	        if (value.equals(entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}	

}
