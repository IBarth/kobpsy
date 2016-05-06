package de.zpid.se4ojs.nlp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;
import org.zpid.se4ojs.textStructure.StructureParser;
import org.zpid.se4ojs.textStructure.bo.BOParagraph;
import org.zpid.se4ojs.textStructure.bo.BOSection;
import org.zpid.se4ojs.textStructure.bo.StructureElement;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.zpid.se4ojs.nlp.type.Section;

/**
 * Reader for files in JATS-XML that
 * initializes some information in the CAS
 * 
 * 
 * @author barth
 */

@TypeCapability(
		outputs = {
			"de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
		    "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
		    "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		    "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
		    "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS" })
public class JatsXmlReader extends ResourceCollectionReaderBase {

	/**
	 * Configuration parameters defined in the descriptor
	 */
	
	/**
     * Write token annotations to the CAS.
     */
	public static final String PARAM_READ_TOKEN = ComponentParameters.PARAM_READ_TOKEN;
	@ConfigurationParameter(name = PARAM_READ_TOKEN, mandatory = true, defaultValue = "true")
	private boolean readToken;

    /**
     * Write part-of-speech annotations to the CAS.
     */
	public static final String PARAM_READ_POS = ComponentParameters.PARAM_READ_POS;
	@ConfigurationParameter(name = PARAM_READ_POS, mandatory = true, defaultValue = "true")
	private boolean readPOS;

    /**
     * Write lemma annotations to the CAS.
     */
	public static final String PARAM_READ_LEMMA = ComponentParameters.PARAM_READ_LEMMA;
	@ConfigurationParameter(name = PARAM_READ_LEMMA, mandatory = true, defaultValue = "true")
	private boolean readLemma;

	/**
	 * Write sentence annotations to the CAS.
	 */
	public static final String PARAM_READ_SENTENCE = ComponentParameters.PARAM_READ_SENTENCE;
	@ConfigurationParameter(name = PARAM_READ_SENTENCE, mandatory = true, defaultValue = "true")
	private boolean readSentence;
	
	public static final String PARAM_READ_PARAGRAPH = "readParagraph";
    @ConfigurationParameter(name = PARAM_READ_PARAGRAPH, mandatory = true, defaultValue = "true")
    private boolean readParagraph;
    
	public static final String PARAM_READ_SECTION = "readSection";
    @ConfigurationParameter(name = PARAM_READ_SECTION, mandatory = true, defaultValue = "true")
    private boolean readSection;

	public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false, defaultValue = "en")
    private String language;

    
	public JatsXmlReader() {
		super();
	}
	
	

	public JatsXmlReader(boolean readParagraph, boolean readSection) {
		this.readParagraph = true;
		this.readSection = true;
	}



	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		ConfigurationParameterInitializer.initialize(this, context);
	}

	/**
	 * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	 * 
	 * Adds the document metadata
	 */
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		Resource res = nextFile();
		initCas(aCAS, res);

		try {
            JCas jCas = aCAS.getJCas();
            List<StructureElement> structureElements = extractTextStructureElements(res.getLocation());
            DocumentRepresentation docRep = new DocumentRepresentation(jCas);
            docRep = buildDocumentText(jCas, structureElements, docRep);
            docRep.addDocumentTextToIndexes();
            jCas.setDocumentLanguage(language);
            getLogger().debug("\n Document text: \n" + jCas.getDocumentText() + "\n\n");
		} catch (CASException e) {
			throw new CollectionException(e);
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	List<StructureElement> extractTextStructureElements(String path) throws ResourceInitializationException {
		
		List<StructureElement> structureElements = new ArrayList<>();
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setFeature("http://xml.org/sax/features/resolve-dtd-uris", false);
		    builder.setFeature("http://xml.org/sax/features/validation", false);
		    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			InputSource is;
			is = new InputSource(path);
			is.setEncoding("UTF-8");
			Document document = (Document) builder.build(is);
			Element rootNode = document.getRootElement();

			StructureParser structureParser = new StructureParser(language);
			structureElements  = structureParser.parse(rootNode, structureElements);

//			TODO add an id element to each paragraph)
			
		} catch (JDOMException | IOException e) {
			String msg = "Unable to parse input xml File. "
					+ path + " cause: " + e.getMessage();
			throw new ResourceInitializationException(
					ResourceInitializationException.COULD_NOT_ACCESS_DATA,
					new Object[] { msg });
		}
		
		return structureElements;
		
	}

	/**
	 * Adds the document text to the jcas.
	 * The document text is built up from the individual text structure elements (top down)
	 * 
	 * @param paragraphTexts
	 * @throws CollectionException 
	 */
	DocumentRepresentation buildDocumentText(JCas jCas, List<StructureElement> container,
			DocumentRepresentation docRep) throws CollectionException {
			
			for (StructureElement se : container) {
				BOSection sec = null;
				String sectionTitle = StringUtils.EMPTY;
				int sectionStart = -1;
				if (se instanceof BOSection) {
					sec = (BOSection) se;
					sectionStart = docRep.getCurrentPos();
					if (!StringUtils.isEmpty(sec.getTitle())) {
						docRep.addText(sec.getTitle());
						sectionTitle = sec.getTitle();
						//TODO annotate section type(s)
					}

					List<StructureElement> childStructures = sec.getChildStructures();
					if (!childStructures.isEmpty()) {
						buildDocumentText(jCas, childStructures, docRep);
						
					}
				} else if (se instanceof BOParagraph) {
					BOParagraph p = (BOParagraph) se;
					String text = p.getText();
					if (text != null) {
						if (readParagraph) {
							docRep.addAnnotation(Paragraph.class, text);
						} 
						docRep.addText(text);
					}
	  				//TODO annotate citations
				}
				if (sec != null && readSection) {
					docRep.addSection(sectionStart, docRep.getCurrentPos(), sectionTitle);
				}
			}
			return docRep;
	}
	
	class DocumentRepresentation {
		
		private static final String TEXT_SEPARATOR = " ";
		private StringBuilder documentTextBuilder;
		private List<Annotation> annotations;
		private JCas jCas;
		
		
		public DocumentRepresentation(JCas jCas) {
			super();
			this.jCas = jCas;
			documentTextBuilder = new StringBuilder();
			annotations = new ArrayList<>();
		}

		public int getCurrentPos() {
			return documentTextBuilder.length();
		}

		void addText(String text) {
			documentTextBuilder.append(text).append(TEXT_SEPARATOR);
		}
		
		Annotation addAnnotation(Class<? extends Annotation> type, String text) throws CollectionException {
			return createAnnotation(type,
					documentTextBuilder.length(), documentTextBuilder.length() + text.length());
		}

		protected Annotation createAnnotation(
				Class<? extends Annotation> type, int startPos, int endPos) throws CollectionException {
			
			Annotation anno = null;
			try {
				Constructor<? extends Annotation> constr = type.getConstructor(JCas.class);
				anno = constr.newInstance(jCas);
			} catch (Exception e) {
				throw new CollectionException(e);
			} 
			anno.setBegin(startPos);
			anno.setEnd(endPos);
			annotations.add(anno);
			return anno;
		}
		
		/**
		 * Adds a section.
		 *  
		 * @param type
		 * @param startPos
		 * @param endPos
		 * @throws CollectionException
		 */
		void addSection(int startPos, int endPos, String title) throws CollectionException {
			Section section = (Section) createAnnotation(Section.class, startPos, endPos);
			if (title != null) {
				section.setTitle(title);
			}
		}
		
		/**
		 * Adds just the annotation - not the covered text.
		 *  
		 * @param type
		 * @param startPos
		 * @param endPos
		 * @throws CollectionException
		 */
		void addAnnotation(Class<? extends Annotation> type, int startPos, int endPos) throws CollectionException {
			createAnnotation(type, startPos, endPos);
		}
 		
		void addDocumentTextToIndexes() {
			jCas.setDocumentText(documentTextBuilder.toString());
			addAnnotationsToIndexes();
		}
		
		private void addAnnotationsToIndexes() {
			for (Annotation anno : annotations) {
				anno.addToIndexes();
			}
		}
		
		String getDocumentText() {
			return documentTextBuilder.toString();
		}
	}

}
