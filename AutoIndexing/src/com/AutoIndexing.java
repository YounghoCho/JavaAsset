package com;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class AutoIndexing {
	//collections, runningTimes
	static 	String[] collections = {"Social_Listening_SNS"};
	//���� �÷��ǵ��� �߰��Ǹ� 7�� Ŀ�ؼ��� �ݺ��ؼ� ã�ƾ��ҵ�., "Social_Listening_SNS_2", "Social_Listening_Store", "VD1_eMarket_test", "crawler_test_1", "da_buzz_sentiment", "sejong_corpus_test_2018=true", "test_yhj", "vd1_buzz_sentiment", "vd1_emarket", "vd1_expert", "vd1_twitter"};
	//static int[] runningTimes = {10};		
	
	public static void main(String[] args) throws IOException, JSchException {
		//ICC�� ��������
		String host = "10.10.80.134";
		int port = 22;
		String user = "esadmin";
		String password = "passw0rd";
		//ICC ����(telnet<SSH (JSch���)
		//ref : https://code-examples.net/ko-kr/q/2edf10
		JSch js = new JSch();
		Session session = js.getSession(user, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        	System.out.println("Connected!!");
        //command ä���� ����.
        Channel channel = session.openChannel("exec"); //can use channelExec (ref : https://epaul.github.io/jsch-documentation/javadoc/com/jcraft/jsch/Session.html#openChannel-java.lang.String-)     
        //ä���� SSH�� ä�� ��ü�� ĳ�����Ѵ�
        ChannelExec channelExec = (ChannelExec) channel;
        //Ŀ�ǵ� ����
        //channelExec.setCommand("cat ./esdata/data/indexer_state.txt"); //esadmin ��ɾ�� response�� null�� ���´�, type esadmin���� ��ġ�˻��ؼ� ���� ��ɽ���.
        channelExec.setCommand("/opt/IBM/es/bin/esadmin check");
        channelExec.connect();        
        	System.out.println("==> Command sent");
       	//���μ��� ����üũ 
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(channelExec.getInputStream(), "UTF-8"));
        	String line;
//���ϱ����о������ (deprecated)
//        	int len = collections.length; 
//        	for(int index = 0; (line = br.readLine()) != null && index < len; index++) { //+2(�ð�line,�ּ�line);
//        		if(line.matches(".*" + collections[index] + ".*"))
//                		System.out.println(line);        
//        	}
        
        	while((line = br.readLine()) != null)
        		if(line.matches(".*" + collections[0] + ".indexservice" + ".*"))
                		System.out.println(line);        
        				//if(�ڿ������� 5�� �о�ͼ� start��?) continue;
        				//else �����ɾ�(�ε��� �̵��������)        	
    		//������ �ð� ������ �ΰ� �����带 �Ѵ�.
			//�ε������񽺴� �Ϸ�����ľ� �Ұ� �α׵� ��� ������ �ð��� �迭���Ͽ� ������.
			//*������ Ŀ�ǵ尡 ����?
        	channelExec.disconnect();
			System.out.println("==> disconnected..");
			
		
	}

}
