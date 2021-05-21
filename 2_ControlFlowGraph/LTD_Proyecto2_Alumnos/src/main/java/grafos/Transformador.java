package grafos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitor;

public class Transformador {

	public static void main(String[] args) throws Exception {
		// Ruta del fichero con el programa que vamos a transformar
		String ruta = "./src/main/java/ejemplos";

		File folder = new File(ruta);

		if (!folder.isDirectory())
		{
			transformFile(ruta);

			return;
		}

		transformFolder(folder);
	}

	private static void transformFolder(File folder) throws FileNotFoundException {
		for (File f : folder.listFiles()) {
			if (f.isDirectory()) {
				transformFolder(f);
			}
			else if (f.getName().endsWith(".java"))
			{
				transformFile(f.getAbsolutePath());
			}
		}
	}

	private static void transformFile(String ruta) throws FileNotFoundException {
		File original = new File(ruta);

		// Parseamos el fichero original. Se crea una unidad de compilación (un AST).
		CompilationUnit cu = JavaParser.parse(original);

		quitarComentarios(cu);

		// Recorremos el AST
		CFG cfg = new CFG();
		VoidVisitor<CFG> visitadorCFG = new VisitadorCFG();
		visitadorCFG.visit(cu, cfg);

		printGraph(ruta, null, cfg.arcos, "CFG");

		ProgramDependencyGraph pdg = new ProgramDependencyGraph();
		VoidVisitor<ProgramDependencyGraph> visitadorPDG = new VisitadorPDG();
		visitadorPDG.visit(cu, pdg);

		List<String> pdgEdges = new ArrayList<String>(pdg.controlEdges);

		pdgEdges.addAll(pdg.dataEdges);

		printGraph(ruta, pdg.nodes, pdgEdges, "PDG");
	}

	private static void printGraph(String ruta, Map<Integer, List<String>> nodes,  List<String> edges, String graphName) {
		// Imprimimos el grafo del programa
		String dotInfo = imprimirGrafo(graphName, nodes, edges);

		// Generamos un PDF con el CFG del programa
		System.out.print("\nGenerando PDF...");
	    GraphViz gv=new GraphViz();
	    gv.addln(gv.start_graph());
	    gv.add(dotInfo);
	    gv.addln(gv.end_graph());
	    String type = "pdf";   // String type = "gif";
	  // gv.increaseDpi();
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    File destino_GRAPH = new File(ruta + "_" + graphName + "." + type);
	    gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), destino_GRAPH);
	    System.out.println("     PDF generado!");
	}

	// Imprime el grafo en la pantalla
	private static String imprimirGrafo(String graphName, Map<Integer, List<String>> groupedNodes, List<String> arcos)
	{
		String dotInfo="";

		if (groupedNodes != null)
		{
			// The control nodes must be ranked on their level. Otherwise, the graphs are optimized and printed weird.
			for (Integer level : groupedNodes.keySet())
			{
				List<String> nodes = groupedNodes.get(level);

				String joinedNodes = String.join(",", nodes);

				System.out.println("Level "+ level + " nodes: " + joinedNodes);

				dotInfo += "{ rank = same {" + joinedNodes + "}}";
			}
		}

		for(String arco:arcos) {
			dotInfo += arco;
			System.out.println("ARCO: "+arco);
		}
		System.out.println("\n" + graphName + ":");
		System.out.println(dotInfo);

		return dotInfo;
	}

	// Elimina todos los comentarios de un nodo y sus hijos
	static void quitarComentarios(Node node){
		node.removeComment();
		for (Comment comment : node.getOrphanComments())
		{
			node.removeOrphanComment(comment);
		}
	    // Do something with the node
	    for (Node child : node.getChildNodes()){
	    	quitarComentarios(child);
	    }
	}

}


////////////////////////////////////////////////////////////////
// COMO CONFIGURAR GRAPHVIZ:
////////////////////////////////////////////////////////////////
//
//Update config.properties file. Copy paste the following:
//
//##############################################################
//#                    Linux Configurations                    #
//##############################################################
//# The dir. where temporary files will be created.
//tempDirForLinux = /tmp
//# Where is your dot program located? It will be called externally.
//dotForLinux = /usr/bin/dot
//
//##############################################################
//#                   Windows Configurations                   #
//##############################################################
//# The dir. where temporary files will be created.
//tempDirForWindows = c:/temp
//# Where is your dot program located? It will be called externally.
//dotForWindows = "c:/Program Files (x86)/Graphviz 2.28/bin/dot.exe"
//
//##############################################################
//#                    Mac Configurations                      #
//##############################################################
//# The dir. where temporary files will be created.
//tempDirForMacOSX = /tmp
//# Where is your dot program located? It will be called externally.
//dotForMacOSX = /usr/local/bin/dot





