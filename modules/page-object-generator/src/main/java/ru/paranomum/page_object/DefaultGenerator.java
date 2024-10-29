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

package ru.paranomum.page_object;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.api.TemplateDefinition;
import ru.paranomum.page_object.api.TemplatePathLocator;
import ru.paranomum.page_object.api.TemplateProcessor;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;
import ru.paranomum.page_object.config.GlobalSettings;
import ru.paranomum.page_object.templating.CommonTemplateContentLocator;
import ru.paranomum.page_object.templating.GeneratorTemplateContentLocator;
import ru.paranomum.page_object.templating.MustacheEngineAdapter;
import ru.paranomum.page_object.templating.TemplateManagerOptions;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.removeStart;

@SuppressWarnings("rawtypes")
public class DefaultGenerator implements Generator {
    private static final String METADATA_DIR = ".openapi-generator";
    protected final Logger LOGGER = LoggerFactory.getLogger(DefaultGenerator.class);
    private final boolean dryRun;
    protected CodegenConfig config;
    protected ClientOptInput opts;
    private Boolean generateSupportingFiles = null;
    private Boolean generateModelTests = null;
    private Boolean generateModelDocumentation = null;
    private String basePath;
    private String contextPath;
    private Map<String, String> generatorPropertyDefaults = new HashMap<>();
    /**
     *  Retrieves an instance to the configured template processor, available after user-defined options are
     *  applied via 
     */
    @Getter protected TemplateProcessor templateProcessor = null;

    private List<TemplateDefinition> userDefinedTemplates = new ArrayList<>();
    private String generatorCheck = "spring";
    private String templateCheck = "apiController.mustache";


    public DefaultGenerator() {
        this(false);
    }

    public DefaultGenerator(Boolean dryRun) {
        this.dryRun = Boolean.TRUE.equals(dryRun);
        LOGGER.info("Generating with dryRun={}", this.dryRun);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Generator opts(ClientOptInput opts) {
        this.opts = opts;
        this.config = opts.getConfig();

        List<TemplateDefinition> userFiles = opts.getUserDefinedTemplates();
        if (userFiles != null) {
            this.userDefinedTemplates = Collections.unmodifiableList(userFiles);
        }

        TemplateManagerOptions templateManagerOptions = new TemplateManagerOptions(this.config.isEnableMinimalUpdate(), this.config.isSkipOverwrite());


        TemplatingEngineAdapter templatingEngine = this.config.getTemplatingEngine();

        if (templatingEngine instanceof MustacheEngineAdapter) {
            MustacheEngineAdapter mustacheEngineAdapter = (MustacheEngineAdapter) templatingEngine;
            mustacheEngineAdapter.setCompiler(this.config.processCompiler(mustacheEngineAdapter.getCompiler()));
        }

        TemplatePathLocator commonTemplateLocator = new CommonTemplateContentLocator();
        TemplatePathLocator generatorTemplateLocator = new GeneratorTemplateContentLocator(this.config);
        this.templateProcessor = new TemplateManager(
                templateManagerOptions,
                templatingEngine,
                new TemplatePathLocator[]{generatorTemplateLocator, commonTemplateLocator}
        );

        return this;
    }

    @Override
    public List<File> generate() {
        return null;
    }

}
