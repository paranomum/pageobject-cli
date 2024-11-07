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

package ru.paranomum.page_object.languages;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.samskivert.mustache.Mustache;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.CliOption;
import ru.paranomum.page_object.CodegenConfig;
import ru.paranomum.page_object.CodegenConstants;
import ru.paranomum.page_object.DefaultCodegen;
import ru.paranomum.page_object.languages.features.BeanValidationFeatures;
import ru.paranomum.page_object.languages.features.DocumentationProviderFeatures;
import ru.paranomum.page_object.utils.CamelizeOption;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static ru.paranomum.page_object.utils.CamelizeOption.*;
import static ru.paranomum.page_object.utils.StringUtils.camelize;
import static ru.paranomum.page_object.utils.StringUtils.escape;

public abstract class AbstractJavaCodegen extends DefaultCodegen implements CodegenConfig,
        DocumentationProviderFeatures {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractJavaCodegen.class);
    private static final String ARTIFACT_VERSION_DEFAULT_VALUE = "1.0.0";

    public static final String DEFAULT_LIBRARY = "<default>";
    public static final String DATE_LIBRARY = "dateLibrary";
    public static final String SUPPORT_ASYNC = "supportAsync";
    public static final String WITH_XML = "withXml";
    public static final String DISABLE_HTML_ESCAPING = "disableHtmlEscaping";
    public static final String BOOLEAN_GETTER_PREFIX = "booleanGetterPrefix";
    public static final String IGNORE_ANYOF_IN_ENUM = "ignoreAnyOfInEnum";
    public static final String ADDITIONAL_MODEL_TYPE_ANNOTATIONS = "additionalModelTypeAnnotations";
    public static final String ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS = "additionalOneOfTypeAnnotations";
    public static final String ADDITIONAL_ENUM_TYPE_ANNOTATIONS = "additionalEnumTypeAnnotations";
    public static final String DISCRIMINATOR_CASE_SENSITIVE = "discriminatorCaseSensitive";
    public static final String OPENAPI_NULLABLE = "openApiNullable";
    public static final String JACKSON = "jackson";
    public static final String TEST_OUTPUT = "testOutput";
    public static final String IMPLICIT_HEADERS = "implicitHeaders";
    public static final String IMPLICIT_HEADERS_REGEX = "implicitHeadersRegex";
    public static final String JAVAX_PACKAGE = "javaxPackage";
    public static final String USE_JAKARTA_EE = "useJakartaEe";
    public static final String CONTAINER_DEFAULT_TO_NULL = "containerDefaultToNull";

    public static final String CAMEL_CASE_DOLLAR_SIGN = "camelCaseDollarSign";
    public static final String USE_ONE_OF_INTERFACES = "useOneOfInterfaces";
    public static final String LOMBOK = "lombok";
    public static final String DEFAULT_TEST_FOLDER = "${project.build.directory}/generated-test-sources/openapi";
    public static final String GENERATE_CONSTRUCTOR_WITH_ALL_ARGS = "generateConstructorWithAllArgs";
    public static final String GENERATE_BUILDERS = "generateBuilders";

    @Getter @Setter
    protected String dateLibrary = "java8";
    @Setter protected boolean supportAsync = false;
    @Setter protected boolean withXml = false;
    @Getter @Setter
    protected String invokerPackage = "org.openapitools";
    @Getter @Setter
    protected String groupId = "org.openapitools";
    @Getter @Setter
    protected String artifactId = "openapi-java";
    @Getter @Setter
    protected String artifactVersion = null;
    @Getter @Setter
    protected String artifactUrl = "https://github.com/openapitools/openapi-generator";
    @Getter @Setter
    protected String artifactDescription = "OpenAPI Java";
    @Getter @Setter
    protected String developerName = "OpenAPI-Generator Contributors";
    @Getter @Setter
    protected String developerEmail = "team@openapitools.org";
    @Getter @Setter
    protected String developerOrganization = "OpenAPITools.org";
    @Getter @Setter
    protected String developerOrganizationUrl = "http://openapitools.org";
    @Getter @Setter
    protected String scmConnection = "scm:git:git@github.com:openapitools/openapi-generator.git";
    @Getter @Setter
    protected String scmDeveloperConnection = "scm:git:git@github.com:openapitools/openapi-generator.git";
    @Getter @Setter
    protected String scmUrl = "https://github.com/openapitools/openapi-generator";
    @Getter @Setter
    protected String licenseName = "Unlicense";
    @Getter @Setter
    protected String licenseUrl = "http://unlicense.org";
    protected String projectFolder = "src/main";
    protected String projectTestFolder = "src/test";
    // this must not be OS-specific
    @Getter @Setter
    protected String sourceFolder = projectFolder + "/java";
    @Getter @Setter
    protected String testFolder = projectTestFolder + "/java";
    /**
     * -- SETTER --
     * Set whether discriminator value lookup is case-sensitive or not.
     *
     * @param discriminatorCaseSensitive true if the discriminator value lookup should be case-sensitive.
     */
    @Setter protected boolean discriminatorCaseSensitive = true;
    @Getter @Setter
    protected Boolean serializableModel = false;
    @Setter protected boolean serializeBigDecimalAsString = false;
    protected String apiDocPath = "docs/";
    protected String modelDocPath = "docs/";
    @Setter protected boolean disableHtmlEscaping = false;
    @Getter @Setter
    protected String booleanGetterPrefix = "get";
    @Setter protected boolean ignoreAnyOfInEnum = false;
    @Setter protected String parentGroupId = "";
    @Setter protected String parentArtifactId = "";
    @Setter protected String parentVersion = "";
    @Setter protected boolean parentOverridden = false;
    @Getter @Setter
    protected List<String> additionalModelTypeAnnotations = new LinkedList<>();
    protected Map<String, Boolean> lombokAnnotations = null;
    @Getter @Setter
    protected List<String> additionalOneOfTypeAnnotations = new LinkedList<>();
    @Setter protected List<String> additionalEnumTypeAnnotations = new LinkedList<>();
    @Getter @Setter
    protected boolean openApiNullable = true;
    @Setter protected String outputTestFolder = "";
    protected DocumentationProvider documentationProvider;
    protected AnnotationLibrary annotationLibrary;
    @Setter protected boolean implicitHeaders = false;
    @Setter protected String implicitHeadersRegex = null;
    @Setter protected boolean camelCaseDollarSign = false;
    @Setter protected boolean useJakartaEe = false;
    @Setter protected boolean containerDefaultToNull = false;
    @Getter @Setter
    protected boolean generateConstructorWithAllArgs = false;
    @Getter @Setter
    protected boolean jackson = false;
    @Getter @Setter
    protected boolean generateBuilders;
    /**
     * useBeanValidation has been moved from child generators to AbstractJavaCodegen.
     * The reason is that getBeanValidation needs it
     */
    @Getter @Setter
    protected boolean useBeanValidation = false;
    private Map<String, String> schemaKeyToModelNameCache = new HashMap<>();

    public AbstractJavaCodegen() {
        super();

        supportsInheritance = true;
        modelTemplateFiles.put("page-object-class.mustache", ".java");

        hideGenerationTimestamp = false;

        setReservedWordsLowerCase(
                Arrays.asList(
                        // special words
                        "object", "list", "file",
                        // used as internal variables, can collide with parameter names
                        "localVarPath", "localVarQueryParams", "localVarCollectionQueryParams",
                        "localVarHeaderParams", "localVarCookieParams", "localVarFormParams", "localVarPostBody",
                        "localVarAccepts", "localVarAccept", "localVarContentTypes",
                        "localVarContentType", "localVarAuthNames", "localReturnType",
                        "ApiClient", "ApiException", "ApiResponse", "Configuration", "StringUtil",

                        // language reserved words
                        "_", "abstract", "continue", "for", "new", "switch", "assert",
                        "default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
                        "this", "break", "double", "implements", "protected", "throw", "byte", "else",
                        "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
                        "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
                        "native", "super", "while", "null", "offsetdatetime", "localdate", "localtime")
        );

        languageSpecificPrimitives = Sets.newHashSet("String",
                "boolean",
                "Boolean",
                "Double",
                "Integer",
                "Long",
                "Float",
                "Object",
                "byte[]"
        );
        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("set", "LinkedHashSet");
        instantiationTypes.put("map", "HashMap");
        typeMapping.put("date", "Date");
        typeMapping.put("file", "File");
        typeMapping.put("AnyType", "Object");

        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("UUID", "java.util.UUID");
        importMapping.put("URI", "java.net.URI");
        importMapping.put("File", "java.io.File");
        importMapping.put("Date", "java.util.Date");
        importMapping.put("Timestamp", "java.sql.Timestamp");
        importMapping.put("Map", "java.util.Map");
        importMapping.put("HashMap", "java.util.HashMap");
        importMapping.put("Array", "java.util.List");
        importMapping.put("ArrayList", "java.util.ArrayList");
        importMapping.put("List", "java.util.*");
        importMapping.put("Set", "java.util.*");
        importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        importMapping.put("DateTime", "org.joda.time.*");
        importMapping.put("LocalDateTime", "org.joda.time.*");
        importMapping.put("LocalDate", "org.joda.time.*");
        importMapping.put("LocalTime", "org.joda.time.*");

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, CodegenConstants.INVOKER_PACKAGE_DESC).defaultValue(this.getInvokerPackage()));
        cliOptions.add(new CliOption(CodegenConstants.GROUP_ID, CodegenConstants.GROUP_ID_DESC).defaultValue(this.getGroupId()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_ID, CodegenConstants.ARTIFACT_ID_DESC).defaultValue(this.getArtifactId()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, CodegenConstants.ARTIFACT_VERSION_DESC).defaultValue(ARTIFACT_VERSION_DEFAULT_VALUE));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_URL, CodegenConstants.ARTIFACT_URL_DESC).defaultValue(this.getArtifactUrl()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_DESCRIPTION, CodegenConstants.ARTIFACT_DESCRIPTION_DESC).defaultValue(this.getArtifactDescription()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_CONNECTION, CodegenConstants.SCM_CONNECTION_DESC).defaultValue(this.getScmConnection()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_DEVELOPER_CONNECTION, CodegenConstants.SCM_DEVELOPER_CONNECTION_DESC).defaultValue(this.getScmDeveloperConnection()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_URL, CodegenConstants.SCM_URL_DESC).defaultValue(this.getScmUrl()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_NAME, CodegenConstants.DEVELOPER_NAME_DESC).defaultValue(this.getDeveloperName()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_EMAIL, CodegenConstants.DEVELOPER_EMAIL_DESC).defaultValue(this.getDeveloperEmail()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION, CodegenConstants.DEVELOPER_ORGANIZATION_DESC).defaultValue(this.getDeveloperOrganization()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION_URL, CodegenConstants.DEVELOPER_ORGANIZATION_URL_DESC).defaultValue(this.getDeveloperOrganizationUrl()));
        cliOptions.add(new CliOption(CodegenConstants.LICENSE_NAME, CodegenConstants.LICENSE_NAME_DESC).defaultValue(this.getLicenseName()));
        cliOptions.add(new CliOption(CodegenConstants.LICENSE_URL, CodegenConstants.LICENSE_URL_DESC).defaultValue(this.getLicenseUrl()));
        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC).defaultValue(this.getSourceFolder()));

        Map<String, String> snapShotVersionOptions = new HashMap<>();
        snapShotVersionOptions.put("true", "Use a SnapShot Version");
        snapShotVersionOptions.put("false", "Use a Release Version");

        if (null != defaultDocumentationProvider()) {
            CliOption documentationProviderCliOption = new CliOption(DOCUMENTATION_PROVIDER,
                    "Select the OpenAPI documentation provider.")
                    .defaultValue(defaultDocumentationProvider().toCliOptValue());
            cliOptions.add(documentationProviderCliOption);

            CliOption annotationLibraryCliOption = new CliOption(ANNOTATION_LIBRARY,
                    "Select the complementary documentation annotation library.")
                    .defaultValue(defaultDocumentationProvider().getPreferredAnnotationLibrary().toCliOptValue());
            cliOptions.add(annotationLibraryCliOption);
        }
    }

    @Override
    public void processOpts() {
        useCodegenAsMustacheParentContext();
        super.processOpts();

        if (null != defaultDocumentationProvider()) {
            documentationProvider = DocumentationProvider.ofCliOption(
                    (String) additionalProperties.getOrDefault(DOCUMENTATION_PROVIDER,
                            defaultDocumentationProvider().toCliOptValue())
            );

            if (!supportedDocumentationProvider().contains(documentationProvider)) {
                String msg = String.format(Locale.ROOT,
                        "The [%s] Documentation Provider is not supported by this generator",
                        documentationProvider.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            annotationLibrary = AnnotationLibrary.ofCliOption(
                    (String) additionalProperties.getOrDefault(ANNOTATION_LIBRARY,
                            documentationProvider.getPreferredAnnotationLibrary().toCliOptValue())
            );

            if (!supportedAnnotationLibraries().contains(annotationLibrary)) {
                String msg = String.format(Locale.ROOT, "The Annotation Library [%s] is not supported by this generator",
                        annotationLibrary.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            if (!documentationProvider.supportedAnnotationLibraries().contains(annotationLibrary)) {
                String msg = String.format(Locale.ROOT,
                        "The [%s] documentation provider does not support [%s] as complementary annotation library",
                        documentationProvider.toCliOptValue(), annotationLibrary.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            additionalProperties.put(DOCUMENTATION_PROVIDER, documentationProvider.toCliOptValue());
            additionalProperties.put(documentationProvider.getPropertyName(), true);
            additionalProperties.put(ANNOTATION_LIBRARY, annotationLibrary.toCliOptValue());
            additionalProperties.put(annotationLibrary.getPropertyName(), true);
        } else {
            additionalProperties.put(DOCUMENTATION_PROVIDER, DocumentationProvider.NONE);
            additionalProperties.put(ANNOTATION_LIBRARY, AnnotationLibrary.NONE);
        }

        convertPropertyToBooleanAndWriteBack(GENERATE_CONSTRUCTOR_WITH_ALL_ARGS, this::setGenerateConstructorWithAllArgs);
        convertPropertyToBooleanAndWriteBack(GENERATE_BUILDERS, this::setGenerateBuilders);
        if (StringUtils.isEmpty(System.getenv("JAVA_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable JAVA_POST_PROCESS_FILE not defined so the Java code may not be properly formatted. To define it, try 'export JAVA_POST_PROCESS_FILE=\"/usr/local/bin/clang-format -i\"' (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        } else if (!this.isEnablePostProcessFile()) {
            LOGGER.info("Warning: Environment variable 'JAVA_POST_PROCESS_FILE' is set but file post-processing is not enabled. To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        convertPropertyToBooleanAndWriteBack(BeanValidationFeatures.USE_BEANVALIDATION, this::setUseBeanValidation);
        convertPropertyToBooleanAndWriteBack(DISABLE_HTML_ESCAPING, this::setDisableHtmlEscaping);
        convertPropertyToStringAndWriteBack(BOOLEAN_GETTER_PREFIX, this::setBooleanGetterPrefix);
        convertPropertyToBooleanAndWriteBack(IGNORE_ANYOF_IN_ENUM, this::setIgnoreAnyOfInEnum);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_MODEL_TYPE_ANNOTATIONS,
                annotations-> Arrays.asList(annotations.trim().split("\\s*(;|\\r?\\n)\\s*")),
                this::setAdditionalModelTypeAnnotations);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS,
                annotations-> Arrays.asList(annotations.trim().split("\\s*(;|\\r?\\n)\\s*")),
                this::setAdditionalOneOfTypeAnnotations);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_ENUM_TYPE_ANNOTATIONS,
                annotations -> Arrays.asList(annotations.split(";")),
                this::setAdditionalEnumTypeAnnotations);

        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        } else if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            // guess from api package
            String derivedInvokerPackage = deriveInvokerPackageName((String) additionalProperties.get(CodegenConstants.API_PACKAGE));
            this.setInvokerPackage(derivedInvokerPackage);
            LOGGER.info("Invoker Package Name, originally not set, is now derived from api package name: {}", derivedInvokerPackage);
        } else if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            // guess from model package
            String derivedInvokerPackage = deriveInvokerPackageName((String) additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
            this.setInvokerPackage(derivedInvokerPackage);
            LOGGER.info("Invoker Package Name, originally not set, is now derived from model package name: {}",
                    derivedInvokerPackage);
        } else {
            //not set, use default to be passed to template
            additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        }

        if (additionalProperties.containsKey(CodegenConstants.GROUP_ID)) {
            this.setGroupId((String) additionalProperties.get(CodegenConstants.GROUP_ID));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(CodegenConstants.GROUP_ID, groupId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_ID)) {
            this.setArtifactId((String) additionalProperties.get(CodegenConstants.ARTIFACT_ID));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(CodegenConstants.ARTIFACT_ID, artifactId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_URL)) {
            this.setArtifactUrl((String) additionalProperties.get(CodegenConstants.ARTIFACT_URL));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_URL, artifactUrl);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_DESCRIPTION)) {
            this.setArtifactDescription((String) additionalProperties.get(CodegenConstants.ARTIFACT_DESCRIPTION));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_DESCRIPTION, artifactDescription);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_CONNECTION)) {
            this.setScmConnection((String) additionalProperties.get(CodegenConstants.SCM_CONNECTION));
        } else {
            additionalProperties.put(CodegenConstants.SCM_CONNECTION, scmConnection);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_DEVELOPER_CONNECTION)) {
            this.setScmDeveloperConnection((String) additionalProperties.get(CodegenConstants.SCM_DEVELOPER_CONNECTION));
        } else {
            additionalProperties.put(CodegenConstants.SCM_DEVELOPER_CONNECTION, scmDeveloperConnection);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_URL)) {
            this.setScmUrl((String) additionalProperties.get(CodegenConstants.SCM_URL));
        } else {
            additionalProperties.put(CodegenConstants.SCM_URL, scmUrl);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_NAME)) {
            this.setDeveloperName((String) additionalProperties.get(CodegenConstants.DEVELOPER_NAME));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_NAME, developerName);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_EMAIL)) {
            this.setDeveloperEmail((String) additionalProperties.get(CodegenConstants.DEVELOPER_EMAIL));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_EMAIL, developerEmail);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION)) {
            this.setDeveloperOrganization((String) additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION, developerOrganization);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION_URL)) {
            this.setDeveloperOrganizationUrl((String) additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION_URL));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION_URL, developerOrganizationUrl);
        }

        convertPropertyToStringAndWriteBack(CodegenConstants.MODEL_PACKAGE, this::setModelPackage);
        convertPropertyToStringAndWriteBack(CodegenConstants.API_PACKAGE, this::setApiPackage);
        convertPropertyToStringAndWriteBack(CodegenConstants.GROUP_ID, this::setGroupId);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_ID, this::setArtifactId);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_URL, this::setArtifactUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_DESCRIPTION, this::setArtifactDescription);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_CONNECTION, this::setScmConnection);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_DEVELOPER_CONNECTION, this::setScmDeveloperConnection);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_URL, this::setScmUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_NAME, this::setDeveloperName);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_EMAIL, this::setDeveloperEmail);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_ORGANIZATION, this::setDeveloperOrganization);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_ORGANIZATION_URL, this::setDeveloperOrganizationUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.LICENSE_NAME, this::setLicenseName);
        convertPropertyToStringAndWriteBack(CodegenConstants.LICENSE_URL, this::setLicenseUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.SOURCE_FOLDER, this::setSourceFolder);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SERIALIZABLE_MODEL, this::setSerializableModel);
        convertPropertyToStringAndWriteBack(CodegenConstants.LIBRARY, this::setLibrary);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING, this::setSerializeBigDecimalAsString );
        // need to put back serializableModel (boolean) into additionalProperties as value in additionalProperties is string
//        additionalProperties.put(CodegenConstants.SERIALIZABLE_MODEL, serializableModel);

            // By default, the discriminator lookup should be case sensitive. There is nothing in the OpenAPI specification
            // that indicates the lookup should be case insensitive. However, some implementations perform
            // a case-insensitive lookup.
        convertPropertyToBooleanAndWriteBack(DISCRIMINATOR_CASE_SENSITIVE, this::setDiscriminatorCaseSensitive);
        convertPropertyToBooleanAndWriteBack(WITH_XML, this::setWithXml);
        convertPropertyToBooleanAndWriteBack(OPENAPI_NULLABLE, this::setOpenApiNullable);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_GROUP_ID, this::setParentGroupId);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_ARTIFACT_ID, this::setParentArtifactId);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_VERSION, this::setParentVersion);
        convertPropertyToBooleanAndWriteBack(IMPLICIT_HEADERS, this::setImplicitHeaders);
        convertPropertyToStringAndWriteBack(IMPLICIT_HEADERS_REGEX, this::setImplicitHeadersRegex);
        convertPropertyToBooleanAndWriteBack(CAMEL_CASE_DOLLAR_SIGN, this::setCamelCaseDollarSign);
        convertPropertyToBooleanAndWriteBack(USE_ONE_OF_INTERFACES, this::setUseOneOfInterfaces);

        if (!StringUtils.isEmpty(parentGroupId) && !StringUtils.isEmpty(parentArtifactId) && !StringUtils.isEmpty(parentVersion)) {
            additionalProperties.put("parentOverridden", true);
        }

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        importMapping.put("List", "java.util.List");
        importMapping.put("Set", "java.util.Set");

        this.sanitizeConfig();

        // optional jackson mappings for BigDecimal support
        if (serializeBigDecimalAsString) {
            importMapping.put("JsonFormat", "com.fasterxml.jackson.annotation.JsonFormat");
        }

        // imports for pojos
        importMapping.put("ApiModelProperty", "io.swagger.annotations.ApiModelProperty");
        importMapping.put("ApiModel", "io.swagger.annotations.ApiModel");
        importMapping.put("Schema", "io.swagger.v3.oas.annotations.media.Schema");
        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("JsonDeserialize", "com.fasterxml.jackson.databind.annotation.JsonDeserialize");
        importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put("JsonSubTypes", "com.fasterxml.jackson.annotation.JsonSubTypes");
        importMapping.put("JsonTypeInfo", "com.fasterxml.jackson.annotation.JsonTypeInfo");
        importMapping.put("JsonTypeName", "com.fasterxml.jackson.annotation.JsonTypeName");
        importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
        importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("JsonIgnore", "com.fasterxml.jackson.annotation.JsonIgnore");
        importMapping.put("JsonIgnoreProperties", "com.fasterxml.jackson.annotation.JsonIgnoreProperties");
        importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
        if (openApiNullable) {
            importMapping.put("JsonNullable", "org.openapitools.jackson.nullable.JsonNullable");
        }
        importMapping.put("SerializedName", "com.google.gson.annotations.SerializedName");
        importMapping.put("TypeAdapter", "com.google.gson.TypeAdapter");
        importMapping.put("JsonAdapter", "com.google.gson.annotations.JsonAdapter");
        importMapping.put("JsonReader", "com.google.gson.stream.JsonReader");
        importMapping.put("JsonWriter", "com.google.gson.stream.JsonWriter");
        importMapping.put("IOException", "java.io.IOException");
        importMapping.put("Arrays", "java.util.Arrays");
        importMapping.put("Objects", "java.util.Objects");
        importMapping.put("StringUtil", invokerPackage + ".StringUtil");
        // import JsonCreator if JsonProperty is imported
        // used later in recursive import in postProcessingModels
        importMapping.put("com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.annotation.JsonCreator");

        convertPropertyToBooleanAndWriteBack(SUPPORT_ASYNC, this::setSupportAsync);
        convertPropertyToStringAndWriteBack(DATE_LIBRARY, this::setDateLibrary);

        if ("joda".equals(dateLibrary)) {
            additionalProperties.put("joda", "true");
            typeMapping.put("date", "LocalDate");
            typeMapping.put("DateTime", "DateTime");
            importMapping.put("LocalDate", "org.joda.time.LocalDate");
            importMapping.put("DateTime", "org.joda.time.DateTime");
        } else if (dateLibrary.startsWith("java8")) {
            additionalProperties.put("java8", "true");
            additionalProperties.put("jsr310", "true");
            typeMapping.put("date", "LocalDate");
            importMapping.put("LocalDate", "java.time.LocalDate");
            importMapping.put("LocalTime", "java.time.LocalTime");
            if ("java8-localdatetime".equals(dateLibrary)) {
                typeMapping.put("DateTime", "LocalDateTime");
                importMapping.put("LocalDateTime", "java.time.LocalDateTime");
            } else {
                typeMapping.put("DateTime", "OffsetDateTime");
                importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
            }
        } else if (dateLibrary.equals("legacy")) {
            additionalProperties.put("legacyDates", "true");
        }

        convertPropertyToStringAndWriteBack(TEST_OUTPUT, this::setOutputTestFolder);
        convertPropertyToBooleanAndWriteBack(USE_JAKARTA_EE, this::setUseJakartaEe);
        if (useJakartaEe) {
            applyJakartaPackage();
        } else {
            applyJavaxPackage();
        }

        convertPropertyToBooleanAndWriteBack(CONTAINER_DEFAULT_TO_NULL, this::setContainerDefaultToNull);

        additionalProperties.put("sanitizeGeneric", (Mustache.Lambda) (fragment, writer) -> {
        });
    }

    private void sanitizeConfig() {
        // Sanitize any config options here. We also have to update the additionalProperties because
        // the whole additionalProperties object is injected into the main object passed to the mustache layer

        this.setApiPackage(sanitizePackageName(apiPackage));
        additionalProperties.remove(CodegenConstants.API_PACKAGE);
        this.setModelPackage(sanitizePackageName(modelPackage));
        additionalProperties.remove(CodegenConstants.MODEL_PACKAGE);
        this.setInvokerPackage(sanitizePackageName(invokerPackage));
        additionalProperties.remove(CodegenConstants.INVOKER_PACKAGE);
    }

    protected void applyJavaxPackage() {
        writePropertyBack(JAVAX_PACKAGE, "javax");
    }

    protected void applyJakartaPackage() {
        writePropertyBack(JAVAX_PACKAGE, "jakarta");
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + File.separator + sourceFolder + File.separator + apiPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return (outputTestFolder + File.separator + testFolder + File.separator + apiPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return (outputTestFolder + File.separator + testFolder + File.separator + modelPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return (outputFolder + File.separator + sourceFolder + File.separator + modelPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + File.separator + apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + File.separator + modelDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelName(name) + "Test";
    }

    @Override
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toVarName(String name) {
        // obtain the name from nameMapping directly if provided
        if (nameMapping.containsKey(name)) {
            return nameMapping.get(name);
        }

        // sanitize name
//        name = sanitizeName(name, "\\W-[\\$]"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if (name.toLowerCase(Locale.ROOT).matches("^_*class$")) {
            return "propertyClass";
        }

        if ("_".equals(name)) {
            name = "_u";
        }

        // numbers are not allowed at the beginning
        if (name.matches("^\\d.*")) {
            name = "_" + name;
        }

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z0-9_]*$")) {
            return name;
        }

        if (startsWithTwoUppercaseLetters(name)) {
            name = name.substring(0, 2).toLowerCase(Locale.ROOT) + name.substring(2);
        }

        // If name contains special chars -> replace them.
        if ((((CharSequence) name).chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character))))) {
            List<String> allowedCharacters = new ArrayList<>();
            allowedCharacters.add("_");
            allowedCharacters.add("$");
            name = escape(name, specialCharReplacements, allowedCharacters, "_");
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        if (camelCaseDollarSign) {
            name = camelize(name, LOWERCASE_FIRST_CHAR);
        } else {
            name = camelize(name, LOWERCASE_FIRST_LETTER);
        }

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    private boolean startsWithTwoUppercaseLetters(String name) {
        boolean startsWithTwoUppercaseLetters = false;
        if (name.length() > 1) {
            startsWithTwoUppercaseLetters = name.substring(0, 2).equals(name.substring(0, 2).toUpperCase(Locale.ROOT));
        }
        return startsWithTwoUppercaseLetters;
    }

    @Override
    public String toParamName(String name) {
        // obtain the name from paramterNameMapping directly if provided
        if (parameterNameMapping.containsKey(name)) {
            return parameterNameMapping.get(name);
        }

        // to avoid conflicts with 'callback' parameter for async call
        if ("callback".equals(name)) {
            return "paramCallback";
        }

        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(final String name) {
        // obtain the name from modelNameMapping directly if provided
        if (modelNameMapping.containsKey(name)) {
            return modelNameMapping.get(name);
        }

        // We need to check if schema-mapping has a different model for this class, so we use it
        // instead of the auto-generated one.
        if (schemaMapping.containsKey(name)) {
            return schemaMapping.get(name);
        }

        // memoization
        String origName = name;
        if (schemaKeyToModelNameCache.containsKey(origName)) {
            return schemaKeyToModelNameCache.get(origName);
        }

        final String sanitizedName = sanitizeName(name);

        String nameWithPrefixSuffix = sanitizedName;
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        final String camelizedName = camelize(nameWithPrefixSuffix);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            schemaKeyToModelNameCache.put(origName, modelName);
            LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", camelizedName, modelName);
            return modelName;
        }

        // model name starts with number
        if (camelizedName.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
            schemaKeyToModelNameCache.put(origName, modelName);
            LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name,
                    modelName);
            return modelName;
        }

        schemaKeyToModelNameCache.put(origName, camelizedName);

        return camelizedName;
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), LOWERCASE_FIRST_LETTER);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
            LOGGER.warn("{} (reserved word) cannot be used as method name. Renamed to {}", operationId, newOperationId);
            return newOperationId;
        }

        // operationId starts with a number
        if (operationId.matches("^\\d.*")) {
            LOGGER.warn(operationId + " (starting with a number) cannot be used as method name. Renamed to " + camelize("call_" + operationId), true);
            operationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
        }

        return operationId;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (enumNameMapping.containsKey(value)) {
            return enumNameMapping.get(value);
        }

        if (value.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return getSymbolName(value).toUpperCase(Locale.ROOT);
        }

        if (" ".equals(value)) {
            return "SPACE";
        }

        // number
        if ("Integer".equals(datatype) || "Long".equals(datatype) ||
                "Float".equals(datatype) || "Double".equals(datatype) || "BigDecimal".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String var = value.replaceAll("\\W+", "_").toUpperCase(Locale.ROOT);
        if (var.matches("\\d.*")) {
            var = "_" + var;
        }
        return this.toVarName(var);
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("Integer".equals(datatype) || "Double".equals(datatype)) {
            return value;
        } else if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048l
            return value + "l";
        } else if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14f
            return value + "f";
        } else if ("BigDecimal".equals(datatype)) {
            // use BigDecimal String constructor
            return "new BigDecimal(\"" + value + "\")";
        } else if ("URI".equals(datatype)) {
            return "URI.create(\"" + escapeText(value) + "\")";
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    private static String sanitizePackageName(String packageName) {
        packageName = packageName.trim(); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if (Strings.isNullOrEmpty(packageName)) {
            return "invalidPackageName";
        }
        return packageName;
    }

    private String sanitizePath(String p) {
        //prefer replace a ", instead of a fuLL URL encode for readability
        return p.replaceAll("\"", "%22");
    }

    @Override
    public void setOutputDir(String dir) {
        super.setOutputDir(dir);
        if (this.outputTestFolder.isEmpty()) {
            setOutputTestFolder(dir);
        }
    }

    public String getOutputTestFolder() {
        if (outputTestFolder.isEmpty()) {
            return DEFAULT_TEST_FOLDER;
        }
        return outputTestFolder;
    }

    @Override
    public DocumentationProvider getDocumentationProvider() {
        return documentationProvider;
    }

    @Override
    public void setDocumentationProvider(DocumentationProvider documentationProvider) {
        this.documentationProvider = documentationProvider;
    }

    @Override
    public AnnotationLibrary getAnnotationLibrary() {
        return annotationLibrary;
    }

    @Override
    public void setAnnotationLibrary(AnnotationLibrary annotationLibrary) {
        this.annotationLibrary = annotationLibrary;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    /*
     * Derive invoker package name based on the input
     * e.g. foo.bar.model => foo.bar
     *
     * @param input API package/model name
     * @return Derived invoker package name based on API package/model name
     */
    private String deriveInvokerPackageName(String input) {
        String[] parts = input.split(Pattern.quote(".")); // Split on period.

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String p : Arrays.copyOf(parts, parts.length - 1)) {
            sb.append(delim).append(p);
            delim = ".";
        }
        return sb.toString();
    }

    /**
     * Builds a SNAPSHOT version from a given version.
     *
     * @param version
     * @return SNAPSHOT version
     */
    private String buildSnapshotVersion(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            return version;
        }
        return version + "-SNAPSHOT";
    }

    @Override
    public String toRegularExpression(String pattern) {
        return escapeText(pattern);
    }

    /**
     * Output the Getter name for boolean property, e.g. isActive
     *
     * @param name the name of the property
     * @return getter name based on naming convention
     */

    /**
     * Camelize the method name of the getter and setter
     *
     * @param name string to be camelized
     * @return Camelized string
     */
    public String getterAndSetterCapitalize(String name) {
        CamelizeOption camelizeOption = UPPERCASE_FIRST_CHAR;
        if (name == null || name.length() == 0) {
            return name;
        }
        name = toVarName(name);
        //
        // Let the property name capitalized
        // except when the first letter of the property name is lowercase and the second letter is uppercase
        // Refer to section 8.8: Capitalization of inferred names of the JavaBeans API specification
        // http://download.oracle.com/otn-pub/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/beans.101.pdf)
        //
        if (name.length() > 1 && Character.isLowerCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            camelizeOption = LOWERCASE_FIRST_LETTER;
        }
        return camelize(name, camelizeOption);
    }

    @Override
    public void postProcessFile(File file, String fileType) {
        super.postProcessFile(file, fileType);
        if (file == null) {
            return;
        }

        String javaPostProcessFile = System.getenv("JAVA_POST_PROCESS_FILE");
        if (StringUtils.isEmpty(javaPostProcessFile)) {
            return; // skip if JAVA_POST_PROCESS_FILE env variable is not defined
        }

        // only process files with java extension
        if ("java".equals(FilenameUtils.getExtension(file.toString()))) {
            this.executePostProcessor(new String[] {javaPostProcessFile, file.toString()});
        }
    }

    public void addImportsToOneOfInterface(List<Map<String, String>> imports) {
        if (jackson) {
            for (String i : Arrays.asList("JsonSubTypes", "JsonTypeInfo")) {
                Map<String, String> oneImport = new HashMap<>();
                oneImport.put("import", importMapping.get(i));
                if (!imports.contains(oneImport)) {
                    imports.add(oneImport);
                }
            }
        }
    }

    @Override
    public boolean isTypeErasedGenerics() {
        return true;
    }
}
