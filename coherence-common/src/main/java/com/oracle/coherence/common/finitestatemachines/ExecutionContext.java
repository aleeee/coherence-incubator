/*
 * File: ExecutionContext.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.finitestatemachines;

/**
 * Provides contextual information about a {@link FiniteStateMachine},
 * typically to aid in runtime decision making for actions
 * (eg: {@link TransitionAction}s, {@link StateEntryAction}s and/or
 * {@link StateExitAction}s) and {@link Event}s.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ExecutionContext
{
    /**
     * Obtains the name of the {@link FiniteStateMachine} that produced the
     * {@link ExecutionContext}.
     *
     * @return  the name of the {@link FiniteStateMachine}
     */
    public String getName();


    /**
     * Obtains the number of successful transitions that have occurred on the
     * {@link FiniteStateMachine} thus far.
     *
     * @return  the number of transitions that have occurred on the
     *          {@link FiniteStateMachine}
     */
    public long getTransitionCount();


    /**
     * Determines if there is a potential backlog of pending events
     * (or waiting threads) for the {@link FiniteStateMachine} that
     * may cause transitions.
     * <p>
     * {@link StateEntryAction}, {@link StateExitAction} and {@link TransitionAction}s
     * may use this information to terminate early.
     *
     * @return  <code>true</code> if there is a backlog of pending events
     *          (of threads) that may cause transitions, <code>false</code>
     *          if there are no pending events (or if the {@link FiniteStateMachine}
     *          is stopped).
     */
    public boolean hasPendingEvents();


    /**
     * Determines if the {@link FiniteStateMachine} is accepting events.
     * <p>
     * When a {@link FiniteStateMachine} is no longer accepting events, that
     * typically indicates that it is in the process of stopping.
     * <p>
     * {@link StateEntryAction}, {@link StateExitAction} and {@link TransitionAction}s
     * may use this information to terminate early.
     *
     * @return <code>true</code> if the {@link FiniteStateMachine} is accepting
     *         events, <code>false</code> otherwise.
     */
    public boolean isAcceptingEvents();
}
