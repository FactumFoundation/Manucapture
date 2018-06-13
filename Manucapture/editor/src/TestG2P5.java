
public class TestG2P5 {

	
	public static void main(String[] args) {
		G2P5 g2p5 = G2P5.create("/home/dudito/.manuscript", "", "A");
		g2p5.active = true;
		
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
