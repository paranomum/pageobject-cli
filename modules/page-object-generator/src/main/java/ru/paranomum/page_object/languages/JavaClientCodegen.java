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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import ru.paranomum.page_object.*;
import ru.paranomum.page_object.languages.features.BeanValidationFeatures;
import ru.paranomum.page_object.languages.features.GzipFeatures;
import ru.paranomum.page_object.languages.features.PerformBeanValidationFeatures;
import ru.paranomum.page_object.meta.features.DocumentationFeature;
import ru.paranomum.page_object.meta.features.GlobalFeature;
import ru.paranomum.page_object.meta.features.SecurityFeature;
import ru.paranomum.page_object.templating.mustache.CaseFormatLambda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.Collections.sort;
import static ru.paranomum.page_object.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static ru.paranomum.page_object.utils.StringUtils.camelize;

public class JavaClientCodegen extends AbstractJavaCodegen
        implements BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures {

    static final String MEDIA_TYPE = "mediaType";

    private final Logger LOGGER = LoggerFactory.getLogger(JavaClientCodegen.class);

    public static final String USE_RX_JAVA2 = "useRxJava2";
    public static final String USE_RX_JAVA3 = "useRxJava3";
    public static final String DO_NOT_USE_RX = "doNotUseRx";
    public static final String USE_PLAY_WS = "usePlayWS";
    public static final String ASYNC_NATIVE = "asyncNative";
    public static final String CONFIG_KEY = "configKey";
    public static final String CONFIG_KEY_FROM_CLASS_NAME = "configKeyFromClassName";
    public static final String PARCELABLE_MODEL = "parcelableModel";
    public static final String USE_RUNTIME_EXCEPTION = "useRuntimeException";
    public static final String USE_REFLECTION_EQUALS_HASHCODE = "useReflectionEqualsHashCode";
    public static final String CASE_INSENSITIVE_RESPONSE_HEADERS = "caseInsensitiveResponseHeaders";
    public static final String MICROPROFILE_FRAMEWORK = "microprofileFramework";
    public static final String MICROPROFILE_MUTINY = "microprofileMutiny";
    public static final String USE_ABSTRACTION_FOR_FILES = "useAbstractionForFiles";
    public static final String DYNAMIC_OPERATIONS = "dynamicOperations";
    public static final String SUPPORT_STREAMING = "supportStreaming";
    public static final String SUPPORT_URL_QUERY = "supportUrlQuery";
    public static final String GRADLE_PROPERTIES = "gradleProperties";
    public static final String ERROR_OBJECT_TYPE = "errorObjectType";

    public static final String FEIGN = "feign";
    public static final String GOOGLE_API_CLIENT = "google-api-client";
    public static final String JERSEY2 = "jersey2";
    public static final String JERSEY3 = "jersey3";
    public static final String NATIVE = "native";
    public static final String OKHTTP_GSON = "okhttp-gson";
    public static final String RESTEASY = "resteasy";
    public static final String RESTTEMPLATE = "resttemplate";
    public static final String WEBCLIENT = "webclient";
    public static final String RESTCLIENT = "restclient";
    public static final String REST_ASSURED = "rest-assured";
    public static final String RETROFIT_2 = "retrofit2";
    public static final String VERTX = "vertx";
    public static final String MICROPROFILE = "microprofile";
    public static final String APACHE = "apache-httpclient";
    public static final String MICROPROFILE_REST_CLIENT_VERSION = "microprofileRestClientVersion";
    public static final String MICROPROFILE_REST_CLIENT_DEFAULT_VERSION = "2.0";
    public static final String MICROPROFILE_REST_CLIENT_DEFAULT_ROOT_PACKAGE = "javax";
    public static final String MICROPROFILE_DEFAULT = "default";
    public static final String MICROPROFILE_KUMULUZEE = "kumuluzee";
    public static final String WEBCLIENT_BLOCKING_OPERATIONS = "webclientBlockingOperations";
    public static final String USE_ENUM_CASE_INSENSITIVE = "useEnumCaseInsensitive";
    public static final String FAIL_ON_UNKNOWN_PROPERTIES = "failOnUnknownProperties";

    public static final String SERIALIZATION_LIBRARY_GSON = "gson";
    public static final String SERIALIZATION_LIBRARY_JACKSON = "jackson";
    public static final String SERIALIZATION_LIBRARY_JSONB = "jsonb";

    public static final String GENERATE_CLIENT_AS_BEAN = "generateClientAsBean";

    protected String gradleWrapperPackage = "gradle.wrapper";
    protected boolean useRxJava = false;
    protected boolean useRxJava2 = false;
    protected boolean useRxJava3 = false;
    // backwards compatibility for openapi configs that specify neither rx1 nor rx2
    // (mustache does not allow for boolean operators so we need this extra field)
    @Setter protected boolean doNotUseRx = true;
    @Setter protected boolean usePlayWS = false;
    @Setter protected String microprofileFramework = MICROPROFILE_DEFAULT;
    @Setter protected String microprofileRestClientVersion = MICROPROFILE_REST_CLIENT_DEFAULT_VERSION;
    @Setter protected boolean microprofileMutiny = false;
    @Setter protected String configKey = null;
    @Setter(AccessLevel.PRIVATE) protected boolean configKeyFromClassName = false;

    @Setter protected boolean asyncNative = false;
    @Setter protected boolean parcelableModel = false;
    @Setter protected boolean performBeanValidation = false;
    @Setter protected boolean useGzipFeature = false;
    @Setter protected boolean useRuntimeException = false;
    @Setter protected boolean useReflectionEqualsHashCode = false;
    protected boolean caseInsensitiveResponseHeaders = false;
    @Setter protected boolean useAbstractionForFiles = false;
    @Setter protected boolean dynamicOperations = false;
    @Setter protected boolean supportStreaming = false;
    @Setter protected boolean withAWSV4Signature = false;
    @Setter protected String gradleProperties;
    @Setter protected String errorObjectType;
    @Getter @Setter protected boolean failOnUnknownProperties = false;
    protected String authFolder;
    /**
     *  Serialization library.
     */
    @Getter protected String serializationLibrary = null;
    @Setter protected boolean useOneOfDiscriminatorLookup = false; // use oneOf discriminator's mapping for model lookup
    protected String rootJavaEEPackage;
    protected Map<String, MpRestClientVersion> mpRestClientVersions = new LinkedHashMap<>();
    @Setter(AccessLevel.PRIVATE) protected boolean useSingleRequestParameter = false;
    protected boolean webclientBlockingOperations = false;
    @Setter protected boolean generateClientAsBean = false;
    @Setter protected boolean useEnumCaseInsensitive = false;

    @Setter protected int maxAttemptsForRetry = 1;
    @Setter protected long waitTimeMillis = 10l;

    private static class MpRestClientVersion {
        public final String rootPackage;
        public final String pomTemplate;

        public MpRestClientVersion(String rootPackage, String pomTemplate) {
            this.rootPackage = rootPackage;
            this.pomTemplate = pomTemplate;
        }
    }

    @Override
    public DocumentationProvider defaultDocumentationProvider() {
        return DocumentationProvider.SOURCE;
    }

    @Override
    public List<DocumentationProvider> supportedDocumentationProvider() {
        List<DocumentationProvider> documentationProviders = new ArrayList<>();
        documentationProviders.add(DocumentationProvider.NONE);
        documentationProviders.add(DocumentationProvider.SOURCE);
        return documentationProviders;
    }

    @Override
    public List<AnnotationLibrary> supportedAnnotationLibraries() {
        List<AnnotationLibrary> annotationLibraries = new ArrayList<>();
        annotationLibraries.add(AnnotationLibrary.NONE);
        annotationLibraries.add(AnnotationLibrary.SWAGGER1);
        annotationLibraries.add(AnnotationLibrary.SWAGGER2);
        return annotationLibraries;
    }

    public JavaClientCodegen() {
        super();

        outputFolder = "generated-code" + File.separator + "java";
        embeddedTemplateDir = templateDir = "Java";
        invokerPackage = "org.openapitools.client";
        artifactId = "openapi-java-client";
        apiPackage = "org.openapitools.client.api";
        modelPackage = "org.openapitools.client.model";
        rootJavaEEPackage = MICROPROFILE_REST_CLIENT_DEFAULT_ROOT_PACKAGE;

        // cliOptions default redefinition need to be updated
        updateOption(CodegenConstants.INVOKER_PACKAGE, this.getInvokerPackage());
        updateOption(CodegenConstants.ARTIFACT_ID, this.getArtifactId());
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);

        modelTestTemplateFiles.put("model_test.mustache", ".java");

        supportedLibraries.put(JERSEY2, "HTTP client: Jersey client 2.25.1. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(JERSEY3, "HTTP client: Jersey client 3.1.1. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(FEIGN, "HTTP client: OpenFeign 13.2.1. JSON processing: Jackson 2.17.1 or Gson 2.10.1");
        supportedLibraries.put(OKHTTP_GSON, "[DEFAULT] HTTP client: OkHttp 4.11.0. JSON processing: Gson 2.10.1. Enable Parcelable models on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        supportedLibraries.put(RETROFIT_2, "HTTP client: OkHttp 4.11.0. JSON processing: Gson 2.10.1 (Retrofit 2.5.0) or Jackson 2.17.1. Enable the RxJava adapter using '-DuseRxJava[2/3]=true'. (RxJava 1.x or 2.x or 3.x)");
        supportedLibraries.put(RESTTEMPLATE, "HTTP client: Spring RestTemplate 5.3.33 (6.1.5 if `useJakartaEe=true`). JSON processing: Jackson 2.17.1");
        supportedLibraries.put(WEBCLIENT, "HTTP client: Spring WebClient 5.1.18. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(RESTCLIENT, "HTTP client: Spring RestClient 6.1.6. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(RESTEASY, "HTTP client: Resteasy client 4.7.6. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(VERTX, "HTTP client: VertX client 3.5.2. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(GOOGLE_API_CLIENT, "HTTP client: Google API client 2.2.0. JSON processing: Jackson 2.17.1");
        supportedLibraries.put(REST_ASSURED, "HTTP client: rest-assured 5.3.2. JSON processing: Gson 2.10.1 or Jackson 2.17.1. Only for Java 8");
        supportedLibraries.put(NATIVE, "HTTP client: Java native HttpClient. JSON processing: Jackson 2.17.1. Only for Java11+");
        supportedLibraries.put(MICROPROFILE, "HTTP client: Microprofile client " + MICROPROFILE_REST_CLIENT_DEFAULT_VERSION + " (default, set desired version via `" + MICROPROFILE_REST_CLIENT_VERSION + "=x.x.x`). JSON processing: JSON-B 1.0.2 or Jackson 2.17.1");
        supportedLibraries.put(APACHE, "HTTP client: Apache httpclient 5.2.1. JSON processing: Jackson 2.17.1");

        CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        // set okhttp-gson as the default
        libraryOption.setDefault(OKHTTP_GSON);
        cliOptions.add(libraryOption);
        setLibrary(OKHTTP_GSON);

        CliOption serializationLibrary = new CliOption(CodegenConstants.SERIALIZATION_LIBRARY, "Serialization library, default depends on value of the option library");
        Map<String, String> serializationOptions = new HashMap<>();
        serializationOptions.put(SERIALIZATION_LIBRARY_GSON, "Use Gson as serialization library");
        serializationOptions.put(SERIALIZATION_LIBRARY_JACKSON, "Use Jackson as serialization library");
        serializationOptions.put(SERIALIZATION_LIBRARY_JSONB, "Use JSON-B as serialization library");
        cliOptions.add(serializationLibrary);

        // Ensure the OAS 3.x discriminator mappings include any descendent schemas that allOf
        // inherit from self, any oneOf schemas, any anyOf schemas, any x-discriminator-values,
        // and the discriminator mapping schemas in the OAS document.
        this.setLegacyDiscriminatorBehavior(false);

        initMpRestClientVersionToRootPackage();
    }

    private void initMpRestClientVersionToRootPackage() {
        mpRestClientVersions.put("1.4.1", new MpRestClientVersion("javax", "pom.mustache"));
        mpRestClientVersions.put("2.0", new MpRestClientVersion("javax", "pom.mustache"));
        mpRestClientVersions.put("3.0", new MpRestClientVersion("jakarta", "pom_3.0.mustache"));
    }

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String getHelp() {
        return "Generates a Java client library (HTTP lib: Jersey (1.x, 2.x), Retrofit (2.x), OpenFeign (10.x) and more.";
    }

    @Override
    public void processOpts() {
        if (WEBCLIENT.equals(getLibrary()) || NATIVE.equals(getLibrary()) || RESTCLIENT.equals(getLibrary())) {
            dateLibrary = "java8";
        } else if (MICROPROFILE.equals(getLibrary())) {
            dateLibrary = "legacy";
        }
        super.processOpts();
        // default jackson unless overriden by setSerializationLibrary
        this.jackson = !additionalProperties.containsKey(CodegenConstants.SERIALIZATION_LIBRARY) || SERIALIZATION_LIBRARY_JACKSON.equals(additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY));

        convertPropertyToBooleanAndWriteBack(CodegenConstants.USE_ONEOF_DISCRIMINATOR_LOOKUP, this::setUseOneOfDiscriminatorLookup);

        // RxJava
        if (additionalProperties.containsKey(USE_RX_JAVA2) && additionalProperties.containsKey(USE_RX_JAVA3)) {
            LOGGER.warn("You specified all RxJava versions 2 and 3 but they are mutually exclusive. Defaulting to v3.");
            convertPropertyToBooleanAndWriteBack(USE_RX_JAVA3, this::setUseRxJava3);
            writePropertyBack(USE_RX_JAVA2, false);
            } else {
            convertPropertyToBooleanAndWriteBack(USE_RX_JAVA3, this::setUseRxJava3);
            convertPropertyToBooleanAndWriteBack(USE_RX_JAVA2, this::setUseRxJava2);
        }
        convertPropertyToBooleanAndWriteBack(CodegenConstants.USE_SINGLE_REQUEST_PARAMETER, this::setUseSingleRequestParameter);

        if (!useRxJava && !useRxJava2 && !useRxJava3) {
            additionalProperties.put(DO_NOT_USE_RX, true);
        }

        // Java Play
        convertPropertyToBooleanAndWriteBack(USE_PLAY_WS, this::setUsePlayWS);

        // Microprofile framework
        if (additionalProperties.containsKey(MICROPROFILE_FRAMEWORK)) {
            if (!MICROPROFILE_KUMULUZEE.equals(microprofileFramework)) {
                throw new RuntimeException("Invalid microprofileFramework '" + microprofileFramework + "'. Must be 'kumuluzee' or none.");
            }
//            this.setMicroprofileFramework(additionalProperties.get(MICROPROFILE_FRAMEWORK).toString());
        }
        convertPropertyToStringAndWriteBack(MICROPROFILE_FRAMEWORK, this::setMicroprofileFramework);

        convertPropertyToBooleanAndWriteBack(MICROPROFILE_MUTINY, this::setMicroprofileMutiny);

        convertPropertyToStringAndWriteBack(MICROPROFILE_REST_CLIENT_VERSION, value->microprofileRestClientVersion=value);
        if (!mpRestClientVersions.containsKey(microprofileRestClientVersion)) {
                throw new IllegalArgumentException(
                        String.format(Locale.ROOT,
                                "Version %s of MicroProfile Rest Client is not supported or incorrect. Supported versions are %s",
                            microprofileRestClientVersion,
                                String.join(", ", mpRestClientVersions.keySet())
                        )
                );
            }

        if (!additionalProperties.containsKey("rootJavaEEPackage")) {
            String mpRestClientVersion = (String) additionalProperties.get(MICROPROFILE_REST_CLIENT_VERSION);
            if (mpRestClientVersions.containsKey(mpRestClientVersion)) {
                rootJavaEEPackage = mpRestClientVersions.get(mpRestClientVersion).rootPackage;
            }
            additionalProperties.put("rootJavaEEPackage", rootJavaEEPackage);
        }

        if (additionalProperties.containsKey(CONFIG_KEY)) {
            convertPropertyToStringAndWriteBack(CONFIG_KEY, this::setConfigKey);
        } else {
            convertPropertyToBooleanAndWriteBack(CONFIG_KEY_FROM_CLASS_NAME, this::setConfigKeyFromClassName);
        }

        convertPropertyToBooleanAndWriteBack(ASYNC_NATIVE, this::setAsyncNative);
        convertPropertyToBooleanAndWriteBack(PARCELABLE_MODEL, this::setParcelableModel);
        convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION, this::setPerformBeanValidation);
        convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE, this::setUseGzipFeature);
        convertPropertyToBooleanAndWriteBack(USE_RUNTIME_EXCEPTION, this::setUseRuntimeException);
        convertPropertyToBooleanAndWriteBack(USE_REFLECTION_EQUALS_HASHCODE, this::setUseReflectionEqualsHashCode);
        convertPropertyToBooleanAndWriteBack(CASE_INSENSITIVE_RESPONSE_HEADERS, this::setUseReflectionEqualsHashCode);
        convertPropertyToBooleanAndWriteBack(USE_ABSTRACTION_FOR_FILES, this::setUseAbstractionForFiles);
        convertPropertyToBooleanAndWriteBack(DYNAMIC_OPERATIONS, this::setDynamicOperations);
        convertPropertyToBooleanAndWriteBack(SUPPORT_STREAMING, this::setSupportStreaming);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.WITH_AWSV4_SIGNATURE_COMMENT, this::setWithAWSV4Signature);
        convertPropertyToStringAndWriteBack(GRADLE_PROPERTIES, this::setGradleProperties);
        convertPropertyToStringAndWriteBack(ERROR_OBJECT_TYPE, this::setErrorObjectType);
        convertPropertyToBooleanAndWriteBack(WEBCLIENT_BLOCKING_OPERATIONS, op -> webclientBlockingOperations=op);
        convertPropertyToBooleanAndWriteBack(FAIL_ON_UNKNOWN_PROPERTIES, this::setFailOnUnknownProperties);

        // add URL query deepObject support to native, apache-httpclient by default
        if (!additionalProperties.containsKey(SUPPORT_URL_QUERY)) {
            if (isLibrary(NATIVE) || isLibrary(APACHE)) {
                // default to true for native and apache-httpclient
                additionalProperties.put(SUPPORT_URL_QUERY, true);
            }
        } else {
            additionalProperties.put(SUPPORT_URL_QUERY, Boolean.parseBoolean(additionalProperties.get(SUPPORT_URL_QUERY).toString()));
        }

        convertPropertyToBooleanAndWriteBack(GENERATE_CLIENT_AS_BEAN, this::setGenerateClientAsBean);
        convertPropertyToBooleanAndWriteBack(USE_ENUM_CASE_INSENSITIVE, this::setUseEnumCaseInsensitive);
        convertPropertyToTypeAndWriteBack(CodegenConstants.MAX_ATTEMPTS_FOR_RETRY, Integer::parseInt, this::setMaxAttemptsForRetry);
        convertPropertyToTypeAndWriteBack(CodegenConstants.WAIT_TIME_OF_THREAD, Long::parseLong, this::setWaitTimeMillis);

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
        final String apiFolder = (sourceFolder + '/' + apiPackage).replace(".", "/");
        final String modelsFolder = (sourceFolder + File.separator + modelPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
        authFolder = (sourceFolder + '/' + invokerPackage + ".auth").replace(".", "/");

        convertPropertyToStringAndWriteBack(CodegenConstants.SERIALIZATION_LIBRARY, this::setSerializationLibrary);

        //TODO: add auto-generated doc to feign
        if (FEIGN.equals(getLibrary())) {
            modelDocTemplateFiles.remove("model_doc.mustache");
            apiDocTemplateFiles.remove("api_doc.mustache");
            //Templates to decode response headers
            // TODO remove "file" from reserved word list as feign client doesn't support using `baseName`
            // as the parameter name yet
            reservedWords.remove("file");
        }
    }

    @Override
    public String apiFilename(String templateName, String tag) {
        if (VERTX.equals(getLibrary())) {
            String suffix = apiTemplateFiles().get(templateName);
            String subFolder = "";
            if (templateName.startsWith("rx")) {
                subFolder = "/rxjava";
            }
            return apiFileFolder() + subFolder + '/' + toApiFilename(tag) + suffix;
        } else {
            return super.apiFilename(templateName, tag);
        }
    }

    /**
     * Prioritizes consumes mime-type list by moving json-vendor and json mime-types up front, but
     * otherwise preserves original consumes definition order.
     * [application/vnd...+json,... application/json, ..as is..]
     *
     * @param consumes consumes mime-type list
     * @return
     */
    static List<Map<String, String>> prioritizeContentTypes(List<Map<String, String>> consumes) {
        if (consumes.size() <= 1)
            return consumes;

        List<Map<String, String>> prioritizedContentTypes = new ArrayList<>(consumes.size());

        List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<>(consumes.size());
        List<Map<String, String>> jsonMimeTypes = new ArrayList<>(consumes.size());

        for (Map<String, String> consume : consumes) {
            if (isJsonVendorMimeType(consume.get(MEDIA_TYPE))) {
                jsonVendorMimeTypes.add(consume);
            } else if (isJsonMimeType(consume.get(MEDIA_TYPE))) {
                jsonMimeTypes.add(consume);
            } else
                prioritizedContentTypes.add(consume);
        }

        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);
        return prioritizedContentTypes;
    }

    private static boolean isMultipartType(List<Map<String, String>> consumes) {
        Map<String, String> firstType = consumes.get(0);
        if (firstType != null) {
            if ("multipart/form-data".equals(firstType.get(MEDIA_TYPE))) {
                return true;
            }
        }
        return false;
    }

    public boolean getUseOneOfDiscriminatorLookup() {
        return this.useOneOfDiscriminatorLookup;
    }

    private boolean getUseSingleRequestParameter() {
        return useSingleRequestParameter;
    }

    public void setUseRxJava(boolean useRxJava) {
        this.useRxJava = useRxJava;
        doNotUseRx = false;
    }

    public void setUseRxJava2(boolean useRxJava2) {
        this.useRxJava2 = useRxJava2;
        doNotUseRx = false;
    }

    public void setUseRxJava3(boolean useRxJava3) {
        this.useRxJava3 = useRxJava3;
        doNotUseRx = false;
    }

    public void setCaseInsensitiveResponseHeaders(final Boolean caseInsensitiveResponseHeaders) {
        this.caseInsensitiveResponseHeaders = caseInsensitiveResponseHeaders;
    }

    public void setSerializationLibrary(String serializationLibrary) {
        if (SERIALIZATION_LIBRARY_JACKSON.equalsIgnoreCase(serializationLibrary)) {
            this.serializationLibrary = SERIALIZATION_LIBRARY_JACKSON;
            this.jackson = true;
        } else if (SERIALIZATION_LIBRARY_GSON.equalsIgnoreCase(serializationLibrary)) {
            this.serializationLibrary = SERIALIZATION_LIBRARY_GSON;
            this.jackson = false;
        } else if (SERIALIZATION_LIBRARY_JSONB.equalsIgnoreCase(serializationLibrary)) {
            this.serializationLibrary = SERIALIZATION_LIBRARY_JSONB;
            this.jackson = false;
        } else {
            throw new IllegalArgumentException("Unexpected serializationLibrary value: " + serializationLibrary);
        }
    }

    public void forceSerializationLibrary(String serializationLibrary) {
        if ((this.serializationLibrary != null) && !this.serializationLibrary.equalsIgnoreCase(serializationLibrary)) {
            LOGGER.warn(
                    "The configured serializationLibrary '{}', is not supported by the library: '{}', switching back to: {}",
                    this.serializationLibrary, getLibrary(), serializationLibrary);
        }
        setSerializationLibrary(serializationLibrary);
    }

    @Override
    public String toApiVarName(String name) {
        String apiVarName = super.toApiVarName(name);
        if (reservedWords.contains(apiVarName)) {
            apiVarName = escapeReservedWord(apiVarName);
        }
        return apiVarName;
    }

    @Override
    public void addImportsToOneOfInterface(List<Map<String, String>> imports) {
        for (String i : Arrays.asList("JsonSubTypes", "JsonTypeInfo", "JsonIgnoreProperties")) {
            Map<String, String> oneImport = new HashMap<>();
            oneImport.put("import", importMapping.get(i));
            if (!imports.contains(oneImport)) {
                imports.add(oneImport);
            }
        }
    }
}
