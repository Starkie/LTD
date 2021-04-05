package transformador;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
//import transformador.Transformador;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class TestJavaFile {
	private static final String DOT_JAVA = ".java";
	private final File file;

	public TestJavaFile(File file) {
		this.file = file;
	}

	@Parameters(name="{index}: {0}")
	public static Collection<File> paramGen() {
		return findJavaPrograms(new File("src/Ejemplos"));
	}

	/**
	 * Recursively finds Java source files that are not transformed. For extra security, it returns
	 * the absolute paths.
	 * @param file File or directory to be checked.
	 * @return List of Java source files contained in the input file name or the input file name.
	 */
	private static List<File> findJavaPrograms(File file) {
		List<File> res = new ArrayList<File>();
		if (file.isDirectory()) {
			for (String s : file.list())
				res.addAll(findJavaPrograms(new File(file, s)));
		} else if (file.getName().endsWith(DOT_JAVA)) {
			res.add(file);
		}
		return res;
	}

	/**
	 * Transforms a java file, then compiles the original and the transformed program and runs them.
	 * Fails when the transformation fails, the compilation fails, or the output and exit codes of the
	 * executions are different.
	 */
	@Test
	public void TestFile() throws IOException, InterruptedException, ParseException {
		System.out.println("Begin test for " + file);
		File out = new File("test" + File.separator + file.getPath());
		if (out.exists()) out.delete();
		if (!out.getParentFile().exists()) out.getParentFile().mkdirs();
		Transformador.transformFile(file, out);
		CompilationUnit cu = JavaParser.parse(file);
		assert out.exists();

		// Compilamos el programa original y el transformado
		new File("bin").mkdirs();
		new File("test/bin").mkdirs();
		Process compOrig = Runtime.getRuntime().exec("javac -d bin " + file.getPath());
		Process compNew  = Runtime.getRuntime().exec("javac -d test/bin " + out.getPath());
		compOrig.waitFor();
		compNew.waitFor();
		assert compOrig.exitValue() == 0 : "Ha fallado la compilación de la versión original de " + this.file;
		assert compNew.exitValue() == 0 : "Ha fallado la compilación de la versión transformada de " + this.file;
		// Ejecutamos ambos programas y comparamos el output
		String canonicalName = cu.getPackage().getName() + "." + file.getName().substring(0, file.getName().lastIndexOf(DOT_JAVA));
		ProcessBuilder runOriB = new ProcessBuilder("java", "-cp", "bin", canonicalName);
		ProcessBuilder runNewB = new ProcessBuilder("java", "-cp", "test/bin", canonicalName);
		final Process runOri = runOriB.start();
		final Process runNew = runNewB.start();
		final boolean[] killed = new boolean[] { false };
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// TODO: if the first one doesn't end, then the second doesn't have to end
					// TODO: if the first one ends, the second must end
					Thread.sleep(5000);
					runOri.destroy();
					runNew.destroy();
					killed[0] = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		runOri.waitFor();
		runNew.waitFor();
		assert !killed[0] : "Maximum runtime exceeded";
		diffLines(runOri.getInputStream(), runNew.getInputStream());
		diffLines(runOri.getErrorStream(), runNew.getErrorStream());
		assert runOri.exitValue() == runNew.exitValue() : "The return codes did not match: original is " + runOri.exitValue() +
				" and transformed is " + runNew.exitValue();
		System.out.println("Test para " + this.file + " OK");
	}

	/** Checks if two InputStream objects match */
	private void diffLines(InputStream original, InputStream transformed) throws IOException {
		String resOrig = stream2string(original);
		String resTran = stream2string(transformed);

		if (!resOrig.equals(resTran)) {
			System.out.println("Salida del programa iterativo:");
			System.out.println(resOrig);
			System.out.println("Salida del programa recursivo:");
			System.out.println(resTran);
			assert false : "La salida de los programas no coincide";
		}
	}

	/** Reads an InputStream until it closes, and returns the whole input as a string. */
	private String stream2string(InputStream stream) throws IOException {
		StringBuilder builder = new StringBuilder();
		String buffer;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		// When the stream closes and there is no more content to read, reader.readLine() will be null
		while ((buffer = reader.readLine()) != null) {
			builder.append(buffer).append('\n');
		}
		return builder.toString();
	}
}