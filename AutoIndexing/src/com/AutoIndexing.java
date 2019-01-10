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
	
	//static String[] collections = {"Social_Listening_SNS"};
	static 	String[] collections = {"test_yhj"}; //
	//to be added : , "Social_Listening_SNS_2", "Social_Listening_Store", "VD1_eMarket_test", "crawler_test_1", "da_buzz_sentiment", "sejong_corpus_test_2018=true", "test_yhj", "vd1_buzz_sentiment", "vd1_emarket", "vd1_expert", "vd1_twitter"};
	//to be added : static int[] runningTimes = {10};		
	//to be updated : 콜렉션수가 늘어나면 loop를 돌리는데, setCommand, matches에도 적용시켜줘야 한다.
	public static void main(String[] args) throws IOException, JSchException {
		int port = 22;
		String host = "10.10.80.134";
		String user = "esadmin";
		String password = "passw0rd";

		//Connect ICC : telnet < SSH used JSch Library (ref : https://code-examples.net/ko-kr/q/2edf10)		
		JSch js = new JSch();
		Session session = js.getSession(user, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("exec"); //channel open with session  (ref : https://epaul.github.io/jsch-documentation/javadoc/com/jcraft/jsch/Session.html#openChannel-java.lang.String-)     
        Channel channelIndex = session.openChannel("exec");
        ChannelExec channelExec = (ChannelExec) channel; //make channel use command 
        ChannelExec channelExecIndex = (ChannelExec) channelIndex; 
        channelExec.setCommand("/opt/IBM/es/bin/esadmin check"); //esadmin command response null(different with ps/df/top), so need to find location
        channelExecIndex.setCommand("/opt/IBM/es/bin/esadmin controller startIndexBuild -cid " + collections[0]); // From KnowledgeCenter (ref : https://www.ibm.com/support/knowledgecenter/en/SS8NLW_12.0.0/com.ibm.discovery.es.ad.doc/iiysarfcomd.html#iiysarfcomd__dcsnonweb)
   
        channelExec.connect();        
        //Check process status 
        BufferedReader br = new BufferedReader(new InputStreamReader(channelExec.getInputStream(), "UTF-8"));
        	String line;
        	while((line = br.readLine()) != null) {
        		//Start IndexProcessor
        		if(line.matches(".*" + collections[0] + ".indexservice" + ".*")) {
        			System.out.println("잘라내기:" + line.substring(line.length()-7, line.length()));
        			if(line.substring(line.length()-7, line.length()).equals("Started")) {
        				//stop indexService 
        				System.out.println("Already It is started");
        			}
					else {
        				channelExecIndex.connect();
						channelExecIndex.disconnect();
						System.out.println("It is started");
					}
        		}
        		
        		//Start Re-build
        		
			}

    		//적당한 시간 간격을 두고 리빌드를 한다.
			//인덱스서비스는 완료시점파악 불가 로그도 없어서 임의의 시간을 배열로하여 돌린다.
			//*리빌드 커맨드가 뭘까?
         channelExec.disconnect();
	}
}
