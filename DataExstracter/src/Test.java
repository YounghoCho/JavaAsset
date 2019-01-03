/**
 * [Name of owner] Copyright(C) 2017-, All rights reserved.
 */
package com.ibm.klab;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
/**
 * @author UihyunKim (uihyun@kr.ibm.com) 2018. 2. 15.
 * @version $Id$
 */
public class AssessmentForWEX {
  private static final String PORT = "8393";
  private static final String PREVIEW_API_PATH = "/api/v10/search/preview";
  private static String HOSTNAME = "10.40.76.231";
  private static String SEARCH_URL = "http://" + HOSTNAME + ":" + PORT;
  private static final String UTF8 = "utf-8";
  private static final String FORMAT_XML = "application/xml";
  private static final String PARAM_COLLECTION = "collection";
  private static final String PARAM_QUERY = "query";
  private static final String PARAM_OUTPUT = "output";
  private static final String PARAM_URI = "uri";
  private static String OPTION_COLLECTION = "DA_Refrigerator_SNS";
  private static String OPTION_QUERY = "*:*";
  private static String OPTION_FILE = "uid.txt";
  private static String OPTION_RESULT = "result";
  /**
   * @param args
   */
  public static void main(String[] args) {
    BufferedReader br = null;
    BufferedWriter bw = null;
    InputSource is;
    PostMethod analyzerPostMethod;
    RequestEntity analyzerRequestEntity;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder;
    Document doc;
    System.out.println("Start\n");
    checkOptions(args);
    File resultFile = new File(OPTION_RESULT);
    String uid;
    String viewName;
    String uri;
    String resultXML;
    String readLine;
    int errorCount = 0;
    List<AnalyzedDoc> analyzedDocList = new ArrayList<AnalyzedDoc>();
    try {
      File file = new File(OPTION_FILE);
      br = new BufferedReader(new FileReader(file));
      while ((readLine = br.readLine()) != null) {
        if (readLine.trim().length() != 0) {
          uid = readLine.trim();
          try {
            viewName = getViewName(uid);
            uri = "jdbc://jdbc%3Amysql%3A%2F%2F10.40.87.213%3A3306%2FVDDA_TRANS_DATA%3FcharacterEncoding%3DUTF-8%26useLegacyDatetimeCode%3Dfalse%26serverTimezone%3DAsia%2FSeoul%26useSSL%3Dtrue" +
                  "/VDDA_TRANS_DATA." + viewName + "/UID/" + uid;
            final HttpClient client = new HttpClient();
            analyzerPostMethod = new PostMethod(SEARCH_URL + PREVIEW_API_PATH);
            Part[] analyzerPartsPreview = { new StringPart(PARAM_COLLECTION, OPTION_COLLECTION,
                                                           UTF8),
                                            new StringPart(PARAM_QUERY, OPTION_QUERY, UTF8),
                                            new StringPart(PARAM_URI, uri, UTF8),
                                            new StringPart(PARAM_OUTPUT, FORMAT_XML, UTF8) };
            analyzerRequestEntity = new MultipartRequestEntity(analyzerPartsPreview,
                                                               analyzerPostMethod.getParams());
            analyzerPostMethod.setRequestEntity(analyzerRequestEntity);
            resultXML = execute(client, analyzerPostMethod);
            resultXML = resultXML.replace("es:", "es");
            is = new InputSource(new StringReader(resultXML));
            builder = factory.newDocumentBuilder();
            doc = builder.parse(is);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            AnalyzedDoc analyzedDoc = new AnalyzedDoc();
            analyzedDoc.setUid(uid);
            XPathExpression expr = xPath.compile("//esapiResponse/esdocumentLevelFacets/esfacet");
            NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            getMetadata(nodeList, analyzedDoc);
            expr = xPath.compile("//esapiResponse/estext");
            nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            getText(nodeList, analyzedDoc);
            expr = xPath.compile("//esapiResponse/esviews/esview");
            nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            getTitleContnet(nodeList, analyzedDoc);
            expr = xPath.compile("//esapiResponse/estextAnnotationFacets/esfacet");
            nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            getAnalysisFacets(nodeList, analyzedDoc);
            analyzedDocList.add(analyzedDoc);
          } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("error uid: " + uid);
            errorCount++;
          }
        }
      }
      sortAnalyzedDoc(analyzedDocList);
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      bw = new BufferedWriter(new FileWriter(resultFile + "_" +
                                             simpleDateFormat.format(System.currentTimeMillis()) +
                                             ".csv"));
      makeCSVFile(bw, analyzedDocList);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    } finally {
      try {
        if (bw != null)
          bw.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
    System.out.println("\nerrorCount: " + errorCount);
    System.out.println("End");
  }
  private static void sortAnalyzedDoc(List<AnalyzedDoc> analyzedDocList) {
    for (AnalyzedDoc analyzedDoc : analyzedDocList) {
      List<AnalyzedPart> analyzedParts = analyzedDoc.getAnalyzedParts();
      Collections.sort(analyzedParts, new Comparator<AnalyzedPart>() {
        @Override
        public int compare(AnalyzedPart o1, AnalyzedPart o2) {
          if (o1.getBegin() > o2.getBegin())
            return 1;
          else if (o1.getBegin() < o2.getBegin())
            return -1;
          else
            return 0;
        }
      });
      analyzedDoc.setAnalyzedParts(analyzedParts);
    }
  }
  private static void checkOptions(String[] args) {
    for (String option : args) {
      if (option.startsWith("ip")) {
        HOSTNAME = option.substring(option.indexOf("=") + 1, option.length());
      } else if (option.startsWith("collection")) {
        OPTION_COLLECTION = option.substring(option.indexOf("=") + 1, option.length());
      } else if (option.startsWith("query")) {
        option = option.substring(option.indexOf("=") + 1, option.length());
        OPTION_QUERY = option.substring(option.indexOf("'") + 1, option.lastIndexOf("'"));
      } else if (option.startsWith("file")) {
        OPTION_FILE = option.substring(option.indexOf("=") + 1, option.length());
      } else if (option.startsWith("result")) {
        OPTION_RESULT = option.substring(option.indexOf("=") + 1, option.length());
      }
    }
    System.out.println("Options");
    System.out
        .println("==============================================================================");
    System.out.println("host: " + HOSTNAME);
    System.out.println("collection: " + OPTION_COLLECTION);
    System.out.println("query: " + OPTION_QUERY);
    System.out.println("file: " + OPTION_FILE);
    System.out.println("result: " + OPTION_RESULT);
    System.out
        .println("==============================================================================");
  }
  private static String getViewName(String uid) {
    StringBuilder sb = new StringBuilder();
    boolean isForum = false;
    if (uid.contains("MAIN")) {
      sb.append("main");
    } else if (uid.contains("FORUM")) {
      sb.append("forum");
      isForum = true;
    } else if (uid.contains("STORE")) {
      sb.append("store");
    } else if (uid.contains("FACEBOOK")) {
      sb.append("facebook");
    } else if (uid.contains("INSTAGRAM")) {
      sb.append("instgram");
    } else if (uid.contains("TWITTER")) {
      sb.append("twitter");
    } else if (uid.contains("YOUTUBE")) {
      sb.append("youtube");
    }
    if (!isForum) {
      if (uid.substring(uid.indexOf('_') + 1, uid.length()).indexOf('_') > -1)
        sb.append("_reply");
      else
        sb.append("_content");
    }
    return sb.toString();
  }
  private static String execute(HttpClient client, HttpMethodBase method) {
    try {
      System.out.println();
      System.out.println("Connect to: " + method.getURI());
      System.out.println();
      client.executeMethod(method);
      final Reader reader = new InputStreamReader(method.getResponseBodyAsStream(), UTF8);
      final StringBuilder buff = new StringBuilder();
      try {
        final char[] chars = new char[4096];
        for (;;) {
          final int len = reader.read(chars);
          if (len < 0)
            break;
          buff.append(chars, 0, len);
        }
      } finally {
        reader.close();
      }
      buff.trimToSize();
      return buff.toString();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      method.releaseConnection();
    }
    return null;
  }
  private static void getMetadata(NodeList nodeList, AnalyzedDoc analyzedDoc) throws IOException {
    String product = "";
    for (int i = 0; i < nodeList.getLength(); i++) {
      boolean isSite = false;
      boolean isProduct = false;
      boolean isModelCode = false;
      Node node = nodeList.item(i);
      if (node.getAttributes() != null) {
        if (node.getAttributes().getNamedItem("id").getTextContent()
            .startsWith("$.structured_facet.site") &&
            !node.getAttributes().getNamedItem("id").getTextContent()
                .startsWith("$.structured_facet.site_menu")) {
          isSite = true;
        } else if (node.getAttributes().getNamedItem("id").getTextContent()
            .startsWith("$.structured_facet.product")) {
          isProduct = true;
        } else if (node.getAttributes().getNamedItem("id").getTextContent()
            .startsWith("$.structured_facet.model_code")) {
          isModelCode = true;
        }
      }
      NodeList childList = node.getChildNodes();
      if (childList != null) {
        for (int j = 0; j < childList.getLength(); j++) {
          Node valueNode = childList.item(j);
          if (valueNode.getNodeName().equals("esfacetValue")) {
            if (valueNode.getAttributes() != null) {
              if (isSite) {
                System.out
                    .println("site name: " +
                             valueNode.getAttributes().getNamedItem("label").getTextContent());
                analyzedDoc
                    .setSiteName(valueNode.getAttributes().getNamedItem("label").getTextContent());
              } else if (isProduct) {
                product = valueNode.getAttributes().getNamedItem("label").getTextContent();
                System.out.println("product: " + product);
                if (product.equals("WF")) {
                  analyzedDoc.setProduct("Washer");
                } else if (product.equals("RF")) {
                  analyzedDoc.setProduct("Refrigerator");
                } else {
                  analyzedDoc.setProduct("Both");
                }
              } else if (isModelCode) {
                System.out
                    .println("model code: " +
                             valueNode.getAttributes().getNamedItem("label").getTextContent());
                analyzedDoc
                    .setModelCode(valueNode.getAttributes().getNamedItem("label").getTextContent());
              }
            }
          }
        }
      }
    }
  }
  private static void getText(NodeList nodeList, AnalyzedDoc analyzedDoc) throws IOException {
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      System.out.println("text: " + node.getTextContent());
      analyzedDoc.setText(node.getTextContent());
    }
  }
  private static void getTitleContnet(NodeList nodeList, AnalyzedDoc analyzedDoc)
      throws IOException {
    String reply = "";
    String title = "";
    String content = "";
    int begin = 0;
    int end = 0;
    for (int i = 0; i < nodeList.getLength(); i++) {
      boolean isReply = false;
      boolean isTitle = false;
      boolean isContent = false;
      Node node = nodeList.item(i);
      if (node.getAttributes() != null) {
        if (node.getAttributes().getNamedItem("id").getTextContent().equals("reply")) {
          isReply = true;
        } else if (node.getAttributes().getNamedItem("id").getTextContent().equals("title")) {
          isTitle = true;
        } else if (node.getAttributes().getNamedItem("id").getTextContent().equals("content")) {
          isContent = true;
        }
      }
      NodeList childList = node.getChildNodes();
      if (childList != null) {
        for (int j = 0; j < childList.getLength(); j++) {
          Node offsetNode = childList.item(j);
          if (offsetNode.getNodeName().equals("esoffset")) {
            if (offsetNode.getAttributes() != null) {
              if (isReply) {
                begin = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("start").getTextContent());
                end = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("end").getTextContent());
                reply = analyzedDoc.getText().substring(begin, end);
                System.out.println("reply content: " + reply);
                analyzedDoc.setReplyBegin(begin);
                analyzedDoc.setReplyEnd(end);
                analyzedDoc.setReply(reply);
              } else if (isTitle) {
                begin = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("start").getTextContent());
                end = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("end").getTextContent());
                title = analyzedDoc.getText().substring(begin, end);
                System.out.println("title: " + title);
                analyzedDoc.setTitleBegin(begin);
                analyzedDoc.setTitleEnd(end);
                analyzedDoc.setTitle(title);
              } else if (isContent) {
                begin = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("start").getTextContent());
                end = Integer
                    .parseInt(offsetNode.getAttributes().getNamedItem("end").getTextContent());
                content = analyzedDoc.getText().substring(begin, end);
                System.out.println("content: " + content);
                analyzedDoc.setContentBegin(begin);
                analyzedDoc.setContentEnd(end);
                analyzedDoc.setContent(content);
              }
            }
          }
        }
      }
    }
  }
  private static void getAnalysisFacets(NodeList nodeList, AnalyzedDoc analyzedDoc)
      throws IOException {
    String facetId = "";
    String facetName = "";
    String facetValue = "";
    int begin = 0;
    int end = 0;
    AnalyzedPart analyzedPart = null;
    List<AnalyzedPart> analyzedParts = new ArrayList<AnalyzedPart>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getAttributes() != null) {
        facetId = node.getAttributes().getNamedItem("id").getTextContent();
        facetName = node.getAttributes().getNamedItem("label").getTextContent();
        if (facetId.startsWith("$._word") || facetId.startsWith("$._phrase") ||
            facetId.startsWith("$.structured_facet") ||
            facetId.startsWith("$._sentiment.expression") ||
            facetId.startsWith("$._sentiment.target"))
          continue;
        System.out.println("\tfacet id: " + facetId);
        if (facetId.startsWith("$.analysis_facet.laundry.product_type_context.project"))
          facetName = "Project";
        else
          facetName = node.getAttributes().getNamedItem("label").getTextContent();
        System.out.println("\tfacet label: " + facetName);
      }
      NodeList childList = node.getChildNodes();
      if (childList != null) {
        for (int j = 0; j < childList.getLength(); j++) {
          Node valueNode = childList.item(j);
          if (valueNode.getNodeName().equals("esfacetValue")) {
            if (valueNode.getAttributes() != null) {
              facetValue = valueNode.getAttributes().getNamedItem("label").getTextContent();
              System.out.println("\t\tfacet value: " + facetValue);
            }
          }
          NodeList grandChildList = valueNode.getChildNodes();
          if (grandChildList != null) {
            for (int k = 0; k < grandChildList.getLength(); k++) {
              analyzedPart = new AnalyzedPart();
              Node offsetNode = grandChildList.item(k);
              if (offsetNode.getNodeName().equals("esoffset")) {
                if (offsetNode.getAttributes() != null) {
                  begin = Integer
                      .parseInt(offsetNode.getAttributes().getNamedItem("start").getTextContent());
                  System.out.println("\t\t\tfacet begin: " + begin);
                  end = Integer
                      .parseInt(offsetNode.getAttributes().getNamedItem("end").getTextContent());
                  System.out.println("\t\t\tfacet end: " + end);
                  analyzedPart.setId(facetId);
                  analyzedPart.setName(facetName);
                  analyzedPart.setValue(facetValue);
                  analyzedPart.setBegin(begin);
                  analyzedPart.setEnd(end);
                  analyzedParts.add(analyzedPart);
                }
              }
            }
          }
        }
      }
    }
    analyzedDoc.setAnalyzedParts(analyzedParts);
  }
  private static void makeCSVFile(BufferedWriter bw, List<AnalyzedDoc> analyzedDocList)
      throws IOException {
    bw.write("UID,Product,Site,Model Code,Title,Content,Reply,Contextual View,Phrase/Word,WEX Facet-Main,WEX Facet-Sub,WEX Facet Value");
    bw.newLine();
    bw.flush();
    for (AnalyzedDoc analyzedDoc : analyzedDocList) {
      String text = analyzedDoc.getText();
      List<AnalyzedPart> analyzedParts = analyzedDoc.getAnalyzedParts();
      for (AnalyzedPart analyzedPart : analyzedParts) {
        bw.write(makeDoubleQuo(analyzedDoc.getUid()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getProduct()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getSiteName()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getModelCode()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getTitle()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getContent()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedDoc.getReply()));
        bw.write(",");
        bw.write(makeDoubleQuo(checkContextualView(analyzedDoc, analyzedPart)));
        bw.write(",");
        bw.write(makeDoubleQuo(text.substring(analyzedPart.getBegin(), analyzedPart.getEnd())));
        bw.write(",");
        bw.write(makeDoubleQuo(checkMainFacet(analyzedPart)));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedPart.getName()));
        bw.write(",");
        bw.write(makeDoubleQuo(analyzedPart.getValue()));
        bw.newLine();
        bw.flush();
      }
    }
  }
  private static String checkContextualView(AnalyzedDoc analyzedDoc, AnalyzedPart analyzedPart) {
    int begin = analyzedPart.getBegin();
    int end = analyzedPart.getEnd();
    if (begin >= analyzedDoc.getContentBegin() && end <= analyzedDoc.getContentEnd()) {
      return "Content";
    } else if (begin >= analyzedDoc.getReplyBegin() && end <= analyzedDoc.getReplyEnd()) {
      return "Reply";
    } else if (begin >= analyzedDoc.getTitleBegin() && end <= analyzedDoc.getTitleEnd()) {
      return "Title";
    }
    return "";
  }
  private static String checkMainFacet(AnalyzedPart analyzedPart) {
    String facetId = analyzedPart.getId();
    String praentFacet = "";
    if (facetId.startsWith("$.analysis_facet.laundry.product_type_context")) {
      praentFacet = "Product Type in Context";
    } else if (facetId.startsWith("$.analysis_facet.laundry.product_feature")) {
      praentFacet = "Product Feature";
    } else if (facetId.startsWith("$.analysis_facet.laundry.customer_satisfaction")) {
      praentFacet = "Customer Satisfaction";
    } else if (facetId.startsWith("$.analysis_facet.laundry.sales_marketing")) {
      praentFacet = "Sales & Marketing";
    } else if (facetId.startsWith("$.analysis_facet.laundry.lifestyle")) {
      praentFacet = "Lifestyle";
    } else if (facetId.startsWith("$.analysis_facet.laundry.price_effect")) {
      praentFacet = "Price Effect";
    } else if (facetId.startsWith("$.analysis_facet.laundry.trademark_feature")) {
      praentFacet = "Trademark Feature";
    } else if (facetId.startsWith("$.analysis_facet.further_navigation")) {
      praentFacet = "Further Navigation";
    }
    if (facetId.startsWith("$.analysis_facet.refrigerator.product_type_context")) {
      praentFacet = "Product Type in Context";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.product_feature")) {
      praentFacet = "Product Feature";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.customer_satisfaction")) {
      praentFacet = "Customer Satisfaction";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.sales_marketing")) {
      praentFacet = "Sales & Marketing";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.lifestyle")) {
      praentFacet = "Lifestyle";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.price_effect")) {
      praentFacet = "Price Effect";
    } else if (facetId.startsWith("$.analysis_facet.refrigerator.trademark_feature")) {
      praentFacet = "Trademark Feature";
    } else if (facetId.startsWith("$.analysis_facet.further_navigation")) {
      praentFacet = "Further Navigation";
    }
    if (praentFacet.length() != 0)
      return praentFacet;
    else
      return analyzedPart.getName();
  }
  private static String makeDoubleQuo(String str) {
    str = str.replace("\"", "\"\"");
    return "\"" + str + "\"";
  }
}

