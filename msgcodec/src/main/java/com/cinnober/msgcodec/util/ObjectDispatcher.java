/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 The MsgCodec Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cinnober.msgcodec.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * An object dispatcher can dispatch objects based on their types to methods in a list of delegates.
 *
 * <p>The dispatcher has a single entry point, {@link #dispatch(Object...)}. The dispatcher will delegate
 * the call to the most specific method in one of the delegates. The delegates should declare methods
 * with one parameter, which will be matched against the object type, and optionally a return value.
 * The name of the methods must match the specified pattern.
 *
 * <p><b>Example:</b> You have the following input types:
 * <pre>
 * class Base {}
 * class Ping extends Base {}
 * class Pong extends Base {}
 * class Thing {}
 * </pre>
 *
 * You have the following delegates:
 * <pre>
 * class MyService {
 *   public Pong onPing(Ping ping) { }
 *   public void onBase(Base base) { }
 * }
 * class MyErrorHandler {
 *   public void onUnhandledType(Object any) { }
 * }
 * </pre>
 *
 * Then create a dispatcher:
 * <pre>
 * MyService myService = ...;
 * MyErrorHandler myErrorHandler = ...;
 * ObjectDispatcher dispatcher = new ObjectDispatcher(Arrays.asList(myService, myErrorHandler));
 *
 * Object result;
 * result = dispatcher.dispatch(new Ping()); // calls MyService.onPing(..)
 * result = dispatcher.dispatch(new Pong()); // calls MyService.onBase(..)
 * result = dispatcher.dispatch(new Thing()); // calls MyErrorHandler.onUnhandledType(..)
 * </pre>
 *
 * <p>If several delegates are registered with the same object type the first registered will be called.
 *
 * <p><b>Example 2:</b> You have the following delegates:
 * <pre>
 * class MyService1 {
 *   public Pong onPing(Ping ping) { }
 * }
 * class MyService2 {*
 *   public Pong onPing(Ping ping) { }
 * }
 * </pre>
 *
 * Then create a dispatcher:
 * <pre>
 * MyService1 myService1 = ...;
 * MyService2 myService2 = ...;
 * // Note the order!! service2 is before service1 in the list, so Ping messages will be
 * // dispatched to MyService2.
 * ObjectDispatcher dispatcher = new ObjectDispatcher(Arrays.asList(myService2, myService1));
 *
 * Object result;
 * result = dispatcher.dispatch(new Ping()); // calls MyService2.onPing(..)
 * </pre>
 *
 * @author Mikael Brannstrom
 *
 */
public class ObjectDispatcher {
    private static final Logger log = Logger.getLogger(ObjectDispatcher.class.getName());

    private static final Pattern DEFAULT_PATTERN = Pattern.compile("((on)|(process)|(handle)|(do))([A-Z0-9_].*)?");

    private final Map<Class<?>, Target> targets;
    private final Class<?>[] methodSignature;
    private final SuperTypeTraverser superTypeTraverser;

    /**
     * Create an object dispatcher using the specified delegates.
     *
     * <p>Method names must matching the prefix "on", "process", "handle" or "do".
     * The methods must take one argument, i.e. no extra parameters in addition to the dispatch parameter.
     *
     * <p>The default super type traverser is used, which traverses super class then implemented interfaces on each
     * level in the inheritance tree.
     *
     * @param delegates the delegates that should be called, not null. NB! The iteration order of delegates may decide
     * which delegates that will be used, see Example 2 in the class javadoc
     */
    public ObjectDispatcher(Collection<Object> delegates) {
        this(delegates, new Class<?>[0], DEFAULT_PATTERN, new DefaultInheritanceTraverser());
    }

    /**
     * Create an object dispatcher using the specified delegates.
     *
     * <p>Method names must matching the prefix "on", "process", "handle" or "do".
     *
     * <p>The default super type traverser is used, which traverses super class then implemented interfaces on each
     * level in the inheritance tree.
     *
     * @param delegates the delegates that should be called, not null. NB! The iteration order of delegates may decide
     * which delegates that will be used, see Example 2 in the class javadoc
     * @param methodSignature the method signature for all but the first parameter, not null.
     */
    public ObjectDispatcher(Collection<Object> delegates,
            Class<?>[] methodSignature) {
        this(delegates, methodSignature, DEFAULT_PATTERN, new DefaultInheritanceTraverser());
    }

    /**
     * Create an object dispatcher using the specified delegates.
     *
     * <p>The default super type traverser is used, which traverses super class then implemented interfaces on each
     * level in the inheritance tree.
     *
     * @param delegates the delegates that should be called, not null. NB! The iteration order of delegates may decide
     * which delegates that will be used, see Example 2 in the class javadoc
     * @param methodSignature the method signature for all but the first parameter, not null.
     * @param methodPattern the pattern of the methods names in the delegates, or null for any method name.
     */
    public ObjectDispatcher(Collection<Object> delegates,
            Class<?>[] methodSignature,
            Pattern methodPattern) {
        this(delegates, methodSignature, methodPattern, new DefaultInheritanceTraverser());
    }


    /**
     * Create an object dispatcher using the specified delegates.
     *
     * @param delegates the delegates that should be called, not null. NB! The iteration order of delegates may decide
     * which delegates that will be used, see Example 2 in the class javadoc
     * @param methodSignature the method signature for all but the first parameter, not null.
     * @param methodPattern the pattern of the methods names in the delegates, or null for any method name.
     * @param superTypeTraverser object that can traverse the inheritance tree of a type, not null.
     * This traverser will be used for determining the order of type widening rules.
     * E.g. traverse super class before implemented interfaces.
     */
    public ObjectDispatcher(Collection<Object> delegates,
            Class<?>[] methodSignature,
            Pattern methodPattern,
            SuperTypeTraverser superTypeTraverser) {
        this.targets = new HashMap<>(16, 0.5f);
        this.methodSignature = Objects.requireNonNull(methodSignature);
        this.superTypeTraverser = Objects.requireNonNull(superTypeTraverser);

        for (Object delegate : delegates) {
            addDelegate(delegate, methodPattern);
        }
    }

    private void addDelegate(Object delegate, Pattern methodPattern) {
        METHODS: for (Method method : delegate.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            if (methodPattern != null && !methodPattern.matcher(method.getName()).matches()) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 + methodSignature.length) {
                continue;
            }
            Class<?> targetType = parameterTypes[0];
            for (int i = 0; i < methodSignature.length; i++) {
                if (!parameterTypes[i+1].isAssignableFrom(methodSignature[i])) {
                    continue METHODS;
                }
            }

            Target target = new Target(method, delegate);
            if (!targets.containsKey(targetType)) {
                targets.put(targetType, target);
            } else {
                log.log(Level.FINE, "Duplicate dispatch method for class {0}", targetType);
            }
        }
    }

    /**
     * Dispatch the object to any of the delegates.
     *
     * @param params the parameters to dispatch, not null. The first parameter is used for choosing
     * a suitable dispatcher.
     * @return the return value, or null if no return value.
     * @throws NoSuchMethodException if no delegate could handle the first parameter object type
     * @throws InvocationTargetException if the delegate throwed an exception
     * @throws IllegalArgumentException if the parameters does not match the required method signature.
     */
    public Object dispatch(Object... params)
            throws IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        if (params.length != 1 + methodSignature.length) {
            throw new IllegalArgumentException("Expected " + methodSignature.length + " parameters");
        }

        Class<?> type = params[0].getClass();
        Target target = findTarget(type);
        return target.process(params);
    }

    /**
     * Warm-up the object type. Any internal caches are built.
     *
     * @param type the type to warm-up. This corresponds to the first parameter, not null.
     * @throws NoSuchMethodException if no delegate could handle the object type
     */
    public void warmup(Class<?> type) throws NoSuchMethodException {
        findTarget(type);
    }

    /**
     * Find a target for the specified object type.
     *
     * @param type the type. This corresponds to the first parameter, not null.
     * @return the target, not null.
     * @throws NoSuchMethodException if no target was found.
     */
    private Target findTarget(Class<?> type) throws NoSuchMethodException {
        synchronized (targets) {
            Target target = targets.get(type);
            if (target == null) {
                for (Class<?> superType : superTypeTraverser.getSuperTypes(type)) {
                    target = targets.get(superType);
                    if (target != null) {
                        targets.put(type, target);
                        break;
                    }
                }
                if (target == null) {
                    StringBuilder str = new StringBuilder();
                    str.append("Unhandled type " + type);
                    str.append("\n");
                    str.append("Available targets:");
                    str.append("\n");
                    for (Map.Entry<Class<?>, Target> entry : targets.entrySet()) {
                        str.append("    ");
                        str.append(entry.getValue().toString());
                        str.append("\n");
                    }
                    throw new NoSuchMethodException(str.toString());
                }
            }
            return target;
        }
    }

    private static class Target {
        private final Method method;
        private final Object instance;

        /**
         * @param method
         * @param instance
         */
        Target(Method method, Object instance) {
            this.method = method;
            this.method.setAccessible(true); // make it faster
            this.instance = instance;
        }


        public Object process(Object... params) throws InvocationTargetException {
            try {
                return method.invoke(instance, params);
            } catch (IllegalAccessException e) {
                throw new Error("Bug", e); // should not happen
            }
        }
        
        @Override 
        public String toString() {
        	return method.toString();
        }
    }

    public static interface SuperTypeTraverser {
        Iterable<Class<?>> getSuperTypes(Class<?> type);
    }

    public static class DefaultInheritanceTraverser implements SuperTypeTraverser {
        @Override
        public Iterable<Class<?>> getSuperTypes(Class<?> type) {
            Collection<Class<?>> superTypes = new ArrayList<>();
            for (Class<?> currentType = type; currentType != null; currentType = currentType.getSuperclass()) {
                // super class
                Class<?> superClass = currentType.getSuperclass();
                if (superClass != null) {
                    superTypes.add(superClass);
                }
                // interfaces (recursively)
                addInterfaces(currentType, superTypes);
            }

            return superTypes;
        }

        private void addInterfaces(Class<?> type, Collection<Class<?>> superTypes) {
            Class<?>[] interfaces = type.getInterfaces();
            for (Class<?> interfaceType : interfaces) {
                superTypes.add(interfaceType);
                addInterfaces(interfaceType, superTypes);
            }
        }
    }
}
