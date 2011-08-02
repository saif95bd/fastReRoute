package saif.multicast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.text.AbstractDocument.BranchElement;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleGraph;

/**
 * @author saif
 *
 */
public class MC {
	Integer nodes[];
	Integer root;
	int noOfNodes;
	private DefaultDirectedGraph<Integer, DefaultEdge> dBaseGraph;
	private UndirectedGraph<Integer, DefaultEdge> baseGraph;
	// private BellmanFordShortestPath<Integer, DefaultEdge> spt;
	private DefaultDirectedGraph<Integer, DefaultEdge> spt; //it does not have multi-edges between two nodes.

	public MC() {
		noOfNodes = 6;
		nodes = new Integer[noOfNodes + 1];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new Integer(i);
		}

		baseGraph = createGraph(nodes);
		dBaseGraph = createDiaGraph(baseGraph);
		// spt= new BellmanFordShortestPath<Integer, DefaultEdge>(baseGraph,
		// nodes[1]);
		root=nodes[1];
		spt = createMT(baseGraph, root);
		
	}// default constructor

	public UndirectedGraph<Integer, DefaultEdge> createGraph(Integer x[]) {
		UndirectedGraph<Integer, DefaultEdge> g = new SimpleGraph<Integer, DefaultEdge>(
				DefaultEdge.class);
		for (int i = 1; i < x.length; i++) {
			g.addVertex(x[i]);
		}
		g.addEdge(x[1], x[2]);
		g.addEdge(x[2], x[3]);
		g.addEdge(x[3], x[4]);
		g.addEdge(x[4], x[5]);
		g.addEdge(x[1], x[5]);
		g.addEdge(x[2], x[4]);
		g.addEdge(x[3], x[6]);
		g.addEdge(x[4], x[6]);
		g.addEdge(x[5], x[6]);
		
		return g;
		
	}

	public DefaultDirectedGraph<Integer, DefaultEdge> createDiaGraph(UndirectedGraph<Integer, DefaultEdge> g){
		DefaultDirectedGraph<Integer, DefaultEdge> dg= new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
		for (Integer v : g.vertexSet()) {
			dg.addVertex(v);
		}
		for (DefaultEdge e : g.edgeSet()) {
			dg.addEdge(g.getEdgeSource(e), g.getEdgeTarget(e));
			dg.addEdge(g.getEdgeTarget(e), g.getEdgeSource(e));
		}
		return dg;
	}
/*	It returns a directed MCtree. We use directed so that later we can find the direction of streams.
 *  generate equvalent directed graph of base graph
	add vertices of the directed graph to a MCtree
	generate shortest-path from the root to other nodes and add the edges to MCtree
*/	
	public DefaultDirectedGraph<Integer, DefaultEdge> createMT(
			UndirectedGraph<Integer, DefaultEdge> bgraph, Integer root) {
		BellmanFordShortestPath<Integer, DefaultEdge> bmf;

		DefaultDirectedGraph<Integer, DefaultEdge> g = new DefaultDirectedGraph<Integer, DefaultEdge>(  //MC tree
				DefaultEdge.class);

		for (Integer vertex : bgraph.vertexSet()) {
			g.addVertex(vertex);
		}

		
		bmf= new BellmanFordShortestPath<Integer, DefaultEdge>(dBaseGraph, root);

		for (Integer inNodes : dBaseGraph.vertexSet()) {
			if (inNodes != root) { // any vertex except the root
				// step 1. find path from bellmanford
				for (DefaultEdge edge : bmf.getPathEdgeList(inNodes)) {
					g.addEdge(dBaseGraph.getEdgeSource(edge),
							dBaseGraph.getEdgeTarget(edge), edge);
				}
			}
		}

		// g=bgraph;
		return g;
	}
	
	public int trafficOverlapWhileFailure(Integer fnode)
	{
		int noOfLinksInAlternateMCtree=0;
		int noOfLinksInUnicastRR=0;
		int OverlapInUnicast=0;
		int overlapMCandBackupMC=0;
		Integer parent;
		List<Integer> children= new ArrayList<Integer>();
		
		//step 1: copy base dia graph
		DefaultDirectedGraph<Integer, DefaultEdge> localDBaseGraph = (DefaultDirectedGraph<Integer, DefaultEdge>) dBaseGraph.clone();
		
		//step 2: find parent of failed node and find children.
		BellmanFordShortestPath<Integer, DefaultEdge> bTree = new BellmanFordShortestPath<Integer, DefaultEdge>(spt, root);
		List<DefaultEdge> elist= bTree.getPathEdgeList(fnode);
		parent=spt.getEdgeSource(elist.get(elist.size()-1)); // we found the parent
		Set<DefaultEdge> slist=  spt.outgoingEdgesOf(fnode);
		for (DefaultEdge defaultEdge : slist) {
			children.add(spt.getEdgeTarget(defaultEdge));
		}
		System.out.print(children.toString());
		//step 3: remove the failed node from local base tree.
		localDBaseGraph.removeVertex(fnode);		
		//step 4:copy edges of SP from parent to children
		DirectedMultigraph<Integer, DefaultEdge> overlapTree = new DirectedMultigraph<Integer, DefaultEdge>(DefaultEdge.class);
		bTree= new BellmanFordShortestPath<Integer, DefaultEdge>(localDBaseGraph, root);
		for (Integer v : spt.vertexSet()) {
			overlapTree.addVertex(v);
		}
		for (Integer ci: children ) {
			for (DefaultEdge defaultEdge : bTree.getPathEdgeList(ci)) {
				overlapTree.addEdge(localDBaseGraph.getEdgeSource(defaultEdge), localDBaseGraph.getEdgeTarget(defaultEdge));
			}
		}//for each child
		noOfLinksInUnicastRR=overlapTree.edgeSet().size();
		DirectedMultigraph<Integer, DefaultEdge> tmg= new DirectedMultigraph<Integer, DefaultEdge>(DefaultEdge.class);
		tmg=(DirectedMultigraph<Integer, DefaultEdge>) overlapTree.clone();
		for (DefaultEdge defaultEdge : overlapTree.edgeSet()) {
			System.out.print("\n#edges 1-2:"+tmg.getAllEdges(nodes[1], nodes[2]).size());
			if(tmg.getAllEdges(overlapTree.getEdgeSource(defaultEdge), overlapTree.getEdgeTarget(defaultEdge))!=null)
			{
				int s,d;
				s=overlapTree.getEdgeSource(defaultEdge);
				d=overlapTree.getEdgeTarget(defaultEdge);
				
				int numberOfduplicateEdge=tmg.getAllEdges(nodes[s], nodes[d]).size();
				System.out.print("\nVisiting edge:"+s+" "+d+"\nnumberOfdup:"+numberOfduplicateEdge);
				if(numberOfduplicateEdge>0){
					OverlapInUnicast= OverlapInUnicast+(numberOfduplicateEdge-1);
					tmg.removeAllEdges(nodes[s], nodes[d]);
				}
			}
		}
		//step 5: copy SPT to a multigraph.
		for (DefaultEdge defaultEdge : spt.edgeSet()) {
			overlapTree.addEdge(spt.getEdgeSource(defaultEdge), spt.getEdgeTarget(defaultEdge));
		}
		
		
		//step 6: calculate overlap and rerouted links
		
		System.out.print("\nOverlap tree:\n");
		for (DefaultEdge defaultEdge : overlapTree.edgeSet()) {
			System.out.print("\n"+overlapTree.getEdgeSource(defaultEdge)+" "+overlapTree.getEdgeTarget(defaultEdge));
		}
		
		System.out.print("\n#unicast links:"+noOfLinksInUnicastRR);
		System.out.print("\n#Overlap links:"+OverlapInUnicast);
		int x=0;
		
		return x;
	}

	public void printGraph() {

		System.out.print("base graph:" + "\n");
		for (DefaultEdge e : baseGraph.edgeSet()) {
			System.out.print("\n"+baseGraph.getEdgeSource(e)+" "+baseGraph.getEdgeTarget(e));
			}
		System.out.print("\nbase dia graph:" + "\n");
		for (DefaultEdge e : dBaseGraph.edgeSet()) {
			System.out.print("\n"+dBaseGraph.getEdgeSource(e)+" "+dBaseGraph.getEdgeTarget(e));
			}
		System.out.print("\nSPT (directed graph):"+spt.vertexSet().toString());
		for (DefaultEdge e : spt.edgeSet()) {
			System.out.print("\n"+spt.getEdgeSource(e)+" "+spt.getEdgeTarget(e));
		}
		System.out.print("\nTraffic overlap when node 2 fails:"+trafficOverlapWhileFailure(nodes[5]));
		// List<DefaultEdge> el= spt.getPathEdgeList(nodes[4]);
	}
}