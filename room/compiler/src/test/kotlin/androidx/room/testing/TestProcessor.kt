/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.testing

import androidx.room.processing.JavacTestProcessor
import androidx.room.processing.XRoundEnv
import androidx.room.processing.XTypeElement
import androidx.room.processor.Context
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass

class TestProcessor(
    val handlers: List<(TestInvocation) -> Boolean>,
    val annotations: MutableSet<String>
) : JavacTestProcessor() {
    var count = 0
    override fun doProcess(annotations: Set<XTypeElement>, roundEnv: XRoundEnv): Boolean {
        return handlers.getOrNull(count++)?.invoke(
            TestInvocation(xProcessingEnv, annotations, roundEnv)) ?: true
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return annotations
    }

    override fun getSupportedOptions(): MutableSet<String> {
        return Context.ARG_OPTIONS.toMutableSet()
    }

    class Builder {
        private var handlers = arrayListOf<(TestInvocation) -> Boolean>()
        private var annotations = mutableSetOf<String>()
        fun nextRunHandler(f: (TestInvocation) -> Boolean): Builder {
            handlers.add(f)
            return this
        }

        fun forAnnotations(vararg klasses: KClass<*>): Builder {
            annotations.addAll(klasses.map { it.java.canonicalName!! })
            return this
        }

        fun forAnnotations(vararg names: String): Builder {
            annotations.addAll(names)
            return this
        }

        fun build(): TestProcessor {
            if (annotations.isEmpty()) {
                throw IllegalStateException("must provide at least 1 annotation")
            }
            if (handlers.isEmpty()) {
                throw IllegalStateException("must provide at least 1 handler")
            }
            return TestProcessor(handlers, annotations)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
