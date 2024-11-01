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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.api.TemplateDefinition;
import ru.paranomum.page_object.api.TemplatePathLocator;
import ru.paranomum.page_object.api.TemplateProcessor;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;
import ru.paranomum.page_object.model.ConfigModel;
import ru.paranomum.page_object.model.ModelCodegen;
import ru.paranomum.page_object.model.VarModelCodegen;
import ru.paranomum.page_object.model.WebElementTypes;
import ru.paranomum.page_object.templating.CommonTemplateContentLocator;
import ru.paranomum.page_object.templating.GeneratorTemplateContentLocator;
import ru.paranomum.page_object.templating.MustacheEngineAdapter;
import ru.paranomum.page_object.templating.TemplateManagerOptions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.paranomum.page_object.utils.StringUtils.*;


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

    private Document doc = null;
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
    public List<File> generate() throws URISyntaxException, IOException {
        try {
            File spec = new File(config.getInputSpec());
            doc = Jsoup.parse(spec);
        } catch(IOException e) {
            LOGGER.info("ITS IOEXCEPTION %s", e);
        }
        ModelCodegen model = new ModelCodegen();
        ConfigModel configModel = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false).readValue(new File(config.getConfigFile()), ConfigModel.class);
        model._package = configModel.packageName;

        for (WebElementTypes type : configModel.getConfiguration()) {
            Elements elements = doc.selectXpath(type.xpath);
            if (!elements.isEmpty()) {
                model.setImport(Map.of("import", type.toImport));
                for (Element el : elements) {
                    Set<String> initData = new HashSet<>();
                    VarModelCodegen var = new VarModelCodegen();
                    var.type = type.type;
                    if (!type.attributeToInit.isEmpty()) {
                        for (String attr : type.attributeToInit) {
                            String attrEl = el.attr(attr);
                            if (type.type.equals("LinkButton"))
                                LOGGER.info("attr - {}, attrStr - {}", attr, attrEl);
                            if (isOnlyRussian(attrEl))
                                initData.add(attrEl);
                        }
                    }
                    if (!type.innerXpathToInit.isEmpty()) {
                        for (String inner : type.innerXpathToInit) {
                            String attr = null;
                            if (inner.equals(".")) {
                                attr = el.text();
                                if (isOnlyRussian(attr))
                                    initData.add(attr);
                                continue;
                            }
                            if (!inner.contains("@")) {
                                Elements els = el.selectXpath(inner);
                                if (els.size() > 1) {
                                    for (Element toAttr : els) {
                                        if (isOnlyRussian(toAttr.text()))
                                            initData.add(toAttr.text());
                                    }
                                    continue;
                                }
                                else {
                                    attr = els.text();
                                }
                            }
                            else {
                                attr = el.selectXpath(inner.substring(0,inner.indexOf('@') - 1))
                                        .attr(inner.substring(inner.indexOf('@') + 1));
                            }
                            if (isOnlyRussian(attr))
                                initData.add(attr);
                        }
                    }
                    if (!initData.isEmpty()) {
                        var.toInit = findLongestString(initData);
                        var.varName = transliterate(var.toInit);
                        model.vars.add(var);
                        el.remove();
                    }
                }
            }
        }
        generateModel(model);
        return null;
    }

    private void generateModel(ModelCodegen model) throws URISyntaxException, IOException {
        Pattern pattern = Pattern.compile("/[a-z-]*.jar");
        Matcher matcher = pattern.matcher(new File(DefaultGenerator.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getPath());
        String outputDir = matcher.replaceAll("");
        File written = processTemplateToFile(model, outputDir);
    }

    private final Set<String> seenFiles = new HashSet<>();

    private File processTemplateToFile(ModelCodegen templateData, String outputDir) throws IOException {
        Path outDir = java.nio.file.Paths.get(outputDir).toAbsolutePath();
        String adjustedOutputFilename = outDir + "/PageObjectClass.java";
        File target = new File(adjustedOutputFilename);
        Path absoluteTarget = target.toPath().toAbsolutePath();
        if (!absoluteTarget.startsWith(outDir)) {
            throw new RuntimeException(String.format(Locale.ROOT, "Target files must be generated within the output directory; absoluteTarget=%s outDir=%s", absoluteTarget, outDir));
        }

        if (seenFiles.stream().filter(f -> f.toLowerCase(Locale.ROOT).equals(absoluteTarget.toString().toLowerCase(Locale.ROOT))).findAny().isPresent()) {
            LOGGER.warn("Duplicate file path detected. Not all operating systems can handle case sensitive file paths. path={}", absoluteTarget.toString());
        }
        seenFiles.add(absoluteTarget.toString());
        return this.templateProcessor.write(templateData, "page-object-class.mustache", target);
    }
}
