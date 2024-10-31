package page_object;

import io.airlift.airline.Cli;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.paranomum.page_object.cmd.Generate;

public class GeneratorTest {

	@Test
	@Disabled
	public void setupAndRunTest() {
		final String[] commonArgs =
				{"generate",
						"-i", "/Users/admin/Desktop/pageobject-generator/modules/page-object-cli/src/main/resources/site.html",
						"-c", "/Users/admin/Desktop/pageobject-generator/modules/page-object-cli/src/main/resources/config.json"};

		Cli.CliBuilder<Runnable> builder =
				Cli.<Runnable>builder("page-object-generator-cli")
						.withCommands(Generate.class);

		Generate generate = (Generate) builder.build().parse(commonArgs);
		generate.run();
	}
}
