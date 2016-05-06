package de.zpid.se4ojs.nlp.nlpPreprocessor.wsd;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.tudarmstadt.ukp.dkpro.wsd.Pair;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.si.POS;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventory;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventoryException;
import de.tudarmstadt.ukp.dkpro.wsd.si.wordnet.WordNetSenseKeySenseInventory;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.Word;

public class WordSenseDisambiguator {

    private static final String SERIALIZED_GRAPH_LOCATION = "/home/isa/dev/tools/WordNet-3.0/DKProWSD_SK_graph.ser";

	public static void main(String[] args)
	        throws MalformedURLException, JWNLException, IOException,
	        SenseInventoryException, InterruptedException,
	        UnsupportedOperationException, ClassNotFoundException
	    {

		
	        // Create an instance of the WordNet sense inventory. You need to
	        // create an extJWNL properties file and change the value of the
	        // PARAM_WORDNET_PROPERTIES_URL to point to its location on your file
	        // system.
	        WordNetSenseKeySenseInventory inventory = new WordNetSenseKeySenseInventory(
	                        GraphVisualizationExample.class.getClassLoader().getResourceAsStream("extjwnl_properties.xml"));

	        // If you happen to have a serialized WordNet graph, you can specify
	        // its location here and it will be read in. (If no serialized graph is
	        // available, this demo will still work, but it can take several minutes
	        // to compute the graph.)
	        inventory
	            	.setUndirectedGraph(GraphUtil.deserializeGraph(SERIALIZED_GRAPH_LOCATION));

	        GraphUtil.serializeGraph(inventory.getUndirectedGraph(), SERIALIZED_GRAPH_LOCATION);
	        // Instantiate a class for performing the degree centrality algorithm
	        DegreeCentralityWSD wsdAlgorithm = new DegreeCentralityWSD(inventory);

	        // Create a simple sentence whose words will be disambiguated
	        List<Pair<String, POS>> sentence = new ArrayList<Pair<String, POS>>();
//	        sentence.add(new Pair<String, POS>("depression", POS.NOUN));
//	        sentence.add(new Pair<String, POS>("disorder", POS.NOUN));
	        sentence.add(new Pair<String, POS>("drink", POS.NOUN));
	        sentence.add(new Pair<String, POS>("milk", POS.NOUN));
	        // You can add more words if desired...
	        // sentence.add(new Pair<String, POS>("straw", POS.NOUN));

	        // Run the algorithm on the sentence
	        wsdAlgorithm.setSearchDepth(4);
	        Map<Pair<String, POS>, Map<String, Double>> dabMap = wsdAlgorithm
	                .getDisambiguation(sentence);
//	        for (Pair<String, POS> tokenPosPair : sentence) {
//	        	System.out.println("\n\ntoken: " + tokenPosPair);
//	        	System.out.println("Most freq. sense: " + inventory.getMostFrequentSense(tokenPosPair.getFirst()));
//	        	Map<String, Double> map = dabMap.get(tokenPosPair);
	        dabMap.entrySet();
	        for (Entry<Pair<String,POS>, Map<String,Double>> entryDab : dabMap.entrySet()) {
	        	System.out.println("tokenPosPair: " + entryDab.getKey());
	        	for (Entry<String, Double> entry : entryDab.getValue().entrySet()) {
	        		System.out.println(entry.getKey());
	        		System.out.println("wordnet sense Id: " + inventory.getWordNetSenseKey(entry.getKey(), "depression"));
	        		System.out.println("is sense key? " + inventory.isSenseKey(entry.getKey()));
	        		System.out.println(" sense words: " + inventory.getSenseWords(entry.getKey()));

	        		System.out.println("sense examples: " + inventory.getSenseExamples(entry.getKey()));
	        		System.out.println(" sense def: " + inventory.getSenseDefinition(entry.getKey()));
//	        		System.out.println("wn get word by sense key: " + );
	        		System.out.println(entry.getValue() + "\n");
	        		
	        		Word word = inventory.getUnderlyingResource().getWordBySenseKey(entry.getKey());
	        		List<Word> words = word.getSynset().getWords();
	        		System.out.println("words in synset: ");
	        		for (Word member : words) {
	        			System.out.println(member.getLemma());
	        		}

	        	}
	        	System.out.println();
	        }

	    }


}
