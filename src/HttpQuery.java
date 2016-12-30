import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpQuery {
	public void query(String path, String authority, String dns) {
		String info = "";
		if (path == "")
			path = "/";
		try {
            Socket socket=new Socket(dns, 80);
            OutputStream os=socket.getOutputStream();
            PrintWriter pw=new PrintWriter(os);
            String request = "GET "+path+" HTTP/1.1\r\nHost: "+authority
            		+"\r\nConnection: keep-alive\r\nUser-agent: Mozilla/5.0\r\n"
            		+"Accept-language: zh-CN\r\n\r\n";
            pw.write(request);
            pw.flush();
            socket.shutdownOutput();
            
            InputStream is=socket.getInputStream();
            BufferedReader br=new BufferedReader(new InputStreamReader(is));
            
            boolean output = false;
            while((info=br.readLine())!=null){
            	//System.out.println("any output: "+info);
            	if (output)
            		System.out.println(info);
            	if (info.startsWith("<html"))
            		output = true;
            }
            br.close();
            is.close();
            pw.close();
            os.close();
            socket.close();
        } catch (UnknownHostException e) {
            System.err.println("Http error: Can not find "+dns+"!");
        } catch (IOException e) {
        	System.err.println("Http error: Fail to resolve "+info+"!");
        }
	}
}
