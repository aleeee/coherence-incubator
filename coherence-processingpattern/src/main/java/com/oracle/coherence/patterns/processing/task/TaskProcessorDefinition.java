/*
 * File: TaskProcessorDefinition.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.patterns.processing.task;

import com.oracle.coherence.common.identifiers.Identifier;

import java.util.Map;

/**
 * A {@link TaskProcessorDefinition} defines a {@link TaskProcessor} There is
 * 1:N relationship between a definition and its processors.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface TaskProcessorDefinition
{
    /**
     * Returns the {@link TaskProcessorType} of the
     * {@link TaskProcessorDefinition}.
     *
     * @return the {@link TaskProcessorType}
     */
    TaskProcessorType getTaskProcessorType();


    /**
     * Returns the name of the {@link TaskProcessorDefinition}.
     *
     * @return the name
     */
    String getName();


    /**
     * Returns the unique identifier for this {@link TaskProcessorDefinition}.
     *
     * @return the {@link Identifier}
     */
    Identifier getIdentifier();


    /**
     * Returns the TaskProcessor associated with this definition.
     * This one may be unassigned (null).
     *
     * @return the {@link TaskProcessor}
     *
     */
    TaskProcessor getTaskProcessor();


    /**
     * Returns the attribute map associated with this {@link TaskProcessorDefinition}.
     *
     * @return the attribute map
     */
    Map<String, String> getAttributeMap();
}