package grafos;

import java.util.ArrayList;
import java.util.List;

public class ProgramDependencyGraph {
	// Contains the edges of control dependencies.
	List<String> controlEdges = new ArrayList<String>();

	// Contains the edges of data dependencies.
	List<String> dataEdges = new ArrayList<String>();
}
