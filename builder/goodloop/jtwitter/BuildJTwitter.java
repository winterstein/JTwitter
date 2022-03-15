package goodloop.jtwitter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JavaDocTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.ZipTask;
import com.winterwell.bob.wwjobs.BuildHacks;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

public class BuildJTwitter extends BuildWinterwellProject {

	/**
	 * Copy from Twitter.version
	 */
	private static final String VERSION = "3.8.6";
	
	public BuildJTwitter() {
		super("jtwitter");
		setVersion(VERSION);
		setMainClass("winterwell.jtwitter.Twitter");
	}	

	@Override
	public void doTask() throws Exception {
		super.doTask();
		if ( ! (""+BuildHacks.getServerType()).equalsIgnoreCase("local")) {
			return;			
		}
		
		// Doc
		File doc = new File(projectDir, "doc");
		try {			
			JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", getJavaSrcDir(), doc);			
//			doctask.setDoclintFlag(true);
			doctask.run();
		} catch(Exception ex) {
			try {
				// Probably Java 8 badness -- try again
				JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", getJavaSrcDir(), doc);			
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
		File[] zips = FileUtils.ls(projectDir, "jtwitter.+zip");
		for (File file : zips) {
			FileUtils.delete(file);
		}		
		File zipFile = new File(projectDir, "jtwitter-"+VERSION+".zip");
		List<File> inputFiles = Arrays.asList(
				getJar(), 
				getJavaSrcDir(), 
				new File(projectDir, "lib")
//				srcExtra
				);
//				new File(base, "test"));
		ZipTask zipTask = new ZipTask(zipFile, inputFiles, projectDir);
		zipTask.run();
		
		// Publish to www
	
		try {
			// FIXME: Hardcoded path
			File webDir = new File("/home/daniel/winterwell/www/software/jtwitter");
			assert getJar().exists();
			FileUtils.copy(getJar(), webDir);
									
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
			
			FileUtils.copy(new File(getJavaSrcDir(), "winterwell/jtwitter/Twitter.java"), webDir);
			FileUtils.copy(new File(projectDir, "changelist.txt"), webDir);
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

}
