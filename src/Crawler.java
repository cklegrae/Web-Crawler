import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler implements Runnable{

	HashMap<String, String> documents;
	
	public Crawler(){
		documents = new HashMap<String, String>();
	}
	
	public void run() {
		// While there are websites with pages in the frontier, we crawl the oldest encountered website's oldest webpage.
		while(!Frontier.done()){
			Website website = Frontier.nextSite();
			if(website == null)
				continue;
			String url = website.nextURL();
			if(website.permitsCrawl(url)){
				Document doc = retrieveURL(url);
				if(doc != null){
					storeDocument(url, doc.toString());
					for(String u: parse(doc)){
						// Every parsed URL must be valid, so we add it to the frontier.
						Frontier.addURL(u);
					}
				}
				// Add the link to our visited list and release the website from the frontier.
				Frontier.setLinkAsVisited(url);
				Frontier.releaseSite(website);
			}
		}
	}
	
	// Look through document for valid URLs, and return them in a list.
	public ArrayList<String> parse(Document doc){
		ArrayList<String> list = new ArrayList<String>();
		Elements e = doc.select("a[href]");
		for(Element element : e){
			String url = element.attr("href").replace("..", "");
			// If we find a relative URL, make it absolute.
			if(!url.trim().startsWith("http") && !url.trim().startsWith("www.")){
				try {
					if(url.trim().startsWith("/"))
						url = url.trim().substring(1);
					String baseURI = doc.baseUri().trim();
					if(baseURI.endsWith("/"))
						baseURI.substring(0, baseURI.length() - 2);
					
					url = "http://" + new URI(baseURI).getHost() + "/" + url.trim();
					
					// Check its validity, and add it to the list if so.
					if(isValidURL(url)){
						list.add(url);
					}
				} catch (URISyntaxException e2) {
					e2.printStackTrace();
				}
			}else{
				if(isValidURL(url))
					list.add(url);
			}
		}
		return list;
	}
	
	// Store document with url/body text pair.
	public void storeDocument(String url, String text){
		documents.put(url, text);
	}
	
	// Grab JSoup document from URL.
	public Document retrieveURL(String url){
		Document doc = null;
		try {
			doc = Jsoup.connect("http://" + url).timeout(5000).get();
		} catch (IOException e) {
			return null;
		}
		return doc;
	}
	
	// Check validity of URL.
	public boolean isValidURL(String url){
		try {
			// If we can connect using the URL, we only consider it valid if it's an html or pdf page.
			String contentType = new URL(url).openConnection().getContentType();
			if(contentType == null)
				return false;
			if(contentType.contains("html") || contentType.contains("pdf"))
				return true;
		} catch (IOException e) {
			return false;
		} catch (IllegalArgumentException e2){
			return false;
		}
		return false;
	}
}
