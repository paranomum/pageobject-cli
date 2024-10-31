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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.jknack.handlebars.internal.text.StringEscapeUtils;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.Lambda;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;
import ru.paranomum.page_object.meta.GeneratorMetadata;
import ru.paranomum.page_object.meta.Stability;
import ru.paranomum.page_object.templating.MustacheEngineAdapter;
import ru.paranomum.page_object.templating.mustache.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.paranomum.page_object.utils.StringUtils.camelize;
import static ru.paranomum.page_object.utils.StringUtils.escape;

public class DefaultCodegen implements CodegenConfig {
    private final Logger LOGGER = LoggerFactory.getLogger(DefaultCodegen.class);

    // A cache of sanitized words. The sanitizeName() method is invoked many times with the same
    // arguments, this cache is used to optimized performance.
    private static final String xSchemaTestExamplesKey = "x-schema-test-examples";
    private static final String xSchemaTestExamplesRefPrefix = "#/components/x-schema-test-examples/";

    static {}

    protected GeneratorMetadata generatorMetadata;
    protected String inputSpec;
    protected String configFile;
    protected String outputFolder = "";
    protected Set<String> defaultIncludes;
    protected Map<String, String> typeMapping;
    // instantiationTypes map from container types only: set, map, and array to the in language-type
    protected Map<String, String> instantiationTypes;
    protected Set<String> reservedWords;
    protected Set<String> languageSpecificPrimitives = new HashSet<>();
    protected Set<String> openapiGeneratorIgnoreList = new HashSet<>();
    protected Map<String, String> importMapping = new HashMap<>();
    // a map to store the mapping between a schema and the new one
    protected Map<String, String> schemaMapping = new HashMap<>();
    // a map to store the mapping between inline schema and the name provided by the user
    protected Map<String, String> inlineSchemaNameMapping = new HashMap<>();
    // a map to store the inline schema naming conventions
    protected Map<String, String> inlineSchemaOption = new HashMap<>();
    // a map to store the mapping between property name and the name provided by the user
    protected Map<String, String> nameMapping = new HashMap<>();
    // a map to store the mapping between parameter name and the name provided by the user
    protected Map<String, String> parameterNameMapping = new HashMap<>();
    // a map to store the mapping between model name and the name provided by the user
    protected Map<String, String> modelNameMapping = new HashMap<>();
    // a map to store the mapping between enum name and the name provided by the user
    protected Map<String, String> enumNameMapping = new HashMap<>();
    // a map to store the mapping between operation id name and the name provided by the user
    protected Map<String, String> operationIdNameMapping = new HashMap<>();
    // a map to store the rules in OpenAPI Normalizer
    protected Map<String, String> openapiNormalizer = new HashMap<>();
    @Setter protected String modelPackage = "", apiPackage = "";
    protected String fileSuffix;
    @Getter @Setter
    protected String modelNamePrefix = "", modelNameSuffix = "";
    @Getter @Setter
    protected String apiNamePrefix = "", apiNameSuffix = "Api";
    protected String testPackage = "";
    @Setter protected String filesMetadataFilename = "FILES";
    @Setter protected String versionMetadataFilename = "VERSION";
    /*
    apiTemplateFiles are for API outputs only (controllers/handlers).
    API templates may be written multiple times; APIs are grouped by tag and the file is written once per tag group.
    */
    protected Map<String, String> apiTemplateFiles = new HashMap<>();
    protected Map<String, String> modelTemplateFiles = new HashMap<>();
    protected Map<String, String> apiTestTemplateFiles = new HashMap<>();
    protected Map<String, String> modelTestTemplateFiles = new HashMap<>();
    protected Map<String, String> apiDocTemplateFiles = new HashMap<>();
    protected Map<String, String> modelDocTemplateFiles = new HashMap<>();
    protected Map<String, String> reservedWordsMappings = new HashMap<>();
    @Setter protected String templateDir;
    protected String embeddedTemplateDir;
    protected Map<String, Object> additionalProperties = new HashMap<>();
    protected Map<String, String> serverVariables = new HashMap<>();
    protected Map<String, Object> vendorExtensions = new HashMap<>();
    protected Map<String, String> templateOutputDirs = new HashMap<>();
    /*
    Supporting files are those which aren't models, APIs, or docs.
    These get a different map of data bound to the templates. Supporting files are written once.
    See also 'apiTemplateFiles'.
    */
//    protected List<SupportingFile> supportingFiles = new ArrayList<>();
    protected List<CliOption> cliOptions = new ArrayList<>();
    protected boolean skipOverwrite;
    protected boolean removeOperationIdPrefix;
    @Getter @Setter
    protected String removeOperationIdPrefixDelimiter = "_";
    @Getter @Setter
    protected int removeOperationIdPrefixCount = 1;
    protected boolean skipOperationExample;
    // sort operations by default
    protected boolean skipSortingOperations = false;

    protected final static Pattern XML_MIME_PATTERN = Pattern.compile("(?i)application\\/(.*)[+]?xml(;.*)?");
    protected final static Pattern JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");
    protected final static Pattern JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?");
    private static final Pattern COMMON_PREFIX_ENUM_NAME = Pattern.compile("[a-zA-Z0-9]+\\z");

    /**
     * True if the code generator supports multiple class inheritance.
     * This is used to model the parent hierarchy based on the 'allOf' composed schemas.
     */
    protected boolean supportsMultipleInheritance;
    /**
     * True if the code generator supports single class inheritance.
     * This is used to model the parent hierarchy based on the 'allOf' composed schemas.
     * Note: the single-class inheritance technique has inherent limitations because
     * a 'allOf' composed schema may have multiple $ref child schemas, each one
     * potentially representing a "parent" in the class inheritance hierarchy.
     * Some language generators also use class inheritance to implement the `additionalProperties`
     * keyword. For example, the Java code generator may generate 'extends HashMap'.
     */
    protected boolean supportsInheritance;
    /**
     * True if the language generator supports the 'additionalProperties' keyword
     * as sibling of a composed (allOf/anyOf/oneOf) schema.
     * Note: all language generators should support this to comply with the OAS specification.
     */
    protected boolean supportsAdditionalPropertiesWithComposedSchema;
    protected boolean supportsMixins;
    protected Map<String, String> supportedLibraries = new LinkedHashMap<>();
    protected String library;
    @Getter @Setter
    protected Boolean sortParamsByRequiredFlag = true;
    @Getter @Setter
    protected Boolean sortModelPropertiesByRequiredFlag = false;
    @Getter @Setter
    protected Boolean ensureUniqueParams = true;
    @Getter @Setter
    protected Boolean allowUnicodeIdentifiers = false;
    protected String gitHost, gitUserId, gitRepoId, releaseNote;
    protected String httpUserAgent;
    protected Boolean hideGenerationTimestamp = true;
    // How to encode special characters like $
    // They are translated to words like "Dollar"
    // Then translated back during JSON encoding and decoding
    protected Map<String, String> specialCharReplacements = new LinkedHashMap<>();
    // When a model is an alias for a simple type
    protected Map<String, String> typeAliases = Collections.emptyMap();
    @Getter @Setter
    protected Boolean prependFormOrBodyParameters = false;
    // The extension of the generated documentation files (defaults to markdown .md)
    protected String docExtension;
    protected String ignoreFilePathOverride;
    // flag to indicate whether to use environment variable to post process file
    protected boolean enablePostProcessFile = false;
    private TemplatingEngineAdapter templatingEngine = new MustacheEngineAdapter();
    // flag to indicate whether to use the utils.OneOfImplementorAdditionalData related logic
    protected boolean useOneOfInterfaces = false;
    // whether or not the oneOf imports machinery should add oneOf interfaces as imports in implementing classes
    protected boolean addOneOfInterfaceImports = false;

    // flag to indicate whether to only update files whose contents have changed
    protected boolean enableMinimalUpdate = false;

    // acts strictly upon a spec, potentially modifying it to have consistent behavior across generators.
    protected boolean strictSpecBehavior = true;
    // flag to indicate whether enum value prefixes are removed
    protected boolean removeEnumValuePrefix = true;

    // Support legacy logic for evaluating discriminators
    @Setter protected boolean legacyDiscriminatorBehavior = true;

    // Specify what to do if the 'additionalProperties' keyword is not present in a schema.
    // See CodegenConstants.java for more details.
    @Setter protected boolean disallowAdditionalPropertiesIfNotPresent = true;

    // If the server adds new enum cases, that are unknown by an old spec/client, the client will fail to parse the network response.
    // With this option enabled, each enum will have a new case, 'unknown_default_open_api', so that when the server sends an enum case that is not known by the client/spec, they can safely fallback to this case.
    @Setter protected boolean enumUnknownDefaultCase = false;
    protected String enumUnknownDefaultCaseName = "unknown_default_open_api";

    // A cache to efficiently lookup schema `toModelName()` based on the schema Key
    private final Map<String, String> schemaKeyToModelNameCache = new HashMap<>();

    protected boolean loadDeepObjectIntoItems = true;

    // if true then baseTypes will be imported
    protected boolean importBaseType = true;

    // if true then container types will be imported
    protected boolean importContainerType = true;

    protected boolean addSuffixToDuplicateOperationNicknames = true;

    // Whether to automatically hardcode params that are considered Constants by OpenAPI Spec
    @Setter protected boolean autosetConstants = false;

    @Override
    public boolean getAddSuffixToDuplicateOperationNicknames() {
        return addSuffixToDuplicateOperationNicknames;
    }

    @Override
    public List<CliOption> cliOptions() {
        return cliOptions;
    }

    /**
     * add this instance to additionalProperties.
     * This instance is used as parent context in Mustache.
     * It means that Mustache uses the values found in this order:
     * first from additionalProperties
     * then from the getter in this instance
     * then from the fields in this instance
     *
     */
    protected void useCodegenAsMustacheParentContext() {
        additionalProperties.put(CodegenConstants.MUSTACHE_PARENT_CONTEXT, this);
    }

    @Override
    public void processOpts() {

        if (!additionalProperties.containsKey(CodegenConstants.MUSTACHE_PARENT_CONTEXT)) {
            // by default empty parent context
            additionalProperties.put(CodegenConstants.MUSTACHE_PARENT_CONTEXT, new Object());
        }
        convertPropertyToStringAndWriteBack(CodegenConstants.TEMPLATE_DIR, this::setTemplateDir);
        convertPropertyToStringAndWriteBack(CodegenConstants.MODEL_PACKAGE, this::setModelPackage);
        convertPropertyToStringAndWriteBack(CodegenConstants.API_PACKAGE, this::setApiPackage);

        convertPropertyToBooleanAndWriteBack(CodegenConstants.HIDE_GENERATION_TIMESTAMP, this::setHideGenerationTimestamp);
        // put the value back in additionalProperties for backward compatibility with generators not using yet convertPropertyToBooleanAndWriteBack
        writePropertyBack(CodegenConstants.HIDE_GENERATION_TIMESTAMP, isHideGenerationTimestamp());

        convertPropertyToBooleanAndWriteBack(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG, this::setSortParamsByRequiredFlag);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG, this::setSortModelPropertiesByRequiredFlag);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.PREPEND_FORM_OR_BODY_PARAMETERS, this::setPrependFormOrBodyParameters);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.ENSURE_UNIQUE_PARAMS, this::setEnsureUniqueParams);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS, this::setAllowUnicodeIdentifiers);
        convertPropertyToStringAndWriteBack(CodegenConstants.API_NAME_PREFIX, this::setApiNamePrefix);
        convertPropertyToStringAndWriteBack(CodegenConstants.API_NAME_SUFFIX, this::setApiNameSuffix);
        convertPropertyToStringAndWriteBack(CodegenConstants.MODEL_NAME_PREFIX, this::setModelNamePrefix);
        convertPropertyToStringAndWriteBack(CodegenConstants.MODEL_NAME_SUFFIX, this::setModelNameSuffix);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.REMOVE_OPERATION_ID_PREFIX, this::setRemoveOperationIdPrefix);
        convertPropertyToStringAndWriteBack(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_DELIMITER, this::setRemoveOperationIdPrefixDelimiter);
        convertPropertyToTypeAndWriteBack(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_COUNT, Integer::parseInt, this::setRemoveOperationIdPrefixCount);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SKIP_OPERATION_EXAMPLE, this::setSkipOperationExample);
        convertPropertyToStringAndWriteBack(CodegenConstants.DOCEXTENSION, this::setDocExtension);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.ENABLE_POST_PROCESS_FILE, this::setEnablePostProcessFile);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.REMOVE_ENUM_VALUE_PREFIX, this::setRemoveEnumValuePrefix);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.LEGACY_DISCRIMINATOR_BEHAVIOR, this::setLegacyDiscriminatorBehavior);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT, this::setDisallowAdditionalPropertiesIfNotPresent);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.ENUM_UNKNOWN_DEFAULT_CASE, this::setEnumUnknownDefaultCase);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.AUTOSET_CONSTANTS, this::setAutosetConstants);
        }


    /***
     * Preset map builder with commonly used Mustache lambdas.
     *
     * To extend the map, override addMustacheLambdas(), call parent method
     * first and then add additional lambdas to the returned builder.
     *
     * If common lambdas are not desired, override addMustacheLambdas() method
     * and return empty builder.
     *
     * @return preinitialized map with common lambdas
     */
    protected ImmutableMap.Builder<String, Lambda> addMustacheLambdas() {

        return new ImmutableMap.Builder<String, Lambda>()
                .put("lowercase", new LowercaseLambda().generator(this))
                .put("uppercase", new UppercaseLambda())
                .put("snakecase", new SnakecaseLambda())
                .put("titlecase", new TitlecaseLambda())
                .put("kebabcase", new KebabCaseLambda())
                .put("camelcase", new CamelCaseAndSanitizeLambda(true).generator(this))
                .put("pascalcase", new CamelCaseAndSanitizeLambda(false).generator(this))
                .put("uncamelize", new UncamelizeLambda())
                .put("forwardslash", new ForwardSlashLambda())
                .put("backslash", new BackSlashLambda())
                .put("doublequote", new DoubleQuoteLambda())
                .put("indented", new IndentedLambda())
                .put("indented_8", new IndentedLambda(8, " ", false, false))
                .put("indented_12", new IndentedLambda(12, " ", false, false))
                .put("indented_16", new IndentedLambda(16, " ", false, false));

    }

    private void registerMustacheLambdas() {
        ImmutableMap<String, Lambda> lambdas = addMustacheLambdas().build();

        if (lambdas.size() == 0) {
            return;
        }

        if (additionalProperties.containsKey("lambda")) {
            LOGGER.error("A property called 'lambda' already exists in additionalProperties");
            throw new RuntimeException("A property called 'lambda' already exists in additionalProperties");
        }
        additionalProperties.put("lambda", lambdas);
    }

    /**
     * Returns the common prefix of variables for enum naming if
     * two or more variables are present
     *
     * @param vars List of variable names
     * @return the common prefix for naming
     */
    public String findCommonPrefixOfVars(List<Object> vars) {
        if (vars.size() > 1) {
            try {
                String[] listStr = vars.toArray(new String[vars.size()]);
                String prefix = StringUtils.getCommonPrefix(listStr);
                // exclude trailing characters that should be part of a valid variable
                // e.g. ["status-on", "status-off"] => "status-" (not "status-o")
                final Matcher matcher = COMMON_PREFIX_ENUM_NAME.matcher(prefix);
                return matcher.replaceAll("");
            } catch (ArrayStoreException e) {
                // do nothing, just return default value
            }
        }
        return "";
    }

    /**
     * Return the enum default value in the language specified format
     *
     * @param value    enum variable name
     * @param datatype data type
     * @return the default value for the enum
     */
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "." + value;
    }

    /**
     * Return the enum value in the language specified format
     * e.g. status becomes "status"
     *
     * @param value    enum variable name
     * @param datatype data type
     * @return the sanitized value for enum
     */
    public String toEnumValue(String value, String datatype) {
        if ("number".equalsIgnoreCase(datatype) || "boolean".equalsIgnoreCase(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    /**
     * Return the sanitized variable name for enum
     *
     * @param value    enum variable name
     * @param datatype data type
     * @return the sanitized variable name for enum
     */
    public String toEnumVarName(String value, String datatype) {
        if (value.length() == 0) {
            return "EMPTY";
        }

        String var = value.replaceAll("\\W+", "_").toUpperCase(Locale.ROOT);
        if (var.matches("\\d.*")) {
            var = "_" + var;
        }

        if (reservedWords.contains(var)) {
            return escapeReservedWord(var);
        }

        return var;
    }

    // override with any message to be shown right before the process finishes
    @Override
    @SuppressWarnings("static-method")
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("# Thanks for using PageObject Generator.                                       #");
        System.out.println("################################################################################");
    }

    // override with any special handling of the JMustache compiler
    @Override
    @SuppressWarnings("unused")
    public Compiler processCompiler(Compiler compiler) {
        return compiler;
    }

    // override with any special handling for the templating engine
    @Override
    @SuppressWarnings("unused")
    public TemplatingEngineAdapter processTemplatingEngine(TemplatingEngineAdapter templatingEngine) {
        return templatingEngine;
    }

    // override with any special text escaping logic
    @Override
    @SuppressWarnings("static-method")
    public String escapeText(String input) {
        if (input == null) {
            return input;
        }

        // remove \t, \n, \r
        // replace \ with \\
        // replace " with \"
        // outer unescape to retain the original multi-byte characters
        // finally escalate characters avoiding code injection
        return escapeUnsafeCharacters(
                StringEscapeUtils.unescapeJava(
                                StringEscapeUtils.escapeJava(input)
                                        .replace("\\/", "/"))
                        .replaceAll("[\\t\\n\\r]", " ")
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\""));
    }

    /**
     * Escape characters while allowing new lines
     *
     * @param input String to be escaped
     * @return escaped string
     */
    @Override
    public String escapeTextWhileAllowingNewLines(String input) {
        if (input == null) {
            return input;
        }

        // remove \t
        // replace \ with \\
        // replace " with \"
        // outer unescape to retain the original multi-byte characters
        // finally escalate characters avoiding code injection
        return escapeUnsafeCharacters(
                StringEscapeUtils.unescapeJava(
                                StringEscapeUtils.escapeJava(input)
                                        .replace("\\/", "/"))
                        .replaceAll("[\\t]", " ")
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\""));
    }

    // override with any special encoding and escaping logic
    @Override
    @SuppressWarnings("static-method")
    public String encodePath(String input) {
        return escapeText(input);
    }

    /**
     * override with any special text escaping logic to handle unsafe
     * characters so as to avoid code injection
     *
     * @param input String to be cleaned up
     * @return string with unsafe characters removed or escaped
     */
    @Override
    public String escapeUnsafeCharacters(String input) {
        LOGGER.warn("escapeUnsafeCharacters should be overridden in the code generator with proper logic to escape " +
                "unsafe characters");
        // doing nothing by default and code generator should implement
        // the logic to prevent code injection
        // later we'll make this method abstract to make sure
        // code generator implements this method
        return input;
    }

    /**
     * Escape single and/or double quote to avoid code injection
     *
     * @param input String to be cleaned up
     * @return string with quotation mark removed or escaped
     */
    @Override
    public String escapeQuotationMark(String input) {
        LOGGER.warn("escapeQuotationMark should be overridden in the code generator with proper logic to escape " +
                "single/double quote");
        return input.replace("\"", "\\\"");
    }

    @Override
    public Set<String> defaultIncludes() {
        return defaultIncludes;
    }

    @Override
    public Map<String, String> typeMapping() {
        return typeMapping;
    }

    @Override
    public Map<String, String> instantiationTypes() {
        return instantiationTypes;
    }

    @Override
    public Set<String> reservedWords() {
        return reservedWords;
    }

    @Override
    public Set<String> languageSpecificPrimitives() {
        return languageSpecificPrimitives;
    }

    @Override
    public Set<String> openapiGeneratorIgnoreList() {
        return openapiGeneratorIgnoreList;
    }

    @Override
    public Map<String, String> importMapping() {
        return importMapping;
    }

    @Override
    public Map<String, String> schemaMapping() {
        return schemaMapping;
    }

    @Override
    public Map<String, String> inlineSchemaNameMapping() {
        return inlineSchemaNameMapping;
    }

    @Override
    public Map<String, String> inlineSchemaOption() {
        return inlineSchemaOption;
    }

    @Override
    public Map<String, String> nameMapping() {
        return nameMapping;
    }

    @Override
    public Map<String, String> parameterNameMapping() {
        return parameterNameMapping;
    }

    @Override
    public Map<String, String> modelNameMapping() {
        return modelNameMapping;
    }

    @Override
    public Map<String, String> enumNameMapping() {
        return enumNameMapping;
    }

    @Override
    public Map<String, String> operationIdNameMapping() {
        return operationIdNameMapping;
    }

    @Override
    public Map<String, String> openapiNormalizer() {
        return openapiNormalizer;
    }

    @Override
    public String testPackage() {
        return testPackage;
    }

    @Override
    public String modelPackage() {
        return modelPackage;
    }

    @Override
    public String apiPackage() {
        return apiPackage;
    }

    @Override
    public String fileSuffix() {
        return fileSuffix;
    }

    @Override
    public String templateDir() {
        return templateDir;
    }

    @Override
    public String embeddedTemplateDir() {
        if (embeddedTemplateDir != null) {
            return embeddedTemplateDir;
        } else {
            return templateDir;
        }
    }

    @Override
    public Map<String, String> apiDocTemplateFiles() {
        return apiDocTemplateFiles;
    }

    @Override
    public Map<String, String> modelDocTemplateFiles() {
        return modelDocTemplateFiles;
    }

    @Override
    public Map<String, String> reservedWordsMappings() {
        return reservedWordsMappings;
    }

    @Override
    public Map<String, String> apiTestTemplateFiles() {
        return apiTestTemplateFiles;
    }

    @Override
    public Map<String, String> modelTestTemplateFiles() {
        return modelTestTemplateFiles;
    }

    @Override
    public Map<String, String> apiTemplateFiles() {
        return apiTemplateFiles;
    }

    @Override
    public Map<String, String> modelTemplateFiles() {
        return modelTemplateFiles;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return outputFolder + File.separator + testPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return outputFolder + File.separator + testPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return outputFolder;
    }

    @Override
    public String modelDocFileFolder() {
        return outputFolder;
    }

    @Override
    public Map<String, Object> additionalProperties() {
        return additionalProperties;
    }

    @Override
    public Map<String, String> serverVariableOverrides() {
        return serverVariables;
    }

    @Override
    public Map<String, Object> vendorExtensions() {
        return vendorExtensions;
    }

    @Override
    public Map<String, String> templateOutputDirs() {
        return templateOutputDirs;
    }

    @Override
    public String outputFolder() {
        return outputFolder;
    }

    @Override
    public void setOutputDir(String dir) {
        this.outputFolder = dir;
    }

    @Override
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    @Override
    public String getOutputDir() {
        return outputFolder();
    }

    @Override
    public String getInputSpec() {
        return inputSpec;
    }

    @Override
    public String getConfigFile() {
        return configFile;
    }

    @Override
    public void setInputSpec(String inputSpec) {
        this.inputSpec = inputSpec;
    }

    @Override
    public String getFilesMetadataFilename() {
        return filesMetadataFilename;
    }

    @Override
    public String getVersionMetadataFilename() {
        return versionMetadataFilename;
    }

    public Boolean getLegacyDiscriminatorBehavior() {
        return legacyDiscriminatorBehavior;
    }

    public Boolean getDisallowAdditionalPropertiesIfNotPresent() {
        return disallowAdditionalPropertiesIfNotPresent;
    }

    public Boolean getEnumUnknownDefaultCase() {
        return enumUnknownDefaultCase;
    }

    public Boolean getUseOneOfInterfaces() {
        return useOneOfInterfaces;
    }

    public void setUseOneOfInterfaces(Boolean useOneOfInterfaces) {
        this.useOneOfInterfaces = useOneOfInterfaces;
    }

    /**
     * Return the regular expression/JSON schema pattern (http://json-schema.org/latest/json-schema-validation.html#anchor33)
     *
     * @param pattern the pattern (regular expression)
     * @return properly-escaped pattern
     */
    public String toRegularExpression(String pattern) {
        return addRegularExpressionDelimiter(escapeText(pattern));
    }

    /**
     * Return the file name of the Api
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    /**
     * Return the file name of the Api Documentation
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    /**
     * Return the file name of the Api Test
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    /**
     * Return the variable name in the Api
     *
     * @param name the variable name of the Api
     * @return the snake-cased variable name
     */
    @Override
    public String toApiVarName(String name) {
        return lowerCamelCase(name);
    }

    /**
     * Return the capitalized file name of the model
     *
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelFilename(String name) {
        return camelize(name);
    }

    /**
     * Return the capitalized file name of the model test
     *
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelTestFilename(String name) {
        return camelize(name) + "Test";
    }

    /**
     * Return the capitalized file name of the model documentation
     *
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelDocFilename(String name) {
        return camelize(name);
    }

    /**
     * Returns metadata about the generator.
     *
     * @return A provided {@link GeneratorMetadata} instance
     */
    @Override
    public GeneratorMetadata getGeneratorMetadata() {
        return generatorMetadata;
    }

    @Override
    public String sanitizeName(String text) {
        return text;
    }

    /**
     * Return the operation ID (method name)
     *
     * @param operationId operation ID
     * @return the sanitized method name
     */
    @SuppressWarnings("static-method")
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        return operationId;
    }

    /**
     * Return the variable name by removing invalid characters and proper escaping if
     * it's a reserved word.
     *
     * @param name the variable name
     * @return the sanitized variable name
     */
    public String toVarName(final String name) {
        // obtain the name from nameMapping directly if provided
        if (nameMapping.containsKey(name)) {
            return nameMapping.get(name);
        }

        if (reservedWords.contains(name)) {
            return escapeReservedWord(name);
        } else if (name.chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character)))) {
            return escape(name, specialCharReplacements, null, null);
        }
        return name;
    }

    /**
     * Return the parameter name by removing invalid characters and proper escaping if
     * it's a reserved word.
     *
     * @param name Codegen property object
     * @return the sanitized parameter name
     */
    @Override
    public String toParamName(String name) {
        // obtain the name from parameterNameMapping directly if provided
        if (parameterNameMapping.containsKey(name)) {
            return parameterNameMapping.get(name);
        }

        name = removeNonNameElementToCamelCase(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        if (reservedWords.contains(name)) {
            return escapeReservedWord(name);
        } else if (name.chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character)))) {
            return escape(name, specialCharReplacements, null, null);
        }
        return name;

    }

    /**
     * Return the parameter name of array of model
     *
     * @param name name of the array model
     * @return the sanitized parameter name
     */
    public String toArrayModelParamName(String name) {
        return toParamName(name);
    }

    /**
     * Return the escaped name of the reserved word
     *
     * @param name the name to be escaped
     * @return the escaped reserved word
     * <p>
     * throws Runtime exception as reserved word is not allowed (default behavior)
     */
    @Override
    @SuppressWarnings("static-method")
    public String escapeReservedWord(String name) {
        throw new RuntimeException("reserved word " + name + " not allowed");
    }

    /**
     * Return the fully-qualified "Model" name for import
     *
     * @param name the name of the "Model"
     * @return the fully-qualified "Model" name for import
     */
    @Override
    public String toModelImport(String name) {
        if ("".equals(modelPackage())) {
            return name;
        } else {
            return modelPackage() + "." + name;
        }
    }

    /**
     * Returns the same content as [[toModelImport]] with key the fully-qualified Model name and value the initial input.
     * In case of union types this method has a key for each separate model and import.
     *
     * @param name the name of the "Model"
     * @return Map of fully-qualified models.
     */
    @Override
    public Map<String, String> toModelImportMap(String name) {
        return Collections.singletonMap(this.toModelImport(name), name);
    }

    /**
     * Return the fully-qualified "Api" name for import
     *
     * @param name the name of the "Api"
     * @return the fully-qualified "Api" name for import
     */
    @Override
    public String toApiImport(String name) {
        return apiPackage() + "." + name;
    }

    /**
     * Default constructor.
     * This method will map between OAS type and language-specified type, as well as mapping
     * between OAS type and the corresponding import statement for the language. This will
     * also add some language specified CLI options, if any.
     * returns string presentation of the example path (it's a constructor)
     */
    public DefaultCodegen() {

        generatorMetadata = GeneratorMetadata.newBuilder()
                .stability(Stability.STABLE)
                .generationMessage(String.format(Locale.ROOT, "OpenAPI Generator: %s", getName()))
                .build();

        defaultIncludes = new HashSet<>(
                Arrays.asList("double",
                        "int",
                        "long",
                        "short",
                        "char",
                        "float",
                        "String",
                        "boolean",
                        "Boolean",
                        "Double",
                        "Void",
                        "Integer",
                        "Long",
                        "Float")
        );

        typeMapping = new HashMap<>();
        typeMapping.put("array", "List");
        typeMapping.put("set", "Set");
        typeMapping.put("map", "Map");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("string", "String");
        typeMapping.put("int", "Integer");
        typeMapping.put("float", "Float");
        typeMapping.put("double", "Double");
        typeMapping.put("number", "BigDecimal");
        typeMapping.put("decimal", "BigDecimal");
        typeMapping.put("DateTime", "Date");
        typeMapping.put("long", "Long");
        typeMapping.put("short", "Short");
        typeMapping.put("integer", "Integer");
        typeMapping.put("UnsignedInteger", "Integer");
        typeMapping.put("UnsignedLong", "Long");
        typeMapping.put("char", "String");
        typeMapping.put("object", "Object");
        // FIXME: java specific type should be in Java Based Abstract Impl's
        typeMapping.put("ByteArray", "byte[]");
        typeMapping.put("binary", "File");
        typeMapping.put("file", "File");
        typeMapping.put("UUID", "UUID");
        typeMapping.put("URI", "URI");
        typeMapping.put("AnyType", "oas_any_type_not_mapped");

        instantiationTypes = new HashMap<>();

        reservedWords = new HashSet<>();
        // option to change how we process + set the data in the discriminator mapping
        Map<String, String> legacyDiscriminatorBehaviorOpts = new HashMap<>();
        legacyDiscriminatorBehaviorOpts.put("true", "The mapping in the discriminator includes descendent schemas that allOf inherit from self and the discriminator mapping schemas in the OAS document.");
        legacyDiscriminatorBehaviorOpts.put("false", "The mapping in the discriminator includes any descendent schemas that allOf inherit from self, any oneOf schemas, any anyOf schemas, any x-discriminator-values, and the discriminator mapping schemas in the OAS document AND Codegen validates that oneOf and anyOf schemas contain the required discriminator and throws an error if the discriminator is missing.");

        // option to change how we process + set the data in the 'additionalProperties' keyword.
        Map<String, String> disallowAdditionalPropertiesIfNotPresentOpts = new HashMap<>();
        disallowAdditionalPropertiesIfNotPresentOpts.put("false",
                "The 'additionalProperties' implementation is compliant with the OAS and JSON schema specifications.");
        disallowAdditionalPropertiesIfNotPresentOpts.put("true",
                "Keep the old (incorrect) behaviour that 'additionalProperties' is set to false by default.");
        this.setDisallowAdditionalPropertiesIfNotPresent(true);

        Map<String, String> enumUnknownDefaultCaseOpts = new HashMap<>();
        enumUnknownDefaultCaseOpts.put("false",
                "No changes to the enum's are made, this is the default option.");
        enumUnknownDefaultCaseOpts.put("true",
                "With this option enabled, each enum will have a new case, 'unknown_default_open_api', so that when the enum case sent by the server is not known by the client/spec, can safely be decoded to this case.");
        this.setEnumUnknownDefaultCase(false);

        // initialize special character mapping
        initializeSpecialCharacterMapping();

        // Register common Mustache lambdas.
        registerMustacheLambdas();
    }

    /**
     * Initialize special character mapping
     */
    protected void initializeSpecialCharacterMapping() {
        // Initialize special characters
        specialCharReplacements.put("$", "Dollar");
        specialCharReplacements.put("^", "Caret");
        specialCharReplacements.put("|", "Pipe");
        specialCharReplacements.put("=", "Equal");
        specialCharReplacements.put("*", "Star");
        specialCharReplacements.put("-", "Minus");
        specialCharReplacements.put("&", "Ampersand");
        specialCharReplacements.put("%", "Percent");
        specialCharReplacements.put("#", "Hash");
        specialCharReplacements.put("@", "At");
        specialCharReplacements.put("!", "Exclamation");
        specialCharReplacements.put("+", "Plus");
        specialCharReplacements.put(":", "Colon");
        specialCharReplacements.put(";", "Semicolon");
        specialCharReplacements.put(">", "Greater_Than");
        specialCharReplacements.put("<", "Less_Than");
        specialCharReplacements.put(".", "Period");
        specialCharReplacements.put("_", "Underscore");
        specialCharReplacements.put("?", "Question_Mark");
        specialCharReplacements.put(",", "Comma");
        specialCharReplacements.put("'", "Quote");
        specialCharReplacements.put("\"", "Double_Quote");
        specialCharReplacements.put("/", "Slash");
        specialCharReplacements.put("\\", "Back_Slash");
        specialCharReplacements.put("(", "Left_Parenthesis");
        specialCharReplacements.put(")", "Right_Parenthesis");
        specialCharReplacements.put("{", "Left_Curly_Bracket");
        specialCharReplacements.put("}", "Right_Curly_Bracket");
        specialCharReplacements.put("[", "Left_Square_Bracket");
        specialCharReplacements.put("]", "Right_Square_Bracket");
        specialCharReplacements.put("~", "Tilde");
        specialCharReplacements.put("`", "Backtick");

        specialCharReplacements.put("<=", "Less_Than_Or_Equal_To");
        specialCharReplacements.put(">=", "Greater_Than_Or_Equal_To");
        specialCharReplacements.put("!=", "Not_Equal");
        specialCharReplacements.put("<>", "Not_Equal");
        specialCharReplacements.put("~=", "Tilde_Equal");
        specialCharReplacements.put("==", "Double_Equal");
    }

    /**
     * Return the symbol name of a symbol
     *
     * @param input Symbol (e.g. $)
     * @return Symbol name (e.g. Dollar)
     */
    protected String getSymbolName(String input) {
        return specialCharReplacements.get(input);
    }

    /**
     * Return the lowerCamelCase of the string
     *
     * @param name string to be lowerCamelCased
     * @return lowerCamelCase string
     */
    @SuppressWarnings("static-method")
    public String lowerCamelCase(String name) {
        return (name.length() > 0) ? (Character.toLowerCase(name.charAt(0)) + name.substring(1)) : "";
    }

    /**
     * Output the language-specific type declaration of a given OAS name.
     *
     * @param name name
     * @return a string presentation of the type
     */
    @Override
    @SuppressWarnings("static-method")
    public String getTypeDeclaration(String name) {
        return name;
    }

    /**
     * Determine the type alias for the given type if it exists. This feature
     * was originally developed for Java because the language does not have an aliasing
     * mechanism of its own but later extends to handle other languages
     *
     * @param name The type name.
     * @return The alias of the given type, if it exists. If there is no alias
     * for this type, then returns the input type name.
     */
    public String getAlias(String name) {
        if (typeAliases != null && typeAliases.containsKey(name)) {
            return typeAliases.get(name);
        }
        return name;
    }
    /**
     * Output the API (class) name (capitalized) ending with the specified or default suffix
     * Return DefaultApi if name is empty
     *
     * @param name the name of the Api
     * @return capitalized Api name
     */
    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return camelize(apiNamePrefix + "_" + name + "_" + apiNameSuffix);
    }

    /**
     * Converts the OpenAPI schema name to a model name suitable for the current code generator.
     * May be overridden for each programming language.
     * In case the name belongs to the TypeSystem it won't be renamed.
     *
     * @param name the name of the model
     * @return capitalized model name
     */
    @Override
    public String toModelName(final String name) {
        // obtain the name from modelNameMapping directly if provided
        if (modelNameMapping.containsKey(name)) {
            return modelNameMapping.get(name);
        }

        if (schemaKeyToModelNameCache.containsKey(name)) {
            return schemaKeyToModelNameCache.get(name);
        }

        String camelizedName = camelize(modelNamePrefix + "_" + name + "_" + modelNameSuffix);
        schemaKeyToModelNameCache.put(name, camelizedName);
        return camelizedName;
    }

    protected String toTestCaseName(String specTestCaseName) {
        return specTestCaseName;
    }

    /**
     * A method that allows generators to pre-process test example payloads
     * This can be useful if one needs to change how values like null in string are represented
     *
     * @param data the test data payload
     * @return the updated test data payload
     */
    protected Object processTestExampleData(Object data) {
        return data;
    }

    protected void setReservedWordsLowerCase(List<String> words) {
        reservedWords = new HashSet<>();
        for (String word : words) {
            reservedWords.add(word.toLowerCase(Locale.ROOT));
        }
    }

    protected boolean isReservedWord(String word) {
        return word != null && reservedWords.contains(word.toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("static-method")
    protected List<Map<String, Object>> toExamples(Map<String, Object> examples) {
        if (examples == null) {
            return null;
        }

        final List<Map<String, Object>> output = new ArrayList<>(examples.size());
        for (Entry<String, Object> entry : examples.entrySet()) {
            final Map<String, Object> kv = new HashMap<>();
            kv.put("contentType", entry.getKey());
            kv.put("example", entry.getValue());
            output.add(kv);
        }
        return output;
    }

    private final Map<String, Integer> seenOperationIds = new HashMap<String, Integer>();

    /**
     * Generate the next name for the given name, i.e. append "2" to the base name if not ending with a number,
     * otherwise increase the number by 1. For example:
     * status    => status2
     * status2   => status3
     * myName100 => myName101
     *
     * @param name The base name
     * @return The next name for the base name
     */
    private static String generateNextName(String name) {
        Pattern pattern = Pattern.compile("\\d+\\z");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String numStr = matcher.group();
            int num = Integer.parseInt(numStr) + 1;
            return name.substring(0, name.length() - numStr.length()) + num;
        } else {
            return name + "2";
        }
    }

    /**
     * Remove characters not suitable for variable or method name from the input and camelize it
     *
     * @param name string to be camelize
     * @return camelized string
     */
    @SuppressWarnings("static-method")
    public String removeNonNameElementToCamelCase(String name) {
        return removeNonNameElementToCamelCase(name, "[-_:;#" + removeOperationIdPrefixDelimiter + "]");
    }

    /**
     * Remove characters that is not good to be included in method name from the input and camelize it
     *
     * @param name                  string to be camelize
     * @param nonNameElementPattern a regex pattern of the characters that is not good to be included in name
     * @return camelized string
     */
    protected String removeNonNameElementToCamelCase(final String name, final String nonNameElementPattern) {
        String result = Arrays.stream(name.split(nonNameElementPattern))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(""));
        if (result.length() > 0) {
            result = result.substring(0, 1).toLowerCase(Locale.ROOT) + result.substring(1);
        }
        return result;
    }

    /**
     * Return a value that is unique, suffixed with _index to make it unique
     * Ensures generated files are unique when compared case-insensitive
     * Not all operating systems support case-sensitive paths
     */
    private String uniqueCaseInsensitiveString(String value, Map<String, String> seenValues) {
        if (seenValues.keySet().contains(value)) {
            return seenValues.get(value);
        }

        Optional<Entry<String,String>> foundEntry = seenValues.entrySet().stream().filter(v -> v.getValue().toLowerCase(Locale.ROOT).equals(value.toLowerCase(Locale.ROOT))).findAny();
        if (foundEntry.isPresent()) {
            int counter = 0;
            String uniqueValue = value + "_" + counter;

            while (seenValues.values().stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toList()).contains(uniqueValue.toLowerCase(Locale.ROOT))) {
                counter++;
                uniqueValue = value + "_" + counter;
            }

            seenValues.put(value, uniqueValue);
            return uniqueValue;
        }

        seenValues.put(value, value);
        return value;
    }

    private final Map<String, String> seenApiFilenames = new HashMap<String, String>();

    @Override
    public String apiFilename(String templateName, String tag) {
        String uniqueTag = uniqueCaseInsensitiveString(tag, seenApiFilenames);
        String suffix = apiTemplateFiles().get(templateName);
        return apiFileFolder() + File.separator + toApiFilename(uniqueTag) + suffix;
    }

    @Override
    public String apiFilename(String templateName, String tag, String outputDir) {
        String uniqueTag = uniqueCaseInsensitiveString(tag, seenApiFilenames);
        String suffix = apiTemplateFiles().get(templateName);
        return outputDir + File.separator + toApiFilename(uniqueTag) + suffix;
    }

    private final Map<String, String> seenModelFilenames = new HashMap<String, String>();

    @Override
    public String modelFilename(String templateName, String modelName) {
        String uniqueModelName = uniqueCaseInsensitiveString(modelName, seenModelFilenames);
        String suffix = modelTemplateFiles().get(templateName);
        return modelFileFolder() + File.separator + toModelFilename(uniqueModelName) + suffix;
    }

    @Override
    public String modelFilename(String templateName, String modelName, String outputDir) {
        String uniqueModelName = uniqueCaseInsensitiveString(modelName, seenModelFilenames);
        String suffix = modelTemplateFiles().get(templateName);
        return outputDir + File.separator + toModelFilename(uniqueModelName) + suffix;
    }

    private final Map<String, String> seenApiDocFilenames = new HashMap<String, String>();

    /**
     * Return the full path and API documentation file
     *
     * @param templateName template name
     * @param tag          tag
     * @return the API documentation file name with full path
     */
    @Override
    public String apiDocFilename(String templateName, String tag) {
        String uniqueTag = uniqueCaseInsensitiveString(tag, seenApiDocFilenames);
        String docExtension = getDocExtension();
        String suffix = docExtension != null ? docExtension : apiDocTemplateFiles().get(templateName);
        return apiDocFileFolder() + File.separator + toApiDocFilename(uniqueTag) + suffix;
    }

    private final Map<String, String> seenApiTestFilenames = new HashMap<String, String>();

    /**
     * Return the full path and API test file
     *
     * @param templateName template name
     * @param tag          tag
     * @return the API test file name with full path
     */
    @Override
    public String apiTestFilename(String templateName, String tag) {
        String uniqueTag = uniqueCaseInsensitiveString(tag, seenApiTestFilenames);
        String suffix = apiTestTemplateFiles().get(templateName);
        return apiTestFileFolder() + File.separator + toApiTestFilename(uniqueTag) + suffix;
    }

    @Override
    public boolean shouldOverwrite(String filename) {
        return !(skipOverwrite && new File(filename).exists());
    }

    @Override
    public boolean isSkipOverwrite() {
        return skipOverwrite;
    }

    @Override
    public void setSkipOverwrite(boolean skipOverwrite) {
        this.skipOverwrite = skipOverwrite;
    }

    @Override
    public boolean isRemoveOperationIdPrefix() {
        return removeOperationIdPrefix;
    }

    @Override
    public boolean isSkipOperationExample() {
        return skipOperationExample;
    }

    @Override
    public void setRemoveOperationIdPrefix(boolean removeOperationIdPrefix) {
        this.removeOperationIdPrefix = removeOperationIdPrefix;
    }

    @Override
    public void setSkipOperationExample(boolean skipOperationExample) {
        this.skipOperationExample = skipOperationExample;
    }

    @Override
    public boolean isSkipSortingOperations() {
        return this.skipSortingOperations;
    }

    @Override
    public void setSkipSortingOperations(boolean skipSortingOperations) {
        this.skipSortingOperations = skipSortingOperations;
    }

    @Override
    public boolean isHideGenerationTimestamp() {
        return hideGenerationTimestamp;
    }

    @Override
    public void setHideGenerationTimestamp(boolean hideGenerationTimestamp) {
        this.hideGenerationTimestamp = hideGenerationTimestamp;
    }

    /**
     * All library templates supported.
     * (key: library name, value: library description)
     *
     * @return the supported libraries
     */
    @Override
    public Map<String, String> supportedLibraries() {
        return supportedLibraries;
    }

    /**
     * Set library template (sub-template).
     *
     * @param library Library template
     */
    @Override
    public void setLibrary(String library) {
        if (library != null && !supportedLibraries.containsKey(library)) {
            StringBuilder sb = new StringBuilder("Unknown library: " + library + "\nAvailable libraries:");
            if (supportedLibraries.size() == 0) {
                sb.append("\n  ").append("NONE");
            } else {
                for (String lib : supportedLibraries.keySet()) {
                    sb.append("\n  ").append(lib);
                }
            }
            throw new RuntimeException(sb.toString());
        }
        this.library = library;
    }

    /**
     * Library template (sub-template).
     *
     * @return Library template
     */
    @Override
    public String getLibrary() {
        return library;
    }

    /**
     * check if current active library equals to passed
     *
     * @param library - library to be compared with
     * @return {@code true} if passed library is active, {@code false} otherwise
     */
    public final boolean isLibrary(String library) {
        return library.equals(this.library);
    }

    /**
     * Set Git host.
     *
     * @param gitHost Git host
     */
    @Override
    public void setGitHost(String gitHost) {
        this.gitHost = gitHost;
    }

    /**
     * Git host.
     *
     * @return Git host
     */
    @Override
    public String getGitHost() {
        return gitHost;
    }

    /**
     * Set Git user ID.
     *
     * @param gitUserId Git user ID
     */
    @Override
    public void setGitUserId(String gitUserId) {
        this.gitUserId = gitUserId;
    }

    /**
     * Git user ID
     *
     * @return Git user ID
     */
    @Override
    public String getGitUserId() {
        return gitUserId;
    }

    /**
     * Set Git repo ID.
     *
     * @param gitRepoId Git repo ID
     */
    @Override
    public void setGitRepoId(String gitRepoId) {
        this.gitRepoId = gitRepoId;
    }

    /**
     * Git repo ID
     *
     * @return Git repo ID
     */
    @Override
    public String getGitRepoId() {
        return gitRepoId;
    }

    /**
     * Set release note.
     *
     * @param releaseNote Release note
     */
    @Override
    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    /**
     * Release note
     *
     * @return Release note
     */
    @Override
    public String getReleaseNote() {
        return releaseNote;
    }

    /**
     * Documentation files extension
     *
     * @return Documentation files extension
     */
    @Override
    public String getDocExtension() {
        return docExtension;
    }

    /**
     * Set Documentation files extension
     *
     * @param userDocExtension documentation files extension
     */
    @Override
    public void setDocExtension(String userDocExtension) {
        this.docExtension = userDocExtension;
    }

    /**
     * Set HTTP user agent.
     *
     * @param httpUserAgent HTTP user agent
     */
    @Override
    public void setHttpUserAgent(String httpUserAgent) {
        this.httpUserAgent = httpUserAgent;
    }

    /**
     * HTTP user agent
     *
     * @return HTTP user agent
     */
    @Override
    public String getHttpUserAgent() {
        return httpUserAgent;
    }

    @SuppressWarnings("static-method")
    protected CliOption buildLibraryCliOption(Map<String, String> supportedLibraries) {
        StringBuilder sb = new StringBuilder("library template (sub-template) to use:");
        for (String lib : supportedLibraries.keySet()) {
            sb.append("\n").append(lib).append(" - ").append(supportedLibraries.get(lib));
        }
        return new CliOption(CodegenConstants.LIBRARY, sb.toString());
    }

    @Override
    public void setTemplatingEngine(TemplatingEngineAdapter templatingEngine) {
        this.templatingEngine = templatingEngine;
    }

    @Override
    public TemplatingEngineAdapter getTemplatingEngine() {
        return this.templatingEngine;
    }

    private String sanitizeValue(String value, String replaceMatch, String replaceValue, List<String> exceptionList) {
        if (exceptionList.size() == 0 || !exceptionList.contains(replaceMatch)) {
            return value.replaceAll(replaceMatch, replaceValue);
        }
        return value;
    }

    protected String getEnumDefaultValue(String defaultValue, String dataType) {
        final String enumDefaultValue;
        if (isDataTypeString(dataType)) {
            enumDefaultValue = toEnumValue(defaultValue, dataType);
        } else {
            enumDefaultValue = defaultValue;
        }
        return enumDefaultValue;
    }

    protected List<Map<String, Object>> buildEnumVars(List<Object> values, String dataType) {
        List<Map<String, Object>> enumVars = new ArrayList<>();
        int truncateIdx = isRemoveEnumValuePrefix()
                ? findCommonPrefixOfVars(values).length()
                : 0;

        for (Object value : values) {
            if (value == null) {
                // raw null values in enums are unions for nullable
                // attributes, not actual enum values, so we remove them here
                continue;
            }
            Map<String, Object> enumVar = new HashMap<>();
            String enumName = truncateIdx == 0
                    ? String.valueOf(value)
                    : value.toString().substring(truncateIdx);

            if (enumName.isEmpty()) {
                enumName = value.toString();
            }

            final String finalEnumName = toEnumVarName(enumName, dataType);

            enumVar.put("name", finalEnumName);
            enumVar.put("value", toEnumValue(String.valueOf(value), dataType));
            enumVar.put("isString", isDataTypeString(dataType));
            // TODO: add isNumeric
            enumVars.add(enumVar);
        }

        if (enumUnknownDefaultCase) {
            // If the server adds new enum cases, that are unknown by an old spec/client, the client will fail to parse the network response.
            // With this option enabled, each enum will have a new case, 'unknown_default_open_api', so that when the server sends an enum case that is not known by the client/spec, they can safely fallback to this case.
            Map<String, Object> enumVar = new HashMap<>();
            String enumName = enumUnknownDefaultCaseName;

            String enumValue = isDataTypeString(dataType)
                    ? enumUnknownDefaultCaseName
                    : // This is a dummy value that attempts to avoid collisions with previously specified cases.
                    // Int.max / 192
                    // The number 192 that is used to calculate this random value, is the Swift Evolution proposal for frozen/non-frozen enums.
                    // [SE-0192](https://github.com/apple/swift-evolution/blob/master/proposals/0192-non-exhaustive-enums.md)
                    // Since this functionality was born in the Swift 5 generator and latter on broth to all generators
                    // https://github.com/OpenAPITools/openapi-generator/pull/11013
                    String.valueOf(11184809);

            enumVar.put("name", toEnumVarName(enumName, dataType));
            enumVar.put("value", toEnumValue(enumValue, dataType));
            enumVar.put("isString", isDataTypeString(dataType));
            // TODO: add isNumeric
            enumVars.add(enumVar);
        }

        return enumVars;
    }

    protected void postProcessEnumVars(List<Map<String, Object>> enumVars) {
        Collections.reverse(enumVars);
        enumVars.forEach(v -> {
            String name = (String) v.get("name");
            long count = enumVars.stream().filter(v1 -> v1.get("name").equals(name)).count();
            if (count > 1) {
                String uniqueEnumName = getUniqueEnumName(name, enumVars);
                LOGGER.debug("Changing duplicate enumeration name from " + v.get("name") + " to " + uniqueEnumName);
                v.put("name", uniqueEnumName);
            }
        });
        Collections.reverse(enumVars);
    }

    private String getUniqueEnumName(String name, List<Map<String, Object>> enumVars) {
        long count = enumVars.stream().filter(v -> v.get("name").equals(name)).count();
        return count > 1
                ? getUniqueEnumName(name + count, enumVars)
                : name;
    }

    protected void updateEnumVarsWithExtensions(List<Map<String, Object>> enumVars, Map<String, Object> vendorExtensions, String dataType) {
        if (vendorExtensions != null) {
            updateEnumVarsWithExtensions(enumVars, vendorExtensions, "x-enum-varnames", "name");
            updateEnumVarsWithExtensions(enumVars, vendorExtensions, "x-enum-descriptions", "enumDescription");
        }
    }

    private void updateEnumVarsWithExtensions(List<Map<String, Object>> enumVars, Map<String, Object> vendorExtensions, String extensionKey, String key) {
        if (vendorExtensions.containsKey(extensionKey)) {
            List<String> values = (List<String>) vendorExtensions.get(extensionKey);
            int size = Math.min(enumVars.size(), values.size());
            for (int i = 0; i < size; i++) {
                enumVars.get(i).put(key, values.get(i));
            }
        }
    }

    /**
     * If the pattern misses the delimiter, add "/" to the beginning and end
     * Otherwise, return the original pattern
     *
     * @param pattern the pattern (regular expression)
     * @return the pattern with delimiter
     */
    public String addRegularExpressionDelimiter(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            return pattern;
        }

        if (!pattern.matches("^/.*")) {
            return "/" + pattern.replaceAll("/", "\\\\/") + "/";
        }

        return pattern;
    }

    /**
     * reads propertyKey from additionalProperties, converts it to a boolean and
     * writes it back to additionalProperties to be usable as a boolean in
     * mustache files.
     *
     * @param propertyKey property key
     * @return property value as boolean
     */
    public boolean convertPropertyToBooleanAndWriteBack(String propertyKey) {
        boolean result = convertPropertyToBoolean(propertyKey);
        writePropertyBack(propertyKey, result);
        return result;
    }


    /**
     * reads propertyKey from additionalProperties, converts it to a boolean and
     * writes it back to additionalProperties to be usable as a boolean in
     * mustache files.
     *
     * @param propertyKey property key
     * @param booleanSetter the setter function reference
     * @return property value as boolean or false if it does not exist
     */
    public boolean convertPropertyToBooleanAndWriteBack(String propertyKey, Consumer<Boolean> booleanSetter) {
        if (additionalProperties.containsKey(propertyKey)) {
            boolean result = convertPropertyToBoolean(propertyKey);
            writePropertyBack(propertyKey, result);
            booleanSetter.accept(result);
            return result;
        }
        return false;
    }

    /**
     * reads propertyKey from additionalProperties, converts it to a string and
     * writes it back to additionalProperties to be usable as a string in
     * mustache files.
     *
     * @param propertyKey property key
     * @param stringSetter the setter function reference
     * @return property value as String or null if not found
     */
    public String convertPropertyToStringAndWriteBack(String propertyKey, Consumer<String> stringSetter) {
        return convertPropertyToTypeAndWriteBack(propertyKey, Function.identity(), stringSetter);
    }

    /**
     * reads propertyKey from additionalProperties, converts it to T and
     * writes it back to additionalProperties to be usable as T in
     * mustache files.
     *
     * @param propertyKey property key
     * @param genericTypeSetter the setter function reference
     * @return property value as instance of type T or null if not found
     */
    public <T> T convertPropertyToTypeAndWriteBack(String propertyKey, Function<String, T> converter, Consumer<T> genericTypeSetter) {
        if (additionalProperties.containsKey(propertyKey)) {
            String value = additionalProperties.get(propertyKey).toString();
            T result = converter.apply(value);
            writePropertyBack(propertyKey, result);
            genericTypeSetter.accept(result);
            return result;
        }
        return null;
    }

    /**
     * Provides an override location, if any is specified, for the .openapi-generator-ignore.
     * <p>
     * This is originally intended for the first generation only.
     *
     * @return a string of the full path to an override ignore file.
     */
    @Override
    public String getIgnoreFilePathOverride() {
        return ignoreFilePathOverride;
    }

    /**
     * Sets an override location for the '.openapi-generator-ignore' location for the first code generation.
     *
     * @param ignoreFileOverride The full path to an ignore file
     */
    @Override
    public void setIgnoreFilePathOverride(final String ignoreFileOverride) {
        this.ignoreFilePathOverride = ignoreFileOverride;
    }

    public boolean convertPropertyToBoolean(String propertyKey) {
        final Object booleanValue = additionalProperties.get(propertyKey);
        boolean result = Boolean.FALSE;
        if (booleanValue instanceof Boolean) {
            result = (Boolean) booleanValue;
        } else if (booleanValue instanceof String) {
            result = Boolean.parseBoolean((String) booleanValue);
        } else {
            LOGGER.warn("The value (generator's option) must be either boolean or string. Default to `false`.");
        }
        return result;
    }

    public void writePropertyBack(String propertyKey, Object value) {
        additionalProperties.put(propertyKey, value);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }

    protected void updateOption(String key, String defaultValue) {
        for (CliOption cliOption : cliOptions) {
            if (cliOption.getOpt().equals(key)) {
                cliOption.setDefault(defaultValue);
                break;
            }
        }
    }

    protected void removeOption(String key) {
        for (int i = 0; i < cliOptions.size(); i++) {
            if (key.equals(cliOptions.get(i).getOpt())) {
                cliOptions.remove(i);
                break;
            }
        }
    }

    /**
     * checks if the data should be classified as "string" in enum
     * e.g. double in C# needs to be double-quoted (e.g. "2.8") by treating it as a string
     * In the future, we may rename this function to "isEnumString"
     *
     * @param dataType data type
     * @return true if it's a enum string
     */
    public boolean isDataTypeString(String dataType) {
        return "String".equals(dataType);
    }

    /**
     * Post-process the auto-generated file, e.g. using go-fmt to format the Go code. The file type can be "model-test",
     * "model-doc", "model", "api", "api-test", "api-doc", "supporting-file",
     * "openapi-generator-ignore", "openapi-generator-version"
     * <p>
     * TODO: store these values in enum instead
     *
     * @param file     file to be processed
     * @param fileType file type
     */
    @Override
    public void postProcessFile(File file, String fileType) {
        LOGGER.debug("Post processing file {} ({})", file, fileType);
    }

    /**
     * Executes an external command for file post processing.
     *
     * @param commandArr an array of commands and arguments. They will be concatenated with space and tokenized again.
     * @return Whether the execution passed (true) or failed (false)
     */
    protected boolean executePostProcessor(String[] commandArr) {
        final String command = String.join(" ", commandArr);
        try {
            // we don't use the array variant here, because the command passed in by the user is often not only a single binary
            // but a combination of binary + parameters, e.g. `/etc/bin prettier -w`, which would then not be found, as the
            // first array item would be expected to be the binary only. The exec method is tokenizing the command for us.
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    LOGGER.error("Error running the command ({}). Exit value: {}, Error output: {}", command, exitValue, sb);
                }
            } else {
                LOGGER.info("Successfully executed: {}", command);
                return true;
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Error running the command ({}). Exception: {}", command, e.getMessage());
            // Restore interrupted state
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Boolean value indicating the state of the option for post-processing file using environment variables.
     *
     * @return true if the option is enabled
     */
    @Override
    public boolean isEnablePostProcessFile() {
        return enablePostProcessFile;
    }

    /**
     * Set the boolean value indicating the state of the option for post-processing file using environment variables.
     *
     * @param enablePostProcessFile true to enable post-processing file
     */
    @Override
    public void setEnablePostProcessFile(boolean enablePostProcessFile) {
        this.enablePostProcessFile = enablePostProcessFile;
    }

    /**
     * Get the boolean value indicating the state of the option for updating only changed files
     */
    @Override
    public boolean isEnableMinimalUpdate() {
        return enableMinimalUpdate;
    }

    /**
     * Set the boolean value indicating the state of the option for updating only changed files
     *
     * @param enableMinimalUpdate true to enable minimal update
     */
    @Override
    public void setEnableMinimalUpdate(boolean enableMinimalUpdate) {
        this.enableMinimalUpdate = enableMinimalUpdate;
    }

    /**
     * Indicates whether the codegen configuration should treat documents as strictly defined by the OpenAPI specification.
     *
     * @return true to act strictly upon spec documents, potentially modifying the spec to strictly fit the spec.
     */
    @Override
    public boolean isStrictSpecBehavior() {
        return this.strictSpecBehavior;
    }

    /**
     * Sets the boolean valid indicating whether generation will work strictly against the specification, potentially making
     * minor changes to the input document.
     *
     * @param strictSpecBehavior true if we will behave strictly, false to allow specification documents which pass validation to be loosely interpreted against the spec.
     */
    @Override
    public void setStrictSpecBehavior(final boolean strictSpecBehavior) {
        this.strictSpecBehavior = strictSpecBehavior;
    }

    /**
     * Get the boolean value indicating whether to remove enum value prefixes
     */
    @Override
    public boolean isRemoveEnumValuePrefix() {
        return this.removeEnumValuePrefix;
    }

    /**
     * Set the boolean value indicating whether to remove enum value prefixes
     *
     * @param removeEnumValuePrefix true to enable enum value prefix removal
     */
    @Override
    public void setRemoveEnumValuePrefix(final boolean removeEnumValuePrefix) {
        this.removeEnumValuePrefix = removeEnumValuePrefix;
    }

    //// Following methods are related to the "useOneOfInterfaces" feature

    /**
     * An map entry for cached sanitized names.
     */
    @Getter private static class SanitizeNameOptions {
        public SanitizeNameOptions(String name, String removeCharRegEx, List<String> exceptions) {
            this.name = name;
            this.removeCharRegEx = removeCharRegEx;
            if (exceptions != null) {
                this.exceptions = Collections.unmodifiableList(exceptions);
            } else {
                this.exceptions = Collections.emptyList();
            }
        }

        private final String name;
        private final String removeCharRegEx;
        private final List<String> exceptions;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SanitizeNameOptions that = (SanitizeNameOptions) o;
            return Objects.equals(getName(), that.getName()) &&
                    Objects.equals(getRemoveCharRegEx(), that.getRemoveCharRegEx()) &&
                    Objects.equals(getExceptions(), that.getExceptions());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getRemoveCharRegEx(), getExceptions());
        }
    }

    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     * application/json
     * application/json; charset=UTF8
     * APPLICATION/JSON
     *
     * @param mime MIME string
     * @return true if the input matches the JSON MIME
     */
    public static boolean isJsonMimeType(String mime) {
        return mime != null && (JSON_MIME_PATTERN.matcher(mime).matches());
    }

    public static boolean isXmlMimeType(String mime) {
        return mime != null && (XML_MIME_PATTERN.matcher(mime).matches());
    }

    /**
     * Check if the given MIME is a JSON Vendor MIME.
     * JSON MIME examples:
     * application/vnd.mycompany+json
     * application/vnd.mycompany.resourceA.version1+json
     *
     * @param mime MIME string
     * @return true if the input matches the JSON vendor MIME
     */
    protected static boolean isJsonVendorMimeType(String mime) {
        return mime != null && JSON_VENDOR_MIME_PATTERN.matcher(mime).matches();
    }

    @Override
    public String defaultTemplatingEngine() {
        return "mustache";
    }

    @Override
    public String generatorLanguageVersion() {
        return null;
    }

    @Override
    public boolean getUseInlineModelResolver() {
        return true;
    }

    @Override
    public boolean getUseOpenapiNormalizer() {
        return true;
    }

    @Override
    public Set<String> getOpenapiGeneratorIgnoreList() {
        return openapiGeneratorIgnoreList;
    }

    @Override
    public boolean isTypeErasedGenerics() {
        return false;
    }

    /*
        A function to convert yaml or json ingested strings like property names
        And convert special characters like newline, tab, carriage return
        Into strings that can be rendered in the language that the generator will output to
        */
    protected String handleSpecialCharacters(String name) {
        return name;
    }
}
