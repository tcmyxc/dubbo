/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.xds.bootstrap;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.AuthorityInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.CertificateProviderInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BootstrapInfo {
    private final ImmutableList<ServerInfo> servers;
    private final Node node;

    @Nullable
    private final ImmutableMap<String, CertificateProviderInfo> certProviders;

    @Nullable
    private final String serverListenerResourceNameTemplate;

    private final String clientDefaultListenerResourceNameTemplate;
    private final ImmutableMap<String, AuthorityInfo> authorities;

    private BootstrapInfo(Builder builder) {
        this.servers = ImmutableList.copyOf(builder.servers);
        this.node = builder.node;
        this.certProviders = builder.certProviders == null ? null : ImmutableMap.copyOf(builder.certProviders);
        this.serverListenerResourceNameTemplate = builder.serverListenerResourceNameTemplate;
        this.clientDefaultListenerResourceNameTemplate = builder.clientDefaultListenerResourceNameTemplate;
        this.authorities = builder.authorities == null ? null : ImmutableMap.copyOf(builder.authorities);
    }

    public ImmutableList<ServerInfo> getServers() {
        return servers;
    }

    public Node getNode() {
        return node;
    }

    @Nullable
    public ImmutableMap<String, CertificateProviderInfo> getCertProviders() {
        return certProviders;
    }

    @Nullable
    public String getServerListenerResourceNameTemplate() {
        return serverListenerResourceNameTemplate;
    }

    public String getClientDefaultListenerResourceNameTemplate() {
        return clientDefaultListenerResourceNameTemplate;
    }

    public ImmutableMap<String, AuthorityInfo> getAuthorities() {
        return authorities;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<ServerInfo> servers;
        private Node node;
        private Map<String, CertificateProviderInfo> certProviders;
        private String serverListenerResourceNameTemplate;
        private String clientDefaultListenerResourceNameTemplate;
        private Map<String, AuthorityInfo> authorities;

        public Builder servers(List<ServerInfo> servers) {
            this.servers = servers;
            return this;
        }

        public Builder node(Node node) {
            this.node = node;
            return this;
        }

        public Builder certProviders(@Nullable Map<String, CertificateProviderInfo> certProviders) {
            this.certProviders = certProviders;
            return this;
        }

        public Builder serverListenerResourceNameTemplate(@Nullable String serverListenerResourceNameTemplate) {
            this.serverListenerResourceNameTemplate = serverListenerResourceNameTemplate;
            return this;
        }

        public Builder clientDefaultListenerResourceNameTemplate(String clientDefaultListenerResourceNameTemplate) {
            this.clientDefaultListenerResourceNameTemplate = clientDefaultListenerResourceNameTemplate;
            return this;
        }

        public Builder authorities(Map<String, AuthorityInfo> authorities) {
            this.authorities = authorities;
            return this;
        }

        public BootstrapInfo build() {
            return new BootstrapInfo(this);
        }
    }

    @Override
    public String toString() {
        return "BootstrapInfo{" + "servers=" + servers + ", node=" + node + ", certProviders=" + certProviders
                + ", serverListenerResourceNameTemplate='" + serverListenerResourceNameTemplate + '\''
                + ", clientDefaultListenerResourceNameTemplate='" + clientDefaultListenerResourceNameTemplate + '\''
                + ", authorities=" + authorities + '}';
    }
}
