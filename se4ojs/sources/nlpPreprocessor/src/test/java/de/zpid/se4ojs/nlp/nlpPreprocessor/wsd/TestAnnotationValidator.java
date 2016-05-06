package de.zpid.se4ojs.nlp.nlpPreprocessor.wsd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventoryException;
import de.zpid.se4ojs.nlp.nlpPreprocessor.JatsXmlReader;
import net.sf.extjwnl.JWNLException;

public class TestAnnotationValidator {

//	private static final String SAMPLE_TEXT = "To manipulate the vendor’s power, priming was used. In previous research, power has been manipulated in various ways, for example,"
//			+ " by varying negotiators’ freedom to act or by giving them a specific organizational status";
	
	static final String SAMPLE_TEXT_1 = "He drank milk.";
	static final String SAMPLE_TEXT_2  = "His depressive disorders vanished.";
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testDisambiguateWordSense() throws 
		UIMAException, SenseInventoryException, UnsupportedOperationException, ClassNotFoundException,
		IOException, SAXException, JWNLException {
		
		AnnotationValidator validator = new AnnotationValidator();

		
//		texts.add(SAMPLE_TEXT);
//		CollectionReader reader = UIMAFramework.produceCollectionReader(validator.createReaderDescription());
//	    JatsXmlReader readerSpy = (JatsXmlReader) spy(reader);
		
//		when(reader.extractElements()).thenReturn(texts);
//		doReturn(texts).when(readerSpy).extractElements();
//		JatsXmlReader reader = mock(JatsXmlReader.class);
//		when(reader.hasNext()).thenReturn(true).thenReturn(false);
//		when(reader.extractElements()).thenReturn(texts);
//		when(reader.getMetaData()).thenReturn(mock(ResourceMetaData.class));
//		ProcessingResourceMetaData metaData = mock(ProcessingResourceMetaData.class);
//		when(reader.getProcessingResourceMetaData())
//		   .thenReturn(metaData);
//		when(metaData.clone()).thenReturn(metaData);

		CollectionReaderDescription readerDescription = 
			CollectionReaderFactory.createReaderDescription(
					TestableJatsXmlReader.class,
					JatsXmlReader.PARAM_INPUT_FILE, "./ejcop.v2i2.34.XML",
					JatsXmlReader.PARAM_LANGUAGE, JatsXmlReader.DOC_LANGUAGE_ENGLISH,
					JatsXmlReader.PARAM_XML_ELEMENT_TO_EXTRACT, JatsXmlReader.TAG_NAME_PARAGRAPH);
		


		final JCasIterator i = new JCasIterator(produceReader(readerDescription), 
				produceAE(validator.createSegmenterDescription()),
				produceAE(validator.createTaggerDescription()),
				produceAE(validator.createLemmatizerDescription()),
				produceAE(validator.createWsdItemCreator()),
				produceAE(validator.createDegreeCentralityDisambiguator()));
		
		i.setSelfComplete(true);
		i.setSelfDestroy(true);
		
		validator.inspectPipeline(new Iterable<JCas>() {

			@Override
			public Iterator<JCas> iterator() {
				return i;
			}
			
		});
	}

	private CollectionReader produceReader(CollectionReaderDescription readerDescription) throws ResourceInitializationException {
		return UIMAFramework.produceCollectionReader(readerDescription);
	}

	private AnalysisEngine produceAE(AnalysisEngineDescription desc) throws ResourceInitializationException {
		return UIMAFramework.produceAnalysisEngine(desc, UIMAFramework.newDefaultResourceManager()
				, null);
	}

}

	
