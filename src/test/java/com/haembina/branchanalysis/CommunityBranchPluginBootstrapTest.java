/*
 * Copyright (C) 2019-2025 Michael Clarke
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

import com.haembina.branchanalysis.classloader.ElevatedClassLoaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommunityBranchPluginBootstrapTest {

    private static final String AGENT_JAR = "haembina-branch-analysis-1.2.3.jar";

    @BeforeEach
    void setup() {
        MockPlugin.invokedContext = null;
    }

    @Test
    void shouldInvokeDefineOnSuccessfulLoad() {
        Plugin.Context context = mock();
        Configuration configuration = mock();
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, true, AGENT_JAR);
        testCase.define(context);

        assertThat(MockPlugin.invokedContext).isSameAs(context);
    }

    @Test
    void shouldNotInvokeDefineOnNonScannerScope() {
        Plugin.Context context = mock();
        Configuration configuration = mock();
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, true, AGENT_JAR);
        testCase.define(context);

        assertThat(MockPlugin.invokedContext).isNull();
    }


    @Test
    void shouldThrowExceptionIfIncorrectClassReturned() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return CommunityBranchPluginBootstrapTest.class;
            }
        };

        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);


        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, true, AGENT_JAR);

        assertThatThrownBy(() -> testCase.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected loaded class to be instance of 'org.sonar.api.Plugin' but was 'com.haembina.branchanalysis.CommunityBranchPluginBootstrapTest'")
            .hasNoCause();
    }

    @Test
    void shouldThrowExceptionOnReflectionFailure() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Whoops");
            }
        };
        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);


        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, true, AGENT_JAR);

        assertThatThrownBy(() -> testCase.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not create CommunityBranchPlugin instance")
            .hasCause(new ClassNotFoundException("Whoops"));
    }

    @CsvSource({"SERVER, web, WEB", "COMPUTE_ENGINE, ce, CE"})
    @ParameterizedTest
    void shouldNameTheMissingJavaagentLineWhenPluginNotMarkedAsAvailableInScope(SonarQubeSide sonarQubeSide,
                                                                                String process, String variable) {
        Plugin.Context context = mock();
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(sonarQubeSide);
        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();

        CommunityBranchPluginBootstrap underTest =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, false, AGENT_JAR);

        assertThatThrownBy(() -> underTest.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Branch analysis needs this plugin's Java agent in the " + process + " process, and SonarQube "
                    + "was started without it. Add sonar." + process + ".javaAdditionalOpts=-javaagent:"
                    + "./extensions/plugins/" + AGENT_JAR + "=" + process + " to conf/sonar.properties, or set SONAR_"
                    + variable + "_JAVAADDITIONALOPTS to the same -javaagent value in Docker, then start SonarQube "
                    + "again.")
            .hasNoCause();

        verify(context).getRuntime();
        verify(sonarRuntime).getSonarQubeSide();
        verifyNoMoreInteractions(context);
        verifyNoMoreInteractions(sonarRuntime);
        verifyNoInteractions(elevatedClassLoaderFactory);
    }


    @EnumSource(value = SonarQubeSide.class, mode = EnumSource.Mode.EXCLUDE, names ="SCANNER")
    @ParameterizedTest
    void shouldNotThrowExceptionWhenPluginMarkedAsAvailableInScope(SonarQubeSide sonarQubeSide) {
        Plugin.Context context = mock();
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(sonarQubeSide);
        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();

        CommunityBranchPluginBootstrap underTest = new CommunityBranchPluginBootstrap(elevatedClassLoaderFactory, true, AGENT_JAR);

        underTest.define(context);

        verify(context).getRuntime();
        verify(sonarRuntime).getSonarQubeSide();
        verifyNoMoreInteractions(context);
        verifyNoMoreInteractions(sonarRuntime);
        verifyNoInteractions(elevatedClassLoaderFactory);
    }

    @Test
    void shouldRefuseServerStartWhenLoadedWithoutTheAgent() {
        Plugin.Context context = mock();
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        CommunityBranchPluginBootstrap underTest = new CommunityBranchPluginBootstrap();

        assertThatThrownBy(() -> underTest.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Branch analysis needs this plugin's Java agent in the web process");
    }

    @Test
    void shouldNameTheJarAClassWasLoadedFrom() {
        assertThat(CommunityBranchPluginBootstrap.jarName(Test.class.getProtectionDomain()))
                .matches("junit-jupiter-api-.+\\.jar");
    }

    @Test
    void shouldNameAPlaceholderJarWhenTheClassCameFromNoJar() throws MalformedURLException {
        CodeSource directory = new CodeSource(URI.create("file:/opt/classes/").toURL(), (Certificate[]) null);

        assertThat(List.of(new ProtectionDomain(null, null),
                        new ProtectionDomain(new CodeSource(null, (Certificate[]) null), null),
                        new ProtectionDomain(directory, null)))
                .extracting(CommunityBranchPluginBootstrap::jarName)
                .containsOnly("haembina-branch-analysis-<version>.jar");
    }

    public static class MockPlugin implements Plugin {

        private static Context invokedContext;

        @Override
        public void define(Context context) {
            invokedContext = context;
        }
    }
}
