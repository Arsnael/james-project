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

package org.apache.james.webadmin.routes;

import java.util.Map;
import java.util.Optional;

import org.apache.james.core.MailAddress;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

class TypeListFetcher {

    static ImmutableSet<String> getTypeList(RecipientRewriteTable recipientRewriteTable, Mapping.Type type) throws RecipientRewriteTableException {
        return Optional.ofNullable(recipientRewriteTable.getAllMappings())
            .map(mappings ->
                mappings.entrySet().stream()
                    .filter(e -> e.getValue().contains(type))
                    .map(Map.Entry::getKey)
                    .flatMap(source -> OptionalUtils.toStream(source.asMailAddress()))
                    .map(MailAddress::asString)
                    .collect(Guavate.toImmutableSortedSet()))
            .orElse(ImmutableSortedSet.of());
    }

}
