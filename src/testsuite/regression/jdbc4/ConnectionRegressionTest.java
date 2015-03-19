/*
  Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FLOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package testsuite.regression.jdbc4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import testsuite.BaseTestCase;
import testsuite.regression.ConnectionRegressionTest.Bug72712StatementInterceptor;
import testsuite.regression.ConnectionRegressionTest.Bug75168LoadBalanceExceptionChecker;

public class ConnectionRegressionTest extends BaseTestCase {
    /**
     * Creates a new ConnectionRegressionTest.
     * 
     * @param name
     *            the name of the test
     */
    public ConnectionRegressionTest(String name) {
        super(name);
    }

    /**
     * Runs all test cases in this test suite
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ConnectionRegressionTest.class);
    }

    /**
     * Tests fix for Bug#75168 - loadBalanceExceptionChecker interface cannot work using JDBC4/JDK7
     * 
     * Bug observed only with JDBC4 classes. This test is duplicated in testsuite.regression.ConnectionRegressionTest#testBug75168().
     * The two nested static classes, Bug75168LoadBalanceExceptionChecker and Bug75168StatementInterceptor are shared between the two tests.
     * 
     * @throws Exception
     */
    public void testBug75168() throws Exception {
        final Properties props = new Properties();
        props.setProperty("loadBalanceExceptionChecker", Bug75168LoadBalanceExceptionChecker.class.getName());
        props.setProperty("statementInterceptors", Bug75168StatementInterceptor.class.getName());

        Connection connTest = getLoadBalancedConnection(2, null, props); // get a load balancing connection with two default servers
        for (int i = 0; i < 3; i++) {
            Statement stmtTest = null;
            try {
                stmtTest = connTest.createStatement();
                stmtTest.execute("SELECT * FROM nonexistent_table");
                fail("'Table doesn't exist' exception was expected.");
            } catch (SQLException e) {
                assertTrue("'Table doesn't exist' exception was expected.", e.getMessage().endsWith("nonexistent_table' doesn't exist"));
            } finally {
                if (stmtTest != null) {
                    stmtTest.close();
                }
            }
        }
        connTest.close();

        boolean stop = false;
        do {
            connTest = getLoadBalancedConnection(2, null, props); // get a load balancing connection with two default servers
            for (int i = 0; i < 3; i++) {
                PreparedStatement pstmtTest = null;
                try {
                    pstmtTest = connTest.prepareStatement("SELECT * FROM nonexistent_table");
                    pstmtTest.execute();
                    fail("'Table doesn't exist' exception was expected.");
                } catch (SQLException e) {
                    assertTrue("'Table doesn't exist' exception was expected.", e.getMessage().endsWith("nonexistent_table' doesn't exist"));
                } finally {
                    if (pstmtTest != null) {
                        pstmtTest.close();
                    }
                }
            }
            connTest.close();

            // do it again with server prepared statements
            props.setProperty("useServerPrepStmts", "true");
        } while (stop = !stop);
    }
}
