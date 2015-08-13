/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class SubclassTest {
  /**
   * This is a subclass of ParseObject that will be used below. We're going to imagine a world in
   * which every "Person" is an instance of "The Flash".
   */
  @ParseClassName("Person")
  public static class Person extends ParseObject {
    public String getNickname() {
      return getString("nickname");
    }

    public void setNickname(String name) {
      put("nickname", name);
    }

    public String getRealName() {
      return getString("realName");
    }

    public void setRealName(String name) {
      put("realName", name);
    }

    @Override
    void setDefaultValues() {
      setNickname("The Flash");
    }

    public static ParseQuery<Person> getQuery() {
      return ParseQuery.getQuery(Person.class);
    }
  }

  @ParseClassName("NoDefaultConstructor")
  public static class NoDefaultConstructor extends ParseObject {
    public NoDefaultConstructor(Void argument) {
    }
  }

  @ParseClassName("ClassWithDirtyingConstructor")
  public static class ClassWithDirtyingConstructor extends ParseObject {
    public ClassWithDirtyingConstructor() {
      put("foo", "Bar");
    }
  }

  @ParseClassName("UnregisteredClass")
  public static class UnregisteredClass extends ParseObject {
  }

  public static class MyUser extends ParseUser {
  }

  public static class MyUser2 extends ParseUser {
  }

  @Before
  public void setUp() throws Exception {
    ParseObject.registerParseSubclasses();
    ParseObject.registerSubclass(Person.class);
    ParseObject.registerSubclass(ClassWithDirtyingConstructor.class);
  }

  @After
  public void tearDown() throws Exception {
    ParseObject.unregisterParseSubclasses();
    ParseObject.unregisterSubclass("Person");
    ParseObject.unregisterSubclass("ClassWithDirtyingConstructor");
  }

  @SuppressWarnings("unused")
  public void testUnregisteredConstruction() throws Exception {
    Exception thrown = null;
    try {
      new UnregisteredClass();
    } catch (Exception e) {
      thrown = e;
    }
    assertNotNull(thrown);
    ParseObject.registerSubclass(UnregisteredClass.class);
    try {
      new UnregisteredClass();
    } finally {
      ParseObject.unregisterSubclass("UnregisteredClass");
    }
  }

  @Test
  public void testSubclassPointers() throws Exception {
    Person flashPointer = (Person) ParseObject.createWithoutData("Person", "someFakeObjectId");
    assertFalse(flashPointer.isDirty());
  }

  @Test
  public void testDirtyingConstructorsThrow() throws Exception {
    ClassWithDirtyingConstructor dirtyObj = new ClassWithDirtyingConstructor();
    assertTrue(dirtyObj.isDirty());
    try {
      ParseObject.createWithoutData("ClassWithDirtyingConstructor", "someFakeObjectId");
      fail("Should throw due to subclass with dirtying constructor");
    } catch (IllegalStateException e) {
      // success
    }
  }

  @Test
  public void testRegisteringSubclassesUsesMostDescendantSubclass() throws Exception {
    try {
      // When we register a ParseUser subclass, we have to clear the cached currentParseUser, so
      // we need to register a mock ParseUserController here, otherwise Parse.getCacheDir() will
      // throw an exception in unit test environment.
      ParseCurrentUserController controller = mock(ParseCurrentUserController.class);
      ParseCorePlugins.getInstance().registerCurrentUserController(controller);
      assertEquals(ParseUser.class, ParseObject.create("_User").getClass());
      ParseObject.registerSubclass(MyUser.class);
      assertEquals(MyUser.class, ParseObject.create("_User").getClass());
      ParseObject.registerSubclass(ParseUser.class);
      assertEquals(MyUser.class, ParseObject.create("_User").getClass());
      ParseObject.registerSubclass(MyUser2.class);
      assertEquals(MyUser2.class, ParseObject.create("_User").getClass());
    } finally {
      ParseObject.unregisterSubclass("_User");
      ParseCorePlugins.getInstance().reset();
    }
  }

  @Test
  public void testRegisteringClassWithNoDefaultConstructorThrows() throws Exception {
    Exception thrown = null;
    try {
      ParseObject.registerSubclass(NoDefaultConstructor.class);
    } catch (Exception e) {
      thrown = e;
    }
    assertNotNull(thrown);
  }
}