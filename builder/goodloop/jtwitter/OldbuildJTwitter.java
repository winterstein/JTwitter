package goodloop.jtwitter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.JavaDocTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.bob.tasks.ZipTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
/**
 * OLD - does not use {@link BuildWinterwellProject}
 * @author daniel
 *
 */
public class OldbuildJTwitter extends BuildTask {

	/**
	 * Copy from Twitter.version
	 */
	private static final String VERSION = "3.8.3";
	
	private File base;

	public OldbuildJTwitter() {
		// The project directory
		base = new WinterwellProjectFinder().apply("jtwitter");				
//				new File(FileUtils.getWinterwellDir(), "jtwitter"); 
				// FileUtils.getWorkingDirectory();
		assert base.isDirectory() : base;
	}

	@Override
	public void doTask() throws Exception {
		File projectDir = base;
		System.out.println(base.getAbsolutePath());
		File bin = new File(base, "bin");
		File src = new File(base, "src");
		File srcExtra = new File(base, "src-extra");
		File lib = new File(base, "lib");
		assert src.isDirectory();

		// Compile
		CompileTask compile = new CompileTask(new File("src"), new File("bin"));
		compile.setDepth(getDepth()+1);
		// classpath
		EclipseClasspath ec = new EclipseClasspath(projectDir);
		ec.setIncludeProjectJars(true);
		Set<File> libs = ec.getCollectedLibs();
		compile.setClasspath(libs);						
		compile.setOutputJavaVersion("1.9"); //TargetVersion("1.5");
		compile.setSrcJavaVersion("1.9");
		compile.setDebug(true);
		compile.run();		
		compile.close();
		
		// Jar
		File jarFile = getJar();
//		File oauth = new File("OAuthHttpClient.java");
//		JarTask jarMisc = new JarTask(jarFile, Arrays.asList(oauth), new File(""));
//		jarMisc.run();
		JarTask jar = new JarTask(jarFile, bin);
//		jar.setAppend(true);|
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, VERSION); // Twitter.version
		jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, "winterwell.jtwitter.Twitter");
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, "JTwitter client library by Winterwell + Good-Loop");
		jar.run();
		
		// Doc
		File doc = new File(base, "doc");
		try {			
			JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", src, doc);			
//			doctask.setDoclintFlag(true);
			doctask.run();
		} catch(Exception ex) {
			try {
				// Probably Java 8 badness -- try again
				JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", src, doc);			
				doctask.setDoclintFlag(true);
				doctask.run();
			} catch(Exception ex2) {
				// WTF?
				System.err.println(ex);
				System.err.println(ex2);
			}
		}
		
		// zip
		// ... clean out the old
		File[] zips = FileUtils.ls(base, "jtwitter.+zip");
		for (File file : zips) {
			FileUtils.delete(file);
		}		
		File zipFile = new File(base, "jtwitter-"+VERSION+".zip");
		List<File> inputFiles = Arrays.asList(
				jarFile, 
				src, 
				lib, srcExtra);
//				new File(base, "test"));
		ZipTask zipTask = new ZipTask(zipFile, inputFiles, base);
		zipTask.run();
		
		// Publish to www
		try {
			// FIXME: Hardcoded path
			File webDir = new File("/home/daniel/winterwell/www/software/jtwitter");
			assert jarFile.exists();
			FileUtils.copy(jarFile, webDir);
									
			File zip2 = FileUtils.copy(zipFile, webDir);
			// ... clean out the old zips
			zips = FileUtils.ls(webDir, "jtwitter.+zip");
			for (File file : zips) {
				if (file.getName().equals(zip2.getName())) continue;
				FileUtils.delete(file);
			}
			// git add
			GitTask git0 = new GitTask(GitTask.ADD, zip2);
			git0.run();
			
			FileUtils.copy(new File(src, "winterwell/jtwitter/Twitter.java"), webDir);
			FileUtils.copy(new File(base,"changelist.txt"), webDir);
			CopyTask copydoc = new CopyTask(doc, new File(webDir, "javadoc"));
			copydoc.run();
			// Update the version number
			File webPageFile = new File(webDir, "../jtwitter.php");
			String webpage = FileUtils.read(webPageFile);
			Pattern pattern = Pattern.compile("<span class='version'>[0-9\\.]+</span>");
			Matcher m = pattern.matcher(webpage);
			assert m.find();
			webpage = webpage.replaceAll("<span class='version'>[0-9\\.]+</span>", 
										"<span class='version'>"+VERSION+"</span>");
			webpage = webpage.replaceAll("jtwitter-[0-9\\-\\.]+\\.zip", zipFile.getName());
			FileUtils.write(webPageFile, webpage);
		
			// Git stuff
			// Commit changes		
			GitTask git = new GitTask(GitTask.COMMIT_ALL, webDir);
			git.setMessage("Publishing JTwitter");
			git.run();
			// Pull and rebase
			git = new GitTask(GitTask.PULL, webDir);
			git.run();
			// Push up to webserver (this publishes the site automagically)
			git = new GitTask(GitTask.PUSH, webDir);
			git.run();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		// attempt to upload (copied from BuildWinterwellProject)
		try {
			String remoteJar = "/home/winterwell/public-software/"+getJar().getName();
			SCPTask scp = new SCPTask(getJar(), "winterwell@winterwell.com",				
					remoteJar);
			// this is online at: https://www.winterwell.com/software/downloads
			scp.setMkdirTask(false);
//			scp.runInThread();
			scp.run();
			report.put("scp to remote", "winterwell.com:"+remoteJar);
		} catch(Throwable ex) {
			Log.i(LOGTAG, ex); // oh well
		}				
	}

	public File getJar() {
		return new File(base, "jtwitter.jar");
	}


}
