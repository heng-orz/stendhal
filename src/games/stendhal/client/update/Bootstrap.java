package games.stendhal.client.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import marauroa.common.Log4J;

import org.apache.log4j.PropertyConfigurator;

/**
 * Starts a program after doing some classpath magic.
 *
 * @author hendrik
 */
public class Bootstrap {
	private String pathSep = null;
	private String jarFolder = null;
	private Properties bootProp = null;
	private Properties bootPropOrg = null;

	/**
	 * An URLClassLoader with does load its classes first and only delegates
	 * missing classes to the parent classloader (default is the other way round)
	 */
	private static class ButtomUpOrderClassLoader extends URLClassLoader {
	    private ButtomUpOrderClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
	    }

	    @Override
		protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException  {
			ClassLoader parent = super.getParent();
			Class clazz = findLoadedClass(name);
			if (clazz == null) {
			    try {
			    	clazz = findClass(name);
			    } catch (ClassNotFoundException e) {
		    		clazz = parent.loadClass(name);
			    }
			}
			if (resolve) {
			    resolveClass(clazz);
			}
			return clazz;
	    }
	}

	/**
	 * saves modifed boot properties to disk
	 *
	 * @throws IOException if an IO-error occurs
	 */
	public void saveBootProp() throws IOException {
		// only try to save it, if it was changed (so that we do not have to
		// care about all the things which could go wrong unless an update
		// was done this time.
		if (!bootProp.equals(bootPropOrg)) {
			String propFile = jarFolder + "jar.properties";
			OutputStream os = new FileOutputStream(propFile);
			bootProp.store(os, "Stendhal Boot Configuration");
			os.close();
		}
	}

	private void init() {
		// discover folder for .jar-files
		pathSep = System.getProperty("file.separator");
		jarFolder = System.getProperty("user.home") + pathSep + "stendhal" + pathSep + "jar" + pathSep;
		File folder = new File(jarFolder);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		System.setProperty("log4j.ignoreTCL", "true");
	}
	
	/**
	 * Sets a dynamic classpath up and returns a Class reference loaded from it
	 *
	 * @param className  name of class to load form the dynamic path
	 * @return Class-object
	 * @throws Exception if an unexpected error occurs
	 */
	private ClassLoader createClassloader() throws Exception {
		// load jar.properties
		String propFile = jarFolder + "jar.properties";
		bootProp = new Properties();
		List<URL> jarFiles = new LinkedList<URL>();
		if (new File(propFile).canRead()) {
			InputStream is = new FileInputStream(propFile);
			bootProp.load(is);
			bootPropOrg = (Properties) bootProp.clone();
			is.close();

			// get list of .jar-files
			String jarNameString = bootProp.getProperty("load", "");
			StringTokenizer st = new StringTokenizer(jarNameString, ",");
			while (st.hasMoreTokens()) {
				String filename = st.nextToken();
				jarFiles.add(new File(jarFolder + filename).toURI().toURL());
			}
			System.out.println("our classpath: " + jarNameString);
		} else {
			System.out.println("no jar.properties");
		}
		
		// add boot classpath at the end so that those classes
		// are loaded by our classloader as well (otherwise the dependencies
		// would be loaded by the system classloader as well).
		String vmClasspath = System.getProperty("java.class.path", "");
		System.out.println("vm  classpath: " + vmClasspath);
		StringTokenizer st = new StringTokenizer(vmClasspath, ":;");
		while (st.hasMoreTokens()) {
			String filename = st.nextToken();
			jarFiles.add(new File(filename).toURI().toURL());
		}

	    // Create new class loader which the list of .jar-files as classpath
		URL[] urlArray = jarFiles.toArray(new URL[jarFiles.size()]);
	    ClassLoader loader = new ButtomUpOrderClassLoader(urlArray, this.getClass().getClassLoader());

	    return loader;
	}

	/**
	 * Starts the main-method of specified class after
	 * dynamically building the classpath
	 *
	 * @param className name of class with "main"-method
	 * @param args command line arguments
	 */
	public void boot(String className, String[] args) {
		init();

		// invoke update handling first
		try {
			ClassLoader classLoader = createClassloader();
			// is this the initial download (or do we already have the program downloaded)?
			boolean initialDownload = false;
			try {
				classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				initialDownload = true;
			}
			
			// start update handling
			Class clazz = classLoader.loadClass("games.stendhal.client.update.UpdateManager");
			Method method = clazz.getMethod("process", String.class, Properties.class, Boolean.class);
			method.invoke(clazz.newInstance(), jarFolder, bootProp, initialDownload);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Something nasty happend while trying to build classpath for UpdateManager.\r\nPlease open a bug report at http://sf.net/projects/arianne with this error message:\r\n" + e);
			e.printStackTrace(System.err);
		}

		// store boot prop (if they ware altered during update)
		try {
			saveBootProp();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Sorry, an error occured while downloading the update. Could not write bootProperties");
		}

		// load program (regenerate classloader stuff)
		try {
			ClassLoader classLoader = createClassloader();
			// Thread.currentThread().setContextClassLoader(classLoader);
			Class clazz = classLoader.loadClass(className);
			Method method = clazz.getMethod("main", args.getClass());
			method.invoke(null, (Object) args);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Something nasty happend while trying to build classpath.\r\nPlease open a bug report at http://sf.net/projects/arianne with this error message:\r\n" + e);
			e.printStackTrace(System.err);
			try {
				Class clazz = Class.forName(className);
				Method method = clazz.getMethod("main", args.getClass());
				method.invoke(null, (Object) args);
			} catch (Exception err) {
				err.printStackTrace(System.err);
			}
		}
	}

	public static void init(String filename) {

		InputStream propsFile = Log4J.class.getClassLoader()
				.getResourceAsStream(filename);
		try {
			Properties props = new Properties();
			if (propsFile == null) {
				System.err.println("Cannot find " + filename
						+ " in classpath. Using default properties.");
			} else {
				System.out.println("Configuring Log4J using " + filename);
				props.load(propsFile);
			}
			PropertyConfigurator.configure(props);
		} catch (IOException ioe) {
			System.err.println("cannot read property-file because " + ioe.getMessage());
		}
	}
}
