import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpQuery {
	public void query(String path, String authority, String port) {
		String info = "";
		if (path.equals(""))
			path = "/";
		try {
			/* 
			 * Make http request 
			 * set some necessary attribute
			 * most importantly set method and host
			 */
			InetAddress ip = InetAddress.getByName(authority);
			if (!port.equals(""))
				authority += ":" + port;
			if (port.equals(""))
				port = "80";
			Socket socket = new Socket(ip, Integer.parseInt(port));
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			String request = "GET " + path + " HTTP/1.1\r\n" + "Host: " + authority
					+ "\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2914.3 Safari/537.36\r\n"
					+ "Connection: keep-alive\r\n"
					+ "Accept-Language: zh-CN,zh;q=0.8\r\n"
					+ "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n"
					+ "\r\n";
			pw.write(request);
			System.err.println(request);
			//socket.setSoTimeout(5000); // set time out
			pw.flush();

			/* Get response from server */
			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			info = br.readLine();
			int len = -1;
			if (info != null && info.startsWith("HTTP/1.1")) { // check whether header is legal
				System.err.println(info);
				String reg = "HTTP/1.1 [1-5]([0-9]{2}) .*";
				Pattern pattern = Pattern.compile(reg);
				Matcher matcher = pattern.matcher(info);
				if (!matcher.matches()) {
					System.err.println("Http error: Broken response in start line!");
					return;
				} else {
					reg = "HTTP/1.1 200 .*";
					pattern = Pattern.compile(reg);
					matcher = pattern.matcher(info);
					if (!matcher.matches()) {
						System.err.println("Http error: Unsuccessfull response in start line!");
						return;
					} 
				}
				boolean header = false; // whether it is header or body
				boolean chuncked = false; // signal for chuncked
				boolean gzip = false; // signal for gzip
				String charset = "utf-8";
				while ((info = br.readLine()) != null) { 
					if (header == false && info.equals("")) {
						break;
					} else if (header == false) {
						if (info.split(": ").length != 2 && !info.equals("")) {
							System.err.println("Http error: Broken response: " + info + "!");
							return;
						} else if (info.split(": ").length == 2) { // fetch some useful attribute
							System.err.println(info);
							String[] result = info.split(": ");
							if (result[0].equals("Content-Length"))
								len = Integer.parseInt(result[1]);
							else if (result[0].equals("Transfer-Encoding"))
								chuncked = result[1].equals("chunked");
							else if (result[0].equals("Content-Encoding"))
								gzip = result[1].equals("gzip");
							else if (result[0].equals("Content-Type"))
								charset = result[1].contains("charset") ?
										result[1].split("charset=")[1].toLowerCase() : "utf-8";
						}
					}
				}

				if (chuncked) {
					handleChunked(br, charset);
				} else if (!chuncked && !gzip){ // simple case
					String read;
					while (len > 0 && (read = br.readLine()) != null) {
						len -= read.getBytes().length;
						System.out.print(read);
						if (len > 0) // readline() may eliminate the crlf
							System.out.print("\r\n");
					}
					if (len == -1) { // didn't specify the length
						while ((read = br.readLine()) != null) 
							System.out.print(read+"\r\n");
					}
				}

				is.close();
				pw.close();
				os.close();
				socket.close();
			}
		} catch (SocketTimeoutException e) {
			System.err.println("Http error: Time out!");
		} catch (NumberFormatException e) {
			System.err.println("Http error: Wrong content length!");
		} catch (UnknownHostException e) {
			System.err.println("Http error: Can not find " + authority + "!");
		} catch (IOException e) {
			System.err.println("Http error: Fail to resolve " + info + "!");
		}
	}

	private void handleChunked(BufferedReader br, String charset) throws IOException {
		byte[] temp; // with reference to RFC
		int size = readChunkSize(br);
		if (size < 0)
			return;
		while (size > 0) { // read till last chunk
			temp = new byte[size];			
			for (int j = 0; j < size; j++) {
				int read = br.read();
				if (read > 256 && charset.equals("utf-8")) // handle chinese
					j += 2; // one char in utf-8 is 3 byte
				System.out.print((char)read);
			}
			br.readLine();
			size = readChunkSize(br);
		}
	}

	private int readChunkSize(BufferedReader br) throws IOException {
		String line = br.readLine(); // chunck size and extension occupy one line
		if (line == null) // ending with CRLF
			return 0;
		if (line.contains(":")) // separate size and extension
			line = line.split(":")[0];
		return Integer.valueOf(line, 16);
	}
}
