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

public class HttpQuery {
	public void query(String path, String authority, String port) {
		String info = "";
		if (path.equals(""))
			path = "/";
		try {
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
			pw.flush();
			// socket.shutdownOutput();

			// socket.setSoTimeout(5000);
			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			info = br.readLine();
			int len = 1;
			if (info != null && info.startsWith("HTTP/1.1")) {//
				System.err.println(info);
				if (info.split(" ").length != 3) {
					System.err.println("Http error: Broken response: " + info + "!");
					return;
				}
				boolean output = false;
				boolean chuncked = false;
				boolean gzip = false;
				while ((info = br.readLine()) != null) {// len > 0 &&
					// System.out.println("any:"+info);

					if (output == false && info.equals("")) {
						output = true;
						is.mark(info.length() + 1);
						break;
					} else if (output == false) {
						if (info.split(": ").length != 2 && !info.equals("")) {
							System.err.println("Http error: Broken response: " + info + "!");
							return;
						} else if (info.split(": ").length == 2) {
							System.err.println(info);
							String[] result = info.split(": ");
							if (result[0].equals("Content-Length"))
								len = Integer.parseInt(result[1]);
							else if (result[0].equals("Transfer-Encoding"))
								chuncked = result[1].equals("chunked");
							else if (result[0].equals("Content-Encoding"))
								gzip = result[1].equals("gzip");
						}
					} else { // if comes to the message body
						break;
					}
				}
				DataInputStream isdata = new DataInputStream(is);
				if (chuncked) {
					handleChunked(isdata);
				} else {
					if (len != 1) {
						byte[] b = new byte[len];
						int i = 0;// = isdata.read(b);
						while (len > 0) {
							i = isdata.read(b, i, len);
							if (i < 0)
								break;
							len -= i;
						}
						System.out.print(new String(b));
					} else {
						String read;
						while ((read = br.readLine()) != null) {
							System.out.print(read);
						}
					}
				}
			} else {
				System.err.println("Http error: Broken response: " + info + "!");
				return;
			}

			is.close();
			pw.close();
			os.close();
			socket.close();
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

	private void handleChunked(DataInputStream isdata) throws IOException {
		// String line = br.readLine();
		// if (line == null)
		// return null;
		// byte[] result = new byte[0];
		byte[] temp;
		int i = 0;
		int size = readChunkSize(isdata);
		// System.out.println("size: "+size); // debug
		while (size > 0) {
			temp = new byte[size];
			int j = 0;
			i += size;
			// br.close();
			while (size > j) { // read data of one chunk
				// DataInputStream isdata = new DataInputStream(is);
				j += isdata.read(temp, j, size - j);
				// System.out.println(j);
				// size -= j;
				// isdata.close();
			}
			System.out.print(new String(temp, "utf-8"));
			// int l = result.length;
			// byte[] t = new byte[l];
			// if (result.length > 0)
			// System.arraycopy(result, 0, t, 0, result.length);
			// result = new byte[l + size];
			// System.arraycopy(t, 0, result, 0, l);
			// System.arraycopy(temp, 0, result, l, size);

			isdata.skipBytes(2);
			size = readChunkSize(isdata);
		}
	}

	private int readChunkSize(DataInputStream isdata) throws IOException {
		ArrayList<Byte> temp = new ArrayList<Byte>();
		byte curl;
		int i = 0;
		while ((curl = isdata.readByte()) != 0x0a) { // read chunk size
			if (i > 0 && temp.get(i - 1) == 0x0b) // and chunk extension
				break;
			i++;
			temp.add(curl);
		}
		byte[] t = new byte[temp.size() - 1];
		for (int j = 0; j < t.length; j++) // ArrayList to Array
			t[j] = temp.get(j);
		String line = new String(t);
		if (line.contains(":"))
			line = line.split(":")[0];
		return Integer.valueOf(line, 16);
	}
}
