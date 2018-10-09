package winterwell.jtwitter;
//package winterwell.jtwitter;
//import java.io.File;
//import java.util.Arrays;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import winterwell.bob.BuildTask;
//import winterwell.bob.tasks.CopyTask;
//import winterwell.bob.tasks.GitTask;
//import winterwell.bob.tasks.JarTask;
//import winterwell.bob.tasks.JavaDocTask;
//import winterwell.bob.tasks.SCPTask;
//import winterwell.bob.tasks.ZipTask;
//import com.winterwell.utils.io.FileUtils;
//import com.winterwell.utils.log.Log;
//
//
///**
// * A version of BuildJTwitter for stable-closed-code
// * 
// * This makes the jar locally. It does NOT update the website.
// * 
// * @author daniel
// *
// */
//public class MakeJTwitterStableClosedCode extends BuildTask {
//
//	
//	private File base;
//
//	public BuildJTwitterStableClosedCode() {
//		// The project directory
//		base = new File(FileUtils.getWinterwellDir(), "jtwitter"); // FileUtils.getWorkingDirectory();
//		assert base.isDirectory() : base;
//	}
//
//	@Override
//	public void doTask() throws Exception {
//		File projectDir = base;
//		System.out.println(base.getAbsolutePath());
//		File bin = new File(base, "bin");
//		File src = new File(base, "src");
//		File srcExtra = new File(base, "src-extra");
//		File lib = new File(base, "lib");
//		assert src.isDirectory();
////		// Compile -- NO! Relies on Eclipse!
//		// Jar
//		File jarFile = getJar();
//		JarTask jar = new JarTask(jarFile, bin);
//		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, Twitter.version);
//		jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, Twitter.class.getName());
//		jar.setManifestProperty(JarTask.MANIFEST_TITLE, "JTwitter client library by Winterwell");
//		jar.run();
//		
//		// zip
//		// ... clean out the old
//		File[] zips = FileUtils.ls(base, "jtwitter.+zip");
//		for (File file : zips) {
//			FileUtils.delete(file);
//		}		
//		File zipFile = new File(base, "jtwitter-"+Twitter.version+".zip");
//		List<File> inputFiles = Arrays.asList(
//				jarFile, 
//				src, 
//				lib, srcExtra);
////				new File(base, "test"));
//		ZipTask zipTask = new ZipTask(zipFile, inputFiles, base);
//		zipTask.run();
//		
//	}
//
//	public File getJar() {
//		return new File(base, "jtwitter.jar");
//	}
//
//
//}
