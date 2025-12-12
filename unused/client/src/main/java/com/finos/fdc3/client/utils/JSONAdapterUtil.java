/**
 * Copyright 2023 Wellington Management Company LLP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.finos.fdc3.client.utils;

import com.sun.codemodel.JCodeModel;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class JSONAdapterUtil
{

public static void main(String[] arg) {
    try {
        convertJsonToJavaClass(new File("C:\\finos\\json-schema\\Sample-Json.json").toURI().toURL(),
            new File("C:\\finos\\json-schema"), "com.finos.fdc3.java.api",
            "Sample");
    }catch(Throwable t) {
        t.printStackTrace();
    }
}

public static void  convertJsonToJavaClass(URL inputJsonUrl, File outputJavaClassDirectory, String packageName, String javaClassName)
    throws IOException
{
    JCodeModel jcodeModel = new JCodeModel();

    GenerationConfig config = new DefaultGenerationConfig() {
        @Override
        public boolean isGenerateBuilders() {
            return true;
        }

        @Override
        public SourceType getSourceType() {
            return SourceType.JSON;
        }
    };

    SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
    mapper.generate(jcodeModel, javaClassName, packageName, inputJsonUrl);

    jcodeModel.build(outputJavaClassDirectory);
}
}
