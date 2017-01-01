
public class Test {
	public static void main(String[] args) {
		URLParser u = new URLParser();
		u.parsingURL(args[0]);
		// System.out.println("Query for "+args[0]);
		DNSQuery d = new DNSQuery();
		d.query(u.getAuthority());
		HttpQuery h = new HttpQuery();
		h.query(u.getPath(), u.getAuthority(), u.getPort());
	}
}
