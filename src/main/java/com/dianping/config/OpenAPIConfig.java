package com.dianping.config;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenAPIConfig implements ApplicationRunner {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info() // 基本信息配置
                        .title("Dianping-OpenAPI文档") // 标题
                        .version("1.0.0") // 版本
                        // 设置 OpenAPI 文档的联系信息，包括联系人姓名，邮箱
                        .contact(new Contact().name("roy").email("1158232144@qq.com"))
                );
    }

    // 配置全局 Header 参数
    @Bean
    public OpenApiCustomiser customerGlobalHeaderOpenApiCustomiser() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            Parameter tokenHeader = new Parameter()
                    .in(ParameterIn.HEADER.toString())
                    .schema(new StringSchema())
                    .name("authorization") // header参数名
                    .description("用户登录令牌")
                    .required(false); // 可以设置true为必填
            operation.addParametersItem(tokenHeader);
        }));
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Dianping API Document URL: http://127.0.0.1:{}{}/doc.html", serverPort, contextPath);
    }
}
