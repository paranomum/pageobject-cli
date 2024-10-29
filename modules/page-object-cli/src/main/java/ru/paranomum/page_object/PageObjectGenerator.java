package ru.paranomum.page_object;

import io.airlift.airline.Cli;
import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseOptionMissingException;
import io.airlift.airline.ParseOptionMissingValueException;
import ru.paranomum.page_object.cmd.BuildInfo;
import ru.paranomum.page_object.cmd.Generate;
import ru.paranomum.page_object.cmd.HelpCommand;
import ru.paranomum.page_object.cmd.PageObjectGeneratorCommand;

import java.util.Locale;

import static ru.paranomum.page_object.Constants.CLI_NAME;

/**
 * User: lanwen Date: 24.03.15 Time: 17:56
 * <p>
 * Command line interface for PageObject Generator use `page-object-cli.jar help` for more info
 */
public class PageObjectGenerator {

    public static void main(String[] args) {
        BuildInfo buildInfo = new BuildInfo();

        Cli.CliBuilder<PageObjectGeneratorCommand> builder =
                Cli.<PageObjectGeneratorCommand>builder(CLI_NAME)
                        .withDescription(
                                String.format(
                                        Locale.ROOT,
                                        "OpenAPI Generator CLI %s (%s).",
                                        buildInfo.getVersion(),
                                        buildInfo.getSha()))
                        .withDefaultCommand(HelpCommand.class)
                        .withCommands(
//                                ListGenerators.class,
                                HelpCommand.class,
                                Generate.class
//                                Meta.class,
//                                ConfigHelp.class,
//                                Validate.class,
//                                Version.class,
//                                CompletionCommand.class,
//                                GenerateBatch.class
                        );
        try {
            builder.build().parse(args).run();

            // If CLI runs without a command, consider this an error. This exists after initial parse/run
            // so we can present the configured "default command".
            // We can check against empty args because unrecognized arguments/commands result in an exception.
            // This is useful to exit with status 1, for example, so that misconfigured scripts fail fast.
            // We don't want the default command to exit internally with status 1 because when the default command is something like "list",
            // it would prevent scripting using the command directly. Example:
            //     java -jar cli.jar list --short | tr ',' '\n' | xargs -I{} echo "Doing something with {}"
            if (args.length == 0) {
                System.exit(1);
            }
        } catch (ParseArgumentsUnexpectedException e) {
            System.err.printf(Locale.ROOT, "[error] %s%n%nSee '%s help' for usage.%n", e.getMessage(), CLI_NAME);
            System.exit(1);
        } catch (ParseOptionMissingException | ParseOptionMissingValueException e) {
            System.err.printf(Locale.ROOT, "[error] %s%n", e.getMessage());
            System.exit(1);
        }
    }
}
