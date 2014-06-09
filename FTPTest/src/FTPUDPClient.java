import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class FTPUDPClient extends JFrame implements ActionListener{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new FTPUDPClient().setVisible(true);
	}

	private JButton enter;
	private JButton back;
	private JTextField urlTxt;
	private JTextArea etxt;
	private JList docList;
	DefaultListModel listModel;
	private JPanel mypanel;
	
	private String dir;  //存储目录
	private DatagramSocket client;
	private String serverIP = "";
	private DatagramPacket initPacket;
	private int port;
	private InetAddress addr;
	
	public FTPUDPClient()
	{
		initData();
		initFrame();
	}

	public void initFrame()
	{
		mypanel = new JPanel();
		enter = new JButton();
		back = new JButton();
		urlTxt = new JTextField();
		etxt = new JTextArea();
		listModel = new DefaultListModel();
		docList = new JList(listModel);
		urlTxt.setBounds(10, 10, 400, 15);
		urlTxt.setPreferredSize(new Dimension(400,30));
		
		enter.setBounds(420, 10, 50, 15);
		enter.setText("连接");
		enter.addActionListener(this);
		
		back.setBounds(480, 10, 50, 15);
		back.setText("后退");
		back.addActionListener(this);
		
		etxt.setBounds(10, 35, 520, 400);
		etxt.setPreferredSize(new Dimension(520, 400));
		
		docList.setBounds(10, 35, 520, 400);
		docList.setPreferredSize(new Dimension(520, 400));
		docList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(docList.getSelectedIndex() != -1) {
					if(e.getClickCount() == 2)
                    	listDoubleClickAction();

				}
			}
		});
		
		mypanel.add(urlTxt);
		mypanel.add(enter);
		mypanel.add(back);
		
		docList.setVisible(false);
		etxt.setVisible(false);
		
		mypanel.add(docList);
		mypanel.add(etxt);
		add(mypanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(560,500);
	}
	
	public void initData()
	{
		
		port = 5050;
		dir = "/";
		try {
			client = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void UDPSend(String sendStr)
	{
		try {
			
	        byte[] sendBuf;
	        sendBuf = sendStr.getBytes();
	        DatagramPacket sendPacket 
	            = new DatagramPacket(sendBuf , sendBuf.length , addr , port );   
	        client.send(sendPacket);
	        System.out.println("?");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String UDPRead()
	{
		String reciStr=null;
        try {
			byte[] recvBuf = new byte[1024];
	        DatagramPacket recvPacket 
	            = new DatagramPacket(recvBuf , recvBuf.length);
	        client.receive(recvPacket);
	        reciStr = new String(recvPacket.getData() , 0 , recvPacket.getLength());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return reciStr;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == enter)
			enterAction();
		else if(e.getSource() == back)
			backAction();
	}
	
	public void enterAction()
	{
		try {
			String rec = null;
			//首先初始化对应的ip等数据
			serverIP = urlTxt.getText();
			addr = InetAddress.getByName(serverIP);
			//随便发点什么，告诉他我要访问了，收到回复的端口后，重新建立udp连接，用新端口进行通信
			UDPSend("hello");
			rec = UDPRead();
			port = Integer.parseInt(rec);
			
			UDPSend("USER annoymouce");
			rec = UDPRead();
			System.out.println(rec);
			
			UDPSend("PASS xxx@xxx.com");
			rec = UDPRead();
			System.out.println(rec);
			
			UDPSend("TYPE I");
			rec = UDPRead();
			System.out.println(rec);
			
			UDPSend("PASV");
			rec = UDPRead();
			System.out.println(rec);
			
			UDPSend("CWD /");
			rec = UDPRead();
			System.out.println(rec);
			
			commLIST();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void backAction()
	{
		if(dir.equals("/"))
			return;
		else
		{
			int i;
			for(i = dir.length()-2; i>=0; i--)
			{
				if(dir.charAt(i) == '/')
					break;
			}
			if(i+1 > 0)
				dir = dir.substring(0, i+1);
			else
				dir = "/";
			
			//更换目录
			UDPSend("CWD "+dir);
			String str = UDPRead();
			commLIST();
			urlTxt.setText(addr.toString().substring(0)+dir);
		}
	}
	
	public void listDoubleClickAction()
	{
		//System.out.println(docList.getSelectedIndex());
		String name = (String)listModel.get(docList.getSelectedIndex());
		//System.out.println(name);
		if(name.endsWith("/"))
		{
			UDPSend("CWD "+dir+name);
			String str = UDPRead();
			dir += name;
			commLIST();
		}
		else
		{	
			UDPSend("RETR "+name);
			dir += name;
			String str = UDPRead();
			str = UDPRead();
			docList.setVisible(false);
			etxt.setText(str);
			etxt.setVisible(true);
			str = UDPRead();
		}
		urlTxt.setText(addr.toString().substring(0)+dir);
	}
	
	public void commLIST()
	{
		String rec;
		UDPSend("LIST");
		rec = UDPRead();
		System.out.println(rec);
		rec = UDPRead();
		System.out.println(rec);
		String[] items = rec.split("\r\n");
		listModel.clear();
		for(int i=0; i<items.length; i++)
		{
			if(items[i].startsWith("d"))
			{
				listModel.addElement(items[i].substring(2)+"/");
			}
			else
			{
				listModel.addElement(items[i].substring(2));
			}
		}
		etxt.setVisible(false);
		docList.setVisible(true);
		//添加列表项
		//etxt.setText(rec);
		rec = UDPRead();
		System.out.println(rec);
	}
}
