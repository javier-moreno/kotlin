/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java.mapping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.java.mapping.PrimitiveTypesUtil.asmTypeForPrimitive;

public class KotlinToJavaTypesMap extends JavaToKotlinClassMapBuilder {
    private static KotlinToJavaTypesMap instance = null;

    @NotNull
    public static KotlinToJavaTypesMap getInstance() {
        if (instance == null) {
            instance = new KotlinToJavaTypesMap();
        }
        return instance;
    }

    private final Map<FqName, Type> asmTypes = new HashMap<FqName, Type>();
    private final Map<FqName, Type> asmNullableTypes = new HashMap<FqName, Type>();

    private KotlinToJavaTypesMap() {
        init();
        initPrimitives();
    }

    private void initPrimitives() {
        FqName builtInsFqName = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            Type asmType = asmTypeForPrimitive(jvmPrimitiveType);
            FqName fqName = builtInsFqName.child(primitiveType.getTypeName());

            register(fqName, asmType);

            JvmClassName wrapperName = JvmClassName.byFqNameWithoutInnerClasses(jvmPrimitiveType.getWrapperFqName());
            registerNullable(fqName, Type.getObjectType(wrapperName.getInternalName()));

            register(builtInsFqName.child(primitiveType.getArrayTypeName()), Type.getType("[" + asmType.getDescriptor()));
        }
    }

    @Nullable
    public Type getJavaAnalog(@NotNull JetType jetType) {
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
        assert classifier != null;
        FqNameUnsafe className = DescriptorUtils.getFQName(classifier);
        if (!className.isSafe()) return null;
        FqName fqName = className.toSafe();
        if (jetType.isNullable()) {
            Type nullableType = asmNullableTypes.get(fqName);
            if (nullableType != null) {
                return nullableType;
            }
        }
        return asmTypes.get(fqName);
    }

    @Override
    protected void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        if (direction == Direction.BOTH || direction == Direction.KOTLIN_TO_JAVA) {
            register(kotlinDescriptor, AsmTypeConstants.getType(javaClass));
        }
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor,
            @NotNull Direction direction
    ) {
        if (direction == Direction.BOTH || direction == Direction.KOTLIN_TO_JAVA) {
            register(javaClass, kotlinDescriptor);
            register(javaClass, kotlinMutableDescriptor);
        }
    }

    private void register(@NotNull ClassDescriptor kotlinDescriptor, @NotNull Type javaType) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(kotlinDescriptor);
        assert fqName.isSafe();
        register(fqName.toSafe(), javaType);
    }

    private void register(@NotNull FqName fqName, @NotNull Type type) {
        asmTypes.put(fqName, type);
    }

    private void registerNullable(@NotNull FqName fqName, @NotNull Type nullableType) {
        asmNullableTypes.put(fqName, nullableType);
    }
}
