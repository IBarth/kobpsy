package de.zpid.se4ojs.nlp.nlpPreprocessor.wsd;

import java.util.Arrays;
import java.util.List;

import org.apache.uima.resource.ResourceInitializationException;

import de.zpid.se4ojs.nlp.nlpPreprocessor.JatsXmlReader;

public class TestableJatsXmlReader extends JatsXmlReader {

	@Override
	public List<String> extractElements() throws ResourceInitializationException {
		String[] texts = new String[]{TestAnnotationValidator.SAMPLE_TEXT_1, TestAnnotationValidator.SAMPLE_TEXT_2};
		return Arrays.asList(texts);
	}
	
}