package io.quarkus.kotlin.deployment;

import static io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem.builder;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassFinalFieldsWritablePredicateBuildItem;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class KotlinProcessor {

    private static final String KOTLIN_JACKSON_MODULE = "com.fasterxml.jackson.module.kotlin.KotlinModule";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KOTLIN);
    }

    /*
     * Register the Kotlin Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerKotlinJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        try {
            Class.forName(KOTLIN_JACKSON_MODULE, false, Thread.currentThread().getContextClassLoader());
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(KOTLIN_JACKSON_MODULE));
        } catch (Exception ignored) {
        }
    }

    /**
     * Kotlin data classes that have multiple constructors need to have their final fields writable,
     * otherwise creating a instance of them with default values, fails in native mode
     */
    @BuildStep
    ReflectiveClassFinalFieldsWritablePredicateBuildItem dataClassPredicate() {
        return new ReflectiveClassFinalFieldsWritablePredicateBuildItem(new IsDataClassWithDefaultValuesPredicate());
    }

    /*
     * Register the Kotlin reflection types if they are present.
     */
    @BuildStep
    void registerKotlinReflection(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeResourcePatterns) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false,
                "kotlin.reflect.jvm.internal.ReflectionFactoryImpl"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, "kotlin.KotlinVersion"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, false, "kotlin.KotlinVersion[]"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, false, "kotlin.KotlinVersion$Companion"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, false, "kotlin.KotlinVersion$Companion[]"));

        nativeResourcePatterns.produce(builder().includePatterns(
                "META-INF/.*.kotlin_module$",
                "META-INF/services/kotlin.reflect.*",
                ".*.kotlin_builtins")
                .build());
    }
}
