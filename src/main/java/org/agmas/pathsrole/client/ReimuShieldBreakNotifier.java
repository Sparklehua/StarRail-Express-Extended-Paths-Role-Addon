package org.agmas.pathsrole.client;

public class ReimuShieldBreakNotifier {
    private static volatile boolean shouldSkipGenericMessage = false;

    public static void markShouldSkip() {
        shouldSkipGenericMessage = true;
    }

    public static boolean consumeShouldSkip() {
        if (shouldSkipGenericMessage) {
            shouldSkipGenericMessage = false;
            return true;
        }
        return false;
    }
}