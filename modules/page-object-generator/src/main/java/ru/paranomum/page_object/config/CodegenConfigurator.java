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

package ru.paranomum.page_object.config;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.*;
import ru.paranomum.page_object.api.TemplateDefinition;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * A class which manages the contextual configuration for code generation.
 * This includes configuring the generator, templating, and the workflow which orchestrates these.
 *
 * This helper also enables the deserialization of {@link GeneratorSettings} via application-specific Jackson JSON usage
 * (see {@link DynamicSettings}.
 */
@SuppressWarnings("UnusedReturnValue")
public class CodegenConfigurator {

    public static final Logger LOGGER = LoggerFactory.getLogger(CodegenConfigurator.class);

    private GeneratorSettings.Builder generatorSettingsBuilder = GeneratorSettings.newBuilder();
    private WorkflowSettings.Builder workflowSettingsBuilder = WorkflowSettings.newBuilder();

    private String generatorName;
    private String inputSpec;
    private String configFile;
    private String templatingEngineName;

    private List<TemplateDefinition> userDefinedTemplates = new ArrayList<>();

    public CodegenConfigurator() {

    }

    public CodegenConfigurator setTemplatingEngineName(String templatingEngineName) {
        this.templatingEngineName = templatingEngineName;
        workflowSettingsBuilder.withTemplatingEngineName(templatingEngineName);
        return this;
    }

    /**
     * Sets the name of the target generator.
     * <p>
     * The generator's name is used to uniquely identify the generator as a mechanism to lookup the
     * desired implementation at runtime.
     *
     * @param generatorName The name of the generator.
     * @return The fluent instance of {@link CodegenConfigurator}
     */
    public CodegenConfigurator setGeneratorName(final String generatorName) {
        this.generatorName = generatorName;
        generatorSettingsBuilder.withGeneratorName(generatorName);
        return this;
    }

    public CodegenConfigurator setInputSpec(String inputSpec) {
        this.inputSpec = inputSpec;
        workflowSettingsBuilder.withInputSpec(inputSpec);
        return this;
    }

    public CodegenConfigurator setConfigFile(String configFile) {
        this.configFile = configFile;
        workflowSettingsBuilder.withConfigFile(configFile);
        return this;
    }

    public CodegenConfigurator setOutputDir(String outputDir) {
        workflowSettingsBuilder.withOutputDir(outputDir);
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public Context<?> toContext() {
//        Validate.notEmpty(generatorName, "generator name must be specified");
        Validate.notEmpty(inputSpec, "input spec must be specified");

        GeneratorSettings generatorSettings = generatorSettingsBuilder.build();
        CodegenConfig config = CodegenConfigLoader.forName("java");
        if (isEmpty(templatingEngineName)) {
            // if templatingEngineName is empty check the config for a default
            String defaultTemplatingEngine = config.defaultTemplatingEngine();
            workflowSettingsBuilder.withTemplatingEngineName(defaultTemplatingEngine);
        } else {
            workflowSettingsBuilder.withTemplatingEngineName(templatingEngineName);
        }

        // at this point, all "additionalProperties" are set, and are now immutable per GeneratorSettings instance.
        WorkflowSettings workflowSettings = workflowSettingsBuilder.build();

        for (Map.Entry<String, String> entry : workflowSettings.getGlobalProperties().entrySet()) {
            GlobalSettings.setProperty(entry.getKey(), entry.getValue());
        }

        return new Context<>(generatorSettings, workflowSettings);
    }

    public ClientOptInput toClientOptInput() {
        Context<?> context = toContext();
        WorkflowSettings workflowSettings = context.getWorkflowSettings();
        GeneratorSettings generatorSettings = context.getGeneratorSettings();

        // We load the config via generatorSettings.getGeneratorName() because this is guaranteed to be set
        // regardless of entrypoint (CLI sets properties on this type, config deserialization sets on generatorSettings).
        CodegenConfig config = CodegenConfigLoader.forName("java");

        // TODO: Work toward CodegenConfig having a "WorkflowSettings" property, or better a "Workflow" object which itself has a "WorkflowSettings" property.
        config.setInputSpec(workflowSettings.getInputSpec());
        config.setOutputDir(workflowSettings.getOutputDir());
        config.setConfigFile(workflowSettings.getConfigFile());

        TemplatingEngineAdapter templatingEngine = TemplatingEngineLoader.byIdentifier(workflowSettings.getTemplatingEngineName());
        config.setTemplatingEngine(templatingEngine);

        Map<String, String> serverVariables = generatorSettings.getServerVariables();
        if (!serverVariables.isEmpty()) {
            // This is currently experimental due to vagueness in the specification
            LOGGER.warn("user-defined server variable support is experimental.");
            config.serverVariableOverrides().putAll(serverVariables);
        }

        // any other additional properties?
        String templateDir = workflowSettings.getTemplateDir();
        if (templateDir != null) {
            config.additionalProperties().put(CodegenConstants.TEMPLATE_DIR, workflowSettings.getTemplateDir());
        }

        return new ClientOptInput()
                .config(config)
                .generatorSettings(generatorSettings)
                .userDefinedTemplates(userDefinedTemplates);
    }
}
