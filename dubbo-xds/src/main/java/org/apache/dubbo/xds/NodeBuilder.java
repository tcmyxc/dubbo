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
package org.apache.dubbo.xds;

import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.istio.IstioEnv;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.envoyproxy.envoy.config.core.v3.Node;

public class NodeBuilder {

    public static Node build() {
        BootstrapInfo bootstrapInfo = Bootstrapper.getInstance().bootstrap();
        assert bootstrapInfo.getNode().getMetadata() != null;
        String podId = bootstrapInfo.getNode().getId();
        String podNamespace =
                (String) bootstrapInfo.getNode().getMetadata().getOrDefault("NAMESPACE", "EMPTY_NAME_SPACE");
        String clusterName = (String) bootstrapInfo.getNode().getMetadata().getOrDefault("CLUSTER_ID", "Kubernetes");
        String generatorName = (String) bootstrapInfo.getNode().getMetadata().getOrDefault("GENERATOR", "grpc");
        String saName = IstioEnv.getInstance().getServiceAccountName();

        Map<String, Value> metadataMap = new HashMap<>();

        metadataMap.put(
                "ISTIO_META_NAMESPACE",
                Value.newBuilder().setStringValue(podNamespace).build());
        metadataMap.put(
                "SERVICE_ACCOUNT", Value.newBuilder().setStringValue(saName).build());

        metadataMap.put(
                "GENERATOR", Value.newBuilder().setStringValue(generatorName).build());
        metadataMap.put(
                "NAMESPACE", Value.newBuilder().setStringValue(podNamespace).build());

        Struct metadata = Struct.newBuilder().putAllFields(metadataMap).build();

        // id -> sidecar~ip~{POD_NAME}~{NAMESPACE_NAME}.svc.cluster.local
        // cluster -> {SVC_NAME}
        return Node.newBuilder()
                .setMetadata(metadata)
                .setId(podId)
                .setCluster(clusterName)
                .build();
    }
}
