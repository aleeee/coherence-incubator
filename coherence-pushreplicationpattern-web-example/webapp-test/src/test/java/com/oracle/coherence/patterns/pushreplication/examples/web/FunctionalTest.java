/*
 * File: FunctionalTest.java
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

package com.oracle.coherence.patterns.pushreplication.examples.web;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

import com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer;

import com.oracle.tools.deferred.Deferred;
import com.oracle.tools.deferred.DeferredAssert;
import com.oracle.tools.deferred.DeferredHelper;
import com.oracle.tools.deferred.Eventually;
import com.oracle.tools.deferred.InstanceUnavailableException;
import com.oracle.tools.deferred.UnresolvableInstanceException;

import com.oracle.tools.runtime.PropertiesBuilder;

import com.oracle.tools.runtime.coherence.Cluster;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.NativeJavaApplicationBuilder;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplicationSchema;

import com.oracle.tools.runtime.java.container.Container;
import com.oracle.tools.runtime.network.AvailablePortIterator;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.tools.deferred.DeferredHelper.eventually;
import static com.oracle.tools.deferred.DeferredHelper.invoking;

import static junit.framework.Assert.assertEquals;

import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.TimeUnit;

/**
 * Test that the CoherenceWeb Examples Work
 */
public class FunctionalTest
{
    private static Cluster               cluster1;
    private static Cluster               cluster2;
    private static SimpleJavaApplication site1;
    private static int                   site1Port;
    private static SimpleJavaApplication site2;
    private static int                   site2Port;


    /**
     * Setup the test including cache and web servers
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        // Staring a process with oracle tools from within an oracle tools process appears to be problematic
        // we'll parse the config and start the processes here.
        AvailablePortIterator portIter      = Container.getAvailablePorts();

        int                   acceptor1Port = portIter.next();
        int                   acceptor2Port = portIter.next();

        // Setup site 1
        PropertiesBuilder globalProps1 = WebServer.parseConfig("System-Property", "site1.properties");
        PropertiesBuilder cohProps1    = WebServer.parseConfig("COHSystem-Property", "site1.properties");

        PropertiesBuilder cache1Props  = new PropertiesBuilder(globalProps1);

        cache1Props.addProperties(cohProps1);

        // Override the system properties for ports for the test environment
        cache1Props.setProperty("client.port", acceptor2Port);
        cache1Props.setProperty("bind.port", acceptor1Port);

        // Setup site 2
        PropertiesBuilder globalProps2    = WebServer.parseConfig("System-Property", "site2.properties");
        PropertiesBuilder cohProps2       = WebServer.parseConfig("COHSystem-Property", "site2.properties");

        PropertiesBuilder cache2Props     = new PropertiesBuilder(globalProps2);

        cache2Props.addProperties(cohProps2);

        // Override the system properties for ports for the test environment
        cache2Props.setProperty("client.port", acceptor1Port);
        cache2Props.setProperty("bind.port", acceptor2Port);

        NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema> appBuilder =
            new NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema>();

        SystemApplicationConsole console = new SystemApplicationConsole();

        // Startup Site1
        site1Port = portIter.next();

        SimpleJavaApplicationSchema site1schema =
            new SimpleJavaApplicationSchema("com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer");

        site1schema.addArgument("site1.properties");
        site1schema.addArgument(String.valueOf(site1Port));
        site1schema.setSystemProperties(globalProps1);

        cluster1 = WebServer.startCacheServer(cache1Props);
        site1    = appBuilder.realize(site1schema, "Site1-Web", console);

        Eventually.assertThat(eventually(invoking(cluster1).getClusterSize()), is(2));

        // Startup Site2
        site2Port = portIter.next();

        SimpleJavaApplicationSchema site2schema =
            new SimpleJavaApplicationSchema("com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer");

        site2schema.addArgument("site2.properties");
        site2schema.addArgument(String.valueOf(site2Port));
        site2schema.setSystemProperties(globalProps2);

        cluster2 = WebServer.startCacheServer(cache2Props);
        site2    = appBuilder.realize(site2schema, "Site2-Web", console);

        Eventually.assertThat(eventually(invoking(cluster2).getClusterSize()), is(2));
    }


    /**
     * Shutdown the cache and web servers
     */
    @AfterClass
    public static void tearDown()
    {
        cluster1.destroy();
        cluster2.destroy();
        site1.destroy();
        site2.destroy();
    }


    /**
     * Test Session Replication by connecting to each of the web servers and validating that session data stored in
     * one cluster is visible to the other cluster.
     *
     * @throws Exception
     */
    @Test
    public void testSessionReplication() throws Exception
    {
        try
        {
            final WebConversation wc       = new WebConversation();

            final String          site1url = "http://127.0.0.1:" + site1Port + "/sessionAccess.jsp";
            final String          site2url = "http://127.0.0.1:" + site2Port + "/sessionAccess.jsp";

            // Make initial request to siteA and add a value to the session
            WebResponse response = DeferredHelper.ensure(new DeferredWebResponse(wc, site1url), 60, TimeUnit.SECONDS);

            WebForm     form     = response.getFormWithID("HttpSessionAttributesForm");

            form.setParameter("key", "A");
            form.setParameter("value", "B");
            response = form.submit(form.getSubmitButton("action", "add"));

            // Assert that the session variables were set
            WebTable table = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));

            // Now make a request to the siteB, validate our session data is there and add another value
            DeferredAssert.assertThat("First set of session variables",
                                      new DeferredRowCount(wc, site2url),
                                      Matchers.equalTo(2),
                                      60,
                                      TimeUnit.SECONDS);

            response = wc.getResponse(site2url);
            table    = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));

            // Add another set of variables from this side
            form = response.getFormWithID("HttpSessionAttributesForm");
            form.setParameter("key", "C");
            form.setParameter("value", "D");

            response = form.submit(form.getSubmitButton("action", "add"));
            table    = response.getTableWithID("HttpSessionAttributes");
            assertEquals("rows", 3, table.getRowCount());

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));
            assertEquals("key2", "C", table.getCellAsText(2, 0));
            assertEquals("value2", "D", table.getCellAsText(2, 1));

            // Now go back to the original site and make sure everything replicated properly
            DeferredAssert.assertThat("First set of session variables",
                                      new DeferredRowCount(wc, site1url),
                                      Matchers.equalTo(3),
                                      60,
                                      TimeUnit.SECONDS);

            response = wc.getResponse(site1url);
            table    = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));
            assertEquals("key2", "C", table.getCellAsText(2, 0));
            assertEquals("value2", "D", table.getCellAsText(2, 1));

            System.out.println("*** Asserted both sites have the same session data shutting down the cluster!");
        }
        catch (Exception e)
        {
            e.printStackTrace();

            throw e;
        }
    }


    /**
     * Note that the DeferredRowCount is necessary to take into account that there will be some lag required for the
     * session data to replicate between sites
     */
    private class DeferredRowCount implements Deferred<Integer>
    {
        private WebConversation wc;
        private String          request;


        /**
         * Constructs a Deferred RowCount
         *
         *
         * @param wc      Handle to the web conversation
         * @param request request to make over the conversation
         */
        public DeferredRowCount(WebConversation wc,
                                String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        /**
         * Return the deferred row count value.
         *
         * @return the deferred row count value
         *
         * @throws UnresolvableInstanceException, InstanceUnavailableException
         */
        @Override
        public Integer get() throws UnresolvableInstanceException, InstanceUnavailableException
        {
            try
            {
                WebResponse response = DeferredHelper.ensure(new DeferredWebResponse(wc, request));
                WebTable    table    = response.getTableWithID("HttpSessionAttributes");

                return table.getRowCount();
            }
            catch (Exception e)
            {
                System.out.println("Error getting rowcount for " + request + " - " + "HttpSessionAttributes");
                e.printStackTrace();

                throw new InstanceUnavailableException(this, e);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Class<Integer> getDeferredClass()
        {
            return Integer.class;
        }
    }


    /**
     * Deferred WebResponse is useful for getting resources from a test environment where it may take time for the
     * resources to be available
     */
    private class DeferredWebResponse implements Deferred<WebResponse>
    {
        private WebConversation wc;
        private String          request;


        /**
         * Construct a DeferredWebResponse
         *
         *
         * @param wc      Handle to the web conversation
         * @param request Request to make over the web conversation
         */
        public DeferredWebResponse(WebConversation wc,
                                   String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        /**
         * Return the WebResponse from the Deferred.
         *
         * @return WebResponse from the Deferred.
         */
        @Override
        public WebResponse get() throws UnresolvableInstanceException, InstanceUnavailableException
        {
            try
            {
                return wc.getResponse(request);
            }
            catch (Exception e)
            {
                throw new InstanceUnavailableException(this, e);
            }
        }


        /**
         * Method description
         *
         * @return
         */
        @Override
        public Class<WebResponse> getDeferredClass()
        {
            return WebResponse.class;
        }
    }
}