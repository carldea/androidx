/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.partialgesturescope

import androidx.test.filters.MediumTest
import androidx.ui.geometry.Offset
import androidx.ui.test.InputDispatcher.InputDispatcherTestRule
import androidx.ui.test.createComposeRule
import androidx.ui.test.inputdispatcher.verifyNoGestureInProgress
import androidx.ui.test.partialgesturescope.Common.partialGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.cancel
import androidx.ui.test.down
import androidx.ui.test.up
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.MultiPointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.expectError
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [cancel] works
 */
@MediumTest
class SendCancelTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule(disableDispatchInRealTime = true)

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Test
    fun onePointer() {
        // When we inject a down event followed by a cancel event
        partialGesture { down(downPosition1) }
        partialGesture { cancel() }

        runOnIdle {
            recorder.run {
                // Then we have recorded just 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
        }

        // And no gesture is in progress
        partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by a cancel event
        partialGesture { down(1, downPosition1) }
        partialGesture { down(2, downPosition2) }
        partialGesture { cancel() }

        runOnIdle {
            recorder.run {
                // Then we have recorded just 2 down events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)
            }
        }

        // And no gesture is in progress
        partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun cancelWithoutDown() {
        expectError<IllegalStateException> {
            partialGesture { cancel() }
        }
    }

    @Test
    fun cancelAfterUp() {
        partialGesture { down(downPosition1) }
        partialGesture { up() }
        expectError<IllegalStateException> {
            partialGesture { cancel() }
        }
    }

    @Test
    fun cancelAfterCancel() {
        partialGesture { down(downPosition1) }
        partialGesture { cancel() }
        expectError<IllegalStateException> {
            partialGesture { cancel() }
        }
    }
}
