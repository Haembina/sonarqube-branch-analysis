/*
 * Copyright (C) 2022-2025 Michael Clarke
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
package com.haembina.branchanalysis.almclient.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import org.assertj.core.api.Condition;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.connector.GitHubConnector;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.InvalidConfigurationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

class GithubClientFactoryTest {

    private static final String PRIVATE_KEY = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEpAIBAAKCAQEAoA3dIxtKx7qpUAhfoJeYAYZzVdRvOW9s3GtxJVuBI6GOob2V
            J7ia1i/h3za591NggqsW+QjjknmhPmqlENCKsyU5FQMkM8eV04xwHxFwjIk6SACB
            JigRSgw95b+EKN2p0caTeOiL/+LQ3pauAf+52iFqeWAKvgjxx3+rlSXzgWCBb9uk
            1XcDoeRzeXkXvOj9ZQ8wKn8FAounmyjKaIy+u7bJCgVWHRfXwjFWsIWZJ9UY1eAR
            ADoPHIHj4WrNmAvE/kOBXNSqmkY+Yu170tQA5iVeHgezrlkM08Qj4+4pqYOPHWZ2
            0TNsRn82fuGSyejLhKATbqiOxierHzECIGGUQQIDAQABAoIBADrJjNFRu3BL89dl
            E/aw55Cb2S4L1oSClDoLrqXZi7/SHcjzkN7jk9+a+7wYZkrdEYQ9IjV7WdcZnKuH
            0TQxXNh7EhHRMxFfu/zVRvNqXOwJlWIP6V/h9KO9hlimNP0rma3m4ZDV3WIx5aT0
            NFqgmptvjaOiLp/pOiEcGCIyq9N3UZgGTeJTRdvNiEJDiFPJaJvkJuspbCAIrqFT
            hzua3aJyi47GD0nebfAfJyC8Cq9VgKzvVYoKejU51cw9+ikCVSZZiOCVWOkZTPjU
            LCuhIAtPppG5s5DxvXtHdWUlpRKRbI1tIVaTk05kBm1SOH0a3H2GiEv4jQvsyZMz
            UQQQe78CgYEAw0Ixmsgy8meCKMxH8oWO/ceyZ4uVQCGy4C/q2JmGHL0DkHF6QrZi
            DuUrqDoI88lhBL/0JyGlRTy3Ddy2hZdjUdvd6HEyLJbQvy6BmUI5AGXU0+ICHfZ2
            YDP+3zY1m4SctM1duJ6XcPPefDapbl4WMxVLs1DTHCAK4dvI9HW9WYcCgYEA0dgY
            oihENXtew/s3vlBk+ZN+5xjnRwThUouxZFnzHBeCdE1237y6lUMK6UfjTmstGxWI
            g5KoawjZJScH8UDO7OiGTuI93TUzrR1Xi63dTynfE5z5BvKKGKjM4Y3aTvAsbk4q
            b+U65srTgvtZG4ASv/4vCxGL8wTJbd8oLWhu9fcCgYEArvUKA5ntZJzw2OOqeBnK
            dYVRS0ycMHnBkPX+pZRywh9vKSc1GL/Zf2VDSBqwWNkR0LK677FLKI3trEMfXPa4
            bOnondWH0sJUS2o9f/kBoGSeXji+EuD7UtpkPteRE0exLqRxnPKl2fT6XyyPhrBR
            jfY//W2nrCTd+2D3YGx7fNMCgYEAyGK6i0c2c3f/M9lXDvcIpcfyvE5stMX1QXVC
            jdjTrfTJT7SVmuxHpLej2MccSktQhHeYqERJbgTCD5dpHznLIDKf5v5nIzFlyp+l
            dS4vkyQh8UHKEJdVxlyTYaSrXww88YzVO4tEJxZyyrapDfjMbukVFVXJNeVRUQlz
            /YCnzVsCgYBipCwzPZzVlK7WdH8jsPC19ZEFd7UIbXM8kqiWhhQp4zL7kJ81Q+zf
            xmiRGsJl9uUjypDDOp1qLejoH5EObg3MlNoOq6aqSu1ZaY0rOnALlJwmZS10G2vC
            4eO4MsTrF0fH8PnJLK/nbrGX2Ll+PyY5Zn8rfKeTVvmwjSTMFPSORw==
            -----END RSA PRIVATE KEY-----
            """;

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final Settings settings = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(123456789123L), ZoneId.of("UTC"));
    private final GitHubBuilder githubBuilder = mock();
    private final Encryption encryption = mock();

    @BeforeEach
    void setUp() {
        when(almSettingDto.getUrl()).thenReturn("url");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("privateKey");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("owner/repo");
        when(almSettingDto.getAppId()).thenReturn("appId");
        when(settings.getEncryption()).thenReturn(encryption);
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn(PRIVATE_KEY);
    }

    @Test
    void shouldThrowExceptionIfUrlMissingInAlmSettings() {
        when(almSettingDto.getUrl()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No URL has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldRefuseMissingUrlWithDefaultGithubBuilder() {
        when(almSettingDto.getUrl()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No URL has been set for Github connections");
    }

    @Test
    void shouldThrowExceptionIsPrivateKeyMissingInAlmSettings() {
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No private key has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfRepoMissingInAlmSettings() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No repository name has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfAppIdMissingInAlmSettings() {
        when(almSettingDto.getAppId()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No App ID has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfGithubCallFails() throws IOException {
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        GitHub github = mock();
        when(github.getApp()).thenThrow(new IOException("dummy"));
        when(githubBuilder.build()).thenReturn(github);
        when(githubBuilder.withEndpoint(any())).thenReturn(githubBuilder);
        when(githubBuilder.withJwtToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.withConnector(any(GitHubConnector.class))).thenReturn(githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Github client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfRepoNameDoesNotContainSlash() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
            .usingRecursiveComparison()
            .isEqualTo(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Repository name must be in the format owner/repo"));
    }

    @Test
    void shouldThrowExceptionIfRsaKeyIsNotParseable() {
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("invalid");
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
            .usingRecursiveComparison()
            .isEqualTo(new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Private key could not be parsed"));
    }

    @Test
    void shouldReturnValidGithubTokenWhenCalledWithCorrectParameters() throws IOException {
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        GHAppCreateTokenBuilder tokenBuilder = mock();
        GitHub github = stubAuthentication(tokenBuilder);

        GithubConnection connection = underTest.connect(almSettingDto, projectAlmSettingDto);
        assertThat(connection.client()).isSameAs(github);
        assertThat(connection.appSlug()).isEqualTo("app-slug");
        assertThat(connection.graphql()).isNotNull();
        verify(tokenBuilder).create();
        verify(githubBuilder, times(2)).withEndpoint("url");
        ArgumentCaptor<String> jwtCaptor = ArgumentCaptor.captor();
        verify(githubBuilder).withJwtToken(jwtCaptor.capture());
        Jws<Claims> jwt = Jwts.parser().verifyWith(publicKey()).build().parseSignedClaims(jwtCaptor.getValue());
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(jwt.getPayload().getIssuer()).isEqualTo("appId");
        assertThat(jwt.getPayload().getIssuedAt()).isEqualTo(Date.from(Instant.ofEpochSecond(123456789113L)));
        assertThat(jwt.getPayload().getExpiration()).isEqualTo(Date.from(Instant.ofEpochSecond(123456789233L)));
        verify(githubBuilder, times(2)).withConnector(any(GitHubConnector.class));
        verify(githubBuilder).withAppInstallationToken("token");
        verify(githubBuilder, times(2)).build();
        verifyNoMoreInteractions(githubBuilder);

    }

    @Test
    void shouldCreateTheClientOfAnAuthenticatedConnection() throws IOException {
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        GHAppCreateTokenBuilder tokenBuilder = mock();
        GitHub github = stubAuthentication(tokenBuilder);

        assertThat(underTest.createClient(almSettingDto, projectAlmSettingDto)).isSameAs(github);
    }

    private static PublicKey publicKey() throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(PRIVATE_KEY))) {
            return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) pemParser.readObject()).getPublic();
        }
    }

    private GitHub stubAuthentication(GHAppCreateTokenBuilder tokenBuilder) throws IOException {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("alm/slug");

        GitHub github = mock();
        when(githubBuilder.withEndpoint(any())).thenReturn(githubBuilder);
        when(githubBuilder.withJwtToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.withConnector(any(GitHubConnector.class))).thenReturn(githubBuilder);
        when(githubBuilder.withAppInstallationToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.build()).thenReturn(github);
        GHApp ghApp = mock();
        when(github.getApp()).thenReturn(ghApp);
        when(ghApp.getSlug()).thenReturn("app-slug");
        GHAppInstallation ghAppInstallation = mock();
        when(ghApp.getInstallationByRepository(any(), any())).thenReturn(ghAppInstallation);
        when(ghAppInstallation.createToken()).thenReturn(tokenBuilder);
        GHAppInstallationToken ghAppInstallationToken = mock();
        when(ghAppInstallationToken.getToken()).thenReturn("token");
        when(tokenBuilder.create()).thenReturn(ghAppInstallationToken);
        when(tokenBuilder.repositories(any())).thenReturn(tokenBuilder);
        return github;
    }
}
