/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package hivemall.tools.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;

@Description(name = "array_concat",
        value = "_FUNC_(array<ANY> x1, array<ANY> x2, ..) - Returns a concatenated array")
@UDFType(deterministic = true, stateful = false)
public class ArrayConcatUDF extends GenericUDF {
    /**
     * @see org.apache.hadoop.hive.serde.serdeConstants
     */
    private static final java.lang.String LIST_TYPE_NAME = "array";

    private final List<Object> ret = new ArrayList<Object>();
    private ListObjectInspector[] argumentOIs;

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if (arguments.length < 1) {
            throw new UDFArgumentLengthException(
                "_FUNC_(array1, array2) needs at least 1 argument.");
        }
        final int nargs = arguments.length;
        for (int i = 0; i < nargs; i++) {
            switch (arguments[i].getCategory()) {
                case LIST:
                    if (((ListObjectInspector) (arguments[i])).getListElementObjectInspector()
                                                              .getCategory()
                                                              .equals(Category.PRIMITIVE)) {
                        break;
                    }
                default:
                    throw new UDFArgumentTypeException(0, "Argument " + i
                            + " of function CONCAT_ARRAY must be " + LIST_TYPE_NAME + "<"
                            + Category.PRIMITIVE + ">, but " + arguments[0].getTypeName()
                            + " was found.");
            }
        }

        ListObjectInspector[] listOIs = new ListObjectInspector[nargs];
        for (int i = 0; i < nargs; i++) {
            listOIs[i] = (ListObjectInspector) arguments[i];
        }
        this.argumentOIs = listOIs;

        ObjectInspector firstElemOI = listOIs[0].getListElementObjectInspector();
        ObjectInspector returnElemOI = ObjectInspectorUtils.getStandardObjectInspector(firstElemOI);

        return ObjectInspectorFactory.getStandardListObjectInspector(returnElemOI);
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        ret.clear();

        for (int i = 0; i < arguments.length; i++) {
            final Object arrayObject = arguments[i].get();
            if (arrayObject == null) {
                continue;
            }

            final ListObjectInspector arrayOI = (ListObjectInspector) argumentOIs[i];
            final int arraylength = arrayOI.getListLength(arrayObject);
            for (int j = 0; j < arraylength; j++) {
                Object rawObj = arrayOI.getListElement(arrayObject, j);
                ObjectInspector elemOI = arrayOI.getListElementObjectInspector();
                Object obj = ObjectInspectorUtils.copyToStandardObject(rawObj, elemOI);
                ret.add(obj);
            }
        }

        return ret;
    }

    @Override
    public String getDisplayString(String[] children) {
        return "concat_array(" + Arrays.toString(children) + ")";
    }
}
