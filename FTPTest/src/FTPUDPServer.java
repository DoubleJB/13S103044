import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;


public class FTPUDPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new FTPUDPServer();
	}
	public FTPUDPServer() {
		
        try {
        	DatagramSocket  server = new DatagramSocket(5050);
        	byte[] recvBuf = new byte[1024];
        	DatagramPacket recvPacket 
            	= new DatagramPacket(recvBuf , recvBuf.length);
        	while(true){
        		server.receive(recvPacket);
                FtpHandler h = new FtpHandler(recvPacket);
				h.start();
        	}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public ArrayList<FtpHandler> users = new ArrayList<FtpHandler>();
	public static int counter = 0;
	public static String initDir = "ftp/";

	class UserInfo {
		String user;
		String password;
		String workDir;
		
		public UserInfo(String a, String b, String c)
		{
			user = a;
			password = b;
			workDir = c;
		}
	}

	class FtpHandler extends Thread {
		String ServerIP = "192.168.1.107";

		DatagramSocket server;
		int port; //�ͻ��˵�port
		InetAddress clientAddr = null;
		int id;
		String cmd = ""; // ���ָ��(�ո�ǰ)
		String param = ""; // �ŵ�ǰָ��֮��Ĳ���(�ո��)
		String user;
		String remoteHost = " "; // �ͻ�IP
		int remotePort = 0; // �ͻ�TCP �˿ں�
		String dir = "/";// ��ǰĿ¼
		String rootdir = "E:/codes/FTPTest/ftp/"; // Ĭ�ϸ�Ŀ¼,��checkPASS������
		int state = 0; // �û�״̬��ʶ��,��checkPASS������
		String reply; // ���ر���
		int type = 0; // �ļ�����(ascII �� bin)
		String requestfile = "";
		boolean isrest = false;

		int sendPort;
        InetAddress addr;
		
		// FtpHandler����
		// ���췽��
		public FtpHandler(DatagramPacket reciPacket) {
			
			dir = "/";
			sendPort = reciPacket.getPort();
			addr = reciPacket.getAddress();
			port = getPort();
			try {
				server = new DatagramSocket(port);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//�ȸ��߿ͻ����һ��˿���
			udpSend(""+port);
		}

		// run ����
		public void run() {
			String str = "";
			int parseResult; // ��cmd һһ��Ӧ�ĺ�

			try {
				state = FtpState.FS_WAIT_LOGIN; // 0
				boolean finished = false;
				while (!finished) {
					str = UdpRead(); // /
					if (str == null)
						finished = true; // ����while
					else {
						parseResult = parseInput(str); // ָ��ת��Ϊָ���
						System.out.println("ָ��:" + cmd + " ����:" + param);
						System.out.print("->");
						switch (state) // �û�״̬����
						{
						case FtpState.FS_WAIT_LOGIN:
							finished = commandUSER();
							break;
						case FtpState.FS_WAIT_PASS:
							finished = commandPASS();
							break;
						case FtpState.FS_LOGIN: {
							switch (parseResult)// ָ��ſ���,���������Ƿ�������еĹؼ�
							{
							case -1:
								errCMD(); // �﷨��
								break;
							case 2:
								finished = commandPASV();
								break;
							case 3:
								finished = commandSYST();
								break;
							case 4:
								finished = commandCDUP(); // ����һ��Ŀ¼
								break;
							case 6:
								finished = commandCWD(); // ��ָ����Ŀ¼
								break;
							case 7:
								finished = commandQUIT(); // �˳�
								break;
							case 9:
								finished = commandPORT(); // �ͻ���IP:��ַ+TCP �˿ں�
								break;
							case 11:
								finished = commandTYPE(); // �ļ���������(ascII �� bin)
								break;
							case 14:
								finished = commandRETR(); // �ӷ������л���ļ�
								break;
							case 15:
								finished = commandSTOR(); // ��������з����ļ�
								break;
							case 22:
								finished = commandABOR(); // �رմ���������dataSocket
								break;
							case 23:
								finished = commandDELE(); // ɾ���������ϵ�ָ���ļ�
								break;
							case 25:
								finished = commandMKD(); // ����Ŀ¼
								break;
							case 27:
								finished = commandLIST(); // �ļ���Ŀ¼���б�
								break;
							case 26:
							case 33:
								finished = commandPWD(); // "��ǰĿ¼" ��Ϣ
								break;
							case 32:
								finished = commandNOOP(); // "������ȷ" ��Ϣ
								break;

							}
						}
							break;

						}
					}
					System.out.println(reply);
					udpSend(reply);

				}
			} catch (Exception e) {
				System.out.println("connection reset!");
				//e.printStackTrace();
			} 
		}

		void udpSend(String sendStr)
		{
			try {

		        byte[] sendBuf;
		        sendBuf = sendStr.getBytes();
		        DatagramPacket sendPacket 
		            = new DatagramPacket(sendBuf , sendBuf.length , addr , sendPort );
		        
				server.send(sendPacket);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String UdpRead()
		{
			String reciStr=null;
	        try {
				byte[] recvBuf = new byte[1024];
		        DatagramPacket recvPacket 
		            = new DatagramPacket(recvBuf , recvBuf.length);
	        	server.receive(recvPacket);
				reciStr = new String(recvPacket.getData() , 0 , recvPacket.getLength());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        return reciStr;
		}
		
		public int getPort() {   
		    DatagramSocket s = null;//ΪUDP����е�Socket��,ֻ�����ж�UDPռ�õĶ˿�  
		    // ��������ֵ֮��Ķ˿ں�  
		    int MINPORT = 10000;  
		    int MAXPORT = 65000;  
		  
		    for (; MINPORT < MAXPORT; MINPORT++) {  
		  
		        try {  
		            // �ڶ���Ϊ���Ա���IP,������������,�򹹽�һ��InetAddress����  
		            s = new DatagramSocket(MINPORT, InetAddress.getLocalHost());  
		            s.close();  
		            return MINPORT;  
		        } catch (IOException e) {  
		            // ��������˵��������,�������������.  
		            continue;  
		        }  
		  
		    }  
		  
		    // ��������þͷ���-1  
		    return -1;  
		}  
		
		// parseInput����
		int parseInput(String s) {
			int p = 0;
			int i = -1;
			p = s.indexOf(" ");
			if (p == -1) // ������޲�������(�޿ո�)
				cmd = s;
			else
				cmd = s.substring(0, p); // �в�������,���˲���

			if (p >= s.length() || p == -1)// ����޿ո�,��ո��ڶ����s������֮��
				param = "";
			else
				param = s.substring(p + 1, s.length());
			cmd = cmd.toUpperCase(); // ת���� String Ϊ��д

			if(cmd.equals("PASV"))
				i = 2;
			if(cmd.equals("SYST"))
				i = 3;
			if (cmd.equals("CDUP"))
				i = 4;
			if (cmd.equals("CWD"))
				i = 6;
			if (cmd.equals("QUIT"))
				i = 7;
			if (cmd.equals("PORT"))
				i = 9;
			if (cmd.equals("TYPE"))
				i = 11;
			if (cmd.equals("RETR"))
				i = 14;
			if (cmd.equals("STOR"))
				i = 15;
			if (cmd.equals("ABOR"))
				i = 22;
			if (cmd.equals("DELE"))
				i = 23;
			if (cmd.equals("MKD"))
				i = 25;
			if (cmd.equals("PWD"))
				i = 26;
			if (cmd.equals("LIST"))
				i = 27;
			if (cmd.equals("NOOP"))
				i = 32;
			if (cmd.equals("XPWD"))
				i = 33;
			return i;
		}

		// validatePath����
		// �ж�·��������,���� int
		int validatePath(String s) {
			File f = new File(s); // ���·��
			if (f.exists() && !f.isDirectory()) {
				String s1 = s.toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 1; // �ļ������Ҳ���·��,����rootdir ��ʼ
				else
					return 0; // �ļ������Ҳ���·��,����rootdir ��ʼ
			}
			f = new File(addTail(dir) + s);// ����·��
			if (f.exists() && !f.isDirectory()) {
				String s1 = (addTail(dir) + s).toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 2; // �ļ������Ҳ���·��,����rootdir ��ʼ
				else
					return 0; // �ļ������Ҳ���·��,����rootdir ��ʼ
			}
			return 0; // �������
		}

		private boolean commandPASV() {
			reply = "227 Entering Passive Mode ("+ServerIP+","+(port/256)+","+(port%256)+").";
			return false;
		}
		
		private boolean commandSYST() {
			// SYST
			reply = "215 UNIX Type: L8";
			return false;
		}
		
		// commandUSER����
		// �û����Ƿ���ȷ
		boolean commandUSER() {
			if (cmd.equals("USER")) {
				reply = "331 �û�����ȷ,��Ҫ����";
				user = param;
				state = FtpState.FS_WAIT_PASS;
				return false;
			} else {
				reply = "501 �����﷨����,�û�����ƥ��";
				return true;
			}

		}

		// commandPASS ����
		// �����Ƿ���ȷ
		boolean commandPASS() {
			if (cmd.equals("PASS")) {
				reply = "230 �û���¼��";
				state = FtpState.FS_LOGIN;
				System.out.println("����Ϣ: �û�: " + param + " ������: "
							+ remoteHost + "��¼��");
				System.out.print("->");
				return false;
			} else {
				reply = "501 �����﷨����,���벻ƥ��";
				return true;
			}

		}

		void errCMD() {
			reply = "500 �﷨����";
		}

		boolean commandCDUP()// ����һ��Ŀ¼
		{
			File f = new File(dir);
			if (f.getParent() != null && (!dir.equals(rootdir)))// �и�·�� && ���Ǹ�·��
			{
				dir = f.getParent();
				reply = "200 ������ȷ";
			} else {
				reply = "550 ��ǰĿ¼�޸�·��";
			}

			return false;
		}// commandCDUP() end

		boolean commandCWD()// CWD (CHANGE WORKING DIRECTORY)
		{ // ������ı乤��Ŀ¼���û�ָ����Ŀ¼
			if(param.equals("/"))
				param = rootdir;
			else if(param.startsWith("/"))
				param = rootdir+param.substring(1, param.length());
			File f = new File(param);
			String s = "";
			String s1 = "";
			if (dir.endsWith("/"))
				s = dir;
			else
				s = dir + "/";
			File f1 = new File(s + param);

			if (f.isDirectory() && f.exists()) {
				if (param.equals("..") || param.equals("..\\")) {
					if (dir.compareToIgnoreCase(rootdir) == 0) {
						reply = "550 ��·��������";
						// return false;
					} else {
						s1 = new File(dir).getParent();
						if (s1 != null) {
							dir = s1;
							reply = "250 ������ļ��������, ��ǰĿ¼��Ϊ: " + dir;
						} else
							reply = "550 ��·��������";
					}
				} else if (param.equals(".") || param.equals(".\\")) {
				} else {
					dir = param;
					reply = "250 ������ļ��������, ����·����Ϊ " + dir;
				}
			} else if (f1.isDirectory() && f1.exists()) {
				dir = s + param;
				reply = "250 ������ļ��������, ����·����Ϊ " + dir;
			} else
				reply = "501 �����﷨����";

			return false;
		} // commandCDW() end

		boolean commandQUIT() {
			reply = "221 ����ر�����";
			return true;
		}// commandQuit() end

		/*
		 * ʹ�ø�����ʱ���ͻ��˱��뷢�Ϳͻ������ڽ������ݵ�32λIP ��ַ��16λ ��TCP �˿ںš�
		 * ��Щ��Ϣ��8λΪһ�飬ʹ��ʮ���ƴ��䣬�м��ö��Ÿ�����
		 */
		boolean commandPORT() {
			int p1 = 0;
			int p2 = 0;
			int[] a = new int[6];// ���ip+tcp
			int i = 0; //
			try {
				while ((p2 = param.indexOf(",", p1)) != -1)// ǰ5λ
				{
					a[i] = Integer.parseInt(param.substring(p1, p2));
					p2 = p2 + 1;
					p1 = p2;
					i++;
				}
				a[i] = Integer.parseInt(param.substring(p1, param.length()));// ���һλ
			} catch (NumberFormatException e) {
				reply = "501 �����﷨����";
				return false;
			}

			remoteHost = a[0] + "." + a[1] + "." + a[2] + "." + a[3];
			remotePort = a[4] * 256 + a[5];
			reply = "200 ������ȷ";
			return false;
		}// commandPort() end

		/*
		 * LIST ����������ͻ��˷��ط������й���Ŀ¼�µ�Ŀ¼�ṹ�������ļ���Ŀ¼���б�
		 * �����������ʱ���ȴ���һ����ʱ���׽�����ͻ��˷���Ŀ¼��Ϣ������׽��ֵ�Ŀ�Ķ˿ں�ȱʡΪ��Ȼ��Ϊ��ǰ����Ŀ¼����File
		 * �������øö����list()�����õ�һ��������Ŀ¼�������ļ�����Ŀ¼���Ƶ��ַ������飬Ȼ������������Ƿ����ļ���
		 * �����е�"."������Ŀ¼���ļ�����󣬽��õ�����������ͨ����ʱ�׽��ַ��͵��ͻ��ˡ�
		 */
		boolean commandLIST()// �ļ���Ŀ¼���б�
		{

				udpSend("150 �ļ�״̬����,ls�� ASCII ��ʽ����");
							
							File f = new File(dir);
							String[] dirStructure = f.list();// ָ��·���е��ļ�������,��������ǰ·����·��
							String fileType;
							String out="";
							for (int i = 0; i < dirStructure.length; i++) {
								if (dirStructure[i].indexOf(".") != -1) {
									fileType = "- "; // ��Ŀ¼(��linux��)
								} else {
									fileType = "d "; // ��Ŀ¼���ļ�����Ŀ¼
								}
								out = out + fileType + dirStructure[i] + "\r\n";// (fileType+dirStructure[i]);
							}
							udpSend(out);
							reply = "226 �����������ӽ���";

			return false;
		}// commandLIST() end

		boolean commandTYPE() // TYPE �������������������
		{
			if (param.equals("A")) {
				type = FtpState.FTYPE_ASCII;// 0
				reply = "200 ������ȷ ,ת ASCII ģʽ";
			} else if (param.equals("I")) {
				type = FtpState.FTYPE_IMAGE;// 1
				reply = "200 ������ȷ ת BINARY ģʽ";
			} else
				reply = "504 �����ִ�����ֲ���";

			return false;
		}

		// connamdRETR ����
		// �ӷ������л���ļ�
		boolean commandRETR() {
			String fillname = dir;
			if(param.startsWith("/"))
				fillname += param.substring(1);
			else
				fillname += param;
			System.out.println(dir);
			System.out.println(addTail(dir.substring(1)));
			System.out.println(fillname);
			requestfile = fillname;
			File f = new File(requestfile);
			if (!f.exists()) {
				f = new File(fillname);
				if (!f.exists()) {
					reply = "550 �ļ�������";
					return false;
				}
			}
			if (f.isDirectory()) {

			} else {
				if (type == FtpState.FTYPE_IMAGE) // bin
				{
					try {
						udpSend("150 �ļ�״̬����,�Զ����η�ʽ���ļ�:  "
								+ requestfile);
						BufferedInputStream fin = new BufferedInputStream(
								new FileInputStream(requestfile));
						byte[] buf = new byte[1024]; // Ŀ�껺����
						int l = 0;
						String sendStr="";
						while ((l = fin.read(buf, 0, 1024)) != -1) // ������δ����
						{
							sendStr = sendStr + new String(buf, 0, l);
						}
						udpSend(sendStr);
						fin.close();
						reply = "226 �����������ӽ���";

					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 ����ʧ��: ���������";
						return false;
					}

				}
				if (type == FtpState.FTYPE_ASCII)// ascII
				{
					try {
						udpSend("150 Opening ASCII mode data connection for "
										+ requestfile);
						BufferedReader fin = new BufferedReader(new FileReader(
								requestfile));
						String s;
						String sendStr="";
						while ((s = fin.readLine()) != null) {
							sendStr+=s; // /???
						}
						fin.close();
						udpSend(sendStr);
						reply = "226 �����������ӽ���";
					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 ����ʧ��: ���������";
						return false;
					}
				}
			}
			return false;

		}

		// commandSTOR ����
		// ��������з����ļ�STOR
		boolean commandSTOR() {
			if (param.equals("")) {
				reply = "501 �����﷨����";
				return false;
			}
			requestfile = addTail(dir) + param;
			if (type == FtpState.FTYPE_IMAGE)// bin
			{
				try {
					udpSend("150 Opening Binary mode data connection for "
									+ requestfile);

					BufferedOutputStream fout = new BufferedOutputStream(
							new FileOutputStream(requestfile));
					byte[] buf = new byte[1024];
					int l = 0;
					String tmp = null;
					while ((tmp = UdpRead()) != null) {
						fout.write(tmp.getBytes(), 0, tmp.getBytes().length);
					}
					fout.close();
					reply = "226 �����������ӽ���";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "451 ����ʧ��: ���������";
					return false;
				}
			}
			if (type == FtpState.FTYPE_ASCII)// ascII
			{
				try {
					udpSend("150 Opening ASCII mode data connection for "
									+ requestfile);
					PrintWriter fout = new PrintWriter(new FileOutputStream(
							requestfile));
					String line;
					while ((line = UdpRead()) != null) {
						fout.println(line);
					}
					fout.close();
					reply = "226 �����������ӽ���";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "451 ����ʧ��: ���������";
					return false;
				}
			}
			return false;
		}

		boolean commandPWD() {
			reply = "257 " + dir + " �ǵ�ǰĿ¼.";
			return false;
		}

		boolean commandNOOP() {
			reply = "200 ������ȷ.";
			return false;
		}

		// ǿ��dataSocket ��
		boolean commandABOR() {
			try {
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 ����ʧ��: ���������";
				return false;
			}
			reply = "421 ���񲻿���, �ر����ݴ�������";
			return false;
		}

		// ɾ���������ϵ�ָ���ļ�
		boolean commandDELE() {
			int i = validatePath(param);
			if (i == 0) {
				reply = "550 ����Ķ���δִ��,�ļ�������,��Ŀ¼����,������";
				return false;
			}
			if (i == 1) {
				File f = new File(param);
				f.delete();
			}
			if (i == 2) {
				File f = new File(addTail(dir) + param);
				f.delete();
			}

			reply = "250 ������ļ��������,�ɹ�ɾ�����������ļ�";
			return false;

		}

		// ����Ŀ¼,Ҫ����·��
		boolean commandMKD() {
			String s1 = param.toLowerCase();
			String s2 = rootdir.toLowerCase();
			if (s1.startsWith(s2)) {
				File f = new File(param);
				if (f.exists()) {
					reply = "550 ����Ķ���δִ��,Ŀ¼�Ѵ���";
					return false;
				} else {
					f.mkdirs();
					reply = "250 ������ļ��������, Ŀ¼����";
				}
			} else {
				File f = new File(addTail(dir) + param);
				if (f.exists()) {
					reply = "550 ����Ķ���δִ��,Ŀ¼�Ѵ���";
					return false;
				} else {
					f.mkdirs();
					reply = "250 ������ļ��������, Ŀ¼����";
				}
			}

			return false;
		}

		String addTail(String s) {
			if (!s.endsWith("/"))
				s = s + "/";
			return s;
		}

	}

	class FtpState {
		final static int FS_WAIT_LOGIN = 0; // �ȴ������û���״̬
		final static int FS_WAIT_PASS = 1; // �ȴ���������״̬
		final static int FS_LOGIN = 2; // �Ѿ���½״̬

		final static int FTYPE_ASCII = 0;
		final static int FTYPE_IMAGE = 1;
		final static int FMODE_STREAM = 0;
		final static int FMODE_COMPRESSED = 1;
		final static int FSTRU_FILE = 0;
		final static int FSTRU_PAGE = 1;
	}
}
