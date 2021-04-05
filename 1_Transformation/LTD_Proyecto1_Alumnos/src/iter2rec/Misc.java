package iter2rec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Misc
{
	public static List<File> getFiles(File root, String[] extensions, boolean recursive)
	{
		if (!root.exists() || !root.isDirectory())
			return new LinkedList<File>();
		if (!recursive)
			return Misc.getFiles(root, extensions);
		return Misc.getFilesRecursively(root, extensions);
	}
	private static List<File> getFilesRecursively(File root, String[] extensions)
	{
		int numHijoAVisitar = 0;
		final LinkedList<Integer> bifurcaciones = new LinkedList<Integer>();

		File dir = root;
		final List<File> files = new LinkedList<File>();

		do
		{
			final List<File> folders = Misc.getFolders(dir);
			final int numeroDeHijos = folders.size();

			if (numHijoAVisitar == 0)
				files.addAll(Misc.getFiles(dir, extensions));

			if (numeroDeHijos == 0 || numHijoAVisitar == numeroDeHijos)
			{
				// Es una hoja o ya se han evaluado todos sus hijos

				// Si es la raiz terminar directamente
				if (dir.getPath().equals(root.getPath()))
					break;
				else
				{
					// Si no, volver al padre
					dir = dir.getParentFile();
					numHijoAVisitar = bifurcaciones.removeLast();
				}
			}
			else
			{
				// Algun hijo no esta evaluado todavia
				
				// Pasar al hijo
				dir = folders.get(numHijoAVisitar);
				bifurcaciones.add(numHijoAVisitar + 1);
				numHijoAVisitar = 0;
			}
		}
		while (true);

		return files;
	}
	private static List<File> getFolders(File folder)
	{
		final File[] folders = folder.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name)
			{
				return new File(dir + File.separator + name).isDirectory();
			}});

		return new LinkedList<File>(Arrays.asList(folders));
	}
	private static List<File> getFiles(File dir, final String[] extensions)
	{
		final File[] files = dir.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name)
			{
				for (String extension : extensions)
					if (name.endsWith(extension))
						return true;

				return false;
			}});

		return new LinkedList<File>(Arrays.asList(files));
	}

	public void copy(File src, File dst)
	{
		try
		{
			final InputStream in = new FileInputStream(src);
			final OutputStream out = new FileOutputStream(dst);

			int len;
			final byte[] buf = new byte[1024];
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
			in.close();
			out.close();
		}
		catch (IOException io)
		{
			io.printStackTrace();
		}
	}
}