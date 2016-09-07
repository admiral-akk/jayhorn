/*
 * jimple2boogie - Translates Jimple (or Java) Programs to Boogie
 * Copyright (C) 2013 Martin Schaeaeaeaeaeaeaeaeaef and Stephan Arlt
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package jayhorn;

import org.kohsuke.args4j.Option;

import java.util.List;
import java.util.Arrays;

/**
 * Options
 * 
 * @author schaef
 */
public class Options {

	// @Option(name = "-android-jars", usage = "Path to the jars that stub the
	// android platform.")
	// private String androidStubPath=null;
	//
	// public String getAndroidStubPath() {
	// return androidStubPath;
	// }
	//
	// public void setAndroidStubPath(String path) {
	// this.androidStubPath = path;
	// }
	//
	// /**
	// * JAR file
	// */
	@Option(name = "-checker", usage = "Select a checker [inconsistency, or safety]", required = false)
	// //@Option(name = "-checker", usage = "Select a checker [safety]",
	// required = false)
	private String checker = "safety";

	public String getChecker() {
		return checker;
	}

	@Option(name = "-solver", usage = "Select a solver [eldarica or spacer]", required = false)
	private String solver = "eldarica";

	public String getSolver() {
		return solver;
	}

	@Option(name = "-solverOptions", usage = "Options for the solver [eldarica: abstract, debug]", required = false)
	private String solverOptions = "";

	public List<String> getSolverOptions() {
		return Arrays.asList(solverOptions.split(","));
	}

	/**
	 * JAR file
	 */
	@Option(name = "-j", usage = "JAR file, class folder, or apk", required = true)
	private String javaInput;

	public String getJavaInput() {
		return this.javaInput;
	}

	// /**
	// * Print Horn clauses
	// */
	@Option(name = "-h", usage = "Print horn clauses", required = false)
	private boolean printHorn = false;

	public boolean getPrintHorn() {
		return this.printHorn;
	}

	public void setPrintHorn(boolean b) {
		this.printHorn = b;
	}

	@Option(name = "-specs", usage = "Use built-in specs", required = false)
	public boolean useSpecs = false;

	
	// /**
	// * Output intermediate representations
	// */
	@Option(name = "-out", usage = "Output directory for intermediate represenations", required = false)
	private String out = null;

	public String getOut() {
		return this.out;
	}

	public String getOutDir() {
		if (this.out != null && !this.out.endsWith("/"))
			return this.out + "/";
		return this.out;
	}

	public String getOutBasename() {
		String outName = "";
		String in = getJavaInput();
		if (in != null) {
			if (in.endsWith("/"))
				in = in.substring(0, in.length() - 1);
			outName = in.substring(in.lastIndexOf('/') + 1, in.length()).replace(".java", "").replace(".class", "");
		}
		if (outName.equals(""))
			outName = "noname";
		return outName;
	}

	public void setOut(String s) {
		this.out = s;
	}

	/**
	 * Classpath
	 */
	@Option(name = "-cp", usage = "Classpath")
	private String classpath;

	@Option(name = "-t", usage = "Timeout per procedure in seconds. Use 0 for no timeout. (Default is 0)")
	private int timeout = 0;

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int seconds) {
		this.timeout = seconds;
	}

	/**
	 * Determines, whether Joogie has an additional classpath
	 * 
	 * @return true = Joogie has an additional classpath
	 */
	public boolean hasClasspath() {
		return (null != classpath);
	}

	/**
	 * Returns the additional classpath
	 * 
	 * @return Additional classpath
	 */
	public String getClasspath() {
		return classpath;
	}

	/**
	 * Assigns the additional classpath
	 * 
	 * @param classpath
	 *            Additional classpath
	 */
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}

	private static Options options;

	public static void resetInstance() {
		options = null;
	}

	public static Options v() {
		if (null == options) {
			options = new Options();
		}
		return options;
	}

	private Options() {
	}

}
