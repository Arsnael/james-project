/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap;

import java.util.Arrays;

import com.google.common.base.Preconditions;

public enum Version {
    DRAFT("draft"),
    RFC8621("rfc-8621");

    public static Version of(String version) {
        Preconditions.checkNotNull(version);

        return Arrays.stream(values())
            .filter(jmapVersion -> jmapVersion.version.equalsIgnoreCase(version))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(version + " is not a supported version"));
    }

    private final String version;

    Version(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
