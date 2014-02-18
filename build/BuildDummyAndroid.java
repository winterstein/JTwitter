import java.io.File;

import winterwell.bob.BuildTask;
import winterwell.bob.tasks.JarTask;


public class BuildDummyAndroid extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		JarTask jt = new JarTask(new File("dummy/android.jar"), new File("binlocal"));
		jt.run();
	}

}
