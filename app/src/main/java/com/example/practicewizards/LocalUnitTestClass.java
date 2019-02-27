package com.example.practicewizards;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class LocalUnitTestClass {
    private static final String FAKE_STRING = "HELLO_WORLD";
    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void readStringFromContext_LocalizedString() {
        // Given a Context object retrieved from Robolectric...
        ClassUnderTest myObjectUnderTest = new ClassUnderTest(context);

        // ...when the string is returned from the object under test...
        String result = myObjectUnderTest.getHelloWorldString();

        // ...then the result should be the expected one.
        assertThat(result).isEqualTo(FAKE_STRING);
    }
}
