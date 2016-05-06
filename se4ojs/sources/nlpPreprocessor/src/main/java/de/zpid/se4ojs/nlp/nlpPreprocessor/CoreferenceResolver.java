/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.zpid.se4ojs.nlp.nlpPreprocessor;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordCoreferenceResolver;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

/**
 * <p>
 * Annnotates the parargraphs of JATS-XML documents with ontology concepts (using NCBOAnnotator).
 * </p> 
 */
public class CoreferenceResolver
{
    protected static final File TARGET_FILE = new File("target/result.txt");

    public void resolveCoreferences()
            throws IOException, UIMAException, SAXException
 {
		CollectionReaderDescription reader = createReaderDescription(
				JatsXmlReader.class,
				JatsXmlReader.PARAM_INPUT_FILE, "./ejcop.v2i2.34.XML",
				JatsXmlReader.PARAM_LANGUAGE, JatsXmlReader.DOC_LANGUAGE_ENGLISH,
				JatsXmlReader.PARAM_XML_ELEMENT_TO_EXTRACT, JatsXmlReader.TAG_NAME_PARAGRAPH);
		AnalysisEngineDescription segmenter = createEngineDescription(StanfordSegmenter.class);
		AnalysisEngineDescription posTagger = createEngineDescription(StanfordPosTagger.class);
		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);
		AnalysisEngineDescription parser = createEngineDescription(StanfordParser.class);
		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription corefResolver = createEngineDescription(
				StanfordCoreferenceResolver.class,
				StanfordCoreferenceResolver.PARAM_POSTPROCESSING, true);

		JCasIterable pipeline = SimplePipeline.iteratePipeline(reader,
				segmenter, posTagger, lemmatizer, parser, ner, corefResolver);
		
		for (JCas jcas : pipeline) {
			Map<Pair<Integer, Integer>, String> corefs = new HashMap<>();
			Collection<CoreferenceChain> chains = JCasUtil.select(jcas,
					CoreferenceChain.class);
			String elementText = jcas.getSofaDataString();
			for (CoreferenceChain chain : chains) {

				CoreferenceLink rootLink = chain.getFirst();
				for (CoreferenceLink link : chain.links().subList(1, chain.links().size())) {
					
					System.out.println(link.getBegin());
					System.out.println(link.getCoveredText());
					System.out.println(link.getEnd());
					Pair<Integer, Integer> pair = new ImmutablePair<Integer, Integer>(link.getBegin(), link.getEnd());

					corefs.put(pair, link.getCoveredText());
				}
			}
			//TODO replace the anaphoras in each paragraph by their "chain antecedent" / root word.
			
//			FileOutputStream fos = new FileOutputStream("out_" + ++idx);
//			XmiCasSerializer.serialize(jcas.getCas(), fos);
		}

	}
    
    public static void main(String[] args)
            throws IOException, UIMAException, SAXException {
    	CoreferenceResolver resolver = new CoreferenceResolver();
    	resolver.resolveCoreferences();
    }
}
