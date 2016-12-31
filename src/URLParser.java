import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLParser {
	private String schema = "";
	private String authority = "";
	private String path = "";
	private String query = "";
	private String fragment = "";
	private String port = "";
	
	public String getPort() {
		return port;
	}

	public String getSchema() {
		return schema;
	}

	public String getAuthority() {
		return authority;
	}

	public String getPath() {
		return path;
	}

	public String getQuery() {
		return query;
	}

	public String getFragment() {
		return fragment;
	}

	public void parsingURL(String url) {
		//schema://authority/path[? query][# fragment]
		String reg = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(url);
		if (matcher.matches()) {
			schema = matcher.group(2);
			authority = matcher.group(4);
			path = matcher.group(5);
			query = matcher.group(7);
			fragment = matcher.group(9);
			reg = "(.*):([0-9]*)";
			pattern = Pattern.compile(reg);
			matcher = pattern.matcher(authority);
			if (matcher.matches()) {
				authority = matcher.group(1);
				port = matcher.group(2);
			}			
		} else {
			System.err.println("Can not match the URL!");
		}
	}
}
