import java.io.File;
import java.util.Arrays;
import java.util.List;

import winterwell.bob.BuildTask;
import winterwell.bob.tasks.CompileTask;
import winterwell.bob.tasks.CopyTask;
import winterwell.bob.tasks.GitTask;
import winterwell.bob.tasks.JarTask;
import winterwell.bob.tasks.JavaDocTask;
import winterwell.bob.tasks.ZipTask;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;
import winterwell.utils.io.FileUtils;



public class BuildJTwitter extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		// The project directory
		File base = FileUtils.getWorkingDirectory();
		File projectDir = base;
		System.out.println(base.getAbsolutePath());
		File bin = new File(base, "bin");
		File src = new File(base, "src");
		File lib = new File(base, "lib");
		assert src.isDirectory();
		// Compile
		CompileTask compile = new CompileTask(new File("src"), new File("bin"));
		compile.setTargetVersion("1.5");
		compile.run();
		// Jar
		File jarFile = new File(base, "jtwitter.jar");
//		File oauth = new File("OAuthHttpClient.java");
//		JarTask jarMisc = new JarTask(jarFile, Arrays.asList(oauth), new File(""));
//		jarMisc.run();
		JarTask jar = new JarTask(jarFile, bin);
//		jar.setAppend(true);
		jar.run();
		
//		JarTask jar2 = new JarTask(new File(base, "jtwitter.jar"), src);
//		jar2.setAppend(true);
//		jar2.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, Twitter.version);
//		jar2.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, Twitter.class.getCanonicalName());
//		jar2.run();
		
		// Doc
		File doc = new File(base, "doc");
		JavaDocTask doctask = new JavaDocTask("winterwell.jtwitter", src, doc);
		doctask.run();
		
		// zip
		File zipFile = new File(base, "jtwitter-"+Twitter.version+".zip");
		List<File> inputFiles = Arrays.asList(jarFile, src, lib, new File(base, "test"));
		ZipTask zipTask = new ZipTask(zipFile, inputFiles, base);
		zipTask.run();
		
		// Publish to www
		// FIXME: Hardcoded path
		File webDir = new File("/home/daniel/winterwell/www/software/jtwitter");
		assert jarFile.exists();
		FileUtils.copy(jarFile, webDir);
		FileUtils.copy(new File(src, "winterwell/jtwitter/Twitter.java"), webDir);
		FileUtils.copy(new File(base,"changelist.txt"), webDir);
		CopyTask copydoc = new CopyTask(doc, new File(webDir, "javadoc"));
		copydoc.run();
		// Update the version number
		File webPageFile = new File(webDir, "../jtwitter.php");
		String webpage = FileUtils.read(webPageFile);
		webpage = webpage.replaceAll("<span class='version'>[0-9\\.]+</span>", 
									"<span class='version'>"+Twitter.version+"</span>");
		webpage = webpage.replaceAll("jtwitter-[0-9\\-\\.]+.zip", zipFile.getName());
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

		Twitter identica = new Twitter("jtwit", TwitterTest.TEST_PASSWORD);
		identica.setAPIRootUrl("http://identi.ca/api");
		identica.setStatus("Released a new version of JTwitter Java library: v"+Twitter.version
				+" http://winterwell.com/software/jtwitter.php");
	}


}
