package transformador;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.FileWriter;

public class Transformador {

	public static void main(String[] args) {
		File in = new File(args[0]);
		File out = new File(args[1]);
		if (in.isDirectory() && (!out.exists() || out.isDirectory()))
			transformFolder(in, out);
		else if (!in.isDirectory() && !out.isDirectory())
			transformFile(in, out);
	}

	public static boolean transformFile(File fIn, File fOut) {
		try {
			CompilationUnit cu = JavaParser.parse(fIn);
			new Visitador_1().visit(cu, null);
			FileWriter fw = new FileWriter(fOut);
			fw.write(cu.toString());
			fw.close();
			System.out.printf("Transform OK %s --> %s\n", fIn.getPath(), fOut.getPath());
			return true;
		} catch (Exception e) {
			System.out.printf("Transform FAILED %s --> %s\n", fIn.getPath(), fOut.getPath());
			e.printStackTrace();
			return false;
		}
	}

	public static int transformFolder(File fIn, File fOut) {
		if (!fIn.isDirectory() || (fOut.exists() && !fOut.isDirectory())) return 0;
		if (!fOut.exists()) fOut.mkdirs();
		int errors = 0;
		for (File f : fIn.listFiles()) {
			if (f.isDirectory())
				errors += transformFolder(f, new File(fOut, f.getName()));
			else if (f.getName().endsWith(".java"))
				if (!transformFile(f, new File(fOut, f.getName())))
					errors++;
		}
		System.out.printf("Folder transform %s (%s --> %s)\n", errors == 0 ? "OK" : "FAILED with " + errors + " errors", fIn.getPath(), fOut.getPath());
		return errors;
	}

}

