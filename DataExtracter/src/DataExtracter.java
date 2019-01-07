import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*[1]
 * 1.static 변수,메서드가 쓰이는 이유 : 프로그램 실행시  static main이 먼저 실행되는데 static이 아닌 변수는 메모리에서 찾을 수 없으니까
 * 2.ArrayList에 파싱한 객체를 담는다.
 */
public class DataExtracter {
	private static String uri = "http://10.10.80.134:8393/api/v10/search/preview?collection=Social_Listening_SNS&query=*:*&output=application/xml&uri=csv%3A%2F%2F%252Fhome%252Fesadmin%252Fdata%252Ftwitter%252Fsns_twitter_all_2017.csv%3Fseq%3Dwasher_2017-1-10_2";

	public static void main(String[] args) throws ClientProtocolException, IOException, XPathExpressionException, ParserConfigurationException {
		HttpClient client = HttpClients.createDefault();
		String result = sendRequest(client, uri);
		List<AnalyzedDoc> DocList = new ArrayList<AnalyzedDoc>();
		
		parseData(result, DocList);
		//for(int i=0; i<DocList.size(); i++) System.out.println(DocList.get(i).getList().get(0));			
		writeCsv(DocList);
	}
	private static void writeCsv(List<AnalyzedDoc> DocList) throws IOException {
		BufferedWriter fw = new BufferedWriter(new FileWriter("test.csv"));
		//header
		fw.write("FacetId");
		fw.write(",");
		fw.write("FacetName");
		fw.write(",");
		fw.write("FacetValue");
		fw.newLine();
		
		for(int i=0; i<DocList.size(); i++) {
			for(int j=0; j<DocList.get(i).getList().size(); j++) {
				fw.write(DocList.get(i).getId());
				fw.write(",");
				fw.write(DocList.get(i).getName());
				fw.write(",");
				fw.write(DocList.get(i).getList().get(j));
				fw.newLine();
			}
		}
		fw.flush();
		fw.close();
		System.out.println("Finished");
	}
	/*
	 * [3]
	 * 1.XML 객체 생성 (InpuSource, Document)
	 * 2.XPath 생성(XML파싱)
	 * 3.ArrayList에 ChildNode의 값들을 담아서 추출했다.
	 */
	private static void parseData(String result, List<AnalyzedDoc> DocList) throws IOException, ParserConfigurationException, XPathExpressionException {
		result = result.replace("es:", "es");
		InputSource is = new InputSource(new StringReader(result));
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate("//esfacet", doc, XPathConstants.NODESET);
			for(int idx = 0; idx < nodeList.getLength(); idx++) {	//31
				AnalyzedDoc analyzedDoc = new AnalyzedDoc();
				analyzedDoc.setId(nodeList.item(idx).getAttributes().getNamedItem("id").getTextContent());
				analyzedDoc.setName(nodeList.item(idx).getAttributes().getNamedItem("label").getTextContent());
				//자식노드 value 추가
				Node parantNode = nodeList.item(idx);
				NodeList childNodeList = parantNode.getChildNodes();
				ArrayList<String> tempList = new ArrayList<>();
				for(int cidx = 0; cidx < childNodeList.getLength(); cidx++) {
					if(childNodeList.item(cidx).getNodeName().equals("#text")) //개행구간이 #test라고 들어오기때문에 제외시킨다.
						continue;
					else 
						tempList.add(childNodeList.item(cidx).getAttributes().getNamedItem("label").getTextContent());
				}
				analyzedDoc.setList(tempList);
				DocList.add(analyzedDoc);
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/*
	 * [2] 1.try-catch는 바로 로그를 확인할 수 있는데, throws하게되면 '호출한 메서드'로 예외를 던져서 그쪽에서 처리하게
	 * 하겠다는 의미가된다고 한다. try-catch로 바로 확인하는게 좋다. 2.HttpResponse는 HttpGet과 달리
	 * httpcore.jar라이브러리에 존재하는 객체이며 Ctrl+Shift+T로 해당 사실을 확인할 수 있다. 3.response 결과는
	 * res로 return 시킬 순 없으니 어디에 담거나 변형시켜야 한다. Stream은 byte단위로 데이터를 처리, Reader는
	 * char단위로 데이터를 처리하는데, 인코딩이 맞다면 상위객체인 Reader(ctrl+T로 확인)를 써도 된다.
	 * InputStreamReader을 썼는데 인자로는 뭐가 몰까? res객체? 아니다 btye단위 데이터를 불러와야한다.
	 * 4.BufferedReader는 null과 비교할 수 없어서 String line을 만든다. 5.xml을 출력하기위해 String을
	 * append해야하는데, 이때 heap에 String을 할당하는 작업이 반복된다. 그리나 StringBuffer를 쓰면 한번 메모리에
	 * 할당되어 데이터 appened만 일어나기때문에 효율이 좋다. StringBuffer와 StringBuilder의 차이는 멀티쓰레딩 지원
	 * 여부이다. Buffer는 Syncronized()가 있어서 여러 쓰레드가 동시접근 못하도록 순차처리할 수 있다.
	 */
	private static String sendRequest(HttpClient client, String uri) throws ClientProtocolException, IOException {
		HttpGet getUri = new HttpGet(uri);
		HttpResponse res = client.execute(getUri);
		BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));
		StringBuilder sb = new StringBuilder();
	
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

}
