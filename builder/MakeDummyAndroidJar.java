import java.io.File;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.JarTask;


public class MakeDummyAndroidJar extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		JarTask jt = new JarTask(new File("dummy/android.jar"), new File("binlocal"));
		jt.run();
	}

}
