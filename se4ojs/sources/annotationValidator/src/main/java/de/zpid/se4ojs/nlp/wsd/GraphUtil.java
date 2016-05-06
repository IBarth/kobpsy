package de.zpid.se4ojs.nlp.wsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.tudarmstadt.ukp.dkpro.wsd.UnorderedPair;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class GraphUtil {
	
    public static final String SERIALIZED_GRAPH_LOCATION = "/home/isa/dev/tools/WordNet-3.0/DKProWSD_SK_graph.ser";

    /**
     * Reads in a serialized WordNet graph.
     *
     * @return the WordNet graph
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static UndirectedGraph<String, UnorderedPair<String>> deserializeGraph(
            String serializedGraphFilename)
        throws IOException, ClassNotFoundException
    {
        System.out.println("Reading graph...");
        File graphfile = new File(serializedGraphFilename);
        if (graphfile.exists() == false) {
            return null;
        }
        FileInputStream fileIn = new FileInputStream(graphfile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        UndirectedGraph<String, UnorderedPair<String>> g;
        g = (UndirectedGraph<String, UnorderedPair<String>>) in.readObject();
        in.close();
        fileIn.close();
        System.out.println("Read a graph with " + g.getEdgeCount()
                + " edges and " + g.getVertexCount() + " vertices");
        return g;
    }
    
    public static void serializeGraph(UndirectedGraph<String, UnorderedPair<String>> graph, String graphLocation) throws IOException {
    	File f = new File(graphLocation);
    	if (graph != null && !f.exists()) {
    		System.out.println("serializing graph ...");
        	FileOutputStream fos = new FileOutputStream(new File(graphLocation));
        	ObjectOutputStream oos = new ObjectOutputStream(fos);
        	oos.writeObject(graph);
        	oos.close();	
    		System.out.println("Finished serializing graph");
    	}

    }
}
