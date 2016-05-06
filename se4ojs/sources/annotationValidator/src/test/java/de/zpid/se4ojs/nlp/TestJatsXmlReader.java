package de.zpid.se4ojs.nlp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.hamcrest.text.IsEqualIgnoringWhiteSpace;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.zpid.se4ojs.textStructure.bo.StructureElement;

import com.google.common.io.Files;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.zpid.se4ojs.nlp.JatsXmlReader.DocumentRepresentation;
import de.zpid.se4ojs.nlp.type.Section;
public class TestJatsXmlReader {

	private static final String DUMMY_TEXT = "src/test/resources/psychOpenTestDocs/dummy.xml";
	private static final String PARAGRAPH_TAG_NAME = "p";
	private static final String SECTION_TAG_NAME = "sec";
	private static final String TARGET_DIR = "/media/sf_shared/psychOpenTestAnnotation";
	private static final String INPUT_TEXT = "ejop.v10i3.753.xml";

	//TODO test AnnotateSections but not paragraphs
	//TODO test Annotate paragraphs but not sections
	//TODO test Section title
	//TODO implement and test Section type(s)
	
	@Test
	public void testAnnotateParagraphs() throws UIMAException, JDOMException, IOException {
		List<Element> paragraphTexts = extractParagraphText(DUMMY_TEXT);
		JatsXmlReader reader = new JatsXmlReader(true, false);
		List<StructureElement>  structEls = reader.extractTextStructureElements(DUMMY_TEXT);
		JCas jCas = JCasFactory.createJCas();
		DocumentRepresentation docRep = reader.buildDocumentText(jCas, structEls,
				reader.new DocumentRepresentation(jCas));
		docRep.addDocumentTextToIndexes();
		Collection<Paragraph> paragraphAnnotations = JCasUtil.select(jCas, Paragraph.class);
		assertEquals("Wrong number of paragraphs extracted: ", paragraphTexts.size(), paragraphAnnotations.size());
		
		Iterator<Paragraph> annotIt = paragraphAnnotations.iterator();
   		Iterator<Element> textIt = paragraphTexts.iterator();
		while (annotIt.hasNext()) {

			Element text = textIt.next();
			System.out.println("parsed P : \n" + text.getValue());
			Paragraph annotText = annotIt.next();
			System.out.println("jCas P : \n" + annotText.getCoveredText());
			assertEquals("Paragraph not correctly identified: ", text.getValue(),
					annotText.getCoveredText());
		}
	}
	@Test
	public void testAnnotateSections() throws UIMAException, JDOMException, IOException {
		List<Element> sectionTexts = extractSectionText(DUMMY_TEXT);
		JatsXmlReader reader = new JatsXmlReader(true, true);
		List<StructureElement>  structEls = reader.extractTextStructureElements(DUMMY_TEXT);
		JCas jCas = JCasFactory.createJCas();
		DocumentRepresentation docRep = reader.buildDocumentText(jCas, structEls,
				reader.new DocumentRepresentation(jCas));
		docRep.addDocumentTextToIndexes();
		Collection<Section> sectionAnnotations = JCasUtil.select(jCas, Section.class);
		assertEquals("Wrong number of sections extracted: ", sectionTexts.size(), sectionAnnotations.size());
		
		Iterator<Section> annotIt = sectionAnnotations.iterator();
   		Iterator<Element> textIt = sectionTexts.iterator();
		while (annotIt.hasNext()) {
			Element text = textIt.next();
			System.out.println("parsed S : \n" + text.getValue());
			Section annotText = annotIt.next();
			System.out.println("jCas Sec : \n" + annotText.getCoveredText());
			assertThat(text.getValue(),
					IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace(annotText.getCoveredText()));
		}
	}
	
	@Test
	public void dumpDocumentText() throws UIMAException, JDOMException, IOException {
		JatsXmlReader reader = new JatsXmlReader(true, true);
		List<StructureElement>  structEls = reader.extractTextStructureElements(TARGET_DIR + "/" + INPUT_TEXT);
		JCas jCas = JCasFactory.createJCas();
		DocumentRepresentation docRep = reader.buildDocumentText(jCas, structEls,
				reader.new DocumentRepresentation(jCas));
		Files.write(docRep.getDocumentText(), 
				new File(TARGET_DIR + "/" + INPUT_TEXT.replace(".xml", ".txt")), Charset.forName("UTF-8"));
	}
	
	private List<Element> extractSectionText(String dummyText) throws JDOMException, IOException {
		Element rootNode = extractRootElement();
		return extractNestedChildElements(rootNode, SECTION_TAG_NAME, new ArrayList<Element>());
		
	}
	
	private List<Element> extractParagraphText(String dummyText) throws JDOMException, IOException {
		Element rootNode = extractRootElement();
		return extractNestedChildElements(rootNode, PARAGRAPH_TAG_NAME, new ArrayList<Element>());
	}


	protected Element extractRootElement() throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		builder.setFeature("http://xml.org/sax/features/resolve-dtd-uris", false);
	    builder.setFeature("http://xml.org/sax/features/validation", false);
	    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
	    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		InputSource is;
		is = new InputSource(DUMMY_TEXT);
		is.setEncoding("UTF-8");
		Document document = (Document) builder.build(is);
		Element rootNode = document.getRootElement();
		return rootNode;
	}

	private List<Element> extractNestedChildElements(Element parent, String tagName, List<Element> extractedElements) {
		for (Element e : parent.getChildren()) {
			if (e.getName().equals(tagName)) {
				extractedElements.add(e);
			} 
			extractNestedChildElements(e, tagName, extractedElements);
		}
		return extractedElements;
	}

}
