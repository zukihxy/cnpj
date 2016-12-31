import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpQuery {
	public void query(String path, String authority, String port) {
		String info = "";
		if (path == "")
			path = "/";
		if (port == "")
			port = "80";
		try {
			InetAddress ip = InetAddress.getByName(authority);
			Socket socket = new Socket(ip, Integer.parseInt(port));
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			if (!port.equals(""))
				authority += ":" + port;
			String request = "GET " + path + " HTTP/1.1\r\nHost: " + authority
					+ "\r\nConnection: keep-alive\r\nUser-agent: Mozilla/5.0\r\n" + "Accept-language: zh-CN\r\n\r\n";
			pw.write(request);
			pw.flush();
			socket.shutdownOutput();

			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			info = br.readLine();
			if (info != null && info.startsWith("HTTP/1.1")) {
			    //System.out.println("any:"+info);
				if (info.split(" ").length != 3) {
					System.err.println("Http error: Broken response: "+info+"!");
					return;
				}
				boolean output = false;
				while ((info = br.readLine()) != null) {
					//System.out.println("any:"+info);
					if (output == false && info.equals("")) {
						output = true;
						continue;
					} else if (output == false){
						if (info.split(": ").length != 2 && !info.equals("")) {
							System.err.println("Http error: Broken response: "+info+"!");
							return;
						} else if (info.split(": ").length == 2) {
							String[] result = info.split(": ");
							if (result[0].equals("Content-Length") && result[1].startsWith("-")) {
								System.err.println("Http error: Wrong length: "+info+"!");
								return;
							}
						}
					} else //if (output)
						System.out.println(info);
					
				}
			} else {
				System.err.println("Http error: Broken response: "+info+"!");
				return;
			}
			br.close();
			is.close();
			pw.close();
			os.close();
			socket.close();
		} catch (UnknownHostException e) {
			System.err.println("Http error: Can not find " + authority + "!");
		} catch (IOException e) {
			System.err.println("Http error: Fail to resolve " + info + "!");
		}
	}
}
