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
	//뒤의 컬렉션들이 추가되면 7번 커넥션을 반복해서 찾아야할듯., "Social_Listening_SNS_2", "Social_Listening_Store", "VD1_eMarket_test", "crawler_test_1", "da_buzz_sentiment", "sejong_corpus_test_2018=true", "test_yhj", "vd1_buzz_sentiment", "vd1_emarket", "vd1_expert", "vd1_twitter"};
	//static int[] runningTimes = {10};		
	
	public static void main(String[] args) throws IOException, JSchException {
		//ICC에 접속정보
		String host = "10.10.80.134";
		int port = 22;
		String user = "esadmin";
		String password = "passw0rd";
		//ICC 접속(telnet<SSH (JSch사용)
		//ref : https://code-examples.net/ko-kr/q/2edf10
		JSch js = new JSch();
		Session session = js.getSession(user, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        	System.out.println("Connected!!");
        //command 채널을 연다.
        Channel channel = session.openChannel("exec"); //can use channelExec (ref : https://epaul.github.io/jsch-documentation/javadoc/com/jcraft/jsch/Session.html#openChannel-java.lang.String-)     
        //채널을 SSH용 채널 객체로 캐스팅한다
        ChannelExec channelExec = (ChannelExec) channel;
        //커맨드 실행
        //channelExec.setCommand("cat ./esdata/data/indexer_state.txt"); //esadmin 명령어는 response가 null로 나온다, type esadmin으로 위치검색해서 직접 명령실행.
        channelExec.setCommand("/opt/IBM/es/bin/esadmin check");
        channelExec.connect();        
        	System.out.println("==> Command sent");
       	//프로세스 상태체크 
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(channelExec.getInputStream(), "UTF-8"));
        	String line;
//파일기준읽어왔을때 (deprecated)
//        	int len = collections.length; 
//        	for(int index = 0; (line = br.readLine()) != null && index < len; index++) { //+2(시간line,주석line);
//        		if(line.matches(".*" + collections[index] + ".*"))
//                		System.out.println(line);        
//        	}
        
        	while((line = br.readLine()) != null)
        		if(line.matches(".*" + collections[0] + ".indexservice" + ".*"))
                		System.out.println(line);        
        				//if(뒤에서부터 5개 읽어와서 start냐?) continue;
        				//else 실행명령어(인덱서 이동묵실장님)        	
    		//적당한 시간 간격을 두고 리빌드를 한다.
			//인덱스서비스는 완료시점파악 불가 로그도 없어서 임의의 시간을 배열로하여 돌린다.
			//*리빌드 커맨드가 뭘까?
        	channelExec.disconnect();
			System.out.println("==> disconnected..");
			
		
	}

}
