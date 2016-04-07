import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Frontier {

	private static LinkedBlockingQueue<Website> mainQueue = new LinkedBlockingQueue<Website>();
	private static LinkedBlockingQueue<Website> waitQueue = new LinkedBlockingQueue<Website>();

	private static String[] uniqueLinks = new String[2000];
	private static int uniqueCount = 0;
	private static int maxLinks = 1000;
	private static ArrayList<String> visitedLinks = new ArrayList<String>();
	
	private static HashMap<Integer, Integer> visitedVsFrontier = new HashMap<Integer, Integer>();
		
	// Returns true if the thread is no longer needed.
	public static boolean done(){
		if(uniqueCount > maxLinks){
			printStrings();
			System.exit(0);
		}
		
		if(mainQueue.isEmpty() && waitQueue.isEmpty())
			return true;
		
		return false;
	}
	
	// Grabs the next website (which has at least one webpage) from the main queue for use by a crawler thread.
	public static Website nextSite(){
		if(mainQueue.isEmpty())
			return null;
		
		try {
			Website poll = mainQueue.poll(1, TimeUnit.SECONDS);
			if(poll != null){
				waitQueue.add(poll);
			}
			return poll;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// Adds URL to the appropriate website, adds new website if none found.
	public static void addURL(String url){
		
		int hash = checkDuplicates(url);
		
		if(hash == -1)
			return;
		
		String urlDomain = getDomain(url);
		
		url = cleanURL(url);
		
		Website[] main = mainQueue.toArray(new Website[mainQueue.size()]);
		Website[] wait = waitQueue.toArray(new Website[waitQueue.size()]);

		for(int i = 0; i < Math.max(main.length, wait.length); i++){
			Website web;
			if(main.length - 1 > i && main[i].getDomain().equals(urlDomain))
				web = main[i];
			else if(wait.length - 1 > i && wait[i].getDomain().equals(urlDomain))
				web = wait[i];
			else
				continue;
			
			web.addURL(url);
			uniqueLinks[hash] = url;
			uniqueCount++;
			return;
		}
				
		Website website = new Website(urlDomain);
		website.addURL(url);
		
		// Website must allow crawling to be valid.
		if(website.addCrawlRules()){
			mainQueue.add(website);
			uniqueLinks[hash] = url;
			uniqueCount++;
		}			
	}
	
	// Hashes URL into uniqueLinks array.
	private static int checkDuplicates(String url){
		url = cleanURL(url);
		
		char[] chars = url.toCharArray();
		int index = 0;
		
		for(int i = 0; i < chars.length; i++){
			index += (((int) chars[i]) * Math.pow(17, i)) % uniqueLinks.length;
		}
		
		index %= uniqueLinks.length;
		
		while(uniqueLinks[index] != null){
			if(url.equals(uniqueLinks[index])){
				return -1;
			}
			index++;
			if(index >= uniqueLinks.length)
				index = 0;
		}
		
		return index;
	}
	
	// Sleep on website to avoid breaking politeness.
	public static void releaseSite(Website website){
		try {
			Thread.sleep(5000);
			if(website.hasNext()){
				mainQueue.add(website);
			}
			waitQueue.remove(website);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// Get URL host.
	private static String getDomain(String url){
		try {
			return new URI(url).getHost();
		} catch (URISyntaxException e) {
			return url;
		}
	}
	
	private static String cleanURL(String url){
		if(url.contains("http"))
			url = url.substring(url.indexOf("//") + 2);
		if(url.contains("www."))
			url = url.substring(url.indexOf("www.") + 4);
		return url;
	}
	
	// Add URL to visited list for analysis purposes.
	public static void setLinkAsVisited(String url){
		visitedLinks.add(url);
		visitedVsFrontier.put(visitedLinks.size(), uniqueCount + visitedLinks.size());
	}
	
	// Print relevant results at the end.
	private static void printStrings(){
		for(int i = 1; i <= visitedVsFrontier.size(); i++){
			// [Document Processed #] [Number of links seen at that point in time]
			System.out.println(i + " " + visitedVsFrontier.get(i));
		}
	}
	
}
