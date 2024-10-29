/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.paranomum.page_object.cmd;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import ru.paranomum.page_object.ClientOptInput;
import ru.paranomum.page_object.DefaultGenerator;
import ru.paranomum.page_object.Generator;
import ru.paranomum.page_object.GeneratorNotFoundException;
import ru.paranomum.page_object.config.CodegenConfigurator;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings({"java:S106"})
@Command(name = "generate", description = "Generate code with the specified generator.")
public class Generate extends PageObjectGeneratorCommand {

    CodegenConfigurator configurator;
    Generator generator;

    @Option(name = {"-g", "--generator-name"}, title = "generator name",
            description = "generator to use (see list command for list)")
    private String generatorName;

    @Option(name = {"-o", "--output"}, title = "output directory",
            description = "where to write the generated files (current dir by default)")
    private String output = "";

    @Override
    public void execute() {

        if (isNotEmpty(generatorName)) {
            configurator.setGeneratorName(generatorName);
        }

        if (isNotEmpty(output)) {
            configurator.setOutputDir(output);
        }

        try {
            final ClientOptInput clientOptInput = configurator.toClientOptInput();

            // this null check allows us to inject for unit testing.
            if (generator == null) {
                generator = new DefaultGenerator();
            }

            generator.opts(clientOptInput);
            generator.generate();
        } catch (GeneratorNotFoundException e) {
            System.err.println(e.getMessage());
            System.err.println("[error] Check the spelling of the generator's name and try again.");
            System.exit(1);
        }
    }
}
