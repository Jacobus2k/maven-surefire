package org.apache.maven.surefire.battery;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;

public class TestListenerInvocationHandler
    implements InvocationHandler
{
    // The String names of the four methods in interface junit.framework.TestListener
    private static final String START_TEST = "startTest";
    private static final String ADD_FAILURE = "addFailure";
    private static final String ADD_ERROR = "addError";
    private static final String END_TEST = "endTest";

    private HashSet failedTestsSet = new HashSet();

    private ReporterManager reportManager;

    private class FailedTest
    {
        private Object testThatFailed;

        private Thread threadOnWhichTestFailed;

        public FailedTest( Object testThatFailed, Thread threadOnWhichTestFailed )
        {
            if ( testThatFailed == null )
            {
                throw new NullPointerException( "testThatFailed is null" );
            }

            if ( threadOnWhichTestFailed == null )
            {
                throw new NullPointerException( "threadOnWhichTestFailed is null" );
            }

            this.testThatFailed = testThatFailed;

            this.threadOnWhichTestFailed = threadOnWhichTestFailed;
        }

        public boolean equals( Object o )
        {

            if ( ( o == null ) || ( getClass() != o.getClass() ) )
            {
                return false;
            }

            FailedTest ft = (FailedTest) o;

            if ( ft.testThatFailed != testThatFailed )
            {
                return false;
            }

            if ( !ft.threadOnWhichTestFailed.equals( threadOnWhichTestFailed ) )
            {
                return false;
            }

            return true;
        }

        public int hashCode()
        {
            return threadOnWhichTestFailed.hashCode();
        }
    }

    public TestListenerInvocationHandler( ReporterManager reportManager,
                                          Object instanceOfTestResult,
                                          ClassLoader loader )
    {
        if ( reportManager == null )
        {
            throw new NullPointerException( "reportManager is null" );
        }

        if ( instanceOfTestResult == null )
        {
            throw new NullPointerException( "instanceOfTestResult is null" );
        }

        if ( loader == null )
        {
            throw new NullPointerException( "loader is null" );
        }

        this.reportManager = reportManager;
    }

    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        String methodName = method.getName();

        if ( methodName.equals( START_TEST ) )
        {
            handleStartTest( args );
        }
        else if ( methodName.equals( ADD_ERROR ) )
        {
            handleAddError( args );
        }
        else if ( methodName.equals( ADD_FAILURE ) )
        {
            handleAddFailure( args );
        }
        else if ( methodName.equals( END_TEST ) )
        {
            handleEndTest( args );
        }

        return null;
    }

    // Handler for TestListener.startTest(Test)
    public void handleStartTest( Object[] args )
    {
        ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[0].getClass().getName() );

        reportManager.testStarting( report );
    }

    // Handler for TestListener.addFailure(Test, Throwable)
    private void handleAddError( Object[] args )
    {
        ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[1].toString(), (Throwable) args[1] );

        reportManager.testError( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private void handleAddFailure( Object[] args )
    {
        ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[1].toString(), (Throwable) args[1] );

        reportManager.testFailed( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private void handleEndTest( Object[] args )
    {
        boolean testHadFailed = failedTestsSet.remove( new FailedTest( args[0], Thread.currentThread() ) );

        if ( !testHadFailed )
        {
            ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[0].getClass().getName() );

            reportManager.testSucceeded( report );
        }
    }
}