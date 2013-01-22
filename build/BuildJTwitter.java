import java.io.File;
import java.util.Arrays;
import java.util.List;

import winterwell.bob.BuildTask;
import winterwell.bob.tasks.CopyTask;
import winterwell.bob.tasks.GitTask;
import winterwell.bob.tasks.JarTask;
import winterwell.bob.tasks.JavaDocTask;
import winterwell.bob.tasks.ZipTask;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;
import winterwell.utils.io.FileUtils;
import winterwell.utils.reporting.Log;



public class BuildJTwitter extends BuildTask {


	@Override
	public void doTask() throws Exception {
		// The project directory
		File base = FileUtils.getWorkingDirectory();
		File projectDir = base;
		System.out.println(base.getAbsolutePath());
		File bin = new File(base, "bin");
		File src = new File(base, "src");
		File srcExtra = new File(base, "src-extra");
		File lib = new File(base, "lib");
		assert src.isDirectory();
//		// Compile
//		CompileTask compile = new CompileTask(new File("src"), new File("bin"));
//		compile.setTargetVersion("1.5");
//		compile.setSourceVersion("1.5");
//		compile.run();
		// Jar
		File jarFile = new File(base, "jtwitter.jar");
//		File oauth = new File("OAuthHttpClient.java");
//		JarTask jarMisc = new JarTask(jarFile, Arrays.asList(oauth), new File(""));
//		jarMisc.run();
		JarTask jar = new JarTask(jarFile, bin);
//		jar.setAppend(true);|
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, Twitter.version);
		jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, Twitter.class.getName());
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, "JTwitter client library by Winterwell");
		jar.run();
		
		// Doc
		File doc = new File(base, "doc");
		JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", src, doc);
		doctask.run();
		
		// zip
		// ... clean out the old
		File[] zips = FileUtils.ls(base, "jtwitter.+zip");
		for (File file : zips) {
			FileUtils.delete(file);
		}		
		File zipFile = new File(base, "jtwitter-"+Twitter.version+".zip");
		List<File> inputFiles = Arrays.asList(
				jarFile, 
				src, 
				lib, srcExtra);
//				new File(base, "test"));
		ZipTask zipTask = new ZipTask(zipFile, inputFiles, base);
		zipTask.run();
		
		// Publish to www
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
		webpage = webpage.replaceAll("<span class='version'>[0-9\\.]+</span>", 
									"<span class='version'>"+Twitter.version+"</span>");
		webpage = webpage.replaceAll("jtwitter-[0-9\\-\\.]+\\.zip", zipFile.getName());
		FileUtils.write(webPageFile, webpage);
		
		// Git stuff
		// Commit changes		
		GitTask git = new GitTask(GitTask.COMMIT, webDir);
		git.setMessage("Publishing JTwitter");
		git.run();
		// Pull and rebase
		git = new GitTask(GitTask.PULL, webDir);
		git.run();
		// Push up to webserver (this publishes the site automagically)
		git = new GitTask(GitTask.PUSH, webDir);
		git.run();

		// Tweet!
		OAuthSignpostClient client = new OAuthSignpostClient(OAuthSignpostClient.JTWITTER_OAUTH_KEY, 
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET, 
				TwitterTest.TEST_ACCESS_TOKEN[0], TwitterTest.TEST_ACCESS_TOKEN[1]);
		Twitter twitter = new Twitter("jtwit", client);
		twitter.setStatus("Released a new version of JTwitter Java library: v"+Twitter.version
				+" http://winterwell.com/software/jtwitter.php");
		twitter.setStatus("Thanks to jake & Vicent @GitHub for awesome-grade customer service :)");

		try {
			Twitter identica = new Twitter("jtwit", TwitterTest.TEST_PASSWORD);
			identica.setAPIRootUrl("http://identi.ca/api");
			identica.setStatus("Released a new version of JTwitter Java library: v"+Twitter.version
					+" http://winterwell.com/software/jtwitter.php");
		} catch(Exception ex) {
			// oh well
			Log.e("build", ex);
		}
		
//		// copy in utils lib		??
//		utils = new File(FileUtils.getWinterwellDir(), "code/lib/winterwell.utils.jar")
//		FileUtils.copy(utils, 
//				new File("winterwell.utils.jar"));
	}


}
