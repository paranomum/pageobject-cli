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
import ru.paranomum.page_object.model.*;
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
    private String basePath;
    private String contextPath;

    private InputSpecModel files = null;
    private List<ModelCodegen> modelsToGenerate = new ArrayList<>();
    private ConfigModel configModel;
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
            files = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false).readValue(spec, InputSpecModel.class);
        } catch(IOException e) {
            LOGGER.info("ITS IOEXCEPTION %s", e);
        }
        configModel = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false).readValue(new File(config.getConfigFile()), ConfigModel.class);

        for (InputSpecModel.HtmlFiles file : files.files) {
            modelsToGenerate.add(parseHtmlToModel(file));
        }

        generateModels(modelsToGenerate);
        return null;
    }

    private void generateModels(List<ModelCodegen> model) throws URISyntaxException, IOException {
        Pattern pattern = Pattern.compile("/[a-z-]*.jar");
        Matcher matcher = pattern.matcher(new File(DefaultGenerator.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getPath());
        String outputDir = matcher.replaceAll("");
        processTemplateToFile(model, outputDir + "/generated_files");
    }

    private ModelCodegen parseHtmlToModel(InputSpecModel.HtmlFiles file) throws IOException {
        Document doc = null;
        try {
            File spec = new File(file.pathToHtml);
            doc = Jsoup.parse(spec);
        } catch(IOException e) {
            LOGGER.info("ITS IOEXCEPTION %s", e);
        }
        ModelCodegen model = new ModelCodegen();
        model.packageName = file.packageName;
        model.className = file.className;

        if (doc != null) {
            Document finalDoc = doc;
            configModel.getConfiguration().stream().parallel().forEach(type -> {
                List<VarModelCodegen> vars = new ArrayList<>();
                List<DataVar> dataVars = new ArrayList<>();
                Elements elements = finalDoc.selectXpath(type.xpath);
                if (!elements.isEmpty()) {
                    model.setImport(Map.of("import", type.toImport));
                    for (Element el : elements) {
                        Set<String> initData = new HashSet<>();
                        VarModelCodegen var = new VarModelCodegen();
                        var.type = type.type;
                        if (!type.attributeToInit.isEmpty()) {
                            for (String attr : type.attributeToInit) {
                                String attrEl = el.attr(attr);
                                if (isOnlyRussian(attrEl))
                                    initData.add(attrEl);
                            }
                        }
                        if (!type.innerXpathToInit.isEmpty()) {
                            for (String inner : type.innerXpathToInit) {
                                String attr;
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
                            char c[] = transliterate(var.toInit).toCharArray();
                            if (Character.isUpperCase(c[0]))
                                c[0] += 32;
                            var.varName = new String(c);
                            if (vars.stream().anyMatch(e -> e.varName.equals(var.getVarName()))) {
                                var.needIndex = true;
                                var.index = vars.stream()
                                        .filter(e -> e.varName.equals(var.getVarName())).count() + 1;
                            }
                            if (type.dataType != null && DataTypeEnum.valueOfLabel(type.dataType) != null) {
                                DataVar dataVar = new DataVar();
                                DataTypeEnum toInit = DataTypeEnum.valueOfLabel(type.dataType);
                                dataVar.name = var.varName;
                                dataVar.type = toInit.toString();
                                if (toInit.initData() != null) {
                                    dataVar.needToInit = true;
                                    dataVar.init = toInit.initData();
                                }
                                dataVars.add(dataVar);
                            }
                            vars.add(var);
                            el.remove();
                        }
                    }
                }
                model.vars.addAll(vars);
                model.dataVars.addAll(dataVars);
            });
        }
        if (!model.dataVars.isEmpty()) {
            model.hasDataVars = true;
            char c[] = model.className.toCharArray();
            if (Character.isUpperCase(c[0]))
                c[0] += 32;
            model.dataVarName = new String(c);
        }
        return model;
    }

    private final Set<String> seenFiles = new HashSet<>();

    private void processTemplateToFile(List<ModelCodegen> models, String outputDir){
        models.stream().parallel().forEach(templateData -> {
            LOGGER.info("Generating class with name {}", templateData.className);
            Path outDir = java.nio.file.Paths.get(outputDir).toAbsolutePath();
            String adjustedOutputFilename = outDir + "/" + templateData.className + ".java";
            File target = new File(adjustedOutputFilename);
            Path absoluteTarget = target.toPath().toAbsolutePath();
            if (!absoluteTarget.startsWith(outDir)) {
                throw new RuntimeException(String.format(Locale.ROOT, "Target files must be generated within the output directory; absoluteTarget=%s outDir=%s", absoluteTarget, outDir));
            }

            if (seenFiles.stream().filter(f -> f.toLowerCase(Locale.ROOT).equals(absoluteTarget.toString().toLowerCase(Locale.ROOT))).findAny().isPresent()) {
                LOGGER.warn("Duplicate file path detected. Not all operating systems can handle case sensitive file paths. path={}", absoluteTarget.toString());
            }
            seenFiles.add(absoluteTarget.toString());
            try {
                this.templateProcessor.write(templateData, "page-object-class.mustache", target);
            } catch (IOException ignore) {}
        });
    }
}
