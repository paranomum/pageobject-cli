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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class CliOption {
    @Getter private final String opt;
    @Getter @Setter
    private String description;
    private String defaultValue;
    @Getter private String optValue;

    public CliOption(String opt, String description) {
        this.opt = opt;
        this.description = description;
    }

    public String getDefault() {
        return defaultValue;
    }

    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public CliOption defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @JsonIgnore
    public String getOptionHelp() {
        StringBuilder sb = new StringBuilder(description);
        if(defaultValue != null) {
            sb.append(" (Default: ").append(defaultValue).append(")");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CliOption cliOption = (CliOption) o;
        return Objects.equals(opt, cliOption.opt) &&
                Objects.equals(description, cliOption.description) &&
                Objects.equals(defaultValue, cliOption.defaultValue) &&
                Objects.equals(optValue, cliOption.optValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, description, defaultValue, optValue);
    }
}
