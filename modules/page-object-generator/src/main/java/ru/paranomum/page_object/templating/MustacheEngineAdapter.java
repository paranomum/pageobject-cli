/*
 * Copyright 2019 OpenAPI-Generator Contributors (https://openapi-generator.tech)
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

package ru.paranomum.page_object.templating;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.paranomum.page_object.api.TemplatingEngineAdapter;
import ru.paranomum.page_object.api.TemplatingExecutor;
import ru.paranomum.page_object.model.ModelCodegen;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;


public class MustacheEngineAdapter implements TemplatingEngineAdapter {

    private final Logger LOGGER = LoggerFactory.getLogger(TemplatingEngineAdapter.class);

    /**
     * Provides an identifier used to load the adapter. This could be a name, uuid, or any other string.
     *
     * @return A string identifier.
     */
    @Override
    public String getIdentifier() {
        return "mustache";
    }

    private final String[] extensions = {"mustache"};
    @Getter @Setter
    Mustache.Compiler compiler = Mustache.compiler();

    /**
     * Compiles a template into a string
     *
     * @param executor    From where we can fetch the templates content (e.g. an instance of DefaultGenerator)
     * @param bundle       The map of values to pass to the template
     * @param templateFile The name of the template (e.g. model.mustache )
     * @return the processed template result
     * @throws IOException an error occurred in the template processing
     */
    @Override
    public String compileTemplate(TemplatingExecutor executor, ModelCodegen bundle, String templateFile) throws IOException {
        Template tmpl = compiler
                .withLoader(name -> findTemplate(executor, name))
                .defaultValue("")
                .compile(executor.getFullTemplateContents(templateFile));
        StringWriter out = new StringWriter();

        // the value of bundle[MUSTACHE_PARENT_CONTEXT] is used a parent content in mustache.
        // See description in https://mustache.github.io/mustache.5.html#Variables
        // See DefaultCodegen.processOpts() and DefaultCodegen.useCodegenAsMustacheParentContext
        tmpl.execute(bundle, out);
        return out.toString();
    }

    @SuppressWarnings("java:S108") // catch-all is expected, and is later thrown
    public Reader findTemplate(TemplatingExecutor generator, String name) {
        for (String extension : extensions) {
            final String templateName = name + "." + extension;
            try {
                return new StringReader(generator.getFullTemplateContents(templateName));
            } catch (Exception exception) {
                LOGGER.error("Failed to read full template {}, {}", templateName, exception.getMessage());
            }
        }

        throw new TemplateNotFoundException(name);
    }

    @Override
    public String[] getFileExtensions() {
        return extensions;
    }
}
