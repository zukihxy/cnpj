import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DNSQuery {	
	private String dns = "";
	
	public String getDns() {
		return dns;
	}

	public void query(String address) {		
		try { 
            DatagramSocket sendSocket = new DatagramSocket();  
            //System.out.println(address);
            ByteArrayOutputStream osbyte = new ByteArrayOutputStream();
            DataOutputStream osdata = new DataOutputStream(osbyte);
            int id = (int)(Math.random() * 1024);
            osdata.writeShort(id); // ID(should be random)
            osdata.writeShort(0x10); // FLAG(RD = 1)
            osdata.writeShort(0x01); // QCOUNT
            osdata.writeShort(0x00); // ANCOUNT
            osdata.writeShort(0x00); // NSCOUNT
            osdata.writeShort(0x00); // ARCOUNT
            String[] labels = address.split("\\.");
            byte[] before = osbyte.toByteArray();
            for (String l : labels) {// QNAME
            	if (l.length() > 63)
            		return;
            	osdata.writeByte(l.length());
            	for (int i = 0; i < l.length(); i++) // must write chars one by one 
            		osdata.writeByte(l.charAt(i));   // or there will be 0x0 after every char
            }  
            osdata.writeByte(0x0); // End of name
            osdata.writeShort(0x01); // QTYPE
            osdata.writeShort(0x01); // QCLASS
            byte[] after = osbyte.toByteArray();
            int query_len = after.length-before.length;
            byte[] obuf = osbyte.toByteArray();
            int port = 53;  
            InetAddress ip = InetAddress.getByName("202.120.224.26");  
            DatagramPacket sendPacket = new DatagramPacket(obuf, obuf.length, ip,  port);  
            sendSocket.send(sendPacket); 
  
            byte[] iBuf = new byte[4096];   
            DatagramPacket getPacket = new DatagramPacket(iBuf, iBuf.length);   
            sendSocket.receive(getPacket);  
  
            ByteArrayInputStream isbyte = new ByteArrayInputStream(iBuf);
            DataInputStream isdata = new DataInputStream(isbyte); 
            int rid = isdata.readShort();
            if (rid != id) {
            	System.err.println("DNS error: Not the response to sent query.");
            	return;
            }
            int rdata = isdata.readByte(); // FLAGS(most significant)
            int qr = (rdata & 128) >> 7;
            rdata = isdata.readByte(); // FLAGS(least significant)
            int rcode = (rdata & 15);
            if (qr != 1 | rcode != 0) {
            	System.err.println("DNS error: Wrong flags in response.");
            	return;
            }
            isdata.skipBytes(2);
            int count = isdata.readShort(); // ANCOUNT
            count += isdata.readShort(); // NSCOUNT
            count += isdata.readShort();// ARCOUNT
            
            int len;
            int[] buf = new int[4];
            isdata.skipBytes(query_len);
            for (int i = 0; i < count; i++) { // resolve RR
            	parseName(isdata); // skip name in RR
            	int type = isdata.readShort();
            	int qclass = isdata.readShort();
            	//System.out.println(type+" "+qclass);
            	isdata.skipBytes(4); // TTL
            	if ((type != 0x01 && type != 0x1c) || qclass != 0x01) {            		
                	len = isdata.readShort();
                	isdata.skipBytes(len);
            		continue;            		
            	} else if (type == 0x01){ // IPV4 address
                	len = isdata.readShort();
                	buf = new int[len];              	
                	for (int j = 0; j < len; j++) 
                		buf[j] = isdata.read();  
                	break;
            	} else if (type == 0x1c) { // IPV6 address
            		// what's the difference?
            	}
            }            
        	dns = buf[0]+"."+buf[1]+"."+buf[2]+"."+buf[3];
		} catch (IOException e) {
			System.err.println("DNS error: Fail to reslove!");
		}
	}
	
	private void parseName(DataInputStream isdata) throws IOException {
		int name_len = isdata.readByte() & 0xff;
    	if (name_len >= 0xc0) // Name is a pointer
    		isdata.skipBytes(1);
    	 else  // Name is directly in this part
    		isdata.skip(name_len);  	
	}
}
