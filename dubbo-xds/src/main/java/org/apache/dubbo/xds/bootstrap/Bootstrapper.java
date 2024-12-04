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

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.XdsLogger;
import org.apache.dubbo.xds.XdsLogger.XdsLogLevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.grpc.Internal;
import io.grpc.InternalLogId;

import static com.google.common.base.Preconditions.checkArgument;

@Internal
public class Bootstrapper {

    public static final String XDSTP_SCHEME = "xdstp:";

    private static Bootstrapper INSTANCE = null;
    private static final String BOOTSTRAP_PATH_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP";
    private static final String BOOTSTRAP_CONFIG_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP_CONFIG";
    private static final String DEFAULT_BOOTSTRAP_PATH = "/bootstrap.json";
    public static final String CLIENT_FEATURE_DISABLE_OVERPROVISIONING = "envoy.lb.does_not_support_overprovisioning";
    public static final String CLIENT_FEATURE_RESOURCE_IN_SOTW = "xds.config.resource-in-sotw";
    private static final String SERVER_FEATURE_IGNORE_RESOURCE_DELETION = "ignore_resource_deletion";
    private static final String SERVER_FEATURE_XDS_V3 = "xds_v3";

    protected final XdsLogger logger;
    protected FileReader reader = LocalFileReader.INSTANCE;

    @VisibleForTesting
    public String bootstrapPathFromEnvVar = null;

    @VisibleForTesting
    public String bootstrapConfigFromEnvVar = System.getenv(BOOTSTRAP_CONFIG_SYS_ENV_VAR);

    public Bootstrapper() {
        logger = XdsLogger.withLogId(InternalLogId.allocate("bootstrapper", null));
    }

    public static Bootstrapper getInstance() {
        if (INSTANCE == null) {
            synchronized (Bootstrapper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Bootstrapper();
                }
            }
        }
        return INSTANCE;
    }

    public BootstrapInfo bootstrap() {
        String jsonContent;
        try {
            jsonContent = getJsonContent();
        } catch (IOException e) {
            throw new XdsInitializationException("Fail to read bootstrap file", e);
        }

        if (jsonContent == null) {
            // TODO:try loading from Dubbo control panel and user specified URL
            return null;
        }

        JsonNode jsonNode;
        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonNode = mapper.readTree(jsonContent);
        } catch (IOException e) {
            throw new XdsInitializationException("Failed to parse JSON", e);
        }
        logger.log(XdsLogLevel.DEBUG, "Bootstrap configuration:\n{0}", jsonNode);
        return buildBootstrapInfo(jsonNode);
    }

    private String getJsonContent() throws IOException, XdsInitializationException {
        String jsonContent;
        String filePath = null;

        // Get the path of the bootstrap config via environment variable and system property
        bootstrapPathFromEnvVar = System.getenv(BOOTSTRAP_PATH_SYS_ENV_VAR);
        if (bootstrapPathFromEnvVar == null) {
            bootstrapPathFromEnvVar = System.getProperty(BOOTSTRAP_PATH_SYS_ENV_VAR);
        }

        // Check environment variable and system property
        if (bootstrapPathFromEnvVar != null && Files.exists(Paths.get(bootstrapPathFromEnvVar))) {
            filePath = bootstrapPathFromEnvVar;
        } else if (Files.exists(Paths.get(DEFAULT_BOOTSTRAP_PATH))) {
            // Check the default path
            filePath = DEFAULT_BOOTSTRAP_PATH;
        }
        if (filePath != null) {
            logger.log(XdsLogLevel.INFO, "Reading bootstrap file from {0}", filePath);
            jsonContent = reader.readFile(filePath);
            logger.log(XdsLogLevel.INFO, "Reading bootstrap from " + filePath);
        } else {
            jsonContent = null;
        }

        return jsonContent;
    }

    private BootstrapInfo buildBootstrapInfo(JsonNode rawBootstrap) {
        checkArgument(!rawBootstrap.isEmpty(), "Bootstrap configuration cannot be empty");

        // parse server info
        JsonNode jsonServer = rawBootstrap.get("xds_servers").get(0);
        ServerInfo serverInfo = new ServerInfo(jsonServer.get("server_uri").asText(), null, false);

        // parse node info
        JsonNode jsonNode = rawBootstrap.get("node");
        JsonNode jsonMetadata = jsonNode.get("metadata");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("CLUSTER_ID", jsonMetadata.get("CLUSTER_ID").asText());
        metadata.put(
                "ENVOY_PROMETHEUS_PORT",
                jsonMetadata.get("ENVOY_PROMETHEUS_PORT").asText());
        metadata.put("ENVOY_STATUS_PORT", jsonMetadata.get("ENVOY_STATUS_PORT").asText());
        metadata.put("GENERATOR", jsonMetadata.get("GENERATOR").asText());
        metadata.put("NAMESPACE", jsonMetadata.get("NAMESPACE").asText());

        Node node = Node.newBuilder()
                .setId(jsonNode.get("id").asText())
                .setMetadata(metadata)
                .build();

        return BootstrapInfo.builder()
                .servers(Collections.singletonList(serverInfo))
                .node(node)
                .build();
    }

    public static class ServerInfo {
        private final String target;
        private final Object implSpecificConfig;
        private final boolean ignoreResourceDeletion;

        public ServerInfo(String target, Object implSpecificConfig, boolean ignoreResourceDeletion) {
            this.target = target;
            this.implSpecificConfig = implSpecificConfig;
            this.ignoreResourceDeletion = ignoreResourceDeletion;
        }

        public String getTarget() {
            return target;
        }

        public Object getImplSpecificConfig() {
            return implSpecificConfig;
        }

        public boolean isIgnoreResourceDeletion() {
            return ignoreResourceDeletion;
        }

        public ServerInfo create(String target, Object implSpecificConfig) {
            return new ServerInfo(target, implSpecificConfig, false);
        }

        public ServerInfo create(String target, Object implSpecificConfig, boolean ignoreResourceDeletion) {
            return new ServerInfo(target, implSpecificConfig, ignoreResourceDeletion);
        }

        @Override
        public String toString() {
            return "ServerInfo{" + "target='" + target + '\'' + ", implSpecificConfig=" + implSpecificConfig
                    + ", ignoreResourceDeletion=" + ignoreResourceDeletion + '}';
        }
    }

    @Internal
    public class CertificateProviderInfo {
        private final String pluginName;
        private final Map<String, ?> config;

        public CertificateProviderInfo(String pluginName, Map<String, ?> config) {
            this.pluginName = pluginName;
            this.config = Collections.unmodifiableMap(config);
        }

        public String getPluginName() {
            return pluginName;
        }

        public Map<String, ?> getConfig() {
            return config;
        }

        public CertificateProviderInfo create(String pluginName, Map<String, ?> config) {
            return new CertificateProviderInfo(pluginName, config);
        }

        @Override
        public String toString() {
            return "CertificateProviderInfo{" + "pluginName='" + pluginName + '\'' + ", config=" + config + '}';
        }
    }

    public class AuthorityInfo {
        private final String clientListenerResourceNameTemplate;
        private final ImmutableList<ServerInfo> xdsServers;

        public AuthorityInfo(String clientListenerResourceNameTemplate, List<ServerInfo> xdsServers) {
            checkArgument(!xdsServers.isEmpty(), "xdsServers must not be empty");
            this.clientListenerResourceNameTemplate = clientListenerResourceNameTemplate;
            this.xdsServers = ImmutableList.copyOf(xdsServers);
        }

        public String getClientListenerResourceNameTemplate() {
            return clientListenerResourceNameTemplate;
        }

        public ImmutableList<ServerInfo> getXdsServers() {
            return xdsServers;
        }

        public AuthorityInfo create(String clientListenerResourceNameTemplate, List<ServerInfo> xdsServers) {
            return new AuthorityInfo(clientListenerResourceNameTemplate, xdsServers);
        }

        @Override
        public String toString() {
            return "AuthorityInfo{" + "clientListenerResourceNameTemplate='" + clientListenerResourceNameTemplate + '\''
                    + ", xdsServers=" + xdsServers + '}';
        }
    }

    @VisibleForTesting
    public void setFileReader(FileReader reader) {
        this.reader = reader;
    }

    public interface FileReader {
        String readFile(String path) throws IOException;
    }

    protected enum LocalFileReader implements FileReader {
        INSTANCE;

        @Override
        public String readFile(String path) throws IOException {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        }
    }
}
