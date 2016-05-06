package de.zpid.se4ojs.nlp.nlpPreprocessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

/**
 * Reader for files in JATS-XML that
 * initializes some information in the CAS
 * 
 * <p>
 * This class is not thread-safe
 * </p>
 * 
 * @author barth
 */
public class JatsXmlReader extends CollectionReader_ImplBase {
	
	private static final String JATS__1_0_XSD = "/jats-publishing-xsd-1.0/JATS-journalpublishing1.xsd";
	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	/**
	 * Configuration parameter defined in the descriptor
	 */
	public static final String PARAM_INPUT_FILE = "InputFile";
	@ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = true)
	private String inputFile;

	public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false)
    private String language;
    
	public static final String PARAM_XML_ELEMENT_TO_EXTRACT = "element";
    @ConfigurationParameter(name = PARAM_XML_ELEMENT_TO_EXTRACT, mandatory = true)
    private String xmlElement;
    
	public static final String DOC_LANGUAGE_ENGLISH = "en";
	
	/** The tag name for the paragraph element in JATS XML */
	public static final String TAG_NAME_PARAGRAPH = "p";


	
	private final Logger logger = org.apache.log4j.Logger.getLogger(JatsXmlReader.class);
	
	private File file;
	
	private Document doc;
	
	private List<String> paragraphTexts;

	/**
	 * Current paragraph number
	 */
	private int currIdx = -1;
	private String fileName;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {

		fileName = (String) getConfigParameterValue(PARAM_INPUT_FILE);
		file = new File(getClass().getClassLoader().getResource(fileName).getFile());
		if (!file.exists() || file.isDirectory()) {
			throw new ResourceInitializationException(
					ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
					new Object[] { inputFile, "Input file does not exist or is a directory"});
		}
		paragraphTexts = extractElements();
	}

	/**
	 * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	 * 
	 * Adds the document metadata
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
		logger.debug("getNext(CAS) - processing paragraph ");
		String pText = paragraphTexts.get(++currIdx);
//		try {
			DocumentMetaData metaData = null;
			try{
				metaData = DocumentMetaData.create(cas);
			} catch (Exception e) {
				System.out.println(currIdx);
			}
//			if (currIdx == 0) {
//				metaData = DocumentMetaData.create(cas);
//			} else {
//				metaData = DocumentMetaData.get(cas);
//			}
			// Set the document metadata
			metaData.setDocumentTitle(new StringBuilder(fileName).append("_")
					.append(currIdx).toString());
			// docMetaData.setDocumentUri(aResource.getResolvedUri().toString()
			// + qualifier);
			// docMetaData.setDocumentId(aResource.getPath() + qualifier);
			// if (aResource.getBase() != null) {
			// docMetaData.setDocumentBaseUri(aResource.getResolvedBase());
			 metaData.setCollectionId(fileName);
			// }

			// Set the document language
			cas.setDocumentLanguage(DOC_LANGUAGE_ENGLISH);
			cas.setDocumentText(pText);
//		} catch (CASException e) {
//			// This should not happen.
//			throw new RuntimeException(e);
//		}
	}

	/**
	 * @see org.apache.uima.collection.CollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {
		return currIdx < paragraphTexts.size() - 1;
	}


	/**
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
	 */
	public void close() throws IOException {
		//currently nothing to close
	}

	/**
	 * 
	 * Extracts the text from each paragraph of the document.
	 * 
	 * @return the list of paragraphs texts
	 * @throws ResourceInitializationException 
	 */
	public List<String> extractElements() throws ResourceInitializationException {
		List<String> elementTexts = new ArrayList<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		try {
			factory.setFeature("http://xml.org/sax/features/namespaces", false);
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
					false);
			factory.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-external-dtd",
					false);

			factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			String schemaSource = getClass().getResource(JATS__1_0_XSD)
					.toString();
			factory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(file);
			//TODO replace by element param
			NodeList elements = doc.getElementsByTagName(TAG_NAME_PARAGRAPH);
			if (elements.getLength() > 0) {
				for (int i = 0; i < elements.getLength(); i++) {
					Element p = (Element) elements.item(i);
					System.out.println(p.getTextContent()); // TODO delete
					//TODO clean text content, e.g. parenthesis without any textual content
					elementTexts.add(p.getTextContent());
				}
			}
//			TODO add an id element to each paragraph)
//			Transformer xformer = TransformerFactory.newInstance()
//					.newTransformer();
//			xformer.transform(new DOMSource(doc), new StreamResult(outFile));
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			String msg = "Unable to parse input xml File. "
					+ file.getAbsolutePath() + " cause: " + e.getMessage();
			throw new ResourceInitializationException(
					ResourceInitializationException.COULD_NOT_ACCESS_DATA,
					new Object[] { msg });
		}
		
		return elementTexts;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(currIdx,  paragraphTexts.size(), Progress.ENTITIES) };
	}
	
	

}
