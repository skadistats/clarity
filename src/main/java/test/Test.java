package test;

import clarity.iterator.BidiIterator;
import clarity.match.Match;
import clarity.parser.Peek;
import clarity.parser.ReplayFile;
import clarity.parser.ReplayIndex;
import clarity.parser.handler.HandlerRegistry;

public class Test {

	public static void main(String[] args) throws Exception {

		try {
			long tStart = System.currentTimeMillis();

			ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem");
			Match match = new Match();
			
			for (BidiIterator<Peek> i = idx.prologueIterator(); i.hasNext();) {
				Peek p = i.next();
				HandlerRegistry.apply(p.getMessage(), match);
			}

			System.out.println("--------------------------------------------------------------------------");
			
			for (BidiIterator<Peek> i = idx.matchIterator(); i.hasNext();) {
				Peek p = i.next();
				HandlerRegistry.apply(p.getMessage(), match);
			}
			
			long tEnd = System.currentTimeMillis();
			System.out.println((double)(tEnd - tStart) / 1000.0 + "s");
			
		} catch (Throwable e) {
			System.out.flush();
			throw e;
		}
		 
	}
	

}
