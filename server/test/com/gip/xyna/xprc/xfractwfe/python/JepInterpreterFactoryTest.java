/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "L6icense");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xprc.xfractwfe.python;



import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;



public class JepInterpreterFactoryTest extends TestCase {

    public void testObjectConvertToPython() {
        JepInterpreterFactory jepf = new JepInterpreterFactory();

        ContainerXO testObj = new ContainerXO();
        try {
            testObj.set("type", "abc");
            testObj.set("member", new BaseTestXO());
        } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
            System.out.println("could not set member variables");
        }

        Map<String, Object> result = jepf.convertToPython(testObj);
        System.out.println(result);

        ContainerException testException = new ContainerException();
        try {
            testException.set("stringMember", "www");
            testException.set("objectMember", testObj);
        } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
            System.out.println("could not set member variables");
        }
        result = jepf.convertToPython(testException);
        System.out.println(result);
    }


    public void testPrimitiveListConvertToPython() {
        JepInterpreterFactory jepf = new JepInterpreterFactory();

        PrimitiveListWrapperXO testObj = new PrimitiveListWrapperXO();
        try {
            testObj.set("values", Arrays.asList("a", "b", "c"));

        } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
            System.out.println("could not set member variables");
        }

        Map<String, Object> result = jepf.convertToPython(testObj);
        System.out.println(result);
    }

    public void testObjectListConvertToPython() {
        JepInterpreterFactory jepf = new JepInterpreterFactory();

        ListWrapperXO testObj = new ListWrapperXO();
        RoleXO role1 = new RoleXO("user1");
        RoleXO role2 = new RoleXO("user2");
        try {
            testObj.set("roles", Arrays.asList(role1, role2));

        } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
            System.out.println("could not set member variables");
        }

        Map<String, Object> result = jepf.convertToPython(testObj);
        System.out.println(result);
    }


    public static class ListWrapperXO extends BaseTestXO {

        private static final long serialVersionUID = 1L;
        private List<RoleXO> roles;


        public Set<String> getVariableNames() {
            Set<String> set = new HashSet<String>();
            set.add("roles");
            return set;
        }


        public Object get(String path) throws InvalidObjectPathException {
            if (path.equals("roles")) {
                return roles;
            } else {
                throw new InvalidObjectPathException(path);
            }
        }


        @SuppressWarnings("unchecked")
        public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            if (path.equals("roles")) {
                roles = (List<RoleXO>) value;
            } else {
                throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
            }
        }
    }

    public static class PrimitiveListWrapperXO extends BaseTestXO {

        private static final long serialVersionUID = 1L;

        @LabelAnnotation(label = "Values")
        private List<String> values;


        public Set<String> getVariableNames() {
            Set<String> set = new HashSet<String>();
            set.add("values");
            return set;
        }


        public Object get(String path) throws InvalidObjectPathException {
            if (path.equals("values")) {
                return values;
            } else {
                throw new InvalidObjectPathException(path);
            }
        }


        @SuppressWarnings("unchecked")
        public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            if (path.equals("values")) {
                values = (List<String>) value;
            } else {
                throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
            }
        }
    }

    public static class RoleXO extends BaseTestXO {

        private static final long serialVersionUID = 1L;

        private String name;


        public RoleXO() {
        }


        public RoleXO(String name) {
            this.name = name;
        }


        public Set<String> getVariableNames() {
            Set<String> set = new HashSet<String>();
            set.add("name");
            return set;
        }


        public Object get(String path) throws InvalidObjectPathException {
            if (path.equals("name")) {
                return name;
            } else {
                throw new InvalidObjectPathException(path);
            }
        }


        public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            if (path.equals("name")) {
                name = (String) value;
            } else {
                throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
            }
        }


    }

    public static class ContainerXO extends BaseTestXO {

        private static final long serialVersionUID = 1L;
        private BaseTestXO member;
        private String type;


        public Set<String> getVariableNames() {
            Set<String> set = new HashSet<String>();
            set.add("member");
            set.add("type");
            return set;
        }


        public Object get(String path) throws InvalidObjectPathException {
            if (path.equals("member")) {
                return member;
            } else if (path.equals("type")) {
                return type;
            } else {
                throw new InvalidObjectPathException(path);
            }
        }


        public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            if (path.equals("member")) {
                member = (BaseTestXO) value;
            } else if (path.equals("type")) {
                type = (String) value;
            } else {
                throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
            }
        }


    }

    public static class BaseTestXO extends XynaObject {

        private static final long serialVersionUID = 1L;


        public void collectChanges(long arg0, long arg1, IdentityHashMap<GeneralXynaObject, DataRangeCollection> arg2, Set<Long> arg3) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public Object get(String arg0) throws InvalidObjectPathException {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public void set(String arg0, Object arg1) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public boolean supportsObjectVersioning() {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public String toXml(String arg0, boolean arg1, long arg2, XMLReferenceCache arg3) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        @Override
        public XynaObject clone() {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        @Override
        public String toXml(String arg0, boolean arg1) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }

    }

    public static class ContainerException extends BaseTestException {

        private ContainerXO objectMember;
        private String stringMember;


        public ContainerException() {
            super();
        }


        public Object get(String path) throws InvalidObjectPathException {
            if (path.equals("objectMember")) {
                return objectMember;
            } else if (path.equals("stringMember")) {
                return stringMember;
            } else {
                throw new InvalidObjectPathException(path);
            }
        }


        public void set(String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            if (path.equals("objectMember")) {
                objectMember = (ContainerXO) value;
            } else if (path.equals("stringMember")) {
                stringMember = (String) value;
            } else {
                throw new XDEV_PARAMETER_NAME_NOT_FOUND(path);
            }
        }

    }
    
    public static class BaseTestException extends XynaExceptionBase {

        public BaseTestException() {
            super("");
        }


        public Object get(String arg0) throws InvalidObjectPathException {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public void set(String arg0, Object arg1) throws XDEV_PARAMETER_NAME_NOT_FOUND {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public String toXml(String arg0, boolean arg1, long arg2, XMLReferenceCache arg3) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        @Override
        public String toXml(String arg0, boolean arg1) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public boolean supportsObjectVersioning() {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public void collectChanges(long arg0, long arg1, IdentityHashMap<GeneralXynaObject, DataRangeCollection> arg2, Set<Long> arg3) {
            fail("Unreachable mock code");
            throw new IllegalStateException();
        }


        public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

            public ObjectVersion(GeneralXynaObject xo, long version,
                                 java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
                super(xo, version, changeSetsOfMembers);
            }


            protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
                return true;
            }


            public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
                return 1;
            }

        }


        public ObjectVersion createObjectVersion(long version,
                                                 java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
            return new ObjectVersion(this, version, changeSetsOfMembers);
        }


        public BaseTestException cloneWithoutCause() {
            BaseTestException cloned = new BaseTestException();
            return cloned;
        }

    }
}
