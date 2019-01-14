import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AutoIndexingWithAPI {
	static String AdminUri = "http://10.10.80.134:8390/api/v20/admin";
	static String collection = "test_yhj";
	
	public static void main(String[] args) throws ClientProtocolException, IOException, XPathExpressionException,
			SAXException, ParserConfigurationException, InterruptedException {
		// Get Security Token
		String tokenUri = AdminUri + "/login/token?token=MWI0ZDVkNWYtZjNiZi00MjM1LWJlZDQtMGJmODUzM2Q1ZTgx";
		String securityToken = getSecurityToken(tokenUri);

		// Get Indexer Status
		String getIndexUri = AdminUri + "/collections/indexer/monitor?collection=" + collection + "&securityToken="
				+ securityToken;
		String indexerStatus = checkIndexService(getIndexUri);

		// Start Indexer
		if (indexerStatus.equals("STOPPED")) {
			String startUri = AdminUri + "/collections/indexer/start?collection=" + collection + "&securityToken="
					+ securityToken;
			String startResult = startIndexer(startUri);
			if (startResult.equals("Running")) {
				System.out.println(collection + "의 indexer가 시작됩니다.");
				Thread.sleep(20000);
			}
		} else {
			System.out.println("indexer가 이미 실행중입니다.");
		}

		// Re-build //시간넣기.
		String rebuildUri = AdminUri + "/collections/indexer/subTasks/start?collection=" + collection
				+ "&securityToken=" + securityToken + "&type=RebuildFromCache&mode=Full";
		startRebuild(rebuildUri);
	}

	private static void startRebuild(String rebuildUri) throws ClientProtocolException, IOException {
		System.out.println("rebuild after 20 seconds");
		getXml(rebuildUri);
		System.out.println("rebuilding...");
	}

	private static String startIndexer(String startUri) throws ClientProtocolException, IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {
		String xml;
		xml = getXml(startUri);
		// Parsing
		String result;
		InputSource is = new InputSource(new StringReader(xml.toString()));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//esafter", doc, XPathConstants.NODESET);
		result = nodeList.item(0).getTextContent();
		return result;
	}

	private static String checkIndexService(String getIndexUri) throws ClientProtocolException, IOException,
			SAXException, ParserConfigurationException, XPathExpressionException {
		String xml;
		xml = getXml(getIndexUri);
		// Parsing
		String result;
		InputSource is = new InputSource(new StringReader(xml.toString()));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//esindexer", doc, XPathConstants.NODESET);
		result = nodeList.item(0).getAttributes().getNamedItem("status").getTextContent();
		return result;
	}

	private static String getSecurityToken(String tokenUri) throws ClientProtocolException, IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {
		// HTTP Request
		String xml;
		xml = getXml(tokenUri);
		// Parsing
		String result;
		InputSource is = new InputSource(new StringReader(xml.toString()));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//essecurityToken", doc, XPathConstants.NODESET);
		result = nodeList.item(0).getTextContent();
		return result;
	}

	private static String getXml(String uri) throws ClientProtocolException, IOException {
		HttpClient client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(uri);
		HttpResponse res = client.execute(httpGet);
		BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			line = line.replace("es:", "es");
			sb.append(line);
		}
		return sb.toString();
	}
}
