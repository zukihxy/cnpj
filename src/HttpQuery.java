import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.zip.Inflater;

public class HttpQuery {
	public void query(String path, String authority, String port) {
		String info = "";
		if (path.equals(""))
			path = "/";
		try {
			/*
			 * Make http request set some necessary attribute most importantly
			 * set method and host
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
					+ "Connection: keep-alive\r\n" + "Accept-Language: zh-CN,zh;q=0.8\r\n"
					+ "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n"
					+ "Accept-Encoding: gzip, deflate, sdch\r\n" + "\r\n";
			pw.write(request);
			System.err.println(request);
			// socket.setSoTimeout(5000); // set time out
			pw.flush();

			/* Get response from server */
			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			info = br.readLine();
			int len = -1;
			if (info != null && info.startsWith("HTTP/1.1")) { // check whether
																// header is
																// legal
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
						} else if (info.split(": ").length == 2) { // fetch some
																	// useful
																	// attribute
							System.err.println(info);
							String[] result = info.split(": ");
							if (result[0].equals("Content-Length"))
								len = Integer.parseInt(result[1]);
							else if (result[0].equals("Transfer-Encoding"))
								chuncked = result[1].equals("chunked");
							else if (result[0].equals("Content-Encoding"))
								gzip = result[1].equals("gzip");
							else if (result[0].equals("Content-Type"))
								charset = result[1].contains("charset") ? result[1].split("charset=")[1].toLowerCase()
										: "utf-8";
						}
					}
				}

				if (chuncked) {
					ArrayList<Byte> re = handleChunked(br, charset, gzip);
					byte[] data = new byte[re.size()];
					if (gzip) {
						for (int k = 0; k < re.size(); k++)
							data[k] = re.get(k);
						handleGzip(data, charset);
					}
				} else if (!chuncked && !gzip) { // simple case
					String read;
					while (len > 0 && (read = br.readLine()) != null) {
						len -= read.getBytes().length;
						System.out.print(read);
						if (len > 0) // readline() may eliminate the crlf
							System.out.print("\r\n");
					}
					if (len == -1) { // didn't specify the length
						while ((read = br.readLine()) != null)
							System.out.print(read + "\r\n");
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

	private ArrayList<Byte> handleChunked(BufferedReader br, String charset, boolean gzip) throws IOException {
		byte[] temp; // with reference to RFC
		int size = readChunkSize(br);
		ArrayList<Byte> result = new ArrayList<Byte>();
		if (size < 0)
			return null;
		while (size > 0) { // read till last chunk
			temp = new byte[size];
			if (!gzip) {
				for (int j = 0; j < size; j++) {
					int read = br.read();
					if (read > 256 && charset.equals("utf-8")) // handle chinese
						j += 2; // one char in utf-8 is 3 byte
					System.out.print((char) read);
				}
			} else {
				while (size > 0) {
					String line = br.readLine();
					byte[] data = line.getBytes();
					for (int j = 0; j < size; j++) {
						result.add(data[j]);
					}
					size -= data.length;
					if (size > 0) { // add eliminated crlf
						result.add((byte)0x0d);
						result.add((byte)0x0a);
					}						
				}
			}
			br.readLine();
			size = readChunkSize(br);
		}
		return result;
	}

	private int readChunkSize(BufferedReader br) throws IOException {
		String line = br.readLine(); // chunck size and extension occupy one
										// line
		if (line == null) // ending with CRLF
			return 0;
		if (line.contains(":")) // separate size and extension
			line = line.split(":")[0];
		return Integer.valueOf(line, 16);
	}

	private byte[] decompress(byte[] data) {
		byte[] output = new byte[0];

		Inflater decompresser = new Inflater();
		decompresser.reset();
		decompresser.setInput(data);

		ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
		try {
			byte[] buf = new byte[1024];
			while (!decompresser.finished()) {
				int i = decompresser.inflate(buf);
				o.write(buf, 0, i);
			}
			output = o.toByteArray();
		} catch (Exception e) {
			output = data;
			e.printStackTrace();
		} finally {
			try {
				o.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		decompresser.end();
		return output;
	}

	private void handleGzip(byte[] data, String charset) {
		if ((data[0] ^ 0x1f) != 0)
			return;
		if ((data[1] ^ 0x8b) != 0)
			return;
		boolean ftext = ((data[2] & 0x1) == 1);
		boolean fhcrc = ((data[2] & 0x2) == 1);
		boolean fextra = ((data[2] & 0x4) == 1);
		boolean fname = ((data[2] & 0x8) == 1);
		boolean fcomment = ((data[2] & 0x10) == 1);
		int index = 0;
		if (fextra) {
			// read len, and len of data
			// move index
		}
		if (fname) {
			// find zero as terminator
			// move index
		}
		if (fcomment) {
			// find zero as terminator
			// move index
		}
		if (fhcrc) {
			// 2 bytes of crc-16
			// move index
		}
		byte[] compressed = new byte[data.length - index - 8];
		byte[] crc32 = new byte[8];
		System.arraycopy(data, index, compressed, 0, compressed.length);
		System.arraycopy(data, compressed.length+index, crc32, 0, 8);
		byte[] origin = decompress(data);
		// calculate crc for compressed data
		// check crc-32 and crc-16 (if exist)
		// print data
		for (int i = 0; i < origin.length; i ++) {
			char print;
			if (origin[i] > 256 && charset.equals("utf-8")) { // handle chinese
				print = (char)(origin[i]&0xff<<16);
				i += 2; // one char in utf-8 is 3 byte
			} else {
				print = (char)origin[i];
			}
			System.out.print((char) print);
		}
	}
}
