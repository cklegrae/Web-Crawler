import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Website {

	ArrayList<String> urls;
	ArrayList<String> disallowed;
	ArrayList<String> allowed;
	String domainName;
	
	public Website(String domainName){
		urls = new ArrayList<String>();
		disallowed = new ArrayList<String>();
		allowed = new ArrayList<String>();
		this.domainName = domainName;
	}
	
	public void addURL(String url){
		urls.add(url);
	}
	
	// Make sure we're being polite, avoiding sites that forbid us from looking at robots.txt.
	public boolean addCrawlRules(){
		String url = "http://" + domainName + "/robots.txt";
		try(BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
	        String line;
	        // User-agent: * triggers the isGenericAgent boolean.
	        boolean isGenericAgent = false;
	        while((line = in.readLine()) != null) {
	        	// We only want to look at settings applying to *, rather than taking all of the rules on the page.
	        	if(line.startsWith("User-agent:")){
	        		if(line.contains("*")){
	        			isGenericAgent = true;
	        		}
	        		else
	        			isGenericAgent = false;
	        	}
	        	
	        	// Ignore line if it's not referring to us.
	        	if(isGenericAgent == false)
	        		continue;
	        	
	            if(line.startsWith("Allow: ")){
	            	allowed.add(line.substring(7));
	            }
	            
	            else if(line.startsWith("Disallow: ")){
	            	if(line.equals("Disallow :"))
	            		disallowed.add("");
	            	else
	            		disallowed.add(line.substring(10));
	            }
	        }
	    } catch (IOException e) {
	    	// If we're forbidden from looking at the robots.txt, return false: we're likely to be forbidden from crawling the site altogether.
	    	if(e.getMessage().contains("403")){
	    			return false;
	    	}
	    }
		
		return true;
	}
	
	// Check if the site allows this page to be crawled.
	public boolean permitsCrawl(String url){
		if(url.isEmpty())
			return false;
		
		// Set URL to be the resources after the domain, e.g. /index.html.
		String domainNameTruncated = domainName.replace("www.", "");
		url = url.substring(url.indexOf(domainNameTruncated.replaceFirst("www.", "")) + domainNameTruncated.replaceFirst("www.", "").length());
		
		// If the url matches a disallowed rule, return false if and only if there does not exist an allowed rule validating this url.
		// Pattern.quote() avoids dangling meta characters.
		for(String disallowedRule : disallowed){
			if(url.matches(".*" + Pattern.quote(disallowedRule) + ".*")){
				for(String allowedRule : allowed){
					if(url.matches(".*" + Pattern.quote(allowedRule) + ".*")){
						return true;
					}
				}
				return false;
			}
		}
		
		return true;
	}
	
	public boolean hasNext(){
		return !urls.isEmpty();
	}
	
	// nextURL returns the oldest URL in the queue.
	public String nextURL(){
		if(urls.isEmpty())
			return "";
		return urls.remove(0);
	}
	
	public String getDomain(){
		return domainName;
	}
	
	public void setDomain(String domain){
		domainName = domain;
	}
	
}
