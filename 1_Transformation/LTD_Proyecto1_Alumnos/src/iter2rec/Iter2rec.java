package iter2rec;

import iter2rec.transformation.Transformer;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Iter2rec
{
	private String originalFile;
	private String destinationFile;
	private boolean open;

	private CompilationUnit cu;
	private Transformer transformer;

	public Iter2rec(String originalFile, String destinationFile)
	{
		this.changeFiles(originalFile, destinationFile);
	}
	public Iter2rec(String file)
	{
		this(file, file);
	}
	public Iter2rec()
	{
		this(null, null);
	}

	// Configuration
	private void checkFiles()
	{
		if (this.originalFile == null)
			throw new RuntimeException("Null original file");
		if (this.destinationFile == null)
			throw new RuntimeException("Null destination file");

		final File file = new File(this.originalFile);
		if (!file.exists())
			throw new RuntimeException("Original file not exists");
	}
	private void checkOpen()
	{
		if (!this.isOpen())
			throw new RuntimeException("File not open");
	}
	public void changeFile(String file)
	{
		this.changeFiles(file, file);
	}
	public void changeFiles(String originalFile, String destinationFile)
	{
		this.originalFile = originalFile;
		this.destinationFile = destinationFile;
		this.open = false;
	}

	// Functions
	public void openFile()
	{
		this.checkFiles();

		this.cu = this.getCompilationUnit();
		this.transformer = new Transformer(this.cu);
		this.open = true;
	}
	public boolean isOpen()
	{
		return this.open;
	}
	public void showCode()
	{
		this.checkOpen();

		System.out.println(this.cu);
	}
	public void saveFile()
	{
		this.checkOpen();

		final File destinationFile = new File(this.destinationFile);
		final File destinationDir = destinationFile.getParentFile();

		if (!destinationDir.exists())
			destinationDir.mkdirs();
		if (!destinationDir.exists())
			throw new RuntimeException("Cannot create the destination directory");

		try
		{
			final FileOutputStream out = new FileOutputStream(this.destinationFile);
			final PrintStream print = new PrintStream(out);

			print.print(this.cu.toString());
		}
		catch (FileNotFoundException fnf)
		{
			fnf.printStackTrace();
		}
	}
	private CompilationUnit getCompilationUnit()
	{
		CompilationUnit cu = null;
		FileInputStream in = null;

		try
		{
			in = new FileInputStream(this.originalFile);
			cu = JavaParser.parse(in);
		}
		catch (IOException io)
		{
			io.printStackTrace();
		}
		catch (ParseException pe)
		{
			pe.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (IOException io)
			{
				
			}
		}

		return cu;
	}

	// Transformations
	private void startTransformation()
	{
		this.checkOpen();
	}
	public boolean transform()
	{
		this.startTransformation();

		final int loopsTransformed = this.transformer.transform();

		return (loopsTransformed > 0);
	}
}