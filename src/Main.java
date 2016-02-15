public class Main {

	public static void main(String[] args){
		Frontier.addURL("http://ciir.cs.umass.edu/");
		for(int i = 0; i < 10; i++){
			Thread t = new Thread(new Crawler(), Integer.toString(i));
			t.start();
		}
	}

}
