package ru.paranomum.page_object.cmd;

import io.airlift.airline.Command;
import io.airlift.airline.Help;

import javax.inject.Inject;

@Command(name = "help", description = "Display help information about openapi-generator")
public class HelpCommand extends PageObjectGeneratorCommand {

    @Inject
    public Help help;

    @Override
    public void execute() {
        help.call();
    }
}
