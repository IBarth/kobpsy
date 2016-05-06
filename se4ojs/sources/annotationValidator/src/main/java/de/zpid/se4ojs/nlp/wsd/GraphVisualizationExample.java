/**
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.zpid.se4ojs.nlp.wsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.extjwnl.JWNLException;
import de.tudarmstadt.ukp.dkpro.wsd.Pair;
import de.tudarmstadt.ukp.dkpro.wsd.UnorderedPair;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.DegreeCentralityWSD;
import de.tudarmstadt.ukp.dkpro.wsd.graphconnectivity.algorithm.JungGraphVisualizer;
import de.tudarmstadt.ukp.dkpro.wsd.si.POS;
import de.tudarmstadt.ukp.dkpro.wsd.si.SenseInventoryException;
import de.tudarmstadt.ukp.dkpro.wsd.si.wordnet.WordNetSenseKeySenseInventory;
import edu.uci.ics.jung.graph.UndirectedGraph;

/**
 * This class displays a simple, interactive visualization of the degree
 * centrality WSD algorithm described in the paper
 * "An Experimental Study of Graph Connectivity for Unsupervised Word Sense Disambiguation"
 * by R. Navigli and M. Lapata
 * (<em>IEEE Transactions on Pattern Analysis and Machine Intelligence</em>
 * 32(4):678–692, 2010). When the class is run, a window will appear showing a
 * sense graph. Clicking anywhere on the graph will step through the algorithm.
 *
 * @author <a href="mailto:miller@ukp.informatik.tu-darmstadt.de">Tristan Miller</a>
 *
 */
public class GraphVisualizationExample
{
    private static final String SERIALIZED_GRAPH_LOCATION = "/home/isa/dev/tools/WordNet-3.0/DKProWSD_SK_graph.ser";

	public static void main(String[] args)
        throws MalformedURLException, JWNLException, IOException,
        SenseInventoryException, InterruptedException,
        UnsupportedOperationException, ClassNotFoundException
    {

        WordNetSenseKeySenseInventory inventory = new WordNetSenseKeySenseInventory(
                        GraphVisualizationExample.class.getClassLoader().getResourceAsStream("extjwnl_properties.xml"));
        inventory
            	.setUndirectedGraph(GraphUtil.deserializeGraph(SERIALIZED_GRAPH_LOCATION));

        GraphUtil.serializeGraph(inventory.getUndirectedGraph(), SERIALIZED_GRAPH_LOCATION);
        DegreeCentralityWSD wsdAlgorithm = new DegreeCentralityWSD(inventory);
        wsdAlgorithm.setSearchDepth(4);
    }



}
