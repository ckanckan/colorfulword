/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */

package graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.commons.collections15.map.LazyMap;
import org.w3c.dom.events.MouseEvent;

import coloring.ColorManager;

import wndata.DataManager;
import wndata.PartOfSpeech;
import wndata.Synset;
import wndata.SynsetPointer;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.VisualizationViewer.GraphMouse;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;

/**
 * Shows how  to create a graph editor with JUNG.
 * Mouse modes and actions are explained in the help text.
 * The application version of GraphEditorDemo provides a
 * File menu with an option to save the visible graph as
 * a jpeg file.
 * 
 * @author Tom Nelson
 * 
 */
public class GraphExplorer extends JApplet {


    Graph<Gnode,Gedge> graph;
    
    AbstractLayout<Gnode,Gedge> layout;

    VisualizationViewer<Gnode,Gedge> vv;

    Map<Synset, Gnode> map;
    
    public static final int EDGE_LENGTH = 70;
    public static final int HEIGHT = 600;
    public static final int WIDTH = 600;
    /**
     * create an instance of a simple graph with popup controls to
     * create a graph.
     * 
     */
    public GraphExplorer(Synset synset) {
        
        // create a simple graph for the demo
//        graph = new SparseMultigraph<Gnode,String>();
    	map=new HashMap<Synset, Gnode>();
    	Graph<Gnode,Gedge> ig = Graphs.<Gnode,Gedge>synchronizedDirectedGraph(new DirectedSparseMultigraph<Gnode,Gedge>());

        ObservableGraph<Gnode,Gedge> og = new ObservableGraph<Gnode,Gedge>(ig);
        graph=og;
        this.layout = new SpringLayout<Gnode,Gedge>(graph,
                new ConstantTransformer(EDGE_LENGTH));
        Dimension d=new Dimension(HEIGHT,WIDTH);
        layout.setSize(d);
        vv =  new VisualizationViewer<Gnode,Gedge>(layout);
        vv.setBackground(Color.white);
        GraphMouse gm=new DefaultModalGraphMouse<Gnode,String>();
        vv.setGraphMouse(gm);
        GraphMouseListener<Gnode> gml=new GraphMouseListener<Gnode>(){
			@Override
			public void graphClicked(Gnode gnode, java.awt.event.MouseEvent me) {
				System.out.println(gnode.synset.toString());
				if (!gnode.extended){
					layout.lock(true);
					Relaxer relaxer = vv.getModel().getRelaxer();
					relaxer.pause();
			        gnode.extended=true;
			        double x=layout.getX(gnode);
			        double y=layout.getY(gnode);
			        SynsetPointer[] sp=gnode.synset.getPointers();
			        for (int i=0;i<sp.length;++i){
			        	Synset synset_=DataManager.getSingleton().getSynset(sp[i].getSynsetOffset(),sp[i].getPartOfSpeech());
			        	if (map.get(synset_)==null)
			        		map.put(synset_, new Gnode(synset_));
			        	graph.addEdge(new Gedge(sp[i].getPointerSymbol().getDescription()),gnode,map.get(synset_));
			        	layout.setLocation(map.get(synset_), x + 0.5 * EDGE_LENGTH * Math.cos(Math.PI*2/sp.length*i), y + 0.5*EDGE_LENGTH * Math.sin(Math.PI*2/sp.length*i));
			        }
					layout.initialize();
					relaxer.resume();
					layout.lock(false);
				}
			}
			@Override
			public void graphPressed(Gnode arg0, java.awt.event.MouseEvent arg1) {}
			@Override
			public void graphReleased(Gnode arg0, java.awt.event.MouseEvent arg1) {}
        };
        vv.addGraphMouseListener(gml);
        Transformer<Gnode,Paint> vertexPaint = new Transformer<Gnode,Paint>() {
        	public Paint transform(Gnode i) {
        		coloring.Color color=ColorManager.getSingleton().getColor(i.synset);
        		return new Color(color.getR(),color.getG(),color.getB());
        	}
        };
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
/*        Transformer<Gnode,Font> vertexFont = new Transformer<Gnode,Font>() {
        	public Font transform(Gnode i) {
        		Map<java.awt.font.TextAttribute,Paint> map= new HashMap<java.awt.font.TextAttribute,Paint>();
        		map.put(java.awt.font.TextAttribute.BACKGROUND, new Color(255,255,255));
        		return new Font(map);
        	}
        };
        vv.getRenderContext().setVertexFontTransformer(vertexFont);*/
        Transformer<Gnode,Shape> vertexShape = new Transformer<Gnode,Shape>() {
        	public Shape transform(Gnode i){
        		float len=(float) (Math.log(i.synset.getWordCount())*10+50);
        		return new Ellipse2D.Float(-len/2,-len/2,len,len);
        	}
        };
        vv.getRenderContext().setVertexShapeTransformer(vertexShape);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Gnode>());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<Gedge>());
        Container content = getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);

        Relaxer relaxer = vv.getModel().getRelaxer();
        relaxer.pause();
        if (map.get(synset)==null)
        	map.put(synset, new Gnode(synset));
        graph.addVertex(map.get(synset));
        System.out.println(map.get(synset).synset.toString());
        map.get(synset).extended=true;
    	layout.setLocation(map.get(synset), HEIGHT/2, WIDTH/2);
        SynsetPointer[] sp=synset.getPointers();
        for (int i=0;i<sp.length;++i){
        	Synset synset_=DataManager.getSingleton().getSynset(sp[i].getSynsetOffset(),sp[i].getPartOfSpeech());
        	if (map.get(synset_)==null)
        		map.put(synset_, new Gnode(synset_));
        	graph.addEdge(new Gedge(sp[i].getPointerSymbol().getDescription()),map.get(synset),map.get(synset_));
        	layout.setLocation(map.get(synset_), HEIGHT/2 + EDGE_LENGTH * Math.cos(Math.PI*2/sp.length*i), WIDTH/2+Math.sin(Math.PI*2/sp.length*i));
        }
        layout.initialize();        
        relaxer.resume();
        getContentPane().add(vv);
    }
    @SuppressWarnings("serial")
	public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Synset start =DataManager.getSingleton().lookup("hire", PartOfSpeech.forChar('n'))[0];
        final GraphExplorer demo = new GraphExplorer(start);
        
        frame.getContentPane().add(demo);
        frame.pack();
        frame.setVisible(true);
    }
}

