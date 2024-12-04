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

import org.apache.dubbo.common.url.component.URLAddress;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import static com.google.common.base.Preconditions.checkNotNull;

public class Node {

    private final String id;
    private final String cluster;

    @Nullable
    private final Map<String, Object> metadata;

    @Nullable
    private final Locality locality;

    private final List<URLAddress> listeningAddresses;
    private final String buildVersion;
    private final String userAgentName;

    @Nullable
    private final String userAgentVersion;

    private final List<String> clientFeatures;

    private Node(
            String id,
            String cluster,
            @Nullable Map<String, Object> metadata,
            @Nullable Locality locality,
            List<URLAddress> listeningAddresses,
            String buildVersion,
            String userAgentName,
            @Nullable String userAgentVersion,
            List<String> clientFeatures) {
        this.id = checkNotNull(id, "id");
        this.cluster = checkNotNull(cluster, "cluster");
        this.metadata = metadata;
        this.locality = locality;
        this.listeningAddresses = Collections.unmodifiableList(checkNotNull(listeningAddresses, "listeningAddresses"));
        this.buildVersion = checkNotNull(buildVersion, "buildVersion");
        this.userAgentName = checkNotNull(userAgentName, "userAgentName");
        this.userAgentVersion = userAgentVersion;
        this.clientFeatures = Collections.unmodifiableList(checkNotNull(clientFeatures, "clientFeatures"));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("cluster", cluster)
                .add("metadata", metadata)
                .add("locality", locality)
                .add("listeningAddresses", listeningAddresses)
                .add("buildVersion", buildVersion)
                .add("userAgentName", userAgentName)
                .add("userAgentVersion", userAgentVersion)
                .add("clientFeatures", clientFeatures)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(id, node.id)
                && Objects.equals(cluster, node.cluster)
                && Objects.equals(metadata, node.metadata)
                && Objects.equals(locality, node.locality)
                && Objects.equals(listeningAddresses, node.listeningAddresses)
                && Objects.equals(buildVersion, node.buildVersion)
                && Objects.equals(userAgentName, node.userAgentName)
                && Objects.equals(userAgentVersion, node.userAgentVersion)
                && Objects.equals(clientFeatures, node.clientFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                cluster,
                metadata,
                locality,
                listeningAddresses,
                buildVersion,
                userAgentName,
                userAgentVersion,
                clientFeatures);
    }

    public static final class Builder {
        private String id = "";
        private String cluster = "";

        @Nullable
        private Map<String, Object> metadata;

        @Nullable
        private Locality locality;
        // TODO(sanjaypujare): eliminate usage of listening_addresses field.
        private final List<URLAddress> listeningAddresses = new ArrayList<>();
        private String buildVersion = "";
        private String userAgentName = "";

        @Nullable
        private String userAgentVersion;

        private final List<String> clientFeatures = new ArrayList<>();

        private Builder() {}

        @VisibleForTesting
        public Node.Builder setId(String id) {
            this.id = checkNotNull(id, "id");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setCluster(String cluster) {
            this.cluster = checkNotNull(cluster, "cluster");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setMetadata(Map<String, Object> metadata) {
            this.metadata = checkNotNull(metadata, "metadata");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setLocality(Locality locality) {
            this.locality = checkNotNull(locality, "locality");
            return this;
        }

        @CanIgnoreReturnValue
        Node.Builder addListeningAddresses(URLAddress address) {
            listeningAddresses.add(checkNotNull(address, "address"));
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setBuildVersion(String buildVersion) {
            this.buildVersion = checkNotNull(buildVersion, "buildVersion");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setUserAgentName(String userAgentName) {
            this.userAgentName = checkNotNull(userAgentName, "userAgentName");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder setUserAgentVersion(String userAgentVersion) {
            this.userAgentVersion = checkNotNull(userAgentVersion, "userAgentVersion");
            return this;
        }

        @CanIgnoreReturnValue
        public Node.Builder addClientFeatures(String clientFeature) {
            this.clientFeatures.add(checkNotNull(clientFeature, "clientFeature"));
            return this;
        }

        public Node build() {
            return new Node(
                    id,
                    cluster,
                    metadata,
                    locality,
                    listeningAddresses,
                    buildVersion,
                    userAgentName,
                    userAgentVersion,
                    clientFeatures);
        }
    }

    public static Node.Builder newBuilder() {
        return new Node.Builder();
    }

    public Node.Builder toBuilder() {
        Node.Builder builder = new Node.Builder();
        builder.id = id;
        builder.cluster = cluster;
        builder.metadata = metadata;
        builder.locality = locality;
        builder.buildVersion = buildVersion;
        builder.listeningAddresses.addAll(listeningAddresses);
        builder.userAgentName = userAgentName;
        builder.userAgentVersion = userAgentVersion;
        builder.clientFeatures.addAll(clientFeatures);
        return builder;
    }

    public String getId() {
        return id;
    }

    public String getCluster() {
        return cluster;
    }

    @Nullable
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Nullable
    public Locality getLocality() {
        return locality;
    }

    public List<URLAddress> getListeningAddresses() {
        return listeningAddresses;
    }
}
