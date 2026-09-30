/*
 * Copyright (C) 2021-2024 Michael Clarke
 * Copyright (C) 2026 Haembina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.haembina.branchanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.feature.SonarQubeFeature;
import org.sonar.server.newcodeperiod.ws.SetAction;
import org.sonar.server.newcodeperiod.ws.UnsetAction;
import org.sonar.server.user.UserSession;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;

class CommunityBranchAgentTest {

    @Test
    void shouldThrowErrorIfAgentArgsNotValid() {
        Instrumentation instrumentation = mock();
        assertThatThrownBy(() -> CommunityBranchAgent.premain("badarg", instrumentation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid/missing agent argument");
    }

    @Test
    void shouldRedefinePluginBootstrapIsAvailableForWebLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        CustomClassloader classLoader = new CustomClassloader();
        Instrumentation instrumentation = mock();

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation, times(4)).addTransformer(classFileTransformerArgumentCaptor.capture());


        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(0).transform(getClass().getClassLoader(), CommunityBranchPluginBootstrap.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            Object instance =  classLoader.loadClass(CommunityBranchPluginBootstrap.class.getName(), result).getConstructor().newInstance();

            Method isAvailable = instance.getClass().getDeclaredMethod("isAvailable");
            isAvailable.setAccessible(true);
            boolean available = (boolean) isAvailable.invoke(instance);
            assertThat(available).isTrue();
        }
    }

    @Test
    void shouldRedefineMultipleAlmFeatureClassForWebLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        CustomClassloader classLoader = new CustomClassloader();
        Instrumentation instrumentation = mock();

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation, times(4)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(1).transform(classLoader, MultipleAlmFeature.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);
            Class<? extends SonarQubeFeature> redefined = classLoader.loadClass(MultipleAlmFeature.class.getName(), result).asSubclass(SonarQubeFeature.class);

            SonarRuntime sonarRuntime = mock();

            SonarQubeFeature multipleAlmFeatureProvider = redefined.getConstructor(SonarRuntime.class).newInstance(sonarRuntime);
            assertThat(multipleAlmFeatureProvider.isAvailable()).isTrue();
        }
    }


    @Test
    void shouldRedefineSetActionClassForWebLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        CustomClassloader classLoader = new CustomClassloader();
        Instrumentation instrumentation = mock();
        DocumentationLinkGenerator documentationLinkGenerator = mock();

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(SetAction.class);
        verify(instrumentation, times(4)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = SetAction.class.getResourceAsStream(SetAction.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(2).transform(classLoader, SetAction.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            Class<?> setActionClass = classLoader.loadClass(SetAction.class.getName(), result);

            DbClient dbClient = mock();
            ComponentFinder componentFinder = mock();
            UserSession userSession = mock();
            PlatformEditionProvider platformEditionProvider = mock();
            NewCodePeriodDao newCodePeriodDao = mock();

            Object setAction = setActionClass.getConstructor(DbClient.class, UserSession.class, ComponentFinder.class, PlatformEditionProvider.class, NewCodePeriodDao.class, DocumentationLinkGenerator.class)
                    .newInstance(dbClient, userSession, componentFinder, platformEditionProvider, newCodePeriodDao, documentationLinkGenerator);

            Field editionProviderField = setActionClass.getDeclaredField("editionProvider");
            editionProviderField.setAccessible(true);
            assertThat(((EditionProvider) editionProviderField.get(setAction)).get()).isEqualTo(Optional.of(EditionProvider.Edition.DEVELOPER));
        }
    }

    @Test
    void shouldRedefinesUnsetActionClassForWebLaunch() throws IOException, UnmodifiableClassException, IllegalClassFormatException, ReflectiveOperationException {
        CustomClassloader classLoader = new CustomClassloader();

        Instrumentation instrumentation = mock();
        CommunityBranchAgent.premain("web", instrumentation);
        DocumentationLinkGenerator documentationLinkGenerator = mock();

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(UnsetAction.class);
        verify(instrumentation, times(4)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = SetAction.class.getResourceAsStream(SetAction.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(3).transform(classLoader, UnsetAction.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            Class<?> unsetActionClass = classLoader.loadClass(UnsetAction.class.getName(), result);
            DbClient dbClient = mock();
            ComponentFinder componentFinder = mock();
            UserSession userSession = mock();
            PlatformEditionProvider platformEditionProvider = mock();
            NewCodePeriodDao newCodePeriodDao = mock();

            Object setAction = unsetActionClass.getConstructor(DbClient.class, UserSession.class, ComponentFinder.class, PlatformEditionProvider.class, NewCodePeriodDao.class, DocumentationLinkGenerator.class)
                    .newInstance(dbClient, userSession, componentFinder, platformEditionProvider, newCodePeriodDao, documentationLinkGenerator);

            Field editionProviderField = unsetActionClass.getDeclaredField("editionProvider");
            editionProviderField.setAccessible(true);
            assertThat(((EditionProvider) editionProviderField.get(setAction)).get()).isEqualTo(Optional.of(EditionProvider.Edition.DEVELOPER));
        }
    }

    @Test
    void shouldSkipNonTargetClasForWebLaunch() throws UnmodifiableClassException, ClassNotFoundException, IllegalClassFormatException {
        Instrumentation instrumentation = mock();

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation, times(4)).addTransformer(classFileTransformerArgumentCaptor.capture());

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};
        byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), "com/github/com/haembina/Dummy", getClass(), getClass().getProtectionDomain(), input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldSkipNonTargetClassForCeLunch() throws UnmodifiableClassException, ClassNotFoundException, IllegalClassFormatException {
        Instrumentation instrumentation = mock();

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};
        byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), "com/github/com/haembina/Dummy", getClass(), getClass().getProtectionDomain(), input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldKeepDefaultClassWhenSonarQubeClassLacksWhatTheAgentRewrites() throws Exception {
        String target = MultipleAlmFeature.class.getName();
        ClassPool pool = ClassPool.getDefault();
        CtClass cached = pool.getOrNull(target);
        if (cached != null) {
            cached.detach();
        }
        ClassPath withoutIsAvailable = new ByteArrayClassPath(target, new ClassPool(true).makeClass(target).toBytecode());
        pool.insertClassPath(withoutIsAvailable);
        Logger agentLogger = (Logger) LoggerFactory.getLogger(CommunityBranchAgent.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        agentLogger.addAppender(logged);
        try {
            Instrumentation instrumentation = mock();
            CommunityBranchAgent.premain("ce", instrumentation);
            ArgumentCaptor<ClassFileTransformer> transformers = ArgumentCaptor.captor();
            verify(instrumentation, times(3)).addTransformer(transformers.capture());

            byte[] input = new byte[]{1, 2, 3};
            byte[] result = transformers.getAllValues().get(2).transform(getClass().getClassLoader(), target.replace('.', '/'), getClass(), getClass().getProtectionDomain(), input);

            assertThat(result).isSameAs(input);
            assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).startsWith("Could not transform class " + target));
        } finally {
            agentLogger.detachAppender(logged);
            pool.removeClassPath(withoutIsAvailable);
            Optional.ofNullable(pool.getOrNull(target)).ifPresent(CtClass::detach);
        }
    }

    @Test
    void shouldRedefineTargetClassesForCeLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        Instrumentation instrumentation = mock();

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.captor();
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(0).transform(getClass().getClassLoader(), CommunityBranchPluginBootstrap.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            Object instance =  classLoader.loadClass(CommunityBranchPluginBootstrap.class.getName(), result).getConstructor().newInstance();

            Method isAvailable = instance.getClass().getDeclaredMethod("isAvailable");
            isAvailable.setAccessible(true);
            boolean available = (boolean) isAvailable.invoke(instance);
            assertThat(available).isTrue();
        }

        try (InputStream inputStream = PlatformEditionProvider.class.getResourceAsStream(PlatformEditionProvider.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(1).transform(getClass().getClassLoader(), PlatformEditionProvider.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            Class<? extends EditionProvider> redefined = classLoader.loadClass(PlatformEditionProvider.class.getName(), result).asSubclass(EditionProvider.class);
            assertThat(redefined.getConstructor().newInstance().get()).contains(EditionProvider.Edition.DEVELOPER);
        }

        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(2).transform(getClass().getClassLoader(), MultipleAlmFeature.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            SonarRuntime sonarRuntime = mock();

            Class<? extends SonarQubeFeature> redefined = classLoader.loadClass(MultipleAlmFeature.class.getName(), result).asSubclass(SonarQubeFeature.class);
            SonarQubeFeature multipleAlmFeatureProvider = redefined.getConstructor(SonarRuntime.class).newInstance(sonarRuntime);
            assertThat(multipleAlmFeatureProvider.isAvailable()).isTrue();
        }
    }

    private static class CustomClassloader extends ClassLoader {

        Class<?> loadClass(String name, byte[] value) {
            return defineClass(name, value, 0, value.length);
        }

    }

}
