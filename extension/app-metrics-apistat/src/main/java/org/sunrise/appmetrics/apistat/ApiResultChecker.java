package org.sunrise.appmetrics.apistat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ApiResultChecker {
    protected ValueGetter getter = null;
    protected ValueChecker checker = null;

    public ApiResultChecker(Method method, String checkSuccessRule) throws NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
        this(method.getReturnType(), checkSuccessRule);
    }

    private ApiResultChecker(Class<?> methodReturnType, String checkSuccessRule) throws NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        ClassLoader ctxClassLoader = methodReturnType.getClassLoader();
        ComparedResultCheckCase caseChecker = null;
        String valueGetterStr = null;
        String toCompareStr = null;
        int pos = checkSuccessRule.indexOf('=');
        if (pos == 0) {
            if (checkSuccessRule.length() < 3) {
                throw new RuntimeException("Invalid check success rule: " + checkSuccessRule);
            }
            caseChecker = new ComparedResultEqualCheckCase();
            toCompareStr = checkSuccessRule.substring(2);
        } else if (pos > 0) {
            if (checkSuccessRule.length() - pos < 2) {
                throw new RuntimeException("Invalid check success rule: " + checkSuccessRule);
            }
            if (checkSuccessRule.charAt(pos + 1) == '=') {
                valueGetterStr = checkSuccessRule.substring(0, pos);
                caseChecker = new ComparedResultEqualCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 2);
            } else if (checkSuccessRule.charAt(pos - 1) == '!') {
                caseChecker = new ComparedResultNotEqualCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 1);
                valueGetterStr = checkSuccessRule.substring(0, pos - 1);
            } else if (checkSuccessRule.charAt(pos - 1) == '>') {
                caseChecker = new ComparedResultGreaterEqualCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 1);
                valueGetterStr = checkSuccessRule.substring(0, pos - 1);
            } else if (checkSuccessRule.charAt(pos - 1) == '<') {
                caseChecker = new ComparedResultSmallerEqualCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 1);
                valueGetterStr = checkSuccessRule.substring(0, pos - 1);
            } else {
                throw new RuntimeException("Invalid check success rule: " + checkSuccessRule);
            }
        } else {  // now without '='
            if ((pos = checkSuccessRule.indexOf('>')) >= 0) {
                caseChecker = new ComparedResultGreaterCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 1);
                valueGetterStr = checkSuccessRule.substring(0, pos);
            } else if ((pos = checkSuccessRule.indexOf('<')) >= 0) {
                caseChecker = new ComparedResultSmallerCheckCase();
                toCompareStr = checkSuccessRule.substring(pos + 1);
                valueGetterStr = checkSuccessRule.substring(0, pos);
            } else if (checkSuccessRule.endsWith("is null")) {
                this.checker = new NullValueChecker();
                valueGetterStr = checkSuccessRule.substring(0, checkSuccessRule.length() - 7);
            } else if (checkSuccessRule.endsWith("is not null")) {
                this.checker = new NotNullValueChecker();
                valueGetterStr = checkSuccessRule.substring(0, checkSuccessRule.length() - 11);
            } else {
                throw new RuntimeException("Invalid check success rule: " + checkSuccessRule);
            }
        }

        Class<?> toCompareValueClass = null;
        if (valueGetterStr == null || valueGetterStr.trim().length() == 0) {
            this.getter = generalValueGetter;
            toCompareValueClass = methodReturnType;
        } else {
            String[] items = valueGetterStr.split(" ");
            ValueGetter[] getters = new ValueGetter[items.length];

            for(int i = 0; i < items.length; i++) {
                String item = items[i];
                String resultType = null;
                if (item.charAt(0) =='(') {
                    pos = item.indexOf(')');
                    resultType = item.substring(1, pos);
                    item = item.substring(pos + 1).trim();
                }
                if ((pos = item.indexOf('(')) > 0) {
                    String valueMethodName = item.substring(0, pos);
                    Method m;
                    try {
                        m = methodReturnType.getMethod(valueMethodName);
                    } catch (NoSuchMethodException nsm) {
                        m = methodReturnType.getDeclaredMethod(valueMethodName);
                    }
                    m.setAccessible(true);
                    toCompareValueClass = m.getReturnType();
                    getters[i] = new MethodValueGetter(m);
                } else {
                    Field f;
                    try {
                        f = methodReturnType.getField(valueGetterStr);
                    } catch (NoSuchFieldException nsf) {
                        f = methodReturnType.getDeclaredField(valueGetterStr);
                    }
                    f.setAccessible(true);
                    toCompareValueClass = f.getType();
                    getters[i] = new FieldValueGetter(f);
                }
                methodReturnType = (resultType == null) ? toCompareValueClass :
                        Class.forName(resultType, false, ctxClassLoader);

            }

            this.getter = (items.length == 1) ? getters[0] : new ValueGetterChain(getters);
        }

        if (toCompareStr != null) {
            if (toCompareValueClass == String.class) {
                this.checker = new ComparableValueChecker<String>(toCompareStr, caseChecker);
            } else
            if (toCompareValueClass == Integer.class || toCompareValueClass.toString().equals("int")) {
                this.checker = new ComparableValueChecker<Integer>(Integer.parseInt(toCompareStr), caseChecker);
            } else
            if (toCompareValueClass == Long.class || toCompareValueClass.toString().equals("long")) {
                this.checker = new ComparableValueChecker<Long>(Long.parseLong(toCompareStr), caseChecker);
            } else
            if (toCompareValueClass == Boolean.class || toCompareValueClass.toString().equals("boolean")) {
                this.checker = new ComparableValueChecker<Boolean>(Boolean.parseBoolean(toCompareStr), caseChecker);
            } else {
                throw new RuntimeException("Unsupported type: " + toCompareValueClass.getName());
            }
        }
    }

    public boolean isResultSuccess(Object result) throws Exception {
        return checker.isSuccess(getter.getValue(result));
    }

    private static interface ValueChecker<T> {
        public boolean isSuccess(T value) ;
    }

    private static class NullValueChecker<T> implements ValueChecker<T> {
        @Override
        public boolean isSuccess(T value) {
            return value == null;
        }
    }

    private static class NotNullValueChecker<T> implements ValueChecker<T> {
        @Override
        public boolean isSuccess(T value) {
            return value != null;
        }
    }

    private static class ComparableValueChecker<T extends Comparable<T>> implements ValueChecker<T> {
        protected T toValue;
        private final ComparedResultCheckCase checker;
        private ComparableValueChecker(T toValue, ComparedResultCheckCase checker) {
            this.toValue = toValue;
            this.checker = checker;
        }

        public boolean isSuccess(T value) {
            if (value == null) return false;
            int comparedResult = value.compareTo(toValue);
            return checker.isSuccess(comparedResult);
        }
    }

    private static interface ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) ;
    }

    private static class ComparedResultEqualCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult == 0;
        }
    }

    private static class ComparedResultNotEqualCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult != 0;
        }
    }

    private static class ComparedResultGreaterCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult > 0;
        }
    }

    private static class ComparedResultGreaterEqualCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult >= 0;
        }
    }

    private static class ComparedResultSmallerCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult < 0;
        }
    }

    private static class ComparedResultSmallerEqualCheckCase implements ComparedResultCheckCase {
        public boolean isSuccess(int comparedResult) {
            return comparedResult <= 0;
        }
    }


    private static class ValueGetter {
        public Object getValue(Object result) throws Exception {
            return result;
        }
    }

    private static class ValueGetterChain extends ValueGetter {
        private final ValueGetter[] getters;
        public ValueGetterChain(ValueGetter[] getters) {
            this.getters = getters;
        }
        public Object getValue(Object result) throws Exception {
            for(int i = 0; i < getters.length; i++) {
                result = getters[i].getValue(result);
            }
            return result;
        }
    }

    private static final ValueGetter generalValueGetter = new ValueGetter();

    private static class FieldValueGetter extends ValueGetter {
        private Field valueField = null;
        public FieldValueGetter(Field valueField) {
            this.valueField = valueField;
        }

        public Object getValue(Object result) throws Exception {
            try {
                return valueField.get(result);
            } catch (Throwable ex) {
                throw new Exception("Field: " + valueField.getDeclaringClass().getCanonicalName() + "." +
                        valueField.getName() + ", object: " + result.getClass().getCanonicalName(), ex);
            }
        }
    }

    private static class MethodValueGetter extends ValueGetter {
        private Method resultMethod = null;
        public MethodValueGetter(Method resultMethod) {
            this.resultMethod = resultMethod;
        }

        public Object getValue(Object result) throws Exception {
            try {
                return resultMethod.invoke(result);
            } catch (Throwable ex) {
                throw new Exception("Method: " + resultMethod.getDeclaringClass().getCanonicalName() + "." +
                        resultMethod.getName() + ", object: " + result.getClass().getCanonicalName(), ex);
            }
        }

    }

}
