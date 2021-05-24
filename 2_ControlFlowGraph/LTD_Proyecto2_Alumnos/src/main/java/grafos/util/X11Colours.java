package grafos.util;

import java.util.Random;

public class X11Colours {
	private static Random randomNumberGenerator = new Random();

    /** Obtained from com.srbenoit.color.ColorNames */
    private static final String[] CNAMES = {
            "DarkGray",  "Red", "Maroon",
             "yellow3",  "Olive", "limegreen",
            "Green",  "DarkCyan",  "Teal",
            "Blue",  "Navy",  "Fuchsia", "Purple", "plum", "paleturquoise4",
            "orange", "orangered4", "tan4"};

    public static String getRandomColour()
    {
    	int i = randomNumberGenerator.nextInt(CNAMES.length);

    	return CNAMES[i];
    }
}
