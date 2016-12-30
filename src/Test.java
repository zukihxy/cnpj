
public class Test {
	public static void main(String[] args){
//		URLParser u = new URLParser();
//		u.parsingURL("https://www.baidu.com");
//		System.out.println(u.getPath());
//		u.parsingURL("https://www.baidu.com/s/pub/ietf/uri?wd=dfaew&rsv_spt=1&rsv_iqid=0xf2 #Related");
//		u.parsingURL("http://www.example.org:8080/pub/WWW/TheProject.html");
		URLParser u = new URLParser();
		u.parsingURL("https://www.baidu.com/s/pub/ietf/uri?wd=dfaew&rsv_spt=1&rsv_iqid=0xf2 #Related");
		DNSQuery d = new DNSQuery();
		d.query(u.getAuthority());
		//System.out.println(d.getDns());
		HttpQuery h = new HttpQuery();
		h.query(u.getPath(), u.getAuthority(), d.getDns());
	}
}
