/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.resources;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.resources.IResourceManager;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.Message;
import org.eclipse.rap.rwt.testfixture.Message.CallOperation;
import org.eclipse.rap.rwt.testfixture.Message.CreateOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONException;


public class JavaScriptLoaderImpl_Test extends TestCase {

  private static final String JS_FILE_1 = "resourcetest1.js";
  private static final String JS_FILE_2 = "utf-8-resource.js";

  private JavaScriptLoader loader = new JavaScriptLoaderImpl();
  private IResourceManager resourceManager;
  private Display display;

  public void testRegisterOnce() {
    ensureFiles( new String[]{ JS_FILE_1 } );

    String expected = getRegistryPath() + "/resourcetest1.js";
    assertTrue( resourceManager.isRegistered( expected ) );
  }

  public void testDoNotRegisterTwice() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    ensureFiles( new String[]{ JS_FILE_2 } );

    // Same module, different return value: not a valid use case!
    // Used to check for repeated registration
    String expected = getRegistryPath() + "/" + JS_FILE_1;
    String notExpected = getRegistryPath() + "/" + JS_FILE_2;
    assertTrue( resourceManager.isRegistered( expected ) );
    assertFalse( resourceManager.isRegistered( notExpected ) );
  }

  public void testRegisterMultipleFiles() {
    ensureFiles( new String[]{ JS_FILE_1, JS_FILE_2 } );

    String expectedOne = getRegistryPath() + "/" + JS_FILE_1;
    String expectedTwo = getRegistryPath() + "/" + JS_FILE_2;
    assertTrue( resourceManager.isRegistered( expectedOne ) );
    assertTrue( resourceManager.isRegistered( expectedTwo ) );
  }

  public void testRegisterMultipleModules() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    ensureFiles( new String[]{ JS_FILE_2 }, true );

    String expectedOne = getRegistryPath( false ) + "/" + JS_FILE_1;
    String expectedTwo = getRegistryPath( true ) + "/" + JS_FILE_2;
    assertTrue( resourceManager.isRegistered( expectedOne ) );
    assertTrue( resourceManager.isRegistered( expectedTwo ) );
  }

  public void testFileNotFound() {
    try {
      ensureFiles( new String[]{ "this-file-does-not-exist.js" } );
      fail();
    } catch( Exception ex ) {
      // expected
    }
  }

  public void testNoFilesGiven() {
    try {
      ensureFiles( new String[]{} );
      fail();
    } catch( Exception ex ) {
      // expected
    }
  }

  public void testLoadOnce() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expected = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    assertNotNull( findLoadOperation( message, expected ) );
  }

  public void testLoadBeforeCreateWidget() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    Shell shell = new Shell( display );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expected = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    CreateOperation create = message.findCreateOperation( shell );
    CallOperation load = findLoadOperation( message, expected );
    assertTrue( load.getPosition() < create.getPosition() );
  }

  public void testDoNotLoadTwiceForSameRequest() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expected = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    assertNotNull( findLoadOperation( message, expected ) );
    assertEquals( 1, message.getOperationCount() );
  }

  public void testDoNotLoadTwiceForSameSession() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    Fixture.fakeNewRequest();
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expected = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    assertNull( findLoadOperation( message, expected ) );
  }

  public void testDoLoadTwiceForSameApplication() {
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    tearDown();
    setUp();
    ensureFiles( new String[]{ JS_FILE_1 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expected = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    assertNotNull( findLoadOperation( message, expected ) );
  }

  public void testLoadMultipleFiles() {
    ensureFiles( new String[]{ JS_FILE_1, JS_FILE_2 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expectedOne = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    String expectedTwo = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_2;
    CallOperation operationOne = findLoadOperation( message, expectedOne );
    CallOperation operationTwo = findLoadOperation( message, expectedTwo );
    assertNotNull( operationOne );
    assertEquals( operationOne.getPosition(), operationTwo.getPosition() );
  }

  public void testLoadMultipleFilesInOrder() throws JSONException {
    ensureFiles( new String[]{ JS_FILE_1, JS_FILE_2 } );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expectedOne = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_1;
    String expectedTwo = "rwt-resources/" + getRegistryPath() + "/" + JS_FILE_2;
    CallOperation operation = findLoadOperation( message, expectedOne );
    JSONArray files = ( JSONArray )operation.getProperty( "files" );
    assertEquals( expectedOne, files.getString( 0 ) );
    assertEquals( expectedTwo, files.getString( 1 ) );
  }

  public void testLoadMultipleModulesInSameRequest() {
    ensureFiles( new String[]{ JS_FILE_1 }, false );
    ensureFiles( new String[]{ JS_FILE_2 }, true );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String expectedOne = "rwt-resources/" + getRegistryPath( false ) + "/" + JS_FILE_1;
    String expectedTwo = "rwt-resources/" + getRegistryPath( true ) + "/" + JS_FILE_2;
    CallOperation operationOne = findLoadOperation( message, expectedOne );
    CallOperation operationTwo = findLoadOperation( message, expectedTwo );
    assertTrue( operationOne.getPosition() < operationTwo.getPosition() );
  }

  public void testLoadMultipleModulesInMultipleRequest() {
    ensureFiles( new String[]{ JS_FILE_1 }, false );
    Fixture.executeLifeCycleFromServerThread();

    Fixture.fakeNewRequest();
    ensureFiles( new String[]{ JS_FILE_2 }, true );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    String notExpected = "rwt-resources/" + getRegistryPath( false ) + "/" + JS_FILE_1;
    String expected = "rwt-resources/" + getRegistryPath( true ) + "/" + JS_FILE_2;
    assertNotNull( findLoadOperation( message, expected ) );
    assertNull( findLoadOperation( message, notExpected ) );
  }

  /////////
  // Helper

  public void setUp() {
    Fixture.setUp();
    display = new Display();
    Fixture.fakeNewRequest( display );
    resourceManager = RWT.getResourceManager();
  }

  public void tearDown() {
    Fixture.tearDown();
  }

  private void ensureFiles( String[] files ) {
    ensureFiles( files, false );
  }

  private void ensureFiles( String[] files, boolean secondModule ) {
    if( secondModule ) {
      DummyModuleTwo.files = files;
      loader.ensureModule( DummyModuleTwo.class );
    } else {
      DummyModule.files = files;
      loader.ensureModule( DummyModule.class );
    }
  }

  private String getRegistryPath() {
    return getRegistryPath( false );
  }

  private String getRegistryPath( boolean secondModule ) {
    String result;
    if( secondModule ) {
      result = "DummyModuleTwo" + String.valueOf( DummyModuleTwo.class.hashCode() );
    } else {
      result = "DummyModule" + String.valueOf( DummyModule.class.hashCode() );
    }
    return result;
  }

  private CallOperation findLoadOperation( Message message, String file ) {
    CallOperation result = null;
    for( int i = 0; i < message.getOperationCount(); i++ ) {
      if( message.getOperation( i ) instanceof CallOperation ) {
        CallOperation operation = ( CallOperation )message.getOperation( i );
        if(    operation.getTarget().equals( "rwt.client.JavaScriptLoader" )
            && "load".equals( operation.getMethodName() )
        ) {
          JSONArray files = ( JSONArray )operation.getProperty( "files" );
          for( int j = 0; j < files.length(); j++ ) {
            try {
              if( files.getString( j ).equals( file ) ) {
                result = operation;
              }
            } catch( JSONException e ) {
              throw new RuntimeException( e );
            }
          }
        }
      }
    }
    return result;
  }

  /////////////////
  // helper classes

  static public class DummyModule implements JavaScriptModule {

    public static String[] files;

    public String getDirectory() {
      return "org/eclipse/rap/rwt/internal/resources";
    }

    public String[] getFileNames() {
      return files;
    }

    public ClassLoader getLoader() {
      return this.getClass().getClassLoader();
    }

  }

  static public class DummyModuleTwo implements JavaScriptModule {

    public static String[] files;

    public String getDirectory() {
      return "org/eclipse/rap/rwt/internal/resources";
    }

    public String[] getFileNames() {
      return files;
    }

    public ClassLoader getLoader() {
      return this.getClass().getClassLoader();
    }

  }

}
