//with characters converted to uppercase.
//Author : Vikas Singh

package concur;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class SortConcurFile extends AbstractTransformation {
    // transform message will be triggered by SAP PI. Here, we call execute
    // which we call during unit testing

    private static String sapsys = System.getProperty("SAPSYSTEMNAME");
    private static String sortingNode = "II";

    public void transform(TransformationInput arg0, TransformationOutput arg1)
	    throws StreamTransformationException {

	this.execute(arg0.getInputPayload().getInputStream(), arg1
		.getOutputPayload().getOutputStream());
    }

    // Common method : can be called from either standalone or SAP
    public void execute(InputStream in, OutputStream out)
	    throws StreamTransformationException {

	try {

	    Element targetConcurElement = null;
	    String sourceXMLtreevalue = null;
	    String targetXMLtreevalue;

	    Document sourceDoc = getDocumentSource(in);
	    Document targetDoc = getTargetDocumentSource();

	    Element sourceRoot = sourceDoc.getDocumentElement();

	    Element targetMessages = targetDoc.createElement("ns0:Messages");
	    targetMessages.setAttribute("xmlns:ns0",
		    "http://sap.com/xi/XI/SplitAndMerge");
	    targetDoc.appendChild(targetMessages);

	    Element targetMessage1 = targetDoc.createElement("ns0:Message1");
	    targetMessages.appendChild(targetMessage1);

	    targetConcurElement = targetDoc.createElement("ns1:MT_concur");
	    targetConcurElement.setAttribute("xmlns:ns1",
		    "http://zimmer.com/concur");
	    targetMessage1.appendChild(targetConcurElement);

	    // Get source header
	    NodeList sourceHeader = sourceRoot.getElementsByTagName("Header");
	    write_trace(
		    "Number of header nodes is " + sourceHeader.getLength(),
		    sapsys);
	    Node sourceHead = sourceHeader.item(0);

	    // Start creating misc. nodes in target
	    write_trace("Adding Recordset", sapsys);
	    Element targetRecordset = targetDoc.createElement("Recordset");
	    targetRecordset.setAttribute("xmlns:ns0", "Recordset");
	    targetConcurElement.appendChild(targetRecordset);
	    write_trace("Recordset Added", sapsys);

	    // Create header
	    Node targetTempdNode = targetDoc.importNode((Element) sourceHead,
		    true);
	    targetRecordset.appendChild(targetTempdNode);

	    NodeList sourceItems = sourceRoot.getElementsByTagName("Item");
	    write_trace(
		    "Number of Item nodes in source tree is "
			    + sourceItems.getLength(), sapsys);

	    int prevcomparatorValue = 0;

	    for (int i = 0; i < sourceItems.getLength(); i++) {
		Node sourceChildNode = sourceItems.item(i);
		sourceXMLtreevalue = extractSortingNodeValue(sourceChildNode);
		NodeList targetItems = targetConcurElement
			.getElementsByTagName("Item");

		Node targetTempdNodeItem = targetDoc.importNode(
			(Element) sourceChildNode, true);

		if (targetItems.getLength() == 0) {
		    write_trace("Creating Target Tree", sapsys);

		    targetRecordset.appendChild(targetTempdNodeItem);

		} else {

		    for (int k = 0; k < targetItems.getLength(); k++) {
			Node targetChildNode = targetItems.item(k);
			targetXMLtreevalue = extractSortingNodeValue(targetChildNode);
			int comparatorVal = targetXMLtreevalue
				.compareTo(sourceXMLtreevalue);

			if (prevcomparatorValue <= 0 && comparatorVal > 0) {
			    write_trace("try to insert : insertBefore ", sapsys);
			    targetRecordset.insertBefore(targetTempdNodeItem,
				    targetChildNode);
			    break;
			}
			targetRecordset.appendChild(targetTempdNodeItem);
			prevcomparatorValue = comparatorVal;
		    }
		}
	    }

	    TransformerFactory transformerFactory = TransformerFactory
		    .newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    DOMSource target = new DOMSource(targetDoc);
	    StreamResult result = new StreamResult(out);
	    transformer.transform(target, result);

	    write_trace("Output is "+out.toString(), sapsys);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private Document getTargetDocumentSource()
	    throws ParserConfigurationException {
	DocumentBuilderFactory targetFactory = DocumentBuilderFactory
		.newInstance();
	DocumentBuilder targetBuilder = targetFactory.newDocumentBuilder();
	Document targetDoc = targetBuilder.newDocument();
	return targetDoc;
    }

    private String extractSortingNodeValue(Node targetChildNode) {

	write_trace("Let's begin the function call ", sapsys);
	String currentNewVal = null;
	NodeList childDataTarget = targetChildNode.getChildNodes();
	write_trace("Number of children " + childDataTarget.getLength(), sapsys);

	boolean foundIt = false;

	for (int l = 0; l < childDataTarget.getLength() && foundIt == false; l++) {
	    String targetItemName = childDataTarget.item(l).getNodeName();
	    if (l == 17) {

		write_trace(
			"Sorting Node value is" + sortingNode + sortingNode,
			sapsys);
		write_trace("TrgetItemName is" + targetItemName
			+ targetItemName, sapsys);
		currentNewVal = getTextValue((Element) targetChildNode,
			targetItemName);
		foundIt = true;
		break;
	    }
	}
	write_trace("currentNewVal is  " + currentNewVal, sapsys);
	return currentNewVal;
    }

    private Document getDocumentSource(InputStream in)
	    throws ParserConfigurationException, SAXException, IOException {
	write_trace("execute - get Document Builder ", sapsys);
	write_trace("System ID is " + sapsys, sapsys);

	// Document Builder for input message
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	write_trace("Factory created", sapsys);

	DocumentBuilder Parser = factory.newDocumentBuilder();
	write_trace("Docbuilder created", sapsys);

	Document source = Parser.parse(in);
	write_trace("Trying to parse", sapsys);
	return source;
    }

    /**
     * I take a xml element and the tag name, look for the tag and get the text
     * content i.e for <employee><name>Vikas</name></employee> xml snippet if
     * the Element points to employee node and tagName is 'name' I will return
     * Vikas
     */
    private String getTextValue(Element ele, String tag) {
	String textVal = null;
	NodeList nl = ele.getElementsByTagName(tag);
	if (nl != null && nl.getLength() > 0) {
	    Element el = (Element) nl.item(0);

	    try {
		textVal = el.getFirstChild().getNodeValue();
	    } catch (Exception e) {
	    }
	}

	return textVal;
    }

    private String setTextValue(Element ele, String tag, String val) {
	String textVal = null;
	NodeList nl = ele.getElementsByTagName(tag);
	if (nl != null && nl.getLength() > 0) {
	    Element el = (Element) nl.item(0);

	    try {
		el.getFirstChild().setNodeValue(val);
	    } catch (Exception e) {
	    }
	}

	return textVal;
    }

    public static void main(String[] args) {
	try {
	    SortConcurFile myMapping = new SortConcurFile();
	    InputStream in = new FileInputStream(new File(
		    "C://PI_JAVA_MAPPINGS//test_data/massaged_test_input2.xml"));
	    OutputStream out = new FileOutputStream(new File(
		    "C://PI_JAVA_MAPPINGS//test_data/out.xml"));
	    myMapping.execute(in, out);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void write_trace(String str, String SID) {
	if (SID == null) {
//	    System.out.println(str);
	} else {
//	    getTrace().addInfo(str);
	}
    }

}



**************************************************************************


public class SortConcurFile extends AbstractTransformation {

    // ---------- declared fields ------ 
    private static String sapsys

    private static String sortingNode


    // ---------- declared constructors ------ 
    public SortConcurFile()


    // ---------- declared methods ------ 
    public static void main(String[])

    public void transform(TransformationInput,TransformationOutput) throws StreamTransformationException

    public void execute(InputStream,OutputStream) throws StreamTransformationException

    private Document getDocumentSource(InputStream) throws ParserConfigurationException,SAXException,IOException

    private String getTextValue(Element,String)

    private void write_trace(String,String)

    private String setTextValue(Element,String,String)

    private Document getTargetDocumentSource() throws ParserConfigurationException

    private String extractSortingNodeValue(Node)


}
