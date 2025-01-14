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

import com.samskivert.mustache.Mustache.Compiler;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;
import ru.paranomum.page_object.meta.GeneratorMetadata;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CodegenConfig {
    String getFilesMetadataFilename();

    String getVersionMetadataFilename();

    void postProcess();

    GeneratorMetadata getGeneratorMetadata();

    String sanitizeName(String text);

    String getName();

    String getHelp();

    Map<String, Object> additionalProperties();

    Map<String, String> serverVariableOverrides();

    Map<String, Object> vendorExtensions();

    Map<String, String> templateOutputDirs();

    String testPackage();

    String apiPackage();

    String apiFileFolder();

    String apiTestFileFolder();

    String apiDocFileFolder();

    String fileSuffix();

    String outputFolder();

    String templateDir();

    String embeddedTemplateDir();

    String modelFileFolder();

    String modelTestFileFolder();

    String modelDocFileFolder();

    String modelPackage();

    String toApiName(String name);

    String toApiVarName(String name);

    String toModelName(String name);

    String toParamName(String name);

    String escapeText(String text);

    String escapeTextWhileAllowingNewLines(String text);

    String encodePath(String text);

    String escapeUnsafeCharacters(String input);

    String escapeReservedWord(String name);

    String escapeQuotationMark(String input);


    String getTypeDeclaration(String name);

    void processOpts();

    List<CliOption> cliOptions();


    Set<String> reservedWords();

    String getInputSpec();

    String getConfigFile();

    void setInputSpec(String inputSpec);

    String getOutputDir();

    void setOutputDir(String dir);

    void setConfigFile(String configFile);

    Set<String> defaultIncludes();

    Map<String, String> typeMapping();

    Map<String, String> instantiationTypes();

    Map<String, String> importMapping();

    Map<String, String> schemaMapping();

    Map<String, String> inlineSchemaNameMapping();

    Map<String, String> inlineSchemaOption();

    Map<String, String> nameMapping();

    Map<String, String> parameterNameMapping();

    Map<String, String> modelNameMapping();

    Map<String, String> enumNameMapping();

    Map<String, String> operationIdNameMapping();

    Map<String, String> openapiNormalizer();

    Map<String, String> apiTemplateFiles();

    Map<String, String> modelTemplateFiles();

    Map<String, String> apiTestTemplateFiles();

    Map<String, String> modelTestTemplateFiles();

    Map<String, String> apiDocTemplateFiles();

    Map<String, String> modelDocTemplateFiles();

    Set<String> languageSpecificPrimitives();

    Set<String> openapiGeneratorIgnoreList();

    Map<String, String> reservedWordsMappings();

    Compiler processCompiler(Compiler compiler);

    TemplatingEngineAdapter processTemplatingEngine(TemplatingEngineAdapter templatingEngine);

    String toApiFilename(String name);

    String toModelFilename(String name);

    String toApiTestFilename(String name);

    String toModelTestFilename(String name);

    String toApiDocFilename(String name);

    String toModelDocFilename(String name);

    String toModelImport(String name);

    Map<String, String> toModelImportMap(String name);

    String toApiImport(String name);

    String modelFilename(String templateName, String modelName);

    String modelFilename(String templateName, String modelName, String outputDir);

    String apiFilename(String templateName, String tag);

    String apiFilename(String templateName, String tag, String outputDir);

    String apiTestFilename(String templateName, String tag);

    String apiDocFilename(String templateName, String tag);

    boolean shouldOverwrite(String filename);

    boolean isSkipOverwrite();

    void setSkipOverwrite(boolean skipOverwrite);

    boolean isRemoveOperationIdPrefix();

    void setRemoveOperationIdPrefix(boolean removeOperationIdPrefix);

    boolean isSkipOperationExample();

    void setSkipOperationExample(boolean skipOperationExample);

    boolean isSkipSortingOperations();

    void setSkipSortingOperations(boolean skipSortingOperations);

    public boolean isHideGenerationTimestamp();

    public void setHideGenerationTimestamp(boolean hideGenerationTimestamp);

    Map<String, String> supportedLibraries();

    void setLibrary(String library);

    /**
     * Library template (sub-template).
     *
     * @return library template
     */
    String getLibrary();

    void setGitHost(String gitHost);

    String getGitHost();

    void setGitUserId(String gitUserId);

    String getGitUserId();

    void setGitRepoId(String gitRepoId);

    String getGitRepoId();

    void setReleaseNote(String releaseNote);

    String getReleaseNote();

    void setHttpUserAgent(String httpUserAgent);

    String getHttpUserAgent();

    void setDocExtension(String docExtension);

    String getDocExtension();

    void setIgnoreFilePathOverride(String ignoreFileOverride);

    String getIgnoreFilePathOverride();

    void postProcessFile(File file, String fileType);

    boolean isEnablePostProcessFile();

    void setEnablePostProcessFile(boolean isEnablePostProcessFile);

    void setTemplatingEngine(TemplatingEngineAdapter s);

    TemplatingEngineAdapter getTemplatingEngine();

    public boolean isEnableMinimalUpdate();

    public void setEnableMinimalUpdate(boolean isEnableMinimalUpdate);

    boolean isStrictSpecBehavior();

    void setStrictSpecBehavior(boolean strictSpecBehavior);

    boolean isRemoveEnumValuePrefix();

    void setRemoveEnumValuePrefix(boolean removeEnumValuePrefix);

    String defaultTemplatingEngine();

//    GeneratorLanguage generatorLanguage();

    /*
    the version of the language that the generator implements
    For python 3.9.0, generatorLanguageVersion would be "3.9.0"
    */
    String generatorLanguageVersion();

    boolean isTypeErasedGenerics();

//    List<VendorExtension> getSupportedVendorExtensions();

    boolean getUseInlineModelResolver();

    boolean getAddSuffixToDuplicateOperationNicknames();

    boolean getUseOpenapiNormalizer();

    Set<String> getOpenapiGeneratorIgnoreList();

}
