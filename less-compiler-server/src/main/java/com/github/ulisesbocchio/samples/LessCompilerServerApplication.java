package com.github.ulisesbocchio.samples;

import com.github.ulisesbocchio.samples.resource.JarResourceLoader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.Sandbox;
import io.apigee.trireme.core.ScriptStatus;

@SpringBootApplication
@Slf4j
public class LessCompilerServerApplication extends WebMvcConfigurerAdapter implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${css.root.dir}")
    private String cssRootDir;

    @Value("${cache.css}")
    private Boolean cacheCss;

    public static final String WORK_DIR = Paths.get(".").normalize().toAbsolutePath().toString() + File.separator + ".work";

    public static void main(String[] args) {
        new SpringApplicationBuilder()
            .sources(LessCompilerServerApplication.class)
            .resourceLoader(new JarResourceLoader(WORK_DIR))
            .build()
            .run(args);
    }

    @Override
    @SneakyThrows
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Initializing Work Directory at: {}", WORK_DIR);
        Resource nodeModules = resourceLoader.getResource("classpath:/node_modules");
        nodeModules.getFile();
        FileUtils.forceDeleteOnExit(new File(WORK_DIR));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations(cssRootDir)
            .resourceChain(cacheCss)
            .addResolver(lessResourceResolver())
            .addResolver(new PathResourceResolver())
            .addTransformer(lessCompilerTransformer());

    }

    @Bean
    public ResourceResolver lessResourceResolver() {
        return new LessResourceResolver();
    }

    public static class LessResourceResolver extends AbstractResourceResolver {

        @Override
        protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            if(requestPath.endsWith(".css")) {
                String lessPath = requestPath.substring(0, requestPath.lastIndexOf(".css")) + ".less";
                return chain.resolveResource(request, lessPath, locations);
            }
            return null;
        }

        @Override
        protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            return chain.resolveUrlPath(resourceUrlPath, locations);
        }
    }

    @Bean
    public ResourceTransformer lessCompilerTransformer() {
        return new LessCompilerResourceTransformer();
    }

    public static class LessCompilerResourceTransformer implements ResourceTransformer, InitializingBean {

        @Autowired
        ResourceLoader globalResourceLoader;
        @Value("${less.compiler.script}")
        private String lessScript;
        NodeEnvironment env;
        private File lessScriptFile;

        @Override
        @SneakyThrows
        public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
            File resourceFile = resource.getFile();
            String lessString = FileUtils.readFileToString(resourceFile);
            NodeScript script = env.createScript("less-compiler.js", lessScriptFile, new String[]{lessString});
            File cssFile = new File(resourceFile.getParentFile(), resourceFile.getName().substring(0, resourceFile.getName().lastIndexOf(".less")) + ".css");
            script.getSandbox().setStdout(FileUtils.openOutputStream(cssFile));
            ScriptStatus status = script.execute().get();
            Resource transform = null;
            if(status.isOk()) {
                transform = new FileSystemResource(cssFile);
            }
            return transform;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            env = new NodeEnvironment();
            env.setSandbox(new Sandbox());
            lessScriptFile = globalResourceLoader.getResource(lessScript).getFile();
        }
    }
}
