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
 * 1.static ����,�޼��尡 ���̴� ���� : ���α׷� �����  static main�� ���� ����Ǵµ� static�� �ƴ� ������ �޸𸮿��� ã�� �� �����ϱ�
 * 2.ArrayList�� �Ľ��� ��ü�� ��´�.
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
	 * 1.XML ��ü ���� (InpuSource, Document)
	 * 2.XPath ����(XML�Ľ�)
	 * 3.ArrayList�� ChildNode�� ������ ��Ƽ� �����ߴ�.
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
				//�ڽĳ�� value �߰�
				Node parantNode = nodeList.item(idx);
				NodeList childNodeList = parantNode.getChildNodes();
				ArrayList<String> tempList = new ArrayList<>();
				for(int cidx = 0; cidx < childNodeList.getLength(); cidx++) {
					if(childNodeList.item(cidx).getNodeName().equals("#text")) //���౸���� #test��� �����⶧���� ���ܽ�Ų��.
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
	 * [2] 1.try-catch�� �ٷ� �α׸� Ȯ���� �� �ִµ�, throws�ϰԵǸ� 'ȣ���� �޼���'�� ���ܸ� ������ ���ʿ��� ó���ϰ�
	 * �ϰڴٴ� �ǹ̰��ȴٰ� �Ѵ�. try-catch�� �ٷ� Ȯ���ϴ°� ����. 2.HttpResponse�� HttpGet�� �޸�
	 * httpcore.jar���̺귯���� �����ϴ� ��ü�̸� Ctrl+Shift+T�� �ش� ����� Ȯ���� �� �ִ�. 3.response �����
	 * res�� return ��ų �� ������ ��� ��ų� �������Ѿ� �Ѵ�. Stream�� byte������ �����͸� ó��, Reader��
	 * char������ �����͸� ó���ϴµ�, ���ڵ��� �´ٸ� ������ü�� Reader(ctrl+T�� Ȯ��)�� �ᵵ �ȴ�.
	 * InputStreamReader�� ��µ� ���ڷδ� ���� ����? res��ü? �ƴϴ� btye���� �����͸� �ҷ��;��Ѵ�.
	 * 4.BufferedReader�� null�� ���� �� ��� String line�� �����. 5.xml�� ����ϱ����� String��
	 * append�ؾ��ϴµ�, �̶� heap�� String�� �Ҵ��ϴ� �۾��� �ݺ��ȴ�. �׸��� StringBuffer�� ���� �ѹ� �޸𸮿�
	 * �Ҵ�Ǿ� ������ appened�� �Ͼ�⶧���� ȿ���� ����. StringBuffer�� StringBuilder�� ���̴� ��Ƽ������ ����
	 * �����̴�. Buffer�� Syncronized()�� �־ ���� �����尡 �������� ���ϵ��� ����ó���� �� �ִ�.
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
